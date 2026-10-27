package com.example.movie.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.domain.model.Shelf

internal val CategoryTabFontSize = 18.sp
internal val CategoryTabLineHeight = 24.sp
internal val CategoryTabLetterSpacing = (-0.2).sp

private val TabSkeletonLabels = listOf(
    "В тренде",
    "Сейчас смотрят",
    "4K",
    "Dolby Vision",
    "Обновления",
    "Фильмы",
)

@Composable
fun CategoryTabs(
    shelves: List<Shelf>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, shelves) {
        val index = shelves.indexOfFirst { it.id == selectedId }
        if (index < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == index }
        if (!visible) listState.animateScrollToItem(index)
    }
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(shelves, key = { it.id }) { shelf ->
            val selected = shelf.id == selectedId
            Text(
                text = shelf.title,
                color = if (selected) Color.White else Color(0xFF8A8A90),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = CategoryTabFontSize,
                lineHeight = CategoryTabLineHeight,
                letterSpacing = CategoryTabLetterSpacing,
                modifier = Modifier.clickable(
                    interactionSource = remember(shelf.id) { MutableInteractionSource() },
                    indication = null,
                    onClick = { onSelect(shelf.id) },
                ),
            )
        }
    }
}

@Composable
fun CategoryTabsSkeleton(modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false,
    ) {
        items(TabSkeletonLabels.size) { index ->
            Text(
                text = TabSkeletonLabels[index],
                color = Color.Transparent,
                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = CategoryTabFontSize,
                lineHeight = CategoryTabLineHeight,
                letterSpacing = CategoryTabLetterSpacing,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            )
        }
    }
}
