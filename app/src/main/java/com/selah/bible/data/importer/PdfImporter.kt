package com.selah.bible.data.importer

import android.content.Context
import android.net.Uri
import com.selah.bible.data.ImportProgress
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream

/**
 * Memory-safe, page-by-page offline PDF Bible parser using PDFBox-Android.
 * Extracts PDF metadata & copyright pages for automatic translation detection
 * (KJV, NLT, NIV, ESV, MEV, or any custom version).
 */
object PdfImporter {

    fun parseFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        overrideTranslationId: String? = null,
        onProgress: (ImportProgress) -> Unit
    ): ParsedImportBatch {
        PDFBoxResourceLoader.init(context.applicationContext)
        onProgress(ImportProgress("Preparing PDF file…", 0.04f, 0))

        val tempFile = File(context.cacheDir, "import_temp.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            } ?: error("Unable to open selected PDF file.")

            val placeholderId = overrideTranslationId ?: "pending"
            var pdfTitle: String? = null
            val frontMatter = StringBuilder()

            val rawVerses = PDDocument.load(tempFile).use { document ->
                val info = document.documentInformation
                pdfTitle = info?.title?.trim()?.takeIf { it.isNotEmpty() }
                if (!info?.subject.isNullOrBlank()) frontMatter.append(info.subject).append('\n')
                if (!info?.author.isNullOrBlank()) frontMatter.append(info.author).append('\n')

                val totalPages = document.numberOfPages.coerceAtLeast(1)

                // Sample initial pages in both content-stream order (better for 2-column Bibles)
                // and visual-position order, then pick whichever yields cleaner, fuller scripture.
                val sampleEnd = minOf(totalPages, 25)
                val useSortByPosition = chooseSortByPosition(document, sampleEnd)

                val parser = BibleTextParser(placeholderId)
                val stripper = PDFTextStripper().apply {
                    sortByPosition = useSortByPosition
                }

                for (page in 1..totalPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = stripper.getText(document)
                    if (page <= 6 && frontMatter.length < 4000) {
                        frontMatter.append(pageText.take(900)).append('\n')
                    }
                    parser.feedText(pageText)

                    if (page % 5 == 0 || page == totalPages) {
                        val frac = 0.08f + 0.84f * (page.toFloat() / totalPages)
                        onProgress(
                            ImportProgress(
                                stage = "Reading PDF page $page / $totalPages…",
                                fraction = frac,
                                versesParsed = parser.parsedCount,
                                currentBook = parser.currentBookName
                            )
                        )
                    }
                }
                parser.finish()
            }
            val detected = TranslationDetector.detect(
                fileName = fileName,
                metadataTitle = pdfTitle,
                frontMatterSample = frontMatter.toString(),
                verses = rawVerses
            )
            val finalId = overrideTranslationId ?: detected.id
            val verses = rawVerses.map { it.copy(translationId = finalId) }
            return ParsedImportBatch(detected.copy(id = finalId), verses)
        } finally {
            tempFile.delete()
        }
    }

    private fun chooseSortByPosition(document: PDDocument, sampleEnd: Int): Boolean {
        return try {
            val streamParser = BibleTextParser("sample")
            val streamStripper = PDFTextStripper().apply { sortByPosition = false }
            val posParser = BibleTextParser("sample")
            val posStripper = PDFTextStripper().apply { sortByPosition = true }

            for (page in 1..sampleEnd) {
                streamStripper.startPage = page
                streamStripper.endPage = page
                streamParser.feedText(streamStripper.getText(document))

                posStripper.startPage = page
                posStripper.endPage = page
                posParser.feedText(posStripper.getText(document))
            }

            val streamVerses = streamParser.finish()
            val posVerses = posParser.finish()
            // Prefer content-stream order on ties (preserves two-column reading order);
            // only switch to sortByPosition if it parses meaningfully more verses.
            posVerses.size > streamVerses.size + 5
        } catch (_: Exception) {
            false
        }
    }
}
