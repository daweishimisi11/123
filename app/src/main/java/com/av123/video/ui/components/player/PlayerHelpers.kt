package com.av123.video.ui.components.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.av123.video.data.prefs.ResizeModeOption
import com.av123.video.ui.util.findActivity

import kotlin.math.roundToInt

/** 竖向滑动手势调节手势占屏幕高度的比例（滑过 75% 高度可覆盖全量程） */
private const val VERTICAL_GESTURE_HEIGHT_FRACTION = 0.75f

internal enum class VerticalGestureSide {
    BRIGHTNESS,
    VOLUME
}

internal fun classifyVerticalGestureSide(startX: Float, widthPx: Float): VerticalGestureSide =
    if (startX < widthPx / 2f) VerticalGestureSide.BRIGHTNESS else VerticalGestureSide.VOLUME

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

/** 应用内画面比例枚举 → media3 AspectRatioFrameLayout 常量 */
@androidx.annotation.OptIn(UnstableApi::class)
internal fun ResizeModeOption.toMedia3ResizeMode(): Int = when (this) {
    ResizeModeOption.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    ResizeModeOption.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    ResizeModeOption.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
    val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return
    trackSelector.parameters = buildTrackSelectionParameters(trackSelector.parameters, track)
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