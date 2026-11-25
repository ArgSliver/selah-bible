package com.selah.bible

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.selah.bible.data.BackupManager
import com.selah.bible.data.BibleDatabase
import com.selah.bible.data.BibleVerse
import com.selah.bible.data.CustomFontManager
import com.selah.bible.data.ImportProgress
import com.selah.bible.data.ReadingPlanCatalog
import com.selah.bible.data.SettingsStore
import com.selah.bible.data.StarterKjvData
import com.selah.bible.data.Translation
import com.selah.bible.data.VotdEngine
import com.selah.bible.data.importer.BibleTextParser
import com.selah.bible.data.importer.EpubImporter
import com.selah.bible.data.importer.PdfImporter
import com.selah.bible.data.importer.SqliteImporter
import com.selah.bible.ui.screens.HomeScreen
import com.selah.bible.ui.screens.PlansScreen
import com.selah.bible.ui.screens.ReaderScreen
import com.selah.bible.ui.screens.SearchLibraryScreen
import com.selah.bible.ui.screens.SettingsScreen
import com.selah.bible.ui.theme.LocalSelahPalette
import com.selah.bible.ui.theme.SelahTheme
import com.selah.bible.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

private enum class NavTab(val label: String) {
    HOME("Home"),
    READER("Bible"),
    PLANS("Plans"),
    LIBRARY("Search"),
    SETTINGS("Studio")
}

class MainActivity : ComponentActivity() {

    private lateinit var db: BibleDatabase
    private lateinit var settingsStore: SettingsStore
    private lateinit var votdEngine: VotdEngine

    private var currentTab by mutableStateOf(NavTab.HOME)
    private var readerBookId by mutableIntStateOf(19)
    private var readerChapter by mutableIntStateOf(23)
    private var readerFocusVerse by mutableStateOf<Int?>(null)
    private var refreshTick by mutableIntStateOf(0)

    private var importProgress by mutableStateOf<ImportProgress?>(null)
    private var importStatusMessage by mutableStateOf<String?>(null)
    private var pendingMergeTarget: Translation? = null

    private val multiFilePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val target = pendingMergeTarget
        pendingMergeTarget = null
        if (!uris.isNullOrEmpty()) runMultiBibleImport(uris, forcedKind = null, targetTranslation = target)
    }
    private val epubPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) runMultiBibleImport(uris, forcedKind = "epub", targetTranslation = null)
    }
    private val pdfPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) runMultiBibleImport(uris, forcedKind = "pdf", targetTranslation = null)
    }
    private val sqlitePicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) runMultiBibleImport(uris, forcedKind = "sqlite", targetTranslation = null)
    }

    private val fontPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val fileName = resolveDisplayName(uri) ?: "custom.ttf"
            lifecycleScope.launch(Dispatchers.IO) {
                val res = runCatching {
                    CustomFontManager.importFontFromUri(this@MainActivity, uri, fileName)
                }
                withContext(Dispatchers.Main) {
                    res.onSuccess { entry ->
                        settingsStore.update { it.copy(customFontFileName = entry.fileName) }
                        refreshTick++
                        Toast.makeText(
                            this@MainActivity,
                            "Imported font \"${entry.displayName}\" (${entry.originalFormat})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { err ->
                        Toast.makeText(
                            this@MainActivity,
                            "Font import failed: ${err.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private val backupExportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        val json = BackupManager.exportUserDataJson(db)
                        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            if (result.isSuccess) "Offline backup saved!" else "Backup failed: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    private val backupImportLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("Could not read backup file")
                        BackupManager.importUserDataJson(db, text)
                    }
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            refreshTick++
                            WidgetUpdater.updateAllWidgets(this@MainActivity)
                            Toast.makeText(this@MainActivity, "Restored ${result.getOrNull()} records!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Restore failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SelahApp
        db = app.database
        settingsStore = app.settingsStore
        votdEngine = VotdEngine(db, settingsStore)

        val initial = settingsStore.current()
        readerBookId = initial.lastBookId
        readerChapter = initial.lastChapter

        handleWidgetIntent(intent)
        importBundledAssetBiblesIfAny()

        setContent {
            val settings by settingsStore.state.collectAsState()

            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            SelahTheme(settings = settings) {
                SelahRootContent(settings = settings)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        val b = intent.getIntExtra(WidgetUpdater.EXTRA_BOOK_ID, -1)
        val c = intent.getIntExtra(WidgetUpdater.EXTRA_CHAPTER, -1)
        val v = intent.getIntExtra(WidgetUpdater.EXTRA_VERSE, -1)
        if (b in 1..66 && c >= 1) {
            readerBookId = b
            readerChapter = c
            readerFocusVerse = if (v >= 1) v else null
            currentTab = NavTab.READER
        }
    }

    /**
     * If the user placed pre-converted .sqlite Bibles in app/src/main/assets/bibles/
     * before compiling their own APK, auto-import them seamlessly on first launch.
     */
    private fun importBundledAssetBiblesIfAny() {
        lifecycleScope.launch(Dispatchers.IO) {
            val files = runCatching { assets.list("bibles")?.toList().orEmpty() }.getOrDefault(emptyList())
                .filter { it.endsWith(".sqlite", true) || it.endsWith(".db", true) }
            if (files.isEmpty()) return@launch

            val existingIds = db.getTranslations().map { it.id }.toSet()
            var importedAny = false
            for (name in files) {
                val (inferredId, inferredCode, inferredName) = BibleTextParser.inferTranslationMeta(name)
                if (inferredId in existingIds) continue

                val temp = File(cacheDir, "asset_$name")
                runCatching {
                    assets.open("bibles/$name").use { input ->
                        FileOutputStream(temp).use { output -> input.copyTo(output) }
                    }
                    var trId = inferredId
                    var trCode = inferredCode
                    var trName = inferredName
                    val verses = ArrayList<BibleVerse>(32000)
                    SQLiteDatabase.openDatabase(temp.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { src ->
                        runCatching {
                            src.rawQuery("SELECT key, value FROM meta", null).use { c ->
                                while (c.moveToNext()) {
                                    when (c.getString(0)) {
                                        "id" -> trId = c.getString(1)
                                        "code" -> trCode = c.getString(1)
                                        "name" -> trName = c.getString(1)
                                    }
                                }
                            }
                        }
                        if (trId !in existingIds) {
                            src.rawQuery("SELECT book_id, chapter, verse, text FROM verses ORDER BY book_id, chapter, verse", null).use { c ->
                                while (c.moveToNext()) {
                                    verses.add(BibleVerse(trId, c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3)))
                                }
                            }
                        }
                    }
                    if (verses.isNotEmpty()) {
                        db.saveImportedTranslation(trId, trCode, trName, verses)
                        importedAny = true
                    }
                }
                temp.delete()
            }
            if (importedAny) {
                withContext(Dispatchers.Main) {
                    refreshTick++
                    WidgetUpdater.updateAllWidgets(this@MainActivity)
                }
            }
        }
    }

    private fun detectFileKind(fileName: String, forcedKind: String?): String {
        if (forcedKind != null) return forcedKind
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".sqlite") || lower.endsWith(".sqlite3") || lower.endsWith(".db") -> "sqlite"
            lower.endsWith(".epub") -> "epub"
            lower.endsWith(".pdf") -> "pdf"
            else -> "epub"
        }
    }

    private fun runMultiBibleImport(
        uris: List<Uri>,
        forcedKind: String?,
        targetTranslation: Translation?
    ) {
        importStatusMessage = null
        lifecycleScope.launch(Dispatchers.IO) {
            val entries = uris.map { uri ->
                val name = resolveDisplayName(uri) ?: "bible.${forcedKind ?: "epub"}"
                val kind = detectFileKind(name, forcedKind)
                val priority = when (kind) {
                    "sqlite" -> 0
                    "epub" -> 1
                    else -> 2
                }
                Triple(uri, name, kind) to priority
            }.sortedBy { it.second }.map { it.first }

            val summaries = LinkedHashMap<String, Triple<Translation, Int, Int>>() // trId -> (tr, addedTotal, upgradedTotal)
            val errors = mutableListOf<String>()
            var lastTranslation: Translation? = null

            entries.forEachIndexed { fileIdx, (uri, fileName, kind) ->
                val fileLabel = "[${fileIdx + 1}/${entries.size}] $fileName"

                val parsedResult = runCatching {
                    when (kind) {
                        "sqlite" -> {
                            val parsed = SqliteImporter.parseFromUri(
                                this@MainActivity, uri, fileName, overrideTranslationId = targetTranslation?.id
                            ) { p ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    importProgress = p.copy(stage = "$fileLabel • ${p.stage}")
                                }
                            }
                            Triple(
                                targetTranslation?.id ?: parsed.translationId,
                                targetTranslation?.code ?: parsed.code,
                                targetTranslation?.name ?: parsed.name
                            ) to parsed.verses
                        }
                        "pdf" -> {
                            val batch = PdfImporter.parseFromUri(
                                this@MainActivity, uri, fileName, overrideTranslationId = targetTranslation?.id
                            ) { p ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    importProgress = p.copy(stage = "$fileLabel • ${p.stage}")
                                }
                            }
                            Triple(
                                targetTranslation?.id ?: batch.detected.id,
                                targetTranslation?.code ?: batch.detected.code,
                                targetTranslation?.name ?: batch.detected.name
                            ) to batch.verses
                        }
                        else -> {
                            val batch = EpubImporter.parseFromUri(
                                this@MainActivity, uri, fileName, overrideTranslationId = targetTranslation?.id
                            ) { p ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    importProgress = p.copy(stage = "$fileLabel • ${p.stage}")
                                }
                            }
                            Triple(
                                targetTranslation?.id ?: batch.detected.id,
                                targetTranslation?.code ?: batch.detected.code,
                                targetTranslation?.name ?: batch.detected.name
                            ) to batch.verses
                        }
                    }
                }

                parsedResult.onSuccess { (meta, verses) ->
                    if (verses.isEmpty()) {
                        errors.add("$fileName: no verses detected")
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            importProgress = ImportProgress(
                                stage = "$fileLabel • Merging ${verses.size} verses into ${meta.second}…",
                                fraction = 0.94f,
                                versesParsed = verses.size
                            )
                        }
                        val merge = db.mergeImportedTranslation(meta.first, meta.second, meta.third, verses)
                        lastTranslation = merge.translation
                        val prev = summaries[meta.first]
                        summaries[meta.first] = Triple(
                            merge.translation,
                            (prev?.second ?: 0) + merge.addedVerses,
                            (prev?.third ?: 0) + merge.upgradedVerses
                        )
                    }
                }.onFailure { err ->
                    errors.add("$fileName: ${err.message}")
                }
            }

            withContext(Dispatchers.Main) {
                importProgress = null
                lastTranslation?.let { tr ->
                    settingsStore.update { it.copy(activeTranslationId = tr.id) }
                }
                val parts = mutableListOf<String>()
                summaries.values.forEach { (tr, added, upgraded) ->
                    val delta = buildString {
                        if (added > 0) append("+$added new")
                        if (upgraded > 0) {
                            if (isNotEmpty()) append(", ")
                            append("$upgraded improved")
                        }
                        if (isEmpty()) append("already up to date")
                    }
                    parts.add("${tr.code}: ${tr.bookCount}/66 books, ${tr.verseCount} verses ($delta)")
                }
                if (errors.isNotEmpty()) {
                    parts.add("Issues: ${errors.joinToString("; ")}")
                }
                importStatusMessage = if (parts.isNotEmpty()) {
                    parts.joinToString("\n")
                } else {
                    "No verses imported."
                }
                refreshTick++
                WidgetUpdater.updateAllWidgets(this@MainActivity)
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return uri.lastPathSegment
    }

    @Composable
    private fun SelahRootContent(settings: com.selah.bible.data.UserSettings) {
        val palette = LocalSelahPalette.current
        // Read refreshTick to trigger lightweight recomposition after local SQLite mutations
        val tick = refreshTick

        val translations = db.getTranslations()
        val votd = votdEngine.getTodayVotd(forceNew = false)
        val isVotdFav = db.isFavorite(votd.bookId, votd.chapter, votd.startVerse)
        val streak = db.getStreakStats()
        val activePlan = ReadingPlanCatalog.getById(settings.activePlanId)
        val completedPlanKeys = db.getCompletedChaptersForPlan(activePlan.id)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.background,
            bottomBar = {
                NavigationBar(
                    containerColor = palette.surface,
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        NavTab.HOME to Icons.Outlined.Home,
                        NavTab.READER to Icons.Outlined.AutoStories,
                        NavTab.PLANS to Icons.Outlined.CalendarMonth,
                        NavTab.LIBRARY to Icons.Outlined.Search,
                        NavTab.SETTINGS to Icons.Outlined.Tune
                    )
                    items.forEach { (tab, icon) ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = if (settings.showNavLabels) {
                                { Text(tab.label, fontSize = 11.sp) }
                            } else null,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                unselectedIconColor = palette.textSecondary,
                                unselectedTextColor = palette.textSecondary,
                                indicatorColor = palette.accent.copy(alpha = 0.14f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Keep tick referenced cleanly so DB mutations refresh screens immediately
                val _sync = tick
                when (currentTab) {
                    NavTab.HOME -> HomeScreen(
                        settings = settings,
                        votd = votd,
                        isVotdFavorite = isVotdFav,
                        streak = streak,
                        activePlan = activePlan,
                        planCompletedKeys = completedPlanKeys,
                        translations = translations,
                        onRefreshVotd = {
                            votdEngine.getTodayVotd(forceNew = true)
                            refreshTick++
                            WidgetUpdater.updateVotdWidgets(this@MainActivity, forceNew = false)
                        },
                        onToggleVotdFavorite = {
                            db.toggleFavorite(
                                BibleVerse(
                                    translationId = settings.activeTranslationId,
                                    bookId = votd.bookId,
                                    chapter = votd.chapter,
                                    verse = votd.startVerse,
                                    text = votd.text
                                )
                            )
                            refreshTick++
                            WidgetUpdater.updateFavoritesWidgets(this@MainActivity, delta = 0)
                        },
                        onOpenReader = { b, c, v ->
                            readerBookId = b
                            readerChapter = c
                            readerFocusVerse = v
                            settingsStore.update { it.copy(lastBookId = b, lastChapter = c) }
                            currentTab = NavTab.READER
                        },
                        onMarkTodayRead = {
                            db.recordReadingActivity(1)
                            refreshTick++
                            WidgetUpdater.updateVotdWidgets(this@MainActivity)
                        },
                        onOpenPlans = { currentTab = NavTab.PLANS },
                        onOpenImport = { currentTab = NavTab.SETTINGS },
                        onQuickToggleTheme = {
                            settingsStore.update { cur ->
                                val nextTheme = if (palette.isDark) cur.preferredLightThemeId else cur.preferredDarkThemeId
                                cur.copy(followSystemTheme = false, themeId = nextTheme)
                            }
                        }
                    )

                    NavTab.READER -> {
                        val verses = db.getChapterVerses(settings.activeTranslationId, readerBookId, readerChapter)
                        val favsInCh = db.getFavoriteVersesInChapter(readerBookId, readerChapter)
                        val hlInCh = db.getChapterHighlights(readerBookId, readerChapter)
                        val notesInCh = db.getChapterNotes(readerBookId, readerChapter)
                        val availBooks = db.getAvailableBookIds(settings.activeTranslationId)
                        val chKey = "$readerBookId:$readerChapter"

                        ReaderScreen(
                            bookId = readerBookId,
                            chapter = readerChapter,
                            focusVerse = readerFocusVerse,
                            verses = verses,
                            favoritesInChapter = favsInCh,
                            highlightsInChapter = hlInCh,
                            notesInChapter = notesInCh,
                            availableBookIds = availBooks,
                            availableChaptersForBook = { b -> db.getAvailableChaptersForBook(settings.activeTranslationId, b) },
                            translations = translations,
                            settings = settings,
                            isChapterCompletedInPlan = chKey in completedPlanKeys,
                            onNavigateChapter = { b, c ->
                                readerBookId = b
                                readerChapter = c
                                readerFocusVerse = null
                                settingsStore.update { it.copy(lastBookId = b, lastChapter = c) }
                            },
                            onSelectTranslation = { id ->
                                settingsStore.update { it.copy(activeTranslationId = id) }
                                WidgetUpdater.updateAllWidgets(this@MainActivity)
                            },
                            onToggleFavorite = { verse ->
                                db.toggleFavorite(verse)
                                refreshTick++
                                WidgetUpdater.updateFavoritesWidgets(this@MainActivity, delta = 0)
                            },
                            onSetHighlight = { verse, colorKey ->
                                db.setHighlight(verse.bookId, verse.chapter, verse.verse, colorKey)
                                refreshTick++
                            },
                            onSaveNote = { verse, note ->
                                db.saveNote(verse.bookId, verse.chapter, verse.verse, note)
                                refreshTick++
                            },
                            onCompareVerse = { verse ->
                                db.compareVerseAcrossTranslations(verse.bookId, verse.chapter, verse.verse)
                            },
                            onMarkChapterRead = { b, c ->
                                db.recordReadingActivity(1)
                                db.setPlanChapterCompleted(activePlan.id, "$b:$c", true)
                                refreshTick++
                                WidgetUpdater.updateVotdWidgets(this@MainActivity)
                            },
                            onUpdateSettings = { transform -> settingsStore.update(transform) },
                            onOpenImport = { currentTab = NavTab.SETTINGS }
                        )
                    }

                    NavTab.PLANS -> PlansScreen(
                        settings = settings,
                        activePlan = activePlan,
                        completedKeys = completedPlanKeys,
                        onSelectPlan = { planId ->
                            settingsStore.update { it.copy(activePlanId = planId) }
                            refreshTick++
                        },
                        onToggleChapterDone = { planId, chKey, completed ->
                            db.setPlanChapterCompleted(planId, chKey, completed)
                            refreshTick++
                            WidgetUpdater.updateVotdWidgets(this@MainActivity)
                        },
                        onResetPlan = { planId ->
                            db.resetPlan(planId)
                            settingsStore.update { it.copy(planStartEpochDay = LocalDate.now().toEpochDay()) }
                            refreshTick++
                        },
                        onOpenChapter = { b, c ->
                            readerBookId = b
                            readerChapter = c
                            readerFocusVerse = null
                            settingsStore.update { it.copy(lastBookId = b, lastChapter = c) }
                            currentTab = NavTab.READER
                        }
                    )

                    NavTab.LIBRARY -> SearchLibraryScreen(
                        settings = settings,
                        favorites = db.getFavorites(),
                        highlights = db.getAllHighlights(),
                        notes = db.getAllNotes(),
                        onSearch = { q, t -> db.searchVerses(settings.activeTranslationId, q, t) },
                        onLoadVerseText = { b, c, v -> db.getPassageText(settings.activeTranslationId, b, c, v, v) },
                        onRemoveFavorite = { fav ->
                            db.removeFavorite(fav.bookId, fav.chapter, fav.verse)
                            refreshTick++
                            WidgetUpdater.updateFavoritesWidgets(this@MainActivity, delta = 0)
                        },
                        onOpenVerse = { b, c, v ->
                            readerBookId = b
                            readerChapter = c
                            readerFocusVerse = v
                            settingsStore.update { it.copy(lastBookId = b, lastChapter = c) }
                            currentTab = NavTab.READER
                        }
                    )

                    NavTab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        translations = translations,
                        customFonts = CustomFontManager.listCustomFonts(this@MainActivity),
                        importProgress = importProgress,
                        importStatusMessage = importStatusMessage,
                        onSelectTranslation = { id ->
                            settingsStore.update { it.copy(activeTranslationId = id) }
                            WidgetUpdater.updateAllWidgets(this@MainActivity)
                        },
                        onDeleteTranslation = { id ->
                            db.deleteTranslation(id)
                            val remaining = db.getTranslations()
                            if (settings.activeTranslationId == id) {
                                val nextId = remaining.firstOrNull()?.id ?: StarterKjvData.TRANSLATION_ID
                                settingsStore.update { it.copy(activeTranslationId = nextId) }
                            }
                            refreshTick++
                            WidgetUpdater.updateAllWidgets(this@MainActivity)
                        },
                        onRenameTranslation = { id, code, name ->
                            db.renameTranslation(id, code, name)
                            refreshTick++
                            WidgetUpdater.updateAllWidgets(this@MainActivity)
                        },
                        onRestoreStarterKjv = {
                            db.restoreStarterKjv()
                            settingsStore.update { it.copy(activeTranslationId = StarterKjvData.TRANSLATION_ID) }
                            refreshTick++
                            WidgetUpdater.updateAllWidgets(this@MainActivity)
                        },
                        onLoadBookCoverage = { trId -> db.getBookCoverage(trId) },
                        onPickMultipleFiles = { target ->
                            pendingMergeTarget = target
                            multiFilePicker.launch(arrayOf("*/*"))
                        },
                        onPickEpub = {
                            epubPicker.launch(arrayOf("application/epub+zip", "application/octet-stream", "*/*"))
                        },
                        onPickPdf = {
                            pdfPicker.launch(arrayOf("application/pdf", "*/*"))
                        },
                        onPickSqlite = {
                            sqlitePicker.launch(arrayOf("application/x-sqlite3", "application/octet-stream", "*/*"))
                        },
                        onPickCustomFont = {
                            fontPicker.launch(arrayOf("*/*"))
                        },
                        onDeleteCustomFont = { fileName ->
                            CustomFontManager.deleteFont(this@MainActivity, fileName)
                            if (settings.customFontFileName == fileName) {
                                settingsStore.update { it.copy(customFontFileName = null) }
                            }
                            refreshTick++
                        },
                        onUpdateSettings = { transform -> settingsStore.update(transform) },
                        onRefreshWidgets = {
                            WidgetUpdater.updateAllWidgets(this@MainActivity)
                            Toast.makeText(this@MainActivity, "Home screen widgets synced!", Toast.LENGTH_SHORT).show()
                        },
                        onExportBackup = {
                            backupExportLauncher.launch("selah_bible_backup.json")
                        },
                        onImportBackup = {
                            backupImportLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                }
            }
        }
    }
}
