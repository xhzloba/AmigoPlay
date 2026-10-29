package com.example.movie.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.movie.R
import java.util.Locale

@Composable
internal fun KpScore(
    score: Float,
    color: Color,
    iconSize: Dp,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_kinopoisk),
            contentDescription = stringResource(R.string.rating_kp),
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(6.dp))
        RatingValue(score, color, fontSize, fontWeight)
    }
}

@Composable
internal fun ImdbScore(
    score: Float,
    color: Color,
    iconHeight: Dp,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_imdb),
            contentDescription = stringResource(R.string.rating_imdb),
            modifier = Modifier
                .height(iconHeight)
                .aspectRatio(2f)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(6.dp))
        RatingValue(score, color, fontSize, fontWeight)
    }
}

@Composable
private fun RatingValue(
    score: Float,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
) {
    Text(
        text = String.format(Locale.US, "%.1f", score),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = fontSize,
        maxLines = 1,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}
