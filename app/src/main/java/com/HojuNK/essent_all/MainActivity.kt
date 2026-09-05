package com.HojuNK.essent_all

import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import android.net.Uri
import android.content.Context
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Constraints
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import java.util.UUID
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.ComponentName
import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.Density

@Composable
fun TypewriterTitle(baseText: String = stringResource(R.string.app_name), targetText: String = "Not essential space") {
    var text by remember { mutableStateOf(baseText) }
    LaunchedEffect(Unit) {
        delay(2000)
        while (text.isNotEmpty()) {
            delay(100)
            text = text.dropLast(1)
        }
        delay(300)
        for (char in targetText) {
            delay(100)
            text += char
        }
    }
    Text(text)
}

data class RadialMenuState(
    val center: Offset, 
    val actions: List<RadialAction>, 
    val hoveredIndex: Int = -1,
    val sourceId: String? = null
)
val LocalRadialMenu = compositionLocalOf <((RadialMenuState?) -> Unit)> { { } }
val LocalRadialMenuState = compositionLocalOf<RadialMenuState?> { null }

enum class NavScreen(val labelRes: Int, val icon: ImageVector) {
    KEY(R.string.nav_key, Icons.Filled.TouchApp),
    EVENTS(R.string.nav_events, Icons.Filled.Bolt),
    MACROS(R.string.nav_macros, Icons.Filled.AutoGraph),
    GAMES(R.string.nav_games, Icons.Filled.SportsEsports)
}

enum class LoadingStage {
    INITIAL,
    CORE_READY,
    TRANSITIONING,
    FULLY_LOADED
}

@Composable
fun PillBottomNavigation(
    currentScreen: NavScreen,
    loadingStage: LoadingStage,
    onScreenSelected: (NavScreen) -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val isExpanded = loadingStage == LoadingStage.FULLY_LOADED || loadingStage == LoadingStage.TRANSITIONING
    
    Surface(
        color = Color(0xFF2B2B2B), // Серый фон панели
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
            .padding(bottom = 48.dp)
            .height(70.dp)
            .wrapContentWidth()
            .animateContentSize()
            .graphicsLayer {
                if (radialMenuState != null) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        20f, 20f, android.graphics.Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                    alpha = 0.6f
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.spacedBy(4.dp) else Arrangement.Center
        ) {
            NavScreen.entries.forEach { screen ->
                val isKey = screen == NavScreen.KEY
                val shouldShow = isKey || isExpanded
                
                if (shouldShow) {
                    val selected = screen == currentScreen
                    PillNavItem(
                        screen = screen, 
                        selected = selected && loadingStage == LoadingStage.FULLY_LOADED,
                        showLabel = loadingStage == LoadingStage.FULLY_LOADED && selected,
                        onClick = { if (isExpanded) onScreenSelected(screen) }
                    )
                }
            }
        }
    }
}





@Composable
fun PillNavItem(
    screen: NavScreen,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Color(0xFF1A1A1A) else Color.Transparent // Темно-серый для выбранного
    val interactionSource = remember { MutableInteractionSource() }
    
    Surface(
        color = background,
        shape = RoundedCornerShape(50.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = stringResource(screen.labelRes),
                tint = if (selected) Color.White else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            AnimatedVisibility(
                visible = showLabel,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(screen.labelRes),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}





private fun openAccessibilitySettings(ctx: Context) {
    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
    val expectedId = ComponentName(context, service).flattenToString()
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        if (colonSplitter.next().equals(expectedId, ignoreCase = true)) return true
    }
    return false
}

private fun showAccessibilityNotification(context: Context) {
    val channelId = "accessibility_check"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    val channel = NotificationChannel(channelId, context.getString(R.string.accessibility_setup_title), NotificationManager.IMPORTANCE_HIGH)
    notificationManager.createNotificationChannel(channel)
    
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(context.getString(R.string.accessibility_permission_required))
        .setContentText(context.getString(R.string.accessibility_enable_msg))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
        
    notificationManager.notify(1001, notification)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainContent(intent) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent { MainContent(intent) }
    }
}



@Composable
fun MainContent(intent: Intent) {
    val ctx = LocalContext.current
    val keyMap by Config.flow(ctx).collectAsState(initial = KeyMap())
    var loadingStage by remember { mutableStateOf(LoadingStage.INITIAL) }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAccessibilityError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(keyMap) {
        if (loadingStage == LoadingStage.INITIAL) {
            delay(300) // Тема и язык
            loadingStage = LoadingStage.CORE_READY
            
            if (apps == null) {
                scope.launch(Dispatchers.Default) {
                    Config.seedDefaultsIfEmpty(ctx)
                    Config.applySpecificDefaults(ctx)
                    val loadedApps = loadLaunchableApps(ctx)
                    withContext(Dispatchers.Main) {
                        apps = loadedApps
                        delay(500)
                        loadingStage = LoadingStage.TRANSITIONING
                        delay(600)
                        loadingStage = LoadingStage.FULLY_LOADED
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                showAccessibilityNotification(ctx)
            }
            delay(30000) // Check every 30 seconds
        }
    }

    var editSlot by remember { mutableStateOf<TapSlot?>(null) }
    var recordedMacroActions by remember { mutableStateOf<List<String>?>(null) }
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    var radialMenuState by remember { mutableStateOf<RadialMenuState?>(null) }

    LaunchedEffect(intent) {
        val actions = intent.getStringArrayExtra("recorded_macro_actions")
        if (actions != null) {
            recordedMacroActions = actions.toList()
        }
        if (intent.action == "com.HojuNK.essent_all.EDIT_CONTROLS") {
            val slotKey = intent.getStringExtra("slot")
            editSlot = TapSlot.entries.find { it.key == slotKey }
        }
    }

    val useDark = when (keyMap.theme) {
        AppTheme.AUTO -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val colorScheme = if (useDark) {
        darkColorScheme(primary = Color.White, onPrimary = Color.Black, surface = Color.Black, onSurface = Color.White, background = Color.Black, onBackground = Color.White)
    } else {
        lightColorScheme(primary = Color.Black, onPrimary = Color.White)
    }
    
    CompositionLocalProvider(
        LocalRadialMenu provides { radialMenuState = it },
        LocalRadialMenuState provides radialMenuState
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            val pagerState = rememberPagerState { NavScreen.entries.size }

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = loadingStage == LoadingStage.FULLY_LOADED,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                        val screen = NavScreen.entries[page]
                        Box(
                            Modifier.graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                alpha = lerp(
                                    start = 0.5f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                )
                                val scale = lerp(
                                    start = 0.9f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                )
                                scaleX = scale
                                scaleY = scale
                            }
                        ) {
                            when (screen) {
                                NavScreen.KEY -> ConfigScreen(keyMap, apps, onOpenSettings = { showSettings = true })
                                NavScreen.EVENTS -> EventsScreen(keyMap, apps, onOpenSettings = { showSettings = true })
                                NavScreen.MACROS -> MacrosScreen(keyMap, apps, onOpenSettings = { showSettings = true }, onEditMacro = { editingMacro = it }, onShowAccessibilityError = { showAccessibilityError = true })
                                NavScreen.GAMES -> GamesScreen(onOpenSettings = { showSettings = true }, onShowAccessibilityError = { showAccessibilityError = true })
                            }
                    }
                }

                // Overlay the floating hotbar
                Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter), contentAlignment = Alignment.BottomCenter) {
                    PillBottomNavigation(
                        currentScreen = NavScreen.entries[pagerState.currentPage],
                        loadingStage = loadingStage,
                        onScreenSelected = { screen ->
                            scope.launch { pagerState.animateScrollToPage(screen.ordinal) }
                        }
                    )
                }

                // Full-screen Settings Overlay
                AnimatedVisibility(
                    visible = showSettings,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        SettingsScreen(keyMap, onBack = { showSettings = false })
                    }
                }

                recordedMacroActions?.let { actions ->
                    MacroAddPanel(
                        initialMacro = Macro(UUID.randomUUID().toString(), "🤖", "", actions = actions, isEnabled = true),
                        apps = apps,
                        onDismiss = { recordedMacroActions = null },
                        onSave = { newMacro ->
                            scope.launch { Config.updateMacro(ctx, newMacro) }
                            recordedMacroActions = null
                        }
                    )
                }

                editingMacro?.let { macro ->
                    MacroAddPanel(
                        initialMacro = macro,
                        apps = apps,
                        onDismiss = { editingMacro = null },
                        onSave = { updated ->
                            scope.launch { Config.updateMacro(ctx, updated) }
                            editingMacro = null
                        }
                    )
                }

                editSlot?.let { slot ->
                    val config = keyMap.customConfigs[slot] ?: CustomConfig()
                    Dialog(onDismissRequest = { editSlot = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                        ControlPanelContent(
                            initialItems = config.controlItems,
                            isEditing = true,
                            apps = apps,
                            onSave = { updatedItems ->
                                scope.launch { Config.setControlItems(ctx, slot, updatedItems) }
                                editSlot = null
                            },
                            onControlClick = { false },
                            onDismiss = { editSlot = null }
                        )
                    }
                }

                // GLOBAL RADIAL MENU OVERLAY
                val currentRadialMenu = radialMenuState
                if (currentRadialMenu != null) {
                    RadialActionMenu(
                        state = currentRadialMenu,
                        onDismiss = { radialMenuState = null }
                    )
                }

                if (showAccessibilityError) {
                    AlertDialog(
                        onDismissRequest = { showAccessibilityError = false },
                        containerColor = Color(0xFF1A1A1A),
                        title = { Text(stringResource(R.string.accessibility_setup_title), color = Color.White) },
                        text = { Text(stringResource(R.string.accessibility_enable_msg), color = Color.Gray) },
                        confirmButton = {
                            TextButton(onClick = {
                                showAccessibilityError = false
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                ctx.startActivity(intent)
                            }) {
                                Text(stringResource(R.string.settings), color = Color(0xFFFFD700))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAccessibilityError = false }) {
                                Text(stringResource(R.string.cancel), color = Color.Gray)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(keyMap: KeyMap, apps: List<AppInfo>?, onOpenSettings: () -> Unit) {
    val radialMenuState = LocalRadialMenuState.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var picker by remember { mutableStateOf<TapSlot?>(null) }
    
    var showTouchForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showSwipeForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showBrightnessForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showFullAppPickerForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showVolumeForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showMediaForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showUrlForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showThemeForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showAutoBrightnessForSlot by remember { mutableStateOf<TapSlot?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TypewriterTitle() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .drawBehind {
                        val blurHeight = 20.dp.toPx()
                        drawRect(color = Color.Black, size = size)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    }
                    .graphicsLayer {
                        if (radialMenuState != null) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                20f, 20f, android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                            alpha = 0.6f
                        }
                    },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.assign_action_hint), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(TapSlot.entries.filter { it != TapSlot.SHORTCUT }.toList()) { slot ->
                    var expanded by remember { mutableStateOf(false) }
                    val value = keyMap.forSlot(slot)
                    val customConfig = keyMap.customConfigs[slot] ?: CustomConfig()

                    Column {
                        val isCustom = value == Config.ACTION_CONTROLS ||
                                       value?.startsWith(Config.ACTION_FAVORITES_PREFIX) == true ||
                                       value?.startsWith(Config.ACTION_MACRO_PREFIX) == true ||
                                       value?.startsWith(Config.ACTION_TOUCH_PREFIX) == true ||
                                       value?.startsWith(Config.ACTION_SWIPE_PREFIX) == true ||
                                       value?.startsWith(Config.ACTION_BRIGHTNESS_PREFIX) == true

                        SlotRow(
                            slot = slot, valueLabel = slotDisplay(ctx, value, keyMap.events, keyMap.macros, apps),
                            icon = apps?.find { it.pkg == value }?.icon,
                            enabled = apps != null,
                            isCustom = isCustom,
                            expanded = expanded, onExpandToggle = { expanded = !expanded },
                            onClick = { picker = slot },
                            onIconClick = { showFullAppPickerForSlot = slot }
                        )

                        if (expanded) {
                            when {
                                value == Config.ACTION_CONTROLS -> {
                                    ControlActionSettings(
                                        items = customConfig.controlItems,
                                        onUpdate = { scope.launch { Config.setControlItems(ctx, slot, it) } }
                                    )
                                }
                                value?.startsWith(Config.ACTION_FAVORITES_PREFIX) == true -> {
                                    FavoritesActionSettings(
                                        config = customConfig,
                                        apps = apps ?: emptyList(),
                                        onUpdateApps = { scope.launch { Config.setFavoriteApps(ctx, slot, it) } },
                                        onUpdateVisibility = { onClick, onOutside, onBack, onKey, autoHide ->
                                            scope.launch { Config.setCustomVisibility(ctx, slot, onClick, onOutside, onBack, onKey, autoHide) }
                                        }
                                    )
                                }
                                value?.startsWith(Config.ACTION_MACRO_PREFIX) == true -> {
                                    MacroSelectionSettings(
                                        currentId = value.removePrefix(Config.ACTION_MACRO_PREFIX),
                                        macroMode = customConfig.macroMode,
                                        macros = keyMap.macros,
                                        onSelect = { id -> scope.launch { Config.set(ctx, slot, "${Config.ACTION_MACRO_PREFIX}$id") } },
                                        onUpdateMode = { mode -> scope.launch { Config.setMacroMode(ctx, slot, mode) } }
                                    )
                                }
                                value?.startsWith(Config.ACTION_TOUCH_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_touch") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_coords)) }
                                }
                                value?.startsWith(Config.ACTION_SWIPE_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_swipe") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_swipe)) }
                                }
                                value?.startsWith(Config.ACTION_BRIGHTNESS_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_brightness") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_brightness)) }
                                }
                                value?.startsWith(Config.ACTION_VOLUME_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_volume") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_volume)) }
                                }
                                value?.startsWith(Config.ACTION_MEDIA_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_media") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_media)) }
                                }
                                value?.startsWith(Config.ACTION_URL_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_url") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_url)) }
                                }
                                value?.startsWith(Config.ACTION_THEME_PREFIX) == true -> {
                                    Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_theme") } }, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) { Text(stringResource(R.string.change_theme)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (picker != null) {
        val slotToUpdate = picker!!
        AppPickerDialog(title = ctx.getString(slotToUpdate.labelRes), slot = slotToUpdate, current = keyMap.forSlot(slotToUpdate),
            onPick = { pkg -> 
                if (pkg == "action:pick_touch") showTouchForSlot = slotToUpdate
                else if (pkg == "action:pick_swipe") showSwipeForSlot = slotToUpdate
                else if (pkg == "action:pick_brightness") showBrightnessForSlot = slotToUpdate
                else if (pkg == "action:full_app_list") showFullAppPickerForSlot = slotToUpdate
                else if (pkg == "action:pick_volume") showVolumeForSlot = slotToUpdate
                else if (pkg == "action:pick_media") showMediaForSlot = slotToUpdate
                else if (pkg == "action:pick_url") showUrlForSlot = slotToUpdate
                else if (pkg == "action:pick_theme") showThemeForSlot = slotToUpdate
                else if (pkg == "action:pick_auto_brightness") showAutoBrightnessForSlot = slotToUpdate
                else scope.launch { Config.set(ctx, slotToUpdate, pkg) }
                picker = null 
            },
            onDismiss = { picker = null }
        )
    }

    showFullAppPickerForSlot?.let { slot ->
        UniversalAppPickerDialog(
            apps = (apps ?: emptyList()).filter { it.isLaunchable },
            selected = listOfNotNull(keyMap.forSlot(slot).takeIf { it != null && !it.startsWith("action:") }),
            multiSelect = false,
            onConfirmed = { picked ->
                val pkg = picked.firstOrNull()
                if (pkg != null) {
                    scope.launch { Config.set(ctx, slot, pkg) }
                }
                showFullAppPickerForSlot = null
            },
            onDismiss = { showFullAppPickerForSlot = null }
        )
    }

    // Handle pickers triggered by setting existing action
    LaunchedEffect(keyMap) {
        TapSlot.entries.forEach { slot ->
            val value = keyMap.forSlot(slot)
            if (value == "action:pick_touch") showTouchForSlot = slot
            if (value == "action:pick_swipe") showSwipeForSlot = slot
            if (value == "action:pick_brightness") showBrightnessForSlot = slot
            if (value == "action:pick_volume") showVolumeForSlot = slot
            if (value == "action:pick_media") showMediaForSlot = slot
            if (value == "action:pick_url") showUrlForSlot = slot
            if (value == "action:pick_theme") showThemeForSlot = slot
            if (value == "action:pick_auto_brightness") showAutoBrightnessForSlot = slot
        }
    }

    showTouchForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_TOUCH_PREFIX)
        TouchPointPickerDialog(initialValue = if (existing == "action:pick_touch") null else existing, onDismiss = { showTouchForSlot = null }, onConfirmed = { x, y, hold ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold") }
            showTouchForSlot = null
        })
    }
    showSwipeForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_SWIPE_PREFIX)
        SwipePointPickerDialog(initialValue = if (existing == "action:pick_swipe") null else existing, onDismiss = { showSwipeForSlot = null }, onConfirmed = { x1, y1, x2, y2, hS, hE ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE") }
            showSwipeForSlot = null
        })
    }
    showBrightnessForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)?.toIntOrNull() ?: 50
        BrightnessPickerDialog(initialValue = existing, onDismiss = { showBrightnessForSlot = null }, onConfirmed = { pct ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_BRIGHTNESS_PREFIX}$pct") }
            showBrightnessForSlot = null
        })
    }
    showVolumeForSlot?.let { slot ->
        VolumePickerDialog(onDismiss = { showVolumeForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_VOLUME_PREFIX}$data") }
            showVolumeForSlot = null
        })
    }
    showMediaForSlot?.let { slot ->
        MediaPickerDialog(onDismiss = { showMediaForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_MEDIA_PREFIX}$data") }
            showMediaForSlot = null
        })
    }
    showUrlForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_URL_PREFIX) ?: ""
        UrlPickerDialog(initialValue = existing, onDismiss = { showUrlForSlot = null }, onConfirmed = { url ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_URL_PREFIX}$url") }
            showUrlForSlot = null
        })
    }
    showThemeForSlot?.let { slot ->
        ThemePickerDialog(onDismiss = { showThemeForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_THEME_PREFIX}$data") }
            showThemeForSlot = null
        })
    }
    showAutoBrightnessForSlot?.let { slot ->
        AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$data") }
            showAutoBrightnessForSlot = null
        })
    }
}



@Composable
private fun FavoritesActionSettings(
    config: CustomConfig,
    apps: List<AppInfo>,
    onUpdateApps: (List<String>) -> Unit,
    onUpdateVisibility: (Boolean, Boolean, Boolean, Boolean, Int) -> Unit
) {
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.select_favorites_title), style = MaterialTheme.typography.labelLarge)
            apps.forEach { app ->
                val isSelected = config.favoriteApps.contains(app.pkg)
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    onUpdateApps(if (isSelected) config.favoriteApps - app.pkg else config.favoriteApps + app.pkg)
                }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp)); Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.size(24.dp))
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
            Text(stringResource(R.string.display_settings), style = MaterialTheme.typography.titleMedium, color = Color.White)

            ShortcutToggle(stringResource(R.string.keep_on_click), config.keepVisibleOnClick) {
                onUpdateVisibility(it, config.keepVisibleOnOutsideClick, config.keepVisibleOnBack, config.keepVisibleOnKey, config.autoHideSeconds)
            }
            ShortcutToggle(stringResource(R.string.keep_on_outside), config.keepVisibleOnOutsideClick) {
                onUpdateVisibility(config.keepVisibleOnClick, it, config.keepVisibleOnBack, config.keepVisibleOnKey, config.autoHideSeconds)
            }
            ShortcutToggle(stringResource(R.string.keep_on_key), config.keepVisibleOnKey) {
                onUpdateVisibility(config.keepVisibleOnClick, config.keepVisibleOnOutsideClick, config.keepVisibleOnBack, it, config.autoHideSeconds)
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.auto_hide), style = MaterialTheme.typography.labelLarge, color = Color.Gray)

            val autoHideOptions = listOf(0 to stringResource(R.string.never_hide), 2 to "2 ${stringResource(R.string.unit_seconds)}", 5 to "5 ${stringResource(R.string.unit_seconds)}", 10 to "10 ${stringResource(R.string.unit_seconds)}", 20 to "20 ${stringResource(R.string.unit_seconds)}")
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(autoHideOptions) { (secs, label) ->
                    val selected = config.autoHideSeconds == secs
                    Surface(
                        onClick = { onUpdateVisibility(config.keepVisibleOnClick, config.keepVisibleOnOutsideClick, config.keepVisibleOnBack, config.keepVisibleOnKey, secs) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) Color.White else Color(0xFF2B2B2B),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}





@Composable
private fun ControlActionSettings(items: List<ControlItem>, onUpdate: (List<ControlItem>) -> Unit) {
    var showGridEditor by remember { mutableStateOf(false) }
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { showGridEditor = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_control_panel)) }
            if (showGridEditor) {
                Dialog(onDismissRequest = { showGridEditor = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    ControlPanelContent(
                        initialItems = items,
                        isEditing = true,
                        onSave = { updatedItems -> onUpdate(updatedItems) },
                        onControlClick = { false },
                        onDismiss = { showGridEditor = false }
                    )
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

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { TypewriterTitle(targetText = stringResource(R.string.settings)) },
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
                    Text("Эффекты", style = MaterialTheme.typography.titleLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Отключить визуальные эффекты")
                        Switch(checked = keyMap.disableAnimations, onCheckedChange = { scope.launch { Config.setDisableAnimations(ctx, it) } })
                    }
                    Text("Если включено, экран не будет замораживаться при нажатии Essential Key.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
private fun CustomActionOption(label: String, type: CustomActionType, current: CustomActionType, onSelect: (CustomActionType) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(type) }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = type == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color.White)); Spacer(Modifier.width(8.dp)); Text(label)
    }
}



@Composable
private fun LanguageSelectorDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
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





private fun AppLanguage.getLabel(ctx: Context): String = when (this) {
    AppLanguage.SYSTEM -> ctx.getString(R.string.lang_system)
    AppLanguage.ENGLISH -> ctx.getString(R.string.lang_english)
    AppLanguage.RUSSIAN -> ctx.getString(R.string.lang_russian)
}

private fun slotDisplay(ctx: Context, value: String?, events: List<EventRule>, macros: List<Macro>, apps: List<AppInfo>?): String = when {
    value == null -> ctx.getString(R.string.default_action)
    value.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
        val coords = value.removePrefix(Config.ACTION_TOUCH_PREFIX).replace("_", ", ")
        "${ctx.getString(R.string.action_touch)} ($coords)"
    }
    value.startsWith(Config.ACTION_PATH_PREFIX) -> {
        val pts = value.removePrefix(Config.ACTION_PATH_PREFIX).split(";").filter { it.isNotEmpty() }
        if (pts.size >= 2) {
            val first = pts.first().split(",")
            val last = pts.last().split(",")
            if (first.size >= 2 && last.size >= 2) {
                "${ctx.getString(R.string.action_swipe_precise)} (${first[0]},${first[1]} -> ${last[0]},${last[1]})"
            } else ctx.getString(R.string.action_swipe_precise)
        } else ctx.getString(R.string.action_swipe_precise)
    }
    value.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
        val parts = value.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
        if (parts.size >= 4) {
            "${ctx.getString(R.string.action_swipe)} (${parts[0]},${parts[1]} -> ${parts[2]},${parts[3]})"
        } else ctx.getString(R.string.action_swipe)
    }
    value.startsWith(Config.ACTION_DELAY_PREFIX) -> {
        val sec = value.removePrefix(Config.ACTION_DELAY_PREFIX)
        "${ctx.getString(R.string.action_delay)} ($sec ${ctx.getString(R.string.unit_seconds)})"
    }
    value.startsWith(Config.ACTION_BRIGHTNESS_PREFIX) -> {
        val pct = value.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)
        "${ctx.getString(R.string.action_brightness)}: $pct%"
    }
    value == Config.ACTION_FAVORITES_PREFIX -> ctx.getString(R.string.favorites)
    value == Config.ACTION_CONTROLS -> ctx.getString(R.string.control_panel)
    value == Config.ACTION_HOME -> ctx.getString(R.string.action_home)
    value == Config.ACTION_NOTIFICATIONS -> ctx.getString(R.string.action_notifications)
    value == Config.ACTION_FLASHLIGHT -> ctx.getString(R.string.action_flashlight_toggle)
    value == Config.ACTION_GLYPH -> ctx.getString(R.string.action_glyph_toggle)
    value == Config.ACTION_BACK -> ctx.getString(R.string.action_back)
    value.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_AUTO_BRIGHTNESS_PREFIX)
        "${ctx.getString(R.string.action_auto_brightness)} ($data)"
    }
    value == Config.ACTION_SCREENSHOT -> ctx.getString(R.string.action_screenshot)
    value == Config.ACTION_LOCK_SCREEN -> ctx.getString(R.string.action_lock_screen)
    value == Config.ACTION_SPLIT_SCREEN -> ctx.getString(R.string.action_split_screen)
    value == Config.ACTION_DND_TOGGLE -> ctx.getString(R.string.control_dnd)
    value == Config.ACTION_CLEAR_NOTIFICATIONS -> ctx.getString(R.string.action_clear_notifications)
    value.startsWith(Config.ACTION_VOLUME_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_VOLUME_PREFIX)
        if (data.contains("set:")) {
            val stream = data.substringBefore(":")
            val pct = data.substringAfter("set:")
            "${ctx.getString(R.string.action_volume)} ($stream: $pct%)"
        } else {
            "${ctx.getString(R.string.action_volume)} ($data)"
        }
    }
    value.startsWith(Config.ACTION_MEDIA_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_MEDIA_PREFIX)
        "${ctx.getString(R.string.action_media)} ($data)"
    }
    value.startsWith(Config.ACTION_URL_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_URL_PREFIX)
        "${ctx.getString(R.string.action_url)}: $data"
    }
    value.startsWith(Config.ACTION_THEME_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_THEME_PREFIX)
        "${ctx.getString(R.string.action_theme)} ($data)"
    }
    value.startsWith(Config.ACTION_MACRO_PREFIX) -> {
        val id = value.removePrefix(Config.ACTION_MACRO_PREFIX)
        val macro = macros.find { it.id == id }
        if (macro != null) "${ctx.getString(R.string.action_macro)}: ${macro.title}" else "${ctx.getString(R.string.action_macro)}: (${ctx.getString(R.string.action_macro_not_selected)})"
    }
    value.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX) -> ctx.getString(R.string.action_wait_confirmation)
    value == Config.ACTION_KILL_ESSENTIAL -> ctx.getString(R.string.kill_essential_space)
    value == Config.ACTION_KILL_NOTES -> ctx.getString(R.string.kill_notes)
    else -> apps?.find { it.pkg == value }?.label ?: value
}

@Composable
private fun SlotRow(slot: TapSlot, valueLabel: String, icon: ImageBitmap?, enabled: Boolean, isCustom: Boolean, expanded: Boolean, onExpandToggle: () -> Unit, onClick: () -> Unit, onIconClick: () -> Unit) {
    Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.clickable(enabled = enabled, onClick = onClick).fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(slot.labelRes), style = MaterialTheme.typography.labelMedium)
                Text(valueLabel, style = MaterialTheme.typography.bodyLarge)
            }
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconClick() }
                )
            }
            if (isCustom || valueLabel.startsWith(stringResource(R.string.shortcut_prefix))) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onExpandToggle) { Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null) }
            }
        }
    }
}





@Composable
fun UniversalAppPickerDialog(
    apps: List<AppInfo>,
    selected: List<String>,
    multiSelect: Boolean,
    onConfirmed: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var currentSelected by remember { mutableStateOf(selected) }

    val filtered = remember(search, apps, showSystem) {
        apps.filter { app ->
            val matchesSearch = app.label.contains(search, ignoreCase = true) || app.pkg.contains(search, ignoreCase = true)
            val matchesSystem = showSystem || !app.isSystem
            matchesSearch && matchesSystem
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (selected.isNotEmpty()) {
            val idx = filtered.indexOfFirst { it.pkg == selected.first() }
            if (idx >= 0) listState.scrollToItem(idx)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.select_app_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                // Search and Toggle Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_hint), color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        trailingIcon = if (search.isNotEmpty()) {
                            { IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, null, tint = Color.Gray) } }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showSystem = !showSystem }) {
                    Checkbox(
                        checked = showSystem,
                        onCheckedChange = { showSystem = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black, uncheckedColor = Color.Gray)
                    )
                    Text("Показывать системные", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    items(filtered, key = { it.pkg }) { a ->
                        val isItemSelected = currentSelected.contains(a.pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (multiSelect) {
                                        currentSelected = if (isItemSelected) currentSelected - a.pkg else currentSelected + a.pkg
                                    } else {
                                        currentSelected = listOf(a.pkg)
                                    }
                                }
                                .background(if (isItemSelected && !multiSelect) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(bitmap = a.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                                Text(a.pkg, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            if (multiSelect) {
                                Checkbox(
                                    checked = isItemSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black)
                                )
                            } else if (isItemSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmed(currentSelected) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}





@Composable
private fun AppPickerDialog(title: String, slot: TapSlot, current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.wrapContentHeight().heightIn(max = 550.dp)) {
                    item { PickRow(stringResource(R.string.default_action), null, current == null) { onPick(null) } }
                    item { PickRow(stringResource(R.string.action_clear_notifications), null, current == Config.ACTION_CLEAR_NOTIFICATIONS) { onPick(Config.ACTION_CLEAR_NOTIFICATIONS) } }
                    item { PickRow(stringResource(R.string.action_auto_brightness_dots), null, current?.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX) == true) { onPick("action:pick_auto_brightness") } }
                    item { PickRow(stringResource(R.string.open_app), null, false) { onPick("action:full_app_list") } }
                    item { PickRow(stringResource(R.string.action_flashlight_toggle), null, current == Config.ACTION_FLASHLIGHT) { onPick(Config.ACTION_FLASHLIGHT) } }
                    item { PickRow(stringResource(R.string.action_glyph_toggle), null, current == Config.ACTION_GLYPH) { onPick(Config.ACTION_GLYPH) } }
                    item { PickRow(stringResource(R.string.action_back), null, current == Config.ACTION_BACK) { onPick(Config.ACTION_BACK) } }
                    item { PickRow(stringResource(R.string.action_home), null, current == Config.ACTION_HOME) { onPick(Config.ACTION_HOME) } }
                    item { PickRow(stringResource(R.string.action_notifications), null, current == Config.ACTION_NOTIFICATIONS) { onPick(Config.ACTION_NOTIFICATIONS) } }
                    item { PickRow(stringResource(R.string.control_panel), null, current == Config.ACTION_CONTROLS) { onPick(Config.ACTION_CONTROLS) } }
                    item { PickRow(stringResource(R.string.action_macro), null, current?.startsWith(Config.ACTION_MACRO_PREFIX) == true) { onPick(Config.ACTION_MACRO_PREFIX) } }
                    item { PickRow(stringResource(R.string.favorites), null, false) { onPick(Config.ACTION_FAVORITES_PREFIX) } }
                    item { PickRow(stringResource(R.string.action_touch_screen), null, false) { onPick("action:pick_touch") } }
                    item { PickRow(stringResource(R.string.action_swipe_screen), null, false) { onPick("action:pick_swipe") } }
                    item { PickRow(stringResource(R.string.action_set_brightness), null, false) { onPick("action:pick_brightness") } }
                    item { PickRow(stringResource(R.string.action_screenshot), null, current == Config.ACTION_SCREENSHOT) { onPick(Config.ACTION_SCREENSHOT) } }
                    item { PickRow(stringResource(R.string.action_lock_screen), null, current == Config.ACTION_LOCK_SCREEN) { onPick(Config.ACTION_LOCK_SCREEN) } }
                    item { PickRow(stringResource(R.string.action_split_screen), null, current == Config.ACTION_SPLIT_SCREEN) { onPick(Config.ACTION_SPLIT_SCREEN) } }
                    item { PickRow(stringResource(R.string.action_volume_control), null, current?.startsWith(Config.ACTION_VOLUME_PREFIX) == true) { onPick("action:pick_volume") } }
                    item { PickRow(stringResource(R.string.action_media_control), null, current?.startsWith(Config.ACTION_MEDIA_PREFIX) == true) { onPick("action:pick_media") } }
                    item { PickRow(stringResource(R.string.action_open_url), null, current?.startsWith(Config.ACTION_URL_PREFIX) == true) { onPick("action:pick_url") } }
                    item { PickRow(stringResource(R.string.action_dnd), null, current == Config.ACTION_DND_TOGGLE) { onPick(Config.ACTION_DND_TOGGLE) } }
                    item { PickRow(stringResource(R.string.action_change_theme), null, current?.startsWith(Config.ACTION_THEME_PREFIX) == true) { onPick("action:pick_theme") } }
                    if (slot == TapSlot.SINGLE || slot == TapSlot.LONG) {
                        item { PickRow(stringResource(R.string.kill_notes), null, current == Config.ACTION_KILL_NOTES) { onPick(Config.ACTION_KILL_NOTES) } }
                    } else {
                        item { PickRow(stringResource(R.string.kill_essential_space), null, current == Config.ACTION_KILL_ESSENTIAL) { onPick(Config.ACTION_KILL_ESSENTIAL) } }
                    }
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(keyMap: KeyMap, apps: List<AppInfo>?, onOpenSettings: () -> Unit) {
    val radialMenuState = LocalRadialMenuState.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddOverlay by remember { mutableStateOf<EventRule?>(null) }
    var showPriorityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TypewriterTitle(targetText = stringResource(R.string.events)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .drawBehind {
                        val blurHeight = 20.dp.toPx()
                        drawRect(color = Color.Black, size = size)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    }
                    .graphicsLayer {
                        if (radialMenuState != null) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                20f, 20f, android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                            alpha = 0.6f
                        }
                    },
                actions = {
                    IconButton(onClick = { showPriorityDialog = true }) {
                        Icon(Icons.Default.LowPriority, null, tint = Color.White)
                    }
                    IconButton(onClick = {
                        val nextMode = if (keyMap.eventsViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                        scope.launch { Config.setEventsViewMode(ctx, nextMode) }
                    }) {
                        Icon(if (keyMap.eventsViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView, null)
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddOverlay = EventRule(UUID.randomUUID().toString(), "⚡", "") },
                containerColor = Color.Gray,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 120.dp)
                    .graphicsLayer {
                        if (radialMenuState != null) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                20f, 20f, android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                            alpha = 0.6f
                        }
                    }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
            }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {
            if (keyMap.events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.events_empty_msg), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else if (keyMap.eventsViewMode == EventsViewMode.STRIPES) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(keyMap.events, key = { it.id }) { rule ->
                        StripeEventItem(
                            rule = rule,
                            onToggle = { enabled ->
                                scope.launch { Config.updateEvent(ctx, rule.copy(isEnabled = enabled)) }
                            },
                            onEdit = { showAddOverlay = rule },
                            onDuplicate = {
                                val newRule = rule.copy(id = UUID.randomUUID().toString(), title = "${rule.title}${ctx.getString(R.string.copy_suffix)}")
                                scope.launch { Config.updateEvent(ctx, newRule) }
                            },
                            onDelete = {
                                scope.launch { Config.deleteEvent(ctx, rule.id) }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(keyMap.events, key = { it.id }) { rule ->
                        RectangleEventItem(
                            rule = rule,
                            onToggle = { enabled ->
                                scope.launch { Config.updateEvent(ctx, rule.copy(isEnabled = enabled)) }
                            },
                            onEdit = { showAddOverlay = rule },
                            onDuplicate = {
                                val newRule = rule.copy(id = UUID.randomUUID().toString(), title = "${rule.title}${ctx.getString(R.string.copy_suffix)}")
                                scope.launch { Config.updateEvent(ctx, newRule) }
                            },
                            onDelete = {
                                scope.launch { Config.deleteEvent(ctx, rule.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddOverlay != null) {
        EventAddPanel(
            initialRule = showAddOverlay!!,
            macros = keyMap.macros,
            apps = apps,
            onDismiss = { showAddOverlay = null },
            onSave = { newRule ->
                scope.launch { Config.updateEvent(ctx, newRule) }
                showAddOverlay = null
            }
        )
    }

    if (showPriorityDialog) {
        EventPriorityDialog(
            initialEvents = keyMap.events,
            onSave = { newList ->
                scope.launch { Config.setEvents(ctx, newList) }
            },
            onDismiss = { showPriorityDialog = false }
        )
    }
}



@Composable
fun StripeEventItem(
    rule: EventRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    var isDeletingAnimating by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    val itemHeight = remember { Animatable(86f) }

    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    val showRadialMenu = LocalRadialMenu.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(isDeletingAnimating) {
        if (isDeletingAnimating) {
            itemHeight.animateTo(0f, animationSpec = tween(500))
            onDelete()
        }
    }

    if (itemHeight.value <= 0.1f && isDeletingAnimating) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight.value.dp)
            .graphicsLayer {
                val activeMenu = radialMenuState
                if (activeMenu != null && activeMenu.sourceId != rule.id) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        20f, 20f, android.graphics.Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                    alpha = 0.6f
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .onGloballyPositioned { itemOffsetInRoot = it.positionInRoot() }
            .background(if (isDeletingAnimating) Color.Red else Color(0xFF1A1A1A))
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (rule.isEnabled) Color(0xFF2B2B2B) else Color(0xFF101010),
            tonalElevation = if (rule.isEnabled) 2.dp else 0.dp,
            border = if (!rule.isEnabled) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)) else null,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rule, itemOffsetInRoot) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val menuCenter = down.position + itemOffsetInRoot
                        val actions = listOf(
                            RadialAction(if (rule.isEnabled) Icons.Default.Block else Icons.Default.CheckCircle, "Toggle") { onToggle(!rule.isEnabled) },
                            RadialAction(Icons.Default.Edit, "Edit") { onEdit() },
                            RadialAction(Icons.Default.ContentCopy, "Duplicate") { onDuplicate() },
                            RadialAction(Icons.Default.Delete, "Delete") { showConfirmDelete = true }
                        )

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        val lpJob = scope.launch {
                            delay(viewConfig.longPressTimeoutMillis)
                            menuTriggered = true
                            showRadialMenu(RadialMenuState(menuCenter, actions, -1, rule.id))
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                val change = event.changes.find { it.id == down.id }

                                if (menuTriggered) {
                                    change?.consume()
                                    if (change != null) {
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(menuCenter, actions, bestIndex, rule.id))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            actions[currentHoveredIndex].onClick()
                                            break
                                        }

                                        val releasePos = change?.position
                                        if (releasePos != null) {
                                            val distance = (releasePos + itemOffsetInRoot - menuCenter).getDistance()
                                            val centerThreshold = with(density) { 70.dp.toPx() }
                                            if (distance < centerThreshold) {
                                                dismissOnExit = false
                                                break
                                            }
                                        }

                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        onToggle(!rule.isEnabled)
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                }
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                EventIconDisplay(rule.icon, displaySize = 42.dp, grayscale = !rule.isEnabled)
                Spacer(Modifier.width(16.dp))
                Text(rule.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (rule.isEnabled) Color.White else Color.Gray)
                Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
            }
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = rule.title,
            onConfirm = { isDeletingAnimating = true; showConfirmDelete = false },
            onDismiss = { showConfirmDelete = false }
        )
    }
}



@Composable
fun EditAnimationContent(progress: Float) {
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Fallback to screen size if item size is not yet available,
        // or just use 360x86 as a safe estimate for the stripe item
        val w = 360f * density.density
        val h = 86f * density.density
        val centerX = w / 2f
        val centerY = h / 2f

        val moveProgress = (progress / 0.4f).coerceIn(0f, 1f)
        val jump1Progress = ((progress - 0.4f) / 0.2f).coerceIn(0f, 1f)
        val jump2Progress = ((progress - 0.6f) / 0.2f).coerceIn(0f, 1f)
        val waveProgress = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)

        val startX = 40.dp.value * density.density
        val currentX = androidx.compose.ui.util.lerp(startX, centerX, moveProgress)

        val jh = 60f * density.density
        val yOffset = if (jump1Progress > 0f && jump1Progress < 1f) {
            -(jh * Math.sin(Math.PI * jump1Progress.toDouble()).toFloat())
        } else if (jump2Progress > 0f && jump2Progress < 1f) {
            -(jh * Math.sin(Math.PI * jump2Progress.toDouble()).toFloat())
        } else 0f

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (moveProgress > 0f) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(startX, centerY),
                    end = Offset(currentX, centerY),
                    strokeWidth = 12f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            if (progress > 0.6f) {
                val blotchAlpha = 0.6f * (1f - waveProgress)
                drawCircle(Color.Gray.copy(alpha = blotchAlpha), radius = 35f, center = Offset(centerX, centerY))
                drawCircle(Color.Gray.copy(alpha = blotchAlpha), radius = 18f, center = Offset(centerX - 30f, centerY + 15f))
                drawCircle(Color.Gray.copy(alpha = blotchAlpha), radius = 22f, center = Offset(centerX + 25f, centerY - 10f))
                drawCircle(Color.Gray.copy(alpha = blotchAlpha), radius = 12f, center = Offset(centerX + 5f, centerY + 30f))
            }
            if (waveProgress > 0f) {
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.4f * (1f - waveProgress)),
                    radius = waveProgress * w * 4f,
                    center = Offset(centerX, centerY)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(currentX.roundToInt() - 24.dp.toPx().toInt(), (centerY + yOffset).roundToInt() - 24.dp.toPx().toInt()) }
                .graphicsLayer {
                    alpha = 1f - (waveProgress * 2f).coerceIn(0f, 1f)
                    rotationZ = 45f + (progress * 180f)
                    scaleX = 1.4f
                    scaleY = 1.4f
                }
        ) {
            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}





@Composable
fun RectangleEventItem(
    rule: EventRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    var showConfirmDelete by remember { mutableStateOf(false) }
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    val showRadialMenu = LocalRadialMenu.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { itemOffsetInRoot = it.positionInRoot() }
                .graphicsLayer {
                    val activeMenu = radialMenuState
                    if (activeMenu != null && activeMenu.sourceId != rule.id) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            20f, 20f, android.graphics.Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                        alpha = 0.6f
                    }
                }
                .pointerInput(rule, itemOffsetInRoot) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val menuCenter = down.position + itemOffsetInRoot
                        val actions = listOf(
                            RadialAction(if (rule.isEnabled) Icons.Default.Block else Icons.Default.CheckCircle, "Toggle") { onToggle(!rule.isEnabled) },
                            RadialAction(Icons.Default.Edit, "Edit") { onEdit() },
                            RadialAction(Icons.Default.ContentCopy, "Duplicate") { onDuplicate() },
                            RadialAction(Icons.Default.Delete, "Delete") { showConfirmDelete = true }
                        )

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        val lpJob = scope.launch {
                            delay(viewConfig.longPressTimeoutMillis)
                            menuTriggered = true
                            showRadialMenu(RadialMenuState(menuCenter, actions, -1, rule.id))
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                val change = event.changes.find { it.id == down.id }

                                if (menuTriggered) {
                                    change?.consume()
                                    if (change != null) {
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(menuCenter, actions, bestIndex, rule.id))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            actions[currentHoveredIndex].onClick()
                                            break
                                        }

                                        val releasePos = change?.position
                                        if (releasePos != null) {
                                            val distance = (releasePos + itemOffsetInRoot - menuCenter).getDistance()
                                            val centerThreshold = with(density) { 70.dp.toPx() }
                                            if (distance < centerThreshold) {
                                                dismissOnExit = false
                                                break
                                            }
                                        }

                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        onToggle(!rule.isEnabled)
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = if (rule.isEnabled) Color(0xFF2B2B2B) else Color(0xFF101010),
            tonalElevation = if (rule.isEnabled) 2.dp else 0.dp,
            border = if (!rule.isEnabled) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)) else null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EventIconDisplay(
                    rule.icon,
                    displaySize = 64.dp,
                    modifier = Modifier.fillMaxWidth(0.67f).aspectRatio(1f),
                    grayscale = !rule.isEnabled
                )
                Text(
                    rule.title,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp),
                    color = if (rule.isEnabled) Color.White else Color.Gray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                if (!rule.isEnabled) {
                    Text(stringResource(R.string.state_off_label), style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = rule.title,
            onConfirm = { onDelete(); showConfirmDelete = false },
            onDismiss = { showConfirmDelete = false }
        )
    }
}



@Composable
fun ConfirmDeleteDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.delete_event_q), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.delete_event_msg, title), color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)) { Text(stringResource(R.string.delete_btn)) }
                }
            }
        }
    }
}





data class RadialAction(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val onClick: () -> Unit)

private fun calculateRadialHover(
    currentPos: Offset,
    menuCenter: Offset,
    actions: List<RadialAction>,
    density: Density,
    screenWidthPx: Float,
    screenHeightPx: Float
): Int {
    val dirX = screenWidthPx / 2f - menuCenter.x
    val dirY = screenHeightPx / 2f - menuCenter.y
    val baseAngleDeg = Math.toDegrees(Math.atan2(dirY.toDouble(), dirX.toDouble())).toFloat()

    val radiusPx = with(density) { 100.dp.toPx() }
    val buttonRadiusPx = with(density) { 28.dp.toPx() }
    val triggerRadiusPx = buttonRadiusPx * 1.6f

    actions.forEachIndexed { index, _ ->
        val step = 140f / 3f
        val span = (actions.size - 1) * step
        val startAngle = baseAngleDeg - span / 2f
        val angle = startAngle + (index * step)

        val offsetX = (radiusPx * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val offsetY = (radiusPx * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()

        val buttonCenter = Offset(menuCenter.x + offsetX, menuCenter.y + offsetY)
        if ((currentPos - buttonCenter).getDistance() < triggerRadiusPx) return index
    }
    return -1
}

@Composable
fun RadialActionMenu(state: RadialMenuState, onDismiss: () -> Unit) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val setRadialMenu = LocalRadialMenu.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var currentHovered = -1
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                            val change = event.changes.firstOrNull()

                            if (change != null) {
                                val bestIndex = calculateRadialHover(
                                    change.position,
                                    state.center,
                                    state.actions,
                                    density,
                                    screenWidthPx,
                                    screenHeightPx
                                )
                                if (bestIndex != currentHovered) {
                                    currentHovered = bestIndex
                                    setRadialMenu(state.copy(hoveredIndex = bestIndex))
                                }

                                if (isReleased) {
                                    if (bestIndex != -1) {
                                        state.actions[bestIndex].onClick()
                                        onDismiss()
                                    } else {
                                        onDismiss()
                                    }
                                    break
                                }
                            } else if (isReleased) {
                                onDismiss()
                                break
                            }
                        }
                    } finally {
                        // Cleanup if needed
                    }
                }
            }
    ) {
        val center = state.center
        val actions = state.actions
        val dirX = screenWidthPx / 2f - center.x
        val dirY = screenHeightPx / 2f - center.y
        val baseAngleDeg = Math.toDegrees(Math.atan2(dirY.toDouble(), dirX.toDouble())).toFloat()

        var startAnim by remember { mutableStateOf(false) }
        LaunchedEffect(state.center) { startAnim = true }

        val currentRadius by animateDpAsState(
            targetValue = if (startAnim) 100.dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )

        // Background dim & blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .graphicsLayer {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        25f, 25f, android.graphics.Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                }
        )

        actions.forEachIndexed { index, action ->
            val isHovered = state.hoveredIndex == index
            val step = 140f / 3f
            val span = (actions.size - 1) * step
            val startAngle = baseAngleDeg - span / 2f
            val angle = startAngle + (index * step)

            val offsetX = (currentRadius.value * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
            val offsetY = (currentRadius.value * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()

            val animatedScale by animateFloatAsState(
                targetValue = if (startAnim) (if (isHovered) 1.25f else 1f) else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            val bgColor by animateColorAsState(if (isHovered) Color.White else Color(0xFF2A2A2A))
            val iconColor by animateColorAsState(if (isHovered) Color.Black else Color.White)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x + offsetX.dp.toPx()).roundToInt() - 28.dp.toPx().toInt(),
                            (center.y + offsetY.dp.toPx()).roundToInt() - 28.dp.toPx().toInt()
                        )
                    }
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = if (startAnim) 1f else 0f
                    }
                    .background(bgColor, CircleShape)
                    .border(1.dp, if (isHovered) Color.White else Color.Gray.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onEditMacro: (Macro) -> Unit,
    onShowAccessibilityError: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TypewriterTitle(targetText = stringResource(R.string.nav_macros)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .drawBehind {
                        val blurHeight = 20.dp.toPx()
                        drawRect(color = Color.Black, size = size)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    }
                    .graphicsLayer {
                        if (radialMenuState != null) {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                20f, 20f, android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                            alpha = 0.6f
                        }
                    },
                actions = {
                    IconButton(onClick = {
                        val nextMode = if (keyMap.macrosViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                        scope.launch { Config.setMacrosViewMode(ctx, nextMode) }
                    }) {
                        Icon(if (keyMap.macrosViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView, null)
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier
                .padding(bottom = 120.dp)
                .graphicsLayer {
                    if (radialMenuState != null) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            20f, 20f, android.graphics.Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                        alpha = 0.6f
                    }
                }
            ) {
                FloatingActionButton(
                    onClick = { showAddMenu = true },
                    containerColor = Color.Gray,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_macro))
                }
                
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A))
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.macro_type_tap), color = Color.White) },
                        onClick = { 
                            showAddMenu = false
                            if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                val intent = Intent(ctx, EssentialKeyService::class.java).apply { 
                                    action = "com.HojuNK.essent_all.START_MACRO_REC"
                                    putExtra("mode", MacroRecordingMode.SINGLE_TAP.name)
                                }
                                ctx.startService(intent)
                            } else {
                                onShowAccessibilityError()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.macro_type_swipe), color = Color.White) },
                        onClick = { 
                            showAddMenu = false
                            if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                val intent = Intent(ctx, EssentialKeyService::class.java).apply { 
                                    action = "com.HojuNK.essent_all.START_MACRO_REC"
                                    putExtra("mode", MacroRecordingMode.SINGLE_SWIPE.name)
                                }
                                ctx.startService(intent)
                            } else {
                                onShowAccessibilityError()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.macro_type_combo), color = Color.White) },
                        onClick = { 
                            showAddMenu = false
                            if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                val intent = Intent(ctx, EssentialKeyService::class.java).apply { 
                                    action = "com.HojuNK.essent_all.START_MACRO_REC"
                                    putExtra("mode", MacroRecordingMode.COMBINATION.name)
                                }
                                ctx.startService(intent)
                            } else {
                                onShowAccessibilityError()
                            }
                        }
                    )
                }
            }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize()) {
            if (keyMap.macros.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.macros_empty_msg), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else if (keyMap.macrosViewMode == EventsViewMode.STRIPES) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(keyMap.macros, key = { it.id }) { macro ->
                        StripeMacroItem(
                            macro = macro,
                            onToggle = { enabled ->
                                scope.launch { Config.updateMacro(ctx, macro.copy(isEnabled = enabled)) }
                            },
                            onEdit = { onEditMacro(macro) },
                            onDuplicate = {
                                val newMacro = macro.copy(id = UUID.randomUUID().toString(), title = "${macro.title}${ctx.getString(R.string.copy_suffix)}")
                                scope.launch { Config.updateMacro(ctx, newMacro) }
                            },
                            onDelete = {
                                scope.launch { Config.deleteMacro(ctx, macro.id) }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(keyMap.macros, key = { it.id }) { macro ->
                        RectangleMacroItem(
                            macro = macro,
                            onToggle = { enabled ->
                                scope.launch { Config.updateMacro(ctx, macro.copy(isEnabled = enabled)) }
                            },
                            onEdit = { onEditMacro(macro) },
                            onDuplicate = {
                                val newMacro = macro.copy(id = UUID.randomUUID().toString(), title = "${macro.title}${ctx.getString(R.string.copy_suffix)}")
                                scope.launch { Config.updateMacro(ctx, newMacro) }
                            },
                            onDelete = {
                                scope.launch { Config.deleteMacro(ctx, macro.id) }
                            }
                        )
                    }
                }
            }
        }
    }
}





@Composable
fun StripeMacroItem(
    macro: Macro,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    var isDeletingAnimating by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    val itemHeight = remember { Animatable(86f) }

    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    val showRadialMenu = LocalRadialMenu.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(isDeletingAnimating) {
        if (isDeletingAnimating) {
            itemHeight.animateTo(0f, animationSpec = tween(500))
            onDelete()
        }
    }

    if (itemHeight.value <= 0.1f && isDeletingAnimating) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight.value.dp)
            .graphicsLayer {
                val activeMenu = radialMenuState
                if (activeMenu != null && activeMenu.sourceId != macro.id) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        20f, 20f, android.graphics.Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                    alpha = 0.6f
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .onGloballyPositioned { itemOffsetInRoot = it.positionInRoot() }
            .background(if (isDeletingAnimating) Color.Red else Color(0xFF1A1A1A))
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(macro, itemOffsetInRoot) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val menuCenter = down.position + itemOffsetInRoot
                        val actions = listOf(
                            RadialAction(Icons.Default.Edit, "Edit") { onEdit() },
                            RadialAction(Icons.Default.ContentCopy, "Duplicate") { onDuplicate() },
                            RadialAction(Icons.Default.Delete, "Delete") { showConfirmDelete = true }
                        )

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        val lpJob = scope.launch {
                            delay(viewConfig.longPressTimeoutMillis)
                            menuTriggered = true
                            showRadialMenu(RadialMenuState(menuCenter, actions, -1, macro.id))
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                val change = event.changes.find { it.id == down.id }

                                if (menuTriggered) {
                                    change?.consume()
                                    if (change != null) {
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(menuCenter, actions, bestIndex, macro.id))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            actions[currentHoveredIndex].onClick()
                                            break
                                        }

                                        val releasePos = change?.position
                                        if (releasePos != null) {
                                            val distance = (releasePos + itemOffsetInRoot - menuCenter).getDistance()
                                            val centerThreshold = with(density) { 70.dp.toPx() }
                                            if (distance < centerThreshold) {
                                                dismissOnExit = false
                                                break
                                            }
                                        }

                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        onEdit()
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                }
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                EventIconDisplay(macro.icon, displaySize = 42.dp)
                Spacer(Modifier.width(16.dp))
                Text(macro.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = macro.title,
            onConfirm = { isDeletingAnimating = true; showConfirmDelete = false },
            onDismiss = { showConfirmDelete = false }
        )
    }
}



@Composable
fun RectangleMacroItem(
    macro: Macro,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    var showConfirmDelete by remember { mutableStateOf(false) }
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    val showRadialMenu = LocalRadialMenu.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { itemOffsetInRoot = it.positionInRoot() }
                .graphicsLayer {
                    val activeMenu = radialMenuState
                    if (activeMenu != null && activeMenu.sourceId != macro.id) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            20f, 20f, android.graphics.Shader.TileMode.DECAL
                        ).asComposeRenderEffect()
                        alpha = 0.6f
                    }
                }
                .pointerInput(macro, itemOffsetInRoot) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val menuCenter = down.position + itemOffsetInRoot
                        val actions = listOf(
                            RadialAction(Icons.Default.Edit, "Edit") { onEdit() },
                            RadialAction(Icons.Default.ContentCopy, "Duplicate") { onDuplicate() },
                            RadialAction(Icons.Default.Delete, "Delete") { showConfirmDelete = true }
                        )

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        val lpJob = scope.launch {
                            delay(viewConfig.longPressTimeoutMillis)
                            menuTriggered = true
                            showRadialMenu(RadialMenuState(menuCenter, actions, -1, macro.id))
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                val change = event.changes.find { it.id == down.id }

                                if (menuTriggered) {
                                    change?.consume()
                                    if (change != null) {
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(menuCenter, actions, bestIndex, macro.id))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            actions[currentHoveredIndex].onClick()
                                            break
                                        }

                                        val releasePos = change?.position
                                        if (releasePos != null) {
                                            val distance = (releasePos + itemOffsetInRoot - menuCenter).getDistance()
                                            val centerThreshold = with(density) { 70.dp.toPx() }
                                            if (distance < centerThreshold) {
                                                dismissOnExit = false
                                                break
                                            }
                                        }

                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        onEdit()
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EventIconDisplay(
                    macro.icon,
                    displaySize = 64.dp,
                    modifier = Modifier.fillMaxWidth(0.67f).aspectRatio(1f)
                )
                Text(
                    macro.title,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = macro.title,
            onConfirm = { onDelete(); showConfirmDelete = false },
            onDismiss = { showConfirmDelete = false }
        )
    }
}



@Composable
fun MacroSelectionSettings(
    currentId: String,
    macroMode: MacroMode,
    macros: List<Macro>,
    onSelect: (String) -> Unit,
    onUpdateMode: (MacroMode) -> Unit
) {
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.macro_execution_mode), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUpdateMode(mode) }) {
                        RadioButton(selected = macroMode == mode, onClick = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(mode.labelRes))
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            Text(stringResource(R.string.select_macro), style = MaterialTheme.typography.labelLarge)
            if (macros.isEmpty()) {
                Text(stringResource(R.string.macros_empty), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            macros.forEach { macro ->
                val isSelected = currentId == macro.id
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(macro.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    EventIconDisplay(macro.icon, displaySize = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(macro.title, modifier = Modifier.weight(1f), color = if (isSelected) Color.White else Color.Gray)
                    RadioButton(selected = isSelected, onClick = null)
                }
            }
        }
    }
}





@Composable
fun EventIconDisplay(
    icon: String,
    displaySize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    grayscale: Boolean = false
) {
    val ctx = LocalContext.current
    var appIcon by remember(icon) { mutableStateOf<ImageBitmap?>(null) }

    // Check if it's a material icon
    val materialIcon = remember(icon) {
        if (icon.startsWith("material:")) {
            val name = icon.removePrefix("material:")
            allMaterialIcons.find { it.first == name }?.second
        } else null
    }

    LaunchedEffect(icon) {
        if (icon.contains(".") && !icon.startsWith("material:")) {
            try {
                val pm = ctx.packageManager
                appIcon = pm.getApplicationIcon(icon).toBitmap(256, 256).asImageBitmap()
            } catch (e: Exception) { appIcon = null }
        } else {
            appIcon = null
        }
    }

    // Powerful Grayscale: -100% saturation for everything (icons, symbols, emojis)
    val filterModifier = if (grayscale) {
        val matrix = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
        Modifier
            .graphicsLayer { alpha = 0.35f } // Even darker
            .drawWithCache {
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(matrix)
                }
                onDrawWithContent {
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
                        drawContent()
                        canvas.restore()
                    }
                }
            }
    } else Modifier

    val finalModifier = if (modifier == Modifier) Modifier.size(displaySize) else modifier

    BoxWithConstraints(modifier = finalModifier.then(filterModifier), contentAlignment = Alignment.Center) {
        if (materialIcon != null) {
            Icon(
                imageVector = materialIcon,
                contentDescription = null,
                tint = if (grayscale) Color.Gray else Color.White,
                modifier = Modifier.fillMaxSize()
            )
        } else if (appIcon != null) {
            Image(
                bitmap = appIcon!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val effectiveSize = if (modifier == Modifier) displaySize else this.maxWidth
            Text(
                icon,
                fontSize = (effectiveSize.value * 0.75f).sp,
                color = if (grayscale) Color.Gray else Color.White
            )
        }
    }
}





@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MacroAddPanel(
    initialMacro: Macro,
    apps: List<AppInfo>?,
    onDismiss: () -> Unit,
    onSave: (Macro) -> Unit
) {
    var currentMacro by rememberSaveable { mutableStateOf(initialMacro) }
    var undoStack by remember { mutableStateOf(listOf<Macro>()) }
    var redoStack by remember { mutableStateOf(listOf<Macro>()) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    fun pushToUndo() {
        undoStack = undoStack + currentMacro
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + currentMacro
            currentMacro = undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + currentMacro
            currentMacro = redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> StandardMacroEditor(
                        macro = currentMacro,
                        apps = apps,
                        onMacroChange = {
                            pushToUndo()
                            currentMacro = it
                        },
                        onDismiss = onDismiss,
                        onSave = { onSave(currentMacro) }
                    )
                    1 -> ProfessionalMacroEditor(
                        macro = currentMacro,
                        apps = apps,
                        onMacroChange = {
                            pushToUndo()
                            currentMacro = it
                        },
                        onUndo = ::undo,
                        onRedo = ::redo,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        onDismiss = onDismiss,
                        onSave = { onSave(currentMacro) }
                    )
                }
            }
        }
    }
}

@Composable
fun StandardMacroEditor(
    macro: Macro,
    apps: List<AppInfo>?,
    onMacroChange: (Macro) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var showIconPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Header animation logic
    val collapseProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset.toFloat() / 200f).coerceIn(0f, 1f)
        }
    }
    val isCollapsed by remember {
        derivedStateOf { collapseProgress > 0.1f }
    }

    var editingActionIndex by remember { mutableIntStateOf(-1) }
    var showTouchPicker by remember { mutableStateOf(false) }
    var showSwipePicker by remember { mutableStateOf(false) }
    var showPreciseSwipePicker by remember { mutableStateOf(false) }
    var showDelayPickerInList by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var showFullAppPicker by remember { mutableStateOf(false) }

    var showActionPicker by remember { mutableStateOf(false) }
    var showBrightnessPicker by remember { mutableStateOf(false) }
    var showVolumePicker by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showUrlPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showWaitConfirmationPicker by remember { mutableStateOf(false) }
    var showAutoBrightnessPicker by remember { mutableStateOf(false) }

    // Stable keys for actions reordering
    var actionIds by rememberSaveable { mutableStateOf(List(macro.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != macro.actions.size) {
        if (actionIds.size < macro.actions.size) {
            actionIds = actionIds + List(macro.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(macro.actions.size)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fixed Header Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f)
                .drawBehind {
                    val blurHeight = 60.dp.toPx()
                    drawRect(color = Color.Black, size = size)
                    if (collapseProgress > 0f) {
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = collapseProgress), Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height),
                            size = androidx.compose.ui.geometry.Size(size.width, blurHeight)
                        )
                    }
                }
                .padding(top = 28.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundHeaderButton(icon = Icons.Default.Close, onClick = onDismiss)

                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "НАСТРОЙКА МАКРОСА",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            alpha = collapseProgress
                            scaleX = 0.8f + (collapseProgress * 0.2f)
                            scaleY = 0.8f + (collapseProgress * 0.2f)
                        }
                    )
                }

                RoundHeaderButton(
                    icon = Icons.Default.Check,
                    onClick = { if (macro.title.isNotEmpty()) onSave() },
                    backgroundColor = Color(0xFF2B2B2B),
                    borderColor = null
                )
            }

            AnimatedVisibility(
                visible = isCollapsed,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .graphicsLayer { alpha = collapseProgress },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.75f)
                                .fillMaxHeight()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                .clickable { scope.launch { scrollState.animateScrollToItem(0) } }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = macro.title.ifEmpty { "Макрос без названия" },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.25f)
                                .fillMaxHeight()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                .clickable { showIconPicker = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EventIconDisplay(macro.icon.ifEmpty { "🤖" }, displaySize = 24.dp)
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            item {
                // Dynamic spacer to push content below the expanded fixed header
                Spacer(Modifier.height(120.dp * collapseProgress))
            }

            item {
                Column(modifier = Modifier.graphicsLayer {
                    alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                    translationY = -collapseProgress * 40f
                }) {
                    Text(
                        text = "НАСТРОЙКА\nМАКРОСА",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .graphicsLayer {
                            alpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
                            translationY = -collapseProgress * 30f
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight()
                            .background(Color.Black, RoundedCornerShape(24.dp))
                            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Название макроса", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = macro.title,
                                onValueChange = { onMacroChange(macro.copy(title = it)) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (macro.title.isEmpty()) {
                                        Text("Макрос без названия", color = Color.Gray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    LabeledPanel(
                        label = "Значёк",
                        value = macro.icon.ifEmpty { "🤖" },
                        modifier = Modifier.weight(0.25f).fillMaxHeight(),
                        onClick = { showIconPicker = true }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            item {
                Text(stringResource(R.string.macro_actions_label), color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
            }

            itemsIndexed(macro.actions, key = { index, _ -> actionIds.getOrElse(index) { index.toString() } }) { index, action ->
                ActionRow(
                    action = action,
                    index = index,
                    apps = apps ?: emptyList(),
                    isDragged = draggedIndex == index,
                    onDragStart = { draggedIndex = index },
                    onDragEnd = { draggedIndex = -1 },
                    onEdit = {
                        editingActionIndex = index
                        when {
                            action.startsWith(Config.ACTION_TOUCH_PREFIX) -> showTouchPicker = true
                            action.startsWith(Config.ACTION_SWIPE_PREFIX) -> showSwipePicker = true
                            action.startsWith(Config.ACTION_PATH_PREFIX) -> showPreciseSwipePicker = true
                            action.startsWith(Config.ACTION_DELAY_PREFIX) -> showDelayPickerInList = true
                            else -> showFullAppPicker = true
                        }
                    },
                    onMove = { from, to ->
                        if (to in macro.actions.indices) {
                            val newList = macro.actions.toMutableList()
                            val item = newList.removeAt(from)
                            newList.add(to, item)

                            val newIds = actionIds.toMutableList()
                            val id = newIds.removeAt(from)
                            newIds.add(to, id)

                            onMacroChange(macro.copy(actions = newList))
                            actionIds = newIds
                            draggedIndex = to
                        }
                    },
                    onDelete = {
                        val newList = macro.actions.toMutableList().apply { removeAt(index) }
                        val newIds = actionIds.toMutableList().apply { removeAt(index) }
                        onMacroChange(macro.copy(actions = newList))
                        actionIds = newIds
                        if (editingActionIndex == index) editingActionIndex = -1
                    }
                )

                Spacer(Modifier.height(8.dp))
                if (index < macro.actions.size - 1) {
                    ConnectingLine()
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Spacer(Modifier.height(12.dp))

                // First Row: Tap & Swipe
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelAddButton(
                        label = stringResource(R.string.touch_btn),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_TOUCH_PREFIX}540_1100_200"
                            val newList = macro.actions + action
                            onMacroChange(macro.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showTouchPicker = true
                        }
                    )
                    PanelAddButton(
                        label = stringResource(R.string.swipe_btn),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_SWIPE_PREFIX}300_1100_700_1100_0_0"
                            val newList = macro.actions + action
                            onMacroChange(macro.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showSwipePicker = true
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Second Row: Precise Swipe & Delay
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelAddButton(
                        label = "ТОЧНЫЙ С.",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_PATH_PREFIX}540,1100,0;540,1300,500"
                            val newList = macro.actions + action
                            onMacroChange(macro.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showPreciseSwipePicker = true
                        }
                    )
                    PanelAddButton(
                        label = "ЗАДЕРЖКА",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_DELAY_PREFIX}0.5"))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Third Row: Wide Action Button
                PanelAddButton(
                    label = "ДОБАВИТЬ ДЕЙСТВИЕ",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showActionPicker = true }
                )

                Spacer(Modifier.height(120.dp))
            }
        }

        if (showActionPicker) {
            EventActionPickerDialog(
                triggerGesture = TapSlot.SINGLE,
                allowMacros = false,
                onDismiss = { showActionPicker = false },
                onPick = { pkg ->
                    when (pkg) {
                        "action:pick_touch" -> { editingActionIndex = -1; showTouchPicker = true }
                        "action:pick_swipe" -> { editingActionIndex = -1; showSwipePicker = true }
                        "action:pick_delay" -> { editingActionIndex = -1; showDelayPickerInList = true }
                        "action:pick_brightness" -> showBrightnessPicker = true
                        "action:pick_volume" -> showVolumePicker = true
                        "action:pick_media" -> showMediaPicker = true
                        "action:pick_url" -> showUrlPicker = true
                        "action:pick_theme" -> showThemePicker = true
                        "action:pick_wait_confirmation" -> showWaitConfirmationPicker = true
                        "action:pick_auto_brightness" -> showAutoBrightnessPicker = true
                        "action:full_app_list" -> showFullAppPicker = true
                        else -> if (pkg != null) onMacroChange(macro.copy(actions = macro.actions + pkg))
                    }
                    showActionPicker = false
                }
            )
        }

        if (showBrightnessPicker) {
            BrightnessPickerDialog(initialValue = 50, onDismiss = { showBrightnessPicker = false }, onConfirmed = { b ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_BRIGHTNESS_PREFIX}$b"))
                showBrightnessPicker = false
            })
        }
        if (showVolumePicker) {
            VolumePickerDialog(onDismiss = { showVolumePicker = false }, onConfirmed = { v ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_VOLUME_PREFIX}$v"))
                showVolumePicker = false
            })
        }
        if (showMediaPicker) {
            MediaPickerDialog(onDismiss = { showMediaPicker = false }, onConfirmed = { m ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_MEDIA_PREFIX}$m"))
                showMediaPicker = false
            })
        }
        if (showUrlPicker) {
            UrlPickerDialog(initialValue = "", onDismiss = { showUrlPicker = false }, onConfirmed = { u ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_URL_PREFIX}$u"))
                showUrlPicker = false
            })
        }
        if (showThemePicker) {
            ThemePickerDialog(onDismiss = { showThemePicker = false }, onConfirmed = { t ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_THEME_PREFIX}$t"))
                showThemePicker = false
            })
        }
        if (showWaitConfirmationPicker) {
            WaitConfirmationDialog(initialValue = null, onDismiss = { showWaitConfirmationPicker = false }, onConfirm = { w ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_WAIT_CONFIRMATION_PREFIX}$w"))
                showWaitConfirmationPicker = false
            })
        }
        if (showAutoBrightnessPicker) {
            AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessPicker = false }, onConfirmed = { a ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$a"))
                showAutoBrightnessPicker = false
            })
        }

        if (showPreciseSwipePicker && editingActionIndex != -1) {
            PreciseSwipePickerDialog(
                initialValue = macro.actions[editingActionIndex].removePrefix(Config.ACTION_PATH_PREFIX),
                onDismiss = { showPreciseSwipePicker = false; editingActionIndex = -1 },
                onConfirmed = { path ->
                    val newList = macro.actions.toMutableList()
                    newList[editingActionIndex] = "${Config.ACTION_PATH_PREFIX}$path"
                    onMacroChange(macro.copy(actions = newList))
                    showPreciseSwipePicker = false; editingActionIndex = -1
                }
            )
        }

        if (showTouchPicker) {
            val existing = macro.actions.getOrNull(editingActionIndex)
            TouchPointPickerDialog(
                initialValue = existing?.removePrefix(Config.ACTION_TOUCH_PREFIX),
                onDismiss = { showTouchPicker = false; editingActionIndex = -1 },
                onConfirmed = { x, y, hold ->
                    val action = "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold"
                    val newList = macro.actions.toMutableList()
                    if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                        newList[editingActionIndex] = action
                    } else {
                        newList.add(action)
                    }
                    onMacroChange(macro.copy(actions = newList))
                    showTouchPicker = false; editingActionIndex = -1
                }
            )
        }
        if (showSwipePicker) {
            val existing = macro.actions.getOrNull(editingActionIndex)
            SwipePointPickerDialog(
                initialValue = existing?.removePrefix(Config.ACTION_SWIPE_PREFIX),
                onDismiss = { showSwipePicker = false; editingActionIndex = -1 },
                onConfirmed = { x1, y1, x2, y2, hS, hE ->
                    val action = "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE"
                    val newList = macro.actions.toMutableList()
                    if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                        newList[editingActionIndex] = action
                    } else {
                        newList.add(action)
                    }
                    onMacroChange(macro.copy(actions = newList))
                    showSwipePicker = false; editingActionIndex = -1
                }
            )
        }

        if (showDelayPickerInList) {
            val existing = macro.actions.getOrNull(editingActionIndex)
            DelayPickerDialog(
                initialValue = existing?.removePrefix(Config.ACTION_DELAY_PREFIX),
                onDismiss = { showDelayPickerInList = false; editingActionIndex = -1 },
                onConfirmed = { sec ->
                    val action = "${Config.ACTION_DELAY_PREFIX}$sec"
                    val newList = macro.actions.toMutableList()
                    if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                        newList[editingActionIndex] = action
                    }
                    onMacroChange(macro.copy(actions = newList))
                    showDelayPickerInList = false; editingActionIndex = -1
                }
            )
        }

        if (showFullAppPicker || (editingActionIndex != -1 && editingActionIndex < macro.actions.size && !macro.actions[editingActionIndex].startsWith("action:"))) {
            val selected = if (editingActionIndex != -1 && editingActionIndex < macro.actions.size) listOf(macro.actions[editingActionIndex]) else emptyList()
            UniversalAppPickerDialog(
                apps = (apps ?: emptyList()).filter { it.isLaunchable },
                selected = selected,
                multiSelect = false,
                onConfirmed = { picked ->
                    val pkg = picked.firstOrNull()
                    if (pkg != null) {
                        val newList = macro.actions.toMutableList()
                        if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                            newList[editingActionIndex] = pkg
                        } else {
                            newList.add(pkg)
                        }
                        onMacroChange(macro.copy(actions = newList))
                    }
                    showFullAppPicker = false; editingActionIndex = -1
                },
                onDismiss = { showFullAppPicker = false; editingActionIndex = -1 }
            )
        }

        if (showIconPicker) {
            EventIconPickerDialog(
                currentIcon = macro.icon,
                apps = apps,
                onDismiss = { showIconPicker = false },
                onIconPicked = { icon ->
                    onMacroChange(macro.copy(icon = icon))
                    showIconPicker = false
                }
            )
        }
    }
}

data class MacroActionTime(
    val id: String,
    val index: Int,
    val type: String,
    val startTime: Long,
    val duration: Long
)

private fun calculateMacroTimeline(actions: List<String>): List<MacroActionTime> {
    val timeline = mutableListOf<MacroActionTime>()
    var currentTime = 0L
    val gap = 150L

    actions.forEachIndexed { index, action ->
        val duration = when {
            action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                val parts = action.removePrefix(Config.ACTION_TOUCH_PREFIX).split("_")
                if (parts.size >= 3) (parts[2].toLongOrNull() ?: 200L).coerceAtLeast(50L) else 200L
            }
            action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                val parts = action.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
                if (parts.size >= 7) (parts[6].toLongOrNull() ?: 350L).coerceAtLeast(100L) else 350L
            }
            action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                val pathData = action.removePrefix(Config.ACTION_PATH_PREFIX)
                val pts = pathData.split(";").filter { it.isNotEmpty() }
                if (pts.isNotEmpty()) {
                    val lastTime = pts.last().split(",").getOrNull(2)?.toLongOrNull() ?: 0L
                    if (lastTime <= 0) (pts.size * 30L).coerceIn(200L, 2000L) else lastTime
                } else 500L
            }
            action.startsWith(Config.ACTION_DELAY_PREFIX) -> {
                val sec = action.removePrefix(Config.ACTION_DELAY_PREFIX).toFloatOrNull() ?: 0f
                (sec * 1000).toLong().coerceAtLeast(0L)
            }
            else -> 200L
        }

        // Stable ID based on position and content for this calculation pass
        val stableId = "${index}_${action.hashCode()}"
        timeline.add(MacroActionTime(stableId, index, action, currentTime, duration))
        currentTime += duration + gap
    }
    return timeline
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfessionalMacroEditor(
    macro: Macro,
    apps: List<AppInfo>?,
    onMacroChange: (Macro) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedActionIndex by remember { mutableIntStateOf(-1) }
    var selectedMediaId by remember { mutableStateOf<String?>(null) }
    var showTouchPicker by remember { mutableStateOf(false) }
    var showSwipePicker by remember { mutableStateOf(false) }
    var showDelayPicker by remember { mutableStateOf(false) }
    var showPreciseSwipePicker by remember { mutableStateOf(false) }
    
    // New Action Pickers
    var showActionPicker by remember { mutableStateOf(false) }
    var showMacroPicker by remember { mutableStateOf(false) }
    var showBrightnessPicker by remember { mutableStateOf(false) }
    var showVolumePicker by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showUrlPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showWaitConfirmationPicker by remember { mutableStateOf(false) }
    var showAutoBrightnessPicker by remember { mutableStateOf(false) }
    var showFullAppPickerPro by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    // Playback State
    var isPlaying by remember { mutableStateOf(false) }
    var playbackTime by remember { mutableLongStateOf(0L) }
    var videoSpeed by remember { mutableFloatStateOf(1f) }
    
    // Internal interactive list to prevent jank during drags
    var interactiveActions by remember(macro.actions) { mutableStateOf(macro.actions) }
    var interactiveMedia by remember(macro.mediaItems) { mutableStateOf(macro.mediaItems) }

    val timelineData = remember(interactiveActions) { calculateMacroTimeline(interactiveActions) }
    val totalDuration = timelineData.lastOrNull()?.let { it.startTime + it.duration } ?: 0L

    val timelineScrollState = rememberScrollState()
    var showMediaSpeedDialog by remember { mutableStateOf(false) }
    
    // Preview Styles
    var previewStyle by remember { mutableIntStateOf(0) } // 0: Mock, 1: Wallpaper, 2: Video
    var aspectError by remember { mutableStateOf<String?>(null) }

    // Dynamic Reordering for Media
    fun onMoveMediaOrder(from: Int, to: Int) {
        val newList = macro.mediaItems.toMutableList()
        val item = newList.removeAt(from)
        newList.add(to, item)
        
        // Update start times to maintain sequence
        var currentT = 0L
        val updated = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
        onMacroChange(macro.copy(mediaItems = updated))
    }

    val scope = rememberCoroutineScope()
    val pickWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { sourceUri ->
                scope.launch {
                    try {
                        val fileName = "media_${UUID.randomUUID()}.jpg"
                        val destFile = java.io.File(ctx.filesDir, fileName)
                        ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        
                        val newItem = MediaItem(
                            id = UUID.randomUUID().toString(),
                            type = MediaType.PHOTO,
                            path = destFile.absolutePath,
                            startTime = 0L, 
                            duration = 1000L 
                        )
                        var newList = macro.mediaItems + newItem
                        if (newList.size == 1 && newList[0].type == MediaType.PHOTO) {
                            newList = listOf(newList[0].copy(startTime = 0L, duration = totalDuration.coerceAtLeast(1000L)))
                        } else {
                            var currentT = 0L
                            newList = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                        }
                        onMacroChange(macro.copy(mediaItems = newList))
                        previewStyle = 1
                    } catch (e: Exception) { aspectError = "Ошибка при сохранении изображения" }
                }
            }
        }
    )

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { sourceUri ->
                scope.launch {
                    try {
                        val fileName = "media_${UUID.randomUUID()}.mp4"
                        val destFile = java.io.File(ctx.filesDir, fileName)
                        ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(destFile.absolutePath)
                        val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 5000L
                        retriever.release()

                        val newItem = MediaItem(
                            id = UUID.randomUUID().toString(),
                            type = MediaType.VIDEO,
                            path = destFile.absolutePath,
                            startTime = 0L, 
                            duration = duration 
                        )
                        var newList = macro.mediaItems + newItem
                        var currentT = 0L
                        newList = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                        onMacroChange(macro.copy(mediaItems = newList))
                        previewStyle = 2
                    } catch (e: Exception) { aspectError = "Ошибка при сохранении видео" }
                }
            }
        }
    )

    if (aspectError != null) {
        AlertDialog(
            onDismissRequest = { aspectError = null },
            title = { Text("Ошибка") },
            text = { Text(aspectError!!) },
            confirmButton = { TextButton(onClick = { aspectError = null }) { Text("ОК") } },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startSysTime = System.currentTimeMillis()
            val initialPlaybackTime = playbackTime
            while (isPlaying && playbackTime < totalDuration) {
                val elapsed = System.currentTimeMillis() - startSysTime
                playbackTime = (initialPlaybackTime + elapsed).coerceAtMost(totalDuration)
                if (playbackTime >= totalDuration) {
                    isPlaying = false
                }
                delay(16) // ~60fps
            }
        }
    }

    // Timeline Expand Logic
    var timelineExpanded by remember { mutableStateOf(false) }
    val extraTimelineHeight by animateDpAsState(if (timelineExpanded) 80.dp else 0.dp)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
        // 1. Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
        ) {
            // Left: Close
            RoundHeaderButton(
                icon = Icons.Default.Close, 
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            // Center: Undo/Redo
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (canUndo) Color.White else Color.Gray)
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (canRedo) Color.White else Color.Gray)
                }
            }

            // Right: Save + Time
            Box(
                contentAlignment = Alignment.TopCenter, 
                modifier = Modifier.align(Alignment.TopEnd).width(48.dp)
            ) {
                RoundHeaderButton(
                    icon = Icons.Default.Check,
                    onClick = { if (macro.title.isNotEmpty()) onSave() },
                    backgroundColor = Color(0xFF2B2B2B),
                    borderColor = null
                )
                
                // Rotated Time Counter
                Text(
                    text = String.format("%02d:%02d / %02d:%02d",
                        (playbackTime / 1000) / 60, (playbackTime / 1000) % 60,
                        (totalDuration / 1000) / 60, (totalDuration / 1000) % 60
                    ),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelLarge,
                    softWrap = false,
                    modifier = Modifier
                        .padding(top = 110.dp) // Lowered by ~1/4 of text length
                        .wrapContentWidth(unbounded = true)
                        .graphicsLayer { 
                            rotationZ = 90f
                        }
                )
            }
        }

        // 2. Interactive Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            
            // Left: Action Buttons (Bottom to Top)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 24.dp)
                    .width(48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bottom to Top
                // Delay
                SmallVerticalButton(label = "ЗАДЕРЖКА", onClick = {
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_DELAY_PREFIX}0.5"))
                })
                
                // Action
                SmallVerticalButton(label = "ДЕЙСТВИЕ", onClick = { showActionPicker = true })

                // Precise Swipe
                SmallVerticalButton(label = "ТОЧНЫЙ С.", onClick = {
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_PATH_PREFIX}540,1100,0;540,1300,500"))
                })

                // Swipe
                SmallVerticalButton(label = "СВАЙП", onClick = {
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_SWIPE_PREFIX}300_1100_700_1100_0_0"))
                })

                // Tap
                SmallVerticalButton(label = "НАЖАТИЕ", onClick = {
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_TOUCH_PREFIX}540_1100_200"))
                })
            }

            // Center: Preview Silhouette (Force lift with offset)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp), 
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-55).dp)
                        .aspectOf(9f / 19.5f, scale = 1.1f)
                        .border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF0D0D0D))
                ) {
                    val activeItem = macro.mediaItems.find { 
                        playbackTime >= it.startTime && playbackTime <= (it.startTime + it.duration)
                    }

                    when (previewStyle) {
                        0 -> MockDesktop()
                        1 -> WallpaperPreview(activeItem?.takeIf { it.type == MediaType.PHOTO }?.path)
                        2 -> VideoPreview(
                            localPath = activeItem?.takeIf { it.type == MediaType.VIDEO }?.path,
                            itemStartTime = activeItem?.startTime ?: 0L,
                            playbackTime = playbackTime,
                            isPlaying = isPlaying,
                            speed = videoSpeed
                        )
                    }

                    // Gesture Overlays
                    macro.actions.forEachIndexed { index, action ->
                        val timeData = timelineData.getOrNull(index)
                        val isActive = timeData != null && playbackTime >= timeData.startTime && playbackTime <= (timeData.startTime + timeData.duration)
                        val isSelected = index == selectedActionIndex
                        
                        val progress = if (isActive && timeData != null && timeData.duration > 0) {
                            ((playbackTime - timeData.startTime).toFloat() / timeData.duration).coerceIn(0f, 1f)
                        } else 1f

                        if (!isPlaying || isActive) {
                            GestureMarker(
                                action = action,
                                isSelected = isSelected || isActive,
                                isHighlighted = isActive,
                                progress = progress,
                                onClick = {
                                    selectedActionIndex = index
                                    timeData?.let { playbackTime = it.startTime }
                                    isPlaying = false
                                }
                            )
                        }
                    }
                }
            }

            // Right: Playback and Style Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 24.dp)
                    .width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
            ) {
                // TOP: Media Add Buttons
                Column(
                    modifier = Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { pickWallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.size(36.dp).background(Color(0xFF2B2B2B), CircleShape)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                        modifier = Modifier.size(36.dp).background(Color(0xFF2B2B2B), CircleShape)
                    ) {
                        Icon(Icons.Default.VideoCall, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // MIDDLE: Format Switcher
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(22.dp))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreviewStyleIcon(icon = Icons.Default.Dashboard, isSelected = previewStyle == 0, onClick = { previewStyle = 0 })
                    PreviewStyleIcon(icon = Icons.Default.Wallpaper, isSelected = previewStyle == 1, onClick = { previewStyle = 1 })
                    PreviewStyleIcon(icon = Icons.Default.Videocam, isSelected = previewStyle == 2, onClick = { previewStyle = 2 })
                }

                // BOTTOM: Play/Stop circle
                IconButton(
                    onClick = { 
                        if (playbackTime >= totalDuration) playbackTime = 0
                        isPlaying = !isPlaying 
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFFD700), CircleShape)
                ) {
                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = Color.Black)
                }
            }
        }

        // 3. Bottom: Timeline Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(bottom = 92.dp)
        ) {
            // White Bar (Feature separator & Draggable Handle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -10) timelineExpanded = true
                            if (dragAmount > 10) timelineExpanded = false
                        }
                    }
                    .clickable { timelineExpanded = !timelineExpanded },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
            
            // Secondary Media Timeline (Revealed when expanded)
            if (extraTimelineHeight > 0.dp) {
                Box(modifier = Modifier.fillMaxWidth().height(extraTimelineHeight)) {
                    val itemsToShow = when (previewStyle) {
                        1 -> macro.mediaItems.filter { it.type == MediaType.PHOTO }
                        2 -> macro.mediaItems.filter { it.type == MediaType.VIDEO }
                        else -> emptyList()
                    }
                    
                    MediaTrackTimeline(
                        label = if (previewStyle == 1) "ФОТО" else "ВИДЕО",
                        color = if (previewStyle == 1) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        totalDuration = totalDuration,
                        scrollState = timelineScrollState,
                        mediaItems = itemsToShow,
                        selectedMediaId = selectedMediaId,
                        onSelect = { selectedMediaId = it },
                        onDelete = { id ->
                            val newList = macro.mediaItems.filter { it.id != id }
                            onMacroChange(macro.copy(mediaItems = newList))
                            if (selectedMediaId == id) selectedMediaId = null
                        },
                        onMediaChange = { id, updatedItem ->
                            val newList = macro.mediaItems.map { 
                                if (it.id == id) updatedItem else it 
                            }
                            onMacroChange(macro.copy(mediaItems = newList))
                        },
                        onMoveOrder = { from, to ->
                            val newList = macro.mediaItems.toMutableList()
                            val item = newList.removeAt(from)
                            newList.add(to, item)

                            // Re-calculate times based on order
                            var currentT = 0L
                            val finalItems = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                            onMacroChange(macro.copy(mediaItems = finalItems))
                        },
                        onSpeedClick = if (previewStyle == 2) ({ showMediaSpeedDialog = true }) else null
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AdvancedTimeline(
                timelineData = timelineData,
                scrollState = timelineScrollState,
                playbackTime = playbackTime,
                totalDuration = totalDuration,
                isPlaying = isPlaying,
                selectedIndex = selectedActionIndex,
                onSelect = {
                    selectedActionIndex = it
                    timelineData.getOrNull(it)?.let { data -> playbackTime = data.startTime }
                },
                onEdit = { index ->
                    selectedActionIndex = index
                    val action = macro.actions[index]
                    when {
                        action.startsWith(Config.ACTION_TOUCH_PREFIX) -> showTouchPicker = true
                        action.startsWith(Config.ACTION_SWIPE_PREFIX) -> showSwipePicker = true
                        action.startsWith(Config.ACTION_PATH_PREFIX) -> showPreciseSwipePicker = true
                        action.startsWith(Config.ACTION_DELAY_PREFIX) -> showDelayPicker = true
                    }
                },
                onDelete = { index ->
                    val newList = macro.actions.toMutableList().apply { removeAt(index) }
                    onMacroChange(macro.copy(actions = newList))
                    selectedActionIndex = -1
                },
                onSeek = {
                    playbackTime = it
                    isPlaying = false
                },
                onMove = { from, to ->
                    if (to in macro.actions.indices) {
                        val newList = macro.actions.toMutableList()
                        val item = newList.removeAt(from)
                        newList.add(to, item)
                        onMacroChange(macro.copy(actions = newList))
                        selectedActionIndex = to
                    }
                },
                onDurationChange = { index, newDuration ->
                    val action = macro.actions[index]
                    val newAction = when {
                        action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                            val pts = action.removePrefix(Config.ACTION_TOUCH_PREFIX).split("_")
                            "${Config.ACTION_TOUCH_PREFIX}${pts[0]}_${pts[1]}_$newDuration"
                        }
                        action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                            val pts = action.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
                            val x1 = pts.getOrNull(0) ?: "0"
                            val y1 = pts.getOrNull(1) ?: "0"
                            val x2 = pts.getOrNull(2) ?: "0"
                            val y2 = pts.getOrNull(3) ?: "0"
                            val hS = pts.getOrNull(4) ?: "0"
                            val hE = pts.getOrNull(5) ?: "0"
                            "${Config.ACTION_SWIPE_PREFIX}${x1}_${y1}_${x2}_${y2}_${hS}_${hE}_$newDuration"
                        }
                        action.startsWith(Config.ACTION_DELAY_PREFIX) -> {
                            "${Config.ACTION_DELAY_PREFIX}${newDuration / 1000f}"
                        }
                        else -> action
                    }
                    val newList = macro.actions.toMutableList().apply { set(index, newAction) }
                    onMacroChange(macro.copy(actions = newList))
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showMediaSpeedDialog) {
        SpeedCoefficientDialog(
            currentSpeed = videoSpeed,
            onDismiss = { showMediaSpeedDialog = false },
            onConfirmed = {
                videoSpeed = it
                showMediaSpeedDialog = false
            }
        )
    }

    // Pickers
    if (showActionPicker) {
        EventActionPickerDialog(triggerGesture = TapSlot.SINGLE, allowMacros = false, onPick = { pkg ->
            when (pkg) {
                "action:pick_touch" -> { selectedActionIndex = -1; showTouchPicker = true }
                "action:pick_swipe" -> { selectedActionIndex = -1; showSwipePicker = true }
                "action:pick_delay" -> { selectedActionIndex = -1; showDelayPicker = true }
                "action:pick_macro" -> showMacroPicker = true
                "action:pick_brightness" -> showBrightnessPicker = true
                "action:pick_volume" -> showVolumePicker = true
                "action:pick_media" -> showMediaPicker = true
                "action:pick_url" -> showUrlPicker = true
                "action:pick_theme" -> showThemePicker = true
                "action:pick_wait_confirmation" -> showWaitConfirmationPicker = true
                "action:pick_auto_brightness" -> showAutoBrightnessPicker = true
                "action:full_app_list" -> showFullAppPickerPro = true
                else -> if (pkg != null) onMacroChange(macro.copy(actions = macro.actions + pkg))
            }
            showActionPicker = false
        }, onDismiss = { showActionPicker = false })
    }
    if (showMacroPicker) {
        val currentKeyMap by Config.flow(ctx).collectAsState(initial = KeyMap())
        MacroPickerDialog(macros = currentKeyMap.macros, onDismiss = { showMacroPicker = false }, onMacroPicked = { id ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_MACRO_PREFIX}$id"))
            showMacroPicker = false
        })
    }
    if (showBrightnessPicker) {
        BrightnessPickerDialog(initialValue = 50, onDismiss = { showBrightnessPicker = false }, onConfirmed = { b ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_BRIGHTNESS_PREFIX}$b"))
            showBrightnessPicker = false
        })
    }
    if (showVolumePicker) {
        VolumePickerDialog(onDismiss = { showVolumePicker = false }, onConfirmed = { v ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_VOLUME_PREFIX}$v"))
            showVolumePicker = false
        })
    }
    if (showMediaPicker) {
        MediaPickerDialog(onDismiss = { showMediaPicker = false }, onConfirmed = { m ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_MEDIA_PREFIX}$m"))
            showMediaPicker = false
        })
    }
    if (showUrlPicker) {
        UrlPickerDialog(initialValue = "", onDismiss = { showUrlPicker = false }, onConfirmed = { u ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_URL_PREFIX}$u"))
            showUrlPicker = false
        })
    }
    if (showThemePicker) {
        ThemePickerDialog(onDismiss = { showThemePicker = false }, onConfirmed = { t ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_THEME_PREFIX}$t"))
            showThemePicker = false
        })
    }
    if (showWaitConfirmationPicker) {
        WaitConfirmationDialog(initialValue = null, onDismiss = { showWaitConfirmationPicker = false }, onConfirm = { w ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_WAIT_CONFIRMATION_PREFIX}$w"))
            showWaitConfirmationPicker = false
        })
    }
    if (showAutoBrightnessPicker) {
        AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessPicker = false }, onConfirmed = { a ->
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$a"))
            showAutoBrightnessPicker = false
        })
    }
    if (showFullAppPickerPro) {
        UniversalAppPickerDialog(apps = (apps ?: emptyList()).filter { it.isLaunchable }, selected = emptyList(), multiSelect = false, onConfirmed = { picked ->
            picked.firstOrNull()?.let { pkg -> onMacroChange(macro.copy(actions = macro.actions + pkg)) }
            showFullAppPickerPro = false
        }, onDismiss = { showFullAppPickerPro = false })
    }
    if (showTouchPicker) {
        val existing = if (selectedActionIndex != -1) macro.actions.getOrNull(selectedActionIndex) else null
        TouchPointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_TOUCH_PREFIX), onDismiss = { showTouchPicker = false; selectedActionIndex = -1 }, onConfirmed = { x, y, hold ->
            val action = "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold"
            val newList = macro.actions.toMutableList()
            if (selectedActionIndex != -1 && selectedActionIndex < newList.size) newList[selectedActionIndex] = action else newList.add(action)
            onMacroChange(macro.copy(actions = newList)); showTouchPicker = false; selectedActionIndex = -1
        })
    }
    if (showSwipePicker) {
        val existing = if (selectedActionIndex != -1) macro.actions.getOrNull(selectedActionIndex) else null
        SwipePointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_SWIPE_PREFIX), onDismiss = { showSwipePicker = false; selectedActionIndex = -1 }, onConfirmed = { x1, y1, x2, y2, hS, hE ->
            val action = "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE"
            val newList = macro.actions.toMutableList()
            if (selectedActionIndex != -1 && selectedActionIndex < newList.size) newList[selectedActionIndex] = action else newList.add(action)
            onMacroChange(macro.copy(actions = newList)); showSwipePicker = false; selectedActionIndex = -1
        })
    }
    if (showDelayPicker) {
        val existing = if (selectedActionIndex != -1) macro.actions.getOrNull(selectedActionIndex) else null
        DelayPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_DELAY_PREFIX), onDismiss = { showDelayPicker = false; selectedActionIndex = -1 }, onConfirmed = { sec ->
            val action = "${Config.ACTION_DELAY_PREFIX}$sec"
            val newList = macro.actions.toMutableList()
            if (selectedActionIndex != -1 && selectedActionIndex < newList.size) newList[selectedActionIndex] = action else newList.add(action)
            onMacroChange(macro.copy(actions = newList)); showDelayPicker = false; selectedActionIndex = -1
        })
    }
    if (showPreciseSwipePicker && selectedActionIndex != -1) {
        PreciseSwipePickerDialog(
            initialValue = macro.actions[selectedActionIndex].removePrefix(Config.ACTION_PATH_PREFIX),
            onDismiss = { showPreciseSwipePicker = false },
            onConfirmed = { path ->
                val newList = macro.actions.toMutableList()
                newList[selectedActionIndex] = "${Config.ACTION_PATH_PREFIX}$path"
                onMacroChange(macro.copy(actions = newList))
                showPreciseSwipePicker = false
            }
        )
    }
}

@Composable
private fun MockDesktop() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Simplified wallpaper-like background
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF000000)))
        ))

        // Dot grid pattern (Nothing style)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotSpacing = 20.dp.toPx()
            val dotRadius = 1.dp.toPx()
            for (x in 0..(size.width / dotSpacing).toInt()) {
                for (y in 0..(size.height / dotSpacing).toInt()) {
                    drawCircle(Color.Gray.copy(alpha = 0.1f), dotRadius, Offset(x * dotSpacing, y * dotSpacing))
                }
            }
        }

        // Bottom icons area
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(4) {
                Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
            }
        }
    }
}

@Composable
private fun GestureMarker(
    action: String,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    progress: Float = 1f,
    onClick: () -> Unit = {}
) {
    val color = when {
        isHighlighted -> Color.White
        isSelected -> Color(0xFFFFD700)
        else -> Color.Gray.copy(alpha = 0.6f)
    }
    var size by remember { mutableStateOf(Size.Zero) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { size = Size(it.size.width.toFloat(), it.size.height.toFloat()) }) {
        if (size != Size.Zero) {
            val widthPx = 1080f
            val heightPx = 2400f

            when {
                action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                    val parts = action.removePrefix(Config.ACTION_TOUCH_PREFIX).split("_")
                    if (parts.size >= 2) {
                        val x = parts[0].toFloatOrNull() ?: 0f
                        val y = parts[1].toFloatOrNull() ?: 0f
                        // Tap animation: fade out as progress goes from 0 to 1
                        val alpha = if (isHighlighted) (1f - progress).coerceIn(0f, 1f) else 1f

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        ((x / widthPx) * size.width).toInt() - 16.dp.roundToPx(),
                                        ((y / heightPx) * size.height).toInt() - 16.dp.roundToPx()
                                    )
                                }
                                .graphicsLayer { this.alpha = alpha }
                                .size(if (isHighlighted) 48.dp else 32.dp)
                                .border(2.dp, color, CircleShape)
                                .background(color.copy(alpha = if (isHighlighted) 0.5f else 0.2f), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClick
                                )
                        )
                    }
                }
                action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                    val parts = action.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
                    if (parts.size >= 4) {
                        val x1 = parts[0].toFloatOrNull() ?: 0f
                        val y1 = parts[1].toFloatOrNull() ?: 0f
                        val x2 = parts[2].toFloatOrNull() ?: 0f
                        val y2 = parts[3].toFloatOrNull() ?: 0f

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val p1 = Offset((x1 / widthPx) * size.width, (y1 / heightPx) * size.height)
                            val p2 = Offset((x2 / widthPx) * size.width, (y2 / heightPx) * size.height)

                            // Trajectory drawn so far
                            val currentEnd = Offset(lerp(p1.x, p2.x, progress), lerp(p1.y, p2.y, progress))

                            // Main line (drawn full if not highlighted, otherwise up to progress)
                            drawLine(
                                color = color.copy(alpha = if (isHighlighted) 0.3f else 1f),
                                start = p1, end = p2,
                                strokeWidth = 4.dp.toPx()
                            )

                            if (isHighlighted) {
                                // Trail/active part
                                drawLine(color = Color.White, start = p1, end = currentEnd, strokeWidth = 6.dp.toPx())
                                drawCircle(color = Color.White, radius = 8.dp.toPx(), center = currentEnd)
                            }

                            drawCircle(color, 6.dp.toPx(), p1)
                            drawCircle(color, 4.dp.toPx(), p2)
                        }

                        // Hit area at start point
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        ((x1 / widthPx) * size.width).toInt() - 20.dp.roundToPx(),
                                        ((y1 / heightPx) * size.height).toInt() - 20.dp.roundToPx()
                                    )
                                }
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClick
                                )
                        )
                    }
                }
                action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                    val pathData = action.removePrefix(Config.ACTION_PATH_PREFIX)
                    val pts = pathData.split(";").filter { it.isNotEmpty() }.mapNotNull {
                        val p = it.split(",")
                        if (p.size >= 2) {
                            val px = p[0].toFloatOrNull() ?: 0f
                            val py = p[1].toFloatOrNull() ?: 0f
                            Offset((px / widthPx) * size.width, (py / heightPx) * size.height)
                        } else null
                    }
                    if (pts.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (pts.size >= 2) {
                                val fullPath = Path()
                                fullPath.moveTo(pts[0].x, pts[0].y)
                                for (i in 1 until pts.size) fullPath.lineTo(pts[i].x, pts[i].y)

                                drawPath(
                                    fullPath,
                                    color.copy(alpha = if (isHighlighted) 0.3f else 1f),
                                    style = Stroke(width = 4.dp.toPx())
                                )

                                if (isHighlighted) {
                                    // Draw path segment by segment based on progress
                                    val totalSegments = pts.size - 1
                                    val targetIndex = (progress * totalSegments).toInt().coerceIn(0, totalSegments)

                                    val trailPath = Path()
                                    trailPath.moveTo(pts[0].x, pts[0].y)
                                    for (i in 1..targetIndex) {
                                        trailPath.lineTo(pts[i].x, pts[i].y)
                                    }

                                    // Add partial segment
                                    if (targetIndex < totalSegments) {
                                        val segProgress = (progress * totalSegments) - targetIndex
                                        val pStart = pts[targetIndex]
                                        val pEnd = pts[targetIndex + 1]
                                        val partialPos = Offset(lerp(pStart.x, pEnd.x, segProgress), lerp(pStart.y, pEnd.y, segProgress))
                                        trailPath.lineTo(partialPos.x, partialPos.y)
                                        drawCircle(Color.White, 8.dp.toPx(), partialPos)
                                    }

                                    drawPath(trailPath, Color.White, style = Stroke(width = 6.dp.toPx()))
                                }
                            }
                            drawCircle(color, 6.dp.toPx(), pts[0])
                            if (pts.size >= 2) drawCircle(color, 4.dp.toPx(), pts.last())
                        }

                        // Hit area at start point
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (pts[0].x).toInt() - 20.dp.roundToPx(),
                                        (pts[0].y).toInt() - 20.dp.roundToPx()
                                    )
                                }
                                .size(40.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdvancedTimeline(
    timelineData: List<MacroActionTime>,
    scrollState: ScrollState,
    playbackTime: Long,
    totalDuration: Long,
    isPlaying: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDurationChange: (Int, Long) -> Unit
) {
    val pxPerMs = 0.2f 
    val density = LocalDensity.current
    var timelineWidth by remember { mutableIntStateOf(0) }
    
    // Drag/Reorder State
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    
    var showQuickMenu by remember { mutableIntStateOf(-1) }

    // Internal interaction state for smooth resize
    var interStartShiftMs by remember { mutableLongStateOf(0L) }
    var interDurDeltaMs by remember { mutableLongStateOf(0L) }

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp) 
                .onGloballyPositioned { timelineWidth = it.size.width }
                .horizontalScroll(scrollState)
                .pointerInput(totalDuration) {
                    detectTapGestures { offset ->
                        val time = ((offset.x + scrollState.value) / (pxPerMs * density.density)).toLong()
                        onSeek(time.coerceIn(0, totalDuration))
                        showQuickMenu = -1
                    }
                }
        ) {
            val totalWidth = (totalDuration * pxPerMs).dp
            
            Box(modifier = Modifier.width(totalWidth).fillMaxHeight().padding(vertical = 4.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (t in 0..totalDuration step 1000L) {
                        val x = t * pxPerMs
                        drawLine(color = Color.White.copy(alpha = 0.15f), start = Offset(x.dp.toPx(), 0f), end = Offset(x.dp.toPx(), size.height), strokeWidth = 1f)
                    }
                }

                timelineData.forEach { data ->
                    key(data.id) {
                        val currentIdx = timelineData.indexOfFirst { it.id == data.id }
                        val isSelected = data.index == selectedIndex
                        val isDragged = data.id == draggedId

                        val baseColor = when {
                            data.type.startsWith(Config.ACTION_TOUCH_PREFIX) -> Color.Gray
                            data.type.startsWith(Config.ACTION_SWIPE_PREFIX) -> Color.Gray.copy(alpha = 0.8f)
                            data.type.startsWith(Config.ACTION_PATH_PREFIX) -> Color.Gray.copy(alpha = 0.9f)
                            data.type.startsWith(Config.ACTION_DELAY_PREFIX) -> Color.Transparent
                            else -> Color.DarkGray
                        }

                        val color = if (isSelected || isDragged) Color(0xFFFFD700) else baseColor
                        val isDelay = data.type.startsWith(Config.ACTION_DELAY_PREFIX)

                        val currentStart = if (isSelected) data.startTime + interStartShiftMs else data.startTime
                        val currentDur = if (isSelected) data.duration + interDurDeltaMs else data.duration

                        // SQUARE MODE: If dragging ANY item, all items shrink
                        val isGlobalDragging = draggedId != null
                        val itemWidth = if (isGlobalDragging) 60.dp else (currentDur * pxPerMs).dp.coerceAtLeast(if (isDelay) 4.dp else 24.dp)
                        val itemHeight = if (isGlobalDragging) 60.dp else 40.dp
                        
                        Box(
                            modifier = Modifier
                                .offset { 
                                    if (isGlobalDragging) {
                                        val xBase = (currentIdx * 70.dp.toPx()).toInt()
                                        val xDrag = if (isDragged) dragOffsetPx.roundToInt() else 0
                                        IntOffset(x = xBase + xDrag, y = 10.dp.roundToPx())
                                    } else {
                                        val xBase = (currentStart * pxPerMs * density.density).toInt()
                                        IntOffset(x = xBase, y = 0)
                                    }
                                }
                                .size(itemWidth, itemHeight)
                                .align(Alignment.CenterStart)
                                .zIndex(if (isDragged || isSelected) 10f else 1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = if (isDragged) 0.8f else 1f))
                                .border(width = 1.dp, color = if (isSelected || isDragged) Color.White else if (isDelay) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(if (isDelay) 0.dp else 6.dp))
                                .combinedClickable(
                                    onClick = { onSelect(data.index); showQuickMenu = -1 },
                                    onDoubleClick = { showQuickMenu = data.index }
                                )
                                .pointerInput(data.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedId = data.id; dragOffsetPx = 0f; showQuickMenu = -1 },
                                        onDragEnd = { draggedId = null; dragOffsetPx = 0f },
                                        onDragCancel = { draggedId = null; dragOffsetPx = 0f },
                                        onDrag = { change, dragAmount -> 
                                            change.consume()
                                            dragOffsetPx += dragAmount.x
                                            
                                            val step = 70.dp.toPx()
                                            val moveBy = (dragOffsetPx / step).roundToInt()
                                            if (moveBy != 0) {
                                                val targetIdx = (currentIdx + moveBy).coerceIn(0, timelineData.size - 1)
                                                if (targetIdx != currentIdx) {
                                                    onMove(currentIdx, targetIdx)
                                                    dragOffsetPx -= (targetIdx - currentIdx) * step
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isDelay) {
                                val icon = when {
                                    data.type.startsWith(Config.ACTION_TOUCH_PREFIX) -> Icons.Default.TouchApp
                                    data.type.startsWith(Config.ACTION_SWIPE_PREFIX) -> Icons.Default.Swipe
                                    data.type.startsWith(Config.ACTION_PATH_PREFIX) -> Icons.Default.Gesture
                                    else -> Icons.Default.Extension
                                }
                                Icon(icon, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }

                            if (isSelected && !isGlobalDragging) {
                                ResizeHandle(Alignment.CenterStart, onResize = { deltaMs -> 
                                    interStartShiftMs += deltaMs
                                    interDurDeltaMs -= deltaMs
                                }, onResizeEnd = {
                                    onDurationChange(data.index, currentDur)
                                    interStartShiftMs = 0; interDurDeltaMs = 0
                                })
                                ResizeHandle(Alignment.CenterEnd, onResize = { deltaMs -> 
                                    interDurDeltaMs += deltaMs
                                }, onResizeEnd = {
                                    onDurationChange(data.index, currentDur)
                                    interStartShiftMs = 0; interDurDeltaMs = 0
                                })
                            }
                        }

                        if (showQuickMenu == data.index) {
                            QuickActionPopup(
                                x = (currentStart * pxPerMs).dp,
                                onEdit = { onEdit(data.index); showQuickMenu = -1 },
                                onDelete = { onDelete(data.index); showQuickMenu = -1 }
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.offset(x = (playbackTime * pxPerMs).dp - 1.dp).width(2.dp).fillMaxHeight().background(Color.White))
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTrackTimeline(
    label: String, 
    color: Color, 
    totalDuration: Long,
    scrollState: ScrollState,
    mediaItems: List<MediaItem>,
    selectedMediaId: String?,
    onSelect: (String) -> Unit,
    onMediaChange: (String, MediaItem) -> Unit,
    onMoveOrder: (Int, Int) -> Unit,
    onDelete: (String) -> Unit,
    onSpeedClick: (() -> Unit)? = null
) {
    val pxPerMs = 0.2f
    val density = LocalDensity.current
    
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color(0xFF0A0A0A))
            .horizontalScroll(scrollState)
    ) {
        val totalWidth = (totalDuration * pxPerMs).dp
        Box(modifier = Modifier.width(totalWidth).fillMaxHeight().padding(vertical = 4.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (t in 0..totalDuration step 1000L) {
                    val x = t * pxPerMs
                    drawLine(color = Color.White.copy(alpha = 0.15f), start = Offset(x.dp.toPx(), 0f), end = Offset(x.dp.toPx(), size.height), strokeWidth = 1f)
                }
            }

            mediaItems.forEachIndexed { idx, item ->
                val isSelected = item.id == selectedMediaId
                val isDragged = item.id == draggedId
                var showQuickMenu by remember { mutableStateOf(false) }

                var interStartShiftMs by remember(item.id) { mutableLongStateOf(0L) }
                var interDurDeltaMs by remember(item.id) { mutableLongStateOf(0L) }

                val currentStart = item.startTime + interStartShiftMs
                val currentDur = item.duration + interDurDeltaMs

                val isGlobalDragging = draggedId != null
                val itemWidth = if (isGlobalDragging) 60.dp else (currentDur * pxPerMs).dp.coerceAtLeast(40.dp)
                val itemHeight = if (isGlobalDragging) 60.dp else 40.dp

                Box(
                    modifier = Modifier
                        .offset {
                            if (isGlobalDragging) {
                                val xPos = (idx * 70.dp.toPx()).toInt()
                                val xDrag = if (isDragged) dragOffsetPx.roundToInt() else 0
                                IntOffset(xPos + xDrag, 10.dp.roundToPx())
                            } else {
                                val xBase = (currentStart * pxPerMs * density.density).toInt()
                                IntOffset(xBase, 0)
                            }
                        }
                        .size(itemWidth, itemHeight)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = if (isDragged) 0.8f else if (isSelected) 0.6f else 0.4f))
                        .border(if (isDragged || isSelected) 2.dp else 1.dp, if (isDragged || isSelected) Color.White else color, RoundedCornerShape(6.dp))
                        .combinedClickable(
                            onClick = { onSelect(item.id); showQuickMenu = false },
                            onDoubleClick = { showQuickMenu = true }
                        )
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggedId = item.id; dragOffsetPx = 0f },
                                onDragEnd = { draggedId = null; dragOffsetPx = 0f },
                                onDragCancel = { draggedId = null; dragOffsetPx = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetPx += dragAmount.x

                                    val threshold = 35.dp.toPx()
                                    if (dragOffsetPx > threshold && idx < mediaItems.size - 1) {
                                        onMoveOrder(idx, idx + 1)
                                        dragOffsetPx = 0f
                                    } else if (dragOffsetPx < -threshold && idx > 0) {
                                        onMoveOrder(idx, idx - 1)
                                        dragOffsetPx = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)

                    if (isSelected && !isGlobalDragging) {
                        ResizeHandle(Alignment.CenterStart, onResize = { deltaMs ->
                            interStartShiftMs += deltaMs
                            interDurDeltaMs -= deltaMs
                        }, onResizeEnd = {
                            onMediaChange(item.id, item.copy(startTime = currentStart.coerceAtLeast(0L), duration = currentDur.coerceAtLeast(100L)))
                            interStartShiftMs = 0; interDurDeltaMs = 0
                        })
                        ResizeHandle(Alignment.CenterEnd, onResize = { deltaMs ->
                            interDurDeltaMs += deltaMs
                        }, onResizeEnd = {
                            onMediaChange(item.id, item.copy(duration = currentDur.coerceAtLeast(100L)))
                            interStartShiftMs = 0; interDurDeltaMs = 0
                        })
                    }
                }

                if (showQuickMenu) {
                    Row(
                        modifier = Modifier
                            .offset(x = (currentStart * pxPerMs).dp, y = (-44).dp)
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onSpeedClick != null && item.type == MediaType.VIDEO) {
                            IconButton(onClick = { onSpeedClick(); showQuickMenu = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Speed, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = { onDelete(item.id); showQuickMenu = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedCoefficientDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onConfirmed: (Float) -> Unit
) {
    var text by remember { mutableStateOf(currentSpeed.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Скорость видео", color = Color.White) },
        text = {
            Column {
                Text("Введите коэффициент (например, 0.5 или 2.0)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = text.toFloatOrNull() ?: 1.0f
                onConfirmed(s.coerceIn(0.1f, 5.0f))
            }) {
                Text("ОК", color = Color(0xFFFFD700))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ОТМЕНА", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun BoxScope.ResizeHandle(
    alignment: Alignment,
    onResize: (Long) -> Unit,
    onResizeEnd: () -> Unit
) {
    val density = LocalDensity.current
    val pxPerMs = 0.2f
    var accum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .align(alignment)
            .width(36.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { accum = 0f },
                    onDragEnd = onResizeEnd,
                    onDragCancel = onResizeEnd,
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accum += dragAmount / density.density
                        val deltaMs = (accum / pxPerMs).toLong()
                        if (deltaMs != 0L) {
                            onResize(deltaMs)
                            accum -= deltaMs * pxPerMs
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight(0.6f)
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun QuickActionPopup(x: androidx.compose.ui.unit.Dp, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .offset(x = x, y = (-44).dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SmallVerticalButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(110.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(22.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { rotationZ = -90f }
            )
        }
    }
}

@Composable
private fun PreviewStyleIcon(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(if (isSelected) Color(0xFFFFD700) else Color.Transparent, CircleShape)
    ) {
        Icon(icon, null, tint = if (isSelected) Color.Black else Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun PreciseSwipePickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    val density = LocalDensity.current
    var pathPoints by remember {
        val pts = initialValue?.split(";")?.filter { it.isNotEmpty() }?.mapNotNull {
            val p = it.split(",")
            if (p.size >= 2) Offset(p[0].toFloat(), p[1].toFloat()) else null
        } ?: emptyList()
        mutableStateOf(pts)
    }

    var isDrawing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "precise_swipe")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "progress"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.9f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isDrawing) {
                        if (isDrawing) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                pathPoints = listOf(down.position)
                                do {
                                    val event = awaitPointerEvent()
                                    val pos = event.changes.first().position
                                    pathPoints = pathPoints + pos
                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                                isDrawing = false
                            }
                        } else {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                pathPoints = pathPoints.map { Offset(it.x + dragAmount.x, it.y + dragAmount.y) }
                            }
                        }
                    }
                ) {
                    if (pathPoints.size >= 2) {
                        val path = Path()
                        path.moveTo(pathPoints[0].x, pathPoints[0].y)
                        for (i in 1 until pathPoints.size) {
                            path.lineTo(pathPoints[i].x, pathPoints[i].y)
                        }
                        drawPath(path, Color.Gray, style = Stroke(width = 8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))))

                        // Running dot
                        val totalSegments = pathPoints.size - 1
                        val currentSegment = (progress * totalSegments).toInt().coerceIn(0, totalSegments - 1)
                        val segmentProgress = (progress * totalSegments) - currentSegment
                        val p1 = pathPoints[currentSegment]
                        val p2 = pathPoints[currentSegment + 1]
                        val currentPos = Offset(lerp(p1.x, p2.x, segmentProgress), lerp(p1.y, p2.y, segmentProgress))
                        drawCircle(Color.White, 20f, currentPos)

                        // Markers
                        drawCircle(Color.White, 12f, pathPoints[0])
                        drawCircle(Color.White, 8f, pathPoints.last())
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isDrawing) "РИСУЙТЕ ТРАЕКТОРИЮ" else "РЕДАКТИРОВАНИЕ ТРАЕКТОРИИ",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!isDrawing) {
                        Text("Тяните для перемещения", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text(stringResource(R.string.cancel))
                    }

                    IconButton(onClick = {
                        // Simplify path: ~2 pts per 1cm.
                        // Assuming 1cm is ~150px (typical for 400-500 dpi phones),
                        // 2 pts/cm means a point every ~75px.
                        if (pathPoints.size > 2) {
                            val simplified = mutableListOf<Offset>()
                            simplified.add(pathPoints.first())
                            var lastAdded = pathPoints.first()
                            val thresholdPx = 75f
                            for (i in 1 until pathPoints.size - 1) {
                                val dist = (pathPoints[i] - lastAdded).getDistance()
                                if (dist > thresholdPx) {
                                    simplified.add(pathPoints[i])
                                    lastAdded = pathPoints[i]
                                }
                            }
                            simplified.add(pathPoints.last())
                            pathPoints = simplified
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = Color.White)
                            Text("ВЫРОВНЯТЬ", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(onClick = {
                        if (pathPoints.isNotEmpty()) {
                            val centerX = pathPoints.map { it.x }.average().toFloat()
                            pathPoints = pathPoints.map { Offset(centerX - (it.x - centerX), it.y) }
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Flip, null, tint = Color.White)
                            Text("ОТРАЗИТЬ", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(onClick = { isDrawing = true; pathPoints = emptyList() }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Gesture, null, tint = Color.White)
                            Text("ПЕРЕРИСОВАТЬ", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = {
                            // Generate timestamps: total duration 500ms distributed among points
                            val totalDur = 500L
                            val res = pathPoints.indices.joinToString(";") { i ->
                                val p = pathPoints[i]
                                val t = if (pathPoints.size > 1) (i * totalDur) / (pathPoints.size - 1) else 0L
                                "${p.x.toInt()},${p.y.toInt()},$t"
                            }
                            onConfirmed(res)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

private fun Offset.getDistance() = kotlin.math.sqrt(x * x + y * y)
private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)


@Composable
private fun WallpaperPreview(localPath: String?) {
    val ctx = LocalContext.current
    var wallpaper by remember(localPath) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(localPath) { mutableStateOf(localPath != null) }

    LaunchedEffect(localPath) {
        if (localPath != null) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(localPath)?.asImageBitmap()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        wallpaper = bitmap
                        isLoading = false
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        } else {
            MockDesktop()
            if (!isLoading && localPath == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Wallpaper, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Нажмите на иконку обоев еще раз,\nчтобы выбрать фоновое изображение",
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp).align(Alignment.Center),
                color = Color(0xFFFFD700),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun VideoPreview(localPath: String?, itemStartTime: Long, playbackTime: Long, isPlaying: Boolean, speed: Float) {
    val ctx = LocalContext.current
    if (localPath == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.VideoLibrary, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text("Нажмите на иконку видео еще раз,\nчтобы выбрать файл", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        return
    }

    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // Sync media player with state
    LaunchedEffect(isPlaying, speed) {
        mediaPlayer?.let { mp ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    val params = mp.playbackParams
                    params.speed = speed
                    mp.playbackParams = params
                } catch (e: Exception) {}
            }
            if (isPlaying) mp.start() else mp.pause()
        }
    }

    LaunchedEffect(playbackTime) {
        mediaPlayer?.let { mp ->
            val relativeTime = (playbackTime - itemStartTime).coerceAtLeast(0L)
            val targetPos = (relativeTime * speed).toInt()
            val delta = (mp.currentPosition - targetPos).absoluteValue
            if (delta > 200) {
                mp.seekTo(targetPos)
            }
        }
    }

    DisposableEffect(localPath) {
        val mp = android.media.MediaPlayer().apply {
            setDataSource(localPath)
            isLooping = true
            prepareAsync()
            setOnPreparedListener {
                it.setVolume(0f, 0f) // Silent preview
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val params = it.playbackParams
                    params.speed = speed
                    it.playbackParams = params
                }
                val relativeTime = (playbackTime - itemStartTime).coerceAtLeast(0L)
                it.seekTo((relativeTime * speed).toInt())
                if (isPlaying) it.start()
            }
        }
        mediaPlayer = mp
        onDispose {
            mp.release()
            mediaPlayer = null
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context ->
            android.view.SurfaceView(context).apply {
                holder.addCallback(object : android.view.SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                        mediaPlayer?.setDisplay(holder)
                    }
                    override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                        mediaPlayer?.setDisplay(null)
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CustomMediaPreview(uri: Uri?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (uri != null) {
            // Very simplified: just show image if it is one.
            // Video requires ExoPlayer which is heavy to add now.
            val ctx = LocalContext.current
            val bitmap = remember(uri) {
                try {
                    val inputStream = ctx.contentResolver.openInputStream(uri)
                    android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                } catch (e: Exception) { null }
            }
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text("Ошибка загрузки", color = Color.Red, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            Text("Нет медиа", color = Color.Gray)
        }
    }
}

private fun Modifier.aspectOf(ratio: Float, scale: Float = 1f): Modifier = layout { measurable, constraints ->
    val h = constraints.maxHeight
    val targetHeight = (h * scale).toInt()
    val targetWidth = (targetHeight * ratio).toInt()
    val placeable = measurable.measure(Constraints.fixed(targetWidth, targetHeight))
    layout(targetWidth, targetHeight) {
        placeable.placeRelative(0, 0)
    }
}



@Composable
fun MacroPickerDialog(
    macros: List<Macro>,
    onDismiss: () -> Unit,
    onMacroPicked: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1A1A1A), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_macro), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(macros) { macro ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onMacroPicked(macro.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EventIconDisplay(macro.icon, displaySize = 32.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(macro.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                    }
                }
                if (macros.isEmpty()) {
                    Text(stringResource(R.string.no_macros_msg), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}





@Composable
fun RoundHeaderButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1A1A1A),
    borderColor: Color? = Color.Gray.copy(alpha = 0.5f),
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (borderColor != null) Modifier.border(2.dp, borderColor, CircleShape) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun LabeledPanel(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            EventIconDisplay(value, displaySize = 32.dp)
        }
    }
}

@Composable
fun ConnectingLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.Gray.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun EventItemBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D), RoundedCornerShape(28.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.padding(end = if (onDelete != null) 32.dp else 0.dp)) {
            content()
        }
        if (onDelete != null) {
            Icon(
                Icons.Default.Close,
                null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

@Composable
fun EventAddPanel(
    initialRule: EventRule,
    macros: List<Macro>,
    apps: List<AppInfo>?,
    onDismiss: () -> Unit,
    onSave: (EventRule) -> Unit
) {
    var currentRule by rememberSaveable { mutableStateOf(initialRule) }
    var showIconPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // States for pickers
    var showAppMultiPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRotationPicker by remember { mutableStateOf(false) }
    var showLightPicker by remember { mutableStateOf(false) }
    var showTouchPicker by remember { mutableStateOf(false) }
    var showSwipePicker by remember { mutableStateOf(false) }
    var showDelayPicker by remember { mutableStateOf(false) }
    var showBrightnessPicker by remember { mutableStateOf(false) }
    var showMacroPicker by remember { mutableStateOf(false) }
    var showBrightnessRangePicker by remember { mutableStateOf(false) }
    var showBatteryRangePicker by remember { mutableStateOf(false) }
    var showWifiPicker by remember { mutableStateOf(false) }
    var showBtPicker by remember { mutableStateOf(false) }
    var showLocPicker by remember { mutableStateOf(false) }
    var showDataPicker by remember { mutableStateOf(false) }
    var showChargingPicker by remember { mutableStateOf(false) }
    var showLockPicker by remember { mutableStateOf(false) }
    var showAirplanePicker by remember { mutableStateOf(false) }
    var showNfcPicker by remember { mutableStateOf(false) }
    var showAutoRotatePicker by remember { mutableStateOf(false) }
    var showHeadphonesPicker by remember { mutableStateOf(false) }
    var showDndPicker by remember { mutableStateOf(false) }
    var showMotionPicker by remember { mutableStateOf(false) }
    var showDarkModePicker by remember { mutableStateOf(false) }
    var showSilentPicker by remember { mutableStateOf(false) }
    var showVibratePicker by remember { mutableStateOf(false) }
    var showHotspotPicker by remember { mutableStateOf(false) }
    var showVolumePicker by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showWaitConfirmationPicker by remember { mutableStateOf(false) }
    var showUrlPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showAutoBrightnessPicker by remember { mutableStateOf(false) }
    var showFullAppPicker by remember { mutableStateOf(false) }

    var editingConditionIndex by remember { mutableIntStateOf(-1) }
    var editingExclusionIndex by remember { mutableIntStateOf(-1) }
    var editingActionIndex by remember { mutableIntStateOf(-1) }
    var pickingForExclusion by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var actionIds by rememberSaveable { mutableStateOf(List(currentRule.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != currentRule.actions.size) {
        if (actionIds.size < currentRule.actions.size) {
            actionIds = actionIds + List(currentRule.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(currentRule.actions.size)
        }
    }

    // Header animation logic
    val collapseProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset.toFloat() / 200f).coerceIn(0f, 1f)
        }
    }
    val isCollapsed by remember {
        derivedStateOf { collapseProgress > 0.1f }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Fixed Header Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .drawBehind {
                            val blurHeight = 60.dp.toPx()
                            drawRect(color = Color.Black, size = size)
                            if (collapseProgress > 0f) {
                                drawRect(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = collapseProgress), Color.Transparent),
                                        startY = size.height,
                                        endY = size.height + blurHeight
                                    ),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height),
                                    size = androidx.compose.ui.geometry.Size(size.width, blurHeight)
                                )
                            }
                        }
                        .padding(top = 28.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundHeaderButton(icon = Icons.Default.Close, onClick = onDismiss)

                        AnimatedVisibility(
                            visible = isCollapsed,
                            enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(),
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (initialRule.title.isEmpty()) "СОЗДАЕМ ИВЕНТ" else "РЕДАКТИРУЕМ ИВЕНТ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer {
                                    alpha = collapseProgress
                                    scaleX = 0.8f + (collapseProgress * 0.2f)
                                    scaleY = 0.8f + (collapseProgress * 0.2f)
                                }
                            )
                        }

                        RoundHeaderButton(
                            icon = Icons.Default.Check,
                            onClick = { if (currentRule.title.isNotEmpty()) onSave(currentRule) },
                            backgroundColor = Color(0xFF2B2B2B),
                            borderColor = null
                        )
                    }

                    AnimatedVisibility(
                        visible = isCollapsed,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max)
                                    .graphicsLayer { alpha = collapseProgress },
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.75f)
                                        .fillMaxHeight()
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                        .clickable { scope.launch { scrollState.animateScrollToItem(0) } }
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = currentRule.title.ifEmpty { "Ивент без названия" },
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight()
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                        .clickable { showIconPicker = true }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EventIconDisplay(currentRule.icon.ifEmpty { "⚡" }, displaySize = 24.dp)
                                }
                            }
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        // Dynamic spacer to push content below the expanded fixed header
                        Spacer(Modifier.height(120.dp * collapseProgress))
                    }

                    item {
                        Column(modifier = Modifier
                            .graphicsLayer {
                                alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                                translationY = -collapseProgress * 20f
                            }
                        ) {
                            val isNew = initialRule.title.isEmpty()
                            Text(
                                text = if (isNew) "СОЗДАЕМ НОВЫЙ\nИВЕНТ" else "РЕДАКТИРУЕМ\nИВЕНТ",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 36.sp
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .graphicsLayer {
                                    alpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
                                    translationY = -collapseProgress * 10f
                                },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.75f)
                                    .fillMaxHeight()
                                    .background(Color.Black, RoundedCornerShape(24.dp))
                                    .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Название ивента", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = currentRule.title,
                                        onValueChange = { currentRule = currentRule.copy(title = it) },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { innerTextField ->
                                            if (currentRule.title.isEmpty()) {
                                                Text("Ивент без названия", color = Color.Gray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }

                            LabeledPanel(
                                label = "Значёк",
                                value = currentRule.icon.ifEmpty { "⚡" },
                                modifier = Modifier.weight(0.25f).fillMaxHeight(),
                                onClick = { showIconPicker = true }
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // Content from Logic Step
                    eventLogicItems(
                        rule = currentRule,
                        onRuleChange = { currentRule = it },
                        apps = apps ?: emptyList(),
                        macros = macros,
                        editingConditionIndex = { editingConditionIndex = it },
                        editingActionIndex = { editingActionIndex = it },
                        editingExclusionIndex = { editingExclusionIndex = it },
                        pickingForExclusion = { pickingForExclusion = it },
                        draggedIndex = draggedIndex,
                        onDraggedIndexChange = { draggedIndex = it },
                        actionIds = actionIds,
                        onActionIdsChange = { actionIds = it },
                        onShowTouchPicker = { showTouchPicker = true },
                        onShowSwipePicker = { showSwipePicker = true },
                        onShowDelayPicker = { showDelayPicker = true },
                        onShowBrightnessPicker = { showBrightnessPicker = true },
                        onShowMacroPicker = { showMacroPicker = true },
                        onShowVolumePicker = { showVolumePicker = true },
                        onShowMediaPicker = { showMediaPicker = true },
                        onShowUrlPicker = { showUrlPicker = true },
                        onShowThemePicker = { showThemePicker = true },
                        onShowWaitConfirmationPicker = { showWaitConfirmationPicker = true },
                        onShowAutoBrightnessPicker = { showAutoBrightnessPicker = true },
                        onShowAppMultiPicker = { showAppMultiPicker = true },
                        onShowTimePicker = { showTimePicker = true },
                        onShowRotationPicker = { showRotationPicker = true },
                        onShowLightPicker = { showLightPicker = true },
                        onShowBrightnessRangePicker = { showBrightnessRangePicker = true },
                        onShowBatteryRangePicker = { showBatteryRangePicker = true },
                        onShowWifiPicker = { showWifiPicker = true },
                        onShowBtPicker = { showBtPicker = true },
                        onShowLocPicker = { showLocPicker = true },
                        onShowDataPicker = { showDataPicker = true },
                        onShowChargingPicker = { showChargingPicker = true },
                        onShowLockPicker = { showLockPicker = true },
                        onShowAirplanePicker = { showAirplanePicker = true },
                        onShowNfcPicker = { showNfcPicker = true },
                        onShowAutoRotatePicker = { showAutoRotatePicker = true },
                        onShowHeadphonesPicker = { showHeadphonesPicker = true },
                        onShowDndPicker = { showDndPicker = true },
                        onShowMotionPicker = { showMotionPicker = true },
                        onShowDarkModePicker = { showDarkModePicker = true },
                        onShowSilentPicker = { showSilentPicker = true },
                        onShowVibratePicker = { showVibratePicker = true },
                        onShowHotspotPicker = { showHotspotPicker = true },
                        onShowFullAppPicker = { showFullAppPicker = true }
                    )
                }
            }
        }
    }

                if (showIconPicker) {
        EventIconPickerDialog(
            currentIcon = currentRule.icon,
            apps = apps,
            onDismiss = { showIconPicker = false },
            onIconPicked = { icon ->
                currentRule = currentRule.copy(icon = icon)
                showIconPicker = false
            }
        )
    }

    if (showTouchPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_TOUCH_PREFIX))) {
        val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
        TouchPointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_TOUCH_PREFIX), onDismiss = { showTouchPicker = false; editingActionIndex = -1 }, onConfirmed = { x, y, hold ->
            val action = "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showTouchPicker = false; editingActionIndex = -1
        })
    }
    if (showSwipePicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_SWIPE_PREFIX))) {
        val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
        SwipePointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_SWIPE_PREFIX), onDismiss = { showSwipePicker = false; editingActionIndex = -1 }, onConfirmed = { x1, y1, x2, y2, hS, hE ->
            val action = "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showSwipePicker = false; editingActionIndex = -1
        })
    }
    if (showDelayPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_DELAY_PREFIX))) {
        val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
        DelayPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_DELAY_PREFIX), onDismiss = { showDelayPicker = false; editingActionIndex = -1 }, onConfirmed = { seconds ->
            val action = "${Config.ACTION_DELAY_PREFIX}$seconds"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showDelayPicker = false; editingActionIndex = -1
        })
    }
    if (showBrightnessPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_BRIGHTNESS_PREFIX))) {
        val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
        BrightnessPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)?.toIntOrNull() ?: 50, onDismiss = { showBrightnessPicker = false; editingActionIndex = -1 }, onConfirmed = { percent ->
            val action = "${Config.ACTION_BRIGHTNESS_PREFIX}$percent"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showBrightnessPicker = false; editingActionIndex = -1
        })
    }
    if (showMacroPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_MACRO_PREFIX))) {
        MacroPickerDialog(macros = macros, onDismiss = { showMacroPicker = false; editingActionIndex = -1 }, onMacroPicked = { macroId ->
            val action = "${Config.ACTION_MACRO_PREFIX}$macroId"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showMacroPicker = false; editingActionIndex = -1
        })
    }
    if (showVolumePicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_VOLUME_PREFIX))) {
        VolumePickerDialog(onDismiss = { showVolumePicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
            val action = "${Config.ACTION_VOLUME_PREFIX}$data"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showVolumePicker = false; editingActionIndex = -1
        })
    }
    if (showMediaPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_MEDIA_PREFIX))) {
        MediaPickerDialog(onDismiss = { showMediaPicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
            val action = "${Config.ACTION_MEDIA_PREFIX}$data"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showMediaPicker = false; editingActionIndex = -1
        })
    }
    if (showUrlPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_URL_PREFIX))) {
        val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
        UrlPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_URL_PREFIX) ?: "", onDismiss = { showUrlPicker = false; editingActionIndex = -1 }, onConfirmed = { url ->
            val action = "${Config.ACTION_URL_PREFIX}$url"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showUrlPicker = false; editingActionIndex = -1
        })
    }
    if (showThemePicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_THEME_PREFIX))) {
        ThemePickerDialog(onDismiss = { showThemePicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
            val action = "${Config.ACTION_THEME_PREFIX}$data"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showThemePicker = false; editingActionIndex = -1
        })
    }
    if (showWaitConfirmationPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX))) {
        val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
        WaitConfirmationDialog(initialValue = existing, onDismiss = { showWaitConfirmationPicker = false; editingActionIndex = -1 }, onConfirm = { action ->
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showWaitConfirmationPicker = false; editingActionIndex = -1
        })
    }
    if (showAutoBrightnessPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX))) {
        AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessPicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
            val action = "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$data"
            val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
            currentRule = currentRule.copy(actions = newList)
            showAutoBrightnessPicker = false; editingActionIndex = -1
        })
    }
    if (showFullAppPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && !currentRule.actions[editingActionIndex].startsWith("action:"))) {
        val selected = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) listOf(currentRule.actions[editingActionIndex]) else emptyList()
        UniversalAppPickerDialog(
            apps = (apps ?: emptyList()).filter { it.isLaunchable },
            selected = selected,
            multiSelect = false,
            onConfirmed = { picked ->
                val pkg = picked.firstOrNull()
                if (pkg != null) {
                    val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, pkg) } else currentRule.actions + pkg
                    currentRule = currentRule.copy(actions = newList)
                }
                showFullAppPicker = false; editingActionIndex = -1
            },
            onDismiss = { showFullAppPicker = false; editingActionIndex = -1 }
        )
    }

    if (showAppMultiPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.APP) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.APP)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        val selectedApps = if (index != -1 && index < targetList.size) targetList[index].value.split(",").filter { it.isNotEmpty() } else emptyList()
        UniversalAppPickerDialog(
            apps = apps ?: emptyList(),
            selected = selectedApps,
            multiSelect = true,
            onConfirmed = { pickedApps ->
                if (pickedApps.isNotEmpty()) {
                    val cond = EventCondition(ConditionType.APP, pickedApps.joinToString(","))
                    if (pickingForExclusion) {
                        val newList = currentRule.exclusions.toMutableList()
                        if (index != -1) newList[index] = cond else newList.add(cond)
                        currentRule = currentRule.copy(exclusions = newList)
                    } else {
                        val newList = currentRule.conditions.toMutableList()
                        if (index != -1) newList[index] = cond else newList.add(cond)
                        currentRule = currentRule.copy(conditions = newList)
                    }
                }
                showAppMultiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            },
            onDismiss = { showAppMultiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }
        )
    }
    if (showRotationPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.ROTATION) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.ROTATION)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        val currentVal = if (index >= 0 && index < targetList.size) targetList[index].value else ""
        RotationPickerDialog(initialValue = if (currentVal.isNotEmpty()) currentVal else "PORTRAIT", onDismiss = { showRotationPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rot ->
            val cond = EventCondition(ConditionType.ROTATION, rot)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showRotationPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showLightPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.LIGHT) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.LIGHT)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        LightRangePickerDialog(initialValue = if (index != -1 && index < targetList.size) targetList[index].value else "0-100", onDismiss = { showLightPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rangeStr ->
            val cond = EventCondition(ConditionType.LIGHT, rangeStr)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showLightPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showTimePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.TIME) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.TIME)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        TimeRangePickerDialog(start = if (index != -1 && index < targetList.size) targetList[index].value.split("-").getOrNull(0) else null, end = if (index != -1 && index < targetList.size) targetList[index].value.split("-").getOrNull(1) else null, onDismiss = { showTimePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { s, e ->
            val cond = EventCondition(ConditionType.TIME, "$s-$e")
            if (pickingForExclusion) { if (index != -1) { val newList = currentRule.exclusions.toMutableList().apply { set(index, cond) }; currentRule = currentRule.copy(exclusions = newList) } else currentRule = currentRule.copy(exclusions = currentRule.exclusions + cond) }
            else { if (index != -1) { val newList = currentRule.conditions.toMutableList().apply { set(index, cond) }; currentRule = currentRule.copy(conditions = newList) } else currentRule = currentRule.copy(conditions = currentRule.conditions + cond) }
            showTimePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showBrightnessRangePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.BRIGHTNESS) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.BRIGHTNESS)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        RangePickerDialog(title = stringResource(R.string.brightness_screen_title), initialValue = if (index != -1 && index < targetList.size) targetList[index].value else "0-100", onDismiss = { showBrightnessRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rangeStr ->
            val cond = EventCondition(ConditionType.BRIGHTNESS, rangeStr)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showBrightnessRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showBatteryRangePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.BATTERY) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.BATTERY)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        RangePickerDialog(title = stringResource(R.string.battery_charge_title), initialValue = if (index != -1 && index < targetList.size) targetList[index].value else "0-100", onDismiss = { showBatteryRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rangeStr ->
            val cond = EventCondition(ConditionType.BATTERY, rangeStr)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showBatteryRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showWifiPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.WIFI) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.WIFI)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_wifi_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showWifiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.WIFI, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showWifiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showBtPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.BLUETOOTH) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.BLUETOOTH)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_bt_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showBtPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.BLUETOOTH, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showBtPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showLocPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.LOCATION) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.LOCATION)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_loc_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showLocPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.LOCATION, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showLocPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showDataPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.MOBILE_DATA) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.MOBILE_DATA)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_data_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showDataPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.MOBILE_DATA, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showDataPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showChargingPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.CHARGING) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.CHARGING)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_charging_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_connected), offLabel = stringResource(R.string.state_none), onDismiss = { showChargingPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.CHARGING, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showChargingPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showLockPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.SCREEN_LOCK) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.SCREEN_LOCK)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_lock_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_locked), offLabel = stringResource(R.string.state_unlocked), onDismiss = { showLockPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.SCREEN_LOCK, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showLockPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showAirplanePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.AIRPLANE_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.AIRPLANE_MODE)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_airplane_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showAirplanePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.AIRPLANE_MODE, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showAirplanePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showNfcPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.NFC) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.NFC)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = "NFC", current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showNfcPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.NFC, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showNfcPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showAutoRotatePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.AUTO_ROTATE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.AUTO_ROTATE)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.control_rotation), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showAutoRotatePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.AUTO_ROTATE, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showAutoRotatePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showHeadphonesPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.HEADPHONES) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.HEADPHONES)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_headphones_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_present), offLabel = stringResource(R.string.state_none), onDismiss = { showHeadphonesPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.HEADPHONES, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showHeadphonesPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showDndPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.DND) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.DND)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.control_dnd), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showDndPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.DND, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showDndPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showMotionPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.MOTION) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.MOTION)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_motion_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_moving), offLabel = stringResource(R.string.state_still), onDismiss = { showMotionPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.MOTION, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showMotionPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showDarkModePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.DARK_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.DARK_MODE)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.app_theme), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showDarkModePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.DARK_MODE, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showDarkModePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showSilentPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.SILENT_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.SILENT_MODE)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_silent_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showSilentPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.SILENT_MODE, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showSilentPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showVibratePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.VIBRATE_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.VIBRATE_MODE)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_vibrate_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showVibratePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.VIBRATE_MODE, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showVibratePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
    if (showHotspotPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.HOTSPOT) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.HOTSPOT)) {
        val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
        val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
        BinaryStatePickerDialog(title = stringResource(R.string.cond_hotspot_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_active), offLabel = stringResource(R.string.state_none), onDismiss = { showHotspotPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
            val cond = EventCondition(ConditionType.HOTSPOT, state)
            if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
            else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
            showHotspotPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
        })
    }
}







@Composable
private fun ShortcutToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


fun LazyListScope.eventLogicItems(
    rule: EventRule,
    onRuleChange: (EventRule) -> Unit,
    apps: List<AppInfo>,
    macros: List<Macro>,
    editingConditionIndex: (Int) -> Unit,
    editingActionIndex: (Int) -> Unit,
    editingExclusionIndex: (Int) -> Unit,
    pickingForExclusion: (Boolean) -> Unit,
    draggedIndex: Int,
    onDraggedIndexChange: (Int) -> Unit,
    actionIds: List<String>,
    onActionIdsChange: (List<String>) -> Unit,
    onShowTouchPicker: () -> Unit,
    onShowSwipePicker: () -> Unit,
    onShowDelayPicker: () -> Unit,
    onShowBrightnessPicker: () -> Unit,
    onShowMacroPicker: () -> Unit,
    onShowVolumePicker: () -> Unit,
    onShowMediaPicker: () -> Unit,
    onShowUrlPicker: () -> Unit,
    onShowThemePicker: () -> Unit,
    onShowWaitConfirmationPicker: () -> Unit,
    onShowAutoBrightnessPicker: () -> Unit,
    onShowAppMultiPicker: () -> Unit,
    onShowTimePicker: () -> Unit,
    onShowRotationPicker: () -> Unit,
    onShowLightPicker: () -> Unit,
    onShowBrightnessRangePicker: () -> Unit,
    onShowBatteryRangePicker: () -> Unit,
    onShowWifiPicker: () -> Unit,
    onShowBtPicker: () -> Unit,
    onShowLocPicker: () -> Unit,
    onShowDataPicker: () -> Unit,
    onShowChargingPicker: () -> Unit,
    onShowLockPicker: () -> Unit,
    onShowAirplanePicker: () -> Unit,
    onShowNfcPicker: () -> Unit,
    onShowAutoRotatePicker: () -> Unit,
    onShowHeadphonesPicker: () -> Unit,
    onShowDndPicker: () -> Unit,
    onShowMotionPicker: () -> Unit,
    onShowDarkModePicker: () -> Unit,
    onShowSilentPicker: () -> Unit,
    onShowVibratePicker: () -> Unit,
    onShowHotspotPicker: () -> Unit,
    onShowFullAppPicker: () -> Unit
) {
    // Syntax Summary
    item {
        Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            val gestureLabel = stringResource(rule.triggerGesture.labelRes).lowercase()
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hasWait = rule.actions.any { it.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX) }
                val midText = if (hasWait) stringResource(R.string.event_shortcut_mid_text) else ""
                Text(
                    text = stringResource(R.string.event_summary_template, gestureLabel, rule.conditions.size, midText, rule.actions.size),
                    color = Color.White, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp
                )
                if (rule.exclusions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.event_exclusion_template, rule.exclusions.size),
                        color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Gesture Selector
    item {
        Column {
            Text(stringResource(R.string.when_gesture_exec), color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            val slots = TapSlot.entries.filter { it != TapSlot.SHORTCUT }
            val initialPage = slots.indexOf(rule.triggerGesture).coerceAtLeast(0)
            val pagerState = rememberPagerState(initialPage = initialPage) { slots.size }
            LaunchedEffect(pagerState.currentPage) {
                onRuleChange(rule.copy(triggerGesture = slots[pagerState.currentPage]))
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(60.dp), contentPadding = PaddingValues(horizontal = 48.dp), pageSpacing = 16.dp) { page ->
                Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxSize().border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(28.dp))) {
                    Box(contentAlignment = Alignment.Center) { Text(stringResource(slots[page].labelRes), color = Color.White, style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Conditions
    item {
        val conditionTitle = if (rule.conditions.isEmpty()) "Когда выполнено условие:"
        else if (rule.conditions.size == 1) "Когда выполнено условие:"
        else if (rule.conditionLogicAll) "Когда выполнены условия (ВСЕ):"
        else "Когда выполнено условие (ХОТЯ БЫ ОДНО):"

        Text(
            text = conditionTitle,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { onRuleChange(rule.copy(conditionLogicAll = !rule.conditionLogicAll)) }
        )
        Spacer(Modifier.height(12.dp))

        rule.conditions.forEachIndexed { index, cond ->
            ConditionRow(condition = cond, apps = apps, onEdit = { editingConditionIndex(index) }, onDelete = {
                val newList = rule.conditions.toMutableList().apply { removeAt(index) }
                onRuleChange(rule.copy(conditions = newList))
            })
            if (rule.conditionLogicAll && index < rule.conditions.size - 1) {
                ConnectingLine()
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }

        var showAddConditionPicker by remember { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        PanelAddButton(label = "Новое условие", onClick = { showAddConditionPicker = true })

        if (showAddConditionPicker) {
            EventConditionPickerDialog(onPick = { type ->
                showAddConditionPicker = false
                when (type) {
                    "condition:app" -> onShowAppMultiPicker()
                    "condition:time" -> onShowTimePicker()
                    "condition:rotation" -> onShowRotationPicker()
                    "condition:light" -> onShowLightPicker()
                    "condition:brightness" -> onShowBrightnessRangePicker()
                    "condition:battery" -> onShowBatteryRangePicker()
                    "condition:wifi" -> onShowWifiPicker()
                    "condition:bluetooth" -> onShowBtPicker()
                    "condition:location" -> onShowLocPicker()
                    "condition:mobile_data" -> onShowDataPicker()
                    "condition:charging" -> onShowChargingPicker()
                    "condition:screen_lock" -> onShowLockPicker()
                    "condition:airplane_mode" -> onShowAirplanePicker()
                    "condition:nfc" -> onShowNfcPicker()
                    "condition:auto_rotate" -> onShowAutoRotatePicker()
                    "condition:headphones" -> onShowHeadphonesPicker()
                    "condition:dnd" -> onShowDndPicker()
                    "condition:motion" -> onShowMotionPicker()
                    "condition:dark_mode" -> onShowDarkModePicker()
                    "condition:silent_mode" -> onShowSilentPicker()
                    "condition:vibrate_mode" -> onShowVibratePicker()
                    "condition:hotspot" -> onShowHotspotPicker()
                    "condition:always" -> onRuleChange(rule.copy(conditions = rule.conditions + EventCondition(ConditionType.ALWAYS)))
                }
            }, onDismiss = { showAddConditionPicker = false })
        }
        Spacer(Modifier.height(24.dp))
    }

    // Actions
    item {
        val actionTitle = if (rule.actions.size <= 1) "Будет выполнено действие:" else "Будут выполнены действия:"
        Text(actionTitle, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(12.dp))
    }

    itemsIndexed(rule.actions, key = { index, _ -> actionIds.getOrElse(index) { index.toString() } }) { index, action ->
        ActionRow(
            action = action, index = index, apps = apps, isDragged = draggedIndex == index,
            onDragStart = { onDraggedIndexChange(index) },
            onDragEnd = { onDraggedIndexChange(-1) },
            onEdit = {
                editingActionIndex(index)
                if (!action.startsWith("action:")) {
                    onShowFullAppPicker()
                } else if (action.startsWith(Config.ACTION_TOUCH_PREFIX)) {
                    onShowTouchPicker()
                } else if (action.startsWith(Config.ACTION_SWIPE_PREFIX)) {
                    onShowSwipePicker()
                } else if (action.startsWith(Config.ACTION_DELAY_PREFIX)) {
                    onShowDelayPicker()
                } else if (action.startsWith(Config.ACTION_BRIGHTNESS_PREFIX)) {
                    onShowBrightnessPicker()
                } else if (action.startsWith(Config.ACTION_VOLUME_PREFIX)) {
                    onShowVolumePicker()
                } else if (action.startsWith(Config.ACTION_MEDIA_PREFIX)) {
                    onShowMediaPicker()
                } else if (action.startsWith(Config.ACTION_URL_PREFIX)) {
                    onShowUrlPicker()
                } else if (action.startsWith(Config.ACTION_THEME_PREFIX)) {
                    onShowThemePicker()
                } else if (action.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX)) {
                    onShowAutoBrightnessPicker()
                } else if (action.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX)) {
                    onShowWaitConfirmationPicker()
                } else if (action.startsWith(Config.ACTION_MACRO_PREFIX)) {
                    onShowMacroPicker()
                }
            },
            onMove = { from, to ->
                if (to in rule.actions.indices) {
                    val newList = rule.actions.toMutableList()
                    val item = newList.removeAt(from)
                    newList.add(to, item)

                    val newIds = actionIds.toMutableList()
                    val id = newIds.removeAt(from)
                    newIds.add(to, id)

                    onRuleChange(rule.copy(actions = newList))
                    onActionIdsChange(newIds)
                    onDraggedIndexChange(to)
                }
            },
            onDelete = {
                val newList = rule.actions.toMutableList().apply { removeAt(index) }
                val newIds = actionIds.toMutableList().apply { removeAt(index) }
                onRuleChange(rule.copy(actions = newList))
                onActionIdsChange(newIds)
            }
        )
        val isWait = action.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX)
        val isLast = index == rule.actions.size - 1
        if (!isLast && !isWait) {
            ConnectingLine()
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }

    item {
        var showActionPicker by remember { mutableStateOf(false) }
        var showFullAppPicker by remember { mutableStateOf(false) }

        Spacer(Modifier.height(8.dp))
        PanelAddButton(label = "Новое действие", onClick = { showActionPicker = true })
        if (showActionPicker) {
            EventActionPickerDialog(triggerGesture = rule.triggerGesture, onPick = { pkg ->
                if (pkg == "action:pick_touch") { editingActionIndex(-1); onShowTouchPicker() }
                else if (pkg == "action:pick_swipe") { editingActionIndex(-1); onShowSwipePicker() }
                else if (pkg == "action:pick_delay") { editingActionIndex(-1); onShowDelayPicker() }
                else if (pkg == "action:pick_brightness") { editingActionIndex(-1); onShowBrightnessPicker() }
                else if (pkg == "action:pick_macro") { editingActionIndex(-1); onShowMacroPicker() }
                else if (pkg == "action:pick_volume") { editingActionIndex(-1); onShowVolumePicker() }
                else if (pkg == "action:pick_media") { editingActionIndex(-1); onShowMediaPicker() }
                else if (pkg == "action:pick_url") { editingActionIndex(-1); onShowUrlPicker() }
                else if (pkg == "action:pick_theme") { editingActionIndex(-1); onShowThemePicker() }
                else if (pkg == "action:pick_auto_brightness") { editingActionIndex(-1); onShowAutoBrightnessPicker() }
                else if (pkg == "action:pick_wait_confirmation") { editingActionIndex(-1); onShowWaitConfirmationPicker() }
                else if (pkg == "action:full_app_list") { editingActionIndex(-1); onShowFullAppPicker() }
                else if (pkg != null) onRuleChange(rule.copy(actions = rule.actions + pkg))
                showActionPicker = false
            }, onDismiss = { showActionPicker = false })
        }
        Spacer(Modifier.height(24.dp))
    }

    // Exclusions
    item {
        val exclusionTitle = if (rule.exclusions.isEmpty()) "Кроме исключения:"
        else if (rule.exclusions.size == 1) "Кроме исключения:"
        else if (rule.exclusionLogicAll) "Кроме исключений (ВСЕ):"
        else "Кроме исключения (ХОТЯ БЫ ОДНО):"

        Text(
            text = exclusionTitle,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { onRuleChange(rule.copy(exclusionLogicAll = !rule.exclusionLogicAll)) }
        )
        Spacer(Modifier.height(12.dp))

        rule.exclusions.forEachIndexed { index, excl ->
            ConditionRow(condition = excl, apps = apps, isExclusion = true, onEdit = { pickingForExclusion(true); editingExclusionIndex(index) }, onDelete = {
                val newList = rule.exclusions.toMutableList().apply { removeAt(index) }
                onRuleChange(rule.copy(exclusions = newList))
            })
            if (rule.exclusionLogicAll && index < rule.exclusions.size - 1) {
                ConnectingLine()
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }

        var showAddExclusionPicker by remember { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        PanelAddButton(label = "Новое исключение", onClick = { showAddExclusionPicker = true })
        if (showAddExclusionPicker) {
            EventConditionPickerDialog(onPick = { type ->
                showAddExclusionPicker = false; pickingForExclusion(true)
                when (type) {
                    "condition:app" -> onShowAppMultiPicker()
                    "condition:time" -> onShowTimePicker()
                    "condition:rotation" -> onShowRotationPicker()
                    "condition:light" -> onShowLightPicker()
                    "condition:brightness" -> onShowBrightnessRangePicker()
                    "condition:battery" -> onShowBatteryRangePicker()
                    "condition:wifi" -> onShowWifiPicker()
                    "condition:bluetooth" -> onShowBtPicker()
                    "condition:location" -> onShowLocPicker()
                    "condition:mobile_data" -> onShowDataPicker()
                    "condition:charging" -> onShowChargingPicker()
                    "condition:screen_lock" -> onShowLockPicker()
                    "condition:airplane_mode" -> onShowAirplanePicker()
                    "condition:nfc" -> onShowNfcPicker()
                    "condition:auto_rotate" -> onShowAutoRotatePicker()
                    "condition:headphones" -> onShowHeadphonesPicker()
                    "condition:dnd" -> onShowDndPicker()
                    "condition:motion" -> onShowMotionPicker()
                    "condition:dark_mode" -> onShowDarkModePicker()
                    "condition:silent_mode" -> onShowSilentPicker()
                    "condition:vibrate_mode" -> onShowVibratePicker()
                    "condition:hotspot" -> onShowHotspotPicker()
                    "condition:always" -> onRuleChange(rule.copy(exclusions = rule.exclusions + EventCondition(ConditionType.ALWAYS)))
                }
            }, onDismiss = { showAddExclusionPicker = false })
        }
    }
}


@Composable
fun LazyItemScope.ActionRow(
    action: String,
    index: Int,
    apps: List<AppInfo>,
    isDragged: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(if (isDragged) 1.05f else 1f)
    val elevation by animateDpAsState(if (isDragged) 8.dp else 2.dp)
    val density = LocalDensity.current

    val currentIndex by rememberUpdatedState(index)
    val currentOnMove by rememberUpdatedState(onMove)

    var dragOffset by remember { mutableStateOf(0f) }

    val itemHeight = 110.dp
    val spacing = 12.dp
    val fullStepPx = with(density) { (itemHeight + spacing).toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isDragged) dragOffset else 0f
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .animateItem(placementSpec = if (isDragged) null else spring(stiffness = Spring.StiffnessMediumLow))
                .background(Color.Black, RoundedCornerShape(24.dp))
                .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(6.dp)
                .pointerInput(action) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragOffset = 0f; onDragStart() },
                        onDragEnd = { dragOffset = 0f; onDragEnd() },
                        onDragCancel = { dragOffset = 0f; onDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                            val moveBy = (dragOffset / fullStepPx).roundToInt()
                            if (moveBy != 0) {
                                val targetIndex = currentIndex + moveBy
                                if (targetIndex != currentIndex) {
                                    currentOnMove(currentIndex, targetIndex)
                                    dragOffset -= moveBy * fullStepPx
                                }
                            }
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("Действие ${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close, null, tint = Color.White,
                    modifier = Modifier.size(16.dp).clickable { onDelete() }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                    .clickable { onEdit() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    slotDisplay(LocalContext.current, action, emptyList(), emptyList(), apps),
                    color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}





@Composable
fun ConditionRow(
    condition: EventCondition,
    apps: List<AppInfo>,
    isExclusion: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeIcon = when(condition.type) {
                ConditionType.APP -> Icons.Default.Apps
                ConditionType.TIME -> Icons.Default.Schedule
                ConditionType.BATTERY -> Icons.Default.BatteryChargingFull
                ConditionType.LIGHT -> Icons.Default.LightMode
                else -> Icons.Default.Tune
            }
            Icon(typeIcon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isExclusion) "Исключение" else "Условие", color = Color.White, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.Close, null, tint = Color.White,
                modifier = Modifier.size(16.dp).clickable { onDelete() }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                .clickable { onEdit() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val label = when (condition.type) {
                ConditionType.ALWAYS -> stringResource(R.string.always)
                ConditionType.APP -> {
                    val pkgs = condition.value.split(",").filter { it.isNotEmpty() }
                    if (pkgs.isEmpty()) "Любые приложения"
                    else pkgs.mapNotNull { p -> apps.find { it.pkg == p }?.label }.joinToString(", ")
                }
                ConditionType.TIME -> "${stringResource(R.string.time_label)}: ${condition.value}"
                ConditionType.ROTATION -> {
                    val rotLabel = when(condition.value) {
                        "PORTRAIT" -> stringResource(R.string.rotation_portrait)
                        "FACE_UP" -> stringResource(R.string.rotation_face_up)
                        "FACE_DOWN" -> stringResource(R.string.rotation_face_down)
                        "LANDSCAPE_LEFT" -> stringResource(R.string.rotation_landscape_left)
                        "LANDSCAPE_RIGHT" -> stringResource(R.string.rotation_landscape_right)
                        else -> condition.value
                    }
                    stringResource(R.string.orientation_label, rotLabel)
                }
                ConditionType.LIGHT -> {
                    if (condition.value.contains("-")) {
                        val parts = condition.value.split("-")
                        stringResource(R.string.light_range_label, parts[0].toInt(), parts[1].toInt())
                    } else "${stringResource(R.string.light_label)}: ${condition.value}"
                }
                ConditionType.BRIGHTNESS -> {
                    val parts = condition.value.split("-")
                    "${stringResource(R.string.brightness_label)}: ${stringResource(R.string.range_percent_label, parts.getOrNull(0)?.toInt() ?: 0, parts.getOrNull(1)?.toInt() ?: 100)}"
                }
                ConditionType.BATTERY -> {
                    val parts = condition.value.split("-")
                    "${stringResource(R.string.battery_label)}: ${stringResource(R.string.range_percent_label, parts.getOrNull(0)?.toInt() ?: 0, parts.getOrNull(1)?.toInt() ?: 100)}"
                }
                ConditionType.WIFI -> "${stringResource(R.string.wifi_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.BLUETOOTH -> "${stringResource(R.string.bt_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.LOCATION -> "${stringResource(R.string.location_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.MOBILE_DATA -> "${stringResource(R.string.mobile_data_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.CHARGING -> "${stringResource(R.string.charging_label)}: ${if (condition.value == "ON") stringResource(R.string.state_connected) else stringResource(R.string.state_none)}"
                ConditionType.SCREEN_LOCK -> "${stringResource(R.string.screen_lock_label)}: ${if (condition.value == "ON") stringResource(R.string.state_locked) else stringResource(R.string.state_unlocked)}"
                ConditionType.AIRPLANE_MODE -> "${stringResource(R.string.airplane_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.NFC -> "${stringResource(R.string.nfc_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.AUTO_ROTATE -> "${stringResource(R.string.auto_rotate_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.HEADPHONES -> "${stringResource(R.string.headphones_label)}: ${if (condition.value == "ON") stringResource(R.string.state_present) else stringResource(R.string.state_none)}"
                ConditionType.DND -> "${stringResource(R.string.dnd_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.MOTION -> "${stringResource(R.string.motion_label)}: ${if (condition.value == "ON") stringResource(R.string.state_moving) else stringResource(R.string.state_still)}"
                ConditionType.DARK_MODE -> "${stringResource(R.string.dark_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.SILENT_MODE -> "${stringResource(R.string.silent_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.VIBRATE_MODE -> "${stringResource(R.string.vibrate_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.HOTSPOT -> "${stringResource(R.string.hotspot_label)}: ${if (condition.value == "ON") stringResource(R.string.state_active) else stringResource(R.string.state_none)}"
                else -> condition.value
            }
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PanelAddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}





@Composable
fun EventActionPickerDialog(
    triggerGesture: TapSlot,
    allowMacros: Boolean = true,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(stringResource(R.string.select_action_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.wrapContentHeight().heightIn(max = 550.dp)) {
                    item { PickRow(stringResource(R.string.action_clear_notifications), null, false) { onPick(Config.ACTION_CLEAR_NOTIFICATIONS) } }
                    item { PickRow(stringResource(R.string.action_auto_brightness_dots), null, false) { onPick("action:pick_auto_brightness") } }
                    item { PickRow(stringResource(R.string.open_app), null, false) { onPick("action:full_app_list") } }
                    item { PickRow(stringResource(R.string.control_flashlight), null, false) { onPick(Config.ACTION_FLASHLIGHT) } }
                    item { PickRow("Gliph (${stringResource(R.string.action_glyph).lowercase()})", null, false) { onPick(Config.ACTION_GLYPH) } }
                    item { PickRow(stringResource(R.string.action_back), null, false) { onPick(Config.ACTION_BACK) } }
                    item { PickRow(stringResource(R.string.action_home), null, false) { onPick(Config.ACTION_HOME) } }
                    item { PickRow(stringResource(R.string.action_open_notifications), null, false) { onPick(Config.ACTION_NOTIFICATIONS) } }

                    val killLabel = if (triggerGesture == TapSlot.SINGLE || triggerGesture == TapSlot.LONG) "Kill Notes" else "Kill Essential Space"
                    val killAction = if (triggerGesture == TapSlot.SINGLE || triggerGesture == TapSlot.LONG) Config.ACTION_KILL_NOTES else Config.ACTION_KILL_ESSENTIAL
                    item { PickRow(killLabel, null, false) { onPick(killAction) } }

                    if (allowMacros) {
                        item { PickRow(stringResource(R.string.action_macro), null, false) { onPick("action:pick_macro") } }
                    }

                    item { PickRow(stringResource(R.string.action_touch_screen), null, false) { onPick("action:pick_touch") } }
                    item { PickRow(stringResource(R.string.action_swipe_screen), null, false) { onPick("action:pick_swipe") } }
                    item { PickRow(stringResource(R.string.action_delay), null, false) { onPick("action:pick_delay") } }
                    item { PickRow(stringResource(R.string.action_set_brightness), null, false) { onPick("action:pick_brightness") } }
                    item { PickRow(stringResource(R.string.control_panel), null, false) { onPick(Config.ACTION_CONTROLS) } }
                    item { PickRow(stringResource(R.string.favorites), null, false) { onPick(Config.ACTION_FAVORITES_PREFIX) } }
                    item { PickRow(stringResource(R.string.action_screenshot), null, false) { onPick(Config.ACTION_SCREENSHOT) } }
                    item { PickRow(stringResource(R.string.action_lock_screen), null, false) { onPick(Config.ACTION_LOCK_SCREEN) } }
                    item { PickRow(stringResource(R.string.action_split_screen), null, false) { onPick(Config.ACTION_SPLIT_SCREEN) } }
                    item { PickRow(stringResource(R.string.action_volume_control), null, false) { onPick("action:pick_volume") } }
                    item { PickRow(stringResource(R.string.action_media_control), null, false) { onPick("action:pick_media") } }
                    item { PickRow(stringResource(R.string.action_open_url), null, false) { onPick("action:pick_url") } }
                    item { PickRow(stringResource(R.string.action_dnd), null, false) { onPick(Config.ACTION_DND_TOGGLE) } }
                    item { PickRow(stringResource(R.string.action_change_theme), null, false) { onPick("action:pick_theme") } }
                    item { PickRow(stringResource(R.string.action_wait_confirmation), null, false) { onPick("action:pick_wait_confirmation") } }
                }
            }
        }
    }
}






@Composable
fun EventConditionPickerDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(stringResource(R.string.select_condition_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.wrapContentHeight()) {
                    item { PickRow(stringResource(R.string.always), null, false) { onPick("condition:always") } }
                    item { PickRow(stringResource(R.string.cond_app_label), null, false) { onPick("condition:app") } }
                    item { PickRow(stringResource(R.string.cond_time_label), null, false) { onPick("condition:time") } }
                    item { PickRow(stringResource(R.string.cond_rotation_label), null, false) { onPick("condition:rotation") } }
                    item { PickRow(stringResource(R.string.cond_light_label), null, false) { onPick("condition:light") } }
                    item { PickRow(stringResource(R.string.cond_brightness_label), null, false) { onPick("condition:brightness") } }
                    item { PickRow(stringResource(R.string.cond_battery_label), null, false) { onPick("condition:battery") } }
                    item { PickRow(stringResource(R.string.cond_wifi_label), null, false) { onPick("condition:wifi") } }
                    item { PickRow(stringResource(R.string.cond_bt_label), null, false) { onPick("condition:bluetooth") } }
                    item { PickRow(stringResource(R.string.cond_loc_label), null, false) { onPick("condition:location") } }
                    item { PickRow(stringResource(R.string.cond_data_label), null, false) { onPick("condition:mobile_data") } }
                    item { PickRow(stringResource(R.string.cond_charging_label), null, false) { onPick("condition:charging") } }
                    item { PickRow(stringResource(R.string.cond_lock_label), null, false) { onPick("condition:screen_lock") } }
                    item { PickRow(stringResource(R.string.cond_airplane_label), null, false) { onPick("condition:airplane_mode") } }
                    item { PickRow(stringResource(R.string.cond_nfc_label), null, false) { onPick("condition:nfc") } }
                    item { PickRow(stringResource(R.string.cond_auto_rotate_label), null, false) { onPick("condition:auto_rotate") } }
                    item { PickRow(stringResource(R.string.cond_headphones_label), null, false) { onPick("condition:headphones") } }
                    item { PickRow(stringResource(R.string.cond_dnd_label), null, false) { onPick("condition:dnd") } }
                    item { PickRow(stringResource(R.string.cond_motion_label), null, false) { onPick("condition:motion") } }
                    item { PickRow(stringResource(R.string.cond_dark_mode_label), null, false) { onPick("condition:dark_mode") } }
                    item { PickRow(stringResource(R.string.cond_silent_label), null, false) { onPick("condition:silent_mode") } }
                    item { PickRow(stringResource(R.string.cond_vibrate_label), null, false) { onPick("condition:vibrate_mode") } }
                    item { PickRow(stringResource(R.string.cond_hotspot_label), null, false) { onPick("condition:hotspot") } }
                }
            }
        }
    }
}






@Composable
fun EventIconPickerDialog(currentIcon: String, apps: List<AppInfo>?, onDismiss: () -> Unit, onIconPicked: (String) -> Unit) {
    val initialTab = when {
        currentIcon.startsWith("material:") -> 0
        currentIcon.contains(".") -> 1
        else -> 2
    }
    var tabIndex by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        "Системные",
        stringResource(R.string.tab_apps),
        stringResource(R.string.tab_symbols)
    )
    val ctx = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            "Все", "Общие", "Медиа", "Связь", "Устройства", "Навигация",
            "Файлы", "Соцсети", "Фигуры", "Спорт", "Система", "Текст", "Магазин"
        )
    }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск иконок...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }
                    } else null
                )

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                                color = Color.White
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (tabIndex) {
                        0 -> { // System Icons (Material)
                            Column {
                                // Categories Scroll
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(categories) { category ->
                                        val isSelected = (selectedCategory == category) || (category == "Все" && selectedCategory == null)
                                        Surface(
                                            onClick = { selectedCategory = if (category == "Все") null else category },
                                            color = if (isSelected) Color.White else Color(0xFF2B2B2B),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                                Text(category, color = if (isSelected) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                val filteredIcons = remember(searchQuery, selectedCategory) {
                                    var list = if (searchQuery.isEmpty()) allMaterialIcons
                                    else allMaterialIcons.filter { it.first.contains(searchQuery, ignoreCase = true) }

                                    if (selectedCategory != null) {
                                        // Filter based on the structure of MaterialIconsList.kt
                                        val commonIdx = allMaterialIcons.indexOfFirst { it.first == "Home" }
                                        val mediaIdx = allMaterialIcons.indexOfFirst { it.first == "PlayArrow" }
                                        val deviceIdx = allMaterialIcons.indexOfFirst { it.first == "Wifi" }
                                        val navIdx = allMaterialIcons.indexOfFirst { it.first == "Map" }
                                        val commIdx = allMaterialIcons.indexOfFirst { it.first == "Chat" }
                                        val fileIdx = allMaterialIcons.indexOfFirst { it.first == "Folder" }
                                        val socialIdx = allMaterialIcons.indexOfFirst { it.first == "Groups" }
                                        val shapesIdx = allMaterialIcons.indexOfFirst { it.first == "Circle" }
                                        val lifestyleIdx = allMaterialIcons.indexOfFirst { it.first == "SportsEsports" }
                                        val systemIdx = allMaterialIcons.indexOfFirst { it.first == "LockOpen" }
                                        val arrowIdx = allMaterialIcons.indexOfFirst { it.first == "ArrowBack" }
                                        val textIdx = allMaterialIcons.indexOfFirst { it.first == "FormatBold" }
                                        val shopIdx = allMaterialIcons.indexOfFirst { it.first == "ShoppingBag" }
                                        val healthIdx = allMaterialIcons.indexOfFirst { it.first == "LocalHospital" }

                                        list = when(selectedCategory) {
                                            "Общие" -> allMaterialIcons.subList(commonIdx, mediaIdx)
                                            "Медиа" -> allMaterialIcons.subList(mediaIdx, deviceIdx)
                                            "Устройства" -> allMaterialIcons.subList(deviceIdx, navIdx)
                                            "Навигация" -> allMaterialIcons.subList(navIdx, commIdx)
                                            "Связь" -> allMaterialIcons.subList(commIdx, fileIdx)
                                            "Файлы" -> allMaterialIcons.subList(fileIdx, socialIdx)
                                            "Соцсети" -> allMaterialIcons.subList(socialIdx, shapesIdx)
                                            "Фигуры" -> allMaterialIcons.subList(shapesIdx, lifestyleIdx)
                                            "Спорт" -> allMaterialIcons.subList(lifestyleIdx, systemIdx)
                                            "Система" -> allMaterialIcons.subList(systemIdx, arrowIdx)
                                            "Текст" -> allMaterialIcons.subList(textIdx, shopIdx)
                                            "Магазин" -> allMaterialIcons.subList(shopIdx, healthIdx)
                                            else -> list
                                        }

                                        if (searchQuery.isNotEmpty()) {
                                            list = list.filter { it.first.contains(searchQuery, ignoreCase = true) }
                                        }
                                    }
                                    list
                                }

                                LazyVerticalGrid(columns = GridCells.Adaptive(72.dp)) {
                                    items(filteredIcons) { (name, icon) ->
                                        val isCurrent = currentIcon == "material:$name"
                                        Column(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .aspectRatio(1f)
                                                .background(if (isCurrent) Color.White.copy(alpha = 0.1f) else Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                                .border(if (isCurrent) 2.dp else 0.dp, if (isCurrent) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                                .clickable { onIconPicked("material:$name") }
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(imageVector = icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(28.dp))
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                name,
                                                color = if (isCurrent) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Apps
                            if (apps == null) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                            } else {
                                val filteredApps = apps!!.filter { it.label.contains(searchQuery, ignoreCase = true) }
                                LazyColumn {
                                    items(filteredApps) { app ->
                                        val isCurrent = currentIcon == app.pkg
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                                                .border(if (isCurrent) 1.dp else 0.dp, if (isCurrent) Color.White.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp))
                                                .clickable { onIconPicked(app.pkg) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text(app.label, color = if (isCurrent) Color.White else Color.Gray)
                                            if (isCurrent) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Symbols
                            var customText by remember { mutableStateOf(if (initialTab == 2) currentIcon else "") }
                            Column {
                                TextField(
                                    value = customText,
                                    onValueChange = { if (it.length <= 3) customText = it },
                                    placeholder = { Text(stringResource(R.string.symbol_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1A1A1A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { if (customText.isNotEmpty()) onIconPicked(customText) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) { Text(stringResource(R.string.select_btn)) }
                            }
                        }
                    }
                }
            }
        }
    }
}





@Composable
fun RotationPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    val options = listOf(
        "PORTRAIT" to stringResource(R.string.rotation_portrait_desc),
        "FACE_UP" to stringResource(R.string.rotation_face_up_desc),
        "FACE_DOWN" to stringResource(R.string.rotation_face_down_desc),
        "LANDSCAPE_LEFT" to stringResource(R.string.rotation_landscape_left_desc),
        "LANDSCAPE_RIGHT" to stringResource(R.string.rotation_landscape_right_desc)
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.cond_rotation_label).substring(0, 11), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                options.forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth().clickable { onConfirmed(key) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = initialValue == key, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = Color.White)
                    }
                }
            }
        }
    }
}





@Composable
fun LightRangePickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    val initialParts = initialValue.split("-")
    val p1 = initialParts.getOrNull(0)?.toFloatOrNull() ?: 0f
    val p2 = initialParts.getOrNull(1)?.toFloatOrNull() ?: 100f

    val startVal = p1.coerceIn(0f, 100f)
    val endVal = p2.coerceIn(0f, 100f).coerceAtLeast(startVal)

    var range by remember { mutableStateOf(startVal..endVal) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.light_threshold), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.light_condition_desc), color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.range_percent_label, range.start.toInt(), range.endInclusive.toInt()), color = Color.White, style = MaterialTheme.typography.headlineMedium)

                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed("${range.start.toInt()}-${range.endInclusive.toInt()}") }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}





@Composable
fun RangePickerDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    val initialParts = initialValue.split("-")
    val p1 = initialParts.getOrNull(0)?.toFloatOrNull() ?: 0f
    val p2 = initialParts.getOrNull(1)?.toFloatOrNull() ?: 100f

    val startVal = p1.coerceIn(0f, 100f)
    val endVal = p2.coerceIn(0f, 100f).coerceAtLeast(startVal)

    var range by remember { mutableStateOf(startVal..endVal) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.range_condition_desc), color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.range_percent_label, range.start.toInt(), range.endInclusive.toInt()), color = Color.White, style = MaterialTheme.typography.headlineMedium)

                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed("${range.start.toInt()}-${range.endInclusive.toInt()}") }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}





@Composable
fun BinaryStatePickerDialog(
    title: String,
    current: String,
    onLabel: String = stringResource(R.string.state_on_label),
    offLabel: String = stringResource(R.string.state_off_label),
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(16.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth().clickable { onConfirmed("ON") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = current == "ON", onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(onLabel, color = Color.White)
                }
                Row(Modifier.fillMaxWidth().clickable { onConfirmed("OFF") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = current == "OFF", onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(offLabel, color = Color.White)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}





@Composable
fun BrightnessPickerDialog(initialValue: Int, onDismiss: () -> Unit, onConfirmed: (Int) -> Unit) {
    var percent by remember { mutableFloatStateOf(initialValue.toFloat()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.brightness_screen_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(32.dp))
                Text("${percent.toInt()}%", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Slider(
                    value = percent,
                    onValueChange = { percent = it },
                     valueRange = 1f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                )
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed(percent.toInt()) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}





@Composable
fun VolumePickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    var stream by remember { mutableStateOf("media") }
    var direction by remember { mutableStateOf("up") }
    var showLevelPicker by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(50f) }

    if (showLevelPicker) {
        Dialog(onDismissRequest = { showLevelPicker = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.action_volume), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(32.dp))
                    Text("${level.toInt()}%", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Slider(
                        value = level,
                        onValueChange = { level = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                    Spacer(Modifier.height(32.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { showLevelPicker = false }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onConfirmed("$stream:set:${level.toInt()}"); showLevelPicker = false; onDismiss() }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_volume), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.stream_label), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("media" to stringResource(R.string.stream_media), "ring" to stringResource(R.string.stream_ring), "notif" to stringResource(R.string.stream_notif)).forEach { (key, label) ->
                        FilterChip(
                            selected = stream == key,
                            onClick = { stream = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.action_label), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("up" to stringResource(R.string.action_up), "down" to stringResource(R.string.action_down)).forEach { (key, label) ->
                            FilterChip(
                                selected = direction == key,
                                onClick = { direction = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("mute" to stringResource(R.string.action_mute), "set" to stringResource(R.string.action_set_percent)).forEach { (key, label) ->
                            FilterChip(
                                selected = direction == key,
                                onClick = {
                                    if (key == "set") {
                                        showLevelPicker = true
                                    } else {
                                        direction = key
                                    }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed("$stream:$direction") }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}





@Composable
fun MediaPickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.media_buttons_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                listOf("play_pause" to "Play / Pause", "next" to stringResource(R.string.media_next), "prev" to stringResource(R.string.media_prev), "stop" to stringResource(R.string.media_stop)).forEach { (key, label) ->
                    TextButton(onClick = { onConfirmed(key) }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}





@Composable
fun UrlPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    var url by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_open_url), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, focusedLabelColor = Color.White)
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed(url) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}





@Composable
fun ThemePickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_change_theme), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                listOf("light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark_short), "toggle" to stringResource(R.string.toggle)).forEach { (key, label) ->
                    TextButton(onClick = { onConfirmed(key) }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}





@Composable
fun AutoBrightnessPickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_auto_brightness), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                listOf("on" to stringResource(R.string.turn_on), "off" to stringResource(R.string.turn_off), "toggle" to stringResource(R.string.toggle)).forEach { (key, label) ->
                    TextButton(onClick = { onConfirmed(key) }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}





@Composable
fun TimeRangePickerDialog(
    start: String?,
    end: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String, String) -> Unit
) {
    var sHour by remember { mutableStateOf(start?.split(":")?.get(0) ?: "00") }
    var sMin by remember { mutableStateOf(start?.split(":")?.get(1) ?: "00") }
    var eHour by remember { mutableStateOf(end?.split(":")?.get(0) ?: "23") }
    var eMin by remember { mutableStateOf(end?.split(":")?.get(1) ?: "59") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_time), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.time_from), color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeField(sHour) { sHour = it }
                    Text(" : ", color = Color.White)
                    TimeField(sMin) { sMin = it }
                }

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.time_to), color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeField(eHour) { eHour = it }
                    Text(" : ", color = Color.White)
                    TimeField(eMin) { eMin = it }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onConfirmed("$sHour:$sMin", "$eHour:$eMin") }) { Text("OK", color = Color.White) }
                }
            }
        }
    }
}





@Composable
fun TimeField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) onValueChange(it) },
        modifier = Modifier.width(64.dp),
        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchPointPickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (Float, Float, String) -> Unit
) {
    val density = LocalDensity.current
    val markerSize = 80.dp
    val markerRadiusPx = with(density) { (markerSize / 2).toPx() }

    val parts = initialValue?.split("_")
    var pos by remember {
        val initial = if (parts != null && parts.size >= 2) Offset(parts[0].toFloat(), parts[1].toFloat()) else Offset(540f, 1100f)
        mutableStateOf(initial)
    }
    var holdDuration by remember {
        mutableStateOf(if (parts != null && parts.size >= 3) parts[2] else "200")
    }
    var showHoldMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.7f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sw = this.constraints.maxWidth.toFloat()
                val sh = this.constraints.maxHeight.toFloat()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showHoldMenu) {
                        Box(Modifier.fillMaxSize().clickable { showHoldMenu = false })
                    }

                    Text(
                        stringResource(R.string.touch_picker_hint),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pos.x - markerRadiusPx).roundToInt(),
                                    (pos.y - markerRadiusPx - (sh * 0.05f)).roundToInt() // Visual shift -5%
                                )
                            }
                            .size(markerSize)
                            .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .pointerInput(Unit) { detectTapGestures(onTap = { showHoldMenu = true }) }
                            .pointerInput(sw, sh) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    pos = Offset((pos.x + dragAmount.x).coerceIn(0f, sw), (pos.y + dragAmount.y).coerceIn(0f, sh))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(12.dp).background(Color.White, CircleShape))
                    }

                    if (showHoldMenu) {
                        HoldMenu(
                            value = holdDuration,
                            onDismiss = { showHoldMenu = false },
                            onConfirm = { holdDuration = it; showHoldMenu = false }
                        )
                    }

                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onConfirmed(pos.x, pos.y, holdDuration) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(stringResource(R.string.confirm_btn)) }
                    }
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipePointPickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (Float, Float, Float, Float, String, String) -> Unit
) {
    val density = LocalDensity.current
    val markerSize = 80.dp
    val markerRadiusPx = with(density) { (markerSize / 2).toPx() }

    val parts = initialValue?.split("_")
    var startPos by remember {
        val initial = if (parts != null && parts.size >= 4) Offset(parts[0].toFloat(), parts[1].toFloat()) else Offset(300f, 1100f)
        mutableStateOf(initial)
    }
    var endPos by remember {
        val initial = if (parts != null && parts.size >= 4) Offset(parts[2].toFloat(), parts[3].toFloat()) else Offset(700f, 1100f)
        mutableStateOf(initial)
    }
    var holdStart by remember { mutableStateOf(if (parts != null && parts.size >= 5) parts[4] else "0") }
    var holdEnd by remember { mutableStateOf(if (parts != null && parts.size >= 6) parts[5] else "0") }

    var showHoldStart by remember { mutableStateOf(false) }
    var showHoldEnd by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "swipe")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)),
        label = "progress"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.8f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sw = this.constraints.maxWidth.toFloat()
                val sh = this.constraints.maxHeight.toFloat()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showHoldStart || showHoldEnd) {
                        Box(Modifier.fillMaxSize().clickable { showHoldStart = false; showHoldEnd = false })
                    }

                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(sw, sh) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            startPos = Offset((startPos.x + dragAmount.x).coerceIn(0f, sw), (startPos.y + dragAmount.y).coerceIn(0f, sh))
                            endPos = Offset((endPos.x + dragAmount.x).coerceIn(0f, sw), (endPos.y + dragAmount.y).coerceIn(0f, sh))
                        }
                    }) {
                        val visualShift = size.height * 0.05f
                        val vStart = Offset(startPos.x, startPos.y - visualShift)
                        val vEnd = Offset(endPos.x, endPos.y - visualShift)

                        drawLine(color = Color.Gray, start = vStart, end = vEnd, strokeWidth = 8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        val currentPos = Offset(lerp(vStart.x, vEnd.x, progress), lerp(vStart.y, vEnd.y, progress))
                        drawCircle(color = Color.White, radius = 20f, center = currentPos)
                    }

                    Text(stringResource(R.string.swipe_picker_hint), color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp), style = MaterialTheme.typography.titleMedium)

                    // Start Marker
                    Box(modifier = Modifier.offset { IntOffset((startPos.x - markerRadiusPx).roundToInt(), (startPos.y - markerRadiusPx - (sh * 0.05f)).roundToInt()) }.size(markerSize)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape).border(2.dp, Color.White, CircleShape)
                        .pointerInput(Unit) { detectTapGestures(onTap = { showHoldStart = true }) }
                        .pointerInput(sw, sh) { detectDragGestures { change, dragAmount -> change.consume(); startPos = Offset((startPos.x + dragAmount.x).coerceIn(0f, sw), (startPos.y + dragAmount.y).coerceIn(0f, sh)) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Color.White)
                    }

                    // End Marker
                    Box(modifier = Modifier.offset { IntOffset((endPos.x - markerRadiusPx).roundToInt(), (endPos.y - markerRadiusPx - (sh * 0.05f)).roundToInt()) }.size(markerSize)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape).border(2.dp, Color.White, CircleShape)
                        .pointerInput(Unit) { detectTapGestures(onTap = { showHoldEnd = true }) }
                        .pointerInput(sw, sh) { detectDragGestures { change, dragAmount -> change.consume(); endPos = Offset((endPos.x + dragAmount.x).coerceIn(0f, sw), (endPos.y + dragAmount.y).coerceIn(0f, sh)) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("B", color = Color.Black)
                    }

                    if (showHoldStart) {
                        HoldMenu(holdStart, onDismiss = { showHoldStart = false }, onConfirm = { holdStart = it; showHoldStart = false })
                    }
                    if (showHoldEnd) {
                        HoldMenu(holdEnd, onDismiss = { showHoldEnd = false }, onConfirm = { holdEnd = it; showHoldEnd = false })
                    }

                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onConfirmed(startPos.x, startPos.y, endPos.x, endPos.y, holdStart, holdEnd) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(stringResource(R.string.confirm_btn)) }
                    }
                }
            }
        }
    }
}





@Composable
fun HoldMenu(value: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(0.85f), shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.hold_ms_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { if (it.all { c -> c.isDigit() }) text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White)
                )
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirm(text.ifEmpty { "0" }) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}





@Composable
fun DelayPickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue ?: "1.0") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${stringResource(R.string.action_delay)} (${stringResource(R.string.unit_seconds)})", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) }
                    TextButton(onClick = { onConfirmed(text) }) { Text("OK", color = Color.White) }
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(onOpenSettings: () -> Unit, onShowAccessibilityError: () -> Unit) {
    val ctx = LocalContext.current
    var hasWriteSettings by remember { mutableStateOf(Settings.System.canWrite(ctx)) }

    // Check permission periodically or when returning to app
    LaunchedEffect(Unit) {
        while(true) {
            hasWriteSettings = Settings.System.canWrite(ctx)
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TypewriterTitle(targetText = stringResource(R.string.games_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .drawBehind {
                        val blurHeight = 20.dp.toPx()
                        drawRect(color = Color.Black, size = size)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Glyph Access Check Section
            Surface(
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (hasWriteSettings) Color.White.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.glyph_control_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (!hasWriteSettings) {
                        Text(stringResource(R.string.glyph_permission_msg), color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${ctx.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.grant_permission_btn))
                        }
                    } else {
                        Text(stringResource(R.string.glyph_permission_ok), color = Color.Green.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                    val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                        action = "com.HojuNK.essent_all.TOGGLE_GLYPHS"
                                    }
                                    ctx.startService(intent)
                                } else {
                                    onShowAccessibilityError()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FlashOn, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.toggle_glyphs_btn))
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                    val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                        action = "com.HojuNK.essent_all.TOGGLE_GLYPH_RUNNER"
                                    }
                                    ctx.startService(intent)
                                } else {
                                    onShowAccessibilityError()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cached, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.glyph_runner_btn))
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(stringResource(R.string.games_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.in_development_msg), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(Modifier.weight(1f))
        }
    }
}






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, onOpenSettings: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { TypewriterTitle(targetText = title) },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.in_development_msg), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventPriorityDialog(
    initialEvents: List<EventRule>,
    onSave: (List<EventRule>) -> Unit,
    onDismiss: () -> Unit
) {
    var tempEvents by remember { mutableStateOf(initialEvents) }
    var selectedSlot by remember { mutableStateOf(TapSlot.SINGLE) }

    val filteredEvents = remember(tempEvents, selectedSlot) {
        tempEvents.filter { it.triggerGesture == selectedSlot }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.event_priority_title), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    },
                    actions = {
                        Surface(
                            onClick = { onSave(tempEvents); onDismiss() },
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.save), color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
                )

                // Slot Tabs
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(TapSlot.SINGLE, TapSlot.LONG, TapSlot.DOUBLE, TapSlot.TRIPLE).forEach { slot ->
                        val selected = selectedSlot == slot
                        Surface(
                            onClick = { selectedSlot = slot },
                            color = if (selected) Color.White else Color(0xFF2B2B2B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when(slot) {
                                        TapSlot.SINGLE -> stringResource(R.string.slot_single_short)
                                        TapSlot.LONG -> stringResource(R.string.slot_long_short)
                                        TapSlot.DOUBLE -> stringResource(R.string.slot_double_short)
                                        TapSlot.TRIPLE -> stringResource(R.string.slot_triple_short)
                                        else -> ""
                                    },
                                    color = if (selected) Color.Black else Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.priority_drag_hint),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(16.dp))

                var draggedIndex by remember { mutableIntStateOf(-1) }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredEvents, key = { _, item -> item.id }) { index, event ->
                        PriorityEventRow(
                            event = event,
                            index = index,
                            isDragged = draggedIndex == index,
                            onDragStart = { draggedIndex = index },
                            onDragEnd = { draggedIndex = -1 },
                            onMove = { from, to ->
                                if (to in filteredEvents.indices) {
                                    val itemToMove = filteredEvents[from]
                                    val targetItem = filteredEvents[to]

                                    val newMaster = tempEvents.toMutableList()
                                    val fromIdxMaster = newMaster.indexOf(itemToMove)
                                    val toIdxMaster = newMaster.indexOf(targetItem)

                                    if (fromIdxMaster != -1 && toIdxMaster != -1) {
                                        newMaster.removeAt(fromIdxMaster)
                                        newMaster.add(toIdxMaster, itemToMove)
                                        tempEvents = newMaster
                                        draggedIndex = to
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}





@Composable
fun LazyItemScope.PriorityEventRow(
    event: EventRule,
    index: Int,
    isDragged: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val scale by animateFloatAsState(if (isDragged) 1.05f else 1f)
    val elevation by animateDpAsState(if (isDragged) 8.dp else 2.dp)
    val density = LocalDensity.current

    val currentIndex by rememberUpdatedState(index)
    val currentOnMove by rememberUpdatedState(onMove)

    var dragOffset by remember { mutableStateOf(0f) }

    // Fixed heights: Content 64dp + 8dp spacing = 72dp total step
    val itemHeight = 64.dp
    val spacing = 8.dp
    val fullStepPx = with(density) { (itemHeight + spacing).toPx() }

    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = elevation,
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .animateItem(
                // Отключаем анимацию перемещения только для того, кто сейчас в руках
                placementSpec = if (isDragged) null else spring(stiffness = Spring.StiffnessMediumLow)
            )
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isDragged) dragOffset else 0f
            }
            .pointerInput(event.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragOffset = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        dragOffset = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y

                        val moveBy = (dragOffset / fullStepPx).roundToInt()
                        if (moveBy != 0) {
                            val targetIndex = currentIndex + moveBy
                            if (targetIndex != currentIndex) {
                                currentOnMove(currentIndex, targetIndex)
                                // Точная коррекция смещения, чтобы палец остался на той же точке элемента
                                dragOffset -= moveBy * fullStepPx
                            }
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${index + 1}.", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(16.dp))
            EventIconDisplay(event.icon, displaySize = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(event.title, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Icon(Icons.Default.DragHandle, null, tint = Color.Gray)
        }
    }
}






@Composable
fun PickRow(label: String, icon: ImageBitmap?, selected: Boolean, subLabel: String? = null, onClick: () -> Unit) {
    Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.clickable(onClick = onClick).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { androidx.compose.foundation.Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp)); Spacer(Modifier.width(12.dp)) }
            else Spacer(Modifier.size(36.dp).width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (subLabel != null) {
                    Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun WaitConfirmationDialog(
    initialValue: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val prefix = Config.ACTION_WAIT_CONFIRMATION_PREFIX
    val parts = initialValue?.removePrefix(prefix)?.split(",")
    var keepOnClick by remember { mutableStateOf(parts?.getOrNull(0)?.toBoolean() ?: false) }
    var keepOnOutside by remember { mutableStateOf(parts?.getOrNull(1)?.toBoolean() ?: false) }
    var keepOnKey by remember { mutableStateOf(parts?.getOrNull(2)?.toBoolean() ?: false) }
    var autoHideSeconds by remember { mutableStateOf(parts?.getOrNull(3)?.toIntOrNull() ?: 5) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.shortcut_settings), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                ShortcutToggle(stringResource(R.string.keep_on_click), keepOnClick) { keepOnClick = it }
                ShortcutToggle(stringResource(R.string.keep_on_outside), keepOnOutside) { keepOnOutside = it }
                ShortcutToggle(stringResource(R.string.keep_on_key), keepOnKey) { keepOnKey = it }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.auto_hide), style = MaterialTheme.typography.labelLarge, color = Color.Gray)

                val autoHideOptions = listOf(
                    0 to stringResource(R.string.never_hide),
                    5 to "5 ${stringResource(R.string.unit_seconds)}",
                    10 to "10 ${stringResource(R.string.unit_seconds)}",
                    20 to "20 ${stringResource(R.string.unit_seconds)}"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(autoHideOptions) { (secs, label) ->
                        val selected = autoHideSeconds == secs
                        Surface(
                            onClick = { autoHideSeconds = secs },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color.White else Color(0xFF2B2B2B),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = {
                        val canHide = !keepOnClick || !keepOnOutside || !keepOnKey || autoHideSeconds > 0
                        if (canHide) {
                            onConfirm("$prefix$keepOnClick,$keepOnOutside,$keepOnKey,$autoHideSeconds")
                        } else {
                            android.widget.Toast.makeText(ctx, ctx.getString(R.string.choose_hide_method_msg), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}
