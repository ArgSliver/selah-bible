package com.selah.bible.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selah.bible.data.CardStyleChoice
import com.selah.bible.ui.theme.LocalCardStyle
import com.selah.bible.ui.theme.LocalSelahPalette

@Composable
fun SelahCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalSelahPalette.current
    val cardStyle = LocalCardStyle.current
    val shape = when (cardStyle) {
        CardStyleChoice.OUTLINED -> RoundedCornerShape(14.dp)
        CardStyleChoice.FLAT -> RoundedCornerShape(12.dp)
        CardStyleChoice.ROUNDED -> RoundedCornerShape(22.dp)
    }
    val border = when (cardStyle) {
        CardStyleChoice.FLAT -> null
        CardStyleChoice.OUTLINED, CardStyleChoice.ROUNDED -> BorderStroke(1.dp, palette.border)
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = palette.surface,
            border = border
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = palette.surface,
            border = border
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val palette = LocalSelahPalette.current
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = palette.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}
