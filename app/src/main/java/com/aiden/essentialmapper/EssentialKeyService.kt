package com.aiden.essentialmapper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.view.animation.DecelerateInterpolator
import com.aiden.essentialmapper.Config
import com.aiden.essentialmapper.PanelSide
import com.aiden.essentialmapper.CustomActionType
import com.aiden.essentialmapper.TapSlot
import kotlinx.coroutines.runBlocking

class EssentialKeyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private var isKillModeActive = false
    private var pendingBackCount = 0
    private var shouldShowPanelAfter = false
    private var isMacroLooping = false
    
    private val deactivateKillMode = Runnable { 
        if (isKillModeActive) {
            Log.i(TAG, "Kill mode timeout")
            finalizeAction()
        }
    }

    private val resolveTaps = Runnable {
        val count = tapCount
        tapCount = 0
        val keyMap = Config.readBlocking(this)
        val slot = when (count) {
            1 -> TapSlot.SINGLE
            2 -> TapSlot.DOUBLE
            3 -> TapSlot.TRIPLE
            else -> null
        }
        val target = slot?.let { keyMap.forSlot(it) }
        dispatch(target, slot)
    }

    private val resolveLongPress = Runnable {
        tapCount = 0
        Log.i(TAG, "long press detected")
        val target = Config.readBlocking(this@EssentialKeyService).long
        dispatch(target, TapSlot.LONG)
    }

    private val cameraManager by lazy { getSystemService(CameraManager::class.java) }
    private val torchCameraId: String? by lazy { findTorchCameraId() }
    @Volatile private var torchOn = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) torchOn = enabled
        }
    }

    private var customPanelView: View? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.scanCode != TARGET_SCAN_CODE) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) handler.postDelayed(resolveLongPress, LONG_PRESS_MS)
            KeyEvent.ACTION_UP -> if (handler.hasCallbacks(resolveLongPress)) {
                handler.removeCallbacks(resolveLongPress)
                tapCount++; handler.removeCallbacks(resolveTaps); handler.postDelayed(resolveTaps, TAP_DEBOUNCE_MS)
            }
        }
        return true
    }

    private fun dispatch(target: String?, slot: TapSlot?) {
        Log.i(TAG, "dispatch -> $target (slot=$slot)")
        
        if (isMacroLooping) {
            Log.i(TAG, "Stopping macro loop")
            isMacroLooping = false
            return
        }

        when (target) {
            null -> { /* по умолчанию */ }
            Config.ACTION_FLASHLIGHT -> toggleTorch()
            Config.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            Config.ACTION_KILL_ESSENTIAL -> {
                setupKill(count = 1, showPanel = false)
            }
            Config.ACTION_KILL_NOTES -> {
                val backs = if (slot == TapSlot.LONG) 2 else 1
                setupKill(count = backs, showPanel = false)
            }
            Config.ACTION_CUSTOM_PANEL -> {
                val backs = if (slot == TapSlot.LONG) 2 else 1
                setupKill(count = backs, showPanel = true, slot = slot)
            }
            else -> launchApp(target)
        }
    }

    private fun setupKill(count: Int, showPanel: Boolean, slot: TapSlot? = null) {
        handler.removeCallbacks(deactivateKillMode)
        isKillModeActive = true
        pendingBackCount = count
        shouldShowPanelAfter = showPanel
        val pendingSlot = slot ?: TapSlot.SINGLE
        
        // Таймаут безопасности (если окно так и не появилось)
        handler.postDelayed(deactivateKillMode, 3000)
        
        // Начинаем немедленную проверку
        checkAndExecute(pendingSlot)
    }

    private fun checkAndExecute(slot: TapSlot) {
        if (!isKillModeActive) return
        
        val activePkg = rootInActiveWindow?.packageName?.toString() ?: ""
        
        if (activePkg in TARGET_PACKAGES) {
            Log.i(TAG, "Target $activePkg detected. Overriding system UI...")
            
            // Выполняем нажатие НАЗАД один раз (или два для Long Press)
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (pendingBackCount == 2) {
                handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 150)
            }
            
            // Сразу переходим к завершению, не дожидаясь повторных проверок
            val showPanel = shouldShowPanelAfter
            isKillModeActive = false
            shouldShowPanelAfter = false
            handler.removeCallbacks(deactivateKillMode)
            
            if (showPanel) {
                // Даем системе время убрать анимацию закрытия Nothing UI
                handler.postDelayed({ showCustomPanel(slot) }, 400)
            }
        } else {
            // Окно еще не появилось, продолжаем мониторинг
            handler.postDelayed({ checkAndExecute(slot) }, 50)
        }
    }

    private fun finalizeAction() {
        isKillModeActive = false
        shouldShowPanelAfter = false
        handler.removeCallbacks(deactivateKillMode)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Мы используем циклическую проверку в checkAndExecute, 
        // поэтому здесь дополнительные действия не требуются.
    }

    override fun onInterrupt() {}

    private fun showCustomPanel(slot: TapSlot) {
        if (customPanelView != null) return
        val config = Config.readBlocking(this)
        val customConfig = config.customConfigs[slot] ?: CustomConfig()
        
        when (customConfig.type) {
            CustomActionType.FAVORITES -> {
                if (customConfig.favoriteApps.isNotEmpty()) showFavoritesRoller(config, customConfig)
                else showSimplePanel(config)
            }
            CustomActionType.RECORDING -> {
                if (customConfig.recordedGesture.isNotEmpty()) {
                    if (customConfig.macroMode == MacroMode.LOOP) {
                        isMacroLooping = true
                        playRecordedGesture(customConfig.recordedGesture, loop = true)
                    } else {
                        playRecordedGesture(customConfig.recordedGesture, loop = false)
                    }
                } else showSimplePanel(config)
            }
            else -> showSimplePanel(config)
        }
    }

    private fun playRecordedGesture(points: List<GesturePoint>, loop: Boolean) {
        if (points.size < 2 || (loop && !isMacroLooping)) return
        try {
            val builder = android.accessibilityservice.GestureDescription.Builder()
            val path = android.graphics.Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            val duration = (points.last().time - points.first().time).coerceIn(50L, 5000L)
            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration))
            
            dispatchGesture(builder.build(), object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    if (loop && isMacroLooping) {
                        // Повторяем через небольшую паузу
                        handler.postDelayed({ playRecordedGesture(points, true) }, 100)
                    }
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    isMacroLooping = false
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play macro", e)
            isMacroLooping = false
        }
    }

    private fun showSimplePanel(config: KeyMap) {
        val panel = object : View(this) {
            private val yellow = Color.parseColor("#FFD700"); private val dark = Color.parseColor("#1A1A1A")
            private var animOffset = 250f; private var isClosing = false
            init { ValueAnimator.ofFloat(250f, 0f).apply { duration = 300; interpolator = DecelerateInterpolator(); addUpdateListener { animOffset = it.animatedValue as Float; invalidate() }; start() } }
            fun performDismiss(onDone: () -> Unit) {
                if (isClosing) return
                isClosing = true
                ValueAnimator.ofFloat(animOffset, 250f).apply { duration = 300; addUpdateListener { animOffset = it.animatedValue as Float; invalidate() }; start(); handler.postDelayed({ onDone() }, 300) }
            }
            override fun onDraw(canvas: Canvas) {
                val isRight = config.panelSide == PanelSide.RIGHT; val rectWidth = 160f; val rectHeight = 300f; val margin = 60f
                val centerX = if (isRight) width.toFloat() - margin - rectWidth / 2f + animOffset else margin + rectWidth / 2f - animOffset
                val centerY = height.toFloat() * config.panelYOffset
                val rect = RectF(centerX - rectWidth / 2f, centerY - rectHeight / 2f, centerX + rectWidth / 2f, centerY + rectHeight / 2f)
                canvas.drawRoundRect(rect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; style = Paint.Style.FILL })
                canvas.drawRoundRect(rect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = yellow; style = Paint.Style.STROKE; strokeWidth = 12f })
                val lineX = if (isRight) width.toFloat() - 4f + animOffset else 4f - animOffset
                val p = Paint().apply { strokeWidth = 6f; shader = LinearGradient(lineX, centerY - 250f, lineX, centerY + 250f, intArrayOf(Color.TRANSPARENT, yellow, Color.TRANSPARENT), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP) }
                canvas.drawLine(lineX, centerY - 250f, lineX, centerY + 250f, p)
            }
        }
        fun close() { panel.performDismiss { try { windowManager.removeView(panel); customPanelView = null } catch (e: Exception) {} } }
        panel.setOnClickListener { close() }
        addOverlay(panel, true); handler.postDelayed({ if (customPanelView == panel) close() }, 5000)
    }

    private fun showFavoritesRoller(config: KeyMap, customConfig: CustomConfig) {
        val apps = customConfig.favoriteApps.mapNotNull { pkg -> try { pkg to packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null } }
        if (apps.isEmpty()) { showSimplePanel(config); return }
        
        val roller = object : View(this) {
            private val yellow = Color.parseColor("#FFD700")
            private val dark = Color.parseColor("#1A1A1A")
            private val itemSize = 180f
            private var scrollY = 0f
            private var animOffset = 300f
            private var isClosing = false
            private var startY = 0f
            private var isDragging = false
            private var snapAnimator: ValueAnimator? = null

            init {
                ValueAnimator.ofFloat(300f, 0f).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animOffset = it.animatedValue as Float; invalidate() }
                    start()
                }
            }

            override fun onDraw(canvas: Canvas) {
                val isRight = config.panelSide == PanelSide.RIGHT
                val margin = 60f
                val centerX = if (isRight) width.toFloat() - margin - itemSize / 2f + animOffset else margin + itemSize / 2f - animOffset
                val centerY = height.toFloat() * config.panelYOffset
                
                val panelHeight = itemSize * 5.5f
                val rect = RectF(centerX - itemSize / 2f, centerY - panelHeight / 2f, centerX + itemSize / 2f, centerY + panelHeight / 2f)
                
                canvas.drawRoundRect(rect, 45f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; style = Paint.Style.FILL })
                
                val selectionRect = RectF(centerX - itemSize / 2f + 8, centerY - itemSize / 2f + 8, centerX + itemSize / 2f - 8, centerY + itemSize / 2f - 8)
                canvas.drawRoundRect(selectionRect, 35f, 35f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#44FFD700"); style = Paint.Style.FILL })
                canvas.drawRoundRect(selectionRect, 35f, 35f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = yellow; style = Paint.Style.STROKE; strokeWidth = 4f })

                canvas.save()
                canvas.clipRect(rect)

                val centerIndexFraction = scrollY / itemSize
                val roundedIndex = Math.round(centerIndexFraction).toInt()

                for (i in (roundedIndex - 4)..(roundedIndex + 4)) {
                    val index = ((i % apps.size) + apps.size) % apps.size
                    val (_, icon) = apps[index]
                    val itemY = centerY + (i * itemSize) - scrollY
                    
                    val dist = Math.abs(itemY - centerY)
                    val scale = 1f - (dist / (panelHeight / 2f)).coerceIn(0f, 0.4f)
                    val alpha = (255 * (1f - dist / (panelHeight / 2f))).toInt().coerceIn(0, 255)
                    
                    val s = (itemSize / 2 * scale).toInt()
                    icon.setBounds((centerX - s + 25).toInt(), (itemY - s + 25).toInt(), (centerX + s - 25).toInt(), (itemY + s - 25).toInt())
                    icon.alpha = alpha
                    icon.draw(canvas)
                }
                canvas.restore()

                canvas.drawRoundRect(rect, 45f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = yellow; style = Paint.Style.STROKE; strokeWidth = 3f })
            }

            override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        isDragging = false
                        snapAnimator?.cancel()
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val delta = (startY - event.y) * 1.5f // Увеличенная чувствительность (множитель)
                        if (Math.abs(delta) > 5f || isDragging) { // Снижен порог срабатывания
                            scrollY += delta
                            startY = event.y
                            isDragging = true
                            invalidate()
                        }
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            snap()
                        } else {
                            val isRight = config.panelSide == PanelSide.RIGHT
                            val centerX = if (isRight) width.toFloat() - 60f - itemSize / 2f + animOffset else 60f + itemSize / 2f - animOffset
                            val centerY = height.toFloat() * config.panelYOffset
                            val hitRect = RectF(centerX - itemSize / 2f, centerY - itemSize / 2f, centerX + itemSize / 2f, centerY + itemSize / 2f)
                            
                            if (hitRect.contains(event.x, event.y)) {
                                val roundedIndex = Math.round(scrollY / itemSize).toInt()
                                val index = ((roundedIndex % apps.size) + apps.size) % apps.size
                                launchApp(apps[index].first)
                                dismiss()
                            } else {
                                dismiss()
                            }
                        }
                    }
                }
                return true
            }

            private fun snap() {
                val target = Math.round(scrollY / itemSize) * itemSize
                snapAnimator = ValueAnimator.ofFloat(scrollY, target).apply {
                    duration = 200
                    addUpdateListener { scrollY = it.animatedValue as Float; invalidate() }
                    start()
                }
            }

            fun dismiss() {
                if (isClosing) return
                isClosing = true
                snapAnimator?.cancel()
                val v = this
                ValueAnimator.ofFloat(animOffset, 300f).apply {
                    duration = 300
                    addUpdateListener { animOffset = it.animatedValue as Float; invalidate() }
                    start()
                    handler.postDelayed({ 
                        try { windowManager.removeView(v); customPanelView = null } catch (e: Exception) {} 
                    }, 300)
                }
            }
        }
        addOverlay(roller, false)
    }

    private fun addOverlay(view: View, notFocusable: Boolean) {
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or (if (notFocusable) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        windowManager.addView(view, WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT))
        customPanelView = view
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.aiden.essentialmapper.START_RECORDING") {
            val slotKey = intent.getStringExtra("slot")
            val slot = TapSlot.entries.find { it.key == slotKey } ?: TapSlot.SINGLE
            showRecordingOverlay(slot)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showRecordingOverlay(slot: TapSlot) {
        if (customPanelView != null) {
            try { windowManager.removeView(customPanelView); customPanelView = null } catch(e:Exception){}
        }
        val yellow = Color.parseColor("#FFD700")
        
        val recorder = object : android.widget.FrameLayout(this) {
            private val points = mutableListOf<GesturePoint>()
            private var isRecording = false
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
                color = yellow; style = Paint.Style.STROKE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND 
            }
            
            private val captureView = object : View(context) {
                override fun onDraw(canvas: Canvas) {
                    if (points.size > 1) {
                        val path = android.graphics.Path()
                        path.moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
                        canvas.drawPath(path, paint)
                    }
                }
                override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
                    if (!isRecording) return false
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            points.clear()
                            points.add(GesturePoint(event.x, event.y, System.currentTimeMillis()))
                        }
                        android.view.MotionEvent.ACTION_MOVE -> { 
                            points.add(GesturePoint(event.x, event.y, System.currentTimeMillis()))
                            invalidate() 
                        }
                    }
                    return true
                }
            }

            init {
                setBackgroundColor(Color.parseColor("#33000000"))
                addView(captureView, android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
                
                val menu = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    val shape = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#EE1A1A1A"))
                        cornerRadius = 40f
                        setStroke(2, yellow)
                    }
                    background = shape
                    setPadding(40, 20, 40, 20)
                    gravity = Gravity.CENTER
                }
                
                val btnStartStop = android.widget.Button(context).apply { 
                    text = "Старт"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE) 
                }
                val btnSave = android.widget.Button(context).apply { 
                    text = "Сохранить"; setBackgroundColor(Color.TRANSPARENT); setTextColor(yellow); visibility = View.GONE 
                }
                val btnClose = android.widget.Button(context).apply { 
                    text = "Закрыть"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.GRAY) 
                }
                
                btnStartStop.setOnClickListener {
                    isRecording = !isRecording
                    btnStartStop.text = if (isRecording) "Стоп" else "Старт"
                    btnStartStop.setTextColor(if (isRecording) Color.RED else Color.WHITE)
                    if (isRecording) {
                        points.clear()
                        captureView.invalidate()
                        btnSave.visibility = View.GONE
                    } else {
                        if (points.size > 2) btnSave.visibility = View.VISIBLE
                    }
                }
                
                btnSave.setOnClickListener {
                    if (points.size > 2) {
                        runBlocking { Config.setRecordedGesture(this@EssentialKeyService, slot, points.toList()) }
                    }
                    dismiss()
                }
                
                btnClose.setOnClickListener { dismiss() }
                
                menu.addView(btnStartStop)
                menu.addView(btnSave)
                menu.addView(btnClose)
                
                val menuParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, 
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = 150
                }
                addView(menu, menuParams)
            }
            
            fun dismiss() { try { windowManager.removeView(this); customPanelView = null } catch (e: Exception) {} }
        }
        
        addOverlay(recorder, false)
    }

    private fun launchApp(pkg: String) { val intent = packageManager.getLaunchIntentForPackage(pkg)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; if (intent != null) startActivity(intent) }
    private fun findTorchCameraId(): String? = try { cameraManager.cameraIdList.firstOrNull { id -> cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } } catch (e: Exception) { null }
    private fun toggleTorch() { val id = torchCameraId ?: return; try { val next = !torchOn; cameraManager.setTorchMode(id, next); torchOn = next } catch (e: Exception) {} }

    override fun onServiceConnected() { super.onServiceConnected(); runBlocking { Config.seedDefaultsIfEmpty(this@EssentialKeyService) }; cameraManager.registerTorchCallback(torchCallback, handler); Log.i(TAG, "EssentialKeyService connected") }
    override fun onUnbind(intent: Intent?): Boolean { cameraManager.unregisterTorchCallback(torchCallback); return super.onUnbind(intent) }

    companion object {
        const val TAG = "EMAP"; const val TARGET_SCAN_CODE = 250; const val TAP_DEBOUNCE_MS = 400L; const val LONG_PRESS_MS = 500L
        private val TARGET_PACKAGES = setOf("com.nothing.ntessentialrecorder", "com.nothing.ntessentialspace", "com.nothing.essentialintelligence.extradark.overlay", "com.nothing.ntessentialrecorder.extradark.overlay", "com.nothing.ntessentialspace.extradark.overlay", "com.nothing.ntessentialspace.overlay.all_devices", "com.nothing.ntessentialspace.overlay.asteroids")
    }
}
