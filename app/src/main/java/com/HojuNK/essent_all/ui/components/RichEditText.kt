package com.HojuNK.essent_all.ui.components

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat

class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    var onImageReceived: ((Uri) -> Unit)? = null
    var onSelectionChangedCallback: ((Int, Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedCallback?.invoke(selStart, selEnd)
    }

    init {
        val mimeTypes = arrayOf("image/png", "image/jpeg", "image/gif", "image/webp")
        ViewCompat.setOnReceiveContentListener(this as android.view.View, mimeTypes) { _, payload ->
            val split = payload.partition { item -> item.uri != null }
            val uriContent = split.first
            val remaining = split.second

            uriContent?.let { content ->
                for (i in 0 until content.clip.itemCount) {
                    val uri = content.clip.getItemAt(i).uri
                    if (uri != null) {
                        onImageReceived?.invoke(uri)
                    }
                }
            }
            remaining
        }
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(editorInfo) ?: return null

        EditorInfoCompat.setContentMimeTypes(
            editorInfo,
            arrayOf("image/png", "image/jpeg", "image/gif", "image/webp")
        )

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
            Log.d("RichEditText", "commitContent called: ${inputContentInfo.description.getMimeType(0)}")

            if (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    Log.e("RichEditText", "requestPermission failed: ${e.message}")
                    return@OnCommitContentListener false
                }
            }

            try {
                val uri = inputContentInfo.contentUri
                onImageReceived?.invoke(uri)
            } finally {
                try {
                    inputContentInfo.releasePermission()
                } catch (_: Exception) {}
            }

            true
        }

        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }
}
