package com.av123.video.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.av123.video.R
import com.av123.video.data.net.UpdateChecker
import com.av123.video.ui.components.BrandLogo
import com.av123.video.ui.icons.AppIconSize
import com.av123.video.ui.icons.AppIcons
import com.av123.video.ui.theme.AppShapes
import com.av123.video.ui.theme.appCardContainerColor
import com.av123.video.ui.util.rememberAppHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REPO_URL = "https://github.com/daweishimisi11/123"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val RELEASES_URL = "$REPO_URL/releases/latest"
private const val ALL_RELEASES_URL = "$REPO_URL/releases"

/** 版本更新日志条目 */
internal data class ChangelogEntry(
    val version: String,
    val highlights: List<String>
)

internal val APP_CHANGELOG: List<ChangelogEntry> = listOf(
    ChangelogEntry(
        version = "v0.2.6",
        highlights = listOf(
            "新增视频播放缓冲大小调节功能（「我的 → 播放」及全屏设置内可配置）",
            "支持分别为 Wi-Fi 与数据流量设置预缓冲时长（15 秒 / 30 秒 / 60 秒 / 120 秒），播放时按网络自动切换并即时生效"
        )
    ),
    ChangelogEntry(
        version = "v0.2.5",
        highlights = listOf(
            "重设计「我的 → 版本」抽屉比例：改为紧凑横向头部与全展开布局，打开即可同时看到检查更新与全部入口"
        )
    ),
    ChangelogEntry(
        version = "v0.2.4",
        highlights = listOf(
            "全屏左上角缓冲秒数胶囊移至最左侧边缘留白区并精简文字，避免遮挡视频画面",
            "在「我的 → 版本」内新增更新日志入口，随时查看各版本变更记录"
        )
    ),
    ChangelogEntry(
        version = "v0.2.3",
        highlights = listOf(
            "新增全屏左上角常驻显示缓冲健康秒数的开关（「我的 → 播放」及全屏设置内可切换）"
        )
    ),
    ChangelogEntry(
        version = "v0.2.2",
        highlights = listOf(
            "拆分全屏「选集」与「设置」抽屉，点击选集仅打开剧集选择、设置内移除选集项",
            "清晰度处于「自动」档时直接显示当前播放分辨率（如 480p / 720p / 1080p）"
        )
    ),
    ChangelogEntry(
        version = "v0.2.1",
        highlights = listOf(
            "详情页简介支持直接点击文字区域展开与收起",
            "移除演员详情页姓名右侧重复的心形关注数胶囊，防止遮挡长姓名"
        )
    ),
    ChangelogEntry(
        version = "v0.2.0",
        highlights = listOf(
            "详情页支持有码与无码版本快捷跳转按钮",
            "自适应调小竖屏详情页顶栏番号与下方标题字号",
            "统一清理安装包产物与应用界面中的 AV 标识"
        )
    )
)

/** 检查更新的结果弹窗；[link] 为空表示只有一句结论 */
private data class UpdateDialog(val title: String, val link: String?)

/**
 * 「关于」底部抽屉（和深色模式同一个形态）：
 * 图标 + 版本号 + 检查更新 + 更多链接。不放简介，避免把一块文字塞进抽屉。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty().ifBlank { "0.1.0" }
    }
    var checking by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<UpdateDialog?>(null) }
    var showChangelog by remember { mutableStateOf(false) }

    fun checkUpdate() {
        if (checking) return
        haptics.tick()
        checking = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { UpdateChecker.check() }
            checking = false
            result.onSuccess { release ->
                val latest = release.tag.removePrefix("v")
                dialog = if (UpdateChecker.isNewer(latest, versionName)) {
                    UpdateDialog(
                        title = resources.getString(R.string.about_new_version, latest),
                        link = release.pageUrl.ifBlank { RELEASES_URL }
                    )
                } else {
                    UpdateDialog(
                        title = resources.getString(R.string.about_up_to_date, "v$versionName"),
                        link = null
                    )
                }
            }.onFailure {
                // 失败只给一句结论，不摆「去下载 / 稍后」——那不是一次可下载的结果
                dialog = UpdateDialog(
                    title = resources.getString(R.string.about_check_failed),
                    link = null
                )
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AboutSheetContent(
            versionName = versionName,
            checking = checking,
            onCheckUpdate = { checkUpdate() },
            onOpenChangelog = {
                haptics.tick()
                showChangelog = true
            },
            onOpenProject = {
                haptics.tick()
                uriHandler.openUri(REPO_URL)
            },
            onOpenFeedback = {
                haptics.tick()
                uriHandler.openUri(ISSUES_URL)
            }
        )
    }

    dialog?.let { current ->
        // 自己排的紧凑弹窗：AlertDialog 的标题/按钮下内边距对一句结论来说太空
        Dialog(onDismissRequest = { dialog = null }) {
            Surface(
                shape = AppShapes.ExtraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 12.dp,
                        top = 18.dp,
                        bottom = 6.dp
                    )
                ) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (current.link != null) {
                            TextButton(onClick = { dialog = null }) {
                                Text(stringResource(R.string.about_later))
                            }
                        }
                        TextButton(onClick = {
                            dialog = null
                            current.link?.let { uriHandler.openUri(it) }
                        }) {
                            Text(
                                stringResource(
                                    if (current.link != null) R.string.about_download
                                    else R.string.profile_about_dialog_got_it
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChangelog) {
        ChangelogDialog(
            entries = APP_CHANGELOG,
            onOpenReleases = {
                showChangelog = false
                uriHandler.openUri(ALL_RELEASES_URL)
            },
            onDismiss = { showChangelog = false }
        )
    }
}

@Composable
internal fun ChangelogDialog(
    entries: List<ChangelogEntry>,
    onOpenReleases: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = AppShapes.ExtraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_changelog),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    entries.forEachIndexed { index, entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AboutBadge(text = entry.version, accent = index == 0)
                            entry.highlights.forEach { item ->
                                Text(
                                    text = "• $item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < entries.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onOpenReleases) {
                        Text(stringResource(R.string.about_changelog_releases))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AboutSheetContent(
    versionName: String,
    checking: Boolean,
    onCheckUpdate: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenFeedback: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        // 横向紧凑头部：左侧 56dp 图标 + 中间品牌名与版本号 + 右侧检查更新按钮，
        // 把原本近 300dp 的纵向堆叠压缩为一行，抽屉弹出时所有功能入口一屏完整呈现
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 直接用启动器图标：自适应图标是 XML，Compose 的 painterResource 解不了，交给 ImageView 渲染
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        setImageResource(R.mipmap.ic_launcher)
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    }
                },
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BrandLogo(style = MaterialTheme.typography.titleLarge)
                AboutBadge(
                    text = "v$versionName",
                    accent = true,
                    compact = true
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onCheckUpdate,
                enabled = !checking,
                shape = AppShapes.Pill,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (checking) R.string.about_checking else R.string.about_check_update
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(AppShapes.Large)
                .background(appCardContainerColor())
        ) {
            AboutLinkRow(
                icon = AppIcons.HistoryOutline,
                title = stringResource(R.string.about_changelog),
                subtitle = stringResource(R.string.about_changelog_subtitle),
                onClick = onOpenChangelog
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AboutLinkRow(
                icon = AppIcons.Public,
                title = stringResource(R.string.about_project),
                subtitle = stringResource(R.string.about_project_url),
                onClick = onOpenProject
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 62.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            AboutLinkRow(
                icon = AppIcons.DescriptionOutline,
                title = stringResource(R.string.about_feedback),
                onClick = onOpenFeedback
            )
        }
    }
}

@Composable
private fun AboutBadge(
    text: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(AppShapes.Pill)
            .background(if (accent) scheme.primary else scheme.surfaceVariant)
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 2.dp else 5.dp
            )
    ) {
        Text(
            text = text,
            fontSize = if (compact) 12.sp else 14.sp,
            color = if (accent) scheme.onPrimary else scheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AboutLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShapes.Medium)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(AppIconSize.Inline)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            // 没有地址的行只显示标题（问题反馈就是这样）
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(AppIconSize.Inline)
        )
    }
}
