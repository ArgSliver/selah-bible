# Selah Bible — Offline, Battery-First Android Bible

**Selah** is a 100% offline, privacy-first, battery-optimized Android Bible app built in native Kotlin & Jetpack Compose with ultra-lightweight `RemoteViews` home screen widgets.

Zero internet permissions. Zero background polling. Zero telemetry. Designed to keep your screen, CPU, and battery resting while you read.

---

## Install & Auto-Update with Obtainium

[![Get it on Obtainium](https://img.shields.io/badge/Get%20it%20on-Obtainium-5E35B1?style=for-the-badge&logo=android&logoColor=white)](https://obtainium.imranr.dev/)

You can install Selah Bible and receive automatic updates directly from GitHub Releases using **[Obtainium](https://github.com/ImranR98/Obtainium)**:

1. Open **Obtainium** on your Android phone and tap **Add App**.
2. Paste this repository's GitHub URL:
   ```text
   https://github.com/ArgSliver/selah-bible
   ```
3. *(Optional)* Under **Filter APKs by Regular Expression**, set `selah-bible.*\.apk` (or leave default — each release attaches a single APK).
4. Tap **Add** → **Install**.

*(Manual download: grab the latest `.apk` from the **Releases** section on the right side of this GitHub page.)*

---

## Key Features

### Battery & Offline Architecture
- **Zero Network Permissions:** `AndroidManifest.xml` declares **no** `INTERNET`, `ACCESS_NETWORK_STATE`, or `WAKE_LOCK` permissions.
- **AMOLED True Black Default:** Uses pure `#000000` backgrounds so OLED pixels physically turn off, paired with a gentle **Warm Paper** daylight mode.
- **Zero-Polling Home Screen Widgets:** Built with pure Android `AppWidgetProvider` + `RemoteViews` (no background services, no exact alarms, no `WorkManager` periodic wakeups). Widgets refresh passively on Android's `ACTION_DATE_CHANGED` broadcast or explicit user tap.
- **Indexed Local SQLite (WAL + FTS4):** Sub-millisecond chapter loading and instant full-text search across multiple full 31,102-verse Bibles with minimal RAM usage.

### Coherent, Non-Looping Verse of the Day (VOTD)
- **Never a Fixed Yearly Loop:** Uses `SecureRandom` entropy combined with persistent SQLite history (`votd_history`) so previously shown passages are excluded until the eligible canon pool is exhausted (~10+ years on a full Bible), then freshly reshuffled.
- **Coherence & Context Expansion:** Automatically filters out genealogies/census fragments and expands mid-sentence verses backward or forward (up to 3 verses) so every Verse of the Day is a complete, self-contained thought.
- **Translation-Synced:** Switching between KJV and NLT re-renders today's exact reference in your active translation.

### Daily Streaks & "Read the Bible in a Year" Challenges
- **Daily Streak Tracker:** Tracks current streak, longest streak, total chapters read, and displays a lightweight **30-day activity heatmap**.
- **5 Built-In Offline Reading Challenges:**
  1. **Bible in a Year (Canonical)** — Genesis to Revelation (all 1,189 chapters in 365 days)
  2. **Bible in a Year (Balanced Mix)** — Daily blend of Old Testament + Wisdom/Psalms + New Testament (all 1,189 chapters in 365 days)
  3. **New Testament in 90 Days** — All 260 NT chapters
  4. **Psalms & Proverbs in 60 Days** — 181 chapters of worship and wisdom
  5. **The Four Gospels in 30 Days** — Matthew, Mark, Luke, and John (89 chapters)

### Lightweight Home Screen Widgets
1. **Selah — Verse of the Day Widget:** Shows today's coherent verse, reference, optional streak badge, a one-tap refresh button to draw another coherent verse, and opens directly to that chapter in context when tapped.
2. **Selah — Favorite Verses Widget:** Showcases your favorited verses with lightweight `‹` and `›` arrows to browse favorites directly from your home screen, rotating once daily at midnight.
- Both widgets are resizable and support **5 themes** (AMOLED True Black, Warm Paper, Sepia Parchment, Slate Graphite, Translucent Dark) and **3 font sizes**.

### Deep Themes & Typography Customization
- **10 Built-in Themes:** AMOLED True Black (Default Dark), Warm Paper (Default Light), Sepia Parchment, Midnight OLED, Forest Moss, Slate Graphite, Royal Navy, Rose Dawn, Nordic Frost, and High Contrast.
- **8 Accent Color Overrides** mixable with any theme.
- **Typography Studio:** Live preview with 5 built-in font families (**Serif**, **Sans-Serif**, **OpenDyslexic**, **Monospace**, **Editorial Script**) **plus custom font imports (`.otf`, `.ttf`, `.woff`, `.woff2`)**, font size (`13sp–28sp`), line height (`1.2x–2.0x`), dyslexia-friendly letter spacing, verse-by-verse vs. flowing paragraph mode, verse number toggle, and wide book margins.

### Study Tools & Offline Backup
- **Universal Translation Support:** Auto-recognizes **KJV, NLT, NIV, ESV, MEV**, 40+ standard translations, and any custom Bible version from filenames, embedded EPUB/PDF metadata, or scripture text (with rename & delete support for every translation, including KJV Starter).
- **Multi-File EPUB + PDF Combining:** Import multiple EPUBs and PDFs together — matching versions combine cleanly to fill any missing books, chapters, or verses (and full KJV automatically finishes the built-in KJV Starter).
- **Side-by-Side Translation Comparison**, **5 Highlight Colors**, **Favorites**, **Personal Study Notes**, **Instant Offline FTS4 Search**, and **Local JSON Backup & Restore**.

---

## Loading Your KJV & NLT Bibles (EPUB / PDF)

Selah ships with a built-in public-domain **KJV Starter pack** so the app and widgets work immediately on install. Because full EPUB/PDF files are large (and translations like NLT are copyrighted), `.epub`, `.pdf`, and `.sqlite` files are excluded in `.gitignore` and loaded locally by you.

### Option 1: Direct In-App Import (Easiest)
1. Copy your `KJV.epub` / `NLT.epub` (or `.pdf`) files to your Android device's `Downloads` folder.
2. Open **Selah → Studio (Settings) → Offline Bibles**.
3. Tap **EPUB** or **PDF** and pick your file. Selah streams and indexes it directly into your local SQLite database.
4. Repeat for your second translation — then tap the `KJV` / `NLT` pill in the Reader top bar to switch instantly or compare verses side by side!

> **Tip:** EPUB files parse faster and cleaner than PDFs because they preserve clean HTML verse markup.

### Option 2: Fast Desktop Pre-Conversion (`tools/convert_bible.py`)
If your PDF/EPUB is very large or you want instant (<1 second) mobile import:

```bash
# Convert EPUB (uses Python 3 standard library — zero dependencies required!)
python3 tools/convert_bible.py \
  --input /path/to/KJV.epub \
  --code KJV \
  --name "King James Version" \
  --output kjv.sqlite

# Convert NLT EPUB or PDF (for PDF, run: pip install -r tools/requirements.txt)
python3 tools/convert_bible.py \
  --input /path/to/NLT.pdf \
  --code NLT \
  --name "New Living Translation" \
  --output nlt.sqlite
```

Then either:
- Copy `kjv.sqlite` and `nlt.sqlite` to your phone and tap **Studio → SQLite** in Selah, **OR**
- Place them in `app/src/main/assets/bibles/` before building your APK so Selah auto-imports them on first launch (ignored by Git via `.gitignore`).

---

## Building the APK

### Requirements
- JDK 17+
- Android SDK (API 35)
- Gradle 8.8+ (or use `./gradlew`)

### Build Commands
```bash
# Run unit tests (VOTD coherence, EPUB/PDF parser, 1,189-chapter reading plans)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build minified release APK (smaller footprint)
./gradlew assembleRelease
```

### Installing on Your Android Phone
- Transfer `app-debug.apk` to your phone and open it to install (enable "Install unknown apps" if prompted), **or**
- Install via USB with ADB:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

### Adding the Home Screen Widgets
1. Long-press an empty space on your Android home screen.
2. Tap **Widgets → Selah Bible**.
3. Drag **Selah — Verse of the Day** and/or **Selah — Favorite Verses** onto your home screen.
4. Customize their theme and text size anytime in **Selah → Studio → Home Screen Widgets Studio**.

---

## Project Structure

```text
selah-bible/
├── app/src/main/
│   ├── AndroidManifest.xml              # Zero network permissions; widget & date receivers
│   ├── res/layout/                      # Lightweight RemoteViews widget layouts
│   └── java/com/selah/bible/
│       ├── data/                        # SQLite + FTS4 DB, Canon, Streaks, Plans, VotdEngine
│       ├── data/importer/               # Streaming EPUB, PDF (PDFBox), and SQLite importers
│       ├── widget/                      # VOTD & Favorites AppWidgetProviders + DateChangeReceiver
│       └── ui/                          # 10-theme engine + Home, Reader, Plans, Search, Studio screens
├── tools/
│   ├── convert_bible.py                 # Desktop EPUB/PDF/TXT/JSON -> compact SQLite converter
│   └── test_convert_bible.py            # Converter tests
├── .gitignore                           # Excludes build outputs, keystores, and user Bible files
└── README.md
```

---

## License

MIT License — see [LICENSE](LICENSE). User-imported Bible translations (such as NLT) remain subject to their respective publishers' copyrights and are kept strictly local on your device.
