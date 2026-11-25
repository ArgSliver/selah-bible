package com.selah.bible.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.selah.bible.R
import com.selah.bible.data.CustomFontManager
import com.selah.bible.data.FontChoice
import com.selah.bible.data.UserSettings

data class SelahPalette(
    val id: String,
    val name: String,
    val subtitle: String,
    val isDark: Boolean,
    val isOledOptimized: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color
)

data class AccentSwatch(
    val id: String,
    val name: String,
    val darkColor: Color?,
    val lightColor: Color?
)

object SelahThemeCatalog {

    val themes: List<SelahPalette> = listOf(
        SelahPalette(
            id = "amoled_black",
            name = "AMOLED True Black",
            subtitle = "Zero-lit OLED pixels • Max battery",
            isDark = true,
            isOledOptimized = true,
            background = Color(0xFF000000),
            surface = Color(0xFF0A0A0A),
            surfaceAlt = Color(0xFF141311),
            border = Color(0xFF23201C),
            textPrimary = Color(0xFFEBE6DC),
            textSecondary = Color(0xFF9B9386),
            accent = Color(0xFFD4A853),
            onAccent = Color(0xFF1A1408)
        ),
        SelahPalette(
            id = "warm_paper",
            name = "Warm Paper",
            subtitle = "Gentle book paper • Soft daylight",
            isDark = false,
            isOledOptimized = false,
            background = Color(0xFFFAF6EE),
            surface = Color(0xFFF3ECE0),
            surfaceAlt = Color(0xFFEAE0CE),
            border = Color(0xFFDED2BE),
            textPrimary = Color(0xFF221E18),
            textSecondary = Color(0xFF6B6051),
            accent = Color(0xFF8C5A2B),
            onAccent = Color(0xFFFFFBF4)
        ),
        SelahPalette(
            id = "sepia_parchment",
            name = "Sepia Parchment",
            subtitle = "Warm archival parchment",
            isDark = false,
            isOledOptimized = false,
            background = Color(0xFFF3E9D2),
            surface = Color(0xFFEADDC0),
            surfaceAlt = Color(0xFFE0CFAF),
            border = Color(0xFFD1BF9A),
            textPrimary = Color(0xFF33281C),
            textSecondary = Color(0xFF73624C),
            accent = Color(0xFF8A4F1E),
            onAccent = Color(0xFFFFF9EF)
        ),
        SelahPalette(
            id = "midnight_oled",
            name = "Midnight OLED",
            subtitle = "True black + cool starlight blue",
            isDark = true,
            isOledOptimized = true,
            background = Color(0xFF000000),
            surface = Color(0xFF080B10),
            surfaceAlt = Color(0xFF111824),
            border = Color(0xFF1E293B),
            textPrimary = Color(0xFFE6EDF6),
            textSecondary = Color(0xFF8B9BB2),
            accent = Color(0xFF6CB4FF),
            onAccent = Color(0xFF051324)
        ),
        SelahPalette(
            id = "forest_night",
            name = "Forest Moss",
            subtitle = "Deep quiet pine & sage",
            isDark = true,
            isOledOptimized = true,
            background = Color(0xFF050B07),
            surface = Color(0xFF0C150F),
            surfaceAlt = Color(0xFF152219),
            border = Color(0xFF213527),
            textPrimary = Color(0xFFE3ECE5),
            textSecondary = Color(0xFF8CA693),
            accent = Color(0xFF78C58E),
            onAccent = Color(0xFF071B0D)
        ),
        SelahPalette(
            id = "slate_graphite",
            name = "Slate Graphite",
            subtitle = "Low-glare matte charcoal",
            isDark = true,
            isOledOptimized = false,
            background = Color(0xFF141619),
            surface = Color(0xFF1C1F24),
            surfaceAlt = Color(0xFF252930),
            border = Color(0xFF323842),
            textPrimary = Color(0xFFEAEDF2),
            textSecondary = Color(0xFF99A2B0),
            accent = Color(0xFFE0A96D),
            onAccent = Color(0xFF1E1409)
        ),
        SelahPalette(
            id = "royal_navy",
            name = "Royal Navy",
            subtitle = "Deep indigo & cathedral gold",
            isDark = true,
            isOledOptimized = false,
            background = Color(0xFF0A1120),
            surface = Color(0xFF111B30),
            surfaceAlt = Color(0xFF1A2742),
            border = Color(0xFF26385C),
            textPrimary = Color(0xFFEDF1F8),
            textSecondary = Color(0xFF95A5C6),
            accent = Color(0xFFEAB308),
            onAccent = Color(0xFF1F1700)
        ),
        SelahPalette(
            id = "rose_dawn",
            name = "Rose Dawn",
            subtitle = "Soft morning blush & wine",
            isDark = false,
            isOledOptimized = false,
            background = Color(0xFFFDF5F5),
            surface = Color(0xFFF6E9EA),
            surfaceAlt = Color(0xFFEDDADC),
            border = Color(0xFFDFC4C7),
            textPrimary = Color(0xFF2D1B1E),
            textSecondary = Color(0xFF77565C),
            accent = Color(0xFF9E3A4D),
            onAccent = Color(0xFFFFF7F8)
        ),
        SelahPalette(
            id = "nordic_frost",
            name = "Nordic Frost",
            subtitle = "Crisp daylight & cobalt ink",
            isDark = false,
            isOledOptimized = false,
            background = Color(0xFFF4F7FB),
            surface = Color(0xFFEAF0F7),
            surfaceAlt = Color(0xFFDFE8F2),
            border = Color(0xFFCAD7E6),
            textPrimary = Color(0xFF172130),
            textSecondary = Color(0xFF566880),
            accent = Color(0xFF2B5EA7),
            onAccent = Color(0xFFF7FAFF)
        ),
        SelahPalette(
            id = "high_contrast",
            name = "High Contrast",
            subtitle = "Pure black & crisp white • Accessible",
            isDark = true,
            isOledOptimized = true,
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceAlt = Color(0xFF121212),
            border = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFD6D6D6),
            accent = Color(0xFFFFE600),
            onAccent = Color(0xFF000000)
        )
    )

    val accents: List<AccentSwatch> = listOf(
        AccentSwatch("default", "Theme Default", null, null),
        AccentSwatch("warm_gold", "Warm Gold", Color(0xFFD4A853), Color(0xFF9A6B1A)),
        AccentSwatch("amber", "Copper Amber", Color(0xFFEB9852), Color(0xFFB55A16)),
        AccentSwatch("sage", "Olive Sage", Color(0xFF7CC892), Color(0xFF2E7D4F)),
        AccentSwatch("ocean", "Sapphire", Color(0xFF6BB5FF), Color(0xFF1E63B5)),
        AccentSwatch("crimson", "Burgundy", Color(0xFFE87A8A), Color(0xFFA12B3F)),
        AccentSwatch("lavender", "Soft Iris", Color(0xFFB89CFF), Color(0xFF6342B8)),
        AccentSwatch("monochrome", "Monochrome", Color(0xFFE6E6E6), Color(0xFF262626))
    )

    val highlightKeys = listOf("gold", "sage", "sky", "rose", "lavender")

    fun highlightColor(key: String?, isDark: Boolean): Color {
        if (key == null) return Color.Transparent
        return if (isDark) {
            when (key) {
                "gold" -> Color(0x3DD4A853)
                "sage" -> Color(0x385CB874)
                "sky" -> Color(0x385AA9F5)
                "rose" -> Color(0x38E56B7F)
                "lavender" -> Color(0x38A78BFA)
                else -> Color.Transparent
            }
        } else {
            when (key) {
                "gold" -> Color(0x55F2C94C)
                "sage" -> Color(0x4D7AC78E)
                "sky" -> Color(0x4D7CC0FF)
                "rose" -> Color(0x4DF299A7)
                "lavender" -> Color(0x4DC4B0FF)
                else -> Color.Transparent
            }
        }
    }

    fun highlightSwatchColor(key: String): Color = when (key) {
        "gold" -> Color(0xFFD4A853)
        "sage" -> Color(0xFF65B97C)
        "sky" -> Color(0xFF5FA8F0)
        "rose" -> Color(0xFFE07587)
        "lavender" -> Color(0xFFA98DF5)
        else -> Color.Gray
    }

    fun resolvePalette(settings: UserSettings, systemDark: Boolean): SelahPalette {
        val targetId = if (settings.followSystemTheme) {
            if (systemDark) settings.preferredDarkThemeId else settings.preferredLightThemeId
        } else {
            settings.themeId
        }
        val base = themes.find { it.id == targetId } ?: themes.first()
        val override = accents.find { it.id == settings.accentOverrideId }
        val customAccent = if (base.isDark) override?.darkColor else override?.lightColor
        return if (customAccent != null) {
            base.copy(
                accent = customAccent,
                onAccent = if (base.isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
            )
        } else {
            base
        }
    }

    val openDyslexicFamily: FontFamily by lazy {
        FontFamily(
            Font(R.font.opendyslexic_regular, FontWeight.Normal),
            Font(R.font.opendyslexic_bold, FontWeight.Bold)
        )
    }

    fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
        FontChoice.SERIF -> FontFamily.Serif
        FontChoice.SANS -> FontFamily.SansSerif
        FontChoice.OPEN_DYSLEXIC -> openDyslexicFamily
        FontChoice.MONO -> FontFamily.Monospace
        FontChoice.CURSIVE -> FontFamily.Cursive
    }

    @Composable
    fun rememberActiveFontFamily(settings: UserSettings): FontFamily {
        val context = LocalContext.current
        return remember(settings.fontChoice, settings.customFontFileName) {
            if (!settings.customFontFileName.isNullOrBlank()) {
                CustomFontManager.loadComposeFontFamily(context, settings.customFontFileName)
                    ?: fontFamilyFor(settings.fontChoice)
            } else {
                fontFamilyFor(settings.fontChoice)
            }
        }
    }
}

val LocalSelahPalette = staticCompositionLocalOf { SelahThemeCatalog.themes.first() }

@Composable
fun SelahTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val palette = SelahThemeCatalog.resolvePalette(settings, systemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.background.toArgb()
                window.navigationBarColor = palette.surface.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !palette.isDark
                controller.isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border
        )
    }

    CompositionLocalProvider(LocalSelahPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
