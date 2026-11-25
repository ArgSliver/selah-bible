package com.selah.bible.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selah.bible.data.ReadingPlan
import com.selah.bible.data.ReadingPlanCatalog
import com.selah.bible.data.UserSettings
import com.selah.bible.ui.components.SectionLabel
import com.selah.bible.ui.components.SelahCard
import com.selah.bible.ui.theme.LocalSelahPalette
import java.time.LocalDate

@Composable
fun PlansScreen(
    settings: UserSettings,
    activePlan: ReadingPlan,
    completedKeys: Set<String>,
    onSelectPlan: (String) -> Unit,
    onToggleChapterDone: (planId: String, chapterKey: String, completed: Boolean) -> Unit,
    onResetPlan: (String) -> Unit,
    onOpenChapter: (bookId: Int, chapter: Int) -> Unit
) {
    val palette = LocalSelahPalette.current
    var showResetConfirm by remember { mutableStateOf(false) }

    val todayNumber = ((LocalDate.now().toEpochDay() - settings.planStartEpochDay).toInt() + 1)
        .coerceIn(1, activePlan.totalDays)
    val completedDays = activePlan.days.count { day ->
        day.assignments.isNotEmpty() && day.assignments.all { it.key() in completedKeys }
    }
    val pct = if (activePlan.totalChapters > 0) {
        (completedKeys.size * 100) / activePlan.totalChapters
    } else 0

    val listState = rememberLazyListState()
    LaunchedEffect(activePlan.id) {
        // Scroll straight to the first incomplete day (or today)
        val firstIncompleteIdx = activePlan.days.indexOfFirst { d ->
            d.assignments.any { it.key() !in completedKeys }
        }.takeIf { it >= 0 } ?: (todayNumber - 1)
        listState.scrollToItem((firstIncompleteIdx + 2).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reading Challenges",
                        color = palette.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "100% offline • Progress counts toward your streak",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { showResetConfirm = true }) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset Challenge",
                        tint = palette.textSecondary
                    )
                }
            }
        }

        // Plan Selector Row
        item(key = "plan_selector") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReadingPlanCatalog.plans, key = { it.id }) { plan ->
                    val selected = plan.id == activePlan.id
                    Surface(
                        onClick = { onSelectPlan(plan.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.accent else palette.surface,
                        border = BorderStroke(1.dp, if (selected) palette.accent else palette.border)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = plan.title,
                                color = if (selected) palette.onAccent else palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${plan.totalDays} days • ${plan.totalChapters} ch",
                                color = if (selected) palette.onAccent.copy(alpha = 0.8f) else palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Summary Card
        item(key = "summary") {
            SelahCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(activePlan.title)
                    Text(
                        text = "$pct%",
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(activePlan.subtitle, color = palette.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = {
                        if (activePlan.totalChapters > 0) {
                            completedKeys.size.toFloat() / activePlan.totalChapters
                        } else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = palette.accent,
                    trackColor = palette.surfaceAlt
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${completedKeys.size} / ${activePlan.totalChapters} chapters",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$completedDays / ${activePlan.totalDays} days complete",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Day-by-Day Cards
        items(activePlan.days, key = { "${activePlan.id}_day_${it.dayNumber}" }) { day ->
            val allDone = day.assignments.isNotEmpty() && day.assignments.all { it.key() in completedKeys }
            val isToday = day.dayNumber == todayNumber

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface,
                border = BorderStroke(
                    width = if (isToday) 1.5.dp else 1.dp,
                    color = when {
                        isToday -> palette.accent
                        allDone -> palette.accent.copy(alpha = 0.35f)
                        else -> palette.border
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Day ${day.dayNumber}",
                                color = if (isToday) palette.accent else palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isToday) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.accent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "TODAY",
                                        color = palette.accent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (allDone) {
                            Text("Completed ✓", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    day.assignments.forEach { assignment ->
                        val checked = assignment.key() in completedKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenChapter(assignment.bookId, assignment.chapter) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = "Toggle Completed",
                                    tint = if (checked) palette.accent else palette.textSecondary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable {
                                            onToggleChapterDone(activePlan.id, assignment.key(), !checked)
                                        }
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = assignment.fullLabel(),
                                    color = if (checked) palette.textSecondary else palette.textPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Read →",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = palette.surface,
            title = { Text("Reset Challenge?", color = palette.textPrimary) },
            text = {
                Text(
                    "This clears checked chapters for ${activePlan.title} and sets Day 1 to today. Your reading streak is preserved.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onResetPlan(activePlan.id)
                    showResetConfirm = false
                }) {
                    Text("Reset", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}
