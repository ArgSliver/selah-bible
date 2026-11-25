package com.selah.bible.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selah.bible.data.BibleCanon
import com.selah.bible.data.ReadingPlan
import com.selah.bible.data.StreakStats
import com.selah.bible.data.Translation
import com.selah.bible.data.UserSettings
import com.selah.bible.data.VotdPassage
import com.selah.bible.ui.components.SectionLabel
import com.selah.bible.ui.components.SelahCard
import com.selah.bible.ui.theme.LocalSelahPalette
import com.selah.bible.ui.theme.SelahThemeCatalog
import java.time.LocalDate

@Composable
fun HomeScreen(
    settings: UserSettings,
    votd: VotdPassage,
    isVotdFavorite: Boolean,
    streak: StreakStats,
    activePlan: ReadingPlan,
    planCompletedKeys: Set<String>,
    translations: List<Translation>,
    onRefreshVotd: () -> Unit,
    onToggleVotdFavorite: () -> Unit,
    onOpenReader: (bookId: Int, chapter: Int, verse: Int?) -> Unit,
    onMarkTodayRead: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenImport: () -> Unit,
    onQuickToggleTheme: () -> Unit
) {
    val palette = LocalSelahPalette.current
    val context = LocalContext.current
    val fontFamily = SelahThemeCatalog.rememberActiveFontFamily(settings)
    val onlyStarter = translations.size == 1 && translations.first().isBuiltIn

    val dayIndex = ((LocalDate.now().toEpochDay() - settings.planStartEpochDay).toInt() + 1)
        .coerceIn(1, activePlan.totalDays)
    val todayPlanDay = activePlan.days.getOrNull(dayIndex - 1) ?: activePlan.days.first()
    val planProgress = if (activePlan.totalChapters > 0) {
        planCompletedKeys.size.toFloat() / activePlan.totalChapters
    } else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Selah",
                    color = palette.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )

                // Quick toggle between AMOLED True Black & Warm Paper
                Surface(
                    onClick = onQuickToggleTheme,
                    shape = CircleShape,
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Icon(
                        imageVector = if (palette.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Switch Theme",
                        tint = palette.accent,
                        modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
            }
        }

        // Verse of the Day Card (Coherent, Random, Non-Looping)
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("Verse of the Day")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleVotdFavorite,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isVotdFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite Verse",
                                tint = palette.accent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "“${votd.text}”\n— ${votd.reference()} (${votd.translationCode})"
                                    )
                                }
                                context.startActivity(Intent.createChooser(send, "Share Verse"))
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share Verse",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onRefreshVotd,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Draw another coherent verse",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "“${votd.text}”",
                    color = palette.textPrimary,
                    fontSize = (settings.fontSizeSp * 0.98f).coerceIn(15f, 22f).sp,
                    lineHeight = (settings.fontSizeSp * 1.48f).coerceIn(22f, 34f).sp,
                    fontFamily = fontFamily,
                    fontStyle = FontStyle.Normal
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${votd.reference()} • ${votd.translationCode}",
                        color = palette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Read Chapter →",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenReader(votd.bookId, votd.chapter, votd.startVerse) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Daily Streak + 30-Day Heatmap Card
        item {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${streak.currentStreak} Day Streak",
                                color = palette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Best: ${streak.longestStreak}d • ${streak.totalChaptersRead} chapters read",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (streak.readToday) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = palette.accent.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Done",
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onMarkTodayRead,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = palette.onAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Check In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Last 30 days",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))

                // Lightweight 30-day grid (2 rows x 15 cells, zero heavy Canvas)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (row in 0 until 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            for (col in 0 until 15) {
                                val idx = row * 15 + col
                                val active = streak.last30DaysMask.getOrElse(idx) { false }
                                val isToday = idx == 29
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (active) palette.accent else palette.surfaceAlt
                                        )
                                        .then(
                                            if (isToday) Modifier.border(
                                                1.dp,
                                                palette.textPrimary,
                                                RoundedCornerShape(3.dp)
                                            ) else Modifier
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Reading Challenge Quick Card
        item {
            SelahCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenPlans
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Active Challenge")
                    Text(
                        text = "Day $dayIndex / ${activePlan.totalDays}",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = activePlan.title,
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { planProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = palette.accent,
                    trackColor = palette.surfaceAlt
                )

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Today's reading:",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    todayPlanDay.assignments.take(4).forEach { assignment ->
                        val done = assignment.key() in planCompletedKeys
                        Surface(
                            onClick = { onOpenReader(assignment.bookId, assignment.chapter, null) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (done) palette.accent.copy(alpha = 0.18f) else palette.surfaceAlt,
                            border = BorderStroke(
                                1.dp,
                                if (done) palette.accent else palette.border
                            )
                        ) {
                            Text(
                                text = (if (done) "✓ " else "") + assignment.label(),
                                color = if (done) palette.accent else palette.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Continue Reading Card
        item {
            val lastBook = BibleCanon.getById(settings.lastBookId)
            SelahCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenReader(settings.lastBookId, settings.lastChapter, null) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Continue Reading",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${lastBook.name} ${settings.lastChapter}",
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Prompt to import full KJV & NLT EPUB/PDF files if only starter is present
        if (onlyStarter) {
            item {
                SelahCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.UploadFile,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Load Your Full KJV & NLT Bibles",
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Currently using the built-in KJV Starter pack. Import your KJV or NLT EPUB, PDF, or converted SQLite files offline to unlock all 66 books and side-by-side comparison.",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenImport,
                        border = BorderStroke(1.dp, palette.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Import EPUB / PDF / SQLite", color = palette.accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}
