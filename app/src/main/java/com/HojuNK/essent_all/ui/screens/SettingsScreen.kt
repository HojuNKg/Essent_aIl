package com.HojuNK.essent_all.ui.screens

import android.app.LocaleManager
import android.content.Intent
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(keyMap: KeyMap, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDraggingSlider by remember { mutableStateOf(false) }
    var previewYOffset by remember { mutableFloatStateOf(keyMap.panelYOffset) }

    LaunchedEffect(keyMap.panelYOffset) {
        if (!isDraggingSlider) previewYOffset = keyMap.panelYOffset
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val json = Config.exportConfig(ctx)
                ctx.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(json.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                val json = ctx.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                if (json != null) {
                    val success = Config.importConfig(ctx, json)
                    if (success) {
                        // Optional: show success toast or notification
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { TypewriterTitle(targetText = stringResource(R.string.settings), animate = false) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
                )
            }
        ) { inner ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { OutlinedButton(onClick = { openAccessibilitySettings(ctx) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.accessibility_settings)) } }
            item { HorizontalDivider() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.app_theme), style = MaterialTheme.typography.titleLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { scope.launch { Config.setTheme(ctx, AppTheme.AUTO) } }) {
                            RadioButton(selected = keyMap.theme == AppTheme.AUTO, onClick = null)
                            Text(stringResource(R.string.theme_auto), modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { scope.launch { Config.setTheme(ctx, AppTheme.LIGHT) } }) {
                            RadioButton(selected = keyMap.theme == AppTheme.LIGHT, onClick = null)
                            Text(stringResource(R.string.theme_light), modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { scope.launch { Config.setTheme(ctx, AppTheme.DARK) } }) {
                            RadioButton(selected = keyMap.theme == AppTheme.DARK, onClick = null)
                            Text(stringResource(R.string.theme_dark), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleLarge)
                    var showLangDialog by remember { mutableStateOf(false) }

                    val localeManager = ctx.getSystemService(LocaleManager::class.java)
                    val currentAppLocales = localeManager.applicationLocales
                    val currentLanguage = when {
                        currentAppLocales.isEmpty -> AppLanguage.SYSTEM
                        currentAppLocales.toLanguageTags().contains("en") -> AppLanguage.ENGLISH
                        currentAppLocales.toLanguageTags().contains("ru") -> AppLanguage.RUSSIAN
                        else -> AppLanguage.SYSTEM
                    }

                    OutlinedButton(
                        onClick = { showLangDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text(currentLanguage.getLabel(ctx))
                    }

                    if (showLangDialog) {
                        LanguageSelectorDialog(
                            current = currentLanguage,
                            onSelect = { lang ->
                                scope.launch {
                                    Config.setLanguage(ctx, lang)
                                    val locales = when (lang) {
                                        AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
                                        AppLanguage.ENGLISH -> LocaleList.forLanguageTags("en")
                                        AppLanguage.RUSSIAN -> LocaleList.forLanguageTags("ru")
                                    }
                                    localeManager.applicationLocales = locales
                                }
                                showLangDialog = false
                            },
                            onDismiss = { showLangDialog = false }
                        )
                    }
                }
            }
            item { HorizontalDivider() }
            item { Text(stringResource(R.string.essential_key_position), style = MaterialTheme.typography.titleLarge) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.side_position), style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (keyMap.panelSide == PanelSide.LEFT) stringResource(R.string.side_left) else stringResource(R.string.side_right))
                        Switch(checked = keyMap.panelSide == PanelSide.RIGHT, onCheckedChange = { isRight ->
                            scope.launch { Config.setPanelConfig(ctx, if (isRight) PanelSide.RIGHT else PanelSide.LEFT, keyMap.panelYOffset) }
                        })
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(R.string.y_offset), style = MaterialTheme.typography.labelMedium)
                        Text("${(previewYOffset * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    Slider(
                        value = previewYOffset,
                        onValueChange = {
                            previewYOffset = it
                            isDraggingSlider = true
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            scope.launch { Config.setPanelConfig(ctx, keyMap.panelSide, previewYOffset) }
                        },
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                }
            }
            item { HorizontalDivider() }
            item { Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleLarge) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("essential_key_config.json") },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export))
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.import_config))
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.confirm_deletion), style = MaterialTheme.typography.titleLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.confirm_deletion_desc), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Switch(checked = keyMap.confirmDeletion, onCheckedChange = { scope.launch { Config.setConfirmDeletion(ctx, it) } })
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Эффекты", style = MaterialTheme.typography.titleLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Отключить визуальные эффекты")
                        Switch(checked = keyMap.disableAnimations, onCheckedChange = { scope.launch { Config.setDisableAnimations(ctx, it) } })
                    }
                    Text("Если включено, экран не будет замораживаться при нажатии Essential Key.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            item { HorizontalDivider() }
            @OptIn(ExperimentalFoundationApi::class)
            item {
                var showTestDialog by remember { mutableStateOf(false) }
                var testedActionId by remember { mutableStateOf<String?>(null) }
                val selectedActionItem = ActionRegistry.allActions.find { isTargetMatch(it, testedActionId) }
                val actionTitle = selectedActionItem?.let { stringResource(it.titleRes) } ?: (testedActionId ?: "Действие не выбрано")

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { showTestDialog = true },
                            onLongClick = {
                                testedActionId?.let { act ->
                                    val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                        setAction("action:test_execute")
                                        putExtra("action_target", act)
                                    }
                                    ctx.startService(intent)
                                }
                            }
                        ),
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🧪 Отладочная тест-кнопка", color = Color(0xFFFFD700), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Выбрано: $actionTitle", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("Тап: выбрать. Зажатие: выполнить", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.Settings, null, tint = Color(0xFFFFD700))
                    }
                }

                if (showTestDialog) {
                    Dialog(
                        onDismissRequest = { showTestDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF121212),
                            modifier = Modifier
                                .fillMaxWidth(0.96f)
                                .fillMaxHeight(0.85f)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Отладочная тест-кнопка", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Spacer(Modifier.height(12.dp))
                                UniversalActionListContent(
                                    keyMap = keyMap,
                                    apps = null,
                                    currentActionId = testedActionId,
                                    slot = null,
                                    showDefaultOption = true,
                                    autoConfirmSimpleSelection = false,
                                    onActionSelected = { pkg ->
                                        testedActionId = pkg
                                        if (pkg != null) {
                                            ActionHistory.addAction(ctx, pkg)
                                        }
                                        showTestDialog = false
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { showTestDialog = false }, modifier = Modifier.align(Alignment.End)) {
                                    Text("Готово", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        } }

        if (isDraggingSlider) {
            val isRight = keyMap.panelSide == PanelSide.RIGHT
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * previewYOffset
                val barWidth = 120f
                val barHeight = 8f
                val x = if (isRight) size.width - barWidth else 0f

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(x, y - barHeight / 2),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
fun CustomActionOption(label: String, type: CustomActionType, current: CustomActionType, onSelect: (CustomActionType) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(type) }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = type == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color.White)); Spacer(Modifier.width(8.dp)); Text(label)
    }
}

@Composable
fun LanguageSelectorDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.app_language), style = MaterialTheme.typography.headlineSmall)
                AppLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(lang) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = lang == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(lang.getLabel(LocalContext.current))
                    }
                }
            }
        }
    }
}
