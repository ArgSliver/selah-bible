package com.selah.bible

import com.selah.bible.data.importer.BibleTextParser
import com.selah.bible.data.importer.EpubImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleTextParserTest {

    @Test
    fun parsesExplicitReferenceLines() {
        val parser = BibleTextParser("kjv")
        parser.feedText(
            """
            Genesis 1:1 In the beginning God created the heaven and the earth.
            Genesis 1:2 And the earth was without form, and void.
            John 3:16 For God so loved the world, that he gave his only begotten Son.
            """.trimIndent()
        )
        val verses = parser.finish()
        assertEquals(3, verses.size)
        assertEquals(1, verses[0].bookId)
        assertEquals(1, verses[0].chapter)
        assertEquals(1, verses[0].verse)
        assertEquals(43, verses[2].bookId)
        assertEquals(3, verses[2].chapter)
        assertEquals(16, verses[2].verse)
    }

    @Test
    fun parsesClassicKjvLongTitlesAndRomanNumerals() {
        val parser = BibleTextParser("kjv_full")
        parser.feedText(
            """
            The First Book of Moses, Called Genesis
            CHAPTER I
            1:1 In the beginning God created the heaven and the earth. 1:2 And the earth was without form, and void.
            The First Book of Samuel, Otherwise Called, The First Book of the Kings
            CHAPTER III
            1 And the child Samuel ministered unto the LORD before Eli.
            The Gospel According to Saint Matthew
            CHAPTER V
            3 Blessed are the poor in spirit: for theirs is the kingdom of heaven.
            The First Epistle of Paul the Apostle to the Corinthians
            CHAPTER XIII
            13 And now abideth faith, hope, charity, these three; but the greatest of these is charity.
            The General Epistle of James
            CHAPTER I
            5 If any of you lack wisdom, let him ask of God.
            The Revelation of Saint John the Divine
            CHAPTER XXI
            1 And I saw a new heaven and a new earth.
            """.trimIndent()
        )
        val verses = parser.finish()
        val bookIds = verses.map { it.bookId }.toSet()
        assertEquals(setOf(1, 9, 40, 46, 59, 66), bookIds)
        assertEquals(7, verses.size)
    }

    @Test
    fun parsesNltStudyBibleDropCapImplicitVerseOneAndStripsNotes() {
        val html = """
            <html><body>
              <h1>Psalms</h1>
              <p><span class="chapter-num">23</span>The LORD is my shepherd; I have all that I need.<a epub:type="noteref" href="#fn1">a</a>
              <sup><a href="#v2">2</a></sup>He lets me rest in green meadows; he leads me beside peaceful streams.</p>
              <aside epub:type="footnote"><p>23:1 Study note about shepherds that must NOT overwrite scripture.</p></aside>
              <div class="study-note"><p>23:2 Another study commentary block.</p></div>
            </body></html>
        """.trimIndent()

        val structured = EpubImporter.htmlToStructuredText(html)
        val parser = BibleTextParser("nlt")
        parser.feedText(structured)
        val verses = parser.finish()

        assertEquals(2, verses.size)
        assertEquals(19, verses[0].bookId)
        assertEquals(23, verses[0].chapter)
        assertEquals(1, verses[0].verse)
        assertTrue(verses[0].text.contains("shepherd"))
        assertFalse(verses[0].text.contains("Study note"))
        assertEquals(2, verses[1].verse)
        assertTrue(verses[1].text.contains("green meadows"))
    }

    @Test
    fun ignoresPdfRunningHeadersMidChapter() {
        val parser = BibleTextParser("kjv_full")
        parser.feedText(
            """
            GENESIS
            CHAPTER 1
            1 In the beginning God created the heaven and the earth.
            24  GENESIS 1:1–15
            GENESIS 1
            2 And the earth was without form, and void.
            """.trimIndent()
        )
        val verses = parser.finish()
        assertEquals(2, verses.size)
        assertEquals(1, verses[0].verse)
        assertEquals(2, verses[1].verse)
    }

    @Test
    fun parsesRuthSongOfSongsAmosAndMarkWithStudyBibleIntros() {
        val parser = BibleTextParser("nlt")

        val ruthHtml = """
            <html><body>
              <h1>Ruth</h1>
              <h2>Introduction</h2>
              <p>Judges 17:6 In those days Israel had no king; all the people did whatever seemed right in their own eyes.</p>
              <p><span class="chapter">1</span>In the days when the judges ruled in Israel, a severe famine came upon the land.
              <sup>2</sup>The man's name was Elimelech, and his wife was Naomi.</p>
            </body></html>
        """.trimIndent()
        parser.hintSectionTitle("08_RUT.xhtml")
        parser.feedText(EpubImporter.htmlToStructuredText(ruthHtml))

        val songHtml = """
            <html><body>
              <h1>Song of Songs (Song of Solomon)</h1>
              <h2>Introduction</h2>
              <p>1 Kings 4:32 He composed some 3,000 proverbs and wrote 1,005 songs.</p>
              <p><span class="chapter-number">1</span>This is Solomon's song of songs, more wonderful than any other.</p>
              <p>Young Woman</p>
              <p><sup>2</sup>Kiss me and kiss me again, for your love is sweeter than wine.</p>
            </body></html>
        """.trimIndent()
        parser.hintSectionTitle("22_SOL.xhtml")
        parser.feedText(EpubImporter.htmlToStructuredText(songHtml))

        val amosHtml = """
            <html><body>
              <h1>Amos</h1>
              <h2>Overview</h2>
              <p>2 Kings 14:25 Jeroboam II recovered the territories of Israel.</p>
              <p><span class="c">1</span>This message was given to Amos, a shepherd from the town of Tekoa in Judah.
              <sup>2</sup>This is what he saw and heard: "The LORD's voice will roar from Zion."</p>
            </body></html>
        """.trimIndent()
        parser.hintSectionTitle("30_AMO.xhtml")
        parser.feedText(EpubImporter.htmlToStructuredText(amosHtml))

        val markHtml = """
            <html><body>
              <h1>Mark</h1>
              <h2>Introduction</h2>
              <p>Isaiah 40:3 Listen! It's the voice of someone shouting, "Clear the way through the wilderness for the LORD!"</p>
              <p><span class="chapter-num">1</span>This is the Good News about Jesus the Messiah, the Son of God.
              <sup>2</sup>It began just as the prophet Isaiah had written.</p>
            </body></html>
        """.trimIndent()
        parser.hintSectionTitle("41_MAR.xhtml")
        parser.feedText(EpubImporter.htmlToStructuredText(markHtml))

        val verses = parser.finish()
        val bookIds = verses.map { it.bookId }.toSet()
        assertEquals(setOf(8, 22, 30, 41), bookIds)
        assertEquals(8, verses.size)
    }

    @Test
    fun parsesPdfWithIntrosAndH2DropCapSubtitles() {
        // 1. PDF with "Introduction" / "Outline" followed by leading verse numbers (no EPUB markers!)
        val pdfParser = BibleTextParser("nlt")
        pdfParser.feedText(
            """
            GENESIS
            Introduction
            Outline
            1 In the beginning God created the heavens and the earth.
            2 The earth was formless and empty, and darkness covered the deep waters.
            EXODUS
            Overview
            1 These are the names of the sons of Israel who moved to Egypt with their father, Jacob.
            RUTH
            Background
            1 In the days when the judges ruled in Israel, a severe famine came upon the land.
            """.trimIndent()
        )
        val pdfVerses = pdfParser.finish()
        assertEquals(setOf(1, 2, 8), pdfVerses.map { it.bookId }.toSet())
        assertEquals(4, pdfVerses.size)

        // 2. EPUB where <h2> wraps BOTH a drop-cap chapter number AND a subtitle
        val html = """
            <html><body>
              <h1>Mark: The Gospel of the Servant</h1>
              <h2><span class="drop-cap">1</span> John the Baptist Prepares the Way</h2>
              <p>This is the Good News about Jesus the Messiah, the Son of God.
              <sup>2</sup>It began just as the prophet Isaiah had written.</p>
            </body></html>
        """.trimIndent()
        val epubParser = BibleTextParser("nlt")
        epubParser.feedText(EpubImporter.htmlToStructuredText(html))
        val epubVerses = epubParser.finish()
        assertEquals(2, epubVerses.size)
        assertEquals(41, epubVerses[0].bookId)
        assertEquals(1, epubVerses[0].chapter)
        assertEquals(1, epubVerses[0].verse)
        assertEquals(2, epubVerses[1].verse)
    }

    @Test
    fun parsesCalibreNltAndFiltersStudyNotesAndCrossRefs() {
        val html = """
            <html><body>
              <h1>Psalms</h1>
              <p><span class="calibre10">23</span>The LORD is my shepherd; I have all that I need.
              <span class="calibre14">2</span>He lets me rest in green meadows; he leads me beside peaceful streams.
              <span class="calibre14">3</span>He renews my strength. He guides me along right paths, bringing honor to his name.
              <span class="calibre14">4</span>Even when I walk through the darkest valley, I will not be afraid, for you are close beside me.
              <span class="calibre14">5</span>You prepare a feast for me in the presence of my enemies.
              <span class="calibre14">6</span>Surely your goodness and unfailing love will pursue me all the days of my life.</p>
              <p>23:6 The Lord expresses his unfailing love through the goodness of his presence and other benefits that he alone provides (69:16; 86:17; 109:21; see 18:6; 61:4). Ps 24 The shepherd of Ps 23 takes possession of Zion.</p>
              <p><sup>7</sup> Ps 32:10; <sup>10</sup> Ps 26:1-2; 39:1; <sup>13</sup> Josh <sup>21</sup> Hos <sup>28</sup> Isa</p>
              <p><span class="calibre10">24</span>The earth is the LORD's, and everything in it.
              <span class="calibre14">2</span>For he laid the earth's foundation on the seas.</p>
            </body></html>
        """.trimIndent()

        val parser = BibleTextParser("nlt")
        parser.feedText(EpubImporter.htmlToStructuredText(html))
        val verses = parser.finish()

        val ps23 = verses.filter { it.bookId == 19 && it.chapter == 23 }
        val ps24 = verses.filter { it.bookId == 19 && it.chapter == 24 }

        assertEquals(listOf(1, 2, 3, 4, 5, 6), ps23.map { it.verse })
        assertTrue(ps23.first().text.contains("shepherd"))
        assertTrue(ps23.last().text.contains("goodness and unfailing love"))
        assertFalse(ps23.any { it.text.contains("69:16") || it.text.contains("Josh") || it.text.contains("Ps 32:10") })

        assertEquals(listOf(1, 2), ps24.map { it.verse })
        assertTrue(ps24.first().text.contains("earth is the LORD"))
    }

    @Test
    fun parsesMultiParagraphCalibreRolloversSmallCapsAndIndexSplitPaths() {
        assertFalse(EpubImporter.isNonScripturePath("OEBPS/index_split_001.html"))
        assertFalse(EpubImporter.isNonScripturePath("text/index_split_042.xhtml"))
        assertTrue(EpubImporter.isNonScripturePath("OEBPS/toc.xhtml"))
        assertTrue(EpubImporter.isNonScripturePath("OEBPS/nav.xhtml"))
        assertTrue(EpubImporter.isNonScripturePath("OEBPS/studynotes_01.xhtml"))

        val html = """
            <html><body>
              <h1>Genesis</h1>
              <p><a id="c1"></a><span class="calibre10">1</span>In the beginning God created the heavens and the earth.</p>
              <p><a id="v2"/><b>2</b>The earth was formless and empty, and darkness covered the deep waters.</p>
              <p><span class="calibre14">3</span>Then God said, "Let there be light," and there was light.</p>
              <p><span class="calibre14">4</span>And God saw that the light was good.</p>
              <p><b>1:1</b> Commentary on creation without any parenthesized citations.</p>
              <p>This second paragraph of commentary must not append onto verse 4.</p>
              <p><span class="calibre10">2</span>So the creation of the heavens and the earth and everything in them was completed.</p>
              <p><span class="calibre14">2</span>On the seventh day God had finished his work of creation, so he rested.</p>
              <p><span class="calibre14">3</span>And the L<span class="sc">ORD</span> God blessed the seventh day and declared it holy.</p>
            </body></html>
        """.trimIndent()

        val parser = BibleTextParser("nlt")
        parser.feedText(EpubImporter.htmlToStructuredText(html))
        val verses = parser.finish()

        val ch1 = verses.filter { it.bookId == 1 && it.chapter == 1 }
        val ch2 = verses.filter { it.bookId == 1 && it.chapter == 2 }
        assertEquals(listOf(1, 2, 3, 4), ch1.map { it.verse })
        assertFalse(ch1.last().text.contains("commentary"))
        assertEquals(listOf(1, 2, 3), ch2.map { it.verse })
        assertTrue(ch2.last().text.contains("LORD God"))
    }

    @Test
    fun parsesPdfAttachedSuperscriptsAndSeeContinuationLines() {
        val parser = BibleTextParser("nlt")
        parser.feedText(
            """
            1 JOHN
            CHAPTER 3
            1See how very much our Father loves us, for he calls us his children, and that is what we are!
            Also the people who belong to this world don't recognize that we are God's children because they don't know him.
            2Dear friends, we are already God's children, but he has not yet shown us what we will be like when Christ appears.3And all who have this eager expectation will keep themselves pure, just as he is pure.
            """.trimIndent()
        )
        val verses = parser.finish()
        assertEquals(3, verses.size)
        assertEquals(62, verses[0].bookId)
        assertEquals(3, verses[0].chapter)
        assertEquals(1, verses[0].verse)
        assertTrue(verses[0].text.startsWith("See how very much"))
        assertTrue(verses[0].text.contains("Also the people"))
        assertEquals(2, verses[1].verse)
        assertEquals(3, verses[2].verse)
        assertTrue(verses[2].text.contains("eager expectation"))
    }

    @Test
    fun infersKjvAndNltMetadataFromFilenames() {
        val (id1, code1, _) = BibleTextParser.inferTranslationMeta("NLT Study Bible.epub")
        assertEquals("nlt", id1)
        assertEquals("NLT", code1)

        val (id2, code2, _) = BibleTextParser.inferTranslationMeta("The Holy Bible_KJV.pdf")
        assertEquals("kjv", id2)
        assertEquals("KJV", code2)
    }
}
