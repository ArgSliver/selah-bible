#!/usr/bin/env python3
"""
Selah Bible — Offline EPUB / PDF / TXT / JSON -> SQLite Converter

Converts large user-owned Bible files (such as KJV or NLT EPUBs/PDFs) into a
compact, indexed .sqlite package (~3-4 MB) that imports into Selah Bible in <1s.

Usage:
    python3 convert_bible.py --input KJV.epub --code KJV --name "King James Version" --output kjv.sqlite
    python3 convert_bible.py --input NLT.pdf  --code NLT --name "New Living Translation" --output nlt.sqlite

EPUB, TXT, and JSON require zero external Python packages (stdlib only).
PDF conversion uses `pypdf` or `pymupdf` (install via `pip install -r requirements.txt`).
"""

from __future__ import annotations

import argparse
import html
import json
import os
import re
import sqlite3
import sys
import xml.etree.ElementTree as ET
import zipfile
from typing import Dict, Iterable, List, Optional, Tuple

# (book_id, canonical_name, chapter_count, aliases)
CANON: List[Tuple[int, str, int, List[str]]] = [
    (1, "Genesis", 50, ["gen", "ge", "gn", "first book of moses"]),
    (2, "Exodus", 40, ["exod", "ex", "exo", "second book of moses"]),
    (3, "Leviticus", 27, ["lev", "le", "lv", "third book of moses"]),
    (4, "Numbers", 36, ["num", "nu", "nm", "fourth book of moses"]),
    (5, "Deuteronomy", 34, ["deut", "de", "dt", "fifth book of moses"]),
    (6, "Joshua", 24, ["josh", "jos", "book of joshua"]),
    (7, "Judges", 21, ["judg", "jdg", "book of judges"]),
    (8, "Ruth", 4, ["ruth", "rth"]),
    (9, "1 Samuel", 31, ["1 sam", "1sam", "i samuel", "1st samuel", "first samuel"]),
    (10, "2 Samuel", 24, ["2 sam", "2sam", "ii samuel", "2nd samuel", "second samuel"]),
    (11, "1 Kings", 22, ["1 kgs", "1kgs", "1 ki", "i kings", "1st kings", "first kings"]),
    (12, "2 Kings", 25, ["2 kgs", "2kgs", "2 ki", "ii kings", "2nd kings", "second kings"]),
    (13, "1 Chronicles", 29, ["1 chr", "1chr", "1 chron", "i chronicles", "1st chronicles", "first chronicles"]),
    (14, "2 Chronicles", 36, ["2 chr", "2chr", "2 chron", "ii chronicles", "2nd chronicles", "second chronicles"]),
    (15, "Ezra", 10, ["ezra", "ezr"]),
    (16, "Nehemiah", 13, ["neh", "ne"]),
    (17, "Esther", 10, ["esth", "est"]),
    (18, "Job", 42, ["job", "jb"]),
    (19, "Psalms", 150, ["psalm", "psalms", "ps", "psa", "book of psalms"]),
    (20, "Proverbs", 31, ["prov", "pro", "pr", "the proverbs"]),
    (21, "Ecclesiastes", 12, ["eccl", "ecc", "ec"]),
    (22, "Song of Solomon", 8, ["song of solomon", "song of songs", "song", "sos", "canticles"]),
    (23, "Isaiah", 66, ["isa", "is"]),
    (24, "Jeremiah", 52, ["jer", "je"]),
    (25, "Lamentations", 5, ["lam", "la"]),
    (26, "Ezekiel", 48, ["ezek", "eze"]),
    (27, "Daniel", 12, ["dan", "da"]),
    (28, "Hosea", 14, ["hos", "ho"]),
    (29, "Joel", 3, ["joel", "joe"]),
    (30, "Amos", 9, ["amos", "am"]),
    (31, "Obadiah", 1, ["obad", "ob"]),
    (32, "Jonah", 4, ["jonah", "jon"]),
    (33, "Micah", 7, ["mic", "mi"]),
    (34, "Nahum", 3, ["nah", "na"]),
    (35, "Habakkuk", 3, ["hab", "hb"]),
    (36, "Zephaniah", 3, ["zeph", "zep"]),
    (37, "Haggai", 2, ["hag", "hg"]),
    (38, "Zechariah", 14, ["zech", "zec"]),
    (39, "Malachi", 4, ["mal", "ml"]),
    (40, "Matthew", 28, ["matt", "mt", "mat", "gospel according to matthew", "st matthew"]),
    (41, "Mark", 16, ["mark", "mk", "mrk", "gospel according to mark", "st mark"]),
    (42, "Luke", 24, ["luke", "lk", "luk", "gospel according to luke", "st luke"]),
    (43, "John", 21, ["john", "jn", "jhn", "gospel according to john", "st john"]),
    (44, "Acts", 28, ["acts", "act", "acts of the apostles"]),
    (45, "Romans", 16, ["rom", "ro"]),
    (46, "1 Corinthians", 16, ["1 cor", "1cor", "i corinthians", "1st corinthians", "first corinthians"]),
    (47, "2 Corinthians", 13, ["2 cor", "2cor", "ii corinthians", "2nd corinthians", "second corinthians"]),
    (48, "Galatians", 6, ["gal", "ga"]),
    (49, "Ephesians", 6, ["eph", "ep"]),
    (50, "Philippians", 4, ["phil", "php"]),
    (51, "Colossians", 4, ["col", "co"]),
    (52, "1 Thessalonians", 5, ["1 thess", "1thess", "i thessalonians", "1st thessalonians"]),
    (53, "2 Thessalonians", 3, ["2 thess", "2thess", "ii thessalonians", "2nd thessalonians"]),
    (54, "1 Timothy", 6, ["1 tim", "1tim", "i timothy", "1st timothy"]),
    (55, "2 Timothy", 4, ["2 tim", "2tim", "ii timothy", "2nd timothy"]),
    (56, "Titus", 3, ["titus", "tit"]),
    (57, "Philemon", 1, ["phlm", "phm", "philemon"]),
    (58, "Hebrews", 13, ["heb", "he"]),
    (59, "James", 5, ["jas", "jam", "james"]),
    (60, "1 Peter", 5, ["1 pet", "1pet", "i peter", "1st peter"]),
    (61, "2 Peter", 3, ["2 pet", "2pet", "ii peter", "2nd peter"]),
    (62, "1 John", 5, ["1 john", "1john", "1 jn", "i john", "1st john"]),
    (63, "2 John", 1, ["2 john", "2john", "2 jn", "ii john", "2nd john"]),
    (64, "3 John", 1, ["3 john", "3john", "3 jn", "iii john", "3rd john"]),
    (65, "Jude", 1, ["jude", "jud"]),
    (66, "Revelation", 22, ["rev", "revelations", "apocalypse", "revelation of john", "revelation of jesus christ"]),
]


def _norm(s: str) -> str:
    s = s.lower().replace(".", "").replace("'", "").replace("’", "").replace("-", " ")
    return re.sub(r"\s+", " ", s).strip()


BOOK_LOOKUP: Dict[str, Tuple[int, str, int]] = {}
for _bid, _bname, _bch, _baliases in CANON:
    for _variant in [_bname, f"book of {_bname}", f"the book of {_bname}", *_baliases]:
        BOOK_LOOKUP[_norm(_variant)] = (_bid, _bname, _bch)


def find_book(raw: str) -> Optional[Tuple[int, str, int]]:
    n = _norm(raw)
    if not n:
        return None
    if n in BOOK_LOOKUP:
        return BOOK_LOOKUP[n]
    for prefix in (
        "the ",
        "book of ",
        "gospel of ",
        "gospel according to ",
        "epistle of paul to the ",
        "letter to the ",
    ):
        if n.startswith(prefix):
            stripped = n[len(prefix):].strip()
            if stripped in BOOK_LOOKUP:
                return BOOK_LOOKUP[stripped]
    return None


EXPLICIT_REF_RE = re.compile(r"^([1-3]?\s?[A-Za-z][A-Za-z .]{1,28}?)\s+(\d{1,3}):(\d{1,3})\s+(.+)$")
CHAPTER_HEADING_RE = re.compile(r"^(?:chapter|ch\.?|psalm|ps\.?)\s+(\d{1,3})(?:\s*[:\-–—].*)?$", re.IGNORECASE)
BOOK_AND_CHAPTER_RE = re.compile(r"^([1-3]?\s?[A-Za-z][A-Za-z .]{1,28}?)\s+(?:chapter\s+)?(\d{1,3})$", re.IGNORECASE)
CH_COLON_V_RE = re.compile(r"^(\d{1,3}):(\d{1,3})\s+(.+)$")
LEADING_VERSE_RE = re.compile(r"^(\d{1,3})(?:[.)\]]|\s+)\s*(.+)$")
INLINE_MARKER_RE = re.compile(r"⟦(\d{1,3})⟧")
INLINE_NUM_VERSE_RE = re.compile(r"(?<=[.!?;:\"'”\)])\s+(\d{1,3})\s+(?=[A-Z“\"‘'\(])")
STANDALONE_NUM_RE = re.compile(r"^(\d{1,3})$")


class BibleParser:
    def __init__(self) -> None:
        self.book: Optional[Tuple[int, str, int]] = None
        self.chapter: int = 0
        self.verse: int = 0
        self.buf: List[str] = []
        self.verses: Dict[Tuple[int, int, int], str] = {}

    def hint_section(self, name: str) -> None:
        cleaned = re.sub(r"^[0-9_\-.\s]+", "", name)
        cleaned = re.sub(r"\.(x?html?|xml|txt)$", "", cleaned, flags=re.IGNORECASE)
        cleaned = cleaned.replace("_", " ").replace("-", " ").strip()
        b = find_book(cleaned)
        if b and (self.book is None or b[0] != self.book[0]):
            self.flush()
            self.book = b
            self.chapter = 1 if b[2] == 1 else 0
            self.verse = 0

    def feed_text(self, text: str) -> None:
        text = text.replace("\u00A0", " ").replace("\r\n", "\n").replace("\r", "\n")
        for line in text.split("\n"):
            self._process_line(line.strip())

    def _clean(self, raw: str) -> str:
        s = re.sub(r"⟦\d+⟧", " ", raw)
        return re.sub(r"\s+", " ", s).strip()

    def _append(self, text: str) -> None:
        c = self._clean(text)
        if c:
            self.buf.append(c)

    def flush(self) -> None:
        if self.book and 1 <= self.chapter <= self.book[2] and 1 <= self.verse <= 176 and self.buf:
            body = self._clean(" ".join(self.buf))
            if len(body) >= 2:
                self.verses[(self.book[0], self.chapter, self.verse)] = body
        self.buf.clear()

    def _is_plausible_verse(self, v: int) -> bool:
        if not (1 <= v <= 176) or self.book is None:
            return False
        if self.verse == 0:
            return v <= 5
        if v == 1 and self.chapter < self.book[2]:
            return True
        return self.verse + 1 <= v <= self.verse + 6

    def _feed_marked(self, line: str) -> None:
        matches = list(INLINE_MARKER_RE.finditer(line))
        if not matches:
            return
        before = line[: matches[0].start()].strip()
        if before and self.verse > 0:
            self._append(before)
        for i, m in enumerate(matches):
            vnum = int(m.group(1))
            seg_start = m.end()
            seg_end = matches[i + 1].start() if i + 1 < len(matches) else len(line)
            seg_text = line[seg_start:seg_end].strip()
            if self.book is None:
                continue
            if vnum == 1 and self.verse > 1 and self.chapter < self.book[2]:
                self.flush()
                self.chapter += 1
            else:
                self.flush()
            if 1 <= vnum <= 176:
                self.verse = vnum
                if seg_text:
                    self._append(seg_text)

    def _feed_segments(self, text: str) -> None:
        matches = list(INLINE_NUM_VERSE_RE.finditer(text))
        if not matches:
            self._append(text)
            return
        cursor = 0
        for m in matches:
            cand = int(m.group(1))
            if self.verse + 1 <= cand <= self.verse + 4:
                before = text[cursor : m.start()].strip()
                if before:
                    self._append(before)
                self.flush()
                self.verse = cand
                cursor = m.end()
        tail = text[cursor:].strip()
        if tail:
            self._append(tail)

    def _process_line(self, line: str) -> None:
        if not line:
            return
        m = EXPLICIT_REF_RE.match(line)
        if m:
            b = find_book(m.group(1))
            ch, v = int(m.group(2)), int(m.group(3))
            if b and 1 <= ch <= b[2] and 1 <= v <= 176:
                self.flush()
                self.book = b
                self.chapter = ch
                self.verse = v
                self._append(m.group(4))
                return

        if len(line) <= 65 and "⟦" not in line:
            b = find_book(line)
            if b:
                self.flush()
                self.book = b
                self.chapter = 1 if b[2] == 1 else 0
                self.verse = 0
                return
            m = BOOK_AND_CHAPTER_RE.match(line)
            if m:
                b = find_book(m.group(1))
                ch = int(m.group(2))
                if b and 1 <= ch <= b[2]:
                    self.flush()
                    self.book = b
                    self.chapter = ch
                    self.verse = 0
                    return

        if self.book is None:
            return

        m = CHAPTER_HEADING_RE.match(line)
        if m:
            ch = int(m.group(1))
            if 1 <= ch <= self.book[2]:
                self.flush()
                self.chapter = ch
                self.verse = 0
                return

        m = STANDALONE_NUM_RE.match(line)
        if m:
            num = int(m.group(1))
            if 1 <= num <= self.book[2] and (self.chapter == 0 or num == self.chapter + 1):
                self.flush()
                self.chapter = num
                self.verse = 0
            return

        m = CH_COLON_V_RE.match(line)
        if m:
            ch, v = int(m.group(1)), int(m.group(2))
            if 1 <= ch <= self.book[2] and 1 <= v <= 176:
                self.flush()
                self.chapter = ch
                self.verse = v
                self._feed_segments(m.group(3))
                return

        if "⟦" in line:
            if self.chapter == 0:
                self.chapter = 1
            self._feed_marked(line)
            return

        m = LEADING_VERSE_RE.match(line)
        if m:
            v = int(m.group(1))
            if self._is_plausible_verse(v):
                if self.chapter == 0:
                    self.chapter = 1
                elif v == 1 and self.verse > 1 and self.chapter < self.book[2]:
                    self.chapter += 1
                self.flush()
                self.verse = v
                self._feed_segments(m.group(2))
                return

        if self.chapter > 0 and self.verse > 0:
            # Ignore ALL-CAPS section subheadings
            if (
                4 <= len(line) <= 65
                and line.upper() == line
                and any(c.isalpha() for c in line)
                and "LORD" not in line
                and not line.endswith((".", ",", ";", ":", "?"))
            ):
                return
            self._feed_segments(line)

    def finish(self) -> List[Tuple[int, int, int, str]]:
        self.flush()
        return [(b, c, v, t) for (b, c, v), t in sorted(self.verses.items())]


SCRIPT_STYLE_RE = re.compile(r"<(script|style|head|nav|aside)\b[^>]*>.*?</\1>", re.IGNORECASE | re.DOTALL)
STUDY_NOTE_RE = re.compile(
    r"<(div|section|p)\b[^>]*class=[\"'][^\"']*(?:study[-_]?note|footnote|endnote|cross[-_]?ref|commentary|sidebar|callout|profile)[^\"']*[\"'][^>]*>.*?</\1>",
    re.IGNORECASE | re.DOTALL,
)
NOTEREF_RE = re.compile(
    r"<a\b[^>]*(?:epub:type=[\"']noteref[\"']|class=[\"'][^\"']*(?:noteref|footnote|fn|xref|crossref)[^\"']*[\"'])[^>]*>.*?</a>",
    re.IGNORECASE | re.DOTALL,
)
CHAP_DROPCAP_RE = re.compile(
    r"<(?:span|div|h[1-6]|p|b|strong)\b[^>]*class=[\"'][^\"']*(?:chap(?:ter)?[-_]?num|cnum|ch[-_]?num|dropcap|\bchap\b|\bch\b)[^\"']*[\"'][^>]*>(?:<[^>]+>)*\s*(\d{1,3})\s*(?:<[^>]+>)*</(?:span|div|h[1-6]|p|b|strong)>",
    re.IGNORECASE,
)
SUP_VERSE_RE = re.compile(
    r"<(?:sup|span|a|b|strong)\b[^>]*class=[\"'][^\"']*(?:verse[-_]?num|versenum|vnum|\bverse\b|\bv\b|\bvn\b)[^\"']*[\"'][^>]*>(?:<[^>]+>)*\s*(\d{1,3})(?:[-–—]\d{1,3})?\s*(?:<[^>]+>)*</(?:sup|span|a|b|strong)>",
    re.IGNORECASE,
)
PLAIN_SUP_RE = re.compile(r"<sup\b[^>]*>(?:<[^>]+>)*\s*(\d{1,3})(?:[-–—]\d{1,3})?\s*(?:<[^>]+>)*</sup>", re.IGNORECASE)
BLOCK_RE = re.compile(r"</?(?:p|div|h[1-6]|br|li|tr|section|article|blockquote)\b[^>]*>", re.IGNORECASE)
TAG_RE = re.compile(r"<[^>]+>")


def html_to_structured_text(raw_html: str) -> str:
    s = SCRIPT_STYLE_RE.sub("\n", raw_html)
    s = STUDY_NOTE_RE.sub("\n", s)
    s = NOTEREF_RE.sub("", s)
    s = CHAP_DROPCAP_RE.sub(r"\nCHAPTER \1\n", s)
    s = SUP_VERSE_RE.sub(r" ⟦\1⟧ ", s)
    s = PLAIN_SUP_RE.sub(r" ⟦\1⟧ ", s)
    s = BLOCK_RE.sub("\n", s)
    s = TAG_RE.sub(" ", s)
    return html.unescape(s)


def _resolve_epub_spine(zf: zipfile.ZipFile) -> List[str]:
    try:
        container_xml = zf.read("META-INF/container.xml")
    except KeyError:
        return []
    root = ET.fromstring(container_xml)
    opf_path = None
    for elem in root.iter():
        if elem.tag.endswith("rootfile") and "full-path" in elem.attrib:
            opf_path = elem.attrib["full-path"]
            break
    if not opf_path:
        return []

    base_dir = os.path.dirname(opf_path)
    opf_root = ET.fromstring(zf.read(opf_path))
    manifest: Dict[str, str] = {}
    spine_ids: List[str] = []
    for elem in opf_root.iter():
        tag = elem.tag.split("}")[-1].lower()
        if tag == "item" and "id" in elem.attrib and "href" in elem.attrib:
            manifest[elem.attrib["id"]] = elem.attrib["href"]
        elif tag == "itemref" and "idref" in elem.attrib:
            spine_ids.append(elem.attrib["idref"])

    paths: List[str] = []
    for item_id in spine_ids:
        href = manifest.get(item_id)
        if not href:
            continue
        href = href.split("#")[0]
        full = os.path.normpath(os.path.join(base_dir, href)).replace("\\", "/")
        paths.append(full)
    return paths


def parse_epub(path: str) -> List[Tuple[int, int, int, str]]:
    parser = BibleParser()
    with zipfile.ZipFile(path, "r") as zf:
        entries = _resolve_epub_spine(zf)
        if not entries:
            entries = sorted(
                n for n in zf.namelist() if n.lower().endswith((".xhtml", ".html", ".htm"))
            )
        for name in entries:
            if name not in zf.namelist():
                continue
            raw = zf.read(name).decode("utf-8", errors="replace")
            parser.hint_section(os.path.basename(name))
            parser.feed_text(html_to_structured_text(raw))
    return parser.finish()


def parse_pdf(path: str) -> List[Tuple[int, int, int, str]]:
    parser = BibleParser()
    try:
        import fitz  # PyMuPDF

        doc = fitz.open(path)
        for i, page in enumerate(doc, 1):
            parser.feed_text(page.get_text("text"))
            if i % 100 == 0:
                print(f"  Parsed {i}/{len(doc)} PDF pages ({len(parser.verses)} verses)…")
        doc.close()
        return parser.finish()
    except ImportError:
        pass

    try:
        from pypdf import PdfReader

        reader = PdfReader(path)
        total = len(reader.pages)
        for i, page in enumerate(reader.pages, 1):
            parser.feed_text(page.extract_text() or "")
            if i % 100 == 0:
                print(f"  Parsed {i}/{total} PDF pages ({len(parser.verses)} verses)…")
        return parser.finish()
    except ImportError:
        raise RuntimeError(
            "PDF conversion requires `pypdf` or `pymupdf`. Install with:\n"
            "  pip install pypdf   (or pip install pymupdf)"
        )


def parse_txt(path: str) -> List[Tuple[int, int, int, str]]:
    parser = BibleParser()
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            parser.feed_text(line)
    return parser.finish()


def parse_json(path: str) -> List[Tuple[int, int, int, str]]:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    out: List[Tuple[int, int, int, str]] = []
    if isinstance(data, list):
        for item in data:
            if isinstance(item, dict):
                if "book_id" in item:
                    out.append((int(item["book_id"]), int(item["chapter"]), int(item["verse"]), str(item["text"]).strip()))
                elif "chapters" in item and ("name" in item or "abbrev" in item):
                    b = find_book(item.get("name") or item.get("abbrev"))
                    if b:
                        for c_idx, ch_verses in enumerate(item["chapters"], 1):
                            for v_idx, v_text in enumerate(ch_verses, 1):
                                out.append((b[0], c_idx, v_idx, str(v_text).strip()))
    return out


def write_sqlite(
    output_path: str,
    translation_id: str,
    code: str,
    name: str,
    verses: Iterable[Tuple[int, int, int, str]],
) -> Tuple[int, int]:
    if os.path.exists(output_path):
        os.remove(output_path)

    conn = sqlite3.connect(output_path)
    cur = conn.cursor()
    cur.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    cur.execute(
        """
        CREATE TABLE verses (
            book_id INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse INTEGER NOT NULL,
            text TEXT NOT NULL,
            PRIMARY KEY (book_id, chapter, verse)
        )
        """
    )
    cur.execute("CREATE INDEX idx_verses_bc ON verses (book_id, chapter)")

    cur.executemany(
        "INSERT INTO meta (key, value) VALUES (?, ?)",
        [("id", translation_id), ("code", code), ("name", name)],
    )

    verse_list = list(verses)
    cur.executemany(
        "INSERT OR REPLACE INTO verses (book_id, chapter, verse, text) VALUES (?, ?, ?, ?)",
        verse_list,
    )
    conn.commit()
    cur.execute("VACUUM")
    conn.close()

    distinct_books = len({b for b, _, _, _ in verse_list})
    return len(verse_list), distinct_books


def main(argv: Optional[List[str]] = None) -> int:
    ap = argparse.ArgumentParser(
        description="Convert and combine one or more KJV/NLT EPUB/PDF/TXT/JSON files into a compact Selah SQLite Bible."
    )
    ap.add_argument(
        "--input", "-i", nargs="+", required=True,
        help="One or more .epub, .pdf, .txt, or .json files (EPUBs are merged first, PDFs fill any missing verses)"
    )
    ap.add_argument("--output", "-o", required=True, help="Output .sqlite path (e.g. kjv.sqlite or nlt.sqlite)")
    ap.add_argument("--code", "-c", required=True, help="Short code (e.g. KJV, NLT)")
    ap.add_argument("--name", "-n", required=True, help="Full display name (e.g. 'New Living Translation')")
    ap.add_argument("--id", dest="tid", default=None, help="Optional unique ID (defaults to lowercase code)")
    args = ap.parse_args(argv)

    def _priority(p: str) -> int:
        ext = os.path.splitext(p)[1].lower()
        return {".json": 0, ".epub": 1, ".txt": 2, ".pdf": 3}.get(ext, 4)

    merged: Dict[Tuple[int, int, int], str] = {}
    for path in sorted(args.input, key=_priority):
        ext = os.path.splitext(path)[1].lower()
        print(f"Converting {path} ({ext})…")
        if ext == ".epub":
            batch = parse_epub(path)
        elif ext == ".pdf":
            batch = parse_pdf(path)
        elif ext == ".json":
            batch = parse_json(path)
        else:
            batch = parse_txt(path)
        added = 0
        for b, c, v, t in batch:
            key = (b, c, v)
            if key not in merged or (len(merged[key]) < 18 and len(t) > len(merged[key])):
                merged[key] = t
                added += 1
        print(f"  -> {len(batch):,} parsed, +{added:,} added/improved (running total: {len(merged):,} verses)")

    if not merged:
        print("ERROR: No verses detected. Check that the file contains selectable text.", file=sys.stderr)
        return 1

    verses = [(b, c, v, t) for (b, c, v), t in sorted(merged.items())]
    tid = args.tid or re.sub(r"[^a-z0-9_]", "_", args.code.lower())
    count, books = write_sqlite(args.output, tid, args.code.upper(), args.name, verses)
    size_mb = os.path.getsize(args.output) / (1024 * 1024)
    print(f"SUCCESS: Wrote {count:,} verses across {books}/66 books -> {args.output} ({size_mb:.2f} MB)")
    print("Next step: Copy the .sqlite file to your phone and open Selah -> Studio -> SQLite Import!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
