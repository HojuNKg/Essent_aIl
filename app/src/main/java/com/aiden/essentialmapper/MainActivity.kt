package com.aiden.essentialmapper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*

data class AppInfo(val pkg: String, val label: String, val icon: ImageBitmap)

@Composable
fun TypewriterTitle(baseText: String = "Essent.aI/", targetText: String = "Not essential space") {
    var text by remember { mutableStateOf(baseText) }
    
    LaunchedEffect(Unit) {
        delay(2000) // Пауза перед началом анимации
        
        // Удаляем по буквам
        while (text.isNotEmpty()) {
            delay(100)
            text = text.dropLast(1)
        }
        
        delay(300)
        
        // Печатаем новый текст
        for (char in targetText) {
            delay(100)
            text += char
        }
    }
    
    Text(text)
}

private suspend fun loadLaunchableApps(ctx: Context): List<AppInfo> =
    withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, 0)
            .map { ri ->
                AppInfo(
                    pkg = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap(),
                )
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

private fun openAccessibilitySettings(ctx: Context) {
    ctx.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { 
            val ctx = LocalContext.current
            val keyMap by Config.flow(ctx).collectAsState(initial = KeyMap())
            var showSettings by remember { mutableStateOf(false) }
            
            val useDark = when (keyMap.theme) {
                AppTheme.AUTO -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            val colorScheme = if (useDark) {
                darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    background = Color.Black,
                    onBackground = Color.White,
                )
            } else {
                lightColorScheme(
                    primary = Color.Black,
                    onPrimary = Color.White,
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                if (showSettings) {
                    SettingsScreen(keyMap, onBack = { showSettings = false })
                } else {
                    ConfigScreen(keyMap, onOpenSettings = { showSettings = true })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(keyMap: KeyMap, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) } // null while loading
    var picker by remember { mutableStateOf<TapSlot?>(null) }

    LaunchedEffect(Unit) {
        Config.seedDefaultsIfEmpty(ctx)
        apps = loadLaunchableApps(ctx)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TypewriterTitle() },
                actions = {
                    val nextTheme = when (keyMap.theme) {
                        AppTheme.AUTO -> AppTheme.LIGHT
                        AppTheme.LIGHT -> AppTheme.DARK
                        AppTheme.DARK -> AppTheme.AUTO
                    }
                    val icon = when (keyMap.theme) {
                        AppTheme.AUTO -> Icons.Default.BrightnessAuto
                        AppTheme.LIGHT -> Icons.Default.BrightnessHigh
                        AppTheme.DARK -> Icons.Default.Brightness4
                    }
                    IconButton(onClick = { scope.launch { Config.setTheme(ctx, nextTheme) } }) {
                        Icon(icon, contentDescription = "Сменить тему")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Назначьте действие для каждого типа нажатия клавиши Essential.",
                style = MaterialTheme.typography.bodyMedium,
            )
            HorizontalDivider()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(TapSlot.entries.toTypedArray()) { slot ->
                    val value = keyMap.forSlot(slot)
                    val customConfig = keyMap.customConfigs[slot] ?: CustomConfig()
                    var expanded by remember { mutableStateOf(false) }

                    SlotRow(
                        slot = slot,
                        valueLabel = slotDisplay(value, apps),
                        icon = apps?.find { it.pkg == value }?.icon,
                        enabled = apps != null,
                        isCustom = value == Config.ACTION_CUSTOM_PANEL,
                        expanded = expanded,
                        onExpandToggle = { expanded = !expanded },
                        onClick = { picker = slot },
                    )

                    if (expanded && value == Config.ACTION_CUSTOM_PANEL) {
                        CustomActionSettings(
                            slot = slot,
                            config = customConfig,
                            apps = apps ?: emptyList(),
                            onUpdateType = { scope.launch { Config.setCustomActionType(ctx, slot, it) } },
                            onUpdateMacroMode = { scope.launch { Config.setMacroMode(ctx, slot, it) } },
                            onUpdateFavorites = { scope.launch { Config.setFavoriteApps(ctx, slot, it) } },
                            onStartRecording = {
                                val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                    action = "com.aiden.essentialmapper.START_RECORDING"
                                    putExtra("slot", slot.key)
                                }
                                ctx.startService(intent)
                            }
                        )
                    }
                }

                if (apps == null) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Загрузка приложений…")
                        }
                    }
                }
            }
        }
    }

    val slot = picker
    val loaded = apps
    if (slot != null && loaded != null) {
        AppPickerDialog(
            title = slot.label,
            slot = slot,
            apps = loaded,
            current = keyMap.forSlot(slot),
            onPick = { pkg ->
                scope.launch { Config.set(ctx, slot, pkg) }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

@Composable
private fun CustomActionSettings(
    slot: TapSlot,
    config: CustomConfig,
    apps: List<AppInfo>,
    onUpdateType: (CustomActionType) -> Unit,
    onUpdateMacroMode: (MacroMode) -> Unit,
    onUpdateFavorites: (List<String>) -> Unit,
    onStartRecording: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Настройка действия", style = MaterialTheme.typography.labelLarge)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomActionOption("По умолчанию", CustomActionType.NONE, config.type, onUpdateType)
                CustomActionOption("Избранные приложения", CustomActionType.FAVORITES, config.type, onUpdateType)
                CustomActionOption("Повтор записанных действий", CustomActionType.RECORDING, config.type, onUpdateType)
            }

            if (config.type == CustomActionType.RECORDING) {
                HorizontalDivider()
                Text("Режим макроса", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MacroMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onUpdateMacroMode(mode) }
                        ) {
                            RadioButton(selected = config.macroMode == mode, onClick = null)
                            Spacer(Modifier.width(4.dp))
                            Text(mode.label)
                        }
                    }
                }
                Button(onClick = onStartRecording, modifier = Modifier.fillMaxWidth()) {
                    Text(if (config.recordedGesture.isEmpty()) "Записать макрос" else "Перезаписать макрос")
                }
            }

            if (config.type == CustomActionType.FAVORITES) {
                HorizontalDivider()
                Text("Избранные приложения", style = MaterialTheme.typography.labelMedium)
                apps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newList = if (config.favoriteApps.contains(app.pkg)) {
                                    config.favoriteApps - app.pkg
                                } else {
                                    config.favoriteApps + app.pkg
                                }
                                onUpdateFavorites(newList)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Checkbox(
                            checked = config.favoriteApps.contains(app.pkg),
                            onCheckedChange = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(keyMap: KeyMap, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    
    LaunchedEffect(Unit) {
        apps = loadLaunchableApps(ctx)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = { openAccessibilitySettings(ctx) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Настройки спец. возможностей")
                }
            }

            item { HorizontalDivider() }

            item { Text("Настраиваемая кнопка", style = MaterialTheme.typography.titleLarge) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сторона расположения", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (keyMap.panelSide == PanelSide.LEFT) "Слева" else "Справа")
                        Switch(
                            checked = keyMap.panelSide == PanelSide.RIGHT,
                            onCheckedChange = { isRight ->
                                scope.launch {
                                    Config.setPanelConfig(
                                        ctx,
                                        if (isRight) PanelSide.RIGHT else PanelSide.LEFT,
                                        keyMap.panelYOffset
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Высота расположения", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${(keyMap.panelYOffset * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = keyMap.panelYOffset,
                        onValueChange = { newValue ->
                            scope.launch {
                                Config.setPanelConfig(ctx, keyMap.panelSide, newValue)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomActionOption(
    label: String,
    type: CustomActionType,
    current: CustomActionType,
    onSelect: (CustomActionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(type) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = type == current, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/** Resolve a stored slot value to a human label. */
private fun slotDisplay(value: String?, apps: List<AppInfo>?): String = when {
    value == null -> "По умолчанию"
    value == Config.ACTION_FLASHLIGHT -> "Фонарик (вкл/выкл)"
    value == Config.ACTION_BACK -> "Назад"
    value == Config.ACTION_KILL_ESSENTIAL -> "Kill Essential Space"
    value == Config.ACTION_KILL_NOTES -> "Kill Notes"
    value == Config.ACTION_CUSTOM_PANEL -> "Настраиваемая"
    else -> apps?.find { it.pkg == value }?.label ?: value
}

@Composable
private fun SlotRow(
    slot: TapSlot,
    valueLabel: String,
    icon: ImageBitmap?,
    enabled: Boolean,
    isCustom: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(slot.label, style = MaterialTheme.typography.labelMedium)
                Text(valueLabel, style = MaterialTheme.typography.bodyLarge)
            }
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
            
            if (isCustom) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Настройки"
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    title: String,
    slot: TapSlot,
    apps: List<AppInfo>,
    current: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
        ) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.weight(1f)) {
                    item {
                        PickRow("По умолчанию", null, selected = current == null) { onPick(null) }
                    }
                    item {
                        PickRow(
                            "Фонарик (вкл/выкл)",
                            null,
                            selected = current == Config.ACTION_FLASHLIGHT,
                        ) { onPick(Config.ACTION_FLASHLIGHT) }
                    }
                    item {
                        PickRow(
                            "Назад",
                            null,
                            selected = current == Config.ACTION_BACK,
                        ) { onPick(Config.ACTION_BACK) }
                    }
                    item {
                        PickRow(
                            "Настраиваемая",
                            null,
                            selected = current == Config.ACTION_CUSTOM_PANEL,
                        ) { onPick(Config.ACTION_CUSTOM_PANEL) }
                    }
                    if (slot == TapSlot.SINGLE || slot == TapSlot.LONG) {
                        item {
                            PickRow(
                                "Kill Notes",
                                null,
                                selected = current == Config.ACTION_KILL_NOTES,
                            ) { onPick(Config.ACTION_KILL_NOTES) }
                        }
                    } else {
                        item {
                            PickRow(
                                "Kill Essential Space",
                                null,
                                selected = current == Config.ACTION_KILL_ESSENTIAL,
                            ) { onPick(Config.ACTION_KILL_ESSENTIAL) }
                        }
                    }
                    items(apps, key = { it.pkg }) { a ->
                        PickRow(a.label, a.icon, selected = current == a.pkg) { onPick(a.pkg) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickRow(
    label: String,
    icon: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Spacer(Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
