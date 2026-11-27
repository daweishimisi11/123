package com.av123.video.ui.components.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.av123.video.data.prefs.SeekGestureSensitivity
import com.av123.video.ui.util.findActivity

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** 竖向滑动手势调节手势占屏幕高度的比例（滑过 75% 高度可覆盖全量程） */
private const val VERTICAL_GESTURE_HEIGHT_FRACTION = 0.75f

internal enum class VerticalGestureSide {
    BRIGHTNESS,
    VOLUME
}

internal fun classifyVerticalGestureSide(startX: Float, widthPx: Float): VerticalGestureSide =
    if (startX < widthPx / 2f) VerticalGestureSide.BRIGHTNESS else VerticalGestureSide.VOLUME

/**
 * 判断全屏手势起点是否落在有效操作安全区内，避免边缘系统手势误触发亮度/音量/快进调节：
 * - 上下各留 [verticalDeadZonePx] 死区（避开顶部下拉状态栏/通知栏与底部上滑导航栏）；
 * - 左右各留 [horizontalDeadZonePx] 死区（避开全面屏侧边返回手势）；
 * - 对小尺寸窗口按容器尺寸比例做上限钳制，防止安全区被过度压缩。
 */
internal fun isWithinPlayerGestureSafeZone(
    startX: Float,
    startY: Float,
    widthPx: Float,
    heightPx: Float,
    verticalDeadZonePx: Float,
    horizontalDeadZonePx: Float = 0f
): Boolean {
    if (widthPx <= 0f || heightPx <= 0f) return false
    val clampedVDead = verticalDeadZonePx.coerceIn(0f, heightPx * 0.2f)
    val clampedHDead = horizontalDeadZonePx.coerceIn(0f, widthPx * 0.1f)
    return startY >= clampedVDead &&
        startY <= (heightPx - clampedVDead) &&
        startX >= clampedHDead &&
        startX <= (widthPx - clampedHDead)
}

/**
 * 竖滑调节指示条停靠在对侧，避免被调节手指遮挡并让出画面中心视野：
 * 左侧滑调亮度时显示在右侧；右侧滑调音量时显示在左侧。
 */
internal fun gestureOverlayAlignment(side: VerticalGestureSide): Alignment = when (side) {
    VerticalGestureSide.BRIGHTNESS -> Alignment.CenterEnd
    VerticalGestureSide.VOLUME -> Alignment.CenterStart
}

internal fun computeDraggedBrightness(
    startBrightness: Float,
    accumulatedDy: Float,
    heightPx: Float
): Float {
    val safeHeight = heightPx.coerceAtLeast(1f) * VERTICAL_GESTURE_HEIGHT_FRACTION
    val delta = -accumulatedDy / safeHeight
    return (startBrightness + delta).coerceIn(0.01f, 1.0f)
}

internal fun computeDraggedVolume(
    startFraction: Float,
    accumulatedDy: Float,
    heightPx: Float,
    maxVolume: Int
): Pair<Int, Float> {
    val safeMax = maxVolume.coerceAtLeast(1)
    val safeHeight = heightPx.coerceAtLeast(1f) * VERTICAL_GESTURE_HEIGHT_FRACTION
    val delta = -accumulatedDy / safeHeight
    val nextFraction = (startFraction + delta).coerceIn(0f, 1f)
    val targetIndex = (nextFraction * safeMax).roundToInt().coerceIn(0, safeMax)
    return targetIndex to nextFraction
}

/**
 * 计算横滑 Seek 的目标位置（面向几秒到几十秒级微调）：
 * - 划满全屏宽度对应 [sensitivity.fullSwipeSeconds] 秒，短片自动不超过总时长；
 * - 脱离视频总长按百分比强缩放，避免长片轻滑一次跳过数分钟。
 */
internal fun computeHorizontalSeekTargetMs(
    startPositionMs: Long,
    accumulatedDx: Float,
    widthPx: Float,
    durationMs: Long,
    sensitivity: SeekGestureSensitivity
): Long {
    if (durationMs <= 0L) return 0L
    val safeWidth = widthPx.coerceAtLeast(1f)
    val fullSwipeMs = minOf(sensitivity.fullSwipeSeconds * 1_000L, durationMs).toFloat()
    val deltaMs = (accumulatedDx / safeWidth) * fullSwipeMs
    return (startPositionMs + deltaMs.roundToLong()).coerceIn(0L, durationMs)
}

/** 横滑 Seek 气泡中的相对位移提示（如 "+12s" / "-8s"），未发生整秒位移返回 null */
internal fun formatSeekDeltaSeconds(startPositionMs: Long, targetPositionMs: Long): String? {
    val deltaSec = ((targetPositionMs - startPositionMs) / 1_000.0).roundToInt()
    if (deltaSec == 0) return null
    return if (deltaSec > 0) "+${deltaSec}s" else "${deltaSec}s"
}


/**
 * 根据全屏状态与屏幕方向同步 Activity 方向与系统栏。
 */
@Composable
internal fun FullscreenSystemUiSync(isFullscreen: Boolean) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val activity = remember(context) { context.findActivity() }

    // 全屏状态或屏幕方向变化时，重新同步系统栏与 Activity 方向
    LaunchedEffect(isFullscreen, configuration.orientation) {
        activity?.let { setFullscreen(it, isFullscreen) }
    }

    // 播放器离开组合（退出页面）时恢复竖屏与系统栏，避免状态残留
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { setFullscreen(it, false) }
        }
    }
}

private fun setFullscreen(activity: Activity, fullscreen: Boolean) {
    val window = activity.window
    val controller = WindowCompat.getInsetsController(window, window.decorView)

    if (fullscreen) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        controller.show(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
    }
}

// ------------------------------------------------------------------------------------------------
// 清晰度轨道选择
// ------------------------------------------------------------------------------------------------

internal data class VideoTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val height: Int,
    val bitrate: Int,
    val label: String,
    val mediaTrackGroup: TrackGroup
)

@androidx.annotation.OptIn(UnstableApi::class)
internal fun parseVideoTracks(tracks: Tracks): List<VideoTrack> {
    val result = mutableListOf<VideoTrack>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != C.TRACK_TYPE_VIDEO) return@forEachIndexed
        for (trackIndex in 0 until group.length) {
            val format = group.getTrackFormat(trackIndex)
            val height = format.height
            if (height <= 0) continue
            val bitrate = format.bitrate
            val label = buildString {
                append(height)
                append("P")
                if (bitrate > 0) {
                    append(" ")
                    append(bitrate / 1_000_000)
                    append("Mbps")
                }
            }
            result.add(
                VideoTrack(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    height = height,
                    bitrate = bitrate,
                    label = label,
                    mediaTrackGroup = group.mediaTrackGroup
                )
            )
        }
    }
    // 按清晰度从高到低排列
    return result.sortedByDescending { it.height }
}

/** 将视频高度规范化为紧凑清晰度文案（如 480p / 720p / 1080p）；无效高度返回 null */
internal fun formatResolutionBadge(height: Int): String? =
    height.takeIf { it > 0 }?.let { "${it}p" }

/** 底部清晰度按钮文案：手动锁定某轨道时显示轨道名，自动档直接展示当前实际播放分辨率 */
internal fun resolveAutoTrackButtonLabel(
    selectedTrack: VideoTrack?,
    activeHeight: Int,
    autoFallbackLabel: String
): String = selectedTrack?.label ?: formatResolutionBadge(activeHeight) ?: autoFallbackLabel

/** 清晰度菜单内“自动”条目文案：当前分辨率已知时显示“自动 (720p)”以便区分档位 */
internal fun resolveAutoTrackMenuLabel(
    autoLabel: String,
    activeHeight: Int
): String = formatResolutionBadge(activeHeight)?.let { "$autoLabel ($it)" } ?: autoLabel

@androidx.annotation.OptIn(UnstableApi::class)
internal fun resolveSelectedVideoHeight(tracks: Tracks): Int {
    tracks.groups.forEach { group ->
        if (group.type != C.TRACK_TYPE_VIDEO) return@forEach
        for (trackIndex in 0 until group.length) {
            if (group.isTrackSelected(trackIndex)) {
                val height = group.getTrackFormat(trackIndex).height
                if (height > 0) return height
            }
        }
    }
    return 0
}

internal enum class BufferHealthLevel { LOW, FAIR, GOOD }

/** 计算当前播放位置前方的剩余缓冲时长（秒），负差值钳制为 0 */
internal fun computeBufferHealthSeconds(positionMs: Long, bufferedPositionMs: Long): Float =
    (bufferedPositionMs - positionMs).coerceAtLeast(0L) / 1000f

/** 格式化缓冲健康秒数文本（如 12.4s / 0.0s） */
internal fun formatBufferHealthSeconds(bufferSeconds: Float): String =
    String.format(java.util.Locale.US, "%.1fs", bufferSeconds.coerceAtLeast(0f))

/** 缓冲健康分级：低于 3s 告警、3–10s 一般、10s 及以上健康 */
internal fun resolveBufferHealthLevel(bufferSeconds: Float): BufferHealthLevel = when {
    bufferSeconds < 3f -> BufferHealthLevel.LOW
    bufferSeconds < 10f -> BufferHealthLevel.FAIR
    else -> BufferHealthLevel.GOOD
}

@androidx.annotation.OptIn(UnstableApi::class)
internal fun buildTrackSelectionParameters(
    currentParameters: TrackSelectionParameters,
    track: VideoTrack?
): TrackSelectionParameters {
    val builder = currentParameters.buildUpon()
    if (track == null) {
        builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
    } else {
        builder.setOverrideForType(
            TrackSelectionOverride(track.mediaTrackGroup, listOf(track.trackIndex))
        )
    }
    return builder.build()
}

@androidx.annotation.OptIn(UnstableApi::class)
internal fun applyTrackSelection(player: ExoPlayer, track: VideoTrack?) {
    player.trackSelectionParameters =
        buildTrackSelectionParameters(player.trackSelectionParameters, track)
}

@androidx.annotation.OptIn(UnstableApi::class)
internal fun extractHttpResponseCode(error: Throwable?): Int? =
    generateSequence(error) { it.cause }
        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
        .firstOrNull()
        ?.responseCode

/**
 * 判断播放错误层点击重试时是否应重建抓流源而非重复 prepare 同一失效 URL：
 * - HTTP 4xx（除 429 限流外，如 403 签名过期 / 404 不存在）立即重新抓流；
 * - 其余错误允许原地重连一次，再次失败后再重新抓流，避免陷入死循环。
 */
internal fun shouldReResolveOnError(
    httpStatusCode: Int?,
    retryAttempts: Int
): Boolean {
    if (retryAttempts > 0) return true
    return httpStatusCode != null && httpStatusCode != 429 && httpStatusCode in 400..499
}

@androidx.annotation.OptIn(UnstableApi::class)
internal fun shouldReResolveOnError(
    error: PlaybackException?,
    retryAttempts: Int
): Boolean = shouldReResolveOnError(extractHttpResponseCode(error), retryAttempts)

internal const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
internal const val ACTION_STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
internal const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

internal fun isMusicVolumeBroadcast(action: String?, streamType: Int): Boolean =
    (action == ACTION_VOLUME_CHANGED || action == ACTION_STREAM_MUTE_CHANGED) &&
        (streamType == -1 || streamType == AudioManager.STREAM_MUSIC)

/**
 * 判断播放器当前是否应保持屏幕常亮：
 * - 正常播放中保持常亮，防止长时间观影触发息屏或锁屏；
 * - 全屏状态下缓冲等待时同样保持常亮，避免缓冲期间黑屏；
 * - 暂停、播放结束、出错或进入画中画时释放常亮标志。
 */
internal fun shouldKeepScreenOn(
    isPlaying: Boolean,
    playWhenReady: Boolean,
    playbackState: Int,
    isFullscreen: Boolean,
    isInPip: Boolean,
    hasError: Boolean
): Boolean {
    if (isInPip || hasError) return false
    if (isPlaying) return true
    return isFullscreen &&
        playWhenReady &&
        playbackState != Player.STATE_ENDED &&
        playbackState != Player.STATE_IDLE
}

internal sealed interface VolumeMuteAction {
    data object MuteStream : VolumeMuteAction
    data class Unmute(val restoreVolume: Int?) : VolumeMuteAction
}

/**
 * 计算右下角音量按钮点击时的静音切换动作：
 * - 非静音且音量 > 0 时静音媒体流；
 * - 已静音或音量归零时取消静音，若当前系统音量为 0 则恢复至此前非零音量（或安全默认音量）。
 */
internal fun resolveVolumeMuteToggle(
    currentVolume: Int,
    isMuted: Boolean,
    lastNonZeroVolume: Int,
    maxVolume: Int
): VolumeMuteAction {
    val effectivelyMuted = isMuted || currentVolume <= 0
    if (!effectivelyMuted) return VolumeMuteAction.MuteStream
    if (currentVolume > 0) return VolumeMuteAction.Unmute(restoreVolume = null)
    val safeMax = maxVolume.coerceAtLeast(1)
    val fallbackVolume = (safeMax / 3).coerceAtLeast(1)
    val target = if (lastNonZeroVolume > 0) {
        lastNonZeroVolume.coerceIn(1, safeMax)
    } else {
        fallbackVolume
    }
    return VolumeMuteAction.Unmute(restoreVolume = target)
}