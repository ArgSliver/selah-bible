package com.selah.bible.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

/**
 * Ultra-lightweight, zero-ORM SQLite database with WAL mode and indexed lookups.
 * Keeps memory footprint minimal and reads sub-millisecond even with multiple
 * full 31,102-verse Bibles (e.g. KJV + NLT) imported locally.
 */
class BibleDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION
) {
    companion object {
        const val DB_NAME = "selah_bible.db"
        const val DB_VERSION = 2

        @Volatile
        private var instance: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase {
            return instance ?: synchronized(this) {
                instance ?: BibleDatabase(context).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE translations (
                id TEXT PRIMARY KEY,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                verse_count INTEGER NOT NULL DEFAULT 0,
                book_count INTEGER NOT NULL DEFAULT 0,
                is_builtin INTEGER NOT NULL DEFAULT 0,
                imported_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE verses (
                translation_id TEXT NOT NULL,
                book_id INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                verse INTEGER NOT NULL,
                text TEXT NOT NULL,
                PRIMARY KEY (translation_id, book_id, chapter, verse)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_verses_lookup ON verses (translation_id, book_id, chapter)")

        // FTS4 virtual table for instant offline full-text search across all imported translations
        db.execSQL(
            """
            CREATE VIRTUAL TABLE verses_fts USING fts4(
                translation_id,
                book_id,
                chapter,
                verse,
                text
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                verse INTEGER NOT NULL,
                translation_id TEXT NOT NULL,
                text_snapshot TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(book_id, chapter, verse)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE notes (
                book_id INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                verse INTEGER NOT NULL,
                note TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (book_id, chapter, verse)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE highlights (
                book_id INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                verse INTEGER NOT NULL,
                color_key TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (book_id, chapter, verse)
            )
            """.trimIndent()
        )

        // Tracks daily reading activity by LocalDate.toEpochDay() for accurate streaks
        db.execSQL(
            """
            CREATE TABLE reading_days (
                epoch_day INTEGER PRIMARY KEY,
                chapters_read INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Tracks Verse of the Day history to prevent loops and keep daily consistency
        db.execSQL(
            """
            CREATE TABLE votd_history (
                epoch_day INTEGER PRIMARY KEY,
                book_id INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                start_verse INTEGER NOT NULL,
                end_verse INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Tracks completion of "Read the Bible in a Year" and other challenges
        db.execSQL(
            """
            CREATE TABLE plan_progress (
                plan_id TEXT NOT NULL,
                chapter_key TEXT NOT NULL,
                completed_at INTEGER NOT NULL,
                PRIMARY KEY (plan_id, chapter_key)
            )
            """.trimIndent()
        )

        seedStarterKjv(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Merge any legacy kjv_starter + kjv_full rows into unified 'kjv'
            db.beginTransaction()
            try {
                val hasLegacy = db.rawQuery(
                    "SELECT 1 FROM translations WHERE id IN ('kjv_starter', 'kjv_full') LIMIT 1",
                    null
                ).use { it.moveToFirst() }
                if (hasLegacy) {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO verses (translation_id, book_id, chapter, verse, text)
                        SELECT 'kjv', book_id, chapter, verse, text FROM verses WHERE translation_id = 'kjv_full'
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO verses (translation_id, book_id, chapter, verse, text)
                        SELECT 'kjv', book_id, chapter, verse, text FROM verses WHERE translation_id = 'kjv_starter'
                        """.trimIndent()
                    )
                    db.delete("verses", "translation_id IN ('kjv_starter', 'kjv_full')", null)
                    db.delete("verses_fts", "translation_id IN ('kjv_starter', 'kjv_full', 'kjv')", null)
                    db.execSQL(
                        """
                        INSERT INTO verses_fts (translation_id, book_id, chapter, verse, text)
                        SELECT translation_id, book_id, chapter, verse, text FROM verses WHERE translation_id = 'kjv'
                        """.trimIndent()
                    )
                    db.delete("translations", "id IN ('kjv_starter', 'kjv_full')", null)

                    var vCount = 0
                    var bCount = 0
                    db.rawQuery(
                        "SELECT COUNT(*), COUNT(DISTINCT book_id) FROM verses WHERE translation_id = 'kjv'",
                        null
                    ).use { c ->
                        if (c.moveToFirst()) {
                            vCount = c.getInt(0)
                            bCount = c.getInt(1)
                        }
                    }
                    if (vCount > 0) {
                        val isStarter = vCount < 500
                        val cv = ContentValues().apply {
                            put("id", StarterKjvData.TRANSLATION_ID)
                            put("code", StarterKjvData.TRANSLATION_CODE)
                            put("name", if (isStarter) StarterKjvData.TRANSLATION_NAME else "King James Version")
                            put("verse_count", vCount)
                            put("book_count", bCount)
                            put("is_builtin", if (isStarter) 1 else 0)
                            put("imported_at", System.currentTimeMillis())
                        }
                        db.insertWithOnConflict("translations", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                    db.execSQL("UPDATE favorites SET translation_id = 'kjv' WHERE translation_id IN ('kjv_starter', 'kjv_full')")
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun seedStarterKjv(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val verses = StarterKjvData.verses
        val books = verses.map { it.bookId }.distinct().size

        db.beginTransaction()
        try {
            val tv = ContentValues().apply {
                put("id", StarterKjvData.TRANSLATION_ID)
                put("code", StarterKjvData.TRANSLATION_CODE)
                put("name", StarterKjvData.TRANSLATION_NAME)
                put("verse_count", verses.size)
                put("book_count", books)
                put("is_builtin", 1)
                put("imported_at", now)
            }
            db.insertWithOnConflict("translations", null, tv, SQLiteDatabase.CONFLICT_REPLACE)

            val insertVerse = db.compileStatement(
                "INSERT OR REPLACE INTO verses (translation_id, book_id, chapter, verse, text) VALUES (?, ?, ?, ?, ?)"
            )
            val insertFts = db.compileStatement(
                "INSERT INTO verses_fts (translation_id, book_id, chapter, verse, text) VALUES (?, ?, ?, ?, ?)"
            )

            for (v in verses) {
                insertVerse.bindString(1, StarterKjvData.TRANSLATION_ID)
                insertVerse.bindLong(2, v.bookId.toLong())
                insertVerse.bindLong(3, v.chapter.toLong())
                insertVerse.bindLong(4, v.verse.toLong())
                insertVerse.bindString(5, v.text)
                insertVerse.executeInsert()

                insertFts.bindString(1, StarterKjvData.TRANSLATION_ID)
                insertFts.bindLong(2, v.bookId.toLong())
                insertFts.bindLong(3, v.chapter.toLong())
                insertFts.bindLong(4, v.verse.toLong())
                insertFts.bindString(5, v.text)
                insertFts.executeInsert()
            }

            // Seed one starter favorite so the Favorites widget immediately looks great
            val fav = ContentValues().apply {
                put("book_id", 19)
                put("chapter", 46)
                put("verse", 10)
                put("translation_id", StarterKjvData.TRANSLATION_ID)
                put("text_snapshot", "Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth.")
                put("created_at", now)
            }
            db.insertWithOnConflict("favorites", null, fav, SQLiteDatabase.CONFLICT_IGNORE)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // -------------------------------------------------------------------------
    // Translations & Bulk Import
    // -------------------------------------------------------------------------

    fun getTranslations(): List<Translation> {
        val list = mutableListOf<Translation>()
        readableDatabase.rawQuery(
            "SELECT id, code, name, verse_count, book_count, is_builtin, imported_at FROM translations ORDER BY is_builtin ASC, imported_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Translation(
                        id = c.getString(0),
                        code = c.getString(1),
                        name = c.getString(2),
                        verseCount = c.getInt(3),
                        bookCount = c.getInt(4),
                        isBuiltIn = c.getInt(5) == 1,
                        importedAt = c.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun getTranslation(id: String): Translation? = getTranslations().find { it.id == id }

    fun saveImportedTranslation(
        id: String,
        code: String,
        name: String,
        verses: List<BibleVerse>
    ): Translation {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val distinctBooks = verses.map { it.bookId }.distinct().size

        db.beginTransaction()
        try {
            db.delete("verses", "translation_id = ?", arrayOf(id))
            db.delete("verses_fts", "translation_id = ?", arrayOf(id))

            val insertVerse = db.compileStatement(
                "INSERT OR REPLACE INTO verses (translation_id, book_id, chapter, verse, text) VALUES (?, ?, ?, ?, ?)"
            )
            val insertFts = db.compileStatement(
                "INSERT INTO verses_fts (translation_id, book_id, chapter, verse, text) VALUES (?, ?, ?, ?, ?)"
            )

            for (v in verses) {
                insertVerse.bindString(1, id)
                insertVerse.bindLong(2, v.bookId.toLong())
                insertVerse.bindLong(3, v.chapter.toLong())
                insertVerse.bindLong(4, v.verse.toLong())
                insertVerse.bindString(5, v.text)
                insertVerse.executeInsert()

                insertFts.bindString(1, id)
                insertFts.bindLong(2, v.bookId.toLong())
                insertFts.bindLong(3, v.chapter.toLong())
                insertFts.bindLong(4, v.verse.toLong())
                insertFts.bindString(5, v.text)
                insertFts.executeInsert()
            }

            val tv = ContentValues().apply {
                put("id", id)
                put("code", code)
                put("name", name)
                put("verse_count", verses.size)
                put("book_count", distinctBooks)
                put("is_builtin", 0)
                put("imported_at", now)
            }
            db.insertWithOnConflict("translations", null, tv, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return Translation(id, code, name, verses.size, distinctBooks, false, now)
    }

    /**
     * Combines incoming verses from an EPUB, PDF, or SQLite file into an existing (or new)
     * translation WITHOUT wiping existing books/chapters/verses.
     * - Missing verses are inserted.
     * - Truncated/fragment verses (< 18 chars) are upgraded if the new source has fuller text.
     * - Clean existing verses are preserved so combining EPUB + PDF fills gaps cleanly.
     */
    fun mergeImportedTranslation(
        id: String,
        code: String,
        name: String,
        incomingVerses: List<BibleVerse>
    ): MergeSummary {
        val db = writableDatabase
        val now = System.currentTimeMillis()

        // Load existing verse map for this translation
        val existingMap = HashMap<Long, String>(32000)
        val existingBooks = HashSet<Int>(66)
        readableDatabase.rawQuery(
            "SELECT book_id, chapter, verse, text FROM verses WHERE translation_id = ?",
            arrayOf(id)
        ).use { c ->
            while (c.moveToNext()) {
                val b = c.getInt(0)
                val ch = c.getInt(1)
                val v = c.getInt(2)
                val t = c.getString(3)
                existingBooks.add(b)
                val key = (b.toLong() shl 20) or (ch.toLong() shl 10) or v.toLong()
                existingMap[key] = t
            }
        }

        val booksBefore = existingBooks.size
        var added = 0
        var upgraded = 0
        val toWrite = ArrayList<BibleVerse>(incomingVerses.size)

        for (v in incomingVerses) {
            val key = (v.bookId.toLong() shl 20) or (v.chapter.toLong() shl 10) or v.verse.toLong()
            val cur = existingMap[key]
            if (cur == null) {
                existingMap[key] = v.text
                existingBooks.add(v.bookId)
                toWrite.add(v.copy(translationId = id))
                added++
            } else if (cur.length < 18 && v.text.length > cur.length) {
                existingMap[key] = v.text
                toWrite.add(v.copy(translationId = id))
                upgraded++
            }
        }

        val totalVerses = existingMap.size
        val totalBooks = existingBooks.size
        val existingMeta = getTranslation(id)
        val finalCode = existingMeta?.code ?: code
        // Automatically graduate KJV Starter -> full King James Version once additional KJV verses are merged onto it!
        val upgradedStarter = existingMeta?.isBuiltIn == true && totalVerses >= 500
        val finalName = when {
            upgradedStarter -> name.ifBlank { "King James Version" }
            existingMeta != null -> existingMeta.name
            else -> name
        }
        val finalBuiltIn = existingMeta?.isBuiltIn == true && !upgradedStarter

        db.beginTransaction()
        try {
            val insertVerse = db.compileStatement(
                "INSERT OR REPLACE INTO verses (translation_id, book_id, chapter, verse, text) VALUES (?, ?, ?, ?, ?)"
            )
            for (v in toWrite) {
                insertVerse.bindString(1, id)
                insertVerse.bindLong(2, v.bookId.toLong())
                insertVerse.bindLong(3, v.chapter.toLong())
                insertVerse.bindLong(4, v.verse.toLong())
                insertVerse.bindString(5, v.text)
                insertVerse.executeInsert()
            }

            // Rebuild FTS4 entries cleanly if anything changed (fast inside a single transaction)
            if (toWrite.isNotEmpty() || existingMeta == null) {
                db.delete("verses_fts", "translation_id = ?", arrayOf(id))
                db.execSQL(
                    """
                    INSERT INTO verses_fts (translation_id, book_id, chapter, verse, text)
                    SELECT translation_id, book_id, chapter, verse, text
                    FROM verses WHERE translation_id = ?
                    """.trimIndent(),
                    arrayOf(id)
                )
            }

            val tv = ContentValues().apply {
                put("id", id)
                put("code", finalCode)
                put("name", finalName)
                put("verse_count", totalVerses)
                put("book_count", totalBooks)
                put("is_builtin", if (finalBuiltIn) 1 else 0)
                put("imported_at", now)
            }
            db.insertWithOnConflict("translations", null, tv, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val tr = Translation(id, finalCode, finalName, totalVerses, totalBooks, finalBuiltIn, now)
        return MergeSummary(
            translation = tr,
            addedVerses = added,
            upgradedVerses = upgraded,
            booksAdded = (totalBooks - booksBefore).coerceAtLeast(0)
        )
    }

    fun getBookCoverage(translationId: String): List<BookCoverage> {
        val counts = HashMap<Int, Pair<Int, Int>>(66) // bookId -> (distinctChapters, verseCount)
        readableDatabase.rawQuery(
            "SELECT book_id, COUNT(DISTINCT chapter), COUNT(*) FROM verses WHERE translation_id = ? GROUP BY book_id",
            arrayOf(translationId)
        ).use { c ->
            while (c.moveToNext()) {
                counts[c.getInt(0)] = c.getInt(1) to c.getInt(2)
            }
        }
        return BibleCanon.books.map { book ->
            val (ch, vc) = counts[book.id] ?: (0 to 0)
            BookCoverage(book = book, chaptersLoaded = ch, verseCount = vc)
        }
    }

    fun deleteTranslation(id: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("verses", "translation_id = ?", arrayOf(id))
            db.delete("verses_fts", "translation_id = ?", arrayOf(id))
            db.delete("translations", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun renameTranslation(id: String, newCode: String, newName: String) {
        val cleanCode = newCode.trim().uppercase().take(8).ifEmpty { return }
        val cleanName = newName.trim().take(64).ifEmpty { return }
        val cv = ContentValues().apply {
            put("code", cleanCode)
            put("name", cleanName)
        }
        writableDatabase.update("translations", cv, "id = ?", arrayOf(id))
    }

    fun restoreStarterKjv() {
        seedStarterKjv(writableDatabase)
    }

    // -------------------------------------------------------------------------
    // Chapter & Verse Queries
    // -------------------------------------------------------------------------

    fun getChapterVerses(translationId: String, bookId: Int, chapter: Int): List<BibleVerse> {
        val list = mutableListOf<BibleVerse>()
        readableDatabase.rawQuery(
            "SELECT verse, text FROM verses WHERE translation_id = ? AND book_id = ? AND chapter = ? ORDER BY verse ASC",
            arrayOf(translationId, bookId.toString(), chapter.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    BibleVerse(
                        translationId = translationId,
                        bookId = bookId,
                        chapter = chapter,
                        verse = c.getInt(0),
                        text = c.getString(1)
                    )
                )
            }
        }
        return list
    }

    fun getAvailableChapters(translationId: String): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT book_id, chapter FROM verses WHERE translation_id = ? ORDER BY book_id ASC, chapter ASC",
            arrayOf(translationId)
        ).use { c ->
            while (c.moveToNext()) {
                list.add(c.getInt(0) to c.getInt(1))
            }
        }
        return list
    }

    fun getAvailableChaptersForBook(translationId: String, bookId: Int): Set<Int> {
        val set = HashSet<Int>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT chapter FROM verses WHERE translation_id = ? AND book_id = ?",
            arrayOf(translationId, bookId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                set.add(c.getInt(0))
            }
        }
        return set
    }

    fun getAvailableBookIds(translationId: String): Set<Int> {
        val set = HashSet<Int>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT book_id FROM verses WHERE translation_id = ?",
            arrayOf(translationId)
        ).use { c ->
            while (c.moveToNext()) {
                set.add(c.getInt(0))
            }
        }
        return set
    }

    fun getPassageText(
        translationId: String,
        bookId: Int,
        chapter: Int,
        startVerse: Int,
        endVerse: Int
    ): String {
        val parts = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT text FROM verses WHERE translation_id = ? AND book_id = ? AND chapter = ? AND verse BETWEEN ? AND ? ORDER BY verse ASC",
            arrayOf(translationId, bookId.toString(), chapter.toString(), startVerse.toString(), endVerse.toString())
        ).use { c ->
            while (c.moveToNext()) {
                parts.add(c.getString(0).trim())
            }
        }
        return parts.joinToString(" ")
    }

    fun compareVerseAcrossTranslations(bookId: Int, chapter: Int, verse: Int): List<Pair<Translation, String>> {
        val translations = getTranslations()
        val results = mutableListOf<Pair<Translation, String>>()
        for (t in translations) {
            val text = getPassageText(t.id, bookId, chapter, verse, verse)
            if (text.isNotBlank()) {
                results.add(t to text)
            }
        }
        return results
    }

    // -------------------------------------------------------------------------
    // Full-Text Search
    // -------------------------------------------------------------------------

    fun searchVerses(
        translationId: String,
        query: String,
        testamentFilter: Testament? = null,
        limit: Int = 80
    ): List<SearchHit> {
        val cleaned = query.trim()
        if (cleaned.length < 2) return emptyList()
        val code = getTranslation(translationId)?.code ?: "KJV"

        val hits = mutableListOf<SearchHit>()
        val ftsTokens = cleaned
            .split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-zA-Z0-9']"), "") }
            .filter { it.isNotBlank() }

        val bookRangeSql = when (testamentFilter) {
            Testament.OLD -> "AND CAST(book_id AS INTEGER) <= 39"
            Testament.NEW -> "AND CAST(book_id AS INTEGER) >= 40"
            null -> ""
        }

        if (ftsTokens.isNotEmpty()) {
            val ftsMatch = ftsTokens.joinToString(" ") { "$it*" }
            try {
                readableDatabase.rawQuery(
                    """
                    SELECT book_id, chapter, verse, text
                    FROM verses_fts
                    WHERE translation_id = ? AND text MATCH ? $bookRangeSql
                    LIMIT ?
                    """.trimIndent(),
                    arrayOf(translationId, ftsMatch, limit.toString())
                ).use { c ->
                    while (c.moveToNext()) {
                        hits.add(
                            SearchHit(
                                bookId = c.getInt(0),
                                chapter = c.getInt(1),
                                verse = c.getInt(2),
                                text = c.getString(3),
                                translationCode = code
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback to LIKE below if query contains special FTS syntax
            }
        }

        if (hits.isEmpty()) {
            val likeRangeSql = when (testamentFilter) {
                Testament.OLD -> "AND book_id <= 39"
                Testament.NEW -> "AND book_id >= 40"
                null -> ""
            }
            readableDatabase.rawQuery(
                """
                SELECT book_id, chapter, verse, text
                FROM verses
                WHERE translation_id = ? AND text LIKE ? $likeRangeSql
                ORDER BY book_id ASC, chapter ASC, verse ASC
                LIMIT ?
                """.trimIndent(),
                arrayOf(translationId, "%$cleaned%", limit.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    hits.add(
                        SearchHit(
                            bookId = c.getInt(0),
                            chapter = c.getInt(1),
                            verse = c.getInt(2),
                            text = c.getString(3),
                            translationCode = code
                        )
                    )
                }
            }
        }

        return hits
    }

    // -------------------------------------------------------------------------
    // Verse of the Day Persistence (Non-Looping History)
    // -------------------------------------------------------------------------

    fun getVotdForDay(epochDay: Long): VerseLocation? {
        readableDatabase.rawQuery(
            "SELECT book_id, chapter, start_verse, end_verse FROM votd_history WHERE epoch_day = ?",
            arrayOf(epochDay.toString())
        ).use { c ->
            if (c.moveToFirst()) {
                return VerseLocation(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3))
            }
        }
        return null
    }

    fun saveVotdForDay(epochDay: Long, loc: VerseLocation) {
        val cv = ContentValues().apply {
            put("epoch_day", epochDay)
            put("book_id", loc.bookId)
            put("chapter", loc.chapter)
            put("start_verse", loc.startVerse)
            put("end_verse", loc.endVerse)
        }
        writableDatabase.insertWithOnConflict("votd_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getVotdHistoryKeys(): Set<String> {
        val keys = HashSet<String>()
        readableDatabase.rawQuery("SELECT book_id, chapter, start_verse FROM votd_history", null).use { c ->
            while (c.moveToNext()) {
                keys.add("${c.getInt(0)}:${c.getInt(1)}:${c.getInt(2)}")
            }
        }
        return keys
    }

    fun clearOldVotdHistoryExceptToday(todayEpoch: Long) {
        writableDatabase.delete("votd_history", "epoch_day != ?", arrayOf(todayEpoch.toString()))
    }

    // -------------------------------------------------------------------------
    // Daily Streak & Reading Log
    // -------------------------------------------------------------------------

    fun recordReadingActivity(chaptersDelta: Int = 1, day: LocalDate = LocalDate.now()) {
        val epochDay = day.toEpochDay()
        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        try {
            var existingChapters = 0
            db.rawQuery(
                "SELECT chapters_read FROM reading_days WHERE epoch_day = ?",
                arrayOf(epochDay.toString())
            ).use { c ->
                if (c.moveToFirst()) existingChapters = c.getInt(0)
            }
            val cv = ContentValues().apply {
                put("epoch_day", epochDay)
                put("chapters_read", existingChapters + chaptersDelta.coerceAtLeast(1))
                put("updated_at", now)
            }
            db.insertWithOnConflict("reading_days", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getStreakStats(today: LocalDate = LocalDate.now()): StreakStats {
        val todayEpoch = today.toEpochDay()
        val days = ArrayList<Long>()
        var totalChapters = 0
        readableDatabase.rawQuery(
            "SELECT epoch_day, chapters_read FROM reading_days ORDER BY epoch_day ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                days.add(c.getLong(0))
                totalChapters += c.getInt(1)
            }
        }

        val daySet = days.toHashSet()
        val readToday = todayEpoch in daySet

        // Calculate current streak (active if read today OR yesterday)
        var currentStreak = 0
        var cursor = if (readToday) todayEpoch else todayEpoch - 1
        while (cursor in daySet) {
            currentStreak++
            cursor--
        }

        // Calculate longest streak ever
        var longest = 0
        var run = 0
        var prev = Long.MIN_VALUE
        for (d in days) {
            if (d == prev + 1) {
                run++
            } else {
                run = 1
            }
            if (run > longest) longest = run
            prev = d
        }

        val last30 = (29 downTo 0).map { offset -> (todayEpoch - offset) in daySet }

        return StreakStats(
            currentStreak = currentStreak,
            longestStreak = longest,
            totalDaysRead = days.size,
            totalChaptersRead = totalChapters,
            readToday = readToday,
            last30DaysMask = last30
        )
    }

    fun getAllReadingDays(): List<Pair<Long, Int>> {
        val list = mutableListOf<Pair<Long, Int>>()
        readableDatabase.rawQuery("SELECT epoch_day, chapters_read FROM reading_days", null).use { c ->
            while (c.moveToNext()) {
                list.add(c.getLong(0) to c.getInt(1))
            }
        }
        return list
    }

    // -------------------------------------------------------------------------
    // Favorites, Highlights, Notes
    // -------------------------------------------------------------------------

    fun toggleFavorite(verse: BibleVerse): Boolean {
        val db = writableDatabase
        val exists = isFavorite(verse.bookId, verse.chapter, verse.verse)
        if (exists) {
            db.delete(
                "favorites",
                "book_id = ? AND chapter = ? AND verse = ?",
                arrayOf(verse.bookId.toString(), verse.chapter.toString(), verse.verse.toString())
            )
            return false
        } else {
            val cv = ContentValues().apply {
                put("book_id", verse.bookId)
                put("chapter", verse.chapter)
                put("verse", verse.verse)
                put("translation_id", verse.translationId)
                put("text_snapshot", verse.text)
                put("created_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            return true
        }
    }

    fun isFavorite(bookId: Int, chapter: Int, verse: Int): Boolean {
        readableDatabase.rawQuery(
            "SELECT 1 FROM favorites WHERE book_id = ? AND chapter = ? AND verse = ? LIMIT 1",
            arrayOf(bookId.toString(), chapter.toString(), verse.toString())
        ).use { c ->
            return c.moveToFirst()
        }
    }

    fun getFavoriteVersesInChapter(bookId: Int, chapter: Int): Set<Int> {
        val set = HashSet<Int>()
        readableDatabase.rawQuery(
            "SELECT verse FROM favorites WHERE book_id = ? AND chapter = ?",
            arrayOf(bookId.toString(), chapter.toString())
        ).use { c ->
            while (c.moveToNext()) set.add(c.getInt(0))
        }
        return set
    }

    fun getFavorites(): List<FavoriteVerse> {
        val list = mutableListOf<FavoriteVerse>()
        readableDatabase.rawQuery(
            "SELECT id, book_id, chapter, verse, translation_id, text_snapshot, created_at FROM favorites ORDER BY created_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    FavoriteVerse(
                        id = c.getLong(0),
                        bookId = c.getInt(1),
                        chapter = c.getInt(2),
                        verse = c.getInt(3),
                        translationId = c.getString(4),
                        textSnapshot = c.getString(5),
                        createdAt = c.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun removeFavorite(bookId: Int, chapter: Int, verse: Int) {
        writableDatabase.delete(
            "favorites",
            "book_id = ? AND chapter = ? AND verse = ?",
            arrayOf(bookId.toString(), chapter.toString(), verse.toString())
        )
    }

    fun setHighlight(bookId: Int, chapter: Int, verse: Int, colorKey: String?) {
        val db = writableDatabase
        if (colorKey == null) {
            db.delete(
                "highlights",
                "book_id = ? AND chapter = ? AND verse = ?",
                arrayOf(bookId.toString(), chapter.toString(), verse.toString())
            )
        } else {
            val cv = ContentValues().apply {
                put("book_id", bookId)
                put("chapter", chapter)
                put("verse", verse)
                put("color_key", colorKey)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("highlights", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getChapterHighlights(bookId: Int, chapter: Int): Map<Int, String> {
        val map = HashMap<Int, String>()
        readableDatabase.rawQuery(
            "SELECT verse, color_key FROM highlights WHERE book_id = ? AND chapter = ?",
            arrayOf(bookId.toString(), chapter.toString())
        ).use { c ->
            while (c.moveToNext()) map[c.getInt(0)] = c.getString(1)
        }
        return map
    }

    fun getAllHighlights(): List<VerseHighlight> {
        val list = mutableListOf<VerseHighlight>()
        readableDatabase.rawQuery(
            "SELECT book_id, chapter, verse, color_key, updated_at FROM highlights ORDER BY updated_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(VerseHighlight(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3), c.getLong(4)))
            }
        }
        return list
    }

    fun saveNote(bookId: Int, chapter: Int, verse: Int, note: String) {
        val db = writableDatabase
        val trimmed = note.trim()
        if (trimmed.isEmpty()) {
            db.delete(
                "notes",
                "book_id = ? AND chapter = ? AND verse = ?",
                arrayOf(bookId.toString(), chapter.toString(), verse.toString())
            )
        } else {
            val cv = ContentValues().apply {
                put("book_id", bookId)
                put("chapter", chapter)
                put("verse", verse)
                put("note", trimmed)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("notes", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getChapterNotes(bookId: Int, chapter: Int): Map<Int, String> {
        val map = HashMap<Int, String>()
        readableDatabase.rawQuery(
            "SELECT verse, note FROM notes WHERE book_id = ? AND chapter = ?",
            arrayOf(bookId.toString(), chapter.toString())
        ).use { c ->
            while (c.moveToNext()) map[c.getInt(0)] = c.getString(1)
        }
        return map
    }

    fun getAllNotes(): List<VerseNote> {
        val list = mutableListOf<VerseNote>()
        readableDatabase.rawQuery(
            "SELECT book_id, chapter, verse, note, updated_at FROM notes ORDER BY updated_at DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(VerseNote(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3), c.getLong(4)))
            }
        }
        return list
    }

    // -------------------------------------------------------------------------
    // Reading Plans ("Read the Bible in a Year")
    // -------------------------------------------------------------------------

    fun getCompletedChaptersForPlan(planId: String): Set<String> {
        val set = HashSet<String>()
        readableDatabase.rawQuery(
            "SELECT chapter_key FROM plan_progress WHERE plan_id = ?",
            arrayOf(planId)
        ).use { c ->
            while (c.moveToNext()) set.add(c.getString(0))
        }
        return set
    }

    fun setPlanChapterCompleted(planId: String, chapterKey: String, completed: Boolean) {
        val db = writableDatabase
        if (completed) {
            val cv = ContentValues().apply {
                put("plan_id", planId)
                put("chapter_key", chapterKey)
                put("completed_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("plan_progress", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            recordReadingActivity(1)
        } else {
            db.delete(
                "plan_progress",
                "plan_id = ? AND chapter_key = ?",
                arrayOf(planId, chapterKey)
            )
        }
    }

    fun resetPlan(planId: String) {
        writableDatabase.delete("plan_progress", "plan_id = ?", arrayOf(planId))
    }

    fun getAllPlanProgress(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        readableDatabase.rawQuery("SELECT plan_id, chapter_key FROM plan_progress", null).use { c ->
            while (c.moveToNext()) list.add(c.getString(0) to c.getString(1))
        }
        return list
    }
}
