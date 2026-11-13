package com.HojuNK.essent_all.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.model.AppInfo
import com.HojuNK.essent_all.model.KeyMap
import com.HojuNK.essent_all.model.Macro
import com.HojuNK.essent_all.ui.components.TypewriterTitle
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.model.LocalRadialMenuState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onEditPattern: (Macro) -> Unit,
    onShowAccessibilityError: () -> Unit,
    isSelectionMode: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val radialMenuState = LocalRadialMenuState.current
    var showGlyphPatterns by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showGlyphPatterns,
        label = "MoreScreenTransition"
    ) { displayingGlyphs ->
        if (displayingGlyphs) {
            GlyphPatternsScreen(
                keyMap = keyMap,
                apps = apps,
                onOpenSettings = onOpenSettings,
                onEditPattern = onEditPattern,
                onShowAccessibilityError = onShowAccessibilityError,
                isSelectionMode = isSelectionMode,
                onSelectionModeChange = onSelectionModeChange,
                onBack = { showGlyphPatterns = false }
            )
        } else {
            val configuration = LocalConfiguration.current

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nothingBlur(enabled = radialMenuState != null),
                topBar = {
                    if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                        TopAppBar(
                            title = {
                                TypewriterTitle(targetText = stringResource(R.string.nav_more), animate = false)
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            modifier = Modifier.drawBehind {
                                val blurHeight = 45.dp.toPx()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Black.copy(alpha = 0.5f)),
                                        startY = 0f,
                                        endY = size.height
                                    ),
                                    size = size
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                                        startY = size.height,
                                        endY = size.height + blurHeight
                                    ),
                                    topLeft = Offset(0f, size.height),
                                    size = Size(size.width, blurHeight)
                                )
                            },
                            actions = {
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, null, tint = Color.White)
                                }
                            }
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Плитка Глифы (Glyphs)
                        item {
                            MoreGridTile(
                                title = stringResource(R.string.more_glyphs),
                                icon = Icons.Default.Lightbulb,
                                onClick = { showGlyphPatterns = true }
                            )
                        }

                        // 2. Плитка Буфер обмена (Clipboard)
                        item {
                            MoreGridTile(
                                title = stringResource(R.string.more_clipboard),
                                icon = Icons.Default.Assignment,
                                onClick = { /* Пока ничего не открывает */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreGridTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF161616),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
