package com.HojuNK.essent_all.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.HojuNK.essent_all.EventsViewMode
import com.HojuNK.essent_all.ListSortOption
import com.HojuNK.essent_all.ListGroupOption

@Composable
fun ViewAndSortDialog(
    viewMode: EventsViewMode,
    onViewModeChange: (EventsViewMode) -> Unit,
    sortOption: ListSortOption,
    onSortOptionChange: (ListSortOption) -> Unit,
    sortAscending: Boolean,
    onSortAscendingToggle: () -> Unit,
    groupOption: ListGroupOption? = null,
    onGroupOptionChange: ((ListGroupOption) -> Unit)? = null,
    showRankingOption: Boolean = true,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clickable(enabled = false) {},
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Вид и сортировка",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                    // 1. Отображать в виде
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Отображать в виде:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SelectablePill(
                                text = "Карточки",
                                isSelected = viewMode == EventsViewMode.RECTANGLES,
                                modifier = Modifier.weight(1f),
                                onClick = { onViewModeChange(EventsViewMode.RECTANGLES) }
                            )
                            SelectablePill(
                                text = "Полоски",
                                isSelected = viewMode == EventsViewMode.STRIPES,
                                modifier = Modifier.weight(1f),
                                onClick = { onViewModeChange(EventsViewMode.STRIPES) }
                            )
                        }
                    }

                    // 2. Сортировать по
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Сортировать по:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            IconButton(
                                onClick = onSortAscendingToggle,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Toggle sort direction",
                                    tint = if (sortAscending) Color(0xFFFFD700) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectablePill(
                                text = "Дате создания",
                                isSelected = sortOption == ListSortOption.CREATION_DATE,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onSortOptionChange(ListSortOption.CREATION_DATE) }
                            )
                            SelectablePill(
                                text = "От А до Я",
                                isSelected = sortOption == ListSortOption.ALPHABETICAL,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onSortOptionChange(ListSortOption.ALPHABETICAL) }
                            )
                            if (showRankingOption) {
                                SelectablePill(
                                    text = "По ранжированию",
                                    isSelected = sortOption == ListSortOption.RANKING,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onSortOptionChange(ListSortOption.RANKING) }
                                )
                            }
                        }
                    }

                    // 3. Группы (если поддерживаются)
                    if (groupOption != null && onGroupOptionChange != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Группы:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SelectablePill(
                                    text = "Объединять",
                                    isSelected = groupOption == ListGroupOption.GROUPED,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onGroupOptionChange(ListGroupOption.GROUPED) }
                                )
                                SelectablePill(
                                    text = "Не объединять",
                                    isSelected = groupOption == ListGroupOption.UNGROUPED,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onGroupOptionChange(ListGroupOption.UNGROUPED) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectablePill(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF252525),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
