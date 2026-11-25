package com.selah.bible.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.selah.bible.data.BookCoverage
import com.selah.bible.data.CardStyleChoice
import com.selah.bible.data.CustomFontEntry
import com.selah.bible.data.CustomFontManager
import com.selah.bible.data.FontChoice
import com.selah.bible.data.ImportProgress
import com.selah.bible.data.MarginPreset
import com.selah.bible.data.ReadingLayoutMode
import com.selah.bible.data.TextAlignmentChoice
import com.selah.bible.data.Translation
import com.selah.bible.data.UserSettings
import com.selah.bible.data.VerseNumberStyle
import com.selah.bible.data.WidgetFontSize
import com.selah.bible.data.WidgetThemeChoice
import com.selah.bible.ui.components.SectionLabel
import com.selah.bible.ui.components.SelahCard
import com.selah.bible.ui.theme.LocalSelahPalette
import com.selah.bible.ui.theme.SelahThemeCatalog

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    translations: List<Translation>,
    customFonts: List<CustomFontEntry>,
    importProgress: ImportProgress?,
    importStatusMessage: String?,
    onSelectTranslation: (String) -> Unit,
    onDeleteTranslation: (String) -> Unit,
    onRenameTranslation: (id: String, code: String, name: String) -> Unit,
    onRestoreStarterKjv: () -> Unit,
    onLoadBookCoverage: (String) -> List<BookCoverage>,
    onPickMultipleFiles: (targetTranslation: Translation?) -> Unit,
    onPickEpub: () -> Unit,
    onPickPdf: () -> Unit,
    onPickSqlite: () -> Unit,
    onPickCustomFont: () -> Unit,
    onDeleteCustomFont: (String) -> Unit,
    onUpdateSettings: ((UserSettings) -> UserSettings) -> Unit,
    onRefreshWidgets: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val palette = LocalSelahPalette.current
    val context = LocalContext.current
    val previewFont = SelahThemeCatalog.rememberActiveFontFamily(settings)
    var coverageTarget by remember { mutableStateOf<Translation?>(null) }
    var renameTarget by remember { mutableStateOf<Translation?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Settings & Studio",
                color = palette.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Offline Bible Translations & Multi-File Combine/Merge Importer
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Offline Bibles (KJV, NLT, NIV, ESV, MEV & Any)")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Auto-recognizes KJV, NLT, NIV, ESV, MEV, and any Bible version from filename, file metadata, or text. Matching EPUBs & PDFs combine automatically — and importing full KJV finishes the KJV starter.",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))

                if (translations.isEmpty()) {
                    OutlinedButton(
                        onClick = onRestoreStarterKjv,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Restore Built-in KJV Starter", color = palette.textSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }

                translations.forEach { tr ->
                    val active = tr.id == settings.activeTranslationId
                    Surface(
                        onClick = { onSelectTranslation(tr.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) palette.accent.copy(alpha = 0.14f) else palette.surfaceAlt,
                        border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tr.code} • ${tr.name}",
                                        color = palette.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${tr.bookCount}/66 books • ${tr.verseCount} verses",
                                        color = if (tr.bookCount >= 66) palette.accent else palette.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (active) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = "Active",
                                            tint = palette.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { renameTarget = tr },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Rename",
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteTranslation(tr.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = { onPickMultipleFiles(tr) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.accent.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (tr.isBuiltIn) "Finish with EPUB / PDF" else "Merge EPUB / PDF",
                                            color = palette.accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Surface(
                                    onClick = { coverageTarget = tr },
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.background,
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Text(
                                        text = if (tr.bookCount < 66) "View ${66 - tr.bookCount} Missing" else "View 66 Books",
                                        color = palette.textSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (importProgress != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surfaceAlt)
                            .padding(12.dp)
                    ) {
                        Text(importProgress.stage, color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { importProgress.fraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = palette.accent,
                            trackColor = palette.border
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append("${importProgress.versesParsed} verses parsed")
                                if (importProgress.currentBook.isNotBlank()) append(" • ${importProgress.currentBook}")
                            },
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Button(
                        onClick = { onPickMultipleFiles(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp)
                    ) {
                        Icon(Icons.Outlined.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import & Combine Multiple Files", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickEpub,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, palette.accent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                        ) {
                            Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("EPUB", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onPickPdf,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, palette.accent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                        ) {
                            Text("PDF", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onPickSqlite,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
                        ) {
                            Text("SQLite", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!importStatusMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(importStatusMessage, color = palette.accent, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }

        // 2. Theme Gallery (10 Themes + Follow System + Accent Swatches)
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Themes & Color Studio")
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Follow System Dark/Light", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Auto-switches between AMOLED Black & Warm Paper",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.followSystemTheme,
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(followSystemTheme = enabled) }
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 2-column theme grid
                val chunkedThemes = SelahThemeCatalog.themes.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedThemes.forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowThemes.forEach { t ->
                                val isSelected = if (settings.followSystemTheme) {
                                    t.id == settings.preferredDarkThemeId || t.id == settings.preferredLightThemeId
                                } else {
                                    t.id == settings.themeId
                                }
                                Surface(
                                    onClick = {
                                        onUpdateSettings { cur ->
                                            if (cur.followSystemTheme) {
                                                if (t.isDark) cur.copy(preferredDarkThemeId = t.id)
                                                else cur.copy(preferredLightThemeId = t.id)
                                            } else {
                                                cur.copy(themeId = t.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = t.background,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) t.accent else t.border
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(Modifier.size(10.dp).clip(CircleShape).background(t.accent))
                                                Box(Modifier.size(10.dp).clip(CircleShape).background(t.textPrimary))
                                            }
                                            if (t.isOledOptimized) {
                                                Text(
                                                    "OLED",
                                                    color = t.accent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = t.name,
                                            color = t.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = t.subtitle,
                                            color = t.textSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Accent Color Override", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelahThemeCatalog.accents.forEach { swatch ->
                        val selected = settings.accentOverrideId == swatch.id
                        val displayColor = (if (palette.isDark) swatch.darkColor else swatch.lightColor) ?: palette.accent
                        Surface(
                            onClick = { onUpdateSettings { it.copy(accentOverrideId = swatch.id) } },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (selected) palette.accent else palette.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(displayColor)
                                        .border(0.5.dp, palette.border, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(swatch.name, color = palette.textPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("One-Tap Neutral Presets", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    followSystemTheme = false,
                                    themeId = "clean_white",
                                    accentOverrideId = "default",
                                    cardStyle = CardStyleChoice.FLAT,
                                    fontChoice = FontChoice.SANS,
                                    customFontFileName = null,
                                    verseNumberStyle = VerseNumberStyle.MUTED
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceAlt,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text("Clean Light", color = palette.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                    Surface(
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    followSystemTheme = false,
                                    themeId = "neutral_dark",
                                    accentOverrideId = "default",
                                    cardStyle = CardStyleChoice.FLAT,
                                    fontChoice = FontChoice.SANS,
                                    customFontFileName = null,
                                    verseNumberStyle = VerseNumberStyle.MUTED
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceAlt,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text("Neutral Dark", color = palette.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                    Surface(
                        onClick = {
                            onUpdateSettings {
                                it.copy(
                                    followSystemTheme = false,
                                    themeId = "amoled_neutral",
                                    accentOverrideId = "default",
                                    cardStyle = CardStyleChoice.FLAT,
                                    fontChoice = FontChoice.SANS,
                                    customFontFileName = null,
                                    verseNumberStyle = VerseNumberStyle.MUTED
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceAlt,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text("Pure Black Neutral", color = palette.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Card & Surface Style", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardStyleChoice.entries.forEach { cs ->
                        val active = settings.cardStyle == cs
                        Surface(
                            onClick = { onUpdateSettings { it.copy(cardStyle = cs) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = cs.label,
                                    color = if (active) palette.onAccent else palette.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Show Bottom Bar Labels",
                    subtitle = "Turn off for an ultra-minimal icon navigation bar",
                    checked = settings.showNavLabels
                ) { checked ->
                    onUpdateSettings { it.copy(showNavLabels = checked) }
                }
            }
        }

        // 3. Typography & Reading Layout Studio
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Typography & Reading Layout")
                Spacer(Modifier.height(10.dp))

                // Live Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.background)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (settings.verseNumberStyle != VerseNumberStyle.HIDDEN) {
                            "10  Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth."
                        } else {
                            "Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth."
                        },
                        color = palette.textPrimary.copy(alpha = settings.textContrastAlpha),
                        fontSize = settings.fontSizeSp.sp,
                        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
                        letterSpacing = settings.letterSpacingEm.em,
                        textAlign = if (settings.textAlignment == TextAlignmentChoice.JUSTIFY) TextAlign.Justify else TextAlign.Start,
                        fontFamily = previewFont
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Built-in Fonts (includes OpenDyslexic)", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontChoice.entries.forEach { fc ->
                        val active = settings.customFontFileName == null && settings.fontChoice == fc
                        Surface(
                            onClick = { onUpdateSettings { it.copy(fontChoice = fc, customFontFileName = null) } },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Text(
                                text = fc.label,
                                color = if (active) palette.onAccent else palette.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = SelahThemeCatalog.fontFamilyFor(fc),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Custom Fonts (.otf, .ttf, .woff, .woff2)", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                if (customFonts.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customFonts.forEach { cf ->
                            val active = settings.customFontFileName == cf.fileName
                            val customFf = remember(cf.fileName) {
                                CustomFontManager.loadComposeFontFamily(context, cf.fileName)
                            }
                            Surface(
                                onClick = { onUpdateSettings { it.copy(customFontFileName = cf.fileName) } },
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) palette.accent else palette.surfaceAlt,
                                border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cf.displayName,
                                        color = if (active) palette.onAccent else palette.textPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = customFf
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove Font",
                                        tint = if (active) palette.onAccent else palette.textSecondary,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable { onDeleteCustomFont(cf.fileName) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = onPickCustomFont,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, palette.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.FontDownload, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import Font (.otf, .ttf, .woff, .woff2)", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                    "Line Height: ${String.format("%.2f", settings.lineHeightMultiplier)}x",
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
                    "Letter Spacing (Dyslexia-friendly): ${String.format("%.2f", settings.letterSpacingEm)}em",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = settings.letterSpacingEm,
                    onValueChange = { v -> onUpdateSettings { it.copy(letterSpacingEm = v) } },
                    valueRange = 0.0f..0.08f,
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                )

                Text(
                    "Text Softness / Contrast: ${(settings.textContrastAlpha * 100).toInt()}%",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = settings.textContrastAlpha,
                    onValueChange = { v -> onUpdateSettings { it.copy(textContrastAlpha = v) } },
                    valueRange = 0.70f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                )

                Text(
                    "Verse Spacing: ${settings.verseSpacingDp}dp",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Slider(
                    value = settings.verseSpacingDp.toFloat(),
                    onValueChange = { v -> onUpdateSettings { it.copy(verseSpacingDp = v.toInt()) } },
                    valueRange = 4f..22f,
                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                )

                Text("Text Alignment", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAlignmentChoice.entries.forEach { ta ->
                        val active = settings.textAlignment == ta
                        Surface(
                            onClick = { onUpdateSettings { it.copy(textAlignment = ta) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(ta.label, color = if (active) palette.onAccent else palette.textPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Verse Numbers", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerseNumberStyle.entries.forEach { vns ->
                        val active = settings.verseNumberStyle == vns
                        Surface(
                            onClick = { onUpdateSettings { it.copy(verseNumberStyle = vns) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(vns.label, color = if (active) palette.onAccent else palette.textPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Reading Margins", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarginPreset.entries.forEach { mp ->
                        val active = settings.marginPreset == mp
                        Surface(
                            onClick = { onUpdateSettings { it.copy(marginPreset = mp) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(mp.label, color = if (active) palette.onAccent else palette.textPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Flowing Paragraph Mode",
                    subtitle = "Book-style paragraphs vs verse-per-line",
                    checked = settings.layoutMode == ReadingLayoutMode.PARAGRAPH
                ) { checked ->
                    onUpdateSettings {
                        it.copy(layoutMode = if (checked) ReadingLayoutMode.PARAGRAPH else ReadingLayoutMode.VERSE_BY_VERSE)
                    }
                }

                SettingToggleRow(
                    title = "Keep Screen On While Reading",
                    subtitle = "Off by default to maximize battery life",
                    checked = settings.keepScreenOn
                ) { checked ->
                    onUpdateSettings { it.copy(keepScreenOn = checked) }
                }
            }
        }

        // 4. Lightweight Home Screen Widgets Studio
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Widgets, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    SectionLabel("Home Screen Widgets Studio")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Long-press your Android home screen → Widgets → Selah Bible to add the Verse of the Day widget and Favorite Verses widget. Both use zero polling and update only on calendar day change or tap.",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(10.dp))
                Text("Widget Theme", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidgetThemeChoice.entries.forEach { wt ->
                        val active = settings.widgetTheme == wt
                        Surface(
                            onClick = {
                                onUpdateSettings { it.copy(widgetTheme = wt) }
                                onRefreshWidgets()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Text(
                                text = wt.label,
                                color = if (active) palette.onAccent else palette.textPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Widget Text Size", color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetFontSize.entries.forEach { wfs ->
                        val active = settings.widgetFontSize == wfs
                        Surface(
                            onClick = {
                                onUpdateSettings { it.copy(widgetFontSize = wfs) }
                                onRefreshWidgets()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) palette.accent else palette.surfaceAlt,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = wfs.label,
                                    color = if (active) palette.onAccent else palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Show Daily Streak on VOTD Widget",
                    subtitle = "Displays subtle streak badge in top corner",
                    checked = settings.widgetShowStreak
                ) { checked ->
                    onUpdateSettings { it.copy(widgetShowStreak = checked) }
                    onRefreshWidgets()
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRefreshWidgets,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, palette.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sync Home Screen Widgets Now", color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 5. Offline Study Backup
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Offline Data Backup")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Export or restore your favorites, highlights, notes, daily streaks, and reading plan progress as a local JSON file.",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export JSON", color = palette.textPrimary, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onImportBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore JSON", color = palette.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    coverageTarget?.let { tr ->
        val coverage = remember(tr.id, tr.verseCount) { onLoadBookCoverage(tr.id) }
        val missingCount = coverage.count { it.isMissing }
        val completeCount = coverage.count { it.isComplete }

        ModalBottomSheet(
            onDismissRequest = { coverageTarget = null },
            containerColor = palette.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "${tr.code} Coverage ($completeCount/66 full)",
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (missingCount > 0) {
                        "$missingCount books missing • Merge another EPUB or PDF to fill gaps automatically."
                    } else {
                        "All 66 books loaded (${tr.verseCount} verses)."
                    },
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        coverageTarget = null
                        onPickMultipleFiles(tr)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = palette.onAccent
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Merge EPUB / PDF into ${tr.code}", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Show missing & partial books first so gaps are immediately visible
                    val ordered = coverage.sortedWith(compareBy({ it.isComplete }, { it.chaptersLoaded > 0 }, { it.book.id }))
                    items(ordered, key = { it.book.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surfaceAlt)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.book.name,
                                color = if (item.isMissing) palette.textSecondary else palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (item.isMissing) FontWeight.Normal else FontWeight.Medium
                            )
                            Text(
                                text = when {
                                    item.isMissing -> "Missing"
                                    item.isComplete -> "✓ ${item.chaptersLoaded}/${item.book.chapters} ch"
                                    else -> "${item.chaptersLoaded}/${item.book.chapters} ch"
                                },
                                color = when {
                                    item.isComplete -> palette.accent
                                    item.isMissing -> Color(0xFFE07587)
                                    else -> palette.textSecondary
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { tr ->
        var codeText by remember(tr.id) { mutableStateOf(tr.code) }
        var nameText by remember(tr.id) { mutableStateOf(tr.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = palette.surface,
            title = { Text("Rename Translation", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it.take(8) },
                        label = { Text("Short Code (e.g. NIV, ESV, MEV)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it.take(64) },
                        label = { Text("Full Translation Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (codeText.isNotBlank() && nameText.isNotBlank()) {
                        onRenameTranslation(tr.id, codeText, nameText)
                    }
                    renameTarget = null
                }) {
                    Text("Save", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalSelahPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.textPrimary, fontSize = 14.sp)
            Text(subtitle, color = palette.textSecondary, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
