package com.av123.video.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

/** 支持在设置中勾选的倍速候选列表（升序） */
val AVAILABLE_PLAYBACK_SPEEDS: List<Float> =
    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

/** 默认展示的倍速选项 */
val DEFAULT_PLAYBACK_SPEEDS: List<Float> =
    listOf(0.5f, 1f, 1.25f, 1.5f, 2f, 3f)

/** 格式化倍速数值（2.0 -> "2"，1.25 -> "1.25"） */
fun formatPlaybackSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()

/** 从存储字符串反序列化倍速选项；非法或空值时回退到默认集合 */
fun parsePlaybackSpeedOptions(raw: String?): List<Float> {
    if (raw.isNullOrBlank()) return DEFAULT_PLAYBACK_SPEEDS
    val parsed = raw.split(',')
        .mapNotNull { token ->
            val value = token.trim().toFloatOrNull() ?: return@mapNotNull null
            AVAILABLE_PLAYBACK_SPEEDS.firstOrNull { abs(it - value) < 0.001f }
        }
        .distinct()
        .sorted()
    return parsed.ifEmpty { DEFAULT_PLAYBACK_SPEEDS }
}

/** 将已选倍速选项规范化并序列化为逗号分隔字符串 */
fun serializePlaybackSpeedOptions(speeds: List<Float>): String {
    val normalized = speeds
        .mapNotNull { value ->
            AVAILABLE_PLAYBACK_SPEEDS.firstOrNull { abs(it - value) < 0.001f }
        }
        .distinct()
        .sorted()
        .ifEmpty { DEFAULT_PLAYBACK_SPEEDS }
    return normalized.joinToString(",") { formatPlaybackSpeed(it) }
}

/** 切换某个倍速选项的显示状态；至少保留 1 个选项，避免倍速菜单为空 */
fun togglePlaybackSpeedOption(current: List<Float>, target: Float): List<Float> {
    val canonical = AVAILABLE_PLAYBACK_SPEEDS.firstOrNull { abs(it - target) < 0.001f }
        ?: return current
    val normalizedCurrent = AVAILABLE_PLAYBACK_SPEEDS.filter { candidate ->
        current.any { abs(it - candidate) < 0.001f }
    }
    val exists = normalizedCurrent.any { abs(it - canonical) < 0.001f }
    if (exists) {
        if (normalizedCurrent.size <= 1) return normalizedCurrent
        return normalizedCurrent.filterNot { abs(it - canonical) < 0.001f }
    }
    return (normalizedCurrent + canonical).distinct().sorted()
}

/** 深色模式偏好 */
enum class DarkMode { SYSTEM, LIGHT, DARK }

/** 视频条目布局：双列网格 / 单列一行一条 */
enum class VideoLayout { GRID, LIST }

/** 应用锁离开后多久重新验证 */
enum class AppLockTimeout(val millis: Long) {
    IMMEDIATE(0L),
    THIRTY_SECONDS(30_000L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(300_000L)
}

/** 播放器横向滑动手势快进/快退灵敏度（划满全屏对应的微调跨度，秒） */
enum class SeekGestureSensitivity(val fullSwipeSeconds: Int) {
    FINE(60),
    STANDARD(90),
    FAST(180)
}

/**
 * 视频播放缓冲大小档位（ExoPlayer 向前预缓冲时长阈值）：
 * - [SMALL] 15 秒（省流）：min 10s / max 15s，起播 1.5s / 重缓冲恢复 3s
 * - [STANDARD] 30 秒（适中）：min 20s / max 30s，起播 2s / 重缓冲恢复 4s
 * - [LARGE] 60 秒（畅享）：min 40s / max 60s，起播 2.5s / 重缓冲恢复 5s
 * - [ULTRA] 120 秒（超大）：min 60s / max 120s，起播 2.5s / 重缓冲恢复 5s
 */
enum class VideoBufferSize(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    SMALL(
        minBufferMs = 10_000,
        maxBufferMs = 15_000,
        bufferForPlaybackMs = 1_500,
        bufferForPlaybackAfterRebufferMs = 3_000
    ),
    STANDARD(
        minBufferMs = 20_000,
        maxBufferMs = 30_000,
        bufferForPlaybackMs = 2_000,
        bufferForPlaybackAfterRebufferMs = 4_000
    ),
    LARGE(
        minBufferMs = 40_000,
        maxBufferMs = 60_000,
        bufferForPlaybackMs = 2_500,
        bufferForPlaybackAfterRebufferMs = 5_000
    ),
    ULTRA(
        minBufferMs = 60_000,
        maxBufferMs = 120_000,
        bufferForPlaybackMs = 2_500,
        bufferForPlaybackAfterRebufferMs = 5_000
    );

    val maxSeconds: Int get() = maxBufferMs / 1_000

    companion object {
        val DEFAULT_WIFI = LARGE
        val DEFAULT_CELLULAR = SMALL
    }
}

/** 全局外观与播放设置 */
data class AppSettings(
    val dynamicColor: Boolean = false,
    /** 底部导航改为悬浮胶囊样式；默认保持贴底 NavigationBar */
    val floatingNavBar: Boolean = false,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    /** 全屏播放回桌面时是否自动进入画中画小窗 */
    val pipEnabled: Boolean = true,
    /** 当前选集播放结束后是否自动连播下一集 */
    val autoPlayNext: Boolean = true,
    /** 全屏播放时将手机屏幕朝下，是否停止播放并结束应用 */
    val flipToExitEnabled: Boolean = false,
    /** 视频列表布局：双列网格 / 单列；一处切换，四个网格页同步 */
    val videoLayout: VideoLayout = VideoLayout.GRID,
    /** 冷启动是否需要生物识别/设备凭据解锁 */
    val appLockEnabled: Boolean = false,
    /** 离开应用超过该时长后重新上锁 */
    val appLockTimeout: AppLockTimeout = AppLockTimeout.THIRTY_SECONDS,
    /** 阻止截屏/录屏，并在最近任务中隐藏预览 */
    val secureScreenEnabled: Boolean = true,
    /** 横滑快进/快退手势灵敏度，默认适中（适合几秒到几十秒微调） */
    val seekGestureSensitivity: SeekGestureSensitivity = SeekGestureSensitivity.STANDARD,
    /** 全屏播放时是否在左上角常驻显示缓冲健康秒数 */
    val showBufferHealth: Boolean = false,
    /** Wi-Fi（及有线等非计费网络）下的视频向前预缓冲大小 */
    val wifiBufferSize: VideoBufferSize = VideoBufferSize.DEFAULT_WIFI,
    /** 数据流量（蜂窝/计费网络）下的视频向前预缓冲大小 */
    val cellularBufferSize: VideoBufferSize = VideoBufferSize.DEFAULT_CELLULAR,
    /** 是否在播放器底栏显示倍速按钮 */
    val showSpeedButton: Boolean = true,
    /** 是否在全屏播放器底栏显示选集按钮 */
    val showEpisodeButton: Boolean = true,
    /** 是否在全屏播放器底栏显示清晰度按钮 */
    val showTrackButton: Boolean = true,
    /** 倍速菜单中显示的可选倍速列表 */
    val speedOptions: List<Float> = DEFAULT_PLAYBACK_SPEEDS
)

/**
 * 启动早期同步读取的最小设置快照：只覆盖首帧主题与防截屏。
 * DataStore 是唯一事实源，这里只是给 `attachBaseContext` 用的镜像。
 */
data class StartupSettings(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val secureScreenEnabled: Boolean = true
)

/** 用户偏好仓库（动态取色开关、深色模式、画中画开关） */
class SettingsStore(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.settingsDataStore

    /** 极小的启动镜像：避免 Activity 创建时阻塞读取 DataStore */
    private val startupPrefs =
        appContext.getSharedPreferences(STARTUP_PREFS, Context.MODE_PRIVATE)

    /**
     * 同步读取启动快照（只读 SharedPreferences，不触碰 DataStore），
     * 供 `MainActivity.attachBaseContext` 在 Compose 初始化前确定首帧主题。
     */
    fun readStartupSettings(): StartupSettings {
        val storedMode = startupPrefs.getString(STARTUP_KEY_DARK_MODE, null)
        val darkMode = storedMode
            ?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() }
            ?: DarkMode.SYSTEM
        return StartupSettings(
            darkMode = darkMode,
            secureScreenEnabled = startupPrefs.getBoolean(STARTUP_KEY_SECURE_SCREEN, true)
        )
    }

    /**
     * DataStore 冷流。页面请使用 `AppContainer.appSettingsState`（进程内热 StateFlow），
     * 避免每个页面各自订阅、重复读盘。
     * 每次设置变化顺带刷新启动镜像，供下次冷启动同步读取。
     */
    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: false,
            floatingNavBar = prefs[KEY_FLOATING_NAV_BAR] ?: false,
            darkMode = runCatching {
                DarkMode.valueOf(prefs[KEY_DARK_MODE] ?: DarkMode.SYSTEM.name)
            }.getOrDefault(DarkMode.SYSTEM),
            pipEnabled = prefs[KEY_PIP_ENABLED] ?: true,
            autoPlayNext = prefs[KEY_AUTO_PLAY_NEXT] ?: true,
            flipToExitEnabled = prefs[KEY_FLIP_TO_EXIT] ?: false,
            videoLayout = runCatching {
                VideoLayout.valueOf(prefs[KEY_VIDEO_LAYOUT] ?: VideoLayout.GRID.name)
            }.getOrDefault(VideoLayout.GRID),
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            appLockTimeout = runCatching {
                AppLockTimeout.valueOf(
                    prefs[KEY_APP_LOCK_TIMEOUT] ?: AppLockTimeout.THIRTY_SECONDS.name
                )
            }.getOrDefault(AppLockTimeout.THIRTY_SECONDS),
            secureScreenEnabled = prefs[KEY_SECURE_SCREEN] ?: true,
            seekGestureSensitivity = runCatching {
                SeekGestureSensitivity.valueOf(
                    prefs[KEY_SEEK_GESTURE_SENSITIVITY] ?: SeekGestureSensitivity.STANDARD.name
                )
            }.getOrDefault(SeekGestureSensitivity.STANDARD),
            showBufferHealth = prefs[KEY_SHOW_BUFFER_HEALTH] ?: false,
            wifiBufferSize = runCatching {
                VideoBufferSize.valueOf(
                    prefs[KEY_WIFI_BUFFER_SIZE] ?: VideoBufferSize.DEFAULT_WIFI.name
                )
            }.getOrDefault(VideoBufferSize.DEFAULT_WIFI),
            cellularBufferSize = runCatching {
                VideoBufferSize.valueOf(
                    prefs[KEY_CELLULAR_BUFFER_SIZE] ?: VideoBufferSize.DEFAULT_CELLULAR.name
                )
            }.getOrDefault(VideoBufferSize.DEFAULT_CELLULAR),
            showSpeedButton = prefs[KEY_SHOW_SPEED_BUTTON] ?: true,
            showEpisodeButton = prefs[KEY_SHOW_EPISODE_BUTTON] ?: true,
            showTrackButton = prefs[KEY_SHOW_TRACK_BUTTON] ?: true,
            speedOptions = parsePlaybackSpeedOptions(prefs[KEY_SPEED_OPTIONS])
        )
    }.onEach { writeStartupMirror(it) }

    /** 每次设置变化刷新启动镜像；下次冷启动由 [readStartupSettings] 同步读取 */
    private fun writeStartupMirror(settings: AppSettings) {
        startupPrefs.edit()
            .putString(STARTUP_KEY_DARK_MODE, settings.darkMode.name)
            .putBoolean(STARTUP_KEY_SECURE_SCREEN, settings.secureScreenEnabled)
            .apply()
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setFloatingNavBar(enabled: Boolean) {
        store.edit { it[KEY_FLOATING_NAV_BAR] = enabled }
    }

    suspend fun setDarkMode(mode: DarkMode) {
        store.edit { it[KEY_DARK_MODE] = mode.name }
    }

    suspend fun setPipEnabled(enabled: Boolean) {
        store.edit { it[KEY_PIP_ENABLED] = enabled }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        store.edit { it[KEY_AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setFlipToExitEnabled(enabled: Boolean) {
        store.edit { it[KEY_FLIP_TO_EXIT] = enabled }
    }

    suspend fun setVideoLayout(layout: VideoLayout) {
        store.edit { it[KEY_VIDEO_LAYOUT] = layout.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        store.edit { it[KEY_APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockTimeout(timeout: AppLockTimeout) {
        store.edit { it[KEY_APP_LOCK_TIMEOUT] = timeout.name }
    }

    suspend fun setSecureScreenEnabled(enabled: Boolean) {
        store.edit { it[KEY_SECURE_SCREEN] = enabled }
    }

    suspend fun setSeekGestureSensitivity(sensitivity: SeekGestureSensitivity) {
        store.edit { it[KEY_SEEK_GESTURE_SENSITIVITY] = sensitivity.name }
    }

    suspend fun setShowBufferHealth(enabled: Boolean) {
        store.edit { it[KEY_SHOW_BUFFER_HEALTH] = enabled }
    }

    suspend fun setWifiBufferSize(size: VideoBufferSize) {
        store.edit { it[KEY_WIFI_BUFFER_SIZE] = size.name }
    }

    suspend fun setCellularBufferSize(size: VideoBufferSize) {
        store.edit { it[KEY_CELLULAR_BUFFER_SIZE] = size.name }
    }

    suspend fun setShowSpeedButton(enabled: Boolean) {
        store.edit { it[KEY_SHOW_SPEED_BUTTON] = enabled }
    }

    suspend fun setShowEpisodeButton(enabled: Boolean) {
        store.edit { it[KEY_SHOW_EPISODE_BUTTON] = enabled }
    }

    suspend fun setShowTrackButton(enabled: Boolean) {
        store.edit { it[KEY_SHOW_TRACK_BUTTON] = enabled }
    }

    suspend fun setSpeedOptions(speeds: List<Float>) {
        store.edit { it[KEY_SPEED_OPTIONS] = serializePlaybackSpeedOptions(speeds) }
    }

    private companion object {
        const val STARTUP_PREFS = "startup_settings"
        const val STARTUP_KEY_DARK_MODE = "dark_mode"
        const val STARTUP_KEY_SECURE_SCREEN = "secure_screen"

        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_FLOATING_NAV_BAR = booleanPreferencesKey("floating_nav_bar")
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        val KEY_PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val KEY_AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val KEY_FLIP_TO_EXIT = booleanPreferencesKey("flip_to_exit_enabled")
        val KEY_VIDEO_LAYOUT = stringPreferencesKey("video_layout")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
        val KEY_SECURE_SCREEN = booleanPreferencesKey("secure_screen")
        val KEY_SEEK_GESTURE_SENSITIVITY = stringPreferencesKey("seek_gesture_sensitivity")
        val KEY_SHOW_BUFFER_HEALTH = booleanPreferencesKey("show_buffer_health")
        val KEY_WIFI_BUFFER_SIZE = stringPreferencesKey("wifi_buffer_size")
        val KEY_CELLULAR_BUFFER_SIZE = stringPreferencesKey("cellular_buffer_size")
        val KEY_SHOW_SPEED_BUTTON = booleanPreferencesKey("show_speed_button")
        val KEY_SHOW_EPISODE_BUTTON = booleanPreferencesKey("show_episode_button")
        val KEY_SHOW_TRACK_BUTTON = booleanPreferencesKey("show_track_button")
        val KEY_SPEED_OPTIONS = stringPreferencesKey("playback_speed_options")
    }
}
