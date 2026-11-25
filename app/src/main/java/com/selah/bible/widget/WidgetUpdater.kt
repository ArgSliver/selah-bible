package com.selah.bible.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.selah.bible.MainActivity
import com.selah.bible.R
import com.selah.bible.data.BibleDatabase
import com.selah.bible.data.SettingsStore
import com.selah.bible.data.VotdEngine
import com.selah.bible.data.WidgetThemeChoice

/**
 * Ultra-lightweight, zero-polling RemoteViews updater for both home screen widgets.
 * Runs in < 2ms directly against indexed SQLite without waking Compose or services.
 */
object WidgetUpdater {

    const val ACTION_VOTD_REFRESH = "com.selah.bible.ACTION_VOTD_REFRESH"
    const val ACTION_FAV_NEXT = "com.selah.bible.ACTION_FAV_NEXT"
    const val ACTION_FAV_PREV = "com.selah.bible.ACTION_FAV_PREV"

    const val EXTRA_BOOK_ID = "open_book_id"
    const val EXTRA_CHAPTER = "open_chapter"
    const val EXTRA_VERSE = "open_verse"

    private data class WidgetPalette(
        val bgRes: Int,
        val textColor: Int,
        val mutedColor: Int,
        val accentColor: Int
    )

    private fun resolvePalette(choice: WidgetThemeChoice): WidgetPalette = when (choice) {
        WidgetThemeChoice.AMOLED -> WidgetPalette(
            bgRes = R.drawable.widget_bg_amoled,
            textColor = Color.parseColor("#FFE8E3D8"),
            mutedColor = Color.parseColor("#FF9B9387"),
            accentColor = Color.parseColor("#FFD4A853")
        )
        WidgetThemeChoice.PAPER -> WidgetPalette(
            bgRes = R.drawable.widget_bg_paper,
            textColor = Color.parseColor("#FF231F1A"),
            mutedColor = Color.parseColor("#FF6E6254"),
            accentColor = Color.parseColor("#FF8C5A2B")
        )
        WidgetThemeChoice.SEPIA -> WidgetPalette(
            bgRes = R.drawable.widget_bg_sepia,
            textColor = Color.parseColor("#FF33291E"),
            mutedColor = Color.parseColor("#FF786852"),
            accentColor = Color.parseColor("#FF8A5524")
        )
        WidgetThemeChoice.SLATE -> WidgetPalette(
            bgRes = R.drawable.widget_bg_slate,
            textColor = Color.parseColor("#FFE6EAF0"),
            mutedColor = Color.parseColor("#FF97A1B0"),
            accentColor = Color.parseColor("#FF78B4F5")
        )
        WidgetThemeChoice.TRANSLUCENT -> WidgetPalette(
            bgRes = R.drawable.widget_bg_translucent,
            textColor = Color.parseColor("#FFF5F2EC"),
            mutedColor = Color.parseColor("#FFC8C2B8"),
            accentColor = Color.parseColor("#FFE6BE6A")
        )
    }

    fun updateAllWidgets(context: Context, forceNewVotd: Boolean = false) {
        updateVotdWidgets(context, forceNew = forceNewVotd)
        updateFavoritesWidgets(context, delta = 0)
    }

    fun updateVotdWidgets(context: Context, forceNew: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, VotdWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val db = BibleDatabase.getInstance(context)
        val settingsStore = SettingsStore(context)
        val settings = settingsStore.current()
        val votd = VotdEngine(db, settingsStore).getTodayVotd(forceNew = forceNew)
        val streak = db.getStreakStats()
        val palette = resolvePalette(settings.widgetTheme)

        for (appWidgetId in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_votd)
            views.setInt(R.id.widget_votd_root, "setBackgroundResource", palette.bgRes)
            views.setTextColor(R.id.widget_votd_label, palette.accentColor)
            views.setTextColor(R.id.widget_votd_text, palette.textColor)
            views.setTextColor(R.id.widget_votd_ref, palette.mutedColor)
            views.setTextColor(R.id.widget_votd_streak, palette.mutedColor)
            views.setTextColor(R.id.widget_votd_refresh, palette.accentColor)
            views.setTextViewTextSize(R.id.widget_votd_text, TypedValue.COMPLEX_UNIT_SP, settings.widgetFontSize.sp)

            views.setTextViewText(R.id.widget_votd_text, "“${votd.text}”")
            views.setTextViewText(R.id.widget_votd_ref, "${votd.reference()} • ${votd.translationCode}")

            if (settings.widgetShowStreak) {
                views.setViewVisibility(R.id.widget_votd_streak, View.VISIBLE)
                val badge = if (streak.currentStreak > 0) "🔥 ${streak.currentStreak}d" else "0d streak"
                views.setTextViewText(R.id.widget_votd_streak, badge)
            } else {
                views.setViewVisibility(R.id.widget_votd_streak, View.GONE)
            }

            // Tap verse -> open exact chapter/verse in Selah
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_BOOK_ID, votd.bookId)
                putExtra(EXTRA_CHAPTER, votd.chapter)
                putExtra(EXTRA_VERSE, votd.startVerse)
            }
            val openPi = PendingIntent.getActivity(
                context,
                1000 + appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_votd_root, openPi)

            // Tap refresh icon -> draw another coherent non-repeating verse
            val refreshIntent = Intent(context, VotdWidgetProvider::class.java).apply {
                action = ACTION_VOTD_REFRESH
            }
            val refreshPi = PendingIntent.getBroadcast(
                context,
                2000 + appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_votd_refresh, refreshPi)

            manager.updateAppWidget(appWidgetId, views)
        }
    }

    fun updateFavoritesWidgets(context: Context, delta: Int = 0) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, FavoritesWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val db = BibleDatabase.getInstance(context)
        val settingsStore = SettingsStore(context)
        val settings = settingsStore.current()
        val palette = resolvePalette(settings.widgetTheme)
        val favorites = db.getFavorites()

        val activeTranslation = db.getTranslation(settings.activeTranslationId)
        val activeCode = activeTranslation?.code ?: "KJV"

        val idx = if (favorites.isEmpty()) {
            0
        } else {
            val current = settingsStore.getFavoritesWidgetIndex()
            val next = ((current + delta) % favorites.size + favorites.size) % favorites.size
            if (delta != 0) settingsStore.setFavoritesWidgetIndex(next)
            next
        }

        for (appWidgetId in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_favorites)
            views.setInt(R.id.widget_fav_root, "setBackgroundResource", palette.bgRes)
            views.setTextColor(R.id.widget_fav_label, palette.accentColor)
            views.setTextColor(R.id.widget_fav_text, palette.textColor)
            views.setTextColor(R.id.widget_fav_ref, palette.mutedColor)
            views.setTextColor(R.id.widget_fav_counter, palette.mutedColor)
            views.setTextColor(R.id.widget_fav_prev, palette.accentColor)
            views.setTextColor(R.id.widget_fav_next, palette.accentColor)
            views.setTextViewTextSize(R.id.widget_fav_text, TypedValue.COMPLEX_UNIT_SP, settings.widgetFontSize.sp)

            if (favorites.isEmpty()) {
                views.setTextViewText(
                    R.id.widget_fav_text,
                    "Tap any verse in Selah to save it as a favorite and display it on your home screen."
                )
                views.setTextViewText(R.id.widget_fav_ref, "No favorites yet")
                views.setTextViewText(R.id.widget_fav_counter, "0/0")
                views.setViewVisibility(R.id.widget_fav_prev, View.GONE)
                views.setViewVisibility(R.id.widget_fav_next, View.GONE)

                val openApp = PendingIntent.getActivity(
                    context,
                    3000 + appWidgetId,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_fav_root, openApp)
            } else {
                val fav = favorites[idx]
                // Render in user's active translation if available, else fallback to snapshot
                val liveText = db.getPassageText(
                    settings.activeTranslationId,
                    fav.bookId,
                    fav.chapter,
                    fav.verse,
                    fav.verse
                ).ifBlank { fav.textSnapshot }

                views.setTextViewText(R.id.widget_fav_text, "“$liveText”")
                views.setTextViewText(R.id.widget_fav_ref, "${fav.reference()} • $activeCode")
                views.setTextViewText(R.id.widget_fav_counter, "${idx + 1}/${favorites.size}")
                val showArrows = if (favorites.size > 1) View.VISIBLE else View.GONE
                views.setViewVisibility(R.id.widget_fav_prev, showArrows)
                views.setViewVisibility(R.id.widget_fav_next, showArrows)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_BOOK_ID, fav.bookId)
                    putExtra(EXTRA_CHAPTER, fav.chapter)
                    putExtra(EXTRA_VERSE, fav.verse)
                }
                val openPi = PendingIntent.getActivity(
                    context,
                    3000 + appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_fav_root, openPi)

                val prevPi = PendingIntent.getBroadcast(
                    context,
                    4000 + appWidgetId,
                    Intent(context, FavoritesWidgetProvider::class.java).apply { action = ACTION_FAV_PREV },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val nextPi = PendingIntent.getBroadcast(
                    context,
                    5000 + appWidgetId,
                    Intent(context, FavoritesWidgetProvider::class.java).apply { action = ACTION_FAV_NEXT },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_fav_prev, prevPi)
                views.setOnClickPendingIntent(R.id.widget_fav_next, nextPi)
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
