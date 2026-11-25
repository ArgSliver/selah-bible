package com.selah.bible.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

enum class ReadingLayoutMode { VERSE_BY_VERSE, PARAGRAPH }
enum class FontChoice(val id: String, val label: String) {
    SERIF("serif", "Serif (Book)"),
    SANS("sans", "Sans-Serif (Clean)"),
    OPEN_DYSLEXIC("opendyslexic", "OpenDyslexic"),
    MONO("mono", "Monospace (Study)"),
    CURSIVE("cursive", "Editorial Script");

    companion object {
        fun fromId(id: String): FontChoice = entries.find { it.id == id } ?: SERIF
    }
}

enum class WidgetThemeChoice(val id: String, val label: String) {
    AMOLED("amoled", "AMOLED True Black"),
    PAPER("paper", "Warm Paper"),
    SEPIA("sepia", "Sepia Parchment"),
    SLATE("slate", "Slate Graphite"),
    TRANSLUCENT("translucent", "Translucent Dark");

    companion object {
        fun fromId(id: String): WidgetThemeChoice = entries.find { it.id == id } ?: AMOLED
    }
}

enum class WidgetFontSize(val id: String, val label: String, val sp: Float) {
    SMALL("small", "Compact", 12.5f),
    MEDIUM("medium", "Balanced", 14.5f),
    LARGE("large", "Large", 17f);

    companion object {
        fun fromId(id: String): WidgetFontSize = entries.find { it.id == id } ?: MEDIUM
    }
}

data class UserSettings(
    val activeTranslationId: String = StarterKjvData.TRANSLATION_ID,
    val lastBookId: Int = 19, // Psalms
    val lastChapter: Int = 23,
    val activePlanId: String = "canonical_365",
    val planStartEpochDay: Long = LocalDate.now().toEpochDay(),
    // Appearance & Customization
    val themeId: String = "amoled_black",
    val followSystemTheme: Boolean = false,
    val preferredLightThemeId: String = "warm_paper",
    val preferredDarkThemeId: String = "amoled_black",
    val accentOverrideId: String = "default",
    val fontChoice: FontChoice = FontChoice.SERIF,
    val customFontFileName: String? = null,
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.55f,
    val letterSpacingEm: Float = 0.0f,
    val verseSpacingDp: Int = 10,
    val layoutMode: ReadingLayoutMode = ReadingLayoutMode.VERSE_BY_VERSE,
    val showVerseNumbers: Boolean = true,
    val keepScreenOn: Boolean = false,
    val wideMargins: Boolean = false,
    // Widget Customization
    val widgetTheme: WidgetThemeChoice = WidgetThemeChoice.AMOLED,
    val widgetFontSize: WidgetFontSize = WidgetFontSize.MEDIUM,
    val widgetShowStreak: Boolean = true,
    val favoritesWidgetIndex: Int = 0
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("selah_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<UserSettings> = _state.asStateFlow()

    fun current(): UserSettings = _state.value
    fun activeTranslationId(): String = _state.value.activeTranslationId

    private fun load(): UserSettings {
        val today = LocalDate.now().toEpochDay()
        val rawTrId = prefs.getString("active_translation", StarterKjvData.TRANSLATION_ID)
            ?: StarterKjvData.TRANSLATION_ID
        val normalizedTrId = if (rawTrId == "kjv_starter" || rawTrId == "kjv_full") {
            StarterKjvData.TRANSLATION_ID
        } else {
            rawTrId
        }
        return UserSettings(
            activeTranslationId = normalizedTrId,
            lastBookId = prefs.getInt("last_book_id", 19),
            lastChapter = prefs.getInt("last_chapter", 23),
            activePlanId = prefs.getString("active_plan_id", "canonical_365") ?: "canonical_365",
            planStartEpochDay = prefs.getLong("plan_start_day", today),
            themeId = prefs.getString("theme_id", "amoled_black") ?: "amoled_black",
            followSystemTheme = prefs.getBoolean("follow_system_theme", false),
            preferredLightThemeId = prefs.getString("preferred_light_theme", "warm_paper") ?: "warm_paper",
            preferredDarkThemeId = prefs.getString("preferred_dark_theme", "amoled_black") ?: "amoled_black",
            accentOverrideId = prefs.getString("accent_override", "default") ?: "default",
            fontChoice = FontChoice.fromId(prefs.getString("font_choice", "serif") ?: "serif"),
            customFontFileName = prefs.getString("custom_font_file", null)?.takeIf { it.isNotBlank() },
            fontSizeSp = prefs.getFloat("font_size_sp", 18f),
            lineHeightMultiplier = prefs.getFloat("line_height", 1.55f),
            letterSpacingEm = prefs.getFloat("letter_spacing", 0.0f),
            verseSpacingDp = prefs.getInt("verse_spacing_dp", 10),
            layoutMode = if (prefs.getString("layout_mode", "verse") == "paragraph") {
                ReadingLayoutMode.PARAGRAPH
            } else {
                ReadingLayoutMode.VERSE_BY_VERSE
            },
            showVerseNumbers = prefs.getBoolean("show_verse_numbers", true),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false),
            wideMargins = prefs.getBoolean("wide_margins", false),
            widgetTheme = WidgetThemeChoice.fromId(prefs.getString("widget_theme", "amoled") ?: "amoled"),
            widgetFontSize = WidgetFontSize.fromId(prefs.getString("widget_font_size", "medium") ?: "medium"),
            widgetShowStreak = prefs.getBoolean("widget_show_streak", true),
            favoritesWidgetIndex = prefs.getInt("fav_widget_index", 0)
        )
    }

    fun update(transform: (UserSettings) -> UserSettings) {
        val updated = transform(_state.value)
        _state.value = updated
        prefs.edit().apply {
            putString("active_translation", updated.activeTranslationId)
            putInt("last_book_id", updated.lastBookId)
            putInt("last_chapter", updated.lastChapter)
            putString("active_plan_id", updated.activePlanId)
            putLong("plan_start_day", updated.planStartEpochDay)
            putString("theme_id", updated.themeId)
            putBoolean("follow_system_theme", updated.followSystemTheme)
            putString("preferred_light_theme", updated.preferredLightThemeId)
            putString("preferred_dark_theme", updated.preferredDarkThemeId)
            putString("accent_override", updated.accentOverrideId)
            putString("font_choice", updated.fontChoice.id)
            if (updated.customFontFileName.isNullOrBlank()) {
                remove("custom_font_file")
            } else {
                putString("custom_font_file", updated.customFontFileName)
            }
            putFloat("font_size_sp", updated.fontSizeSp)
            putFloat("line_height", updated.lineHeightMultiplier)
            putFloat("letter_spacing", updated.letterSpacingEm)
            putInt("verse_spacing_dp", updated.verseSpacingDp)
            putString("layout_mode", if (updated.layoutMode == ReadingLayoutMode.PARAGRAPH) "paragraph" else "verse")
            putBoolean("show_verse_numbers", updated.showVerseNumbers)
            putBoolean("keep_screen_on", updated.keepScreenOn)
            putBoolean("wide_margins", updated.wideMargins)
            putString("widget_theme", updated.widgetTheme.id)
            putString("widget_font_size", updated.widgetFontSize.id)
            putBoolean("widget_show_streak", updated.widgetShowStreak)
            putInt("fav_widget_index", updated.favoritesWidgetIndex)
            apply()
        }
    }

    fun getFavoritesWidgetIndex(): Int = prefs.getInt("fav_widget_index", 0)

    fun setFavoritesWidgetIndex(idx: Int) {
        prefs.edit().putInt("fav_widget_index", idx).apply()
        _state.value = _state.value.copy(favoritesWidgetIndex = idx)
    }
}
