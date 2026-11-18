package com.HojuNK.essent_all.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

data class ScreenTextItem(
    val id: String,
    val text: String,
    val boundingBox: Rect
)

data class SupportedLanguage(val code: String, val name: String, val mlKitCode: String)

object TranslationLanguages {
    val ALL = listOf(
        SupportedLanguage("ru", "Русский", TranslateLanguage.RUSSIAN),
        SupportedLanguage("en", "English", TranslateLanguage.ENGLISH),
        SupportedLanguage("de", "Deutsch", TranslateLanguage.GERMAN),
        SupportedLanguage("es", "Español", TranslateLanguage.SPANISH),
        SupportedLanguage("fr", "Français", TranslateLanguage.FRENCH),
        SupportedLanguage("zh", "中文", TranslateLanguage.CHINESE),
        SupportedLanguage("ja", "日本語", TranslateLanguage.JAPANESE),
        SupportedLanguage("ko", "한국어", TranslateLanguage.KOREAN),
        SupportedLanguage("it", "Italiano", TranslateLanguage.ITALIAN),
        SupportedLanguage("pt", "Português", TranslateLanguage.PORTUGUESE),
        SupportedLanguage("tr", "Türkçe", TranslateLanguage.TURKISH),
        SupportedLanguage("ar", "العربية", TranslateLanguage.ARABIC)
    )

    private const val PREFS_NAME = "screen_search_prefs"
    private const val KEY_TARGET_LANG = "target_lang_code"

    fun getSavedTargetLanguage(context: Context): SupportedLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultLangCode = Locale.getDefault().language.lowercase()
        val savedCode = prefs.getString(KEY_TARGET_LANG, defaultLangCode) ?: defaultLangCode
        return ALL.find { it.code == savedCode } ?: ALL.find { it.code == "ru" } ?: ALL.first()
    }

    fun saveTargetLanguage(context: Context, langCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TARGET_LANG, langCode).apply()
    }
}

object ImagePreProcessor {
    fun prepareBitmapForOcr(original: Bitmap): Bitmap {
        return try {
            var scaledBitmap = original
            if (original.width < 1000 || original.height < 1000) {
                val scale = 1.5f
                scaledBitmap = Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt(),
                    true
                )
            }

            val bmpGrayscale = Bitmap.createBitmap(
                scaledBitmap.width,
                scaledBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmpGrayscale)
            val paint = Paint()

            val cm = ColorMatrix().apply {
                setSaturation(0f)
            }

            val contrast = 1.2f
            val translate = (-0.5f * 255 * contrast + 0.5f * 255)
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(contrastMatrix)

            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

            bmpGrayscale
        } catch (e: Throwable) {
            Log.e("ImagePreProcessor", "prepareBitmapForOcr exception: ${e.message}")
            original
        }
    }
}

object CyrillicVisualRestorer {
    private val visualMap = mapOf(
        'n' to 'п', 'p' to 'р', 'u' to 'и', 'v' to 'в', 'm' to 'т',
        'w' to 'ш', 'b' to 'б', 'y' to 'у', 'x' to 'х', 'c' to 'с',
        'o' to 'о', 'a' to 'а', 'e' to 'е', 'k' to 'к', 'h' to 'н',
        'g' to 'д', 'z' to 'з', 'r' to 'г', 'l' to 'л',
        'N' to 'П', 'P' to 'Р', 'U' to 'И', 'V' to 'В', 'M' to 'Т',
        'W' to 'Ш', 'B' to 'В', 'Y' to 'У', 'X' to 'Х', 'C' to 'С',
        'O' to 'О', 'A' to 'А', 'E' to 'Е', 'K' to 'К', 'H' to 'Н',
        'G' to 'Д', 'Z' to 'З', 'R' to 'Г', 'L' to 'Л'
    )

    fun hasCyrillic(text: String): Boolean {
        return text.any { it in '\u0400'..'\u04FF' }
    }

    fun isLikelyVisualCyrillic(text: String): Boolean {
        if (hasCyrillic(text)) return false
        val words = text.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.isEmpty()) return false

        var lookalikeCount = 0
        for (w in words) {
            val lower = w.lowercase()
            if (lower.contains("np") || lower.contains("pu") || lower.contains("uv") ||
                lower.contains("em") || lower.contains("ha") || lower.contains("oi") ||
                lower.contains("ck") || lower.contains("ob") || lower.contains("we") ||
                lower.contains("ue") || lower.contains("hp") || lower.contains("uo") ||
                lower.contains("npo") || lower.contains("npu") || lower.contains("mae") ||
                lower.contains("mo") || lower.contains("me") || lower.contains("uu")
            ) {
                lookalikeCount++
            }
        }
        return lookalikeCount > 0
    }

    fun restoreCyrillic(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(visualMap[ch] ?: ch)
        }
        return sb.toString()
    }
}

object OcrTextPostProcessor {
    private val latinToCyrillicMap = mapOf(
        'a' to 'а', 'A' to 'А',
        'B' to 'В',
        'c' to 'с', 'C' to 'С',
        'e' to 'е', 'E' to 'Е',
        'H' to 'Н',
        'K' to 'К',
        'M' to 'М',
        'o' to 'о', 'O' to 'О',
        'p' to 'р', 'P' to 'Р',
        'T' to 'Т',
        'x' to 'х', 'X' to 'Х',
        'y' to 'у', 'Y' to 'У'
    )

    fun cleanOcrText(rawText: String): String {
        val words = rawText.split("\\s+".toRegex())
        val cleanedWords = words.map { word ->
            fixWord(word)
        }
        return cleanedWords.joinToString(" ")
    }

    private fun fixWord(word: String): String {
        if (word.length <= 1) return word

        val cyrillicCount = word.count { it in 'а'..'я' || it in 'А'..'Я' }
        val latinCount = word.count { it in 'a'..'z' || it in 'A'..'Z' }

        var fixed = word

        if (cyrillicCount > latinCount && latinCount > 0) {
            val sb = StringBuilder()
            for (ch in fixed) {
                sb.append(latinToCyrillicMap[ch] ?: ch)
            }
            fixed = sb.toString()
        } else if (cyrillicCount == 0 && latinCount > 0) {
            if (CyrillicVisualRestorer.isLikelyVisualCyrillic(fixed)) {
                fixed = CyrillicVisualRestorer.restoreCyrillic(fixed)
            }
        }

        if (!fixed.all { it.isUpperCase() }) {
            val first = fixed.first()
            val rest = fixed.substring(1).lowercase()
            fixed = first + rest
        }

        return fixed
    }
}

class ScreenOcrHelper(private val context: Context? = null) {
    private val mlKitRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var tessApi: TessBaseAPI? = null
    private var isTessInitialized = false

    private fun isGmsAvailable(ctx: Context): Boolean {
        return try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            availability.isGooglePlayServicesAvailable(ctx) == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            true
        }
    }

    private fun initTesseract(ctx: Context) {
        if (isTessInitialized) return
        try {
            val tessDir = java.io.File(ctx.filesDir, "tesseract")
            val tessDataDir = java.io.File(tessDir, "tessdata")
            if (!tessDataDir.exists()) tessDataDir.mkdirs()

            val rusFile = java.io.File(tessDataDir, "rus.traineddata")
            if (!rusFile.exists()) {
                try {
                    ctx.assets.open("tessdata/rus.traineddata").use { input ->
                        java.io.FileOutputStream(rusFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {}
            }

            val api = TessBaseAPI()
            if (rusFile.exists()) {
                api.init(tessDir.absolutePath, "rus+eng")
            } else {
                api.init(tessDir.absolutePath, "eng")
            }
            tessApi = api
            isTessInitialized = true
        } catch (e: Exception) {
            Log.e("ScreenOcrHelper", "Tesseract init failed: ${e.message}")
        }
    }

    fun recognizeBitmap(bitmap: Bitmap, onResult: (List<ScreenTextItem>) -> Unit) {
        try {
            val preparedBitmap = ImagePreProcessor.prepareBitmapForOcr(bitmap)
            val hasGms = context?.let { isGmsAvailable(it) } ?: true

            if (hasGms) {
                runMlKitWithFallback(preparedBitmap, onResult)
            } else {
                runTesseractFallback(preparedBitmap, onResult)
            }
        } catch (e: Throwable) {
            Log.e("ScreenOcrHelper", "recognizeBitmap error: ${e.message}", e)
            onResult(emptyList())
        }
    }

    private fun runMlKitWithFallback(bitmap: Bitmap, onResult: (List<ScreenTextItem>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        mlKitRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val items = mutableListOf<ScreenTextItem>()
                var counter = 0
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val rawText = line.text.trim()
                        val box = line.boundingBox
                        if (rawText.isNotEmpty() && box != null) {
                            val restored = if (!CyrillicVisualRestorer.hasCyrillic(rawText) && CyrillicVisualRestorer.isLikelyVisualCyrillic(rawText)) {
                                CyrillicVisualRestorer.restoreCyrillic(rawText)
                            } else {
                                rawText
                            }
                            val cleanedText = OcrTextPostProcessor.cleanOcrText(restored)
                            items.add(ScreenTextItem(id = "item_${counter++}", text = cleanedText, boundingBox = box))
                        }
                    }
                }
                if (items.isNotEmpty()) {
                    onResult(items)
                } else {
                    runTesseractFallback(bitmap, onResult)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ScreenOcrHelper", "ML Kit OCR Error: ${e.message}, falling back...", e)
                runTesseractFallback(bitmap, onResult)
            }
    }

    private fun runTesseractFallback(bitmap: Bitmap, onResult: (List<ScreenTextItem>) -> Unit) {
        val ctx = context
        if (ctx == null) {
            onResult(emptyList())
            return
        }
        try {
            initTesseract(ctx)
            val api = tessApi
            if (api != null) {
                api.setImage(bitmap)
                val recognizedText = api.utF8Text
                api.clear()

                if (!recognizedText.isNullOrBlank()) {
                    val lines = recognizedText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                    val items = lines.mapIndexed { idx, lineText ->
                        val cleaned = OcrTextPostProcessor.cleanOcrText(lineText)
                        val top = (bitmap.height * (idx + 1) / (lines.size + 1)) - 20
                        val bottom = top + 40
                        ScreenTextItem(
                            id = "tess_$idx",
                            text = cleaned,
                            boundingBox = Rect(10, top.coerceAtLeast(0), bitmap.width - 10, bottom.coerceAtMost(bitmap.height))
                        )
                    }
                    onResult(items)
                    return
                }
            }
        } catch (e: Exception) {
            Log.e("ScreenOcrHelper", "Tesseract fallback failed: ${e.message}")
        }
        onResult(emptyList())
    }
}

class ScreenTranslatorHelper {
    private var currentTranslator: Translator? = null
    private val languageIdentifier = LanguageIdentification.getClient()

    fun translateWithAutoDetect(
        textToTranslate: String,
        targetLangMlKitCode: String,
        onResult: (detectedSourceLang: String?, translatedText: String) -> Unit
    ) {
        languageIdentifier.identifyLanguage(textToTranslate)
            .addOnSuccessListener { languageCode ->
                val sourceLang = if (languageCode != "und" && languageCode.isNotEmpty()) {
                    TranslateLanguage.fromLanguageTag(languageCode) ?: TranslateLanguage.ENGLISH
                } else {
                    TranslateLanguage.ENGLISH
                }

                val detectedTag = if (languageCode != "und") languageCode.uppercase() else null

                if (sourceLang == targetLangMlKitCode) {
                    onResult(detectedTag, textToTranslate)
                    return@addOnSuccessListener
                }

                translateText(textToTranslate, sourceLang, targetLangMlKitCode) { translated ->
                    onResult(detectedTag, translated)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ScreenTranslatorHelper", "Language ID failed: ${e.message}", e)
                translateText(textToTranslate, TranslateLanguage.ENGLISH, targetLangMlKitCode) { translated ->
                    onResult(null, translated)
                }
            }
    }

    private fun translateText(
        textToTranslate: String,
        sourceLang: String,
        targetLang: String,
        onTranslated: (String) -> Unit
    ) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()

        currentTranslator?.close()
        val translator = Translation.getClient(options)
        currentTranslator = translator

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(textToTranslate)
                    .addOnSuccessListener { translated ->
                        onTranslated(translated)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ScreenTranslatorHelper", "Translation error: ${e.message}", e)
                        onTranslated("Ошибка перевода")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ScreenTranslatorHelper", "Model download error: ${e.message}", e)
                onTranslated("Не удалось скачать языковой пакет")
            }
    }

    fun close() {
        currentTranslator?.close()
        currentTranslator = null
        languageIdentifier.close()
    }
}

fun cropBitmap(original: Bitmap, rect: Rect): Bitmap {
    val left = rect.left.coerceIn(0, original.width - 1)
    val top = rect.top.coerceIn(0, original.height - 1)
    val width = rect.width().coerceAtMost(original.width - left).coerceAtLeast(1)
    val height = rect.height().coerceAtMost(original.height - top).coerceAtLeast(1)
    return Bitmap.createBitmap(original, left, top, width, height)
}

fun searchImageWithGoogleLens(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = java.io.File(context.cacheDir, "images")
        cachePath.mkdirs()
        val tempFile = java.io.File(cachePath, "screen_search_crop_${System.currentTimeMillis()}.png")
        val stream = java.io.FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imageUri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )

        val lensIntent = Intent(Intent.ACTION_VIEW).apply {
            setComponent(
                android.content.ComponentName(
                    "com.google.ar.lens",
                    "com.google.vr.apps.ornament.app.lens.LensLauncherActivity"
                )
            )
            setDataAndType(imageUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(lensIntent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                setPackage("com.google.android.googlequicksearchbox")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(shareIntent)
            } catch (ex: Exception) {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Поиск по картинке"
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    } catch (e: Exception) {
        Log.e("ScreenSearchHelper", "Image search error: ${e.message}", e)
        android.widget.Toast.makeText(context, "Не удалось запустить поиск по картинке", android.widget.Toast.LENGTH_SHORT).show()
    }
}
