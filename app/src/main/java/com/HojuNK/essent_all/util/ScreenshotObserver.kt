package com.HojuNK.essent_all.util

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

class ScreenshotObserver(
    private val context: Context,
    private val onNewScreenshot: (Uri) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val contentResolver = context.contentResolver
    private var lastCapturedUri: String? = null

    fun startListening() {
        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            contentResolver.unregisterContentObserver(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val dateAdded = cursor.getLong(dateCol)
                    val currentTimeSec = System.currentTimeMillis() / 1000L

                    if (currentTimeSec - dateAdded < 15L &&
                        (name.contains("screenshot", ignoreCase = true) ||
                         name.contains("скриншот", ignoreCase = true) ||
                         name.contains("screen", ignoreCase = true) ||
                         name.contains("shot", ignoreCase = true))) {

                        val imageUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        if (imageUri.toString() != lastCapturedUri) {
                            lastCapturedUri = imageUri.toString()
                            onNewScreenshot(imageUri)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
