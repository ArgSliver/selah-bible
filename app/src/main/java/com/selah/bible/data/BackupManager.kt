package com.selah.bible.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Pure offline JSON export/import for user study data:
 * favorites, highlights, notes, daily streak logs, and reading plan progress.
 */
object BackupManager {

    fun exportUserDataJson(db: BibleDatabase): String {
        val root = JSONObject()
        root.put("app", "SelahBible")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val favArr = JSONArray()
        for (f in db.getFavorites()) {
            favArr.put(JSONObject().apply {
                put("bookId", f.bookId)
                put("chapter", f.chapter)
                put("verse", f.verse)
                put("translationId", f.translationId)
                put("text", f.textSnapshot)
            })
        }
        root.put("favorites", favArr)

        val notesArr = JSONArray()
        for (n in db.getAllNotes()) {
            notesArr.put(JSONObject().apply {
                put("bookId", n.bookId)
                put("chapter", n.chapter)
                put("verse", n.verse)
                put("note", n.note)
            })
        }
        root.put("notes", notesArr)

        val hlArr = JSONArray()
        for (h in db.getAllHighlights()) {
            hlArr.put(JSONObject().apply {
                put("bookId", h.bookId)
                put("chapter", h.chapter)
                put("verse", h.verse)
                put("color", h.colorKey)
            })
        }
        root.put("highlights", hlArr)

        val streakArr = JSONArray()
        for ((epochDay, chapters) in db.getAllReadingDays()) {
            streakArr.put(JSONObject().apply {
                put("epochDay", epochDay)
                put("chapters", chapters)
            })
        }
        root.put("readingDays", streakArr)

        val planArr = JSONArray()
        for ((planId, chKey) in db.getAllPlanProgress()) {
            planArr.put(JSONObject().apply {
                put("planId", planId)
                put("chapterKey", chKey)
            })
        }
        root.put("planProgress", planArr)

        return root.toString(2)
    }

    fun importUserDataJson(db: BibleDatabase, jsonText: String): Int {
        val root = JSONObject(jsonText)
        var importedCount = 0

        root.optJSONArray("favorites")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val v = BibleVerse(
                    translationId = o.optString("translationId", StarterKjvData.TRANSLATION_ID),
                    bookId = o.getInt("bookId"),
                    chapter = o.getInt("chapter"),
                    verse = o.getInt("verse"),
                    text = o.optString("text", "")
                )
                if (!db.isFavorite(v.bookId, v.chapter, v.verse)) {
                    db.toggleFavorite(v)
                    importedCount++
                }
            }
        }

        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.saveNote(o.getInt("bookId"), o.getInt("chapter"), o.getInt("verse"), o.getString("note"))
                importedCount++
            }
        }

        root.optJSONArray("highlights")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.setHighlight(o.getInt("bookId"), o.getInt("chapter"), o.getInt("verse"), o.getString("color"))
                importedCount++
            }
        }

        root.optJSONArray("readingDays")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val day = LocalDate.ofEpochDay(o.getLong("epochDay"))
                db.recordReadingActivity(o.optInt("chapters", 1), day)
                importedCount++
            }
        }

        root.optJSONArray("planProgress")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.setPlanChapterCompleted(o.getString("planId"), o.getString("chapterKey"), true)
                importedCount++
            }
        }

        return importedCount
    }
}
