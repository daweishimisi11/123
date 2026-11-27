@file:OptIn(ExperimentalMaterial3Api::class)
package com.av123.video.ui.components.player

import com.av123.video.ui.icons.AppIcons

import com.av123.video.ui.theme.AppShapes
import com.av123.video.ui.theme.AppTypeScale

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.av123.video.MainActivity
import com.av123.video.R
import com.av123.video.data.model.Episode
import com.av123.video.data.prefs.DEFAULT_PLAYBACK_SPEEDS
import com.av123.video.data.prefs.formatPlaybackSpeed
import com.av123.video.ui.theme.Motion
import com.av123.video.ui.util.AppLog
import com.av123.video.ui.util.findActivity
import com.av123.video.ui.util.formatDuration
import com.av123.video.ui.util.rememberAppHaptics
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ControlCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerProgressSlider(
    position: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    onDragStateChange: (Boolean) -> Unit = {},
    abLoopState: AbLoopState = AbLoopState()
) {
    val safeDuration = duration.coerceAtLeast(0L)
    val sliderPosition = remember(position, safeDuration) {
        if (safeDuration > 0) position.toFloat() / safeDuration.toFloat() else 0f
    }
    var draggingValue by remember { mutableFloatStateOf(-1f) }
    val isDragging = draggingValue >= 0f
    val currentValue = if (isDragging) draggingValue else sliderPosition
    val interactionSource = remember { MutableInteractionSource() }

    // 拖动状态上抛：父层据此暂停控制层自动隐藏计时
    LaunchedEffect(isDragging) { onDragStateChange(isDragging) }

    // 气泡与进度条之间的间距、小圆点半径（与 thumbSize 14dp 对应）
    val bubbleGap = 6.dp
    val thumbRadius = 7.dp

    // 自定义 Layout：Slider 正常占位测量，时间气泡按自然尺寸测量后浮于小圆点上方，
    // 水平跟随圆点中心，靠近两端时夹紧在进度条内，垂直溢出不影响布局且默认不裁剪
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                Slider(
                    value = currentValue,
                    onValueChange = { draggingValue = it },
                    interactionSource = interactionSource,
                    onValueChangeFinished = {
                        if (safeDuration > 0) {
                            onSeek((draggingValue * safeDuration).toLong())
                        }
                        draggingValue = -1f
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                    ),
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            thumbSize = DpSize(14.dp, 14.dp),
                            colors = SliderDefaults.colors(thumbColor = Color.White)
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(4.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (safeDuration > 0L &&
                    (abLoopState.pointAMs != null || abLoopState.pointBMs != null)
                ) {
                    val markerColor = Color(0xFFFFD54F)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = thumbRadius.toPx()
                        val trackW = (size.width - r * 2f).coerceAtLeast(1f)
                        val centerY = size.height / 2f
                        val aFrac = abLoopState.pointAMs?.let {
                            (it.toFloat() / safeDuration).coerceIn(0f, 1f)
                        }
                        val bFrac = abLoopState.pointBMs?.let {
                            (it.toFloat() / safeDuration).coerceIn(0f, 1f)
                        }
                        if (aFrac != null && bFrac != null && bFrac > aFrac) {
                            val aX = r + aFrac * trackW
                            val bX = r + bFrac * trackW
                            drawRoundRect(
                                color = markerColor.copy(alpha = 0.72f),
                                topLeft = Offset(aX, centerY - 2.dp.toPx()),
                                size = Size((bX - aX).coerceAtLeast(2.dp.toPx()), 4.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                        val tickW = 2.5.dp.toPx()
                        val tickH = 10.dp.toPx()
                        aFrac?.let { frac ->
                            val x = r + frac * trackW - tickW / 2f
                            drawRoundRect(
                                color = markerColor,
                                topLeft = Offset(x, centerY - tickH / 2f),
                                size = Size(tickW, tickH),
                                cornerRadius = CornerRadius(tickW / 2f)
                            )
                        }
                        bFrac?.let { frac ->
                            val x = r + frac * trackW - tickW / 2f
                            drawRoundRect(
                                color = markerColor,
                                topLeft = Offset(x, centerY - tickH / 2f),
                                size = Size(tickW, tickH),
                                cornerRadius = CornerRadius(tickW / 2f)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isDragging,
                enter = fadeIn(Motion.Overlay),
                exit = fadeOut(Motion.Overlay)
            ) {
                val previewPositionMs =
                    if (safeDuration > 0) (currentValue * safeDuration).toLong() else 0L
                SeekTimeBubble(timeText = formatDuration(previewPositionMs))
            }
        },
        measurePolicy = { measurables, constraints ->
            val sliderPlaceable = measurables[0].measure(constraints)
            val bubblePlaceable = measurables.getOrNull(1)?.measure(Constraints())
            layout(sliderPlaceable.width, sliderPlaceable.height) {
                sliderPlaceable.placeRelative(0, 0)
                bubblePlaceable?.let { bubble ->
                    if (bubble.width > 0 && bubble.height > 0) {
                        // Material3 Slider 轨道两侧内缩一个圆点半径，圆心 = radius + fraction * (w - 2r)
                        val radiusPx = thumbRadius.roundToPx()
                        val centerX = radiusPx +
                            currentValue.coerceIn(0f, 1f) * (sliderPlaceable.width - radiusPx * 2)
                        val maxBubbleX = (sliderPlaceable.width - bubble.width).coerceAtLeast(0)
                        val bubbleX = (centerX.roundToInt() - bubble.width / 2)
                            .coerceIn(0, maxBubbleX)
                        val bubbleY = -bubble.height - bubbleGap.roundToPx()
                        bubble.placeRelative(bubbleX, bubbleY)
                    }
                }
            }
        }
    )
}

/** 底部控制栏统一胶囊按钮：激活态（弹出对应选项卡时）高亮描边与底色 */
@Composable
internal fun PlayerControlPillButton(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = Dp.Unspecified
) {
    val shape = CircleShape
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .then(if (maxWidth != Dp.Unspecified) Modifier.widthIn(max = maxWidth) else Modifier)
            .height(30.dp)
            .clip(shape)
            .background(
                if (active) primary.copy(alpha = 0.30f)
                else Color.White.copy(alpha = 0.14f)
            )
            .border(
                width = if (active) 1.2.dp else 0.8.dp,
                color = if (active) primary else Color.White.copy(alpha = 0.18f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) Color.White else Color.White.copy(alpha = 0.94f),
            fontSize = AppTypeScale.Label,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SpeedPopupMenu(
    expanded: Boolean,
    selected: Float,
    speedOptions: List<Float> = DEFAULT_PLAYBACK_SPEEDS,
    onSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val options = speedOptions.ifEmpty { DEFAULT_PLAYBACK_SPEEDS }
    AboveAnchorPopup(expanded = expanded, onDismiss = onDismiss) {
        PlayerFloatingOptionCard(
            title = stringResource(R.string.player_speed_title),
            badge = "${formatPlaybackSpeed(selected)}x",
            minWidth = 112.dp,
            maxWidth = 144.dp
        ) {
            options.forEach { speed ->
                PlayerOptionTile(
                    label = "${formatPlaybackSpeed(speed)}x",
                    selected = kotlin.math.abs(speed - selected) < 0.001f,
                    onClick = { onSelected(speed) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun EpisodePopupMenu(
    expanded: Boolean,
    episodes: List<Episode>,
    selectedEpisodeIndex: Int,
    onSelectEpisode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AboveAnchorPopup(expanded = expanded, onDismiss = onDismiss) {
        PlayerFloatingOptionCard(
            title = stringResource(R.string.player_episodes),
            badge = stringResource(R.string.player_episodes_count, episodes.size),
            minWidth = 124.dp,
            maxWidth = 176.dp
        ) {
            episodes.forEachIndexed { index, episode ->
                val label = episode.name.ifBlank {
                    stringResource(R.string.detail_episode_n, index + 1)
                }
                PlayerOptionTile(
                    label = label,
                    selected = index == selectedEpisodeIndex,
                    onClick = { onSelectEpisode(index) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun TrackPopupMenu(
    expanded: Boolean,
    tracks: List<VideoTrack>,
    selected: VideoTrack?,
    activeHeight: Int = 0,
    onSelected: (VideoTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    val autoLabel = resolveAutoTrackMenuLabel(
        autoLabel = stringResource(R.string.player_track_auto),
        activeHeight = activeHeight
    )
    AboveAnchorPopup(expanded = expanded, onDismiss = onDismiss) {
        PlayerFloatingOptionCard(
            title = stringResource(R.string.player_track_title),
            minWidth = 116.dp,
            maxWidth = 168.dp
        ) {
            PlayerOptionTile(
                label = autoLabel,
                selected = selected == null,
                onClick = { onSelected(null) },
                modifier = Modifier.fillMaxWidth()
            )
            tracks.forEach { track ->
                PlayerOptionTile(
                    label = track.label,
                    selected = track == selected,
                    onClick = { onSelected(track) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun AbRepeatPopupMenu(
    expanded: Boolean,
    abLoopState: AbLoopState,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val unsetLabel = stringResource(R.string.player_ab_unset)
    val badge = when {
        abLoopState.isActive -> stringResource(R.string.player_ab_badge_active)
        abLoopState.pointAMs != null -> stringResource(R.string.player_ab_badge_a)
        else -> null
    }
    AboveAnchorPopup(expanded = expanded, onDismiss = onDismiss) {
        PlayerFloatingOptionCard(
            title = stringResource(R.string.player_ab_title),
            badge = badge,
            minWidth = 152.dp,
            maxWidth = 192.dp
        ) {
            PlayerOptionTile(
                label = stringResource(R.string.player_ab_set_a),
                trailingText = abLoopState.pointAMs?.let { formatDuration(it) } ?: unsetLabel,
                selected = abLoopState.pointAMs != null,
                onClick = onSetPointA,
                modifier = Modifier.fillMaxWidth()
            )
            PlayerOptionTile(
                label = stringResource(R.string.player_ab_set_b),
                trailingText = abLoopState.pointBMs?.let { formatDuration(it) } ?: unsetLabel,
                selected = abLoopState.pointBMs != null,
                onClick = onSetPointB,
                modifier = Modifier.fillMaxWidth()
            )
            if (abLoopState.pointAMs != null || abLoopState.pointBMs != null) {
                PlayerOptionTile(
                    label = stringResource(R.string.player_ab_clear),
                    selected = false,
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 固定弹出在锚点按钮正上方、水平居中且不越界的浮层定位器，避免遮挡底部控制栏与按钮本身 */
private class AboveAnchorPopupPositionProvider(
    private val gapPx: Int,
    private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val x = centeredX.coerceIn(marginPx, maxX)
        val y = (anchorBounds.top - popupContentSize.height - gapPx).coerceAtLeast(marginPx)
        return IntOffset(x, y)
    }
}

@Composable
private fun AboveAnchorPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!expanded) return
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        with(density) {
            AboveAnchorPopupPositionProvider(
                gapPx = 10.dp.roundToPx(),
                marginPx = 12.dp.roundToPx()
            )
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        val visibleState = remember {
            MutableTransitionState(false).apply { targetState = true }
        }
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(Motion.Overlay) + scaleIn(Motion.Overlay, initialScale = 0.94f)
        ) {
            content()
        }
    }
}

/** 播放器底部选项卡（倍速 / 选集 / 清晰度 / A-B）共用的深色毛玻璃竖排浮卡容器 */
@Composable
private fun PlayerFloatingOptionCard(
    title: String,
    minWidth: Dp,
    maxWidth: Dp,
    badge: String? = null,
    maxHeight: Dp = 212.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = AppShapes.Large,
        color = Color(0xF015161B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        shadowElevation = 12.dp,
        modifier = Modifier
            .widthIn(min = minWidth, max = maxWidth)
            .heightIn(max = maxHeight)
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = AppTypeScale.Label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = badge,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = AppTypeScale.Caption,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider(
                thickness = 0.8.dp,
                color = Color.White.copy(alpha = 0.09f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                content = content
            )
        }
    }
}

@Composable
private fun PlayerOptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    val haptics = rememberAppHaptics()
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(
                if (selected) primary.copy(alpha = 0.26f)
                else Color.White.copy(alpha = 0.08f)
            )
            .then(
                if (selected) Modifier.border(1.2.dp, primary, shape)
                else Modifier
            )
            .clickable {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailingText != null) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.84f),
            fontSize = AppTypeScale.Label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (trailingText != null) TextAlign.Start else TextAlign.Center
        )
        if (trailingText != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = trailingText,
                color = if (selected) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.56f),
                fontSize = AppTypeScale.Caption,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

/**
 * 当前播放链接（m3u8）弹窗：链接为可点击超链接（拉起浏览器），
 * 支持一键复制，右上角 X 关闭。
 */
@Composable
internal fun PlaybackUrlContent(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500L.milliseconds)
            copied = false
        }
    }

    fun openInBrowser() {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .clip(AppShapes.Large)
            .background(Color.Black.copy(alpha = 0.86f))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.player_menu_url),
                    color = Color.White,
                    fontSize = AppTypeScale.Body,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                PopupCloseButton(onClick = onDismiss)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 超链接样式（浅蓝 + 下划线），点击拉起浏览器播放
            Text(
                text = url,
                color = Color(0xFF8AB4F8),
                fontSize = AppTypeScale.Caption,
                fontFamily = FontFamily.Monospace,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openInBrowser() }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(url))
                        copied = true
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ContentCopy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (copied) stringResource(R.string.player_url_copied)
                        else stringResource(R.string.player_url_copy),
                        color = Color.White,
                        fontSize = AppTypeScale.Label
                    )
                }
            }
        }
    }
}

/** 弹窗右上角通用关闭按钮 */
@Composable
internal fun PopupCloseButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = AppIcons.Close,
            contentDescription = stringResource(R.string.common_close),
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}