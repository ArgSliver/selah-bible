package com.selah.bible.data.importer

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.selah.bible.data.BibleVerse
import com.selah.bible.data.ImportProgress
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class ParsedImportBatch(
    val detected: DetectedTranslation,
    val verses: List<BibleVerse>
)

/**
 * Pure-Kotlin, Study-Bible-aware streaming EPUB parser.
 * - Extracts OPF <dc:title>/<dc:publisher>/<dc:rights> + frontmatter for automatic translation detection
 * - Follows OPF spine order across all 66 books
 * - Strips study notes, footnotes, cross-references, and front/back-matter
 * - Marks <h1>/<h2>/<h3> headings with ⟦H⟧, drop-cap chapters with CHAPTER N, and nested verse numbers with ⟦N⟧
 */
object EpubImporter {

    private val skippedFileKeywords = listOf(
        "cover", "copyright", "colophon", "glossary", "concordance", "dictionary",
        "maps", "timeline", "credits", "acknowledg", "dedication", "backmatter",
        "frontmatter", "studynote", "study_note", "footnote", "endnote",
        "tableofcontents", "subject_index", "topic_index", "scripture_index"
    )

    private val scriptStyleAsideRegex = Regex(
        "<(script|style|head|nav|aside)\\b[^>]*>[\\s\\S]*?</\\1>",
        RegexOption.IGNORE_CASE
    )
    private val studyNoteBlockRegex = Regex(
        "<(div|section|p)\\b[^>]*class=[\"'][^\"']*(?:study[-_]?note|footnote|endnote|cross[-_]?ref|commentary|sidebar|callout|profile|note[-_]?body|fn[-_]?text)[^\"']*[\"'][^>]*>[\\s\\S]*?</\\1>",
        RegexOption.IGNORE_CASE
    )
    private val emptyAnchorRegex = Regex(
        "<a\\b[^>]*>(?:\\s*</a>)|<a\\b[^>]*/>",
        RegexOption.IGNORE_CASE
    )
    private val noteRefLinkRegex = Regex(
        "<a\\b[^>]*(?:epub:type=[\"']noteref[\"']|href=[\"']#(?:fn|ftn|note|endnote|xref|ref)[^\"']*[\"']|class=[\"'][^\"']*(?:noteref|footnote|fn|xref|crossref)[^\"']*[\"'])[^>]*>[\\s\\S]*?</a>",
        RegexOption.IGNORE_CASE
    )
    private val inlineFootnoteCalloutRegex = Regex(
        "<a\\b[^>]*href=[^>]*>(?:<[^>]+>)*\\s*(?:[a-z*†‡§]|\\d{1,3}\\s*:\\s*\\d{1,3})\\s*(?:<[^>]+>)*</a>|<sup\\b[^>]*>\\s*[a-z*†‡§]\\s*</sup>",
        RegexOption.IGNORE_CASE
    )
    private val headingTagRegex = Regex(
        "<h([1-3])\\b[^>]*>([\\s\\S]*?)</h\\1>",
        RegexOption.IGNORE_CASE
    )
    private val chapterDropCapRegex = Regex(
        "<(?:span|div|h[1-6]|p|b|strong)\\b[^>]*class=[\"'][^\"']*(?:chap(?:ter)?(?:[-_]?(?:num|number))?|cnum|ch[-_]?num|drop[-_]?cap|\\bchap\\b|\\bch\\b|\\bcn\\b|\\bc\\b)[^\"']*[\"'][^>]*>(?:<[^>]+>)*\\s*(\\d{1,3})\\s*(?:<[^>]+>)*</(?:span|div|h[1-6]|p|b|strong)>",
        RegexOption.IGNORE_CASE
    )
    private val verseClassTagRegex = Regex(
        "<(?:sup|span|a|b|strong)\\b[^>]*class=[\"'][^\"']*(?:verse[-_]?num|versenum|vnum|\\bverse\\b|\\bv\\b|\\bvn\\b)[^\"']*[\"'][^>]*>(?:<[^>]+>)*\\s*(\\d{1,3})(?:[-–—]\\d{1,3})?\\s*(?:<[^>]+>)*</(?:sup|span|a|b|strong)>",
        RegexOption.IGNORE_CASE
    )
    private val plainSupVerseRegex = Regex(
        "<sup\\b[^>]*>(?:<[^>]+>)*\\s*(\\d{1,3})(?:[-–—]\\d{1,3})?\\s*(?:<[^>]+>)*</sup>",
        RegexOption.IGNORE_CASE
    )
    // Calibre-converted EPUBs (like NLT Study Bible) rename verse classes to generic calibreXX spans/bold tags
    private val genericInlineVerseRegex = Regex(
        "<(?:span|b|strong)\\b[^>]*>\\s*(\\d{1,3})\\s*</(?:span|b|strong)>",
        RegexOption.IGNORE_CASE
    )
    private val tableCellTags = Regex("</?(?:td|th)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val blockBreakTags = Regex(
        "</?(?:p|div|h[1-6]|br|li|tr|section|article|blockquote)\\b[^>]*>",
        RegexOption.IGNORE_CASE
    )
    private val allTagsRegex = Regex("<[^>]+>")
    private val numericEntityRegex = Regex("&#(\\d+);")
    private val hexEntityRegex = Regex("&#x([0-9a-fA-F]+);")

    private data class OpfInfo(
        val spinePaths: List<String>,
        val title: String?,
        val metadataSample: String
    )

    fun parseFromUri(
        context: Context,
        uri: Uri,
        fileName: String,
        overrideTranslationId: String? = null,
        onProgress: (ImportProgress) -> Unit
    ): ParsedImportBatch {
        onProgress(ImportProgress("Reading EPUB archive…", 0.05f, 0))

        val tempFile = File(context.cacheDir, "import_temp.epub")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            } ?: error("Unable to open selected EPUB file.")

            val placeholderId = overrideTranslationId ?: "pending"
            val parser = BibleTextParser(placeholderId)
            val frontMatterSample = StringBuilder()
            var opfTitle: String? = null

            ZipFile(tempFile).use { zip ->
                val opfInfo = resolveOpf(zip)
                opfTitle = opfInfo.title
                frontMatterSample.append(opfInfo.metadataSample).append('\n')

                val allSpine = if (opfInfo.spinePaths.isNotEmpty()) {
                    opfInfo.spinePaths
                } else {
                    zip.entries().asSequence()
                        .map { it.name }
                        .filter { it.endsWith(".xhtml", true) || it.endsWith(".html", true) || it.endsWith(".htm", true) }
                        .sorted()
                        .toList()
                }

                // Sample first few frontmatter/copyright sections for translation auto-detection
                allSpine.take(6).forEach { path ->
                    if (frontMatterSample.length < 4000) {
                        val e = zip.getEntry(path) ?: return@forEach
                        val raw = zip.getInputStream(e).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val stripped = decodeHtmlEntities(allTagsRegex.replace(scriptStyleAsideRegex.replace(raw, " "), " "))
                        frontMatterSample.append(stripped.take(900)).append('\n')
                    }
                }

                val entriesToRead = allSpine.filterNot { isNonScripturePath(it) }
                val total = entriesToRead.size.coerceAtLeast(1)
                entriesToRead.forEachIndexed { idx, entryPath ->
                    val entry = zip.getEntry(entryPath) ?: return@forEachIndexed
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    parser.hintSectionTitle(entryPath.substringAfterLast('/'))
                    val plain = htmlToStructuredText(html)
                    parser.feedText(plain)

                    if (idx % 3 == 0 || idx == total - 1) {
                        val frac = 0.08f + 0.82f * ((idx + 1).toFloat() / total)
                        onProgress(
                            ImportProgress(
                                stage = "Parsing EPUB (${idx + 1}/$total sections)…",
                                fraction = frac,
                                versesParsed = parser.parsedCount,
                                currentBook = parser.currentBookName
                            )
                        )
                    }
                }
            }

            val rawVerses = parser.finish()
            val detected = TranslationDetector.detect(
                fileName = fileName,
                metadataTitle = opfTitle,
                frontMatterSample = frontMatterSample.toString(),
                verses = rawVerses
            )
            val finalId = overrideTranslationId ?: detected.id
            val verses = rawVerses.map { it.copy(translationId = finalId) }
            return ParsedImportBatch(detected.copy(id = finalId), verses)
        } finally {
            tempFile.delete()
        }
    }

    internal fun isNonScripturePath(path: String): Boolean {
        val base = path.substringAfterLast('/').lowercase()
        val stem = base.substringBeforeLast('.')
        // Never skip Calibre's index_split_000.html / text_split_000.html chapters!
        if ("split" in stem) return false
        if (stem == "toc" || stem == "nav" || stem == "contents" || stem == "index") return true
        return skippedFileKeywords.any { it in stem }
    }

    private fun resolveOpf(zip: ZipFile): OpfInfo {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return OpfInfo(emptyList(), null, "")
        val opfPath = zip.getInputStream(containerEntry).use { stream ->
            val xpp = Xml.newPullParser()
            xpp.setInput(stream, "UTF-8")
            var event = xpp.eventType
            var found: String? = null
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && xpp.name.equals("rootfile", true)) {
                    found = xpp.getAttributeValue(null, "full-path")
                    break
                }
                event = xpp.next()
            }
            found
        } ?: return OpfInfo(emptyList(), null, "")

        val opfEntry = zip.getEntry(opfPath) ?: return OpfInfo(emptyList(), null, "")
        val baseDir = if ('/' in opfPath) opfPath.substringBeforeLast('/') + "/" else ""
        val manifest = HashMap<String, String>()
        val spineIds = ArrayList<String>()
        var dcTitle: String? = null
        val metaText = StringBuilder()

        zip.getInputStream(opfEntry).use { stream ->
            val xpp = Xml.newPullParser()
            xpp.setInput(stream, "UTF-8")
            var event = xpp.eventType
            var currentTag = ""
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = xpp.name.lowercase().substringAfter(':')
                        when (currentTag) {
                            "item" -> {
                                val id = xpp.getAttributeValue(null, "id")
                                val href = xpp.getAttributeValue(null, "href")
                                if (id != null && href != null) manifest[id] = href
                            }
                            "itemref" -> {
                                val idref = xpp.getAttributeValue(null, "idref")
                                if (idref != null) spineIds.add(idref)
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val txt = xpp.text?.trim().orEmpty()
                        if (txt.isNotEmpty()) {
                            when (currentTag) {
                                "title" -> {
                                    if (dcTitle == null) dcTitle = txt
                                    metaText.append(txt).append(' ')
                                }
                                "publisher", "rights", "description", "creator", "subject" -> {
                                    metaText.append(txt).append(' ')
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> currentTag = ""
                }
                event = xpp.next()
            }
        }

        val paths = spineIds.mapNotNull { id ->
            val href = manifest[id]?.substringBefore('#') ?: return@mapNotNull null
            normalizeZipPath(baseDir + href)
        }
        return OpfInfo(paths, dcTitle, metaText.toString())
    }

    private fun normalizeZipPath(path: String): String {
        val parts = ArrayList<String>()
        for (seg in path.split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    internal fun htmlToStructuredText(html: String): String {
        // Raw newlines in HTML source are whitespace, not paragraph breaks; collapse them first
        // so wrapped <p> paragraphs keep all their inline verse markers on one logical line.
        var s = html.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ')
        s = scriptStyleAsideRegex.replace(s, "\n")
        s = studyNoteBlockRegex.replace(s, "\n")
        s = emptyAnchorRegex.replace(s, "")
        s = noteRefLinkRegex.replace(s, "")
        s = inlineFootnoteCalloutRegex.replace(s, "")
        // Convert drop-cap chapter numbers into explicit CHAPTER lines
        s = chapterDropCapRegex.replace(s) { m -> "\nCHAPTER ${m.groupValues[1]}\n" }
        // Tag clean h1-h3 book/chapter titles with ⟦H⟧ ONLY if they don't already contain CHAPTER/verse markers
        s = headingTagRegex.replace(s) { m ->
            val rawInner = m.groupValues[2]
            if ("CHAPTER " in rawInner || "⟦" in rawInner) {
                "\n$rawInner\n"
            } else {
                val inner = allTagsRegex.replace(rawInner, "").replace(Regex("\\s+"), " ").trim()
                if (inner.isNotEmpty() && inner.length <= 100) "\n⟦H⟧$inner\n" else "\n$rawInner\n"
            }
        }
        // Convert verse tags (including nested <a>/<b> inside <sup>, class="verse", and Calibre <span>N</span>)
        s = verseClassTagRegex.replace(s) { m -> " ⟦${m.groupValues[1]}⟧ " }
        s = plainSupVerseRegex.replace(s) { m -> " ⟦${m.groupValues[1]}⟧ " }
        s = genericInlineVerseRegex.replace(s) { m -> " ⟦${m.groupValues[1]}⟧ " }
        s = tableCellTags.replace(s, " ")
        s = blockBreakTags.replace(s, "\n")
        // Strip remaining inline tags without inserting spaces so small-caps (L<span>ORD</span>) stay intact
        s = allTagsRegex.replace(s, "")
        s = decodeHtmlEntities(s)
        return s
    }

    private fun decodeHtmlEntities(input: String): String {
        var s = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
        s = numericEntityRegex.replace(s) { m ->
            val cp = m.groupValues[1].toIntOrNull()
            if (cp != null && cp in 32..0x10FFFF) String(Character.toChars(cp)) else " "
        }
        s = hexEntityRegex.replace(s) { m ->
            val cp = m.groupValues[1].toIntOrNull(16)
            if (cp != null && cp in 32..0x10FFFF) String(Character.toChars(cp)) else " "
        }
        return s
    }
}
