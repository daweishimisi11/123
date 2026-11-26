package com.av123.video.ui.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.av123.video.R
import com.av123.video.data.repository.FavoriteGroup
import com.av123.video.data.repository.GroupEditResult
import com.av123.video.data.repository.MAX_FAVORITE_GROUPS
import com.av123.video.data.repository.MAX_FAVORITE_GROUP_NAME_LENGTH
import com.av123.video.ui.icons.AppIcons
import com.av123.video.ui.theme.AppShapes
import com.av123.video.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch

/**
 * 详情页「收藏到」弹层：
 * - 支持未收藏时选择收藏夹收藏，也支持已收藏时原地切换所属收藏夹或取消收藏；
 * - 未分组与多收藏夹自由切换，点「完成」统一落库。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritePickerSheet(
    groups: List<FavoriteGroup>,
    initialGroupIds: Set<Long>,
    isFavorite: Boolean,
    createGroup: suspend (String) -> GroupEditResult,
    onConfirm: (Set<Long>) -> Unit,
    onRemoveFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    var selectedGroupIds by remember(initialGroupIds) { mutableStateOf(initialGroupIds) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val duplicateText = stringResource(R.string.favorites_group_duplicate)
    val limitText = stringResource(
        R.string.favorites_group_limit_reached,
        MAX_FAVORITE_GROUPS
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_favorite_picker_title),
                style = MaterialTheme.typography.titleMedium
            )

            GroupOptionRow(
                icon = AppIcons.Inbox,
                label = stringResource(R.string.detail_favorite_only),
                selected = selectedGroupIds.isEmpty(),
                onClick = {
                    haptics.tick()
                    selectedGroupIds = emptySet()
                }
            )

            // 分组多时在面板内部滚动，避免 ModalBottomSheet 被 20 行撑出屏幕
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题行：左侧说明，右侧「新建收藏夹」
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            if (groups.isEmpty()) R.string.detail_favorite_no_groups_short
                            else R.string.detail_favorite_pick_group
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!creating && groups.size < MAX_FAVORITE_GROUPS) {
                        TextButton(
                            onClick = {
                                haptics.tick()
                                creating = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.favorites_new_group),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                // 新建项固定在分组列表顶部
                if (creating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it.take(MAX_FAVORITE_GROUP_NAME_LENGTH)
                                errorText = null
                            },
                            singleLine = true,
                            isError = errorText != null,
                            supportingText = errorText?.let { message ->
                                { Text(message) }
                            },
                            label = { Text(stringResource(R.string.favorites_group_name_hint)) },
                            modifier = Modifier.weight(1f)
                        )
                        val canSubmit = newName.isNotBlank() && !submitting
                        IconButton(
                            enabled = canSubmit,
                            onClick = {
                                submitting = true
                                scope.launch {
                                    when (val result = createGroup(newName.trim())) {
                                        is GroupEditResult.Success -> {
                                            selectedGroupIds = selectedGroupIds + result.group.id
                                            creating = false
                                            newName = ""
                                            errorText = null
                                        }
                                        GroupEditResult.DuplicateName -> errorText = duplicateText
                                        GroupEditResult.LimitReached -> errorText = limitText
                                    }
                                    submitting = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = stringResource(R.string.favorites_dialog_done),
                                tint = if (canSubmit) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                }
                            )
                        }
                        IconButton(onClick = {
                            creating = false
                            newName = ""
                            errorText = null
                        }) {
                            Icon(AppIcons.Close, contentDescription = null)
                        }
                    }
                }

                groups.forEach { group ->
                    val inGroup = group.id in selectedGroupIds
                    GroupOptionRow(
                        icon = AppIcons.Bookmarks,
                        label = group.name,
                        selected = inGroup,
                        onClick = {
                            haptics.tick()
                            selectedGroupIds = if (inGroup) {
                                selectedGroupIds - group.id
                            } else {
                                selectedGroupIds + group.id
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isFavorite) {
                    OutlinedButton(
                        onClick = {
                            haptics.confirm()
                            onRemoveFavorite()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.detail_favorite_remove_action))
                    }
                }
                Button(
                    onClick = {
                        haptics.confirm()
                        onConfirm(selectedGroupIds)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.detail_favorite_done))
                }
            }
        }
    }
}

/** 单选行：左侧语义图标常显，选中只由右侧一个 Check 表示 */
@Composable
private fun GroupOptionRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.Medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        if (selected) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
