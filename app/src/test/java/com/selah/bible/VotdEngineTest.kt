package com.selah.bible

import com.selah.bible.data.BibleVerse
import com.selah.bible.data.StarterKjvData
import com.selah.bible.data.VotdCoherence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class VotdEngineTest {

    @Test
    fun rejectsGenealogyFragments() {
        val verses = listOf(
            BibleVerse("kjv", 1, 11, 12, "And Arphaxad lived five and thirty years, and begat Salah:"),
            BibleVerse("kjv", 1, 11, 13, "And Arphaxad lived after he begat Salah four hundred and three years, and begat sons and daughters.")
        )
        assertNull(VotdCoherence.buildCoherentPassage(verses, 0))
        assertNull(VotdCoherence.buildCoherentPassage(verses, 1))
    }

    @Test
    fun expandsIncompleteClauseToCompleteCoherentThought() {
        val verses = listOf(
            BibleVerse("kjv", 45, 8, 38, "For I am persuaded, that neither death, nor life, nor angels, nor principalities, nor powers, nor things present, nor things to come,"),
            BibleVerse("kjv", 45, 8, 39, "Nor height, nor depth, nor any other creature, shall be able to separate us from the love of God, which is in Christ Jesus our Lord.")
        )
        val result = VotdCoherence.buildCoherentPassage(verses, 0)
        assertNotNull(result)
        val (loc, text) = result!!
        assertEquals(38, loc.startVerse)
        assertEquals(39, loc.endVerse)
        assertTrue(text.endsWith("Lord."))
    }

    @Test
    fun expandsBackwardWhenStartingWithDanglingContinuation() {
        val verses = listOf(
            BibleVerse("kjv", 45, 8, 38, "For I am persuaded, that neither death, nor life, nor angels, nor principalities, nor powers, nor things present, nor things to come,"),
            BibleVerse("kjv", 45, 8, 39, "Nor height, nor depth, nor any other creature, shall be able to separate us from the love of God, which is in Christ Jesus our Lord.")
        )
        val result = VotdCoherence.buildCoherentPassage(verses, 1)
        assertNotNull(result)
        assertEquals(38, result!!.first.startVerse)
        assertEquals(39, result.first.endVerse)
    }

    @Test
    fun drawsNonRepeatingCoherentVersesAcrossStarterPack() {
        val byChapter = StarterKjvData.verses
            .map { BibleVerse("kjv", it.bookId, it.chapter, it.verse, it.text) }
            .groupBy { it.bookId to it.chapter }
        val chapters = byChapter.keys.toList()
        val shown = mutableSetOf<String>()
        val rng = Random(42L)

        // Draw 40 consecutive days and confirm zero duplicates and clean punctuation
        repeat(40) {
            val pick = VotdCoherence.selectRandomCoherent(
                availableChapters = chapters,
                chapterLoader = { b, c -> byChapter[b to c].orEmpty() },
                alreadyShownKeys = shown,
                random = rng
            )
            assertNotNull("Expected a coherent verse on draw $it", pick)
            val key = VotdCoherence.historyKey(pick!!.first)
            assertTrue("Verse should not repeat in history: $key", key !in shown)
            shown.add(key)
            val lastChar = pick.second.trimEnd().last()
            assertTrue("Passage should end cleanly: ${pick.second}", lastChar == '.' || lastChar == '?' || lastChar == '!')
        }
    }
}
