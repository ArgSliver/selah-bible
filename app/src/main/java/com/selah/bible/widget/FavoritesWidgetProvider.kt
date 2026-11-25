package com.selah.bible.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class FavoritesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.updateFavoritesWidgets(context, delta = 0)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetUpdater.ACTION_FAV_NEXT -> WidgetUpdater.updateFavoritesWidgets(context, delta = 1)
            WidgetUpdater.ACTION_FAV_PREV -> WidgetUpdater.updateFavoritesWidgets(context, delta = -1)
        }
    }
}
