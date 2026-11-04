package com.example.movie.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaMuted

internal val PosterCaptionBlock = 42.dp

@Composable
internal fun posterTitlesOn(): Boolean {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return settings.showPosterTitles
}

@Composable
internal fun PosterCaption(title: String, year: Int?, modifier: Modifier = Modifier) {
    val style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    Column(modifier.padding(top = 10.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = year?.toString().orEmpty(),
            color = CinemaMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            style = style,
            maxLines = 1,
        )
    }
}

@Composable
internal fun PosterCaptionSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(top = 10.dp)
            .height(PosterCaptionBlock - 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.82f)
                .height(9.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        )
        Box(
            Modifier
                .fillMaxWidth(0.36f)
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        )
    }
}
