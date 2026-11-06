package com.HojuNK.essent_all

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.*
import android.graphics.*
import com.nothing.ketchum.GlyphManager
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import android.view.accessibility.AccessibilityEvent
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager

import android.view.animation.DecelerateInterpolator
import android.app.NotificationManager
import android.app.KeyguardManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.HojuNK.essent_all.ui.components.ClipboardOverlayContent
import com.HojuNK.essent_all.util.EventIconDisplay
import androidx.lifecycle.Lifecycle

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.*

class EssentialKeyService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, SensorEventListener {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var lastLightValue = -1f
    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var isSensorsRegistered = false
    private var currentKeyMap: KeyMap? = null
    private var isAccessibilityConnected = false
    private var pendingShortcutEvent: EventRule? = null
    private var pendingActionAfterKill: String? = null

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private var isKillModeActive = false
    private var pendingBackCount = 0
    private var shouldShowPanelAfter = false
    private var isMacroActive = false
    private var macroJob: Job? = null
    private var macroIndicatorView: View? = null
    private var activeShortcutEvent: EventRule? = null
    private var triggeredBySlot: TapSlot? = null
    private var panelClosedByKey = false

    private var freezeOverlayView: View? = null
    private var isFreezeOverrideActive = false
    
    private var currentPackage: String = ""
    private var previousPackage: String = ""
    
    private var customPanelView: View? = null
    private var dismissActivePanel: (() -> Unit)? = null
    private var activePanelSlot: TapSlot? = null
    
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val states = mutableMapOf<ControlType, Boolean>()

    private var isClipboardOverlayOpen = false
    private var wasClipboardOverlayOpenBeforeLock = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (isClipboardOverlayOpen) {
                        wasClipboardOverlayOpenBeforeLock = true
                        dismissActivePanel?.invoke()
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (wasClipboardOverlayOpenBeforeLock) {
                        wasClipboardOverlayOpenBeforeLock = false
                        handler.postDelayed({
                            showClipboardOverlay()
                        }, 250)
                    }
                }
            }
            updateStates()
            customPanelView?.invalidate()
        }
    }

    private fun updateStates() {
        try {
            states[ControlType.FLASHLIGHT] = torchOn
            states[ControlType.ROTATION] = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            states[ControlType.VOLUME] = am.ringerMode != AudioManager.RINGER_MODE_SILENT
        } catch (e: Exception) {}
    }

    private val deactivateKillMode = Runnable { if (isKillModeActive) finalizeAction(TapSlot.SINGLE) }

    private val resolveTaps = Runnable {
        val count = tapCount; tapCount = 0
        serviceScope.launch {
            val keyMap = Config.readBlocking(this@EssentialKeyService)
            val slot = when (count) { 1 -> TapSlot.SINGLE; 2 -> TapSlot.DOUBLE; 3 -> TapSlot.TRIPLE; else -> null }
            dispatch(slot?.let { keyMap.forSlot(it) }, slot)
        }
    }

    private val resolveLongPress = Runnable { 
        tapCount = 0
        serviceScope.launch {
            val keyMap = Config.readBlocking(this@EssentialKeyService)
            dispatch(keyMap.long, TapSlot.LONG)
        }
    }

    private val cameraManager by lazy { getSystemService(CameraManager::class.java) }
    private val torchCameraId: String? by lazy { findTorchCameraId() }
    @Volatile private var torchOn = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) { torchOn = enabled; updateStates(); customPanelView?.invalidate() }
        }
    }

    private var volUpTapCount = 0
    private var volDownTapCount = 0
    private var isVolUpHolding = false
    private var isVolDownHolding = false
    private var volUpHoldJob: Job? = null
    private var volDownHoldJob: Job? = null

    private val resolveVolUpTaps = Runnable {
        val count = volUpTapCount; volUpTapCount = 0
        serviceScope.launch {
            val keyMap = currentKeyMap ?: Config.readBlocking(this@EssentialKeyService)
            val slot = when (count) { 1 -> TapSlot.VOL_UP_SINGLE; 2 -> TapSlot.VOL_UP_DOUBLE; 3 -> TapSlot.VOL_UP_TRIPLE; else -> null }
            val target = slot?.let { keyMap.forSlot(it) }

            if (target != null) {
                dispatch(target, slot)
            } else {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                repeat(count) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
            }
        }
    }

    private val resolveVolUpLongPress = Runnable {
        volUpTapCount = 0
        handler.removeCallbacks(resolveVolUpTaps)
        isVolUpHolding = true
        val keyMap = currentKeyMap ?: Config.readBlocking(this@EssentialKeyService)
        val target = keyMap.volUpLong

        if (target != null) {
            dispatch(target, TapSlot.VOL_UP_LONG)
        } else {
            volUpHoldJob?.cancel()
            volUpHoldJob = serviceScope.launch {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                while (isActive && isVolUpHolding) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    delay(150)
                }
            }
        }
    }

    private val resolveVolDownTaps = Runnable {
        val count = volDownTapCount; volDownTapCount = 0
        serviceScope.launch {
            val keyMap = currentKeyMap ?: Config.readBlocking(this@EssentialKeyService)
            val slot = when (count) { 1 -> TapSlot.VOL_DOWN_SINGLE; 2 -> TapSlot.VOL_DOWN_DOUBLE; 3 -> TapSlot.VOL_DOWN_TRIPLE; else -> null }
            val target = slot?.let { keyMap.forSlot(it) }

            if (target != null) {
                dispatch(target, slot)
            } else {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                repeat(count) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                }
            }
        }
    }

    private val resolveVolDownLongPress = Runnable {
        volDownTapCount = 0
        handler.removeCallbacks(resolveVolDownTaps)
        isVolDownHolding = true
        val keyMap = currentKeyMap ?: Config.readBlocking(this@EssentialKeyService)
        val target = keyMap.volDownLong

        if (target != null) {
            dispatch(target, TapSlot.VOL_DOWN_LONG)
        } else {
            volDownHoldJob?.cancel()
            volDownHoldJob = serviceScope.launch {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                while (isActive && isVolDownHolding) {
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    delay(150)
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        try {
            val config = currentKeyMap ?: Config.readBlocking(this)

            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                val hasVolUp = config.hasAnyActionOrEvent(
                    TapSlot.VOL_UP_SINGLE, TapSlot.VOL_UP_LONG, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE
                )
                if (!hasVolUp) return false // Smart Pass-through: unmapped keys work natively with 0 lag

                val hasMultiTap = config.forSlot(TapSlot.VOL_UP_DOUBLE) != null ||
                                  config.forSlot(TapSlot.VOL_UP_TRIPLE) != null ||
                                  config.events.any { it.isEnabled && it.triggerGesture in listOf(TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE) }

                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            handler.postDelayed(resolveVolUpLongPress, LONG_PRESS_MS)
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        if (isVolUpHolding) {
                            isVolUpHolding = false
                            volUpHoldJob?.cancel()
                        } else if (handler.hasCallbacks(resolveVolUpLongPress)) {
                            handler.removeCallbacks(resolveVolUpLongPress)
                            volUpTapCount++
                            handler.removeCallbacks(resolveVolUpTaps)
                            if (!hasMultiTap) {
                                resolveVolUpTaps.run()
                            } else {
                                handler.postDelayed(resolveVolUpTaps, TAP_DEBOUNCE_MS)
                            }
                        }
                    }
                }
                return true
            }

            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                val hasVolDown = config.hasAnyActionOrEvent(
                    TapSlot.VOL_DOWN_SINGLE, TapSlot.VOL_DOWN_LONG, TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE
                )
                if (!hasVolDown) return false // Smart Pass-through

                val hasMultiTap = config.forSlot(TapSlot.VOL_DOWN_DOUBLE) != null ||
                                  config.forSlot(TapSlot.VOL_DOWN_TRIPLE) != null ||
                                  config.events.any { it.isEnabled && it.triggerGesture in listOf(TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE) }

                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            handler.postDelayed(resolveVolDownLongPress, LONG_PRESS_MS)
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        if (isVolDownHolding) {
                            isVolDownHolding = false
                            volDownHoldJob?.cancel()
                        } else if (handler.hasCallbacks(resolveVolDownLongPress)) {
                            handler.removeCallbacks(resolveVolDownLongPress)
                            volDownTapCount++
                            handler.removeCallbacks(resolveVolDownTaps)
                            if (!hasMultiTap) {
                                resolveVolDownTaps.run()
                            } else {
                                handler.postDelayed(resolveVolDownTaps, TAP_DEBOUNCE_MS)
                            }
                        }
                    }
                }
                return true
            }

            if (event.scanCode != TARGET_SCAN_CODE) {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                    val currentPanel = customPanelView
                    if (currentPanel != null) {
                        // Shortcut check
                        val shortcut = activeShortcutEvent ?: pendingShortcutEvent
                        if (shortcut != null) {
                            if (!shortcut.keepVisibleOnBack) { 
                                closeOverlay(currentPanel)
                                pendingShortcutEvent = null
                                activeShortcutEvent = null
                                return true 
                            }
                        } else {
                            // Favorites check
                            activePanelSlot?.let { s ->
                                val custom = config.customConfigs[s]
                                if (custom != null && !custom.keepVisibleOnBack) { dismissActivePanel?.invoke(); return true }
                            }
                        }
                    }
                }
                return false
            }
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        // PRE-EMPTIVE FREEZE: Capture screen BEFORE system can react
                        if (!config.disableAnimations) {
                            showFreezeOverlay(config, isOverride = true)
                        }

                        // Toggle logic: If a panel is showing, check if we should close it
                        val currentPanel = customPanelView
                        if (currentPanel != null) {
                            // Check Shortcut behavior
                            val shortcut = activeShortcutEvent ?: pendingShortcutEvent
                            var shouldClose = false
                            if (shortcut != null) {
                                if (!shortcut.keepVisibleOnKey) shouldClose = true
                            } else {
                                // Check Favorites/Controls behavior
                                activePanelSlot?.let { slot ->
                                    val custom = config.customConfigs[slot]
                                    if (custom != null && !custom.keepVisibleOnKey) shouldClose = true
                                }
                            }

                            if (shouldClose) {
                                Log.d(TAG, "Closing panel via Essential Key press")
                                panelClosedByKey = true
                                dismissActivePanel?.invoke()
                                // We DO NOT return true here anymore, so tap sequence continues 
                                // and dispatch() will perform the final Kill action
                            }
                        }
                        
                        updateSensorRegistration()
                        handler.postDelayed(resolveLongPress, LONG_PRESS_MS)
                    }
                }
                KeyEvent.ACTION_UP -> if (handler.hasCallbacks(resolveLongPress)) {
                    handler.removeCallbacks(resolveLongPress); tapCount++; handler.removeCallbacks(resolveTaps); handler.postDelayed(resolveTaps, TAP_DEBOUNCE_MS)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onKeyEvent: ${e.message}")
            return false
        }
    }

    private fun updateSensorRegistration() {
        val config = currentKeyMap ?: return
        val needsSensors = config.events.any { it.isEnabled && (it.conditions.any { c -> c.type == ConditionType.LIGHT || c.type == ConditionType.ROTATION } || it.exclusions.any { c -> c.type == ConditionType.LIGHT || c.type == ConditionType.ROTATION }) }
        
        if (needsSensors && !isSensorsRegistered) {
            isSensorsRegistered = true
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            Log.d(TAG, "Sensors started (persistent mode)")
        } else if (!needsSensors && isSensorsRegistered) {
            sensorManager.unregisterListener(this)
            isSensorsRegistered = false
            Log.d(TAG, "Sensors stopped (not needed)")
        }
    }

    private fun stopSensors() {
        if (isSensorsRegistered) {
            sensorManager.unregisterListener(this)
            isSensorsRegistered = false
            Log.d(TAG, "Sensors stopped")
        }
    }

    private fun dispatch(target: String?, slot: TapSlot?) {
        try {
            if (slot == null) return

            val keyMap = currentKeyMap ?: Config.readBlocking(this)
            
            // 1. Unified Analysis: Pre-analyze if an action is about to happen
            val activePkg = rootInActiveWindow?.packageName?.toString() ?: ""
            val isSpace = activePkg == "com.nothing.launcher" || activePkg.contains("essential")
            val pkgToMatch = if (slot == TapSlot.DOUBLE && isSpace) previousPackage else activePkg
            val now = Calendar.getInstance()
            val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

            val matchedRules = keyMap.events.filter { it.isEnabled && it.triggerGesture == slot }.filter { rule ->
                val condResults = rule.conditions.map { checkCondition(it, pkgToMatch, currentTimeStr) }
                val condMatch = if (rule.conditionLogicAll) condResults.all { it } else condResults.any { it }
                if (!condMatch) return@filter false
                
                val exclResults = rule.exclusions.map { checkCondition(it, pkgToMatch, currentTimeStr) }
                val exclusionMet = if (rule.exclusionLogicAll) exclResults.all { it } else exclResults.any { it }
                !exclusionMet
            }

            val defaultAction = keyMap.forSlot(slot)
            val hasUserActionOrEvent = (defaultAction != null) || matchedRules.isNotEmpty() || (target != null)
            val isOverrideAction = isMacroActive || (activePanelSlot != null) || hasUserActionOrEvent
            
            // Show magic freeze overlay if animations are not disabled
            if (!keyMap.disableAnimations) {
                showFreezeOverlay(keyMap, isOverride = isOverrideAction)
            }

            // 2. Execution Logic
            if (isMacroActive && !hasUserActionOrEvent) {
                Log.d(TAG, "Stopping active macro")
                macroJob?.cancel()
                isMacroActive = false
                hideMacroIndicator()
                
                val killAction = if (slot == TapSlot.DOUBLE || slot == TapSlot.TRIPLE) Config.ACTION_KILL_ESSENTIAL else Config.ACTION_KILL_NOTES
                executeActionDirectly(killAction, slot)
                return
            }

            if ((activePanelSlot != null || panelClosedByKey) && !hasUserActionOrEvent) {
                val custom = activePanelSlot?.let { keyMap.customConfigs[it] }
                val shortcut = activeShortcutEvent ?: pendingShortcutEvent
                
                var shouldClose = panelClosedByKey
                if (!shouldClose) {
                    if (slot == triggeredBySlot || activePanelSlot != TapSlot.SHORTCUT) {
                        if (shortcut != null) { if (!shortcut.keepVisibleOnKey) shouldClose = true }
                        else if (custom != null) { if (!custom.keepVisibleOnKey) shouldClose = true }
                        else shouldClose = true
                    }
                }

                if (shouldClose) {
                    Log.d(TAG, "Closing active panel: $slot (closedByKey=$panelClosedByKey)")
                    if (!panelClosedByKey) dismissActivePanel?.invoke()
                    panelClosedByKey = false
                    
                    val killAction = if (slot == TapSlot.DOUBLE || slot == TapSlot.TRIPLE) Config.ACTION_KILL_ESSENTIAL else Config.ACTION_KILL_NOTES
                    executeActionDirectly(killAction, slot)
                    return
                }
                panelClosedByKey = false
            }

            if (matchedRules.isNotEmpty()) {
                stopSensors()
                
                val shortcutRules = matchedRules.filter { it.useShortcut }
                val directRules = matchedRules.filter { !it.useShortcut }
                
                if (shortcutRules.isNotEmpty()) {
                    showShortcutPanel(shortcutRules.first(), slot)
                    return
                }
                
                if (directRules.isNotEmpty()) {
                    executeEventActions(directRules.first())
                    return
                }
            }

            if (defaultAction != null) {
                Log.d(TAG, "No matching event, dispatching direct action: $defaultAction")
                executeAction(defaultAction, slot)
            } else if (target != null) {
                executeAction(target, slot)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dispatch: ${e.message}", e)
        }
    }

    private fun checkCondition(condition: EventCondition, activePkg: String, currentTimeStr: String): Boolean {
        return when (condition.type) {
            ConditionType.ALWAYS -> true
            ConditionType.APP -> {
                val pkgs = condition.value.split(",").filter { it.isNotEmpty() }
                activePkg in pkgs
            }
            ConditionType.TIME -> {
                val times = condition.value.split("-")
                if (times.size == 2) isTimeInRange(currentTimeStr, times[0], times[1]) else false
            }
            ConditionType.ROTATION -> {
                val rot = getCurrentRotation()
                Log.d(TAG, "Condition Rotation: current=$rot, target=${condition.value}")
                condition.value == rot
            }
            ConditionType.LIGHT -> {
                val luxPct = getCurrentLightPercent()
                checkRange(condition.value, luxPct)
            }
            ConditionType.BRIGHTNESS -> {
                val brPct = getCurrentBrightnessPercent()
                checkRange(condition.value, brPct)
            }
            ConditionType.BATTERY -> {
                val batPct = getCurrentBatteryPercent()
                checkRange(condition.value, batPct)
            }
            ConditionType.WIFI -> {
                val enabled = getWifiEnabled()
                (condition.value == "ON") == enabled
            }
            ConditionType.BLUETOOTH -> {
                val enabled = getBluetoothEnabled()
                (condition.value == "ON") == enabled
            }
            ConditionType.LOCATION -> {
                val enabled = getLocationEnabled()
                (condition.value == "ON") == enabled
            }
            ConditionType.MOBILE_DATA -> {
                val enabled = getMobileDataEnabled()
                (condition.value == "ON") == enabled
            }
            ConditionType.CHARGING -> (condition.value == "ON") == getIsCharging()
            ConditionType.SCREEN_LOCK -> (condition.value == "ON") == getIsScreenLocked()
            ConditionType.AIRPLANE_MODE -> (condition.value == "ON") == getIsAirplaneModeOn()
            ConditionType.NFC -> (condition.value == "ON") == getIsNfcEnabled()
            ConditionType.AUTO_ROTATE -> (condition.value == "ON") == getIsAutoRotateEnabled()
            ConditionType.HEADPHONES -> (condition.value == "ON") == getIsHeadphonesConnected()
            ConditionType.DND -> (condition.value == "ON") == getIsDndEnabled()
            ConditionType.MOTION -> (condition.value == "ON") == getIsMoving()
            ConditionType.DARK_MODE -> (condition.value == "ON") == getIsDarkModeEnabled()
            ConditionType.SILENT_MODE -> (condition.value == "ON") == getIsSilentModeEnabled()
            ConditionType.VIBRATE_MODE -> (condition.value == "ON") == getIsVibrateModeEnabled()
            ConditionType.HOTSPOT -> (condition.value == "ON") == getIsHotspotEnabled()
        }
    }

    private fun checkRange(rangeStr: String, current: Int): Boolean {
        if (!rangeStr.contains("-")) return false
        val pts = rangeStr.split("-")
        if (pts.size != 2) return false
        val min = pts[0].toIntOrNull() ?: 0
        val max = pts[1].toIntOrNull() ?: 100
        return current in min..max
    }

    private fun getCurrentBrightnessPercent(): Int {
        return try {
            val br = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (br * 100 / 255)
        } catch (e: Exception) { 50 }
    }

    private fun getCurrentBatteryPercent(): Int {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 50
    }

    private fun getIsCharging(): Boolean {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getIsScreenLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isKeyguardLocked
    }

    private fun getIsAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    private fun getIsNfcEnabled(): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        return adapter?.isEnabled == true
    }

    private fun getIsAutoRotateEnabled(): Boolean {
        return Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
    }

    private fun getIsHeadphonesConnected(): Boolean {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        return am.isWiredHeadsetOn || am.isBluetoothA2dpOn
    }

    private fun getIsDndEnabled(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun getIsMoving(): Boolean {
        // Fallback: Use accelerometer variance if permission not granted or API not available
        // For now, just return false as a placeholder if we don't want to add full API logic here
        // But we can check if accelerometer shows significant movement
        val magnitude = Math.sqrt((lastAccelX * lastAccelX + lastAccelY * lastAccelY + lastAccelZ * lastAccelZ).toDouble())
        return Math.abs(magnitude - 9.8) > 0.5
    }

    private fun getIsDarkModeEnabled(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getIsSilentModeEnabled(): Boolean {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        return am.ringerMode == AudioManager.RINGER_MODE_SILENT
    }

    private fun getIsVibrateModeEnabled(): Boolean {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        return am.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    private fun getIsHotspotEnabled(): Boolean {
        val wm = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val method = wm.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wm) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    private fun getWifiEnabled(): Boolean {
        val wm = getSystemService(WIFI_SERVICE) as? android.net.wifi.WifiManager
        return wm?.isWifiEnabled == true
    }

    private fun getBluetoothEnabled(): Boolean {
        val ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return ba?.isEnabled == true
    }

    private fun getLocationEnabled(): Boolean {
        val lm = getSystemService(LOCATION_SERVICE) as? android.location.LocationManager
        return lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
    }

    private fun getMobileDataEnabled(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return try {
            val method = cm.javaClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            method.invoke(cm) as Boolean
        } catch (e: Exception) {
            getMobileDataEnabledFallback()
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun getMobileDataEnabledFallback(): Boolean {
        // Standard Settings.Global check
        val standard = Settings.Global.getInt(contentResolver, "mobile_data", -1)
        if (standard != -1) return standard == 1

        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                // Subscription specific check if possible
                val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                return if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    try {
                        val subTm = tm.createForSubscriptionId(subId)
                        subTm.isDataEnabled
                    } catch (e: Exception) {
                        tm.isDataEnabled
                    }
                } else {
                    tm.isDataEnabled
                }
            }
        } catch (e: Exception) {}
        
        // Comprehensive fallback for various Android versions and Nothing OS specific keys
        val keys = listOf("mobile_data0", "mobile_data1", "mobile_data_enabled")
        for (key in keys) {
            try {
                val v = Settings.Global.getInt(contentResolver, key, -1)
                if (v != -1) return v == 1
                val s = Settings.Secure.getInt(contentResolver, key, -1)
                if (s != -1) return s == 1
            } catch (e: Exception) {}
        }
        
        return false
    }

    private fun getCurrentRotation(): String {
        val x = lastAccelX
        val y = lastAccelY
        val z = lastAccelZ
        val absX = Math.abs(x)
        val absY = Math.abs(y)
        val absZ = Math.abs(z)

        return when {
            // Z is dominant (lying flat)
            absZ > absX && absZ > absY -> if (z > 0) "FACE_UP" else "FACE_DOWN"
            // X is dominant (landscape)
            absX > absY && absX > absZ -> if (x > 0) "LANDSCAPE_LEFT" else "LANDSCAPE_RIGHT"
            // Default or Y is dominant
            else -> "PORTRAIT"
        }
    }

    private fun getCurrentLightPercent(): Int {
        // Logarithmic scale for human perception (0 to ~2000 lux mapped to 0-100%)
        // Indoor is usually 100-500, bright office 500-1000, outdoor 1000+
        if (lastLightValue <= 0) return 0
        val lux = lastLightValue.coerceAtMost(2000f)
        return (Math.log10(lux.toDouble() + 1.0) / Math.log10(2001.0) * 100.0).toInt().coerceIn(0, 100)
    }

    private fun isTimeInRange(current: String, start: String, end: String): Boolean {
        return try {
            val cur = current.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val s = start.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val e = end.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            if (s <= e) cur in s..e else cur >= s || cur <= e
        } catch (e: Exception) { false }
    }

    private fun executeAction(target: String?, slot: TapSlot) {
        if (target == null) return
        Log.d(TAG, "Executing action: $target")
        
        // List of actions that must cancel Nothing's default behavior first
        val isApp = !target.startsWith("action:")
        val needsKill = isApp || 
                         target == Config.ACTION_FLASHLIGHT || 
                         target.startsWith(Config.ACTION_FLASHLIGHT_PREFIX) ||
                         target == Config.ACTION_BACK || 
                         target == Config.ACTION_HOME || 
                         target == Config.ACTION_NOTIFICATIONS ||
                         target == Config.ACTION_RECENTS ||
                         target == Config.ACTION_QUICK_SETTINGS ||
                         target == Config.ACTION_POWER_MENU ||
                         target == Config.ACTION_ROTATION_TOGGLE ||
                         target == Config.ACTION_RING_VIBRATE ||
                         target == Config.ACTION_WIFI_TOGGLE ||
                         target == Config.ACTION_BLUETOOTH_TOGGLE ||
                         target == Config.ACTION_CALCULATOR ||
                         target == Config.ACTION_OPEN_CAMERA ||
                         target == Config.ACTION_CAMERA_SHUTTER ||
                         target == Config.ACTION_EXTRA_DIM ||
                         target.startsWith(Config.ACTION_BRIGHTNESS_PREFIX) ||
                         target == Config.ACTION_CONTROLS ||
                         target == Config.ACTION_FAVORITES_PREFIX ||
                         target == Config.ACTION_GLYPH ||
                         target == Config.ACTION_GLYPH_TOGGLE ||
                         target.startsWith(Config.ACTION_GLYPH_PREFIX) ||
                         target.startsWith(Config.ACTION_GLYPH_PATTERN_PREFIX) ||
                         target.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX) ||
                         target.startsWith(Config.ACTION_GLYPH_WAVE_PREFIX) ||
                         target.startsWith(Config.ACTION_GLYPH_DRAW_PREFIX) ||
                         target.startsWith(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX) ||
                         target.startsWith(Config.ACTION_MACRO_PREFIX) ||
                         target == Config.ACTION_SCREENSHOT ||
                         target == Config.ACTION_LOCK_SCREEN ||
                         target == Config.ACTION_SPLIT_SCREEN ||
                         target == Config.ACTION_DND_TOGGLE ||
                         target == Config.ACTION_CLEAR_NOTIFICATIONS ||
                         target.startsWith(Config.ACTION_VOLUME_PREFIX) ||
                         target.startsWith(Config.ACTION_MEDIA_PREFIX) ||
                         target.startsWith(Config.ACTION_URL_PREFIX) ||
                         target.startsWith(Config.ACTION_THEME_PREFIX)

        val isEssentialKey = slot == TapSlot.SINGLE || slot == TapSlot.LONG || slot == TapSlot.DOUBLE || slot == TapSlot.TRIPLE
        if (needsKill && isEssentialKey && !isKillModeActive && !isMacroActive) {
            val killCount = if (slot == TapSlot.SINGLE || slot == TapSlot.LONG) {
                if (slot == TapSlot.LONG) 2 else 1
            } else 1
            
            setupKill(killCount, false, slot, pendingAction = target)
            return
        }

        executeActionDirectly(target, slot)
    }

    private fun executeActionDirectly(target: String, slot: TapSlot) {
        // If we are executing an action that DOESN'T involve setupKill, 
        // trigger removal now since the "system action cancellation" point is reached.
        if (target != Config.ACTION_KILL_ESSENTIAL && target != Config.ACTION_KILL_NOTES && 
            target != Config.ACTION_CUSTOM_PANEL && target != Config.ACTION_CONTROLS) {
            triggerAdaptiveFreezeRemoval()
        }

        when {
            target.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                performTouchAction(target.removePrefix(Config.ACTION_TOUCH_PREFIX))
                return
            }
            target.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                performSwipeAction(target.removePrefix(Config.ACTION_SWIPE_PREFIX))
                return
            }
            target.startsWith(Config.ACTION_PATH_PREFIX) -> {
                val data = target.removePrefix(Config.ACTION_PATH_PREFIX)
                serviceScope.launch { performPathActionSync(data) }
                return
            }
            target == Config.ACTION_FAVORITES_PREFIX -> {
                showFavoritesRoller(currentKeyMap ?: Config.readBlocking(this), (currentKeyMap?.customConfigs?.get(slot) ?: CustomConfig()), slot)
                return
            }
            target == Config.ACTION_FLASHLIGHT || target.startsWith(Config.ACTION_FLASHLIGHT_PREFIX) -> {
                handleFlashlightAction(target)
                return
            }
            target == Config.ACTION_GLYPH || target == Config.ACTION_GLYPH_TOGGLE || target.startsWith(Config.ACTION_GLYPH_PREFIX) -> {
                handleGlyphAction(target)
                return
            }
            target.startsWith(Config.ACTION_GLYPH_PATTERN_PREFIX) -> {
                val patternId = target.removePrefix(Config.ACTION_GLYPH_PATTERN_PREFIX)
                val config = currentKeyMap ?: Config.readBlocking(this)
                val pattern = config.glyphPatterns.find { it.id == patternId }
                if (pattern != null) {
                    com.HojuNK.essent_all.util.GlyphController.playGlyphPattern(pattern)
                }
                return
            }
            target.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX) ||
            target.startsWith(Config.ACTION_GLYPH_WAVE_PREFIX) ||
            target.startsWith(Config.ACTION_GLYPH_DRAW_PREFIX) ||
            target.startsWith(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX) -> {
                com.HojuNK.essent_all.util.GlyphController.playGlyphPattern(Macro(id = "temp", icon = "", title = "Temp", actions = listOf(target)))
                return
            }
            target.startsWith(Config.ACTION_MACRO_PREFIX) -> {
                val id = target.removePrefix(Config.ACTION_MACRO_PREFIX)
                val config = currentKeyMap ?: Config.readBlocking(this)
                val macro = config.macros.find { it.id == id }
                val custom = config.customConfigs[slot]
                
                if (macro != null) {
                    isMacroActive = true
                    showMacroIndicator(config)
                    macroJob = serviceScope.launch {
                        // Fixed: Force SINGLE mode if launched from a SHORTCUT slot
                        val isLoopRequested = custom?.macroMode == MacroMode.LOOP && slot != TapSlot.SHORTCUT
                        if (isLoopRequested) {
                            while (isActive && isMacroActive) {
                                executeMacroActionsSync(macro)
                                delay(300)
                            }
                        } else {
                            executeMacroActionsSync(macro)
                            isMacroActive = false
                            hideMacroIndicator()
                        }
                    }
                }
                return
            }
        }
        
        when (target) {
            Config.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            Config.ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            Config.ACTION_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            Config.ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            Config.ACTION_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            Config.ACTION_POWER_MENU -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            Config.ACTION_SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            Config.ACTION_LOCK_SCREEN -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            Config.ACTION_SPLIT_SCREEN -> {
                val success = performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                if (!success) {
                    // Fallback for some devices: open recents then split
                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                    handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }, 350)
                }
            }
            Config.ACTION_DND_TOGGLE -> toggleDnd()
            Config.ACTION_ROTATION_TOGGLE -> {
                try {
                    val current = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                    val next = if (current == 1) 0 else 1
                    if (Settings.System.canWrite(this)) {
                        Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, next)
                    } else {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                } catch (e: Exception) {}
            }
            Config.ACTION_RING_VIBRATE -> {
                try {
                    val am = getSystemService(AUDIO_SERVICE) as AudioManager
                    val mode = am.ringerMode
                    val next = when (mode) {
                        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                        AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                    am.ringerMode = next
                } catch (e: Exception) {}
            }
            Config.ACTION_WIFI_TOGGLE -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        val wm = getSystemService(WIFI_SERVICE) as WifiManager
                        @Suppress("DEPRECATION")
                        wm.isWifiEnabled = !wm.isWifiEnabled
                    }
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
            Config.ACTION_BLUETOOTH_TOGGLE -> {
                try {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {}
            }
            Config.ACTION_CALCULATOR -> {
                try {
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = packageManager.getLaunchIntentForPackage("com.google.android.calculator") ?: packageManager.getLaunchIntentForPackage("com.android.calculator2")
                        if (intent != null) startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (ex: Exception) {}
                }
            }
            Config.ACTION_OPEN_CAMERA -> {
                try {
                    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {}
            }
            Config.ACTION_CAMERA_SHUTTER -> {
                try {
                    val am = getSystemService(AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAMERA))
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CAMERA))
                } catch (e: Exception) {}
            }
            Config.ACTION_EXTRA_DIM -> {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {}
            }
            Config.ACTION_CLIPBOARD -> showClipboardOverlay()
            Config.ACTION_CLEAR_NOTIFICATIONS -> clearAllNotifications()
            Config.ACTION_KILL_ESSENTIAL -> setupKill(1, false, slot)
            Config.ACTION_KILL_NOTES -> setupKill(slot.let { if (it == TapSlot.LONG) 2 else 1 }, false, slot)
            Config.ACTION_CUSTOM_PANEL, Config.ACTION_CONTROLS -> setupKill(slot.let { if (it == TapSlot.LONG) 2 else 1 }, true, slot)
            else -> {
                when {
                    target.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX) -> handleAutoBrightnessAction(target.removePrefix(Config.ACTION_AUTO_BRIGHTNESS_PREFIX))
                    target.startsWith(Config.ACTION_VOLUME_PREFIX) -> handleVolumeAction(target.removePrefix(Config.ACTION_VOLUME_PREFIX))
                    target.startsWith(Config.ACTION_MEDIA_PREFIX) -> handleMediaAction(target.removePrefix(Config.ACTION_MEDIA_PREFIX))
                    target.startsWith(Config.ACTION_URL_PREFIX) -> launchUrl(target.removePrefix(Config.ACTION_URL_PREFIX))
                    target.startsWith(Config.ACTION_THEME_PREFIX) -> handleThemeAction(target.removePrefix(Config.ACTION_THEME_PREFIX))
                    target.startsWith(Config.ACTION_BRIGHTNESS_PREFIX) -> {
                        val percent = target.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX).toIntOrNull() ?: 50
                        setSystemBrightness(percent)
                    }
                    else -> launchApp(target)
                }
            }
        }
    }

    private fun toggleDnd() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }
        val current = nm.currentInterruptionFilter
        val next = if (current == NotificationManager.INTERRUPTION_FILTER_NONE) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_NONE
        nm.setInterruptionFilter(next)
    }

    private fun handleAutoBrightnessAction(action: String) {
        try {
            val currentMode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            val nextMode = when (action) {
                "on" -> Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                "off" -> Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                "toggle" -> if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                } else {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                }
                else -> return
            }
            
            if (!Settings.System.canWrite(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            }
            
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, nextMode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set auto brightness", e)
        }
    }

    private fun handleVolumeAction(action: String) {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val stream = when {
            action.contains("media") -> AudioManager.STREAM_MUSIC
            action.contains("ring") -> AudioManager.STREAM_RING
            action.contains("notif") -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }
        
        if (action.contains("set:")) {
            val percent = action.substringAfter("set:").toIntOrNull() ?: 50
            val max = am.getStreamMaxVolume(stream)
            val value = (max * (percent / 100f)).toInt()
            am.setStreamVolume(stream, value, AudioManager.FLAG_SHOW_UI)
            return
        }

        val direction = when {
            action.contains("up") -> AudioManager.ADJUST_RAISE
            action.contains("down") -> AudioManager.ADJUST_LOWER
            action.contains("mute") -> AudioManager.ADJUST_MUTE
            action.contains("unmute") -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_SAME
        }
        am.adjustStreamVolume(stream, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun handleMediaAction(action: String) {
        val keyCode = when (action) {
            "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return
        }
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun launchUrl(url: String) {
        try {
            val fixedUrl = if (!url.startsWith("http")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) { Log.e(TAG, "Failed to launch URL: $url", e) }
    }

    private fun handleThemeAction(action: String) {
        try {
            val mode = when (action) {
                "dark" -> 2
                "light" -> 1
                "toggle" -> if (getIsDarkModeEnabled()) 1 else 2
                else -> return
            }
            
            // This requires WRITE_SECURE_SETTINGS which is hard to get.
            // But some custom ROMs/Nothing OS might allow it or we might have it if granted via ADB.
            val success = Settings.Secure.putInt(contentResolver, "ui_night_mode", mode)
            if (success) return
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set theme directly: ${e.message}")
        }

        // Fallback: Open display settings where theme can be changed
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open display settings", e)
        }
    }

    private fun clearAllNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        // As an accessibility service, we can try to find the "Clear all" button after shade opens
        handler.postDelayed({
            val root = rootInActiveWindow ?: return@postDelayed
            findAndClickClearAll(root)
            performGlobalAction(GLOBAL_ACTION_BACK) // Close shade
        }, 800)
    }

    private fun findAndClickClearAll(node: android.view.accessibility.AccessibilityNodeInfo) {
        val clearTexts = listOf("clear all", "очистить все", "очистить всё", "delete all")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val txt = child.text?.toString()?.lowercase()
            if (txt != null && clearTexts.any { txt.contains(it) }) {
                child.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
            findAndClickClearAll(child)
        }
    }

    private fun showShortcutPanel(
        event: EventRule,
        slot: TapSlot,
        keepClick: Boolean = event.keepVisibleOnClick,
        keepOutside: Boolean = event.keepVisibleOnOutsideClick,
        keepKey: Boolean = event.keepVisibleOnKey,
        autoHide: Int = event.autoHideSeconds,
        resumeIndex: Int = -1
    ) {
        triggeredBySlot = slot
        activeShortcutEvent = event.copy(
            keepVisibleOnClick = keepClick,
            keepVisibleOnOutsideClick = keepOutside,
            keepVisibleOnKey = keepKey,
            autoHideSeconds = autoHide
        )
        activePanelSlot = TapSlot.SHORTCUT
        if (customPanelView != null) return
        val config = currentKeyMap ?: Config.readBlocking(this)
        
        val dm = android.util.DisplayMetrics(); windowManager.defaultDisplay.getRealMetrics(dm)
        val realH = dm.heightPixels.toFloat()
        val rw = 180f; val rh = 180f; val m = 60f
        val isRight = config.panelSide == PanelSide.RIGHT

        val panel = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@EssentialKeyService)
            setViewTreeViewModelStoreOwner(this@EssentialKeyService)
            setViewTreeSavedStateRegistryOwner(this@EssentialKeyService)
            
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    if (!keepOutside) dismissActivePanel?.invoke()
                    true
                } else false
            }
            
            setContent {
                val density = LocalDensity.current
                val rwDp = with(density) { rw.toDp() }
                val rhDp = with(density) { rh.toDp() }
                
                // Entry animation
                var offsetX by remember { mutableStateOf(if (isRight) 200f else -200f) }
                val animatedOffset by animateFloatAsState(
                    targetValue = offsetX, 
                    animationSpec = tween(300, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f))
                )
                LaunchedEffect(Unit) { offsetX = 0f }
                
                Box(
                    modifier = Modifier
                        .size(rwDp, rhDp)
                        .graphicsLayer { translationX = animatedOffset }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (!keepOutside) dismissActivePanel?.invoke()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                        border = BorderStroke(with(density) { 12f.toDp() }, androidx.compose.ui.graphics.Color.White),
                        modifier = Modifier
                            .size(with(density) { 160f.toDp() })
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    if (resumeIndex != -1) executeEventActions(event, resumeIndex)
                                    else executeEventActions(event)
                                    if (!keepClick) dismissActivePanel?.invoke()
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(with(density) { 32f.toDp() })) {
                            EventIconDisplay(icon = event.icon, displaySize = with(density) { 90f.toDp() })
                        }
                    }
                }
            }
        }

        val closePanel = { 
            try {
                windowManager.removeView(panel);
                customPanelView = null; activePanelSlot = null;
                dismissActivePanel = null; activeShortcutEvent = null;
                triggeredBySlot = null
            } catch(e:Exception){}
        }

        activePanelSlot = TapSlot.SHORTCUT
        dismissActivePanel = closePanel

        val params = WindowManager.LayoutParams(
            rw.toInt(), rh.toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.TOP
            val edgeMargin = (m - (rw - 160f)/2).toInt()
            x = edgeMargin
            y = (realH * (config.panelYOffset - 0.05f) - rh/2).toInt()
        }

        addOverlay(panel, params)

        if (autoHide > 0) {
            handler.postDelayed({ if (dismissActivePanel == closePanel) closePanel() }, (autoHide * 1000).toLong())
        }
    }

    private fun executeEventActions(event: EventRule, startIndex: Int = 0) {
        serviceScope.launch {
            // Wait for any UI transitions (like shortcut panel closing)
            if (startIndex == 0) {
                delay(400)
                
                if (event.delayBefore > 0) {
                    Log.d(TAG, "Event: global delay ${event.delayBefore}s")
                    delay((event.delayBefore * 1000).toLong())
                }
            }
            
            for (index in startIndex until event.actions.size) {
                val action = event.actions[index]
                // Protection: Before GESTURES, wait for any background Kill to finish
                if (action.startsWith(Config.ACTION_TOUCH_PREFIX) || 
                    action.startsWith(Config.ACTION_SWIPE_PREFIX) ||
                    action.startsWith(Config.ACTION_PATH_PREFIX)) {
                    var waitCount = 0
                    while (isKillModeActive && waitCount < 30) {
                        delay(100)
                        waitCount++
                    }
                    delay(400) // Mandatory safety delay for window transition
                }

                Log.d(TAG, "Executing event action: $action")
                when {
                    action.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX) -> {
                        val parts = action.removePrefix(Config.ACTION_WAIT_CONFIRMATION_PREFIX).split(",")
                        val kClick = parts.getOrNull(0)?.toBoolean() ?: false
                        val kOutside = parts.getOrNull(1)?.toBoolean() ?: false
                        val kKey = parts.getOrNull(2)?.toBoolean() ?: false
                        val autoHide = parts.getOrNull(3)?.toIntOrNull() ?: event.autoHideSeconds
                        
                        showShortcutPanel(event, event.triggerGesture, kClick, kOutside, kKey, autoHide, index + 1)
                        return@launch
                    }
                    action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                        performTouchActionSync(action.removePrefix(Config.ACTION_TOUCH_PREFIX))
                    }
                    action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                        performSwipeActionSync(action.removePrefix(Config.ACTION_SWIPE_PREFIX))
                    }
                    action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                        performPathActionSync(action.removePrefix(Config.ACTION_PATH_PREFIX))
                    }
                    action.startsWith(Config.ACTION_DELAY_PREFIX) -> {
                        val seconds = action.removePrefix(Config.ACTION_DELAY_PREFIX).toFloatOrNull() ?: 0f
                        delay((seconds * 1000).toLong())
                    }
                    else -> {
                        // Fixed: Pass the event's trigger gesture to preserve linearity/looping logic
                        executeActionDirectly(action, event.triggerGesture)
                        
                        if (action == Config.ACTION_KILL_NOTES || action == Config.ACTION_KILL_ESSENTIAL) {
                            var timeout = 0
                            while (isKillModeActive && timeout < 30) {
                                delay(100)
                                timeout++
                            }
                            delay(400)
                        }
                    }
                }
                delay(150) // Delay between sequence steps
            }
        }
    }

    private fun showMacroIndicator(config: KeyMap) {
        if (macroIndicatorView != null) return
        
        val dm = android.util.DisplayMetrics(); windowManager.defaultDisplay.getRealMetrics(dm)
        val realH = dm.heightPixels.toFloat()
        val isRight = config.panelSide == PanelSide.RIGHT
        val yellowInt = android.graphics.Color.parseColor("#FFD700")

        val view = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            private var glowAlpha = 0f
            private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 500
                addUpdateListener { 
                    glowAlpha = it.animatedValue as Float
                    invalidate()
                }
            }

            init { animator.start() }

            override fun onDraw(canvas: Canvas) {
                val w = width.toFloat(); val h = height.toFloat()
                val cx = if (isRight) w else 0f
                val cy = h / 2f
                
                val radius = w * 0.8f 
                val gradient = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(
                        (yellowInt and 0x00FFFFFF) or ((0xFF * glowAlpha).toInt() shl 24),
                        (yellowInt and 0x00FFFFFF) or ((0x44 * glowAlpha).toInt() shl 24),
                        android.graphics.Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                
                val matrix = Matrix()
                matrix.postScale(0.8f, 1.8f, cx, cy) // Stretched oval center
                gradient.setLocalMatrix(matrix)
                
                paint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            
            fun fadeOut(onDone: () -> Unit) {
                ValueAnimator.ofFloat(glowAlpha, 0f).apply {
                    duration = 400
                    addUpdateListener { glowAlpha = it.animatedValue as Float; invalidate() }
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { onDone() }
                    })
                    start()
                }
            }
        }

        // Layout: Centered exactly like shortcuts/panels
        val params = WindowManager.LayoutParams(
            120, 406, // Increased height to 406 (+5% from 386)
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.TOP
            x = 0
            // Fixed positioning: Shifted down by an additional 10% to fix "too high" bug
            // Updated centering offset to 203 (half of 406)
            y = (realH * (config.panelYOffset + 0.10f) - 203).toInt()
        }

        windowManager.addView(view, params)
        macroIndicatorView = view
    }

    private fun hideMacroIndicator() {
        val v = macroIndicatorView ?: return
        macroIndicatorView = null
        try {
            v.animate().alpha(0f).setDuration(400).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try { windowManager.removeView(v) } catch (e: Exception) {}
                }
            }).start()
        } catch (e: Exception) {
            try { windowManager.removeView(v) } catch (ex: Exception) {}
        }
    }

    private suspend fun executeMacroActionsSync(macro: Macro) {
        // 1. Initial buffer for UI transitions
        delay(400)
        
        for (action in macro.actions) {
            // 2. Protection: Before GESTURES, wait for any background Kill to finish
            if (action.startsWith(Config.ACTION_TOUCH_PREFIX) || action.startsWith(Config.ACTION_SWIPE_PREFIX)) {
                var waitCount = 0
                while (isKillModeActive && waitCount < 30) {
                    delay(100)
                    waitCount++
                }
                delay(400) // Mandatory safety delay for window transition
            }

            Log.d(TAG, "Executing macro action: $action")
            when {
                action.startsWith(Config.ACTION_TOUCH_PREFIX) -> performTouchActionSync(action.removePrefix(Config.ACTION_TOUCH_PREFIX))
                action.startsWith(Config.ACTION_SWIPE_PREFIX) -> performSwipeActionSync(action.removePrefix(Config.ACTION_SWIPE_PREFIX))
                action.startsWith(Config.ACTION_PATH_PREFIX) -> performPathActionSync(action.removePrefix(Config.ACTION_PATH_PREFIX))
                else -> {
                    executeActionDirectly(action, TapSlot.SINGLE)
                    // If action itself is a Kill, wait for it
                    if (action == Config.ACTION_KILL_NOTES || action == Config.ACTION_KILL_ESSENTIAL) {
                        var timeout = 0
                        while (isKillModeActive && timeout < 30) { delay(100); timeout++ }
                        delay(400)
                    }
                }
            }
            delay(150) // Delay between sequence steps
        }
    }

    private fun toggleGlyph() {
        com.HojuNK.essent_all.util.GlyphController.toggleGlyph()
    }

    private fun toggleGlyphRunner() {
        com.HojuNK.essent_all.util.GlyphController.toggleGlyphRunner()
    }

    private fun setSystemBrightness(percent: Int) {
        try {
            if (Settings.System.canWrite(this)) {
                val value = (percent / 100f * 255).toInt().coerceIn(1, 255)
                Log.d(TAG, "Setting system brightness to $percent% ($value)")
                
                // 1. Force manual mode first
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                
                // 2. Short delay to let system process mode change
                handler.postDelayed({
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
                    
                    // 3. Double-tap the value after 150ms for extreme reliability
                    handler.postDelayed({
                        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
                    }, 150)
                }, 50)
            } else {
                Log.w(TAG, "No WRITE_SETTINGS permission for brightness")
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to set brightness", e) }
    }

    private suspend fun performPathActionSync(data: String) {
        return suspendCancellableCoroutine { continuation ->
            try {
                val pts = data.split(";").filter { it.isNotEmpty() }.mapNotNull {
                    val p = it.split(",")
                    if (p.size == 3) GesturePoint(p[0].toFloat(), p[1].toFloat(), p[2].toLong()) else null
                }
                if (pts.size >= 2) {
                    val builder = android.accessibilityservice.GestureDescription.Builder()
                    val path = Path(); path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
                    val duration = pts.last().time.coerceIn(50L, 5000L)
                    builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration))
                    
                    dispatchGesture(builder.build(), object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }, null)
                } else if (continuation.isActive) continuation.resume(Unit)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private suspend fun performTouchActionSync(coords: String) {
        return suspendCancellableCoroutine { continuation ->
            try {
                val parts = if (coords.contains("_")) coords.split("_") else coords.split(",")
                if (parts.size >= 2) {
                    val x = parts[0].toFloat(); val y = parts[1].toFloat()
                    val hold = if (parts.size >= 3) parts[2].toLong() else 150L
                    
                    val path = Path().apply { moveTo(x, y) }
                    val gesture = android.accessibilityservice.GestureDescription.Builder()
                        .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, hold))
                        .build()

                    dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }, null)
                } else if (continuation.isActive) continuation.resume(Unit)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private suspend fun performSwipeActionSync(coords: String) {
        return suspendCancellableCoroutine { continuation ->
            try {
                val parts = if (coords.contains("_")) coords.split("_") else coords.split(",")
                if (parts.size >= 4) {
                    val x1 = parts[0].toFloat(); val y1 = parts[1].toFloat()
                    val x2 = parts[2].toFloat(); val y2 = parts[3].toFloat()
                    val hS = if (parts.size >= 5) parts[4].toLong() else 0L
                    val hE = if (parts.size >= 6) parts[5].toLong() else 0L
                    
                    val builder = android.accessibilityservice.GestureDescription.Builder()
                    val swipeDuration = 350L
                    
                    if (hS <= 0 && hE <= 0) {
                        // Simple swipe
                        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
                        builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, swipeDuration))
                    } else {
                        // Continuous complex gesture
                        var timeline = 0L
                        
                        // Phase 1: Hold at start
                        if (hS > 0) {
                            val p1 = Path().apply { moveTo(x1, y1) }
                            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(p1, 0, hS, true))
                            timeline = hS
                        }
                        
                        // Phase 2: Movement (Starts exactly when hold ends)
                        val p2 = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
                        builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(p2, timeline, swipeDuration, hE > 0))
                        timeline += swipeDuration
                        
                        // Phase 3: Hold at end
                        if (hE > 0) {
                            val p3 = Path().apply { moveTo(x2, y2) }
                            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(p3, timeline, hE))
                        }
                    }

                    dispatchGesture(builder.build(), object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    }, null)
                } else if (continuation.isActive) continuation.resume(Unit)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private fun performTouchAction(coords: String) {
        serviceScope.launch { performTouchActionSync(coords) }
    }

    private fun performSwipeAction(coords: String) {
        serviceScope.launch { performSwipeActionSync(coords) }
    }

    private fun setupKill(count: Int, showPanel: Boolean, slot: TapSlot?, pendingAction: String? = null) {
        handler.removeCallbacks(deactivateKillMode); isKillModeActive = true; pendingBackCount = count; shouldShowPanelAfter = showPanel
        pendingActionAfterKill = pendingAction
        handler.postDelayed(deactivateKillMode, 3000); checkAndExecute(slot ?: TapSlot.SINGLE)
    }

    private fun checkAndExecute(slot: TapSlot) {
        if (!isKillModeActive) return
        val activePkg = rootInActiveWindow?.packageName?.toString() ?: ""
        if (activePkg in TARGET_PACKAGES) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            if (pendingBackCount == 2) handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 150)
            isKillModeActive = false; handler.removeCallbacks(deactivateKillMode)
            
            // SYSTEM ACTION CANCELED: Adaptive removal trigger
            triggerAdaptiveFreezeRemoval()

            handler.postDelayed({ 
                if (pendingShortcutEvent != null) {
                    showShortcutPanel(pendingShortcutEvent!!, slot)
                    pendingShortcutEvent = null
                } else if (pendingActionAfterKill != null) {
                    executeActionDirectly(pendingActionAfterKill!!, slot)
                    pendingActionAfterKill = null
                } else if (shouldShowPanelAfter) {
                    showCustomPanel(slot)
                }
            }, 400)
        } else handler.postDelayed({ checkAndExecute(slot) }, 50)
    }

    private fun finalizeAction(slot: TapSlot) { isKillModeActive = false; shouldShowPanelAfter = false }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: ""
            if (pkg.isNotEmpty() && pkg != currentPackage) {
                previousPackage = currentPackage
                currentPackage = pkg
                Log.d(TAG, "Window changed: previous=$previousPackage, current=$currentPackage")
            }
        }
    }
    override fun onInterrupt() {}

    private fun showCustomPanel(slot: TapSlot) {
        if (customPanelView != null) return
        val config = Config.readBlocking(this); val customConfig = config.customConfigs[slot] ?: CustomConfig()
        if (config.forSlot(slot) == Config.ACTION_CONTROLS) showControlPanel(customConfig, slot)
        else {
            when (customConfig.type) {
                CustomActionType.FAVORITES -> if (customConfig.favoriteApps.isNotEmpty()) showFavoritesRoller(config, customConfig, slot) else showSimplePanel(config)
                CustomActionType.CONTROLS -> showControlPanel(customConfig, slot)
                else -> showSimplePanel(config)
            }
        }
    }

    private fun showControlPanel(customConfig: CustomConfig, slot: TapSlot) {
        activePanelSlot = slot; triggeredBySlot = slot; updateStates()
        val composeView = ComposeView(this).apply {
            setContent {
                var isEditing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                ControlPanelContent(
                    initialItems = customConfig.controlItems,
                    isEditing = isEditing,
                    onSave = { updatedItems ->
                        scope.launch { Config.setControlItems(this@EssentialKeyService, slot, updatedItems) }
                    },
                    onControlClick = { item ->
                        if (item.type == ControlType.EDIT) {
                            isEditing = true
                            false
                        } else {
                            handleControlClick(item)
                        }
                    },
                    onDismiss = { dismissActivePanel?.invoke() }
                )
            }
        }
        
        dismissActivePanel = {
            try {
                windowManager.removeView(composeView)
                customPanelView = null
                activePanelSlot = null
                dismissActivePanel = null
            } catch (e: Exception) {}
        }

        addOverlay(composeView, false)
    }

    private fun addOverlay(view: View, focusable: Boolean) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        addOverlay(view, params)
    }

    private fun addOverlay(view: View, params: WindowManager.LayoutParams) {
        view.setViewTreeLifecycleOwner(this); view.setViewTreeViewModelStoreOwner(this); view.setViewTreeSavedStateRegistryOwner(this)
        windowManager.addView(view, params)
        customPanelView = view
    }

    private fun closeOverlay(view: View?) {
        if (view == null) return
        try {
            windowManager.removeView(view)
            if (customPanelView == view) customPanelView = null
        } catch (e: Exception) {}
    }

    private fun handleControlClick(item: ControlItem): Boolean {
        val type = item.type
        try {
            when (type) {
                ControlType.EDIT -> return false // Handled in Compose content
                ControlType.FLASHLIGHT -> { toggleTorch(); return false }
                ControlType.ROTATION -> {
                    if (Settings.System.canWrite(this)) {
                        val cur = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                        Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (cur == 1) 0 else 1)
                    } else {
                        startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:$packageName"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        return true
                    }
                    return false
                }
                ControlType.VOLUME -> {
                    val am = getSystemService(AUDIO_SERVICE) as AudioManager
                    am.ringerMode = when(am.ringerMode) {
                        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                        AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                        else -> AudioManager.RINGER_MODE_NORMAL
                    }
                    return false
                }
                ControlType.DND -> {
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    if (!nm.isNotificationPolicyAccessGranted) {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return true
                    }
                    nm.setInterruptionFilter(if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_NONE)
                    return false
                }
                ControlType.SETTINGS -> {
                    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return true
                }
                ControlType.APP -> {
                    if (item.extra != null) {
                        launchApp(item.extra)
                        return true
                    }
                    return false
                }
                ControlType.URL -> {
                    if (item.extra != null) {
                        val url = if (!item.extra.startsWith("http")) "https://${item.extra}" else item.extra
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        return true
                    }
                    return false
                }
                else -> return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun showSimplePanel(config: KeyMap) {
        val rw = 160f; val rh = 300f; val m = 60f
        val isRight = config.panelSide == PanelSide.RIGHT
        val dm = android.util.DisplayMetrics(); windowManager.defaultDisplay.getRealMetrics(dm)
        val realH = dm.heightPixels

        val panel = object : View(this) {
            private val white = android.graphics.Color.WHITE
            private val dark = android.graphics.Color.parseColor("#1A1A1A")
            private var animOffset = 250f; private var isClosing = false
            init { ValueAnimator.ofFloat(250f, 0f).apply { duration = 300; interpolator = DecelerateInterpolator(); addUpdateListener { animOffset = it.animatedValue as Float; translationX = if (isRight) animOffset else -animOffset; invalidate() }; start() } }
            fun performDismiss(onDone: () -> Unit) {
                if (isClosing) return; isClosing = true
                ValueAnimator.ofFloat(0f, 250f).apply { duration = 300; addUpdateListener { animOffset = it.animatedValue as Float; translationX = if (isRight) animOffset else -animOffset; invalidate() }; start(); handler.postDelayed({ onDone() }, 300) }
            }
            override fun onDraw(canvas: Canvas) {
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawRoundRect(rect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; style = Paint.Style.FILL })
                canvas.drawRoundRect(rect, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; style = Paint.Style.STROKE; strokeWidth = 12f })
            }
        }
        
        val params = WindowManager.LayoutParams(
            rw.toInt(), rh.toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.TOP
            x = m.toInt(); y = (realH * (config.panelYOffset - 0.05f) - rh/2).toInt()
        }

        fun close() { panel.performDismiss { closeOverlay(panel) } }
        panel.setOnClickListener { close() }
        addOverlay(panel, params)
        handler.postDelayed({ if (customPanelView == panel) close() }, 5000)
    }

    private fun showFavoritesRoller(config: KeyMap, customConfig: CustomConfig, slot: TapSlot) {
        val apps = customConfig.favoriteApps.mapNotNull { pkg -> try { pkg to packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null } }
        if (apps.isEmpty()) { showSimplePanel(config); return }
        
        val itemSize = 180f; val ph = (itemSize * 5.5f).toInt()
        val m = 60f; val isRight = config.panelSide == PanelSide.RIGHT
        val dm = android.util.DisplayMetrics(); windowManager.defaultDisplay.getRealMetrics(dm)
        val realH = dm.heightPixels

        val roller = object : View(this) {
            private val white = android.graphics.Color.WHITE
            private val dark = android.graphics.Color.parseColor("#1A1A1A")
            var scrollY = 0f; var animOffset = 300f; var isClosing = false
            var startY = 0f; var isDragging = false; var snapAnimator: ValueAnimator? = null
            init { ValueAnimator.ofFloat(300f, 0f).apply { duration = 300; interpolator = DecelerateInterpolator(); addUpdateListener { animOffset = it.animatedValue as Float; translationX = if (isRight) animOffset else -animOffset; invalidate() }; start() } }
            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f; val cy = height / 2f
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawRoundRect(rect, 45f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dark; style = Paint.Style.FILL })
                val sr = RectF(8f, cy - itemSize/2 + 8f, width - 8f, cy + itemSize/2 - 8f)
                canvas.drawRoundRect(sr, 35f, 35f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#44FFFFFF"); style = Paint.Style.FILL })
                canvas.drawRoundRect(sr, 35f, 35f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; style = Paint.Style.STROKE; strokeWidth = 4f })
                canvas.save(); canvas.clipRect(rect)
                val ri = Math.round(scrollY / itemSize).toInt()
                for (i in (ri - 4)..(ri + 4)) {
                    val index = ((i % apps.size) + apps.size) % apps.size; val (_, icon) = apps[index]
                    val ip = (i * itemSize) - scrollY
                    val ix = cx; val iy = cy + ip
                    val dist = Math.abs(iy - cy)
                    val scale = 1f - (dist / (height / 2f)).coerceIn(0f, 0.4f); val alpha = (255 * (1f - dist / (ph / 2f))).toInt().coerceIn(0, 255)
                    val s = (itemSize / 2 * scale).toInt(); icon.setBounds((ix - s + 25).toInt(), (iy - s + 25).toInt(), (ix + s - 25).toInt(), (iy + s - 25).toInt())
                    icon.alpha = alpha; icon.draw(canvas)
                }
                canvas.restore(); canvas.drawRoundRect(rect, 45f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white; style = Paint.Style.STROKE; strokeWidth = 3f })
            }
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    if (!customConfig.keepVisibleOnOutsideClick) dismiss()
                    return true
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { startY = event.y; isDragging = false; snapAnimator?.cancel() }
                    MotionEvent.ACTION_MOVE -> { val cp = event.y; val d = (startY - cp) * 1.5f; if (Math.abs(d) > 5f || isDragging) { scrollY += d; startY = cp; isDragging = true; invalidate() } }
                    MotionEvent.ACTION_UP -> { if (isDragging) { snapAnimator = ValueAnimator.ofFloat(scrollY, Math.round(scrollY / itemSize) * itemSize).apply { duration = 150; addUpdateListener { scrollY = it.animatedValue as Float; invalidate() }; start() } } else { launchApp(apps[((Math.round(scrollY/itemSize)%apps.size)+apps.size)%apps.size].first); if (!customConfig.keepVisibleOnClick) dismiss() } }
                }
                return true
            }
            fun dismiss() { if (isClosing) return; isClosing = true; snapAnimator?.cancel(); val v = this; ValueAnimator.ofFloat(0f, 300f).apply { duration = 300; addUpdateListener { animOffset = it.animatedValue as Float; translationX = if (isRight) animOffset else -animOffset; invalidate() }; start(); handler.postDelayed({ closeOverlay(v); activePanelSlot = null; dismissActivePanel = null }, 300) } }
        }

        activePanelSlot = slot
        dismissActivePanel = { roller.dismiss() }
        
        val params = WindowManager.LayoutParams(
            itemSize.toInt(), ph,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.TOP
            x = 60; y = (realH.toFloat() * (config.panelYOffset - 0.05f) - ph/2).toInt()
        }
        
        addOverlay(roller, params)
        activePanelSlot = slot
        triggeredBySlot = slot
        val currentDismiss = { roller.dismiss() }
        dismissActivePanel = currentDismiss
        
        if (customConfig.autoHideSeconds > 0) {
            handler.postDelayed({ if (dismissActivePanel == currentDismiss) currentDismiss() }, (customConfig.autoHideSeconds * 1000).toLong())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Critical safety check: if we are not truly active as an Accessibility Service, 
        // WindowManager will throw BadTokenException for TYPE_ACCESSIBILITY_OVERLAY.
        if (!isAccessibilityConnected) {
            Log.e(TAG, "EssentialKeyService started but Accessibility Service is NOT active. Ignoring intent.")
            return START_NOT_STICKY
        }

        if (intent?.action == "com.HojuNK.essent_all.START_MACRO_REC") {
            val modeName = intent.getStringExtra("mode") ?: MacroRecordingMode.COMBINATION.name
            val mode = MacroRecordingMode.valueOf(modeName)
            showMacroRecorderOverlay(mode)
        } else if (intent?.action == "com.HojuNK.essent_all.TOGGLE_GLYPHS") {
            toggleGlyph()
        } else if (intent?.action == "com.HojuNK.essent_all.TOGGLE_GLYPH_RUNNER") {
            toggleGlyphRunner()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showMacroRecorderOverlay(mode: MacroRecordingMode) {
        closeOverlay(customPanelView)
        val white = Color.WHITE
        val recordedActions = mutableListOf<String>()
        var isRecording = false
        var lastActionTime = 0L
        var lastX = -100f; var lastY = -100f
        
        val dm = android.util.DisplayMetrics(); windowManager.defaultDisplay.getRealMetrics(dm)
        val realH = dm.heightPixels.toFloat()
        val side = currentKeyMap?.panelSide ?: PanelSide.RIGHT
        val yOff = (currentKeyMap?.panelYOffset ?: 0.5f) - 0.05f

        val island = android.widget.LinearLayout(this)
        var captureLayer: View? = null
        var btnRecRef: android.widget.ImageButton? = null

        val inactivityTimeout = object : Runnable {
            override fun run() {
                Log.d(TAG, "Recording auto-stopped due to 1 minute inactivity")
                isRecording = false
                try { if (captureLayer != null) windowManager.removeView(captureLayer) } catch(e:Exception){}
                btnRecRef?.setImageResource(android.R.drawable.ic_media_play)
                btnRecRef?.setColorFilter(white)
            }
        }

        fun stopRecordingInUI() {
            isRecording = false
            handler.removeCallbacks(inactivityTimeout)
            try { if (captureLayer != null) windowManager.removeView(captureLayer) } catch(e:Exception){}
            btnRecRef?.setImageResource(android.R.drawable.ic_media_play)
            btnRecRef?.setColorFilter(white)
        }

        fun resetInactivityTimer() {
            handler.removeCallbacks(inactivityTimeout)
            if (isRecording) handler.postDelayed(inactivityTimeout, 60000)
        }

        // 1. Capture Canvas: A pure "listener" layer. NO re-dispatching during recording.
        val captureParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        captureLayer = object : View(this) {
            private val redPaint = Paint().apply { color = Color.parseColor("#66FF0000"); style = Paint.Style.STROKE; strokeWidth = 15f }
            private var isInjecting = false
            private val touchPoints = mutableListOf<GesturePoint>()
            private var startTime = 0L

            override fun onDraw(canvas: Canvas) {
                if (isRecording) canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), redPaint)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (!isRecording || isInjecting) return false
                resetInactivityTimer()
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Record delay between actions
                        if (recordedActions.isNotEmpty()) {
                            val delayMs = System.currentTimeMillis() - lastActionTime
                            if (delayMs > 50) { // Reduced threshold for better responsiveness
                                val delaySec = String.format(Locale.US, "%.2f", delayMs / 1000f)
                                recordedActions.add("${Config.ACTION_DELAY_PREFIX}$delaySec")
                            }
                        }
                        
                        touchPoints.clear()
                        startTime = System.currentTimeMillis()
                        val adjY = event.y + (realH * 0.05f) // Corrected: +5% height to fix "above finger" offset
                        touchPoints.add(GesturePoint(event.x, adjY, startTime))
                        lastX = event.x; lastY = adjY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val adjY = event.y + (realH * 0.05f)
                        touchPoints.add(GesturePoint(event.x, adjY, System.currentTimeMillis()))
                    }
                    MotionEvent.ACTION_UP -> {
                        val adjY = event.y + (realH * 0.05f)
                        val endTime = System.currentTimeMillis()
                        touchPoints.add(GesturePoint(event.x, adjY, endTime))
                        
                        val duration = (endTime - startTime).coerceAtLeast(50L)
                        val dx = (event.x - lastX).toDouble()
                        val dy = (adjY - lastY).toDouble()
                        val dist = Math.sqrt(dx * dx + dy * dy)
                        
                        val action = if (dist < 40 && duration < 500) {
                            // TAP detected with accurate duration
                            "${Config.ACTION_TOUCH_PREFIX}${event.x.toInt()}_${adjY.toInt()}_$duration"
                        } else {
                            // PATH detected (complex/non-linear)
                            val pathData = touchPoints.joinToString(";") { "${it.x.toInt()},${it.y.toInt()},${it.time - startTime}" }
                            "${Config.ACTION_PATH_PREFIX}$pathData"
                        }
                        
                        recordedActions.add(action)
                        lastActionTime = endTime
                        
                        // Ретрансляция жеста в систему (Passthrough)
                        serviceScope.launch {
                            try {
                                isInjecting = true
                                // Принудительно отключаем захват касаний оверлеем
                                if (captureLayer?.isAttachedToWindow == true) {
                                    captureParams.flags = captureParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    windowManager.updateViewLayout(captureLayer, captureParams)
                                }
                                
                                // Даем системе время применить флаги
                                delay(30)
                                
                                when {
                                    action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                                        performTouchActionSync(action.removePrefix(Config.ACTION_TOUCH_PREFIX))
                                    }
                                    action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                                        performPathActionSync(action.removePrefix(Config.ACTION_PATH_PREFIX))
                                    }
                                    else -> {
                                        performSwipeActionSync(action.removePrefix(Config.ACTION_SWIPE_PREFIX))
                                    }
                                }
                                
                                // Возвращаем оверлей в режим захвата
                                if (captureLayer?.isAttachedToWindow == true) {
                                    captureParams.flags = captureParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                    windowManager.updateViewLayout(captureLayer, captureParams)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Passthrough failed: ${e.message}")
                            } finally {
                                // Минимальная пауза после инъекции
                                delay(50)
                                isInjecting = false
                            }
                        }

                        if (mode == MacroRecordingMode.SINGLE_TAP && dist < 40) { 
                            isRecording = false; handler.postDelayed({ stopRecordingInUI() }, 300) 
                        } else if (mode == MacroRecordingMode.SINGLE_SWIPE && dist >= 40) {
                            isRecording = false; handler.postDelayed({ stopRecordingInUI() }, 800)
                        }
                    }
                }
                return true
            }
        }

        val islandParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (side == PanelSide.RIGHT) Gravity.END else Gravity.START) or Gravity.TOP
            x = 60; y = (realH * yOff - 100).toInt()
        }

        island.apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            background = android.graphics.drawable.GradientDrawable().apply { 
                setColor(Color.parseColor("#EE1A1A1A"))
                cornerRadius = 50f; setStroke(3, white) 
            }
            setPadding(20, 10, 20, 10); gravity = Gravity.CENTER
            
            // Drag Button
            val btnDrag = android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_directions); background = null; setColorFilter(Color.WHITE)
                var startX = 0f; var startY = 0f; var initialX = 0; var initialY = 0
                val isRightSide = side == PanelSide.RIGHT
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> { startX = event.rawX; startY = event.rawY; initialX = islandParams.x; initialY = islandParams.y }
                        MotionEvent.ACTION_MOVE -> { 
                            val deltaX = event.rawX - startX
                            val deltaY = event.rawY - startY
                            islandParams.x = (initialX + (if (isRightSide) -deltaX else deltaX)).toInt()
                            islandParams.y = (initialY + deltaY).toInt()
                            windowManager.updateViewLayout(island, islandParams)
                        }
                    }
                    true
                }
            }

            btnRecRef = android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_media_play); background = null; setColorFilter(white)
                setOnClickListener {
                    isRecording = !isRecording
                    if (isRecording) {
                        lastActionTime = System.currentTimeMillis()
                        try {
                            windowManager.addView(captureLayer, captureParams)
                            // Принудительно выводим островок на передний план, пересоздавая его окно
                            windowManager.removeView(island)
                            windowManager.addView(island, islandParams)
                        } catch (e: Exception) { Log.e(TAG, "Recording overlay error: ${e.message}") }
                        
                        setImageResource(android.R.drawable.presence_online)
                    setColorFilter(android.graphics.Color.RED)
                        resetInactivityTimer()
                    } else {
                        stopRecordingInUI()
                    }
                }
            }

            val btnReset = android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_rotate); background = null; setColorFilter(Color.WHITE)
                setOnClickListener { recordedActions.clear() }
            }
            
            val btnSave = android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_save); background = null; setColorFilter(white)
                setOnClickListener { if (recordedActions.isNotEmpty()) { stopRecordingInUI(); stopAndSave(recordedActions) } }
            }

            val btnClose = android.widget.ImageButton(context).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel); background = null; setColorFilter(Color.WHITE)
                setOnClickListener { stopRecordingInUI(); closeOverlay(island) }
            }

            addView(btnDrag); addView(btnRecRef); addView(btnReset); addView(btnSave); addView(btnClose)
        }

        addOverlay(island, islandParams)
    }

    private fun stopAndSave(actions: List<String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("recorded_macro_actions", actions.toTypedArray())
        }
        startActivity(intent)
        closeOverlay(customPanelView)
    }

    private fun launchApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } ?: return
        startActivity(intent)
    }

    private fun findTorchCameraId(): String? = try { cameraManager.cameraIdList.firstOrNull { id -> cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } } catch (e: Exception) { null }
    private fun handleFlashlightAction(action: String) {
        val id = torchCameraId ?: return
        val mode = if (action.startsWith(Config.ACTION_FLASHLIGHT_PREFIX)) {
            action.removePrefix(Config.ACTION_FLASHLIGHT_PREFIX)
        } else {
            "toggle"
        }
        try {
            val next = when (mode) {
                "on" -> true
                "off" -> false
                else -> !torchOn
            }
            cameraManager.setTorchMode(id, next)
            torchOn = next
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle flashlight action: ${e.message}")
        }
    }

    private fun handleGlyphAction(action: String) {
        val mode = if (action.startsWith(Config.ACTION_GLYPH_PREFIX)) {
            action.removePrefix(Config.ACTION_GLYPH_PREFIX)
        } else {
            "toggle"
        }
        when (mode) {
            "on" -> com.HojuNK.essent_all.util.GlyphController.turnOnGlyphs()
            "off" -> com.HojuNK.essent_all.util.GlyphController.turnOffGlyphs()
            else -> com.HojuNK.essent_all.util.GlyphController.toggleGlyph()
        }
    }

    private fun toggleTorch() { val id = torchCameraId ?: return; try { val next = !torchOn; cameraManager.setTorchMode(id, next); torchOn = next } catch (e: Exception) {} }

    private fun showFreezeOverlay(keyMap: KeyMap, isOverride: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(0, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val hardwareBuffer = screenshot.hardwareBuffer
                        val colorSpace = screenshot.colorSpace
                        val bitmap = try {
                            Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                        } catch (e: Exception) { null }
                        hardwareBuffer.close()
                        
                        if (bitmap != null) {
                            handler.post { createAndShowFreezeView(bitmap, keyMap, isOverride) }
                        }
                    }
                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Freeze screenshot failed: $errorCode")
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "takeScreenshot exception: ${e.message}")
            }
        }
    }

    private fun createAndShowFreezeView(bitmap: Bitmap, keyMap: KeyMap, isOverride: Boolean) {
        if (freezeOverlayView != null) removeFreezeOverlay()
        
        isFreezeOverrideActive = isOverride
        val context = this
        val isRight = keyMap.panelSide == PanelSide.RIGHT
        val dm = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val realH = dm.heightPixels.toFloat()
        
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarH = if (resId > 0) resources.getDimensionPixelSize(resId) else 0

        val view = object : FrameLayout(context) {
            init {
                addView(ImageView(context).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_XY 
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    pivotY = 0f
                    translationY = -statusBarH.toFloat()
                    scaleY = (realH + statusBarH.toFloat()) / realH
                    scaleX = 1.0f
                })
                
                addView(object : View(context) {
                    private val yellow = android.graphics.Color.parseColor("#FFD700")
                    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    private var glowAlpha = 0f
                    
                    init {
                        layoutParams = LayoutParams(120, 406).apply {
                            gravity = (if (isRight) Gravity.END else Gravity.START) or Gravity.TOP
                            topMargin = (realH * (keyMap.panelYOffset + 0.10f) - 203).toInt()
                        }
                        ValueAnimator.ofFloat(0f, 1f).apply {
                            duration = 300
                            addUpdateListener { glowAlpha = it.animatedValue as Float; invalidate() }
                            start()
                        }
                    }

                    override fun onDraw(canvas: Canvas) {
                        val w = width.toFloat(); val h = height.toFloat()
                        val cx = if (isRight) w else 0f
                        val cy = h / 2f
                        val radius = w * 0.8f 
                        val gradient = RadialGradient(
                            cx, cy, radius,
                            intArrayOf(
                                (yellow and 0x00FFFFFF) or ((0xFF * glowAlpha).toInt() shl 24),
                                (yellow and 0x00FFFFFF) or ((0x44 * glowAlpha).toInt() shl 24),
                                android.graphics.Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        val matrix = Matrix()
                        matrix.postScale(0.8f, 1.8f, cx, cy) 
                        gradient.setLocalMatrix(matrix)
                        paint.shader = gradient
                        canvas.drawRect(0f, 0f, w, h, paint)
                    }
                })
            }
        }
        
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            alpha = 1.0f
        }
        
        freezeOverlayView = view
        try {
            windowManager.addView(view, params)
            customPanelView?.let { panel ->
                try {
                    windowManager.removeView(panel)
                    windowManager.addView(panel, panel.layoutParams)
                } catch (e: Exception) {}
            }
        } catch (e: Exception) { Log.e(TAG, "Failed to add freeze overlay", e) }
        
        if (!isOverride) {
            // Adaptive: If no action is pending, dissolve immediately
            handler.postDelayed({ dissolveFreezeOverlay(view) }, 200)
        } else {
            // Safety timeout: remove after 8 seconds if no trigger received
            handler.postDelayed({ dissolveFreezeOverlay(view) }, 8000)
        }
    }

    private fun triggerAdaptiveFreezeRemoval() {
        if (isFreezeOverrideActive) {
            isFreezeOverrideActive = false
            // Immediately start dissolving (no delay)
            freezeOverlayView?.let { dissolveFreezeOverlay(it) }
        }
    }

    private fun pasteTextToActiveField(text: String) {
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("EssentAI Paste", text))
            Toast.makeText(this, "Скопировано в буфер", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to paste text", e)
        }
    }

    private fun showClipboardOverlay() {
        dismissActivePanel?.invoke()
        try {
            ClipboardHistoryManager.syncSystemClipboard(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read clipboard on overlay open", e)
        }

        isClipboardOverlayOpen = true
        val isLocked = getIsScreenLocked()
        val activePkg = rootInActiveWindow?.packageName?.toString() ?: currentPackage

        lateinit var composeView: ComposeView

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@EssentialKeyService)
            setViewTreeViewModelStoreOwner(this@EssentialKeyService)
            setViewTreeSavedStateRegistryOwner(this@EssentialKeyService)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    // Always pinned: do not dismiss on outside touch
                    true
                } else false
            }

            setContent {
                ClipboardOverlayContent(
                    isScreenLocked = isLocked,
                    activePackageName = activePkg,
                    onDismiss = { dismissActivePanel?.invoke() },
                    onPasteItem = { text ->
                        pasteTextToActiveField(text)
                        dismissActivePanel?.invoke()
                    },
                    onHeightChanged = { newHeightPx ->
                        if (params.height != newHeightPx) {
                            params.height = newHeightPx
                            try {
                                windowManager.updateViewLayout(composeView, params)
                            } catch (e: Exception) {}
                        }
                    },
                    onKeyboardFocusRequested = { needsFocus ->
                        if (needsFocus) {
                            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                        } else {
                            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        }
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    },
                    onToggleCustomizableMode = { active ->
                        if (active) {
                            params.gravity = Gravity.TOP or Gravity.START
                        } else {
                            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        }
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    },
                    onMoveOverlay = { dx, dy ->
                        params.gravity = Gravity.TOP or Gravity.START
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    },
                    onResizeCorner = { dw, dh, corner ->
                        params.gravity = Gravity.TOP or Gravity.START
                        val newWidth = (params.width + dw.toInt()).coerceAtLeast(200)
                        val newHeight = (params.height + dh.toInt()).coerceAtLeast(150)
                        if (corner == "TOP_LEFT") {
                            params.x -= (newWidth - params.width)
                            params.y -= (newHeight - params.height)
                        } else if (corner == "TOP_RIGHT") {
                            params.y -= (newHeight - params.height)
                        } else if (corner == "BOTTOM_LEFT") {
                            params.x -= (newWidth - params.width)
                        }
                        params.width = newWidth
                        params.height = newHeight
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    },
                    onSaveCustomLayout = {
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    },
                    onResetCustomLayout = {
                        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        params.width = WindowManager.LayoutParams.MATCH_PARENT
                        params.height = WindowManager.LayoutParams.MATCH_PARENT
                        params.x = 0
                        params.y = 0
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    }
                )
            }
        }

        val closePanel: () -> Unit = {
            try {
                isClipboardOverlayOpen = false
                windowManager.removeView(composeView)
                customPanelView = null
                activePanelSlot = null
                dismissActivePanel = null

                val shortcutEv = activeShortcutEvent
                if (shortcutEv != null && shortcutEv.keepVisibleOnClick) {
                    val slot = triggeredBySlot ?: TapSlot.SINGLE
                    handler.post {
                        showShortcutPanel(
                            shortcutEv,
                            slot,
                            keepClick = true,
                            keepOutside = shortcutEv.keepVisibleOnOutsideClick,
                            keepKey = shortcutEv.keepVisibleOnKey,
                            autoHide = shortcutEv.autoHideSeconds
                        )
                    }
                } else {
                    activeShortcutEvent = null
                    triggeredBySlot = null
                }
            } catch (e: Exception) {}
        }

        dismissActivePanel = closePanel

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show clipboard overlay", e)
        }
    }

    private fun dissolveFreezeOverlay(view: View) {
        if (freezeOverlayView == view) {
            view.animate()
                .alpha(0f)
                .setDuration(1000)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction {
                    if (freezeOverlayView == view) removeFreezeOverlay()
                }.start()
        }
    }

    private fun removeFreezeOverlay() {
        freezeOverlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            freezeOverlayView = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected(); 
        instance = this
        isAccessibilityConnected = true
        com.HojuNK.essent_all.util.GlyphController.init(this)
        
        val info = serviceInfo
        info.flags = info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        serviceInfo = info
        
        serviceScope.launch {
            Config.flow(this@EssentialKeyService).collect { keyMap ->
                currentKeyMap = keyMap
                updateSensorRegistration()
            }
        }

        cameraManager.registerTorchCallback(torchCallback, handler)
        
        val filter = IntentFilter().apply { 
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION) 
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(stateReceiver, filter)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        Log.i(TAG, "EssentialKeyService connected")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> lastLightValue = event.values[0]
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelX = event.values[0]
                lastAccelY = event.values[1]
                lastAccelZ = event.values[2]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onUnbind(intent: Intent?): Boolean { 
        if (instance == this) instance = null
        isAccessibilityConnected = false
        serviceScope.cancel()
        cameraManager.unregisterTorchCallback(torchCallback)
        sensorManager.unregisterListener(this)
        try { unregisterReceiver(stateReceiver) } catch (e: Exception) {}
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        return super.onUnbind(intent) 
    }

    companion object {
        const val TAG = "EMAP"; const val TARGET_SCAN_CODE = 250; const val TAP_DEBOUNCE_MS = 900L; const val LONG_PRESS_MS = 500L
        private val TARGET_PACKAGES = setOf("com.nothing.ntessentialrecorder", "com.nothing.ntessentialspace", "com.nothing.essentialintelligence.extradark.overlay", "com.nothing.ntessentialrecorder.extradark.overlay", "com.nothing.ntessentialspace.extradark.overlay", "com.nothing.ntessentialspace.overlay.overlay.all_devices", "com.nothing.ntessentialspace.overlay.asteroids")
        var instance: EssentialKeyService? = null
            private set

        fun triggerPowerLongAction(context: Context) {
            instance?.let { service ->
                val config = service.currentKeyMap ?: Config.readBlocking(service)
                val action = config.powerLong ?: Config.ACTION_POWER_MENU
                service.dispatch(action, TapSlot.POWER_LONG)
            }
        }
    }
}
