package com.selah.bible.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.selah.bible.data.BibleCanon
import com.selah.bible.data.BibleVerse
import com.selah.bible.data.FontChoice
import com.selah.bible.data.ReadingLayoutMode
import com.selah.bible.data.Testament
import com.selah.bible.data.TextAlignmentChoice
import com.selah.bible.data.Translation
import com.selah.bible.data.UserSettings
import com.selah.bible.data.VerseNumberStyle
import com.selah.bible.ui.theme.LocalSelahPalette
import com.selah.bible.ui.theme.SelahThemeCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Int,
    chapter: Int,
    focusVerse: Int?,
    verses: List<BibleVerse>,
    favoritesInChapter: Set<Int>,
    highlightsInChapter: Map<Int, String>,
    notesInChapter: Map<Int, String>,
    availableBookIds: Set<Int>,
    bookChapterCounts: Map<Int, Int>,
    availableChaptersForBook: (Int) -> Set<Int>,
    translations: List<Translation>,
    settings: UserSettings,
    isChapterCompletedInPlan: Boolean,
    onNavigateChapter: (bookId: Int, chapter: Int) -> Unit,
    onSelectTranslation: (String) -> Unit,
    onToggleFavorite: (BibleVerse) -> Unit,
    onSetHighlight: (BibleVerse, String?) -> Unit,
    onSaveNote: (BibleVerse, String) -> Unit,
    onCompareVerse: (BibleVerse) -> List<Pair<Translation, String>>,
    onMarkChapterRead: (bookId: Int, chapter: Int) -> Unit,
    onUpdateSettings: ((UserSettings) -> UserSettings) -> Unit,
    onOpenImport: () -> Unit
) {
    val palette = LocalSelahPalette.current
    val context = LocalContext.current
    val currentBook = BibleCanon.getById(bookId)
    val activeTranslation = translations.find { it.id == settings.activeTranslationId }
        ?: translations.firstOrNull()
    val fontFamily = SelahThemeCatalog.rememberActiveFontFamily(settings)

    var showBookPicker by remember { mutableStateOf(false) }
    var showTranslationPicker by remember { mutableStateOf(false) }
    var showQuickStyle by remember { mutableStateOf(false) }
    var selectedVerse by remember { mutableStateOf<BibleVerse?>(null) }
    var noteDialogVerse by remember { mutableStateOf<BibleVerse?>(null) }
    var compareVerse by remember { mutableStateOf<BibleVerse?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(bookId, chapter, focusVerse, verses.size) {
        if (focusVerse != null && verses.isNotEmpty()) {
            val idx = verses.indexOfFirst { it.verse == focusVerse }
            if (idx >= 0) {
                listState.scrollToItem(idx)
            }
        } else {
            listState.scrollToItem(0)
        }
    }

    val horizontalPadding = settings.marginPreset.dp.dp
    val scriptureColor = palette.textPrimary.copy(alpha = settings.textContrastAlpha)
    val textAlign = if (settings.textAlignment == TextAlignmentChoice.JUSTIFY) TextAlign.Justify else TextAlign.Start
    val verseNumColor = when (settings.verseNumberStyle) {
        VerseNumberStyle.ACCENT -> palette.accent
        VerseNumberStyle.MUTED -> palette.textSecondary.copy(alpha = 0.72f)
        VerseNumberStyle.HIDDEN -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // Minimal top reading bar
        Surface(
            color = palette.surface,
            border = BorderStroke(0.5.dp, palette.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Book + Chapter pill
                Surface(
                    onClick = { showBookPicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = palette.surfaceAlt,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentBook.name} $chapter",
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Choose Book",
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Translation pill (KJV / NLT / etc.)
                    Surface(
                        onClick = { showTranslationPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = activeTranslation?.code ?: "KJV",
                            color = palette.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Quick font/theme studio button
                    IconButton(onClick = { showQuickStyle = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FormatSize,
                            contentDescription = "Reading Appearance",
                            tint = palette.textPrimary
                        )
                    }
                }
            }
        }

        // Scripture Content
        if (verses.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentBook.name} $chapter",
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This chapter is not in the built-in KJV Starter pack yet. Import your full KJV or NLT EPUB, PDF, or SQLite file to read all 1,189 chapters offline.",
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onOpenImport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import Full KJV / NLT", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 20.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(
                    if (settings.layoutMode == ReadingLayoutMode.VERSE_BY_VERSE) settings.verseSpacingDp.dp else 12.dp
                )
            ) {
                item(key = "chapter_header") {
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(
                            text = currentBook.name.uppercase(),
                            color = palette.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            text = "Chapter $chapter",
                            color = palette.textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily
                        )
                    }
                }

                if (settings.layoutMode == ReadingLayoutMode.PARAGRAPH) {
                    item(key = "paragraph_body") {
                        val paragraph = buildAnnotatedString {
                            verses.forEach { v ->
                                val hlColor = SelahThemeCatalog.highlightColor(
                                    highlightsInChapter[v.verse],
                                    palette.isDark
                                )
                                if (settings.verseNumberStyle != VerseNumberStyle.HIDDEN) {
                                    withStyle(
                                        SpanStyle(
                                            color = verseNumColor,
                                            fontSize = (settings.fontSizeSp * 0.65f).sp,
                                            fontWeight = if (settings.verseNumberStyle == VerseNumberStyle.ACCENT) FontWeight.Bold else FontWeight.Medium,
                                            baselineShift = BaselineShift.Superscript
                                        )
                                    ) {
                                        append("${v.verse}\u00A0")
                                    }
                                }
                                withStyle(SpanStyle(background = hlColor)) {
                                    append(v.text)
                                    append(" ")
                                }
                            }
                        }
                        Text(
                            text = paragraph,
                            color = scriptureColor,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                            letterSpacing = settings.letterSpacingEm.em,
                            textAlign = textAlign,
                            fontFamily = fontFamily
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Tip: Switch to Verse-by-Verse mode in Aa to tap individual verses for favorites, highlights, and notes.",
                            color = palette.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    items(verses, key = { it.verse }) { v ->
                        val isFav = v.verse in favoritesInChapter
                        val hlKey = highlightsInChapter[v.verse]
                        val note = notesInChapter[v.verse]
                        val hlBg = SelahThemeCatalog.highlightColor(hlKey, palette.isDark)
                        val isFocused = focusVerse == v.verse

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        hlBg != Color.Transparent -> hlBg
                                        isFocused -> palette.accent.copy(alpha = 0.12f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedVerse = v }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            val annotated = buildAnnotatedString {
                                if (settings.verseNumberStyle != VerseNumberStyle.HIDDEN) {
                                    withStyle(
                                        SpanStyle(
                                            color = verseNumColor,
                                            fontSize = (settings.fontSizeSp * 0.68f).sp,
                                            fontWeight = if (settings.verseNumberStyle == VerseNumberStyle.ACCENT) FontWeight.Bold else FontWeight.Medium,
                                            baselineShift = BaselineShift.Superscript
                                        )
                                    ) {
                                        append("${v.verse}  ")
                                    }
                                }
                                append(v.text)
                            }
                            Text(
                                text = annotated,
                                color = scriptureColor,
                                fontSize = settings.fontSizeSp.sp,
                                lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                                letterSpacing = settings.letterSpacingEm.em,
                                textAlign = textAlign,
                                fontFamily = fontFamily
                            )

                            if (isFav || note != null) {
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isFav) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Favorite,
                                                contentDescription = null,
                                                tint = palette.accent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text("Favorited", color = palette.accent, fontSize = 11.sp)
                                        }
                                    }
                                    if (note != null) {
                                        Text(
                                            text = "Note: ${note.take(48)}${if (note.length > 48) "…" else ""}",
                                            color = palette.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Mark chapter read / log streak footer
                item(key = "chapter_footer") {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onMarkChapterRead(bookId, chapter)
                            Toast.makeText(
                                context,
                                "Logged ${currentBook.shortName} $chapter • Streak updated!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isChapterCompletedInPlan) palette.surfaceAlt else palette.accent,
                            contentColor = if (isChapterCompletedInPlan) palette.accent else palette.onAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isChapterCompletedInPlan) "Chapter Completed ✓" else "Finish Chapter & Log Streak",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Bottom Prev / Next chapter bar
        Surface(
            color = palette.surface,
            border = BorderStroke(0.5.dp, palette.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (chapter > 1) {
                            onNavigateChapter(bookId, chapter - 1)
                        } else if (bookId > 1) {
                            val prevBook = BibleCanon.getById(bookId - 1)
                            onNavigateChapter(prevBook.id, prevBook.chapters)
                        }
                    }
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = palette.accent)
                    Text("Prev", color = palette.textPrimary)
                }

                Text(
                    text = "${currentBook.shortName} $chapter / ${currentBook.chapters}",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )

                TextButton(
                    onClick = {
                        if (chapter < currentBook.chapters) {
                            onNavigateChapter(bookId, chapter + 1)
                        } else if (bookId < 66) {
                            onNavigateChapter(bookId + 1, 1)
                        }
                    }
                ) {
                    Text("Next", color = palette.textPrimary)
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = palette.accent)
                }
            }
        }
    }

    // Book & Chapter Picker Bottom Sheet
    if (showBookPicker) {
        BookChapterPickerSheet(
            currentBookId = bookId,
            currentChapter = chapter,
            availableBookIds = availableBookIds,
            bookChapterCounts = bookChapterCounts,
            availableChaptersForBook = availableChaptersForBook,
            onDismiss = { showBookPicker = false },
            onSelect = { b, c ->
                showBookPicker = false
                onNavigateChapter(b, c)
            }
        )
    }

    // Translation Switcher Bottom Sheet
    if (showTranslationPicker) {
        ModalBottomSheet(
            onDismissRequest = { showTranslationPicker = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Bible Translations",
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                translations.forEach { t ->
                    val selected = t.id == settings.activeTranslationId
                    Surface(
                        onClick = {
                            onSelectTranslation(t.id)
                            showTranslationPicker = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.surfaceAlt,
                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.border)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${t.code} — ${t.name}",
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${t.bookCount} books • ${t.verseCount} verses offline",
                                    color = palette.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (selected) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = palette.accent)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        showTranslationPicker = false
                        onOpenImport()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, palette.accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Import KJV / NLT (EPUB, PDF, SQLite)", color = palette.accent)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Quick Style Bottom Sheet (quick reading appearance controls)
    if (showQuickStyle) {
        ModalBottomSheet(
            onDismissRequest = { showQuickStyle = false },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Reading Appearance", color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Text("Theme", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "clean_white", "soft_gray", "warm_paper", "sepia_parchment",
                        "neutral_dark", "amoled_neutral", "amoled_black"
                    ).forEach { id ->
                        val t = SelahThemeCatalog.themes.first { it.id == id }
                        val selected = !settings.followSystemTheme && settings.themeId == id
                        Surface(
                            onClick = { onUpdateSettings { it.copy(followSystemTheme = false, themeId = id) } },
                            shape = CircleShape,
                            color = t.background,
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.accent else t.border),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 12.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(t.accent)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Font", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(FontChoice.SERIF, FontChoice.SANS, FontChoice.OPEN_DYSLEXIC).forEach { fc ->
                        val active = settings.customFontFileName == null && settings.fontChoice == fc
                        Surface(
                            onClick = { onUpdateSettings { it.copy(fontChoice = fc, customFontFileName = null) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (fc) {
                                        FontChoice.SERIF -> "Serif"
                                        FontChoice.SANS -> "Sans"
                                        FontChoice.OPEN_DYSLEXIC -> "Dyslexic"
                                        else -> fc.label
                                    },
                                    color = if (active) palette.onAccent else palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = SelahThemeCatalog.fontFamilyFor(fc)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Font Size: ${settings.fontSizeSp.toInt()}sp", color = palette.textSecondary, fontSize = 12.sp)
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = { v -> onUpdateSettings { it.copy(fontSizeSp = v) } },
                    valueRange = 13f..28f,
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                )

                Text(
                    "Line Spacing: ${String.format("%.2f", settings.lineHeightMultiplier)}x",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = settings.lineHeightMultiplier,
                    onValueChange = { v -> onUpdateSettings { it.copy(lineHeightMultiplier = v) } },
                    valueRange = 1.2f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                )

                Text(
                    "Text Softness: ${(settings.textContrastAlpha * 100).toInt()}%",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = settings.textContrastAlpha,
                    onValueChange = { v -> onUpdateSettings { it.copy(textContrastAlpha = v) } },
                    valueRange = 0.70f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Flowing Paragraph Mode", color = palette.textPrimary, fontSize = 14.sp)
                    Switch(
                        checked = settings.layoutMode == ReadingLayoutMode.PARAGRAPH,
                        onCheckedChange = { checked ->
                            onUpdateSettings {
                                it.copy(
                                    layoutMode = if (checked) ReadingLayoutMode.PARAGRAPH else ReadingLayoutMode.VERSE_BY_VERSE
                                )
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Justified Book Alignment", color = palette.textPrimary, fontSize = 14.sp)
                    Switch(
                        checked = settings.textAlignment == TextAlignmentChoice.JUSTIFY,
                        onCheckedChange = { checked ->
                            onUpdateSettings {
                                it.copy(
                                    textAlignment = if (checked) TextAlignmentChoice.JUSTIFY else TextAlignmentChoice.LEFT
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Verse Action Bottom Sheet
    selectedVerse?.let { verse ->
        val isFav = verse.verse in favoritesInChapter
        val currentHl = highlightsInChapter[verse.verse]
        val ref = BibleCanon.formatReference(verse.bookId, verse.chapter, verse.verse)

        ModalBottomSheet(
            onDismissRequest = { selectedVerse = null },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(ref, color = palette.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = verse.text,
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 3
                )
                Spacer(Modifier.height(14.dp))

                // Highlight palette row
                Text("Highlight", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    SelahThemeCatalog.highlightKeys.forEach { key ->
                        val isChosen = currentHl == key
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SelahThemeCatalog.highlightSwatchColor(key))
                                .clickable {
                                    onSetHighlight(verse, if (isChosen) null else key)
                                    selectedVerse = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Text("✓", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (currentHl != null) {
                        TextButton(onClick = {
                            onSetHighlight(verse, null)
                            selectedVerse = null
                        }) {
                            Text("Clear", color = palette.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = palette.border)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionTile(
                        icon = if (isFav) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        label = if (isFav) "Unfavorite" else "Favorite"
                    ) {
                        onToggleFavorite(verse)
                        selectedVerse = null
                    }
                    ActionTile(icon = Icons.Outlined.EditNote, label = "Note") {
                        noteDialogVerse = verse
                        selectedVerse = null
                    }
                    ActionTile(icon = Icons.Outlined.CompareArrows, label = "Compare") {
                        compareVerse = verse
                        selectedVerse = null
                    }
                    ActionTile(icon = Icons.Outlined.ContentCopy, label = "Copy") {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText(ref, "“${verse.text}” — $ref"))
                        Toast.makeText(context, "Copied $ref", Toast.LENGTH_SHORT).show()
                        selectedVerse = null
                    }
                    ActionTile(icon = Icons.Outlined.Share, label = "Share") {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "“${verse.text}”\n— $ref (${activeTranslation?.code ?: "KJV"})")
                        }
                        context.startActivity(Intent.createChooser(send, "Share Verse"))
                        selectedVerse = null
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Personal Note Sheet
    noteDialogVerse?.let { verse ->
        var text by remember(verse) { mutableStateOf(notesInChapter[verse.verse].orEmpty()) }
        val ref = BibleCanon.formatReference(verse.bookId, verse.chapter, verse.verse)
        ModalBottomSheet(
            onDismissRequest = { noteDialogVerse = null },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Note for $ref", color = palette.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    placeholder = { Text("Write a reflection or study note (stored offline)…") }
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { noteDialogVerse = null }) {
                        Text("Cancel", color = palette.textSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveNote(verse, text)
                            noteDialogVerse = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        )
                    ) {
                        Text("Save Note")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Compare Translations Sheet (KJV vs NLT side-by-side)
    compareVerse?.let { verse ->
        val comparisons = remember(verse) { onCompareVerse(verse) }
        val ref = BibleCanon.formatReference(verse.bookId, verse.chapter, verse.verse)
        ModalBottomSheet(
            onDismissRequest = { compareVerse = null },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Compare $ref", color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                comparisons.forEach { (tr, txt) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceAlt,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${tr.code} • ${tr.name}",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = txt,
                                color = palette.textPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontFamily = fontFamily
                            )
                        }
                    }
                }
                if (comparisons.size <= 1) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Import your NLT or KJV file in Settings to compare multiple translations side by side.",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalSelahPalette.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = palette.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = palette.textPrimary, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookChapterPickerSheet(
    currentBookId: Int,
    currentChapter: Int,
    availableBookIds: Set<Int>,
    bookChapterCounts: Map<Int, Int>,
    availableChaptersForBook: (Int) -> Set<Int>,
    onDismiss: () -> Unit,
    onSelect: (bookId: Int, chapter: Int) -> Unit
) {
    val palette = LocalSelahPalette.current
    val partialAmber = Color(0xFFE5A93C)
    var selectedTestament by remember { mutableStateOf(BibleCanon.getById(currentBookId).testament) }
    var pickedBookId by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp)
        ) {
            if (pickedBookId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Testament.OLD to "Old Testament", Testament.NEW to "New Testament").forEach { (t, label) ->
                        val active = selectedTestament == t
                        Surface(
                            onClick = { selectedTestament = t },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) palette.onAccent else palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val books = BibleCanon.books.filter { it.testament == selectedTestament }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        val loadedChapters = bookChapterCounts[book.id]
                            ?: if (book.id in availableBookIds) book.chapters else 0
                        val hasContent = loadedChapters > 0
                        val isPartial = loadedChapters in 1 until book.chapters
                        val isCurrent = book.id == currentBookId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) palette.accent.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable {
                                    if (book.chapters == 1) onSelect(book.id, 1) else pickedBookId = book.id
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = book.name,
                                color = if (hasContent) palette.textPrimary else palette.textSecondary.copy(alpha = 0.55f),
                                fontSize = 15.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = when {
                                    !hasContent -> "0/${book.chapters} ch • missing"
                                    isPartial -> "Partial • $loadedChapters/${book.chapters} ch"
                                    else -> "${book.chapters} ch"
                                },
                                color = when {
                                    isPartial -> partialAmber
                                    !hasContent -> palette.textSecondary.copy(alpha = 0.65f)
                                    else -> palette.textSecondary
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isPartial) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                val book = BibleCanon.getById(pickedBookId!!)
                val availChapters = remember(book.id) { availableChaptersForBook(book.id) }
                val loadedCount = availChapters.size
                val isPartial = loadedCount in 1 until book.chapters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { pickedBookId = null }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = palette.accent)
                        Text("Books", color = palette.accent)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = book.name,
                            color = palette.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                loadedCount == 0 -> "Missing (0/${book.chapters} ch)"
                                isPartial -> "Partial • $loadedCount/${book.chapters} ch"
                                else -> "$loadedCount/${book.chapters} ch"
                            },
                            color = when {
                                isPartial -> partialAmber
                                loadedCount == 0 -> Color(0xFFE07587)
                                else -> palette.textSecondary
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isPartial) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    Spacer(Modifier.width(64.dp))
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items((1..book.chapters).toList()) { ch ->
                        val has = ch in availChapters
                        val isCur = book.id == currentBookId && ch == currentChapter
                        Surface(
                            onClick = { onSelect(book.id, ch) },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isCur -> palette.accent
                                has -> palette.surfaceAlt
                                else -> palette.background
                            },
                            border = BorderStroke(1.dp, if (has || isCur) palette.border else palette.border.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$ch",
                                    color = when {
                                        isCur -> palette.onAccent
                                        has -> palette.textPrimary
                                        else -> palette.textSecondary.copy(alpha = 0.45f)
                                    },
                                    fontWeight = if (has || isCur) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
