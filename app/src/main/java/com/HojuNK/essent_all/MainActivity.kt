package com.HojuNK.essent_all

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.CancellationException
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.res.Configuration
import android.view.Surface as ViewSurface
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.screens.*
import com.HojuNK.essent_all.ui.components.*
import com.HojuNK.essent_all.ui.editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 1001)
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1001)
            }
        }

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
    val activity = ctx as? ComponentActivity
    val window = activity?.window
    val keyMap by Config.flow(ctx).collectAsState(initial = KeyMap())
    var loadingStage by remember { mutableStateOf(LoadingStage.INITIAL) }
    var apps by remember { mutableStateOf<List<AppInfo>?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAccessibilityError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Immersive Mode Logic for Landscape
    LaunchedEffect(isLandscape) {
        window?.let { win ->
            val controller = WindowCompat.getInsetsController(win, win.decorView)
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var editSlot by remember { mutableStateOf<TapSlot?>(null) }
    var activeBlockDetail by remember { mutableStateOf<Pair<String, List<TapSlot>>?>(null) }
    var recordedMacroActions by remember { mutableStateOf<List<String>?>(null) }
    var editingMacro by remember { mutableStateOf<Macro?>(null) }
    var editingEvent by remember { mutableStateOf<EventRule?>(null) }
    var editingGlyphPattern by remember { mutableStateOf<Macro?>(null) }
    var forcePortrait by remember { mutableStateOf(false) }
    var showEventPriority by remember { mutableStateOf(false) }
    var isEventsSelectionMode by remember { mutableStateOf(false) }
    var isMacrosSelectionMode by remember { mutableStateOf(false) }
    var isGlyphsSelectionMode by remember { mutableStateOf(false) }
    var openedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedEventsCount by remember { mutableStateOf(0) }
    var landscapeSelectAllAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var landscapeGroupAction by remember { mutableStateOf<EventGroup?>(null) }
    var landscapeGroupEditAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var landscapeGroupDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        GlyphController.init(ctx)
    }

    // Orientation Locking Logic
    LaunchedEffect(showSettings, recordedMacroActions, editingMacro, editingEvent, editingGlyphPattern, editSlot, forcePortrait, showEventPriority) {
        val shouldLock = showSettings || recordedMacroActions != null || editingMacro != null || editingEvent != null || editingGlyphPattern != null || editSlot != null || forcePortrait || showEventPriority
        activity?.requestedOrientation = if (shouldLock) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    LaunchedEffect(Unit) {
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
            } else {
                delay(300)
                loadingStage = LoadingStage.TRANSITIONING
                delay(400)
                loadingStage = LoadingStage.FULLY_LOADED
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

    var radialMenuState by remember { mutableStateOf<RadialMenuState?>(null) }
    var lastMenuCenter by remember { mutableStateOf(Offset.Zero) }
    val blurProgress by animateFloatAsState(
        targetValue = if (radialMenuState != null) 1f else 0f,
        animationSpec = if (radialMenuState != null) {
            tween(600, easing = LinearOutSlowInEasing)
        } else {
            tween(250, easing = FastOutLinearInEasing)
        },
        label = "blur_wave_progress"
    )

    LaunchedEffect(radialMenuState) {
        if (radialMenuState != null) {
            lastMenuCenter = radialMenuState!!.center
        }
    }

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
        LocalRadialMenuState provides radialMenuState,
        LocalOrientationLock provides { forcePortrait = it }
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                val pagerState = rememberPagerState { NavScreen.entries.size }
                val isAppLoading = loadingStage != LoadingStage.FULLY_LOADED && loadingStage != LoadingStage.TRANSITIONING
                val anyEditorVisible = showSettings || recordedMacroActions != null || editingMacro != null || editingEvent != null || editingGlyphPattern != null || editSlot != null || activeBlockDetail != null || showEventPriority || forcePortrait
                var isActionPickerOpen by remember { mutableStateOf(false) }
                val currentScreen = NavScreen.entries[pagerState.currentPage]
                val isSelectionActive = (currentScreen == NavScreen.EVENTS && isEventsSelectionMode) ||
                                        (currentScreen == NavScreen.MACROS && isMacrosSelectionMode) ||
                                        (currentScreen == NavScreen.GAMES && isGlyphsSelectionMode)
                val isHotbarHidden = isAppLoading || anyEditorVisible || isActionPickerOpen || isSelectionActive

                var predictiveBackTarget by remember { mutableStateOf<String?>(null) }
                var lastPredictiveBackTarget by remember { mutableStateOf<String?>(null) }
                var isPredictiveBackGesture by remember { mutableStateOf(false) }
                val predictiveBackAnim = remember { Animatable(0f) }
                val predictiveBackProgress = predictiveBackAnim.value

                val activeOverlay = when {
                    editingEvent != null -> "editing_event"
                    editingMacro != null -> "editing_macro"
                    recordedMacroActions != null -> "recorded_macro"
                    editingGlyphPattern != null -> "editing_glyph"
                    editSlot != null -> "edit_slot"
                    activeBlockDetail != null -> "action_picker"
                    showSettings -> "settings"
                    showEventPriority -> "event_priority"
                    else -> null
                }

                PredictiveBackHandler(enabled = activeOverlay != null) { progressFlow ->
                    val target = activeOverlay ?: return@PredictiveBackHandler
                    predictiveBackTarget = target
                    lastPredictiveBackTarget = target
                    isPredictiveBackGesture = true
                    try {
                        progressFlow.collect { backEvent ->
                            predictiveBackAnim.snapTo(backEvent.progress)
                        }
                        predictiveBackAnim.animateTo(1f, animationSpec = tween(150, easing = LinearOutSlowInEasing))
                        when (target) {
                            "editing_event" -> editingEvent = null
                            "editing_macro" -> editingMacro = null
                            "recorded_macro" -> recordedMacroActions = null
                            "editing_glyph" -> editingGlyphPattern = null
                            "edit_slot" -> editSlot = null
                            "action_picker" -> activeBlockDetail = null
                            "settings" -> showSettings = false
                            "event_priority" -> showEventPriority = false
                        }
                    } catch (_: CancellationException) {
                        predictiveBackAnim.animateTo(
                            0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                        )
                        lastPredictiveBackTarget = null
                    } finally {
                        predictiveBackAnim.snapTo(0f)
                        predictiveBackTarget = null
                        isPredictiveBackGesture = false
                    }
                }

                val rotation = ctx.display?.rotation ?: ViewSurface.ROTATION_0
                val density = LocalDensity.current
                val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                val maxDist = kotlin.math.sqrt((screenWidthPx * screenWidthPx + screenHeightPx * screenHeightPx).toDouble()).toFloat()

                // Основной контейнер оверлеев
                Box(modifier = Modifier.fillMaxSize()) {
                    val currentScreen = NavScreen.entries[pagerState.currentPage]
                    var hotbarWidth by remember { mutableStateOf(0f) }
                    var headerWidth by remember { mutableStateOf(0f) }
                    
                    // Независимая визуальная позиция для хотбара
                    val visualPagerPosition = remember { Animatable(0f) }
                    val isTabClicking = remember { mutableStateOf(false) }
                    val jumpSource = remember { mutableStateOf(0) }
                    val jumpTarget = remember { mutableStateOf(0) }
                    val isDraggingHotbar = remember { mutableStateOf(false) }
                    
                    // Анимация "глубины" при перетаскивании
                    val dragDepth by animateFloatAsState(
                        targetValue = if (isDraggingHotbar.value) 0.96f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                        label = "dragDepth"
                    )
                    
                    // Синхронизация с пейджером (только если мы не кликаем по табам)
                    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
                        if (!isTabClicking.value) {
                            visualPagerPosition.snapTo(pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        }
                    }

                    // 1. Контент (HorizontalPager) - Самый нижний слой в Box
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = loadingStage == LoadingStage.FULLY_LOADED && !isHotbarHidden,
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer {
                                val currentScale = dragDepth
                                scaleX = currentScale
                                scaleY = currentScale
                                shadowElevation = (1f - currentScale) * 50f
                                clip = isDraggingHotbar.value
                                shape = RoundedCornerShape(with(density) { ((1f - currentScale) * 40f).dp.toPx() }.dp)
                                
                                if (blurProgress > 0.001f && !isLandscape) {
                                    renderEffect = createNothingWaveEffect(
                                        radius = blurProgress * 45f,
                                        progress = blurProgress,
                                        center = lastMenuCenter,
                                        maxDist = maxDist
                                    ).asComposeRenderEffect()
                                    alpha = lerp(1.0f, 0.6f, blurProgress)
                                } else {
                                    renderEffect = null
                                    alpha = 1.0f
                                }
                            }
                    ) { page ->
                        val screen = NavScreen.entries[page]
                        
                        // Скрываем промежуточные страницы при "прыжках" через пункты хотбара
                        val isIntermediary = remember(isTabClicking.value, jumpSource.value, jumpTarget.value, page) {
                            if (isTabClicking.value && abs(jumpTarget.value - jumpSource.value) > 1) {
                                page != jumpSource.value && page != jumpTarget.value
                            } else false
                        }

                        Box(
                            Modifier.fillMaxSize() // ПРИНУДИТЕЛЬНО РАСТЯГИВАЕМ НА ВЕСЬ ЭКРАН
                                .graphicsLayer {
                                if (isIntermediary) {
                                    alpha = 0f
                                } else {
                                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                    alpha = if (pageOffset > 1f) 0f else {
                                        lerp(start = 0f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
                                    }
                                    scaleX = 1f
                                    scaleY = 1f
                                }
                            }
                        ) {
                            when (screen) {
                                NavScreen.KEY -> ConfigScreen(
                                    keyMap = keyMap, 
                                    apps = apps, 
                                    onOpenSettings = { showSettings = true },
                                    onActionPickerStateChange = { isActionPickerOpen = it },
                                    onOpenBlockDetail = { title, slots -> activeBlockDetail = Pair(title, slots) }
                                )
                                NavScreen.EVENTS -> EventsScreen(
                                    keyMap = keyMap, 
                                    apps = apps, 
                                    onOpenSettings = { showSettings = true }, 
                                    onEditEvent = { editingEvent = it },
                                    onOpenPriority = { showEventPriority = true },
                                    isSelectionMode = isEventsSelectionMode,
                                    onSelectionModeChange = { isEventsSelectionMode = it },
                                    onOpenedGroupChanged = { group ->
                                        openedGroupId = group?.id
                                        landscapeGroupAction = group
                                    },
                                    onSelectedCountChanged = { count ->
                                        selectedEventsCount = count
                                    },
                                    onSelectAllActionChanged = { action ->
                                        landscapeSelectAllAction = action
                                    },
                                    onGroupEditActionChanged = { action ->
                                        landscapeGroupEditAction = action
                                    },
                                    onGroupDeleteActionChanged = { action ->
                                        landscapeGroupDeleteAction = action
                                    }
                                )
                                NavScreen.MACROS -> MacrosScreen(
                                    keyMap = keyMap, 
                                    apps = apps, 
                                    onOpenSettings = { showSettings = true }, 
                                    onEditMacro = { editingMacro = it }, 
                                    onShowAccessibilityError = { showAccessibilityError = true },
                                    isSelectionMode = isMacrosSelectionMode,
                                    onSelectionModeChange = { isMacrosSelectionMode = it }
                                )
                                NavScreen.GAMES -> GlyphPatternsScreen(
                                    keyMap = keyMap, 
                                    apps = apps, 
                                    onOpenSettings = { showSettings = true }, 
                                    onEditPattern = { editingGlyphPattern = it },
                                    onShowAccessibilityError = { showAccessibilityError = true },
                                    isSelectionMode = isGlyphsSelectionMode,
                                    onSelectionModeChange = { isGlyphsSelectionMode = it }
                                )
                            }
                        }
                    }

                    // 2. Хедер в ландшафте
                    if (isLandscape) {
                        val headerAlignment = if (rotation == ViewSurface.ROTATION_90) Alignment.CenterStart else Alignment.CenterEnd
                        val headerRotation = if (rotation == ViewSurface.ROTATION_90) -90f else 90f
                        var headerOffset by remember { mutableStateOf(Offset.Zero) }

                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier
                                .align(headerAlignment)
                                .onGloballyPositioned { 
                                    headerWidth = it.size.width.toFloat() 
                                    headerOffset = it.positionInRoot()
                                }
                                .width(configuration.screenHeightDp.dp)
                                .height(70.dp)
                                .graphicsLayer {
                                    rotationZ = headerRotation
                                    val currentCenterOffset = headerWidth / 2f
                                    val targetCenterOffset = 35.dp.toPx()
                                    translationX = if (headerAlignment == Alignment.CenterStart)
                                        -(currentCenterOffset - targetCenterOffset)
                                        else (currentCenterOffset - targetCenterOffset)
                                }
                                .graphicsLayer {
                                    renderEffect = null
                                    alpha = 1.0f
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (currentScreen == NavScreen.EVENTS && isEventsSelectionMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isEventsSelectionMode = false }) {
                                            OrientationRotatable { Icon(Icons.Default.Close, null, tint = Color.White) }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.selected_count, selectedEventsCount),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = { landscapeSelectAllAction?.invoke() }) {
                                        OrientationRotatable { Icon(Icons.Default.SelectAll, null, tint = Color.White) }
                                    }
                                } else if (currentScreen == NavScreen.EVENTS && landscapeGroupAction != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { openedGroupId = null; landscapeGroupAction = null }) {
                                            OrientationRotatable { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(landscapeGroupAction!!.title, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { landscapeGroupEditAction?.invoke() }) {
                                            OrientationRotatable { Icon(Icons.Default.Edit, null, tint = Color.White) }
                                        }
                                        IconButton(onClick = {
                                            val nextMode = if (keyMap.eventsViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                            scope.launch { Config.setEventsViewMode(ctx, nextMode) }
                                        }) {
                                            OrientationRotatable { Icon(if (keyMap.eventsViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView, null, tint = Color.White) }
                                        }
                                        IconButton(onClick = { showSettings = true }) {
                                            OrientationRotatable { Icon(Icons.Default.Settings, null, tint = Color.White) }
                                        }
                                    }
                                } else {
                                    TypewriterTitle(animate = currentScreen == NavScreen.KEY)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (currentScreen == NavScreen.EVENTS) {
                                            IconButton(onClick = { showEventPriority = true }) {
                                                OrientationRotatable { Icon(Icons.Default.LowPriority, null, tint = Color.White) }
                                            }
                                            IconButton(onClick = { isEventsSelectionMode = !isEventsSelectionMode }) {
                                                OrientationRotatable { Icon(Icons.Default.Checklist, null, tint = Color.White) }
                                            }
                                            IconButton(onClick = {
                                                val nextMode = if (keyMap.eventsViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                                scope.launch { Config.setEventsViewMode(ctx, nextMode) }
                                            }) {
                                                OrientationRotatable { Icon(if (keyMap.eventsViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView, null, tint = Color.White) }
                                            }
                                        }
                                        if (currentScreen == NavScreen.MACROS) {
                                            IconButton(onClick = {
                                                val nextMode = if (keyMap.macrosViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                                scope.launch { Config.setMacrosViewMode(ctx, nextMode) }
                                            }) {
                                                OrientationRotatable { Icon(if (keyMap.macrosViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView, null, tint = Color.White) }
                                            }
                                        }
                                        val glyphActive by GlyphController.glyphActive.collectAsState()
                                        if (currentScreen == NavScreen.GAMES) {
                                            IconButton(onClick = {
                                                GlyphController.toggleGlyph()
                                            }) {
                                                OrientationRotatable { 
                                                    Icon(
                                                        Icons.Default.Lightbulb, 
                                                        null, 
                                                        tint = if (glyphActive) Color(0xFFFFD700) else Color.White
                                                    ) 
                                                }
                                            }
                                        }
                                        IconButton(onClick = { showSettings = true }) {
                                            OrientationRotatable { Icon(Icons.Default.Settings, null, tint = Color.White) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Хотбар
                    // Фоновое размытие и затемнение для зоны хотбара
                    // 3. Хотбар
                    // Фоновое размытие и затемнение для зоны хотбара
                    val hotbarBgAlpha by animateFloatAsState(
                        targetValue = if (isHotbarHidden) 0f else 1f,
                        animationSpec = tween(400),
                        label = "hotbarBgAlpha"
                    )

                    if (!isLandscape && hotbarBgAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(150.dp)
                                .graphicsLayer { alpha = hotbarBgAlpha }
                                .drawBehind {
                                    val blurHeight = 45.dp.toPx()
                                    val solidHeight = size.height - blurHeight
                                    // Верхняя размытая часть (переход от прозрачного к полутемному)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                            startY = 0f,
                                            endY = blurHeight
                                        ),
                                        size = Size(size.width, blurHeight)
                                    )
                                    // Нижняя плотная часть (от полутемного к глухому черному)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Black),
                                            startY = blurHeight,
                                            endY = size.height
                                        ),
                                        topLeft = Offset(0f, blurHeight),
                                        size = Size(size.width, solidHeight)
                                    )
                                }
                        )
                    } else {
                        // Landscape: No background effects under hotbar/header as requested
                    }

                    var navOffset by remember { mutableStateOf(Offset.Zero) }
                    PillBottomNavigation(
                        pagerPosition = visualPagerPosition.value,
                        loadingStage = loadingStage,
                        isDragging = isDraggingHotbar.value,
                        isHidden = isHotbarHidden,
                        onScreenSelected = { screen ->
                            if (!isHotbarHidden) {
                                scope.launch { 
                                    jumpSource.value = pagerState.currentPage
                                    jumpTarget.value = screen.ordinal
                                    isTabClicking.value = true
                                    launch {
                                        visualPagerPosition.animateTo(
                                            screen.ordinal.toFloat(),
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                    launch {
                                        pagerState.animateScrollToPage(
                                            screen.ordinal,
                                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                        )
                                        isTabClicking.value = false
                                    }
                                }
                            }
                        },
                        onDrag = { delta ->
                            if (!isHotbarHidden) {
                                isDraggingHotbar.value = true
                                // 1. Ширина страницы в ландшафте меньше на 140dp из-за паддингов
                                val sidePaddingPx = if (isLandscape) with(density) { 70.dp.toPx() } else 0f
                                val effectivePageSize = screenWidthPx - (sidePaddingPx * 2)

                                // 2. Чувствительность хотбара (60dp на 1 страницу)
                                val stepPx = with(density) { 60.dp.toPx() }
                                val pagerDelta = delta / stepPx
                                
                                // 3. Мгновенно двигаем пейджер
                                pagerState.dispatchRawDelta(pagerDelta * effectivePageSize)
                                
                                // 4. Мгновенно обновляем визуальный селектор через snapTo
                                scope.launch {
                                    visualPagerPosition.snapTo(pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                }
                            }
                        },
                        onDragEnd = {
                            if (!isHotbarHidden) {
                                isDraggingHotbar.value = false
                                scope.launch {
                                    val targetPage = (pagerState.currentPage + pagerState.currentPageOffsetFraction + 0.5f).toInt()
                                        .coerceIn(0, NavScreen.entries.size - 1)
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        },
                            modifier = when {
                                isLandscape && rotation == ViewSurface.ROTATION_90 ->
                                    Modifier.align(Alignment.CenterEnd)
                                        .onGloballyPositioned { hotbarWidth = it.size.width.toFloat(); navOffset = it.positionInRoot() }
                                        .graphicsLayer {
                                            renderEffect = null
                                            alpha = 1.0f
                                            rotationZ = -90f
                                            val currentCenterOffset = hotbarWidth / 2f
                                            val targetCenterOffset = 83.dp.toPx()
                                            translationX = (currentCenterOffset - targetCenterOffset)
                                        }
                                isLandscape && rotation == ViewSurface.ROTATION_270 ->
                                    Modifier.align(Alignment.CenterStart)
                                        .onGloballyPositioned { hotbarWidth = it.size.width.toFloat(); navOffset = it.positionInRoot() }
                                        .graphicsLayer {
                                            renderEffect = null
                                            alpha = 1.0f
                                            rotationZ = 90f
                                            val currentCenterOffset = hotbarWidth / 2f
                                            val targetCenterOffset = 83.dp.toPx()
                                            translationX = -(currentCenterOffset - targetCenterOffset)
                                        }
                                else -> Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                                    .onGloballyPositioned { navOffset = it.positionInRoot() }
                                    .graphicsLayer {
                                        if (blurProgress > 0.001f) {
                                            renderEffect = createNothingWaveEffect(radius = blurProgress * 45f, progress = blurProgress, center = lastMenuCenter, maxDist = maxDist, layerOffset = navOffset).asComposeRenderEffect()
                                            alpha = lerp(1.0f, 0.6f, blurProgress)
                                        } else { renderEffect = null; alpha = 1.0f }
                                    }
                            }
                        )
                }

                // Вспомогательные слои поверх основного Box
                val isSettingsPredictiveDismissed = lastPredictiveBackTarget == "settings" && !showSettings
                AnimatedContent(
                    targetState = showSettings,
                    transitionSpec = {
                        if (isSettingsPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "settings_anim"
                ) { visible ->
                    if (visible) {
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "settings") {
                            predictiveBackProgress
                        } else if (isSettingsPredictiveDismissed) {
                            1f
                        } else 0f
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                },
                            color = MaterialTheme.colorScheme.background
                        ) {
                            SettingsScreen(keyMap, onBack = { showSettings = false })
                        }
                    }
                }

                val isEventPriorityPredictiveDismissed = lastPredictiveBackTarget == "event_priority" && !showEventPriority
                AnimatedContent(
                    targetState = showEventPriority,
                    transitionSpec = {
                        if (isEventPriorityPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "event_priority_anim"
                ) { visible ->
                    if (visible) {
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "event_priority") {
                            predictiveBackProgress
                        } else if (isEventPriorityPredictiveDismissed) {
                            1f
                        } else 0f
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                },
                            color = MaterialTheme.colorScheme.background
                        ) {
                            EventPriorityScreen(
                                keyMap = keyMap,
                                onBack = { showEventPriority = false },
                                onSave = { newList ->
                                    scope.launch { Config.setEvents(ctx, newList) }
                                }
                            )
                        }
                    }
                }

                val isRecordedMacroPredictiveDismissed = lastPredictiveBackTarget == "recorded_macro" && recordedMacroActions == null
                AnimatedContent(
                    targetState = recordedMacroActions,
                    transitionSpec = {
                        if (isRecordedMacroPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "recorded_macro_anim"
                ) { actions ->
                    if (actions != null) {
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "recorded_macro") {
                            predictiveBackProgress
                        } else if (isRecordedMacroPredictiveDismissed) {
                            1f
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                }
                        ) {
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
                    }
                }

                val isEditingMacroPredictiveDismissed = lastPredictiveBackTarget == "editing_macro" && editingMacro == null
                AnimatedContent(
                    targetState = editingMacro,
                    transitionSpec = {
                        if (isEditingMacroPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "editing_macro_anim"
                ) { macro ->
                    if (macro != null) {
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "editing_macro") {
                            predictiveBackProgress
                        } else if (isEditingMacroPredictiveDismissed) {
                            1f
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                }
                        ) {
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
                    }
                }

                val isEditingEventPredictiveDismissed = lastPredictiveBackTarget == "editing_event" && editingEvent == null
                AnimatedContent(
                    targetState = editingEvent,
                    transitionSpec = {
                        if (isEditingEventPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "editing_event_anim"
                ) { rule ->
                    if (rule != null) {
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "editing_event") {
                            predictiveBackProgress
                        } else if (isEditingEventPredictiveDismissed) {
                            1f
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                }
                        ) {
                            EventAddPanel(
                                initialRule = rule,
                                macros = keyMap.macros,
                                glyphPatterns = keyMap.glyphPatterns,
                                apps = apps,
                                onDismiss = { editingEvent = null },
                                onSave = { updated ->
                                    scope.launch { Config.updateEvent(ctx, updated) }
                                    editingEvent = null
                                }
                            )
                        }
                    }
                }

                val isEditingGlyphPredictiveDismissed = lastPredictiveBackTarget == "editing_glyph" && editingGlyphPattern == null
                AnimatedContent(
                    targetState = editingGlyphPattern,
                    transitionSpec = {
                        if (isEditingGlyphPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "editing_glyph_anim"
                ) { pattern ->
                    if (pattern != null) {
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "editing_glyph") {
                            predictiveBackProgress
                        } else if (isEditingGlyphPredictiveDismissed) {
                            1f
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                }
                        ) {
                            GlyphAddPanel(
                                initialPattern = pattern,
                                apps = apps,
                                onDismiss = { editingGlyphPattern = null },
                                onSave = { updated ->
                                    scope.launch { Config.updateGlyphPattern(ctx, updated) }
                                    editingGlyphPattern = null
                                }
                            )
                        }
                    }
                }

                val isEditSlotPredictiveDismissed = lastPredictiveBackTarget == "edit_slot" && editSlot == null
                AnimatedContent(
                    targetState = editSlot,
                    transitionSpec = {
                        if (isEditSlotPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "edit_slot_anim"
                ) { slot ->
                    if (slot != null) {
                        val config = keyMap.customConfigs[slot] ?: CustomConfig()
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "edit_slot") {
                            predictiveBackProgress
                        } else if (isEditSlotPredictiveDismissed) {
                            1f
                        } else 0f
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = progress * screenHeightPx
                                    scaleX = 1f - progress * 0.08f
                                    scaleY = 1f - progress * 0.08f
                                    alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)
                                    clip = progress > 0.001f
                                    shape = RoundedCornerShape(with(density) { (progress * 28f).dp.toPx() }.dp)
                                },
                            color = Color.Black
                        ) {
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
                }

                val isActionPickerPredictiveDismissed = lastPredictiveBackTarget == "action_picker" && activeBlockDetail == null
                AnimatedContent(
                    targetState = activeBlockDetail,
                    transitionSpec = {
                        if (isActionPickerPredictiveDismissed) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(200)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeOut(tween(200))
                        }
                    },
                    label = "action_picker_anim"
                ) { detail ->
                    if (detail != null) {
                        val (title, slots) = detail
                        val progress = if (isPredictiveBackGesture && predictiveBackTarget == "action_picker") {
                            predictiveBackProgress
                        } else if (isActionPickerPredictiveDismissed) {
                            1f
                        } else 0f
                        ButtonActionsScreen(
                            title = title,
                            slots = slots,
                            keyMap = keyMap,
                            apps = apps,
                            ctx = ctx,
                            scope = scope,
                            predictiveBackProgress = progress,
                            onBack = { activeBlockDetail = null }
                        )
                    }
                }

                val currentRadialMenu = radialMenuState
                if (currentRadialMenu != null) {
                    if (currentRadialMenu.type == MenuType.RADIAL) {
                        RadialActionMenu(state = currentRadialMenu, onDismiss = { radialMenuState = null })
                    } else {
                        MacroCreationMenu(state = currentRadialMenu, onDismiss = { radialMenuState = null })
                    }
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
