package com.selah.bible.data.importer

import com.selah.bible.data.BibleVerse
import com.selah.bible.data.StarterKjvData

data class DetectedTranslation(
    val id: String,
    val code: String,
    val name: String
)

/**
 * Multi-stage offline Bible translation auto-detector supporting KJV, NLT, NIV, ESV, MEV,
 * 40+ major translations, and ANY custom/unknown Bible version.
 *
 * Detection order:
 * 1. Explicit filename keywords / codes (word-boundary safe)
 * 2. Embedded EPUB OPF metadata / PDF document info / copyright page text
 * 3. Signature verse fingerprinting (John 3:16, Psalm 23:1, Genesis 1:1)
 * 4. Clean title & code extraction from metadata/filename for any custom version
 */
object TranslationDetector {

    private data class KnownVersion(
        val id: String,
        val code: String,
        val name: String,
        val fullPhrases: List<String>,
        val wordCodes: List<String>,
        val copyrightHints: List<String> = emptyList()
    )

    // Order matters: more specific codes (NKJV, NRSVUE, NASB95, HCSB, NIrV) come BEFORE shorter ones (KJV, RSV, NIV, CSB)!
    private val registry: List<KnownVersion> = listOf(
        KnownVersion("nkjv", "NKJV", "New King James Version",
            listOf("new king james"), listOf("nkjv")),
        KnownVersion(StarterKjvData.TRANSLATION_ID, "KJV", "King James Version",
            listOf("king james", "authorized version", "authorised version"), listOf("kjv", "akjv", "kjva")),
        KnownVersion("nlt", "NLT", "New Living Translation",
            listOf("new living translation", "nlt study bible", "new living"), listOf("nlt"),
            listOf("tyndale house foundation", "tyndale house publishers")),
        KnownVersion("nirv", "NIrV", "New International Reader's Version",
            listOf("new international reader"), listOf("nirv")),
        KnownVersion("niv", "NIV", "New International Version",
            listOf("new international version", "niv study bible"), listOf("niv", "niv84", "niv11"),
            listOf("biblica, inc", "international bible society")),
        KnownVersion("esv", "ESV", "English Standard Version",
            listOf("english standard version", "esv study bible"), listOf("esv"),
            listOf("crossway bibles", "good news publishers")),
        KnownVersion("mev", "MEV", "Modern English Version",
            listOf("modern english version", "mev study bible"), listOf("mev"),
            listOf("military bible association", "passio", "charisma house")),
        KnownVersion("nasb95", "NASB95", "New American Standard Bible (1995)",
            listOf("new american standard bible 1995", "nasb 1995"), listOf("nasb95", "nasb1995")),
        KnownVersion("nasb", "NASB", "New American Standard Bible",
            listOf("new american standard"), listOf("nasb", "nas"),
            listOf("lockman foundation")),
        KnownVersion("lsb", "LSB", "Legacy Standard Bible",
            listOf("legacy standard bible"), listOf("lsb")),
        KnownVersion("hcsb", "HCSB", "Holman Christian Standard Bible",
            listOf("holman christian standard"), listOf("hcsb")),
        KnownVersion("csb", "CSB", "Christian Standard Bible",
            listOf("christian standard bible"), listOf("csb"),
            listOf("holman bible publishers")),
        KnownVersion("amp", "AMP", "Amplified Bible",
            listOf("amplified bible"), listOf("amp", "ampc")),
        KnownVersion("msg", "MSG", "The Message",
            listOf("the message bible", "the message: the bible in contemporary language"), listOf("msg"),
            listOf("eugene h. peterson", "navpress")),
        KnownVersion("net", "NET", "New English Translation",
            listOf("new english translation", "net bible"), listOf("net")),
        KnownVersion("nrsvue", "NRSVue", "New Revised Standard Version Updated Edition",
            listOf("new revised standard version updated"), listOf("nrsvue")),
        KnownVersion("nrsv", "NRSV", "New Revised Standard Version",
            listOf("new revised standard"), listOf("nrsv")),
        KnownVersion("rsv", "RSV", "Revised Standard Version",
            listOf("revised standard version"), listOf("rsv", "rsvce")),
        KnownVersion("asv", "ASV", "American Standard Version",
            listOf("american standard version"), listOf("asv")),
        KnownVersion("bsb", "BSB", "Berean Standard Bible",
            listOf("berean standard bible", "berean study bible"), listOf("bsb")),
        KnownVersion("web", "WEB", "World English Bible",
            listOf("world english bible"), listOf("web", "webbe")),
        KnownVersion("cev", "CEV", "Contemporary English Version",
            listOf("contemporary english version"), listOf("cev")),
        KnownVersion("ceb", "CEB", "Common English Bible",
            listOf("common english bible"), listOf("ceb")),
        KnownVersion("gnt", "GNT", "Good News Translation",
            listOf("good news translation", "good news bible", "today's english version"), listOf("gnt", "gnb", "tev")),
        KnownVersion("erv", "ERV", "Easy-to-Read Version",
            listOf("easy-to-read version", "easy to read version"), listOf("erv")),
        KnownVersion("gw", "GW", "GOD'S WORD Translation",
            listOf("god's word translation", "gods word translation"), listOf("gw", "gwt")),
        KnownVersion("icb", "ICB", "International Children's Bible",
            listOf("international children's bible"), listOf("icb")),
        KnownVersion("isv", "ISV", "International Standard Version",
            listOf("international standard version"), listOf("isv")),
        KnownVersion("tlb", "TLB", "The Living Bible",
            listOf("the living bible"), listOf("tlb")),
        KnownVersion("tpt", "TPT", "The Passion Translation",
            listOf("passion translation"), listOf("tpt")),
        KnownVersion("ylt", "YLT", "Young's Literal Translation",
            listOf("young's literal", "youngs literal"), listOf("ylt")),
        KnownVersion("dby", "DBY", "Darby Translation",
            listOf("darby translation", "darby bible"), listOf("dby", "darby")),
        KnownVersion("dra", "DRA", "Douay-Rheims Bible",
            listOf("douay-rheims", "douay rheims"), listOf("dra", "drb")),
        KnownVersion("bbe", "BBE", "Bible in Basic English",
            listOf("bible in basic english"), listOf("bbe")),
        KnownVersion("cjb", "CJB", "Complete Jewish Bible",
            listOf("complete jewish bible"), listOf("cjb")),
        KnownVersion("tlv", "TLV", "Tree of Life Version",
            listOf("tree of life version"), listOf("tlv")),
        KnownVersion("ncv", "NCV", "New Century Version",
            listOf("new century version"), listOf("ncv")),
        KnownVersion("ehv", "EHV", "Evangelical Heritage Version",
            listOf("evangelical heritage version"), listOf("ehv")),
        KnownVersion("nlv", "NLV", "New Life Version",
            listOf("new life version"), listOf("nlv")),
        KnownVersion("gnv", "GNV", "Geneva Bible",
            listOf("geneva bible"), listOf("gnv")),
        KnownVersion("rvr1960", "RVR60", "Reina-Valera 1960",
            listOf("reina-valera 1960", "reina valera 1960"), listOf("rvr60", "rvr1960", "rv1960")),
        KnownVersion("rvr", "RVR", "Reina-Valera",
            listOf("reina-valera", "reina valera"), listOf("rvr")),
        KnownVersion("nvi", "NVI", "Nueva Versión Internacional",
            listOf("nueva versión internacional", "nueva version internacional"), listOf("nvi")),
        KnownVersion("lsg", "LSG", "Louis Segond",
            listOf("louis segond"), listOf("lsg")),
        KnownVersion("lut", "LUT", "Lutherbibel",
            listOf("lutherbibel", "luther bible"), listOf("lut"))
    )

    fun detectFromFilenameOnly(fileName: String): DetectedTranslation {
        matchKnownInText(fileName, includeCopyrightHints = false)?.let { return it }
        return buildFallback(fileName, null)
    }

    fun detect(
        fileName: String,
        metadataTitle: String? = null,
        frontMatterSample: String? = null,
        verses: List<BibleVerse> = emptyList()
    ): DetectedTranslation {
        // 1. Check filename
        matchKnownInText(fileName, includeCopyrightHints = false)?.let { return it }

        // 2. Check embedded EPUB/PDF title metadata
        if (!metadataTitle.isNullOrBlank()) {
            matchKnownInText(metadataTitle, includeCopyrightHints = false)?.let { return it }
        }

        // 3. Check copyright / title page text inside the file
        if (!frontMatterSample.isNullOrBlank()) {
            matchKnownInText(frontMatterSample, includeCopyrightHints = true)?.let { return it }
        }

        // 4. Fingerprint iconic scripture verses (John 3:16, Psalm 23:1, Genesis 1:1)
        if (verses.isNotEmpty()) {
            fingerprintVerses(verses)?.let { return it }
        }

        // 5. Fallback: preserve any real embedded title or clean filename for custom/unknown versions
        return buildFallback(fileName, metadataTitle)
    }

    private fun matchKnownInText(text: String, includeCopyrightHints: Boolean): DetectedTranslation? {
        val lower = text.lowercase()
        val spaced = lower.replace(Regex("[^a-z0-9]+"), " ")

        // Full phrase match first (highest precision)
        for (v in registry) {
            if (v.fullPhrases.any { it in lower }) {
                return DetectedTranslation(v.id, v.code, v.name)
            }
        }

        // Word-boundary code match (e.g. "Bible_NIV_2011.epub" -> " bible niv 2011 epub ")
        val padded = " $spaced "
        for (v in registry) {
            if (v.wordCodes.any { " $it " in padded }) {
                return DetectedTranslation(v.id, v.code, v.name)
            }
        }

        // Publisher/copyright hints inside title/copyright pages
        if (includeCopyrightHints) {
            for (v in registry) {
                if (v.copyrightHints.any { it in lower }) {
                    return DetectedTranslation(v.id, v.code, v.name)
                }
            }
        }

        return null
    }

    private fun fingerprintVerses(verses: List<BibleVerse>): DetectedTranslation? {
        val john316 = verses.find { it.bookId == 43 && it.chapter == 3 && it.verse == 16 }?.text?.lowercase().orEmpty()
        val ps231 = verses.find { it.bookId == 19 && it.chapter == 23 && it.verse == 1 }?.text?.lowercase().orEmpty()
        val gen11 = verses.find { it.bookId == 1 && it.chapter == 1 && it.verse == 1 }?.text?.lowercase().orEmpty()

        return when {
            "this is how god loved the world" in john316 || "i have all that i need" in ps231 ->
                DetectedTranslation("nlt", "NLT", "New Living Translation")
            "whosoever believeth in him" in john316 || ("created the heaven and the earth" in gen11 && "shall not want" in ps231) ->
                DetectedTranslation(StarterKjvData.TRANSLATION_ID, "KJV", "King James Version")
            "gave his one and only son" in john316 && "i lack nothing" in ps231 ->
                DetectedTranslation("niv", "NIV", "New International Version")
            "gave his one and only son" in john316 ->
                DetectedTranslation("niv", "NIV", "New International Version")
            "for god so loved the world, that he gave his only son" in john316 && "shall not want" in ps231 ->
                DetectedTranslation("esv", "ESV", "English Standard Version")
            "for god so loved the world that he gave his only begotten son, that whoever believes in him should not perish, but have eternal life" in john316 ->
                DetectedTranslation("mev", "MEV", "Modern English Version")
            "for god so loved the world, that he gave his only begotten son, that whoever believes in him shall not perish" in john316 ->
                DetectedTranslation("nasb", "NASB", "New American Standard Bible")
            "for god loved the world in this way" in john316 ->
                DetectedTranslation("csb", "CSB", "Christian Standard Bible")
            "this is how much god loved the world" in john316 ->
                DetectedTranslation("msg", "MSG", "The Message")
            "so greatly loved and dearly prized the world" in john316 ->
                DetectedTranslation("amp", "AMP", "Amplified Bible")
            else -> null
        }
    }

    private fun buildFallback(fileName: String, metadataTitle: String?): DetectedTranslation {
        val rawTitle = metadataTitle
            ?.takeIf { it.trim().length in 2..64 && !it.equals("unknown", true) }
            ?.trim()
            ?: fileName.substringBeforeLast('.')
                .replace(Regex("[^a-zA-Z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifEmpty { "Custom Bible" }

        // If the title contains an uppercase acronym like "(XYZ)" or "XYZ Bible", prefer that acronym!
        val acronym = Regex("\\b([A-Z]{2,6})\\b")
            .findAll(rawTitle)
            .map { it.groupValues[1] }
            .firstOrNull { it !in setOf("THE", "HOLY", "BIBLE", "OLD", "NEW", "PDF", "EPUB", "STUDY") }

        val code = acronym ?: rawTitle.split(' ')
            .filter { it.isNotBlank() && it.lowercase() !in setOf("the", "of", "a", "an", "holy", "bible") }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .take(5)
            .ifEmpty { rawTitle.take(4).uppercase() }

        val id = "tr_" + rawTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(28)
        return DetectedTranslation(id = id, code = code, name = rawTitle)
    }
}
