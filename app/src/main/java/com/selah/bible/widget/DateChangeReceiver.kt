package com.selah.bible.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Passive broadcast receiver triggered by Android OS on calendar day change.
 * Updates both widgets at midnight with ZERO background polling or exact alarms.
 */
class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            WidgetUpdater.updateVotdWidgets(context, forceNew = false)
            // Rotate favorite verse once on day change if multiple favorites exist
            WidgetUpdater.updateFavoritesWidgets(context, delta = 1)
        }
    }
}
