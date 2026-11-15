package com.HojuNK.essent_all.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.ClipboardHistoryManager
import com.HojuNK.essent_all.ui.components.ClipboardOverlayContent
import com.HojuNK.essent_all.ui.components.ClipboardOverlayState
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.model.LocalRadialMenuState

@Composable
fun ClipboardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val radialMenuState = LocalRadialMenuState.current

    LaunchedEffect(Unit) {
        try {
            ClipboardHistoryManager.syncSystemClipboard(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val useDark = com.HojuNK.essent_all.LocalUseDark.current
    val bg = if (useDark) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .nothingBlur(enabled = radialMenuState != null)
    ) {
        ClipboardOverlayContent(
            isScreenLocked = false,
            initialState = ClipboardOverlayState.EXPANDED,
            isFullScreen = true,
            onDismiss = onBack,
            onPasteItem = { text ->
                ClipboardHistoryManager.copyToClipboard(context, text)
                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
