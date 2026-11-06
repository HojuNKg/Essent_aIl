package com.HojuNK.essent_all.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

data class GlyphLed(
    val id: Int,
    val name: String,
    val zone: Int,
    val pos: Offset // Нормализованные координаты (0.0 - 1.0)
)

object GlyphLayout {
    val leds: List<GlyphLed> by lazy {
        val list = mutableListOf<GlyphLed>()

        val cx = 0.5f
        val cy = 0.45f
        val verticalShift = 0.04f

        // Zone 1 (A1 - A11): Индексы 20 - 30. Правая дуга.
        val rA = 0.36f
        for (i in 0 until 11) {
            val baseRotation = 15.0
            val startAngle = -38.0 + baseRotation
            val endAngle = 38.0 + baseRotation
            val angle = startAngle + (i * (endAngle - startAngle) / 10.0)
            val x = cx + 0.08f + rA * cos(Math.toRadians(angle)).toFloat() * 0.9f
            val y = cy + verticalShift + rA * sin(Math.toRadians(angle)).toFloat()
            list.add(GlyphLed(i + 20, "A${i + 1}", 1, Offset(x, y)))
        }

        // Zone 3 (C1 - C20): Индексы 0 - 19. Верхняя левая дуга.
        // Повернута на -45 градусов (против часовой стрелки)
        val rC = 0.38f
        for (i in 0 until 20) {
            val rotationC = -15.0
            val startAngle = 202.5 + rotationC
            val endAngle = 262.5 + rotationC
            val angle = startAngle + (i * (endAngle - startAngle) / 19.0)
            val x = cx - 0.00f + rC * cos(Math.toRadians(angle)).toFloat()
            val y = cy + 0.03f + rC * sin(Math.toRadians(angle)).toFloat()
            list.add(GlyphLed(i, "C${i + 1}", 3, Offset(x, y)))
        }

        // Zone 2 (B1 - B5): Индексы 31 - 35. Нижняя левая полоска.
        val midB = Offset(0.2375f - 0.03f, 0.70f + verticalShift + 0.03f)
        val halfLen = 0.07f
        val angleB = Math.toRadians(50.0)
        
        val startX = midB.x + halfLen * cos(angleB).toFloat()
        val startY = midB.y + halfLen * sin(angleB).toFloat()
        val endX = midB.x - halfLen * cos(angleB).toFloat()
        val endY = midB.y - halfLen * sin(angleB).toFloat()

        for (i in 0 until 5) {
            val progress = i / 4.0f
            val x = startX + (endX - startX) * progress
            val y = startY + (endY - startY) * progress
            list.add(GlyphLed(i + 31, "B${i + 1}", 2, Offset(x, y)))
        }

        list.sortedBy { it.id }
    }
}
