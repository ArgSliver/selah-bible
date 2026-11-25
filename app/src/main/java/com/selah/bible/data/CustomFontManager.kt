package com.selah.bible.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import com.github.khoben.libwoff2dec.Woff2Decoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Inflater

data class CustomFontEntry(
    val fileName: String,
    val displayName: String,
    val originalFormat: String
)

/**
 * Offline custom font manager supporting .otf, .ttf, .woff (WOFF 1.0), and .woff2 (WOFF 2.0).
 * - .otf / .ttf: validated and stored directly
 * - .woff: decoded in pure Kotlin (WOFF 1.0 zlib SFNT reconstruction) to .ttf/.otf
 * - .woff2: decoded via native Google Woff2Decoder to .ttf/.otf
 */
object CustomFontManager {

    private const val DIR_NAME = "custom_fonts"
    private val cache = ConcurrentHashMap<String, FontFamily>()

    private fun fontsDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    fun listCustomFonts(context: Context): List<CustomFontEntry> {
        val dir = fontsDir(context)
        val files = dir.listFiles { f ->
            f.isFile && (f.name.endsWith(".ttf", true) || f.name.endsWith(".otf", true))
        } ?: return emptyList()

        return files.sortedBy { it.name.lowercase() }.map { f ->
            val ext = f.extension.uppercase()
            CustomFontEntry(
                fileName = f.name,
                displayName = formatDisplayName(f.nameWithoutExtension),
                originalFormat = ext
            )
        }
    }

    fun loadComposeFontFamily(context: Context, fileName: String?): FontFamily? {
        if (fileName.isNullOrBlank()) return null
        cache[fileName]?.let { return it }
        val file = File(fontsDir(context), fileName)
        if (!file.exists()) return null
        return runCatching {
            val tf = Typeface.createFromFile(file)
            FontFamily(tf).also { cache[fileName] = it }
        }.getOrNull()
    }

    fun deleteFont(context: Context, fileName: String) {
        cache.remove(fileName)
        File(fontsDir(context), fileName).delete()
    }

    fun importFontFromUri(context: Context, uri: Uri, rawFileName: String): CustomFontEntry {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read selected font file.")
        if (rawBytes.size < 16) error("Font file is empty or too small.")

        val ext = rawFileName.substringAfterLast('.', "").lowercase()
        val magic = String(rawBytes, 0, 4, Charsets.ISO_8859_1)

        val sfntBytes: ByteArray = when {
            magic == "wOF2" || ext == "woff2" -> decodeWoff2(context, rawBytes)
            magic == "wOFF" || ext == "woff" -> decodeWoff1ToSfnt(rawBytes)
            isValidSfntHeader(rawBytes) || ext == "ttf" || ext == "otf" -> rawBytes
            else -> error("Unsupported font format ($ext). Please select a .ttf, .otf, .woff, or .woff2 file.")
        }

        if (!isValidSfntHeader(sfntBytes)) {
            error("Converted font did not contain valid TrueType/OpenType tables.")
        }

        val outExt = if (String(sfntBytes, 0, 4, Charsets.ISO_8859_1) == "OTTO") "otf" else "ttf"
        val baseSafe = rawFileName
            .substringBeforeLast('.')
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
            .ifEmpty { "custom_font_${System.currentTimeMillis()}" }
            .take(42)

        val targetFile = File(fontsDir(context), "$baseSafe.$outExt")
        FileOutputStream(targetFile).use { it.write(sfntBytes) }

        // Verify Android Typeface can actually render this font
        val tf = runCatching { Typeface.createFromFile(targetFile) }.getOrElse { err ->
            targetFile.delete()
            error("Android could not load this font: ${err.message}")
        }
        cache[targetFile.name] = FontFamily(tf)

        return CustomFontEntry(
            fileName = targetFile.name,
            displayName = formatDisplayName(baseSafe),
            originalFormat = ext.ifEmpty { outExt }.uppercase()
        )
    }

    private fun decodeWoff2(context: Context, woff2Bytes: ByteArray): ByteArray {
        val inTmp = File(context.cacheDir, "tmp_font.woff2")
        val outTmp = File(context.cacheDir, "tmp_font_decoded.ttf")
        try {
            inTmp.writeBytes(woff2Bytes)
            val ok = runCatching { Woff2Decoder.decodeFile(inTmp.absolutePath, outTmp.absolutePath) }
                .getOrDefault(false)
            if (ok && outTmp.exists() && outTmp.length() > 16) {
                return outTmp.readBytes()
            }
            val decoded = runCatching { Woff2Decoder.decodeBytes(woff2Bytes) }.getOrNull()
            if (decoded != null && decoded.size > 16) return decoded
            error("Failed to decode WOFF2 font file.")
        } finally {
            inTmp.delete()
            outTmp.delete()
        }
    }

    /**
     * Pure-Kotlin W3C WOFF 1.0 -> SFNT (TTF/OTF) decoder.
     * Reconstructs the SFNT offset table, table records, and inflates zlib-compressed tables.
     */
    internal fun decodeWoff1ToSfnt(woff: ByteArray): ByteArray {
        if (woff.size < 44) error("Invalid WOFF file (header too short).")
        val buf = ByteBuffer.wrap(woff).order(ByteOrder.BIG_ENDIAN)

        val signature = buf.int
        if (signature != 0x774F4646) error("Invalid WOFF 1.0 signature.") // 'wOFF'
        val flavor = buf.int // 0x00010000 for TTF, 'OTTO' for OTF
        buf.int // length
        val numTables = buf.short.toInt() and 0xFFFF
        buf.short // reserved
        buf.int // totalSfntSize
        buf.short // majorVersion
        buf.short // minorVersion
        buf.int // metaOffset
        buf.int // metaLength
        buf.int // metaOrigLength
        buf.int // privOffset
        buf.int // privLength

        if (numTables <= 0 || 44 + numTables * 20 > woff.size) {
            error("Corrupt WOFF table directory.")
        }

        data class WoffEntry(
            val tag: Int,
            val offset: Int,
            val compLength: Int,
            val origLength: Int,
            val origChecksum: Int
        )

        val entries = ArrayList<WoffEntry>(numTables)
        for (i in 0 until numTables) {
            entries.add(
                WoffEntry(
                    tag = buf.int,
                    offset = buf.int,
                    compLength = buf.int,
                    origLength = buf.int,
                    origChecksum = buf.int
                )
            )
        }

        // Compute SFNT header parameters
        var maxPow2 = 1
        var entrySelector = 0
        while (maxPow2 * 2 <= numTables) {
            maxPow2 *= 2
            entrySelector++
        }
        val searchRange = maxPow2 * 16
        val rangeShift = numTables * 16 - searchRange

        val tableDataList = ArrayList<ByteArray>(numTables)
        for (e in entries) {
            if (e.offset < 0 || e.compLength < 0 || e.offset + e.compLength > woff.size) {
                error("Corrupt WOFF table bounds.")
            }
            val rawTable = if (e.compLength < e.origLength) {
                inflateZlib(woff, e.offset, e.compLength, e.origLength)
            } else {
                woff.copyOfRange(e.offset, e.offset + e.origLength)
            }
            tableDataList.add(rawTable)
        }

        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12 + numTables * 16).order(ByteOrder.BIG_ENDIAN)
        header.putInt(flavor)
        header.putShort(numTables.toShort())
        header.putShort(searchRange.toShort())
        header.putShort(entrySelector.toShort())
        header.putShort(rangeShift.toShort())

        var currentOffset = 12 + numTables * 16
        for (i in 0 until numTables) {
            val e = entries[i]
            val data = tableDataList[i]
            header.putInt(e.tag)
            header.putInt(e.origChecksum)
            header.putInt(currentOffset)
            header.putInt(data.size)
            val paddedLen = (data.size + 3) and 3.inv()
            currentOffset += paddedLen
        }
        out.write(header.array())

        for (data in tableDataList) {
            out.write(data)
            val pad = (4 - (data.size % 4)) % 4
            repeat(pad) { out.write(0) }
        }
        return out.toByteArray()
    }

    private fun inflateZlib(src: ByteArray, offset: Int, length: Int, expectedSize: Int): ByteArray {
        val inflater = Inflater()
        return try {
            inflater.setInput(src, offset, length)
            val result = ByteArray(expectedSize)
            var filled = 0
            while (!inflater.finished() && filled < expectedSize) {
                val n = inflater.inflate(result, filled, expectedSize - filled)
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                }
                filled += n
            }
            result
        } finally {
            inflater.end()
        }
    }

    internal fun isValidSfntHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        val magic = String(bytes, 0, 4, Charsets.ISO_8859_1)
        return (b0 == 0x00 && b1 == 0x01 && b2 == 0x00 && b3 == 0x00) || // TrueType
            magic == "OTTO" || // OpenType CFF
            magic == "true" || // Apple TrueType
            magic == "typ1" ||
            magic == "ttcf"    // TrueType Collection
    }

    private fun formatDisplayName(base: String): String =
        base.replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
}
