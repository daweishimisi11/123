package com.av123.video.ui.lock

import com.av123.video.ui.icons.AppIcons
import com.av123.video.ui.util.rememberAppHaptics

import com.av123.video.ui.theme.AppShapes

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.av123.video.R
import com.av123.video.ui.components.BrandLogo
import com.av123.video.ui.util.findActivity

/** 应用锁允许的认证方式：强生物识别，失败可回退设备凭据（PIN/图案/密码） */
private const val LOCK_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * 设备是否具备应用锁认证条件（已录入强生物识别或设置了屏幕锁）。
 * 开启应用锁开关前必须先过此判定，避免开启后无法解锁。
 */
fun canAuthenticateAppLock(context: android.content.Context): Boolean =
    BiometricManager.from(context).canAuthenticate(LOCK_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

/**
 * 拉起系统生物识别 / 设备凭据验证。回调均在主线程：
 * - 成功：[onSuccess]；
 * - 错误（取消、负向按钮、锁定等终态）：[onError]；单次指纹失败由系统继续提示，不上抛。
 *
 * 注意：同时声明 DEVICE_CREDENTIAL 时禁止 setNegativeButtonText（系统会抛异常），
 * 取消/改用密码由系统弹窗自身承载。
 */
internal fun authenticateAppLock(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (Int, String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errorCode, errString.toString())
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(LOCK_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}

internal enum class AppLockToggleAction {
    REQUIRE_AUTH,
    DISABLE_DIRECT,
    SHOW_UNAVAILABLE
}

/** 切换应用锁开关前的决策：开启/关闭均先走系统验证；无凭据时开启提示不可用、关闭允许兜底解除 */
internal fun resolveAppLockToggleAction(
    targetEnabled: Boolean,
    canAuthenticate: Boolean
): AppLockToggleAction = when {
    canAuthenticate -> AppLockToggleAction.REQUIRE_AUTH
    targetEnabled -> AppLockToggleAction.SHOW_UNAVAILABLE
    else -> AppLockToggleAction.DISABLE_DIRECT
}

/** 计算离开后台时长是否达到重新锁定阈值 */
internal fun shouldRelockAfterBackground(awayMs: Long, relockAfterMs: Long): Boolean =
    relockAfterMs <= 0L || awayMs >= relockAfterMs

/** 进程内应用锁状态；供播放器等子组件感知锁定态并暂停恢复，避免锁屏下误续播 */
internal val LocalAppLockState = staticCompositionLocalOf<AppLockState?> { null }

/**
 * 进程内应用锁状态容器：
 * - [unlocked] 控制锁屏遮罩显隐；
 * - [hasUnlockedOnce] 记录本次进程是否已解锁进入过内容区：超时重新锁定时保持底层内容树挂载，
 *   解锁后可直接回到锁定前的页面与滚动/播放位置，不会重建 NavHost 退回首页。
 */
@Stable
class AppLockState internal constructor(
    initialUnlocked: Boolean,
    relockAfterMs: Long
) {
    var unlocked by mutableStateOf(initialUnlocked)
        internal set

    var hasUnlockedOnce by mutableStateOf(initialUnlocked)
        internal set

    internal var backgroundedAt: Long = 0L
    internal var relockAfterMs: Long = relockAfterMs

    fun markBackgrounded(nowMs: Long = SystemClock.elapsedRealtime()) {
        if (unlocked) {
            backgroundedAt = nowMs
        }
    }

    /** 回到前台时评估是否应重新上锁；返回 true 表示当前处于锁定态需要弹出验证 */
    fun evaluateOnResume(nowMs: Long = SystemClock.elapsedRealtime()): Boolean {
        if (!unlocked) return true
        if (backgroundedAt > 0L) {
            val awayMs = nowMs - backgroundedAt
            backgroundedAt = 0L
            if (shouldRelockAfterBackground(awayMs, relockAfterMs)) {
                unlocked = false
                return true
            }
        }
        return false
    }

    /** 供子组件在 ON_RESUME 同帧内同步判断是否已锁定或即将触发超时重锁 */
    fun isLockedOrPendingRelock(nowMs: Long = SystemClock.elapsedRealtime()): Boolean {
        if (!unlocked) return true
        val bg = backgroundedAt
        return bg > 0L && shouldRelockAfterBackground(nowMs - bg, relockAfterMs)
    }

    fun unlock() {
        unlocked = true
        hasUnlockedOnce = true
        backgroundedAt = 0L
    }
}

/**
 * 应用锁门禁：
 * - 响应运行时 [enabled] 开关变化：运行期刚在设置页开启时不二次弹窗，离开超时后即生效；
 * - 离开超过 [relockAfterMs] 后回前台重新验证（0 表示立即）；
 * - 冷启动未首次解锁前不组合 [content]，超时重新锁定时保留已组合的导航树并在上方覆盖全屏锁屏遮罩，
 *   拦截触摸与返回键，指纹解锁后直接回到锁定前所在页面；
 * - 取消/报错切到后台，回前台自动重弹。
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    relockAfterMs: Long,
    lockState: AppLockState = remember {
        AppLockState(initialUnlocked = !enabled, relockAfterMs = relockAfterMs)
    },
    content: @Composable () -> Unit
) {
    // 曾处于关闭状态说明是运行期刚通过设置页认证开启，本次进程保持已解锁直到下次切后台超时
    var unlockedByRuntimeEnable by remember { mutableStateOf(false) }
    lockState.relockAfterMs = relockAfterMs

    if (!enabled) {
        unlockedByRuntimeEnable = true
        lockState.unlock()
        CompositionLocalProvider(LocalAppLockState provides null) {
            content()
        }
        return
    }
    if (unlockedByRuntimeEnable && !lockState.hasUnlockedOnce) {
        lockState.unlock()
    }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    // 认证触发计数：进入即 1（首次 ON_RESUME 可能已过），之后每次需要验证时 +1
    var promptTick by remember { mutableIntStateOf(1) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lockState) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // 画中画仍属于前台可见播放，不算离开应用
                    if (activity?.isInPictureInPictureMode != true) {
                        lockState.markBackgrounded()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (lockState.evaluateOnResume()) {
                        promptTick++
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val title = stringResource(R.string.app_lock_prompt_title)
    val subtitle = stringResource(R.string.app_lock_unlock)
    // 计数变化（进入、超时回前台）自动弹系统验证；已解锁时不弹
    androidx.compose.runtime.LaunchedEffect(promptTick, activity, lockState.unlocked) {
        if (lockState.unlocked) return@LaunchedEffect
        val act = activity ?: return@LaunchedEffect
        authenticateAppLock(
            activity = act,
            title = title,
            subtitle = subtitle,
            onSuccess = { lockState.unlock() },
            onError = { _, _ ->
                // 取消/报错：切到后台，避免停留在未验证界面；回前台自动重弹
                act.moveTaskToBack(true)
            }
        )
    }

    CompositionLocalProvider(LocalAppLockState provides lockState) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (lockState.hasUnlockedOnce) {
                content()
            }
            if (!lockState.unlocked) {
                // 超时重锁后拦截系统返回键，避免误把底层导航栈弹出
                BackHandler(enabled = lockState.hasUnlockedOnce) {
                    activity?.moveTaskToBack(true)
                }
                AppLockOverlay(
                    onUnlockClick = {
                        val act = activity ?: return@AppLockOverlay
                        authenticateAppLock(
                            activity = act,
                            title = title,
                            subtitle = subtitle,
                            onSuccess = { lockState.unlock() },
                            onError = { _, _ -> act.moveTaskToBack(true) }
                        )
                    },
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                // Main 阶段发生在子按钮之后，只消费未被按钮处理的触控事件，防止穿透到底层页面
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                )
            }
        }
    }
}

/** 品牌锁屏：渐变图标 + 标题 + 手动解锁按钮（系统弹窗未出现/被滑掉时兜底触发） */
@Composable
private fun AppLockOverlay(
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandLogo(style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(AppShapes.Hero)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_lock_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_lock_unlock),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            val haptics = rememberAppHaptics()
            Button(
                onClick = {
                    haptics.confirm()
                    onUnlockClick()
                }
            ) {
                Text(stringResource(R.string.app_lock_unlock_button))
            }
        }
    }
}
