package com.selah.bible

import com.selah.bible.data.BibleCanon
import com.selah.bible.data.ReadingPlanCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPlanTest {

    @Test
    fun canonHas66BooksAnd1189Chapters() {
        assertEquals(66, BibleCanon.books.size)
        assertEquals(1189, BibleCanon.totalChapters())
    }

    @Test
    fun canonicalYearPlanCoversEveryChapterExactlyOnce() {
        val plan = ReadingPlanCatalog.getById("canonical_365")
        assertEquals(365, plan.days.size)
        val allKeys = plan.days.flatMap { d -> d.assignments.map { it.key() } }
        assertEquals(1189, allKeys.size)
        assertEquals(1189, allKeys.toSet().size)
    }

    @Test
    fun balancedYearPlanCoversEveryChapterExactlyOnce() {
        val plan = ReadingPlanCatalog.getById("balanced_365")
        assertEquals(365, plan.days.size)
        val allKeys = plan.days.flatMap { d -> d.assignments.map { it.key() } }
        assertEquals(1189, allKeys.size)
        assertEquals(1189, allKeys.toSet().size)
    }

    @Test
    fun newTestament90PlanCoversAll260NtChapters() {
        val plan = ReadingPlanCatalog.getById("nt_90")
        assertEquals(90, plan.days.size)
        val allKeys = plan.days.flatMap { d -> d.assignments.map { it.key() } }
        assertEquals(260, allKeys.size)
        assertEquals(260, allKeys.toSet().size)
    }
}
