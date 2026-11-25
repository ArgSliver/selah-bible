package com.selah.bible

import com.selah.bible.data.BibleVerse
import com.selah.bible.data.CustomFontManager
import com.selah.bible.data.StarterKjvData
import com.selah.bible.data.importer.TranslationDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater

class FontAndDetectorTest {

    @Test
    fun detectsNivEsvMevKjvNltAndCustomVersions() {
        val kjv = TranslationDetector.detect("The Holy Bible_KJV.epub")
        assertEquals(StarterKjvData.TRANSLATION_ID, kjv.id)
        assertEquals("KJV", kjv.code)

        val niv = TranslationDetector.detect("Holy_Bible_NIV_2011.epub")
        assertEquals("niv", niv.id)
        assertEquals("NIV", niv.code)

        val esv = TranslationDetector.detect("ESV_Study_Bible.pdf")
        assertEquals("esv", esv.id)
        assertEquals("ESV", esv.code)

        val mev = TranslationDetector.detect("Modern_English_Version_MEV.epub")
        assertEquals("mev", mev.id)
        assertEquals("MEV", mev.code)

        // Generic filename -> detected from copyright/metadata inside file!
        val mevFromMeta = TranslationDetector.detect(
            fileName = "bible_download_3.pdf",
            frontMatterSample = "Scripture taken from the Modern English Version. Copyright 2014 by Military Bible Association."
        )
        assertEquals("mev", mevFromMeta.id)
        assertEquals("MEV", mevFromMeta.code)

        val esvFromMeta = TranslationDetector.detect(
            fileName = "book.epub",
            frontMatterSample = "The Holy Bible, English Standard Version. Crossway Bibles."
        )
        assertEquals("esv", esvFromMeta.id)
        assertEquals("ESV", esvFromMeta.code)

        // Generic filename + no metadata -> detected via John 3:16 verse fingerprint!
        val nivFromVerse = TranslationDetector.detect(
            fileName = "unknown.epub",
            verses = listOf(
                BibleVerse("pending", 43, 3, 16, "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life."),
                BibleVerse("pending", 19, 23, 1, "The LORD is my shepherd, I lack nothing.")
            )
        )
        assertEquals("niv", nivFromVerse.id)
        assertEquals("NIV", nivFromVerse.code)

        // Arbitrary unknown/custom Bible version auto-generates clean code & name
        val custom = TranslationDetector.detect(
            fileName = "download.epub",
            metadataTitle = "Clear Word Paraphrase (CWP)"
        )
        assertEquals("CWP", custom.code)
        assertEquals("Clear Word Paraphrase (CWP)", custom.name)
    }

    @Test
    fun decodesWoff1ToValidSfntHeader() {
        // Build a minimal valid WOFF 1.0 archive with 1 zlib-compressed SFNT table ('head')
        val tableRaw = ByteArray(54) { it.toByte() }
        val deflater = Deflater()
        deflater.setInput(tableRaw)
        deflater.finish()
        val compBuf = ByteArray(128)
        val compLen = deflater.deflate(compBuf)
        deflater.end()
        val compressed = compBuf.copyOf(compLen)

        val woffOut = ByteArrayOutputStream()
        val hdr = ByteBuffer.allocate(44 + 20).order(ByteOrder.BIG_ENDIAN)
        hdr.putInt(0x774F4646) // 'wOFF'
        hdr.putInt(0x00010000) // TrueType flavor
        hdr.putInt(64 + compressed.size) // length
        hdr.putShort(1) // numTables
        hdr.putShort(0) // reserved
        hdr.putInt(12 + 16 + 56) // totalSfntSize
        hdr.putShort(1) // major
        hdr.putShort(0) // minor
        hdr.putInt(0); hdr.putInt(0); hdr.putInt(0) // meta
        hdr.putInt(0); hdr.putInt(0) // priv
        // 1 table directory entry
        hdr.putInt(0x68656164) // 'head'
        hdr.putInt(64) // offset
        hdr.putInt(compressed.size)
        hdr.putInt(tableRaw.size)
        hdr.putInt(0x12345678) // checksum
        woffOut.write(hdr.array())
        woffOut.write(compressed)

        val sfnt = CustomFontManager.decodeWoff1ToSfnt(woffOut.toByteArray())
        assertTrue("Decoded WOFF1 must have a valid SFNT TrueType header", CustomFontManager.isValidSfntHeader(sfnt))
        assertTrue(sfnt.size >= 12 + 16 + 54)
    }
}
