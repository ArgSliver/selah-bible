package com.selah.bible.data.importer

import com.selah.bible.data.BibleBook
import com.selah.bible.data.BibleCanon
import com.selah.bible.data.BibleVerse

/**
 * Resilient, Study-Bible-aware Bible text parser supporting KJV, NLT, NIV, ESV, MEV,
 * and arbitrary EPUB & PDF layouts.
 *
 * Key safeguards:
 * - Never latches into a drop-all state on PDFs or EPUBs (removed brittle intro-skip latch)
 * - Protects Book Introductions (when currentChapter == 0) from stray cross-reference quotes
 *   of OTHER books (e.g. "Judges 17:6" in Ruth's intro, "1 Kings 4:32" in Song of Songs' intro,
 *   "2 Kings 14:25" in Amos's intro, "Isaiah 40:3" in Mark's intro) without disabling explicit
 *   "Book Ch:V" parsing across full files
 * - Handles HTML headings with subtitles ("Ruth: A Story of Redemption", "Mark 1 — Jesus...")
 * - Supports Roman numerals, inline Chapter:Verse, drop-cap implicit Verse 1, Song of Songs speakers
 */
class BibleTextParser(private val translationId: String) {

    private var currentBook: BibleBook? = null
    private var currentChapter: Int = 0
    private var currentVerse: Int = 0
    private val currentText = StringBuilder()
    private var expectingImplicitVerseOne: Boolean = false
    private var lastLineWasExplicitRef: Boolean = false
    private var sawMarkedVersesInCurrentBook: Boolean = false
    // Tracks speculative standalone chapter advances so a stray PDF page number ("2" during Genesis 1:25)
    // automatically reverts if the very next line continues with verse 26!
    private var tentativeChapterFrom: Int = 0
    private var tentativeVerseFrom: Int = 0

    // Keyed by packed (bookId, chapter, verse)
    private val verseMap = LinkedHashMap<Long, BibleVerse>(32000)

    val parsedCount: Int get() = verseMap.size
    val currentBookName: String get() = currentBook?.name ?: ""

    companion object {
        private val explicitRefRegex = Regex(
            "^([1-3]?\\s?[A-Za-z][A-Za-z .]{1,28}?)\\s+(\\d{1,3}):(\\d{1,3})(?:[-–—]\\d{1,3})?\\s+(.+)$"
        )
        private val chapterHeadingRegex = Regex(
            "^(?:chapter|ch\\.?|psalm|ps\\.?)\\s+([0-9]{1,3}|[IVXLC]{1,8})(?:\\s*[:\\-–—.].*|\\s+[A-Z].*)?$",
            RegexOption.IGNORE_CASE
        )
        private val bookAndChapterRegex = Regex(
            "^([1-3]?\\s?[A-Za-z][A-Za-z .,'’()]{1,46}?)\\s+(?:chapter\\s+)?([0-9]{1,3}|[IVXLC]{1,8})(?:\\s*[:\\-–—].*)?$",
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
        // Supports spaced ("1 In the beginning"), punctuated ("1. In..."), and PDF attached superscripts ("1In the beginning")
        private val leadingVerseRegex = Regex("^(\\d{1,3})(?:[-–—]\\d{1,3})?(?:[.)\\]]\\s*|\\s+|(?=[A-Z“\"‘'\\(]))(.+)$")
        private val inlineVerseMarkerRegex = Regex("⟦(\\d{1,3})⟧")
        // Supports both spaced ("...void. 2 And") and attached PDF superscripts ("...void.2And", "...void. 2And")
        private val inlineNumericVerseRegex = Regex("(?<=[.!?;,\"'”’\\)—–-]|(?<!\\d):)\\s*(\\d{1,3})(?:[-–—]\\d{1,3})?\\s*(?=[A-Z“\"‘'\\(])")
        private val standaloneNumberRegex = Regex("^(\\d{1,3})$")
        private val filenameBookChapterRegex = Regex("^([1-3]?\\s?[a-zA-Z]+(?:\\s+[a-zA-Z]+)*?)\\s*(\\d{1,3})$")
        private val crossRefPrefixRegex = Regex(
            "^(?:cf\\.?|study\\s+notes?|cross-?ref(?:erence)?s?|footnotes?)\\b|^(?:see|compare|also|notes?)\\s+(?:note|notes|also|[1-3]?\\s?[A-Za-z.]+\\s+\\d{1,3}:\\d{1,3}|\\d{1,3}:\\d{1,3})",
            RegexOption.IGNORE_CASE
        )

        private val songSpeakerLabels = setOf(
            "young woman", "young man", "young women of jerusalem", "friends",
            "king solomon", "solomon", "beloved", "lover", "daughters of jerusalem",
            "she", "he", "others", "chorus", "the bride", "the bridegroom", "the woman", "the man"
        )

        private val bareCrossRefAbbrevs = setOf(
            "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Neh", "Esth",
            "Ps", "Psa", "Prov", "Eccl", "Isa", "Jer", "Lam", "Ezek", "Hos",
            "Obad", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
            "Matt", "Rom", "Gal", "Eph", "Phil", "Col", "Thess", "Tim", "Phlm",
            "Heb", "Jas", "Pet", "Rev"
        )

        fun inferTranslationMeta(fileName: String): Triple<String, String, String> {
            val d = TranslationDetector.detectFromFilenameOnly(fileName)
            return Triple(d.id, d.code, d.name)
        }
    }

    /**
     * Soft starting hint from EPUB section filename (e.g. "08_RUT.xhtml", "22_SOL_01.xhtml", "41_Mark.xhtml").
     * Never locks out subsequent books if an EPUB packs multiple books in one XHTML file.
     */
    fun hintSectionTitle(title: String) {
        val base = title
            .replace(Regex("\\.(x?html?|xml|txt)$", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        val candidates = listOf(
            base.replace(Regex("^(?:(?:ot|nt|bk|book|part|sec|ch|nlt|kjv|niv|esv|mev|bible)[\\s]*)*\\d{1,3}\\s+", RegexOption.IGNORE_CASE), "").trim(),
            base.replace(Regex("^(?:0\\d+|[4-9]\\d|\\d{3,})\\s+"), "").trim(),
            base.replace(Regex("^\\d+\\s+"), "").trim(),
            base
        ).distinct()

        for (cand in candidates) {
            val directBook = BibleCanon.findByName(cand)
            if (directBook != null) {
                if (directBook.id != currentBook?.id) {
                    flushCurrentVerse()
                    currentBook = directBook
                    currentChapter = if (directBook.chapters == 1) 1 else 0
                    currentVerse = 0
                    expectingImplicitVerseOne = (directBook.chapters == 1)
                    lastLineWasExplicitRef = false
                    sawMarkedVersesInCurrentBook = false
                }
                return
            }

            val m = filenameBookChapterRegex.matchEntire(cand)
            if (m != null) {
                val book = BibleCanon.findByName(m.groupValues[1])
                val ch = m.groupValues[2].toIntOrNull()
                if (book != null && ch != null && ch in 1..book.chapters) {
                    flushCurrentVerse()
                    if (book.id != currentBook?.id) sawMarkedVersesInCurrentBook = false
                    currentBook = book
                    currentChapter = ch
                    currentVerse = 0
                    expectingImplicitVerseOne = true
                    lastLineWasExplicitRef = false
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

    private fun exitVerseForStudyNote() {
        flushCurrentVerse()
        currentVerse = 0
        expectingImplicitVerseOne = false
    }

    private fun processLine(rawLine: String) {
        if (rawLine.isEmpty()) return

        val isHtmlHeading = rawLine.startsWith("⟦H⟧")
        val line = if (isHtmlHeading) rawLine.removePrefix("⟦H⟧").trim() else rawLine
        if (line.isEmpty()) return

        if (line.length <= 4 && line.all { it == '-' || it == '*' || it == '•' || it == '—' }) return
        if (crossRefPrefixRegex.containsMatchIn(line)) {
            exitVerseForStudyNote()
            return
        }
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

        // 1. Explicit "Book Chapter:Verse text" line (e.g. "Genesis 1:1 In the beginning...")
        explicitRefRegex.matchEntire(line)?.let { m ->
            val book = BibleCanon.findByName(m.groupValues[1])
            val ch = m.groupValues[2].toIntOrNull()
            val v = m.groupValues[3].toIntOrNull()
            val body = m.groupValues[4].trim()
            if (book != null && ch != null && v != null && ch in 1..book.chapters && v in 1..176) {
                val cur = currentBook
                // Guard ONLY against isolated cross-ref quotes inside a Study Bible intro (currentChapter == 0)
                // quoting a completely different book before Chapter 1 has started!
                val isStrayIntroQuote = cur != null && currentChapter == 0 && !lastLineWasExplicitRef &&
                    book.id != cur.id && book.id != cur.id + 1
                if (!isStrayIntroQuote && !isStudyNoteOrCrossRef(body)) {
                    flushCurrentVerse()
                    if (book.id != currentBook?.id) sawMarkedVersesInCurrentBook = false
                    currentBook = book
                    currentChapter = ch
                    currentVerse = v
                    expectingImplicitVerseOne = false
                    lastLineWasExplicitRef = true
                    feedVerseSegments(body)
                }
                return
            }
        }

        // 2. Book heading line (up to 115 chars; also handles HTML headings with subtitles like "Ruth: A Story...")
        if (line.length <= 115 && !line.contains('⟦')) {
            val startsLikeTitle = line.first().isUpperCase() || line.first().isDigit()
            val endsWithSentencePunct = line.endsWith('.') || line.endsWith(',') || line.endsWith(';') ||
                line.endsWith('?') || line.endsWith('!') || line.endsWith('”') || line.endsWith('"')
            val midSentenceContinuation = !isHtmlHeading && currentVerse > 0 && currentText.isNotEmpty() && run {
                val lastCh = currentText.trimEnd().lastOrNull()
                lastCh != null && lastCh !in ".!?:”\"'’)"
            }

            val bookOnly = if (isHtmlHeading || (startsLikeTitle && !endsWithSentencePunct && !midSentenceContinuation)) {
                BibleCanon.findByFullBookTitle(line) ?: if (isHtmlHeading) {
                    val beforeSub = line.substringBefore(':').substringBefore('—').substringBefore('–').trim()
                    if (beforeSub.length in 3..60) BibleCanon.findByFullBookTitle(beforeSub) else null
                } else {
                    null
                }
            } else {
                null
            }
            if (bookOnly != null) {
                // If already reading this exact book mid-chapter and NOT an explicit HTML heading, treat as PDF running header
                if (!isHtmlHeading && currentBook?.id == bookOnly.id && currentChapter > 0 && currentVerse > 0) {
                    return
                }
                flushCurrentVerse()
                if (bookOnly.id != currentBook?.id) sawMarkedVersesInCurrentBook = false
                currentBook = bookOnly
                currentChapter = if (bookOnly.chapters == 1) 1 else 0
                currentVerse = 0
                expectingImplicitVerseOne = (bookOnly.chapters == 1)
                lastLineWasExplicitRef = false
                tentativeChapterFrom = 0
                tentativeVerseFrom = 0
                return
            }

            // "Book Chapter" heading (e.g. "Ruth 1", "Song of Songs 2", "Amos 5", "Mark 1 — The Beginning")
            if (startsLikeTitle && !midSentenceContinuation) {
                bookAndChapterRegex.matchEntire(line)?.let { m ->
                    val rawBookPart = m.groupValues[1].trim()
                    val rawNumPart = m.groupValues[2].trim()
                    val hasChapterWord = line.contains("chapter", ignoreCase = true)
                    // Never allow lowercase or bare "I" Roman numeral without "chapter" (prevents "Am I" / "He I" hijacks!)
                    val validNumToken = rawNumPart.all { it.isDigit() } ||
                        (rawNumPart == rawNumPart.uppercase() && (hasChapterWord || rawNumPart.length >= 2))
                    val book = if (validNumToken) BibleCanon.findByFullBookTitle(rawBookPart) else null
                    val ch = if (book != null) BibleCanon.parseChapterNumber(rawNumPart) else null
                    if (book != null && ch != null && ch in 1..book.chapters) {
                        // Guard against PDF running page headers repeating the current chapter
                        if (!isHtmlHeading && currentBook?.id == book.id && currentChapter == ch && currentVerse > 0) {
                            return
                        }
                        // Guard against Study Bible intro bullet lines quoting another book's chapter (e.g. "Judges 17")
                        if (!isHtmlHeading && currentBook != null && currentChapter == 0 &&
                            book.id != currentBook!!.id && book.id != currentBook!!.id + 1
                        ) {
                            return
                        }
                        flushCurrentVerse()
                        if (book.id != currentBook?.id) sawMarkedVersesInCurrentBook = false
                        currentBook = book
                        currentChapter = ch
                        currentVerse = 0
                        expectingImplicitVerseOne = true
                        lastLineWasExplicitRef = false
                        tentativeChapterFrom = 0
                        tentativeVerseFrom = 0
                        return
                    }
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

        val activeBook = currentBook ?: return

        // 3. Chapter heading ("Chapter 12", "CHAPTER XII", "PSALM 46")
        chapterHeadingRegex.matchEntire(line)?.let { m ->
            val ch = BibleCanon.parseChapterNumber(m.groupValues[1])
            if (ch != null && ch in 1..activeBook.chapters) {
                if (currentChapter == ch && currentVerse > 0) return
                flushCurrentVerse()
                currentChapter = ch
                currentVerse = 0
                expectingImplicitVerseOne = true
                lastLineWasExplicitRef = false
                tentativeChapterFrom = 0
                tentativeVerseFrom = 0
                return
            }
        }

        // 4. Standalone chapter number ("1", "2", "12")
        standaloneNumberRegex.matchEntire(line)?.let { m ->
            val num = m.groupValues[1].toIntOrNull()
            if (num != null && num in 1..activeBook.chapters) {
                if (currentChapter == 0 || num == currentChapter + 1) {
                    val prevCh = currentChapter
                    val prevV = currentVerse
                    flushCurrentVerse()
                    tentativeChapterFrom = if (prevCh > 0 && prevV > 0) prevCh else 0
                    tentativeVerseFrom = if (prevCh > 0 && prevV > 0) prevV else 0
                    currentChapter = num
                    currentVerse = 0
                    expectingImplicitVerseOne = true
                    lastLineWasExplicitRef = false
                    return
                }
            }
            return
        }

        // 5. Standalone "Chapter:Verse" on its own line
        chapterColonVerseStandaloneRegex.matchEntire(line)?.let { m ->
            if (sawMarkedVersesInCurrentBook) {
                // Study Bible note heading like "23:6" after inline-marked scripture
                exitVerseForStudyNote()
                return
            }
            val ch = m.groupValues[1].toIntOrNull()
            val v = m.groupValues[2].toIntOrNull()
            if (ch != null && v != null && ch in 1..activeBook.chapters && v in 1..176) {
                flushCurrentVerse()
                currentChapter = ch
                currentVerse = v
                expectingImplicitVerseOne = false
                lastLineWasExplicitRef = false
                tentativeChapterFrom = 0
                tentativeVerseFrom = 0
                return
            }
        }

        // 6. One or more "Chapter:Verse text" entries on a line
        if (chapterColonVerseRegex.matches(line)) {
            if (sawMarkedVersesInCurrentBook && !line.contains('⟦')) {
                // In Study Bible EPUBs, scripture uses inline markers while Study Notes use "23:6 ..."
                exitVerseForStudyNote()
                return
            }
            val hits = inlineChapterColonVerseRegex.findAll(line).toList()
            if (hits.size > 1) {
                for (i in hits.indices) {
                    val ch = hits[i].groupValues[1].toIntOrNull() ?: continue
                    val v = hits[i].groupValues[2].toIntOrNull() ?: continue
                    if (ch in 1..activeBook.chapters && v in 1..176) {
                        val segStart = hits[i].range.last + 1
                        val segEnd = if (i + 1 < hits.size) hits[i + 1].range.first else line.length
                        val body = line.substring(segStart, segEnd).trim()
                        if (isStudyNoteOrCrossRef(body)) {
                            exitVerseForStudyNote()
                            continue
                        }
                        flushCurrentVerse()
                        currentChapter = ch
                        currentVerse = v
                        expectingImplicitVerseOne = false
                        lastLineWasExplicitRef = false
                        tentativeChapterFrom = 0
                        tentativeVerseFrom = 0
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
                    if (isStudyNoteOrCrossRef(body)) {
                        exitVerseForStudyNote()
                        return
                    }
                    flushCurrentVerse()
                    currentChapter = ch
                    currentVerse = v
                    expectingImplicitVerseOne = false
                    lastLineWasExplicitRef = false
                    tentativeChapterFrom = 0
                    tentativeVerseFrom = 0
                    feedVerseSegments(body)
                    return
                }
            }
        }

        // 7. Explicit superscript/span verse markers injected by EPUB parser: ⟦1⟧ text ⟦2⟧ text
        if (line.contains('⟦')) {
            lastLineWasExplicitRef = false
            tentativeChapterFrom = 0
            tentativeVerseFrom = 0
            feedMarkedParagraph(line)
            return
        }

        // Skip HTML section subheadings inside chapters (e.g. <h3>Jesus Calms the Storm</h3>)
        if (isHtmlHeading) return

        // 8. Leading verse number ("1 In the beginning...", "16. For God...", or PDF drop-cap "12 Rehoboam went...")
        leadingVerseRegex.matchEntire(line)?.let { m ->
            val v = m.groupValues[1].toIntOrNull()
            val body = m.groupValues[2].trim()
            if (v != null && !isStudyNoteOrCrossRef(body)) {
                // Revert a speculative standalone page number if this line continues the previous chapter's verses!
                if (currentVerse == 0 && tentativeChapterFrom > 0 &&
                    v in (tentativeVerseFrom + 1)..(tentativeVerseFrom + 3)
                ) {
                    currentChapter = tentativeChapterFrom
                    currentVerse = v
                    tentativeChapterFrom = 0
                    tentativeVerseFrom = 0
                    expectingImplicitVerseOne = false
                    lastLineWasExplicitRef = false
                    feedVerseSegments(body)
                    return
                }

                // Detect PDF drop-cap chapter rollover on the same line as Verse 1:
                // e.g. at end of 1 Kings 11:43, seeing "12 Rehoboam went to Shechem..." or "12Rehoboam..."
                val isLeadingChapterDropCap = v in 2..activeBook.chapters &&
                    v == currentChapter + 1 &&
                    (currentVerse == 0 || v < currentVerse || v > currentVerse + 3) &&
                    body.length >= 10 && body.any { it.isLowerCase() }

                if (isLeadingChapterDropCap) {
                    flushCurrentVerse()
                    currentChapter = v
                    currentVerse = 1
                    expectingImplicitVerseOne = false
                    lastLineWasExplicitRef = false
                    tentativeChapterFrom = 0
                    tentativeVerseFrom = 0
                    feedVerseSegments(body)
                    return
                }

                if (isPlausibleVerseNumber(v, activeBook)) {
                    if (currentChapter == 0) {
                        currentChapter = 1
                    } else if (v == 1 && currentVerse > 1 && currentChapter < activeBook.chapters) {
                        currentChapter += 1
                    }
                    flushCurrentVerse()
                    currentVerse = v
                    expectingImplicitVerseOne = false
                    lastLineWasExplicitRef = false
                    tentativeChapterFrom = 0
                    tentativeVerseFrom = 0
                    feedVerseSegments(body)
                    return
                }
            }
        }

        // 9. Implicit Verse 1 right after a Chapter heading or Drop-Cap
        if (currentChapter > 0 && currentVerse == 0 && expectingImplicitVerseOne) {
            if (!isLikelySectionHeading(line) && line.length >= 14 && line.any { it.isLowerCase() }) {
                currentVerse = 1
                expectingImplicitVerseOne = false
                lastLineWasExplicitRef = false
                tentativeChapterFrom = 0
                tentativeVerseFrom = 0
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
        if (currentVerse == 0) return v <= 3
        if (v == 1 && currentChapter < book.chapters) return true
        return v in (currentVerse + 1)..(currentVerse + 3)
    }

    /**
     * Filters out Study Bible commentary notes and cross-reference footnotes
     * (e.g. "Ps 32:10;", "Josh", "Hos", "Isa", "(69:16; 86:17; see 18:6)", "Ps 24 The shepherd of Ps 23...").
     *
     * Note: Never flags short English words ("He", "to", "in", "Dan", "son.") on normal continuation lines.
     */
    internal fun isStudyNoteOrCrossRef(raw: String, allowSingleWordAbbrev: Boolean = false): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false

        // Bare abbreviated book citation as an entire verse/segment ("Josh", "Hos", "Isa", "Ps", "Exod", "Deut")
        if (allowSingleWordAbbrev && text.length <= 12 && !text.contains(' ')) {
            val bare = text.trimEnd('.', ';', ',')
            if (bare in bareCrossRefAbbrevs) {
                return true
            }
        }

        // Pure cross-reference line like "Ps 32:10;" or "Ps 26:1-2; 39:1;" or "1 Cor 12:4"
        if (Regex("^[1-3]?\\s?[A-Za-z]{2,12}\\.?\\s+\\d{1,3}:\\d{1,3}(?:[-–—]\\d{1,3})?(?:\\s*[;,]\\s*(?:[1-3]?\\s?[A-Za-z.]+\\s+)?\\d{1,3}:\\d{1,3}(?:[-–—]\\d{1,3})?)*\\s*;?$")
                .matches(text)
        ) {
            return true
        }

        // Parenthesized Bible chapter:verse citations never occur in actual scripture: "(104:15; 133:2)", "(7:1)", "(see 18:6)"
        if (Regex("\\(\\s*(?:see\\s+|cf\\.?\\s+|also\\s+)?(?:[1-3]?\\s?[A-Za-z.]+\\s+)?\\d{1,3}:\\d{1,3}").containsMatchIn(text)) {
            return true
        }

        // Semicolon-separated chapter:verse citation list: "32:10;" or "26:1-2; 39:1"
        if (Regex("\\b\\d{1,3}:\\d{1,3}(?:[-–—]\\d{1,3})?\\s*;").containsMatchIn(text)) {
            return true
        }

        // Abbreviated commentary citations ("Ps 23", "Ps 24", "Gen 1", "Matt 5", "1 Cor 13")
        if (Regex("\\b(?:Gen|Exod|Lev|Num|Deut|Josh|Judg|1\\s?Sam|2\\s?Sam|1\\s?Kgs|2\\s?Kgs|1\\s?Chr|2\\s?Chr|Neh|Esth|Ps|Prov|Eccl|Isa|Jer|Lam|Ezek|Hos|Mic|Nah|Hab|Zeph|Zech|Mal|Matt|Mk|Lk|Jn|Rom|1\\s?Cor|2\\s?Cor|Gal|Eph|Phil|Col|1\\s?Thess|2\\s?Thess|1\\s?Tim|2\\s?Tim|Heb|Jas|1\\s?Pet|2\\s?Pet|Rev)\\.?\\s+\\d{1,3}(?:[:\\s]|$)|\\bDan\\.?\\s+\\d{1,3}:\\d{1,3}").containsMatchIn(text)) {
            return true
        }

        // Study Bible textual footnote openings
        if (Regex("^(?:(?:Hebrew|Greek|Aramaic)\\s+(?:reads|text|literally|word|term|meaning|manuscripts|is|means)|Or\\s+[“\"]|Literally,|Some\\s+manuscripts|Other\\s+manuscripts|See\\s+note\\b|Compare\\s+)").containsMatchIn(text)) {
            return true
        }

        return false
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

        // Ignore chapter navigation grids (e.g. "⟦1⟧ ⟦2⟧ ⟦3⟧ ... ⟦50⟧" with zero words)
        if (matches.size >= 3 && line.replace(inlineVerseMarkerRegex, "").trim().isEmpty()) {
            return
        }

        val beforeFirst = line.substring(0, matches.first().range.first).trim()
        if (beforeFirst.isNotEmpty() && !isStudyNoteOrCrossRef(beforeFirst, allowSingleWordAbbrev = false)) {
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
            val nextMarkerNum = matches.getOrNull(i + 1)?.groupValues?.get(1)?.toIntOrNull()

            // Skip cross-reference footnote segments without advancing currentVerse!
            if (segText.isNotEmpty() && isStudyNoteOrCrossRef(segText, allowSingleWordAbbrev = true)) {
                exitVerseForStudyNote()
                continue
            }

            // Detect Calibre drop-cap chapter rollover across both single-paragraph and multi-paragraph chapters
            // (e.g. Ch 1 v31 -> ⟦2⟧ Verse 1 text, or Ch 23 v6 -> ⟦24⟧ Verse 1 text, or direct jump to Psalm ⟦23⟧).
            val isCalibreChapterDropCap = vNum in 1..activeBook.chapters && (
                (vNum == currentChapter + 1 && vNum >= 2 && (
                    nextMarkerNum == 2 ||
                    currentVerse == 0 ||
                    vNum < currentVerse ||
                    vNum > currentVerse + 3
                )) ||
                (currentChapter == 0 && currentVerse == 0 && vNum >= 2 && (
                    nextMarkerNum == 2 || (segText.length >= 12 && !isStudyNoteOrCrossRef(segText, allowSingleWordAbbrev = true))
                ))
            )

            if (isCalibreChapterDropCap) {
                flushCurrentVerse()
                sawMarkedVersesInCurrentBook = true
                currentChapter = vNum
                currentVerse = 0
                expectingImplicitVerseOne = true
                if (segText.isNotEmpty()) {
                    currentVerse = 1
                    expectingImplicitVerseOne = false
                    appendToCurrent(segText)
                }
                continue
            }

            // Handle the rare coincidence where chapter C had exactly C verses, so drop-cap C+1
            // was temporarily stored as (C, C+1) and now verse 2 arrives in the next paragraph!
            if (vNum == 2 && currentChapter in 1 until activeBook.chapters && currentVerse == currentChapter + 1) {
                flushCurrentVerse()
                val oldKey = (activeBook.id.toLong() shl 20) or (currentChapter.toLong() shl 10) or currentVerse.toLong()
                val promoted = verseMap.remove(oldKey)
                currentChapter += 1
                if (promoted != null) {
                    val newKey = (activeBook.id.toLong() shl 20) or (currentChapter.toLong() shl 10) or 1L
                    verseMap[newKey] = promoted.copy(chapter = currentChapter, verse = 1)
                }
                currentVerse = 2
                expectingImplicitVerseOne = false
                sawMarkedVersesInCurrentBook = true
                if (segText.isNotEmpty()) {
                    appendToCurrent(segText)
                }
                continue
            }

            if (currentChapter == 0) currentChapter = 1

            if (!isPlausibleVerseNumber(vNum, activeBook)) {
                // Ignore phantom footnote / cross-reference numbers (e.g. 7, 10, 13, 21, 28 in Psalm 23!)
                continue
            }

            if (vNum == 1 && currentVerse > 1 && currentChapter < activeBook.chapters) {
                flushCurrentVerse()
                currentChapter += 1
            } else {
                flushCurrentVerse()
            }
            sawMarkedVersesInCurrentBook = true
            currentVerse = vNum
            expectingImplicitVerseOne = false
            if (segText.isNotEmpty()) {
                appendToCurrent(segText)
            }
        }
    }

    private fun feedVerseSegments(text: String) {
        if (isStudyNoteOrCrossRef(text, allowSingleWordAbbrev = false)) {
            exitVerseForStudyNote()
            return
        }
        val matches = inlineNumericVerseRegex.findAll(text).toList()
        if (matches.isEmpty()) {
            appendToCurrent(text)
            return
        }

        var cursor = 0
        for (m in matches) {
            val candidateVerse = m.groupValues[1].toIntOrNull()
            if (candidateVerse != null && candidateVerse in (currentVerse + 1)..(currentVerse + 3)) {
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
            if (body.length >= 4 && !isStudyNoteOrCrossRef(body, allowSingleWordAbbrev = true)) {
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
