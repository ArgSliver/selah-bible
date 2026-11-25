package com.selah.bible.data

enum class Testament { OLD, NEW }

enum class BookGenre(val votdWeight: Int) {
    PENTATEUCH(2),
    HISTORY(1),
    WISDOM(5),
    MAJOR_PROPHETS(3),
    MINOR_PROPHETS(2),
    GOSPELS(5),
    ACTS(3),
    EPISTLES(5),
    APOCALYPTIC(2)
}

data class BibleBook(
    val id: Int, // 1..66
    val name: String,
    val shortName: String,
    val chapters: Int,
    val testament: Testament,
    val genre: BookGenre,
    val aliases: List<String> = emptyList()
)

object BibleCanon {
    val books: List<BibleBook> = listOf(
        BibleBook(1, "Genesis", "Gen", 50, Testament.OLD, BookGenre.PENTATEUCH, listOf(
            "gen", "ge", "gn", "first book of moses", "first book of moses called genesis",
            "the first book of moses called genesis"
        )),
        BibleBook(2, "Exodus", "Exod", 40, Testament.OLD, BookGenre.PENTATEUCH, listOf(
            "exod", "ex", "exo", "second book of moses", "second book of moses called exodus",
            "the second book of moses called exodus"
        )),
        BibleBook(3, "Leviticus", "Lev", 27, Testament.OLD, BookGenre.PENTATEUCH, listOf(
            "lev", "le", "lv", "third book of moses", "third book of moses called leviticus",
            "the third book of moses called leviticus"
        )),
        BibleBook(4, "Numbers", "Num", 36, Testament.OLD, BookGenre.PENTATEUCH, listOf(
            "num", "nu", "nm", "nb", "fourth book of moses", "fourth book of moses called numbers",
            "the fourth book of moses called numbers"
        )),
        BibleBook(5, "Deuteronomy", "Deut", 34, Testament.OLD, BookGenre.PENTATEUCH, listOf(
            "deut", "deu", "de", "dt", "fifth book of moses", "fifth book of moses called deuteronomy",
            "the fifth book of moses called deuteronomy"
        )),
        BibleBook(6, "Joshua", "Josh", 24, Testament.OLD, BookGenre.HISTORY, listOf(
            "josh", "jos", "jsh", "book of joshua"
        )),
        BibleBook(7, "Judges", "Judg", 21, Testament.OLD, BookGenre.HISTORY, listOf(
            "judg", "jdg", "jg", "jdgs", "book of judges"
        )),
        BibleBook(8, "Ruth", "Ruth", 4, Testament.OLD, BookGenre.HISTORY, listOf(
            "ruth", "rut", "rth", "ru", "book of ruth", "the book of ruth"
        )),
        BibleBook(9, "1 Samuel", "1 Sam", 31, Testament.OLD, BookGenre.HISTORY, listOf(
            "1 sam", "1sam", "1sa", "1sm", "i samuel", "i sam", "1st samuel", "first samuel",
            "first book of samuel", "the first book of samuel",
            "first book of samuel otherwise called the first book of the kings",
            "the first book of samuel otherwise called the first book of the kings"
        )),
        BibleBook(10, "2 Samuel", "2 Sam", 24, Testament.OLD, BookGenre.HISTORY, listOf(
            "2 sam", "2sam", "2sa", "2sm", "ii samuel", "ii sam", "2nd samuel", "second samuel",
            "second book of samuel", "the second book of samuel",
            "second book of samuel otherwise called the second book of the kings",
            "the second book of samuel otherwise called the second book of the kings"
        )),
        BibleBook(11, "1 Kings", "1 Kgs", 22, Testament.OLD, BookGenre.HISTORY, listOf(
            "1 kgs", "1kgs", "1 ki", "1ki", "1kg", "i kings", "i kgs", "1st kings", "first kings",
            "first book of kings", "first book of the kings", "the first book of the kings",
            "first book of the kings commonly called the third book of the kings",
            "the first book of the kings commonly called the third book of the kings"
        )),
        BibleBook(12, "2 Kings", "2 Kgs", 25, Testament.OLD, BookGenre.HISTORY, listOf(
            "2 kgs", "2kgs", "2 ki", "2ki", "2kg", "ii kings", "ii kgs", "2nd kings", "second kings",
            "second book of kings", "second book of the kings", "the second book of the kings",
            "second book of the kings commonly called the fourth book of the kings",
            "the second book of the kings commonly called the fourth book of the kings"
        )),
        BibleBook(13, "1 Chronicles", "1 Chr", 29, Testament.OLD, BookGenre.HISTORY, listOf(
            "1 chr", "1chr", "1 chron", "1chron", "1ch", "i chronicles", "i chr", "1st chronicles",
            "first chronicles", "first book of chronicles", "first book of the chronicles",
            "the first book of the chronicles"
        )),
        BibleBook(14, "2 Chronicles", "2 Chr", 36, Testament.OLD, BookGenre.HISTORY, listOf(
            "2 chr", "2chr", "2 chron", "2chron", "2ch", "ii chronicles", "ii chr", "2nd chronicles",
            "second chronicles", "second book of chronicles", "second book of the chronicles",
            "the second book of the chronicles"
        )),
        BibleBook(15, "Ezra", "Ezra", 10, Testament.OLD, BookGenre.HISTORY, listOf("ezra", "ezr")),
        BibleBook(16, "Nehemiah", "Neh", 13, Testament.OLD, BookGenre.HISTORY, listOf("neh", "ne")),
        BibleBook(17, "Esther", "Esth", 10, Testament.OLD, BookGenre.HISTORY, listOf("esth", "est", "es")),
        BibleBook(18, "Job", "Job", 42, Testament.OLD, BookGenre.WISDOM, listOf("job", "jb")),
        BibleBook(19, "Psalms", "Ps", 150, Testament.OLD, BookGenre.WISDOM, listOf(
            "psalm", "psalms", "ps", "psa", "pss", "psm", "book of psalms", "the book of psalms"
        )),
        BibleBook(20, "Proverbs", "Prov", 31, Testament.OLD, BookGenre.WISDOM, listOf(
            "prov", "pro", "pr", "prv", "the proverbs", "book of proverbs"
        )),
        BibleBook(21, "Ecclesiastes", "Eccl", 12, Testament.OLD, BookGenre.WISDOM, listOf(
            "eccl", "ecc", "ec", "qoh", "ecclesiastes or the preacher", "the preacher"
        )),
        BibleBook(22, "Song of Solomon", "Song", 8, Testament.OLD, BookGenre.WISDOM, listOf(
            "song of solomon", "the song of solomon", "song of songs", "the song of songs",
            "song of songs song of solomon", "song of sol", "sol", "son", "ss",
            "song", "sos", "sng", "sg", "cant", "canticles", "song of solomon which is solomons"
        )),
        BibleBook(23, "Isaiah", "Isa", 66, Testament.OLD, BookGenre.MAJOR_PROPHETS, listOf(
            "isa", "is", "prophet isaiah", "book of the prophet isaiah", "the book of the prophet isaiah"
        )),
        BibleBook(24, "Jeremiah", "Jer", 52, Testament.OLD, BookGenre.MAJOR_PROPHETS, listOf(
            "jer", "je", "jr", "prophet jeremiah", "book of the prophet jeremiah", "the book of the prophet jeremiah"
        )),
        BibleBook(25, "Lamentations", "Lam", 5, Testament.OLD, BookGenre.MAJOR_PROPHETS, listOf(
            "lam", "la", "lamentations of jeremiah", "the lamentations of jeremiah"
        )),
        BibleBook(26, "Ezekiel", "Ezek", 48, Testament.OLD, BookGenre.MAJOR_PROPHETS, listOf(
            "ezek", "eze", "ezk", "prophet ezekiel", "book of the prophet ezekiel", "the book of the prophet ezekiel"
        )),
        BibleBook(27, "Daniel", "Dan", 12, Testament.OLD, BookGenre.MAJOR_PROPHETS, listOf(
            "dan", "da", "dn", "book of daniel", "book of the prophet daniel"
        )),
        BibleBook(28, "Hosea", "Hos", 14, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("hos", "ho", "book of the prophet hosea")),
        BibleBook(29, "Joel", "Joel", 3, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("joel", "jol", "joe", "jl", "book of the prophet joel")),
        BibleBook(30, "Amos", "Amos", 9, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("amos", "amo", "am", "book of amos", "book of the prophet amos")),
        BibleBook(31, "Obadiah", "Obad", 1, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("obad", "ob", "oba", "book of the prophet obadiah")),
        BibleBook(32, "Jonah", "Jonah", 4, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("jonah", "jon", "jnh", "book of the prophet jonah")),
        BibleBook(33, "Micah", "Mic", 7, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("mic", "mi", "book of the prophet micah")),
        BibleBook(34, "Nahum", "Nah", 3, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("nah", "na", "nam", "book of the prophet nahum")),
        BibleBook(35, "Habakkuk", "Hab", 3, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("hab", "hb", "book of the prophet habakkuk")),
        BibleBook(36, "Zephaniah", "Zeph", 3, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("zeph", "zep", "zp", "book of the prophet zephaniah")),
        BibleBook(37, "Haggai", "Hag", 2, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("hag", "hg", "book of the prophet haggai")),
        BibleBook(38, "Zechariah", "Zech", 14, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("zech", "zec", "zc", "book of the prophet zechariah")),
        BibleBook(39, "Malachi", "Mal", 4, Testament.OLD, BookGenre.MINOR_PROPHETS, listOf("mal", "ml", "book of the prophet malachi")),
        BibleBook(40, "Matthew", "Matt", 28, Testament.NEW, BookGenre.GOSPELS, listOf(
            "matt", "mt", "mat", "st matthew", "saint matthew",
            "gospel according to matthew", "gospel according to st matthew", "gospel according to saint matthew",
            "the gospel according to saint matthew", "the gospel according to st matthew", "the gospel of matthew",
            "good news according to matthew", "the good news according to matthew"
        )),
        BibleBook(41, "Mark", "Mark", 16, Testament.NEW, BookGenre.GOSPELS, listOf(
            "mark", "mar", "mk", "mr", "mrk", "st mark", "saint mark",
            "gospel according to mark", "gospel according to st mark", "gospel according to saint mark",
            "the gospel according to saint mark", "the gospel according to st mark", "the gospel of mark",
            "good news according to mark", "the good news according to mark"
        )),
        BibleBook(42, "Luke", "Luke", 24, Testament.NEW, BookGenre.GOSPELS, listOf(
            "luke", "lk", "lu", "luk", "st luke", "saint luke",
            "gospel according to luke", "gospel according to st luke", "gospel according to saint luke",
            "the gospel according to saint luke", "the gospel according to st luke", "the gospel of luke"
        )),
        BibleBook(43, "John", "John", 21, Testament.NEW, BookGenre.GOSPELS, listOf(
            "john", "jn", "jhn", "st john", "saint john",
            "gospel according to john", "gospel according to st john", "gospel according to saint john",
            "the gospel according to saint john", "the gospel according to st john", "the gospel of john"
        )),
        BibleBook(44, "Acts", "Acts", 28, Testament.NEW, BookGenre.ACTS, listOf(
            "acts", "act", "ac", "acts of the apostles", "the acts of the apostles"
        )),
        BibleBook(45, "Romans", "Rom", 16, Testament.NEW, BookGenre.EPISTLES, listOf(
            "rom", "ro", "rm", "epistle of paul to the romans", "epistle of paul the apostle to the romans",
            "the epistle of paul the apostle to the romans", "letter to the romans", "paul s letter to the romans"
        )),
        BibleBook(46, "1 Corinthians", "1 Cor", 16, Testament.NEW, BookGenre.EPISTLES, listOf(
            "1 cor", "1cor", "1co", "i corinthians", "i cor", "1st corinthians", "first corinthians",
            "first epistle to the corinthians", "first epistle of paul to the corinthians",
            "first epistle of paul the apostle to the corinthians",
            "the first epistle of paul the apostle to the corinthians", "first letter to the corinthians"
        )),
        BibleBook(47, "2 Corinthians", "2 Cor", 13, Testament.NEW, BookGenre.EPISTLES, listOf(
            "2 cor", "2cor", "2co", "ii corinthians", "ii cor", "2nd corinthians", "second corinthians",
            "second epistle to the corinthians", "second epistle of paul to the corinthians",
            "second epistle of paul the apostle to the corinthians",
            "the second epistle of paul the apostle to the corinthians", "second letter to the corinthians"
        )),
        BibleBook(48, "Galatians", "Gal", 6, Testament.NEW, BookGenre.EPISTLES, listOf(
            "gal", "ga", "epistle of paul the apostle to the galatians",
            "the epistle of paul the apostle to the galatians", "letter to the galatians"
        )),
        BibleBook(49, "Ephesians", "Eph", 6, Testament.NEW, BookGenre.EPISTLES, listOf(
            "eph", "ep", "epistle of paul the apostle to the ephesians",
            "the epistle of paul the apostle to the ephesians", "letter to the ephesians"
        )),
        BibleBook(50, "Philippians", "Phil", 4, Testament.NEW, BookGenre.EPISTLES, listOf(
            "phil", "php", "pp", "epistle of paul the apostle to the philippians",
            "the epistle of paul the apostle to the philippians", "letter to the philippians"
        )),
        BibleBook(51, "Colossians", "Col", 4, Testament.NEW, BookGenre.EPISTLES, listOf(
            "col", "co", "epistle of paul the apostle to the colossians",
            "the epistle of paul the apostle to the colossians", "letter to the colossians"
        )),
        BibleBook(52, "1 Thessalonians", "1 Thess", 5, Testament.NEW, BookGenre.EPISTLES, listOf(
            "1 thess", "1thess", "1 thes", "1thes", "1 th", "1th", "i thessalonians", "i thess",
            "1st thessalonians", "first thessalonians",
            "first epistle of paul the apostle to the thessalonians",
            "the first epistle of paul the apostle to the thessalonians", "first letter to the thessalonians"
        )),
        BibleBook(53, "2 Thessalonians", "2 Thess", 3, Testament.NEW, BookGenre.EPISTLES, listOf(
            "2 thess", "2thess", "2 thes", "2thes", "2 th", "2th", "ii thessalonians", "ii thess",
            "2nd thessalonians", "second thessalonians",
            "second epistle of paul the apostle to the thessalonians",
            "the second epistle of paul the apostle to the thessalonians", "second letter to the thessalonians"
        )),
        BibleBook(54, "1 Timothy", "1 Tim", 6, Testament.NEW, BookGenre.EPISTLES, listOf(
            "1 tim", "1tim", "1 ti", "1ti", "i timothy", "i tim", "1st timothy", "first timothy",
            "first epistle of paul the apostle to timothy",
            "the first epistle of paul the apostle to timothy", "first letter to timothy"
        )),
        BibleBook(55, "2 Timothy", "2 Tim", 4, Testament.NEW, BookGenre.EPISTLES, listOf(
            "2 tim", "2tim", "2 ti", "2ti", "ii timothy", "ii tim", "2nd timothy", "second timothy",
            "second epistle of paul the apostle to timothy",
            "the second epistle of paul the apostle to timothy", "second letter to timothy"
        )),
        BibleBook(56, "Titus", "Titus", 3, Testament.NEW, BookGenre.EPISTLES, listOf(
            "titus", "tit", "ti", "epistle of paul to titus", "the epistle of paul to titus",
            "epistle of paul the apostle to titus", "letter to titus"
        )),
        BibleBook(57, "Philemon", "Phlm", 1, Testament.NEW, BookGenre.EPISTLES, listOf(
            "phlm", "phm", "phile", "philemon", "epistle of paul to philemon",
            "the epistle of paul to philemon", "epistle of paul the apostle to philemon", "letter to philemon"
        )),
        BibleBook(58, "Hebrews", "Heb", 13, Testament.NEW, BookGenre.EPISTLES, listOf(
            "heb", "he", "epistle of paul the apostle to the hebrews",
            "the epistle of paul the apostle to the hebrews", "epistle to the hebrews", "letter to the hebrews"
        )),
        BibleBook(59, "James", "Jas", 5, Testament.NEW, BookGenre.EPISTLES, listOf(
            "jas", "jam", "jm", "james", "general epistle of james", "the general epistle of james",
            "epistle of james", "letter of james"
        )),
        BibleBook(60, "1 Peter", "1 Pet", 5, Testament.NEW, BookGenre.EPISTLES, listOf(
            "1 pet", "1pet", "1 pe", "1pe", "1pt", "i peter", "i pet", "1st peter", "first peter",
            "first epistle general of peter", "the first epistle general of peter",
            "first general epistle of peter", "first epistle of peter", "first letter of peter"
        )),
        BibleBook(61, "2 Peter", "2 Pet", 3, Testament.NEW, BookGenre.EPISTLES, listOf(
            "2 pet", "2pet", "2 pe", "2pe", "2pt", "ii peter", "ii pet", "2nd peter", "second peter",
            "second epistle general of peter", "the second epistle general of peter",
            "second general epistle of peter", "second epistle of peter", "second letter of peter"
        )),
        BibleBook(62, "1 John", "1 John", 5, Testament.NEW, BookGenre.EPISTLES, listOf(
            "1 john", "1john", "1 jn", "1jn", "1jo", "i john", "i jn", "1st john", "first john",
            "first epistle general of john", "the first epistle general of john",
            "first general epistle of john", "first epistle of john", "first letter of john"
        )),
        BibleBook(63, "2 John", "2 John", 1, Testament.NEW, BookGenre.EPISTLES, listOf(
            "2 john", "2john", "2 jn", "2jn", "2jo", "ii john", "ii jn", "2nd john", "second john",
            "second epistle of john", "the second epistle of john",
            "second epistle general of john", "second letter of john"
        )),
        BibleBook(64, "3 John", "3 John", 1, Testament.NEW, BookGenre.EPISTLES, listOf(
            "3 john", "3john", "3 jn", "3jn", "3jo", "iii john", "iii jn", "3rd john", "third john",
            "third epistle of john", "the third epistle of john",
            "third epistle general of john", "third letter of john"
        )),
        BibleBook(65, "Jude", "Jude", 1, Testament.NEW, BookGenre.EPISTLES, listOf(
            "jude", "jud", "general epistle of jude", "the general epistle of jude",
            "epistle of jude", "letter of jude"
        )),
        BibleBook(66, "Revelation", "Rev", 22, Testament.NEW, BookGenre.APOCALYPTIC, listOf(
            "rev", "re", "revelations", "apocalypse", "revelation of john", "revelation to john",
            "revelation of jesus christ", "the revelation of st john the divine",
            "the revelation of saint john the divine", "revelation of saint john the divine",
            "revelation of st john the divine", "book of revelation"
        ))
    )

    private val byId: Map<Int, BibleBook> = books.associateBy { it.id }

    private val normalizedLookup: Map<String, BibleBook> by lazy {
        val map = HashMap<String, BibleBook>(1024)
        for (book in books) {
            map[normalize(book.name)] = book
            map[normalize(book.shortName)] = book
            map[normalize("the ${book.name}")] = book
            map[normalize("the book of ${book.name}")] = book
            map[normalize("book of ${book.name}")] = book
            for (alias in book.aliases) {
                map[normalize(alias)] = book
            }
        }
        map
    }

    fun getById(id: Int): BibleBook = byId[id] ?: books.first()

    fun findByName(raw: String): BibleBook? {
        val norm = normalize(raw)
        if (norm.isEmpty()) return null
        normalizedLookup[norm]?.let { return it }

        // Handle titles with parenthetical alternate names like "Song of Songs (Song of Solomon)" or "Mark — Introduction"
        if ('(' in raw) {
            val beforeParen = raw.substringBefore('(').trim()
            if (beforeParen.isNotEmpty()) {
                normalizedLookup[normalize(beforeParen)]?.let { return it }
            }
            val insideParen = raw.substringAfter('(').substringBefore(')').trim()
            if (insideParen.isNotEmpty()) {
                normalizedLookup[normalize(insideParen)]?.let { return it }
            }
        }

        val prefixes = listOf(
            "the book of the prophet ",
            "book of the prophet ",
            "the book of ",
            "book of ",
            "the gospel according to saint ",
            "the gospel according to st ",
            "the gospel according to ",
            "gospel according to saint ",
            "gospel according to st ",
            "gospel according to ",
            "the gospel of ",
            "gospel of ",
            "the general epistle of ",
            "general epistle of ",
            "the epistle of paul the apostle to the ",
            "the epistle of paul the apostle to ",
            "the epistle of paul to the ",
            "the epistle of paul to ",
            "epistle of paul the apostle to the ",
            "epistle of paul the apostle to ",
            "epistle of paul to the ",
            "epistle of paul to ",
            "the epistle to the ",
            "epistle to the ",
            "the letter of paul to the ",
            "the letter to the ",
            "letter to the ",
            "the letter of ",
            "letter of ",
            "the "
        )
        for (p in prefixes) {
            if (norm.startsWith(p)) {
                val stripped = norm.removePrefix(p).trim()
                normalizedLookup[stripped]?.let { return it }
            }
        }
        return null
    }

    /**
     * Parses Roman numerals (I..CL = 1..150) used in classic KJV PDFs and EPUBs.
     */
    fun parseChapterNumber(raw: String): Int? {
        val s = raw.trim().uppercase()
        s.toIntOrNull()?.let { return it }
        if (s.isEmpty() || !s.all { it in "IVXLC" }) return null
        val values = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100)
        var total = 0
        var prev = 0
        for (ch in s.reversed()) {
            val v = values[ch] ?: return null
            if (v < prev) total -= v else total += v
            prev = v
        }
        return if (total in 1..150) total else null
    }

    fun formatReference(bookId: Int, chapter: Int, startVerse: Int, endVerse: Int = startVerse): String {
        val book = getById(bookId)
        val displayBook = if (book.id == 19) "Psalm" else book.name
        return if (endVerse > startVerse) {
            "$displayBook $chapter:$startVerse-$endVerse"
        } else {
            "$displayBook $chapter:$startVerse"
        }
    }

    fun totalChapters(): Int = books.sumOf { it.chapters }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace(Regex("[,;:.`'’\"“”()\\[\\]/\\-–—]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
