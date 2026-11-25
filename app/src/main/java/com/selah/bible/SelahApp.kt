package com.selah.bible

import android.app.Application
import com.selah.bible.data.BibleDatabase
import com.selah.bible.data.SettingsStore

class SelahApp : Application() {
    val database: BibleDatabase by lazy { BibleDatabase.getInstance(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
}
