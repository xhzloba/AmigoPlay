package com.example.movie.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaMuted

internal val PosterCaptionBlock = 38.dp

@Composable
internal fun posterTitlesOn(): Boolean {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return settings.showPosterTitles
}

@Composable
internal fun largePostersOn(): Boolean {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return settings.largePosters
}

/** Полки главной/каталога: чуть крупнее обычного, но мягче полного списка. */
@Composable
internal fun posterSize(base: Dp): Dp = if (largePostersOn()) base * 1.22f else base

/** Полный список / избранное / поиск — прежний крупный режим (2 колонки). */
@Composable
internal fun posterGridColumns(): Int = if (largePostersOn()) 2 else 3

@Composable
internal fun PosterCaption(title: String, year: Int?, modifier: Modifier = Modifier) {
    val style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    Column(modifier.padding(top = 2.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = year?.toString().orEmpty(),
            color = CinemaMuted,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            style = style,
            maxLines = 1,
        )
    }
}

@Composable
internal fun PosterCaptionSkeleton(modifier: Modifier = Modifier) {
    // Те же слоты, что у PosterCaption: 2.dp, строка 15.sp, 4.dp, строка 15.sp.
    // Иначе нижние ряды подпрыгивают, когда скелетон сменяется текстом.
    Column(modifier.padding(top = 2.dp)) {
        CaptionBar(0.82f, 11.dp, 0.10f, FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        CaptionBar(0.36f, 10.dp, 0.06f, FontWeight.Normal)
    }
}

@Composable
private fun CaptionBar(fraction: Float, bar: Dp, alpha: Float, weight: FontWeight) {
    Box(Modifier.fillMaxWidth()) {
        Text(
            text = " ",
            color = Color.Transparent,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = weight,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            maxLines = 1,
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .height(bar)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = alpha)),
        )
    }
}
