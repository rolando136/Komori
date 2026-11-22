package com.rolando.komori.widget

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils

object WidgetWallpaper {
    fun isDark(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val colors = WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) return isDark(colors)
            } catch (_: SecurityException) {
                // Some launchers/OEMs restrict even wallpaper color metadata.
            }
        }
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    internal fun isDark(colors: WallpaperColors): Boolean {
        // Metadata is available without accessing the wallpaper image itself.
        val samples = listOfNotNull(colors.primaryColor, colors.secondaryColor, colors.tertiaryColor)
        val weights = listOf(0.7, 0.2, 0.1).take(samples.size)
        val luminance = samples.zip(weights).sumOf { (color, weight) ->
            ColorUtils.calculateLuminance(color.toArgb()) * weight
        } / weights.sum()
        return luminance < 0.45
    }
}
