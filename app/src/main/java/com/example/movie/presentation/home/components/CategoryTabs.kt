package com.example.movie.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.BuildConfig
import com.example.movie.R
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
    Column(modifier = modifier) {
        BrandMark()
        Spacer(Modifier.height(12.dp))
        LazyRow(
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
}

private val LogoHeight = 32.dp
private val LogoAspect = 754f / 186f
private val LogoBleed = LogoHeight * (29f / 186f)

@Composable
private fun BrandMark() {
    Row(
        modifier = Modifier.padding(start = 22.dp - LogoBleed, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.amigoplay_logo),
            contentDescription = "AmigoPlay",
            modifier = Modifier
                .height(LogoHeight)
                .aspectRatio(LogoAspect, matchHeightConstraintsFirst = true),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = BuildConfig.VERSION_NAME,
            color = Color(0xFF8A8A90),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
fun CategoryTabsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Spacer(
            Modifier
                .padding(start = 22.dp, end = 22.dp)
                .width(115.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
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
}
