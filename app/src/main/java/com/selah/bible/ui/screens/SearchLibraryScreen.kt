package com.selah.bible.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selah.bible.data.BibleCanon
import com.selah.bible.data.FavoriteVerse
import com.selah.bible.data.SearchHit
import com.selah.bible.data.Testament
import com.selah.bible.data.UserSettings
import com.selah.bible.data.VerseHighlight
import com.selah.bible.data.VerseNote
import com.selah.bible.ui.components.SelahCard
import com.selah.bible.ui.theme.LocalSelahPalette
import com.selah.bible.ui.theme.SelahThemeCatalog

@Composable
fun SearchLibraryScreen(
    settings: UserSettings,
    favorites: List<FavoriteVerse>,
    highlights: List<VerseHighlight>,
    notes: List<VerseNote>,
    onSearch: (query: String, testament: Testament?) -> List<SearchHit>,
    onLoadVerseText: (bookId: Int, chapter: Int, verse: Int) -> String,
    onRemoveFavorite: (FavoriteVerse) -> Unit,
    onOpenVerse: (bookId: Int, chapter: Int, verse: Int) -> Unit
) {
    val palette = LocalSelahPalette.current
    val fontFamily = SelahThemeCatalog.rememberActiveFontFamily(settings)
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var testamentFilter by remember { mutableStateOf<Testament?>(null) }

    val searchHits = remember(query, testamentFilter, settings.activeTranslationId) {
        if (query.trim().length >= 2) onSearch(query, testamentFilter) else emptyList()
    }

    val tabs = listOf("Search", "Favorites (${favorites.size})", "Highlights", "Notes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Search & Study Library",
            color = palette.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        // Segmented pill tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Surface(
                    onClick = { selectedTab = idx },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) palette.accent else palette.surface,
                    border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) palette.onAccent else palette.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Offline FTS Search
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search offline scriptures (e.g. peace, shepherd)…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "All", Testament.OLD to "Old Testament", Testament.NEW to "New Testament").forEach { (t, lbl) ->
                        val active = testamentFilter == t
                        Surface(
                            onClick = { testamentFilter = t },
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) palette.accent.copy(alpha = 0.16f) else palette.surface,
                            border = BorderStroke(1.dp, if (active) palette.accent else palette.border)
                        ) {
                            Text(
                                text = lbl,
                                color = if (active) palette.accent else palette.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (query.trim().length < 2) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Instant offline full-text search powered by SQLite FTS4.",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else if (searchHits.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matches found for \"$query\".", color = palette.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(searchHits) { hit ->
                            SelahCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenVerse(hit.bookId, hit.chapter, hit.verse) }
                            ) {
                                Text(
                                    text = "${hit.reference()} • ${hit.translationCode}",
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = hit.text,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Favorites (synced directly with Favorite Verses Widget!)
                if (favorites.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No favorite verses yet. Tap any verse in the Reader to save it here and on your Favorites Widget.",
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Synced automatically with your Favorite Verses home screen widget.",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        items(favorites, key = { it.id }) { fav ->
                            val liveText = remember(fav.id, settings.activeTranslationId) {
                                onLoadVerseText(fav.bookId, fav.chapter, fav.verse).ifBlank { fav.textSnapshot }
                            }
                            SelahCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenVerse(fav.bookId, fav.chapter, fav.verse) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = fav.reference(),
                                        color = palette.accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { onRemoveFavorite(fav) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove Favorite",
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "“$liveText”",
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Highlights
                if (highlights.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No highlighted verses yet.", color = palette.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(highlights) { hl ->
                            val text = remember(hl, settings.activeTranslationId) {
                                onLoadVerseText(hl.bookId, hl.chapter, hl.verse)
                            }
                            SelahCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenVerse(hl.bookId, hl.chapter, hl.verse) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(SelahThemeCatalog.highlightSwatchColor(hl.colorKey))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = BibleCanon.formatReference(hl.bookId, hl.chapter, hl.verse),
                                        color = palette.accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (text.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = text,
                                        color = palette.textPrimary,
                                        fontSize = 14.sp,
                                        fontFamily = fontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Notes
                if (notes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No study notes yet. Tap any verse in the Reader to add a note.", color = palette.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(notes) { n ->
                            SelahCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onOpenVerse(n.bookId, n.chapter, n.verse) }
                            ) {
                                Text(
                                    text = n.reference(),
                                    color = palette.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = n.note,
                                    color = palette.textPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
