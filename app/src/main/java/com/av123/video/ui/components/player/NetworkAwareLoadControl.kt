package com.av123.video.ui.components.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import com.av123.video.data.prefs.VideoBufferSize
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/** 用于区分视频播放缓冲策略的网络大类 */
enum class BufferNetworkCategory {
    WIFI,
    CELLULAR
}

/**
 * 将底层网络能力归一化为缓冲策略分类（Wi-Fi / 数据流量）。
 * - 显式 Wi-Fi 传输优先归入 [BufferNetworkCategory.WIFI]（即使同时挂载 VPN）；
 * - 蜂窝传输归入 [BufferNetworkCategory.CELLULAR]；
 * - 有线以太网归入 [BufferNetworkCategory.WIFI]；
 * - 其余（如未暴露底层传输的纯 VPN）按是否计费流量兜底区分。
 */
internal fun resolveBufferNetworkCategory(
    hasWifi: Boolean,
    hasEthernet: Boolean,
    hasCellular: Boolean,
    isMetered: Boolean
): BufferNetworkCategory = when {
    hasWifi -> BufferNetworkCategory.WIFI
    hasCellular -> BufferNetworkCategory.CELLULAR
    hasEthernet -> BufferNetworkCategory.WIFI
    isMetered -> BufferNetworkCategory.CELLULAR
    else -> BufferNetworkCategory.WIFI
}

internal fun readBufferNetworkCategory(context: Context): BufferNetworkCategory {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return BufferNetworkCategory.WIFI
    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    return resolveBufferNetworkCategory(
        hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
        hasEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true,
        hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true,
        isMetered = cm.isActiveNetworkMetered
    )
}

internal fun selectActiveBufferSize(
    category: BufferNetworkCategory,
    wifiBufferSize: VideoBufferSize,
    cellularBufferSize: VideoBufferSize
): VideoBufferSize = when (category) {
    BufferNetworkCategory.WIFI -> wifiBufferSize
    BufferNetworkCategory.CELLULAR -> cellularBufferSize
}

/**
 * 与播放框架解耦的缓冲滞回评估器，支持按当前网络/设置动态切换目标档位并便于纯 JVM 单测。
 */
internal class BufferLoadingEvaluator {

    private val loadingStates = ConcurrentHashMap<Any, Boolean>()

    fun reset(playerKey: Any) {
        loadingStates[playerKey] = false
    }

    fun remove(playerKey: Any) {
        loadingStates.remove(playerKey)
    }

    fun shouldContinueLoading(
        playerKey: Any,
        bufferedDurationUs: Long,
        playbackSpeed: Float,
        bufferSize: VideoBufferSize,
        delegateAllowsContinue: Boolean
    ): Boolean {
        val rawMinUs = bufferSize.minBufferMs * 1_000L
        val maxUs = bufferSize.maxBufferMs * 1_000L
        val scaledMinUs = if (playbackSpeed > 1f) {
            minOf((rawMinUs * playbackSpeed.toDouble()).roundToLong(), maxUs)
        } else {
            rawMinUs
        }
        val effectiveMinUs = maxOf(MIN_EFFECTIVE_BUFFER_US, scaledMinUs)

        val previous = loadingStates[playerKey] ?: false
        val next = when {
            bufferedDurationUs < effectiveMinUs -> delegateAllowsContinue
            bufferedDurationUs >= maxUs -> false
            else -> previous && delegateAllowsContinue
        }
        loadingStates[playerKey] = next
        return next
    }

    fun shouldStartPlayback(
        bufferedDurationUs: Long,
        playbackSpeed: Float,
        rebuffering: Boolean,
        targetLiveOffsetUs: Long,
        bufferSize: VideoBufferSize
    ): Boolean {
        val baseMs = if (rebuffering) {
            bufferSize.bufferForPlaybackAfterRebufferMs
        } else {
            bufferSize.bufferForPlaybackMs
        }
        val baseUs = baseMs * 1_000L
        var requiredUs = if (playbackSpeed > 1f) {
            (baseUs * playbackSpeed.toDouble()).roundToLong()
        } else {
            baseUs
        }
        if (targetLiveOffsetUs != C.TIME_UNSET) {
            requiredUs = minOf(requiredUs, targetLiveOffsetUs / 2L)
        }
        return requiredUs <= 0L || bufferedDurationUs >= requiredUs
    }

    private companion object {
        const val MIN_EFFECTIVE_BUFFER_US = 500_000L
    }
}

/**
 * 支持 Wi-Fi / 数据流量及用户设置即时切换的 [LoadControl]。
 * 底层复用单个 [DefaultLoadControl] 管理内存分配器与 OOM 保护，上层动态执行当前生效档位的缓冲窗口。
 */
@UnstableApi
internal class NetworkAwareLoadControl(
    private val bufferSizeProvider: () -> VideoBufferSize
) : LoadControl {

    private val evaluator = BufferLoadingEvaluator()
    private val delegate = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ VideoBufferSize.ULTRA.minBufferMs,
            /* maxBufferMs = */ VideoBufferSize.ULTRA.maxBufferMs,
            /* bufferForPlaybackMs = */ VideoBufferSize.SMALL.bufferForPlaybackMs,
            /* bufferForPlaybackAfterRebufferMs = */ VideoBufferSize.SMALL.bufferForPlaybackAfterRebufferMs
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    override fun onPrepared(playerId: PlayerId) {
        evaluator.reset(playerId)
        delegate.onPrepared(playerId)
    }

    override fun onTracksSelected(
        parameters: LoadControl.Parameters,
        trackGroups: TrackGroupArray,
        trackSelections: Array<out ExoTrackSelection?>
    ) {
        delegate.onTracksSelected(parameters, trackGroups, trackSelections)
    }

    override fun onStopped(playerId: PlayerId) {
        evaluator.remove(playerId)
        delegate.onStopped(playerId)
    }

    override fun onReleased(playerId: PlayerId) {
        evaluator.remove(playerId)
        delegate.onReleased(playerId)
    }

    override fun getAllocator(playerId: PlayerId): Allocator = delegate.getAllocator(playerId)

    override fun getBackBufferDurationUs(playerId: PlayerId): Long =
        delegate.getBackBufferDurationUs(playerId)

    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean =
        delegate.retainBackBufferFromKeyframe(playerId)

    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean {
        val delegateAllows = delegate.shouldContinueLoading(parameters)
        return evaluator.shouldContinueLoading(
            playerKey = parameters.playerId,
            bufferedDurationUs = parameters.bufferedDurationUs,
            playbackSpeed = parameters.playbackSpeed,
            bufferSize = bufferSizeProvider(),
            delegateAllowsContinue = delegateAllows
        )
    }

    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean {
        if (!delegate.shouldStartPlayback(parameters)) return false
        return evaluator.shouldStartPlayback(
            bufferedDurationUs = parameters.bufferedDurationUs,
            playbackSpeed = parameters.playbackSpeed,
            rebuffering = parameters.rebuffering,
            targetLiveOffsetUs = parameters.targetLiveOffsetUs,
            bufferSize = bufferSizeProvider()
        )
    }

    override fun shouldContinuePreloading(
        playerId: PlayerId,
        timeline: Timeline,
        mediaPeriodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long
    ): Boolean = delegate.shouldContinuePreloading(playerId, timeline, mediaPeriodId, bufferedDurationUs)
}
