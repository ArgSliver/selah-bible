package com.selah.bible.data.importer

import com.selah.bible.data.BibleBook
import com.selah.bible.data.BibleCanon
import com.selah.bible.data.BibleVerse

/**
 * Resilient, Study-Bible-aware Bible text parser supporting real-world KJV, NLT, NIV, ESV, MEV,
 * and arbitrary EPUB & PDF layouts:
 * - Prevents Study Bible book introductions & cross-reference quotes (e.g. "Judges 17:6" inside
 *   Ruth's intro, "1 Kings 4:32" inside Song of Songs' intro, "Isaiah 40:3" inside Mark's intro)
 *   from hijacking the active book!
 * - Recognizes explicit ⟦H⟧ HTML headings from EPUB <h1>/<h2>/<h3> tags
 * - Full classic KJV headers, Roman numeral chapters, inline Chapter:Verse, drop-cap implicit Verse 1
 * - Ignores NLT Song of Songs speaker labels and PDF running page headers
 */
class BibleTextParser(private val translationId: String) {

    private var currentBook: BibleBook? = null
    private var sectionLockedBook: BibleBook? = null
    private var headingBasedMode: Boolean = false
    private var inBookIntroduction: Boolean = false
    private var currentChapter: Int = 0
    private var currentVerse: Int = 0
    private val currentText = StringBuilder()
    private var expectingImplicitVerseOne: Boolean = false

    // Keyed by packed (bookId, chapter, verse)
    private val verseMap = LinkedHashMap<Long, BibleVerse>(32000)

    val parsedCount: Int get() = verseMap.size
    val currentBookName: String get() = currentBook?.name ?: ""

    companion object {
        private val explicitRefRegex = Regex(
            "^([1-3]?\\s?[A-Za-z][A-Za-z .]{1,28}?)\\s+(\\d{1,3}):(\\d{1,3})(?:[-–—]\\d{1,3})?\\s+(.+)$"
        )
        private val chapterHeadingRegex = Regex(
            "^(?:chapter|ch\\.?|psalm|ps\\.?)\\s+([0-9]{1,3}|[IVXLC]{1,8})(?:\\s*[:\\-–—.].*)?$",
            RegexOption.IGNORE_CASE
        )
        private val bookAndChapterRegex = Regex(
            "^([1-3]?\\s?[A-Za-z][A-Za-z .,'’()]{1,50}?)\\s+(?:chapter\\s+)?([0-9]{1,3}|[IVXLC]{1,8})$",
            RegexOption.IGNORE_CASE
        )
        private val runningHeaderRangeRegex = Regex(
            "^(?:\\d{1,4}\\s+)?[1-3]?\\s?[A-Za-z .]{2,26}\\s+\\d{1,3}:\\d{1,3}\\s*[-–—]\\s*(?:\\d{1,3}:)?\\d{1,3}(?:\\s+\\d{1,4})?$"
        )
        private val runningHeaderPageBookRegex = Regex(
            "^(?:\\d{1,4}\\s+([1-3]?\\s?[A-Za-z][A-Za-z .]{2,26})|([1-3]?\\s?[A-Za-z][A-Za-z .]{2,26})\\s+\\d{1,4})$"
        )
        private val chapterColonVerseStandaloneRegex = Regex("^(\\d{1,3}):(\\d{1,3})(?:[-–—]\\d{1,3})?$")
        private val chapterColonVerseRegex = Regex("^(\\d{1,3}):(\\d{1,3})(?:[-–—]\\d{1,3})?\\s+(.+)$")
        private val inlineChapterColonVerseRegex = Regex("(?<=\\s|^)(\\d{1,3}):(\\d{1,3})(?:[-–—]\\d{1,3})?\\s+")
        private val leadingVerseRegex = Regex("^(\\d{1,3})(?:[-–—]\\d{1,3})?(?:[.)\\]]|\\s+)\\s*(.+)$")
        private val inlineVerseMarkerRegex = Regex("⟦(\\d{1,3})⟧")
        private val inlineNumericVerseRegex = Regex("(?<=[.!?;:\"'”\\)])\\s+(\\d{1,3})(?:[-–—]\\d{1,3})?\\s+(?=[A-Z“\"‘'\\(])")
        private val standaloneNumberRegex = Regex("^(\\d{1,3})$")
        private val filenameBookChapterRegex = Regex("^([1-3]?\\s?[a-zA-Z]+(?:\\s+[a-zA-Z]+)*?)\\s*(\\d{1,3})$")
        private val crossRefPrefixRegex = Regex("^(?:see|cf\\.?|compare|also|note|notes|study note|cross-?ref)\\b", RegexOption.IGNORE_CASE)
        private val introSectionHeaderRegex = Regex(
            "^(?:introduction(?:\\s+to\\b.*)?|book\\s+introduction|overview|outline|blueprint|megathemes|vital\\s+statistics|author|setting|background|purpose|key\\s+verse|key\\s+people|special\\s+features|timeline)$",
            RegexOption.IGNORE_CASE
        )
        // Song of Songs dramatic speaker headings in NLT/NIV/ESV/MEV
        private val songSpeakerLabels = setOf(
            "young woman", "young man", "young women of jerusalem", "friends",
            "king solomon", "solomon", "beloved", "lover", "daughters of jerusalem",
            "she", "he", "others", "chorus", "the bride", "the bridegroom", "the woman", "the man"
        )

        fun inferTranslationMeta(fileName: String): Triple<String, String, String> {
            val d = TranslationDetector.detectFromFilenameOnly(fileName)
            return Triple(d.id, d.code, d.name)
        }
    }

    /**
     * Hint from EPUB section filename (e.g. "08_Ruth.xhtml", "22_SNG_01.xhtml", "ot30_amos.html", "41_Mark.xhtml").
     */
    fun hintSectionTitle(title: String) {
        sectionLockedBook = null
        val base = title
            .replace(Regex("\\.(x?html?|xml|txt)$", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        // Strip common EPUB filename prefixes like "08 ", "b08 ", "ot08 ", "nt02 ", "nlt 08 ", "ch041 "
        val candidates = listOf(
            base.replace(Regex("^(?:(?:ot|nt|bk|book|part|sec|ch|nlt|kjv|niv|esv|mev|bible)[\\s]*)*\\d{1,3}\\s+", RegexOption.IGNORE_CASE), "").trim(),
            base.replace(Regex("^(?:0\\d+|[4-9]\\d|\\d{3,})\\s+"), "").trim(),
            base.replace(Regex("^\\d+\\s+"), "").trim(),
            base
        ).distinct()

        for (cand in candidates) {
            val directBook = BibleCanon.findByName(cand)
            if (directBook != null) {
                sectionLockedBook = directBook
                headingBasedMode = true
                inBookIntroduction = false
                if (directBook.id != currentBook?.id) {
                    flushCurrentVerse()
                    currentBook = directBook
                    currentChapter = if (directBook.chapters == 1) 1 else 0
                    currentVerse = 0
                    expectingImplicitVerseOne = (directBook.chapters == 1)
                }
                return
            }

            val m = filenameBookChapterRegex.matchEntire(cand)
            if (m != null) {
                val book = BibleCanon.findByName(m.groupValues[1])
                val ch = m.groupValues[2].toIntOrNull()
                if (book != null && ch != null && ch in 1..book.chapters) {
                    sectionLockedBook = book
                    headingBasedMode = true
                    inBookIntroduction = false
                    flushCurrentVerse()
                    currentBook = book
                    currentChapter = ch
                    currentVerse = 0
                    expectingImplicitVerseOne = true
                    return
                }
            }
        }
    }

    fun feedText(block: String) {
        val normalized = block
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        for (rawLine in normalized.split('\n')) {
            processLine(rawLine.trim())
        }
    }

    private fun processLine(rawLine: String) {
        if (rawLine.isEmpty()) return

        val isHtmlHeading = rawLine.startsWith("⟦H⟧")
        val line = if (isHtmlHeading) rawLine.removePrefix("⟦H⟧").trim() else rawLine
        if (line.isEmpty()) return

        if (line.length <= 4 && line.all { it == '-' || it == '*' || it == '•' || it == '—' }) return
        if (crossRefPrefixRegex.containsMatchIn(line)) return
        if (runningHeaderRangeRegex.matches(line)) return

        val upper = line.uppercase()
        if (upper == "THE HOLY BIBLE" || upper == "OLD TESTAMENT" || upper == "NEW TESTAMENT" ||
            upper == "THE OLD TESTAMENT" || upper == "THE NEW TESTAMENT" ||
            upper == "KING JAMES VERSION" || upper == "NEW LIVING TRANSLATION" ||
            upper == "NEW INTERNATIONAL VERSION" || upper == "ENGLISH STANDARD VERSION" ||
            upper == "MODERN ENGLISH VERSION"
        ) {
            return
        }

        // Skip dramatic speaker labels in Song of Songs ("Young Woman", "Young Man", etc.)
        if (currentBook?.id == 22 && line.lowercase().trimEnd(':') in songSpeakerLabels) {
            return
        }

        // Detect Study Bible book introduction / outline headers so outline bullets don't become fake verses
        if ((isHtmlHeading || line.length <= 40) && introSectionHeaderRegex.matches(line) && currentChapter == 0) {
            inBookIntroduction = true
            return
        }

        // 1. Book heading line (up to 115 chars; handles HTML <h1>/<h2> headings and plain text book headers)
        if (line.length <= 115 && !line.contains('⟦')) {
            val bookOnly = BibleCanon.findByName(line)
            if (bookOnly != null) {
                // If already reading this exact book mid-chapter and NOT an explicit HTML heading, ignore PDF running header
                if (!isHtmlHeading && currentBook?.id == bookOnly.id && currentChapter > 0 && currentVerse > 0) {
                    return
                }
                // In Study Bible intros, an inline single-word mention of another book shouldn't hijack a section-locked book unless it's an HTML heading
                if (!isHtmlHeading && sectionLockedBook != null && bookOnly.id != sectionLockedBook!!.id) {
                    return
                }
                flushCurrentVerse()
                currentBook = bookOnly
                headingBasedMode = true
                inBookIntroduction = false
                currentChapter = if (bookOnly.chapters == 1) 1 else 0
                currentVerse = 0
                expectingImplicitVerseOne = (bookOnly.chapters == 1)
                return
            }

            // "Book Chapter" heading (e.g. "Ruth 1", "Song of Songs 2", "Amos 5", "Mark 1", "Psalm 23")
            bookAndChapterRegex.matchEntire(line)?.let { m ->
                val book = BibleCanon.findByName(m.groupValues[1])
                val ch = BibleCanon.parseChapterNumber(m.groupValues[2])
                if (book != null && ch != null && ch in 1..book.chapters) {
                    // Guard against PDF running page headers repeating the current chapter
                    if (!isHtmlHeading && currentBook?.id == book.id && currentChapter == ch && currentVerse > 0) {
                        return
                    }
                    // Guard against Study Bible intro cross-references hijacking a locked book section
                    if (!isHtmlHeading && sectionLockedBook != null && book.id != sectionLockedBook!!.id) {
                        return
                    }
                    flushCurrentVerse()
                    currentBook = book
                    headingBasedMode = true
                    inBookIntroduction = false
                    currentChapter = ch
                    currentVerse = 0
                    expectingImplicitVerseOne = true
                    return
                }
            }

            runningHeaderPageBookRegex.matchEntire(line)?.let { m ->
                val maybeBook = m.groupValues[1].ifBlank { m.groupValues[2] }
                val matched = BibleCanon.findByName(maybeBook)
                if (matched != null && matched.id == currentBook?.id) {
                    return
                }
            }
        }

        // 2. Explicit "Book Chapter:Verse text" line (e.g. "Genesis 1:1 In the beginning...")
        // CRITICAL FIX: Never let an isolated cross-reference quote inside a Study Bible Book Introduction
        // (e.g. "Judges 17:6 ..." inside Ruth's intro or "Isaiah 40:3 ..." inside Mark's intro)
        // hijack currentBook away from a heading-based or section-locked book!
        if (!headingBasedMode && sectionLockedBook == null) {
            explicitRefRegex.matchEntire(line)?.let { m ->
                val book = BibleCanon.findByName(m.groupValues[1])
                val ch = m.groupValues[2].toIntOrNull()
                val v = m.groupValues[3].toIntOrNull()
                val body = m.groupValues[4].trim()
                if (book != null && ch != null && v != null && ch in 1..book.chapters && v in 1..176) {
                    flushCurrentVerse()
                    currentBook = book
                    currentChapter = ch
                    currentVerse = v
                    expectingImplicitVerseOne = false
                    feedVerseSegments(body)
                    return
                }
            }
        }

        val activeBook = currentBook ?: return

        // 3. Chapter heading ("Chapter 12", "CHAPTER XII", "PSALM 46")
        chapterHeadingRegex.matchEntire(line)?.let { m ->
            val ch = BibleCanon.parseChapterNumber(m.groupValues[1])
            if (ch != null && ch in 1..activeBook.chapters) {
                if (currentChapter == ch && currentVerse > 0) return
                flushCurrentVerse()
                inBookIntroduction = false
                currentChapter = ch
                currentVerse = 0
                expectingImplicitVerseOne = true
                return
            }
        }

        // 4. Standalone chapter number ("1", "2", "12")
        standaloneNumberRegex.matchEntire(line)?.let { m ->
            val num = m.groupValues[1].toIntOrNull()
            if (num != null && num in 1..activeBook.chapters) {
                if (currentChapter == 0 || num == currentChapter + 1 || (inBookIntroduction && num == 1)) {
                    flushCurrentVerse()
                    inBookIntroduction = false
                    currentChapter = num
                    currentVerse = 0
                    expectingImplicitVerseOne = true
                    return
                }
            }
            return
        }

        // While inside a Study Bible Book Introduction (before Chapter 1 starts), ignore intro prose & outline bullets
        // unless explicit verse markup ⟦1⟧ / ⟦2⟧ appears (meaning scripture has actually begun!)
        if (inBookIntroduction && !line.contains('⟦')) {
            return
        }

        // 5. Standalone "Chapter:Verse" on its own line
        chapterColonVerseStandaloneRegex.matchEntire(line)?.let { m ->
            val ch = m.groupValues[1].toIntOrNull()
            val v = m.groupValues[2].toIntOrNull()
            if (ch != null && v != null && ch in 1..activeBook.chapters && v in 1..176) {
                flushCurrentVerse()
                inBookIntroduction = false
                currentChapter = ch
                currentVerse = v
                expectingImplicitVerseOne = false
                return
            }
        }

        // 6. One or more "Chapter:Verse text" entries on a line
        if (chapterColonVerseRegex.matches(line)) {
            val hits = inlineChapterColonVerseRegex.findAll(line).toList()
            if (hits.size > 1) {
                for (i in hits.indices) {
                    val ch = hits[i].groupValues[1].toIntOrNull() ?: continue
                    val v = hits[i].groupValues[2].toIntOrNull() ?: continue
                    if (ch in 1..activeBook.chapters && v in 1..176) {
                        val segStart = hits[i].range.last + 1
                        val segEnd = if (i + 1 < hits.size) hits[i + 1].range.first else line.length
                        val body = line.substring(segStart, segEnd).trim()
                        flushCurrentVerse()
                        inBookIntroduction = false
                        currentChapter = ch
                        currentVerse = v
                        expectingImplicitVerseOne = false
                        appendToCurrent(body)
                    }
                }
                return
            }
            chapterColonVerseRegex.matchEntire(line)?.let { m ->
                val ch = m.groupValues[1].toIntOrNull()
                val v = m.groupValues[2].toIntOrNull()
                val body = m.groupValues[3]
                if (ch != null && v != null && ch in 1..activeBook.chapters && v in 1..176) {
                    flushCurrentVerse()
                    inBookIntroduction = false
                    currentChapter = ch
                    currentVerse = v
                    expectingImplicitVerseOne = false
                    feedVerseSegments(body)
                    return
                }
            }
        }

        // 7. Explicit superscript/span verse markers injected by EPUB parser: ⟦1⟧ text ⟦2⟧ text
        if (line.contains('⟦')) {
            inBookIntroduction = false
            if (currentChapter == 0) currentChapter = 1
            feedMarkedParagraph(line)
            return
        }

        // Skip HTML section subheadings inside chapters (e.g. <h3>Jesus Calms the Storm</h3>)
        if (isHtmlHeading) return

        // 8. Leading verse number ("1 In the beginning..." or "16. For God so loved...")
        leadingVerseRegex.matchEntire(line)?.let { m ->
            val v = m.groupValues[1].toIntOrNull()
            val body = m.groupValues[2].trim()
            if (v != null && isPlausibleVerseNumber(v, activeBook)) {
                if (currentChapter == 0) {
                    currentChapter = 1
                } else if (v == 1 && currentVerse > 1 && currentChapter < activeBook.chapters) {
                    currentChapter += 1
                }
                flushCurrentVerse()
                currentVerse = v
                expectingImplicitVerseOne = false
                feedVerseSegments(body)
                return
            }
        }

        // 9. Implicit Verse 1 right after a Chapter heading or Drop-Cap
        if (currentChapter > 0 && currentVerse == 0 && expectingImplicitVerseOne) {
            if (!isLikelySectionHeading(line) && line.length >= 14 && line.any { it.isLowerCase() }) {
                currentVerse = 1
                expectingImplicitVerseOne = false
                feedVerseSegments(line)
                return
            }
        }

        // 10. Continuation text for active verse
        if (currentChapter > 0 && currentVerse > 0) {
            if (isLikelySectionHeading(line)) return
            feedVerseSegments(line)
        }
    }

    private fun isPlausibleVerseNumber(v: Int, book: BibleBook): Boolean {
        if (v !in 1..176) return false
        if (currentVerse == 0) return v <= 5
        if (v == 1 && currentChapter < book.chapters) return true
        return v in (currentVerse + 1)..(currentVerse + 8)
    }

    private fun isLikelySectionHeading(line: String): Boolean {
        if (line.length > 72 || line.length < 3) return false
        if (line.endsWith('.') || line.endsWith(',') || line.endsWith(';') ||
            line.endsWith(':') || line.endsWith('?') || line.endsWith('!')
        ) {
            return false
        }
        val words = line.split(' ').filter { it.isNotBlank() }
        if (words.size > 10) return false
        if (line.any { it.isLetter() } && line == line.uppercase() && !line.contains("LORD")) {
            return true
        }
        return false
    }

    private fun feedMarkedParagraph(line: String) {
        val matches = inlineVerseMarkerRegex.findAll(line).toList()
        if (matches.isEmpty()) return

        val beforeFirst = line.substring(0, matches.first().range.first).trim()
        if (beforeFirst.isNotEmpty()) {
            if (currentVerse > 0) {
                appendToCurrent(beforeFirst)
            } else if (expectingImplicitVerseOne && beforeFirst.length >= 10) {
                val firstMarkerNum = matches.first().groupValues[1].toIntOrNull() ?: 1
                if (firstMarkerNum == 2 || firstMarkerNum == 3) {
                    currentVerse = 1
                    expectingImplicitVerseOne = false
                    appendToCurrent(beforeFirst)
                }
            }
        }

        for (i in matches.indices) {
            val vNum = matches[i].groupValues[1].toIntOrNull() ?: continue
            val segStart = matches[i].range.last + 1
            val segEnd = if (i + 1 < matches.size) matches[i + 1].range.first else line.length
            val segText = line.substring(segStart, segEnd).trim()

            val activeBook = currentBook ?: continue
            if (vNum == 1 && currentVerse > 1 && currentChapter < activeBook.chapters) {
                flushCurrentVerse()
                currentChapter += 1
            } else {
                flushCurrentVerse()
            }
            if (vNum in 1..176) {
                currentVerse = vNum
                expectingImplicitVerseOne = false
                if (segText.isNotEmpty()) {
                    appendToCurrent(segText)
                }
            }
        }
    }

    private fun feedVerseSegments(text: String) {
        val matches = inlineNumericVerseRegex.findAll(text).toList()
        if (matches.isEmpty()) {
            appendToCurrent(text)
            return
        }

        var cursor = 0
        for (m in matches) {
            val candidateVerse = m.groupValues[1].toIntOrNull()
            if (candidateVerse != null && candidateVerse in (currentVerse + 1)..(currentVerse + 5)) {
                val before = text.substring(cursor, m.range.first).trim()
                if (before.isNotEmpty()) appendToCurrent(before)
                flushCurrentVerse()
                currentVerse = candidateVerse
                cursor = m.range.last + 1
            }
        }
        val tail = text.substring(cursor).trim()
        if (tail.isNotEmpty()) appendToCurrent(tail)
    }

    private fun appendToCurrent(text: String) {
        val cleaned = cleanVerseBody(text)
        if (cleaned.isEmpty()) return
        if (currentText.isNotEmpty()) currentText.append(' ')
        currentText.append(cleaned)
    }

    private fun cleanVerseBody(raw: String): String {
        return raw
            .replace("⟦H⟧", " ")
            .replace(Regex("⟦\\d+⟧"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun flushCurrentVerse() {
        val book = currentBook ?: return
        if (currentChapter in 1..book.chapters && currentVerse in 1..176 && currentText.isNotEmpty()) {
            val body = cleanVerseBody(currentText.toString())
            if (body.length >= 2) {
                val key = (book.id.toLong() shl 20) or (currentChapter.toLong() shl 10) or currentVerse.toLong()
                val existing = verseMap[key]
                if (existing == null || (existing.text.length < 20 && body.length > existing.text.length)) {
                    verseMap[key] = BibleVerse(
                        translationId = translationId,
                        bookId = book.id,
                        chapter = currentChapter,
                        verse = currentVerse,
                        text = body
                    )
                }
            }
        }
        currentText.setLength(0)
    }

    fun finish(): List<BibleVerse> {
        flushCurrentVerse()
        return verseMap.values.sortedWith(compareBy({ it.bookId }, { it.chapter }, { it.verse }))
    }
}
