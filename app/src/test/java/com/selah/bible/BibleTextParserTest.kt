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
    fun infersKjvAndNltMetadataFromFilenames() {
        val (id1, code1, _) = BibleTextParser.inferTranslationMeta("NLT Study Bible.epub")
        assertEquals("nlt", id1)
        assertEquals("NLT", code1)

        val (id2, code2, _) = BibleTextParser.inferTranslationMeta("The Holy Bible_KJV.pdf")
        assertEquals("kjv", id2)
        assertEquals("KJV", code2)
    }
}
