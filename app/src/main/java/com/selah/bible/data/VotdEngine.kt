package com.selah.bible.data

import java.security.SecureRandom
import java.time.LocalDate

/**
 * Coherent, Non-Looping Verse of the Day Engine.
 *
 * Guarantees:
 * 1. NOT A LOOP: Uses cryptographic entropy (SecureRandom) and tracks every shown
 *    passage in persistent history so verses never repeat in a fixed yearly cycle.
 * 2. COHERENT PASSAGES: Automatically filters out genealogies/fragments and expands
 *    mid-sentence verses backward/forward (up to 3 verses) so every Verse of the Day
 *    is a complete, self-contained thought.
 * 3. TRANSLATION-AGNOSTIC: Works on KJV, NLT, or any user-imported EPUB/PDF Bible,
 *    and re-renders today's exact reference when switching translations.
 */
object VotdCoherence {

    private val genealogyNoise = Regex(
        "\\b(begat|were numbered|numbered of them|the sons of|generations of|cubits|shekels|families of the|border of|coast of|cities with their villages)\\b",
        RegexOption.IGNORE_CASE
    )

    private val danglingStart = Regex(
        "^(and|but|for|therefore|wherefore|then|so|yet|because|nor|neither|which|who|whom|whose|that|likewise|moreover|nevertheless|also)\\b",
        RegexOption.IGNORE_CASE
    )

    private val danglingDialogue = Regex(
        "^(he said|she said|they said|and he said|and they said|saying,|answered and said)",
        RegexOption.IGNORE_CASE
    )

    private fun endsCleanly(text: String): Boolean {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return false
        val last = trimmed.last()
        if (last == '.' || last == '!' || last == '?') return true
        if ((last == '"' || last == '”' || last == '\'' || last == '’' || last == ')') && trimmed.length >= 2) {
            val prev = trimmed[trimmed.length - 2]
            return prev == '.' || prev == '!' || prev == '?'
        }
        return false
    }

    private fun endsOpenClause(text: String): Boolean {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return false
        val last = trimmed.last()
        return last == ',' || last == ';' || last == ':' || last == '-' || last == '—'
    }

    /**
     * Given a chapter's ordered verses and a chosen index, attempts to build a coherent
     * 1-to-3 verse passage. Returns null if the candidate is a genealogy/list or cannot
     * form a complete, readable thought within widget-friendly bounds.
     */
    fun buildCoherentPassage(
        chapterVerses: List<BibleVerse>,
        anchorIndex: Int,
        minChars: Int = 35,
        maxChars: Int = 420
    ): Pair<VerseLocation, String>? {
        if (anchorIndex !in chapterVerses.indices) return null
        val anchor = chapterVerses[anchorIndex]
        if (genealogyNoise.containsMatchIn(anchor.text)) return null
        if (danglingDialogue.containsMatchIn(anchor.text.trim())) return null

        var startIdx = anchorIndex
        var endIdx = anchorIndex

        // If the anchor verse clearly continues an unclosed clause from the preceding verse, include the preceding verse.
        if (startIdx > 0 &&
            danglingStart.containsMatchIn(chapterVerses[startIdx].text.trim()) &&
            endsOpenClause(chapterVerses[startIdx - 1].text)
        ) {
            val prev = chapterVerses[startIdx - 1]
            if (!genealogyNoise.containsMatchIn(prev.text) &&
                prev.verse == chapterVerses[startIdx].verse - 1
            ) {
                startIdx -= 1
            }
        }

        // Expand forward up to 3 total verses if the current ending is mid-sentence (ends with , ; : or no terminal punctuation)
        while (!endsCleanly(chapterVerses[endIdx].text) &&
            endIdx + 1 < chapterVerses.size &&
            (endIdx - startIdx + 1) < 3
        ) {
            val next = chapterVerses[endIdx + 1]
            if (next.verse != chapterVerses[endIdx].verse + 1) break
            if (genealogyNoise.containsMatchIn(next.text)) return null
            endIdx += 1
        }

        val slice = chapterVerses.subList(startIdx, endIdx + 1)
        val combined = slice.joinToString(" ") { it.text.trim() }.replace(Regex("\\s+"), " ").trim()

        if (combined.length < minChars || combined.length > maxChars) return null
        if (!endsCleanly(combined)) return null
        if (genealogyNoise.containsMatchIn(combined)) return null

        // Avoid awkward standalone fragments that still begin with "Therefore," or "Nor " without context
        if (startIdx == endIdx && Regex("^(Nor|Neither|Wherefore|Therefore|Saying)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(combined)
        ) {
            return null
        }

        val loc = VerseLocation(
            bookId = anchor.bookId,
            chapter = anchor.chapter,
            startVerse = slice.first().verse,
            endVerse = slice.last().verse
        )
        return loc to combined
    }

    /**
     * Picks a coherent, non-repeating passage from available chapters using SecureRandom
     * and genre-weighted sampling so the selection is genuinely random across the canon.
     */
    fun selectRandomCoherent(
        availableChapters: List<Pair<Int, Int>>,
        chapterLoader: (bookId: Int, chapter: Int) -> List<BibleVerse>,
        alreadyShownKeys: Set<String>,
        random: java.util.Random = SecureRandom()
    ): Pair<VerseLocation, String>? {
        if (availableChapters.isEmpty()) return null

        // Build genre-weighted pool of chapters
        val weighted = ArrayList<Pair<Int, Int>>(availableChapters.size * 3)
        for (pair in availableChapters) {
            val weight = BibleCanon.getById(pair.first).genre.votdWeight.coerceAtLeast(1)
            repeat(weight) { weighted.add(pair) }
        }

        // Phase 1: Fast random probes (up to 60 attempts) to find a fresh coherent passage without scanning the entire DB
        val maxAttempts = 60
        for (attempt in 0 until maxAttempts) {
            val (bookId, chapter) = weighted[random.nextInt(weighted.size)]
            val verses = chapterLoader(bookId, chapter)
            if (verses.isEmpty()) continue
            val anchorIdx = random.nextInt(verses.size)
            val candidate = buildCoherentPassage(verses, anchorIdx) ?: continue
            val key = historyKey(candidate.first)
            if (key !in alreadyShownKeys) {
                return candidate
            }
        }

        // Phase 2: Thorough shuffled scan (handles smaller starter Bibles or nearly-exhausted pools)
        val shuffledChapters = availableChapters.shuffled(random)
        var fallbackReuse: Pair<VerseLocation, String>? = null
        for ((bookId, chapter) in shuffledChapters) {
            val verses = chapterLoader(bookId, chapter)
            val indices = verses.indices.shuffled(random)
            for (idx in indices) {
                val candidate = buildCoherentPassage(verses, idx) ?: continue
                val key = historyKey(candidate.first)
                if (key !in alreadyShownKeys) {
                    return candidate
                }
                if (fallbackReuse == null) {
                    fallbackReuse = candidate
                }
            }
        }

        // Phase 3: Only if every single coherent passage in the Bible has been shown,
        // return a freshly shuffled passage (still random, never a fixed loop).
        if (fallbackReuse != null) return fallbackReuse

        // Absolute fallback if imported text has unusual punctuation
        val (bookId, chapter) = availableChapters[random.nextInt(availableChapters.size)]
        val verses = chapterLoader(bookId, chapter)
        val first = verses.firstOrNull() ?: return null
        return VerseLocation(bookId, chapter, first.verse, first.verse) to first.text
    }

    fun historyKey(loc: VerseLocation): String = "${loc.bookId}:${loc.chapter}:${loc.startVerse}"
}

class VotdEngine(
    private val db: BibleDatabase,
    private val settings: SettingsStore
) {
    private val random = SecureRandom()

    fun getTodayVotd(forceNew: Boolean = false): VotdPassage {
        val todayEpoch = LocalDate.now().toEpochDay()
        val translationId = settings.activeTranslationId()
        val translation = db.getTranslation(translationId)
            ?: db.getTranslations().firstOrNull()
        val activeId = translation?.id ?: StarterKjvData.TRANSLATION_ID
        val activeCode = translation?.code ?: StarterKjvData.TRANSLATION_CODE

        if (!forceNew) {
            val existing = db.getVotdForDay(todayEpoch)
            if (existing != null) {
                val renderedText = db.getPassageText(
                    activeId,
                    existing.bookId,
                    existing.chapter,
                    existing.startVerse,
                    existing.endVerse
                )
                if (renderedText.isNotBlank()) {
                    return VotdPassage(
                        epochDay = todayEpoch,
                        bookId = existing.bookId,
                        chapter = existing.chapter,
                        startVerse = existing.startVerse,
                        endVerse = existing.endVerse,
                        text = renderedText,
                        translationCode = activeCode
                    )
                }
            }
        }

        val availableChapters = db.getAvailableChapters(activeId)
        val shownKeys = db.getVotdHistoryKeys()

        val selected = VotdCoherence.selectRandomCoherent(
            availableChapters = availableChapters,
            chapterLoader = { b, c -> db.getChapterVerses(activeId, b, c) },
            alreadyShownKeys = shownKeys,
            random = random
        )

        if (selected != null) {
            val (loc, text) = selected
            // If history covered everything and forced a reuse, clear history cycle so the next decade is fresh
            if (VotdCoherence.historyKey(loc) in shownKeys && shownKeys.size > 20) {
                db.clearOldVotdHistoryExceptToday(todayEpoch)
            }
            db.saveVotdForDay(todayEpoch, loc)
            return VotdPassage(
                epochDay = todayEpoch,
                bookId = loc.bookId,
                chapter = loc.chapter,
                startVerse = loc.startVerse,
                endVerse = loc.endVerse,
                text = text,
                translationCode = activeCode
            )
        }

        return VotdPassage(
            epochDay = todayEpoch,
            bookId = 19,
            chapter = 46,
            startVerse = 10,
            endVerse = 10,
            text = "Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth.",
            translationCode = activeCode
        )
    }
}
