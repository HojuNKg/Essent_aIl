package com.HojuNK.essent_all

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ClipboardItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "text", // "text", "link", "image"
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isNote: Boolean = false
)

object ClipboardHistoryManager {
    private const val PREF_NAME = "clipboard_history_prefs"
    private const val KEY_HISTORY = "clipboard_history_list"
    private const val MAX_ITEMS = 50

    fun getHistory(context: Context): List<ClipboardItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<ClipboardItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ClipboardItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        type = obj.optString("type", "text"),
                        content = obj.optString("content", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isPinned = obj.optBoolean("isPinned", false),
                        isNote = obj.optBoolean("isNote", false)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(context: Context, list: List<ClipboardItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("content", item.content)
                put("timestamp", item.timestamp)
                put("isPinned", item.isPinned)
                put("isNote", item.isNote)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun addItemAndGetId(context: Context, content: String, type: String = "text", isNote: Boolean = false): String {
        if (content.isBlank()) return ""
        val current = getHistory(context).toMutableList()
        
        val resolvedType = when {
            type == "image" || content.startsWith("content://") || content.startsWith("file://") -> "image"
            content.startsWith("http://") || content.startsWith("https://") -> "link"
            else -> "text"
        }

        // If item already exists, remove it and re-add at the top with fresh timestamp
        val existing = current.find { it.content == content }
        if (existing != null) {
            current.remove(existing)
            val newItem = existing.copy(timestamp = System.currentTimeMillis(), isNote = existing.isNote || isNote)
            current.add(0, newItem)
            saveHistory(context, current)
            return newItem.id
        }

        val newItem = ClipboardItem(
            type = resolvedType,
            content = content,
            isNote = isNote
        )
        current.add(0, newItem)
        if (current.size > MAX_ITEMS) {
            val pinned = current.filter { it.isPinned }
            val unpinned = current.filter { !it.isPinned }.take(MAX_ITEMS - pinned.size)
            saveHistory(context, pinned + unpinned)
        } else {
            saveHistory(context, current)
        }
        return newItem.id
    }

    fun addItem(context: Context, content: String, type: String = "text") {
        addItemAndGetId(context, content, type)
    }

    fun syncSystemClipboard(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val text = item.text?.toString()
                val uri = item.uri
                if (!text.isNullOrBlank()) {
                    addItem(context, text, "text")
                    return true
                } else if (uri != null) {
                    addItem(context, uri.toString(), "image")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardHistoryManager", "Sync system clipboard failed", e)
        }
        return false
    }

    fun removeItem(context: Context, id: String) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.id == id }
        saveHistory(context, current)
    }

    fun updateItem(context: Context, updatedItem: ClipboardItem) {
        val current = getHistory(context).toMutableList()
        val index = current.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            current[index] = updatedItem
            saveHistory(context, current)
        } else {
            addItem(context, updatedItem.content, updatedItem.type)
        }
    }

    fun togglePin(context: Context, id: String) {
        val current = getHistory(context).map { item ->
            if (item.id == id) item.copy(isPinned = !item.isPinned) else item
        }
        saveHistory(context, current)
    }

    fun clearHistory(context: Context) {
        val current = getHistory(context).filter { it.isPinned }
        saveHistory(context, current)
    }

    fun copyToClipboard(context: Context, text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (text.contains("<") && text.contains(">")) {
                val plainText = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trimEnd()
                val clipData = ClipData.newHtmlText("EssentAI Clipboard", plainText, text)
                cm?.setPrimaryClip(clipData)
            } else {
                cm?.setPrimaryClip(ClipData.newPlainText("EssentAI Clipboard", text))
            }
            addItem(context, text)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun copyToClipboard(context: Context, item: ClipboardItem) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (item.type == "image") {
                val uri = Uri.parse(item.content)
                cm?.setPrimaryClip(ClipData.newUri(context.contentResolver, "EssentAI Clipboard Image", uri))
            } else {
                if (item.content.contains("<") && item.content.contains(">")) {
                    val plainText = android.text.Html.fromHtml(item.content, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trimEnd()
                    val clipData = ClipData.newHtmlText("EssentAI Clipboard", plainText, item.content)
                    cm?.setPrimaryClip(clipData)
                } else {
                    cm?.setPrimaryClip(ClipData.newPlainText("EssentAI Clipboard", item.content))
                }
            }
            addItem(context, item.content, item.type)
        } catch (e: Exception) {
            Log.e("ClipboardHistoryManager", "Copy to clipboard failed", e)
        }
    }
}
