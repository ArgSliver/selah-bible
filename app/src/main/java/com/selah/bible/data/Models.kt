package com.selah.bible.data

data class Translation(
    val id: String,          // e.g. "kjv_starter", "kjv", "nlt"
    val code: String,        // e.g. "KJV", "NLT"
    val name: String,        // e.g. "King James Version"
    val verseCount: Int,
    val bookCount: Int,
    val isBuiltIn: Boolean,
    val importedAt: Long
)

data class BibleVerse(
    val translationId: String,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class VerseLocation(
    val bookId: Int,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int = startVerse
) {
    fun reference(): String = BibleCanon.formatReference(bookId, chapter, startVerse, endVerse)
}

data class VotdPassage(
    val epochDay: Long,
    val bookId: Int,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val text: String,
    val translationCode: String
) {
    fun reference(): String = BibleCanon.formatReference(bookId, chapter, startVerse, endVerse)
}

data class FavoriteVerse(
    val id: Long,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val translationId: String,
    val textSnapshot: String,
    val createdAt: Long
) {
    fun reference(): String = BibleCanon.formatReference(bookId, chapter, verse)
}

data class VerseNote(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val note: String,
    val updatedAt: Long
) {
    fun reference(): String = BibleCanon.formatReference(bookId, chapter, verse)
}

data class VerseHighlight(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val colorKey: String,
    val updatedAt: Long
)

data class StreakStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalDaysRead: Int,
    val totalChaptersRead: Int,
    val readToday: Boolean,
    val last30DaysMask: List<Boolean> // index 0 = 29 days ago, index 29 = today
)

data class ChapterAssignment(
    val bookId: Int,
    val chapter: Int
) {
    fun label(): String = "${BibleCanon.getById(bookId).shortName} $chapter"
    fun fullLabel(): String = "${BibleCanon.getById(bookId).name} $chapter"
    fun key(): String = "$bookId:$chapter"
}

data class ReadingPlanDay(
    val dayNumber: Int, // 1..totalDays
    val assignments: List<ChapterAssignment>
)

data class ReadingPlan(
    val id: String,
    val title: String,
    val subtitle: String,
    val totalDays: Int,
    val days: List<ReadingPlanDay>
) {
    val totalChapters: Int get() = days.sumOf { it.assignments.size }
}

data class ImportProgress(
    val stage: String,
    val fraction: Float, // 0f..1f
    val versesParsed: Int,
    val currentBook: String = ""
)

data class SearchHit(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val translationCode: String
) {
    fun reference(): String = BibleCanon.formatReference(bookId, chapter, verse)
}

data class BookCoverage(
    val book: BibleBook,
    val chaptersLoaded: Int,
    val verseCount: Int
) {
    val isComplete: Boolean get() = chaptersLoaded >= book.chapters
    val isPartial: Boolean get() = chaptersLoaded in 1 until book.chapters
    val isMissing: Boolean get() = chaptersLoaded == 0
}

data class MergeSummary(
    val translation: Translation,
    val addedVerses: Int,
    val upgradedVerses: Int,
    val booksAdded: Int
)
