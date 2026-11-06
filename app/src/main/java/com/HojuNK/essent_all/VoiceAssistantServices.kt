package com.HojuNK.essent_all

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.util.Log

class VoiceAssistantService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.d("VoiceAssistantService", "Voice interaction assistant ready")
    }

    companion object {
        fun isAppCurrentAssistant(context: Context): Boolean {
            return isActiveService(context, ComponentName(context, VoiceAssistantService::class.java))
        }

        fun openDefaultAssistantSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    Log.e("VoiceAssistantService", "Cannot open settings: ${ex.message}")
                }
            }
        }
    }
}

class VoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle?): VoiceInteractionSession {
        return VoiceAssistantSession(this)
    }
}

class VoiceAssistantSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d("VoiceAssistantSession", "Power button long press session triggered!")
        EssentialKeyService.triggerPowerLongAction(context)
        finish()
    }
}

class VoiceRecognitionService : RecognitionService() {
    override fun onStartListening(intent: Intent?, listener: Callback?) {}
    override fun onCancel(listener: Callback?) {}
    override fun onStopListening(listener: Callback?) {}
}
