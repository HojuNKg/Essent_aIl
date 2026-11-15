package com.HojuNK.essent_all.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.HojuNK.essent_all.ClipboardHistoryManager
import com.HojuNK.essent_all.R
import kotlinx.coroutines.delay

@Composable
fun TestClipboardOverlayContent(
    onDismiss: () -> Unit,
    onPasteItem: (String) -> Unit
) {
    val context = LocalContext.current
    var history by remember {
        mutableStateOf(
            try {
                ClipboardHistoryManager.getHistory(context)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    // Update data every second and sync system clipboard
    LaunchedEffect(Unit) {
        while (true) {
            try {
                ClipboardHistoryManager.syncSystemClipboard(context)
                history = ClipboardHistoryManager.getHistory(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF121212),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with title and close button ('X')
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.action_test_clipboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.clipboard_empty),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        ClipboardHistoryManager.copyToClipboard(context, item.content)
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        onPasteItem(item.content)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = item.content,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 4
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp)),
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            ClipboardHistoryManager.copyToClipboard(context, item.content)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = Color(0xFFFFD700)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
