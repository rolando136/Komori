package com.rolando.komori.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Random

val LocalAppTheme = staticCompositionLocalOf { AppThemeOption.Komori }
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private var cachedLightBrush: ShaderBrush? = null
private var cachedDarkBrush: ShaderBrush? = null

/**
 * Generates or returns a cached seamless 256x256 paper grain ShaderBrush.
 * Utilizes multi-frequency micro-speckles and organic cotton/washi paper fibers.
 */
fun getPaperGrainBrush(isDark: Boolean): ShaderBrush {
    return if (isDark) {
        cachedDarkBrush ?: createPaperGrainBrush(isDark = true).also { cachedDarkBrush = it }
    } else {
        cachedLightBrush ?: createPaperGrainBrush(isDark = false).also { cachedLightBrush = it }
    }
}

private fun createPaperGrainBrush(isDark: Boolean): ShaderBrush {
    val size = 256
    val pixels = IntArray(size * size)
    val random = Random(1337L) // Deterministic seed for visual stability

    // 1. Base micro-grain speckles
    for (i in 0 until (size * size)) {
        val roll = random.nextInt(100)
        pixels[i] = if (!isDark) {
            when {
                // Warm sepia / umber tooth fleck
                roll < 14 -> {
                    val alpha = 8 + random.nextInt(14) // ~3% to 8%
                    (alpha shl 24) or (0x5A shl 16) or (0x45 shl 8) or 0x32
                }
                // Soft ivory highlight fleck
                roll in 14..25 -> {
                    val alpha = 10 + random.nextInt(14) // ~4% to 9%
                    (alpha shl 24) or (0xFF shl 16) or (0xFD shl 8) or 0xF5
                }
                else -> 0x00000000
            }
        } else {
            when {
                // Warm stone parchment fleck
                roll < 14 -> {
                    val alpha = 8 + random.nextInt(12) // ~3% to 7%
                    (alpha shl 24) or (0xD6 shl 16) or (0xCA shl 8) or 0xB8
                }
                // Antique book deep charcoal fleck
                roll in 14..25 -> {
                    val alpha = 12 + random.nextInt(14) // ~4% to 10%
                    (alpha shl 24) or (0x0A shl 16) or (0x09 shl 8) or 0x07
                }
                else -> 0x00000000
            }
        }
    }

    // 2. Seamless organic fibers (washi / cotton pulp strands)
    val fiberCount = 160
    for (f in 0 until fiberCount) {
        val startX = random.nextInt(size)
        val startY = random.nextInt(size)
        val length = 2 + random.nextInt(4) // 2 to 5 pixels
        val dir = random.nextInt(8)
        val dx = when (dir) {
            0, 1, 7 -> 1
            3, 4, 5 -> -1
            else -> 0
        }
        val dy = when (dir) {
            1, 2, 3 -> 1
            5, 6, 7 -> -1
            else -> 0
        }

        val fiberAlpha = if (!isDark) 14 + random.nextInt(16) else 10 + random.nextInt(14)
        val fiberColor = if (!isDark) {
            (fiberAlpha shl 24) or (0x68 shl 16) or (0x53 shl 8) or 0x3E
        } else {
            (fiberAlpha shl 24) or (0xC7 shl 16) or (0xBA shl 8) or 0xA8
        }

        var cx = startX
        var cy = startY
        for (step in 0 until length) {
            val px = ((cx % size) + size) % size
            val py = ((cy % size) + size) % size
            val idx = py * size + px
            pixels[idx] = fiberColor
            cx += dx
            cy += dy
        }
    }

    val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    val imageBitmap = bitmap.asImageBitmap()
    val shader = ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated)
    return ShaderBrush(shader)
}

/**
 * Modifier that overlays an authentic paper grain texture when the Paper theme is active.
 * Passes through untouched for all other themes.
 */
@Composable
fun Modifier.paperGrain(): Modifier {
    val theme = LocalAppTheme.current
    val isDark = LocalIsDarkTheme.current
    if (theme != AppThemeOption.Paper) return this
    val brush = remember(isDark) { getPaperGrainBrush(isDark) }
    return this.drawWithContent {
        drawContent()
        drawRect(brush = brush)
    }
}
