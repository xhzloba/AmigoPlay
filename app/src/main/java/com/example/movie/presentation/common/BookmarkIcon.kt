package com.example.movie.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.movie.R

internal val BookmarkOutline: ImageVector = ImageVector.Builder(
    name = "bookmark",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).path(
    fill = SolidColor(Color.Black),
    pathFillType = PathFillType.EvenOdd,
) {
    moveTo(38f, 6f)
    verticalLineTo(40.125f)
    lineTo(24.85f, 33.74f)
    lineTo(23.5f, 33.065f)
    lineTo(22.15f, 33.74f)
    lineTo(9f, 40.125f)
    verticalLineTo(6f)
    horizontalLineTo(38f)
    close()
    moveTo(38f, 3f)
    horizontalLineTo(9f)
    curveTo(8.20435f, 3f, 7.44129f, 3.31607f, 6.87868f, 3.87868f)
    curveTo(6.31607f, 4.44129f, 6f, 5.20435f, 6f, 6f)
    verticalLineTo(45f)
    lineTo(23.5f, 36.5f)
    lineTo(41f, 45f)
    verticalLineTo(6f)
    curveTo(41f, 5.20435f, 40.6839f, 4.44129f, 40.1213f, 3.87868f)
    curveTo(39.5587f, 3.31607f, 38.7957f, 3f, 38f, 3f)
    close()
}.build()

internal val BookmarkFilled: ImageVector = ImageVector.Builder(
    name = "bookmarkFilled",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).path(
    fill = SolidColor(Color.Black),
) {
    moveTo(38f, 3f)
    horizontalLineTo(9f)
    curveTo(8.20435f, 3f, 7.44129f, 3.31607f, 6.87868f, 3.87868f)
    curveTo(6.31607f, 4.44129f, 6f, 5.20435f, 6f, 6f)
    verticalLineTo(45f)
    lineTo(23.5f, 36.5f)
    lineTo(41f, 45f)
    verticalLineTo(6f)
    curveTo(41f, 5.20435f, 40.6839f, 4.44129f, 40.1213f, 3.87868f)
    curveTo(39.5587f, 3.31607f, 38.7957f, 3f, 38f, 3f)
    close()
}.build()

@Composable
internal fun BookmarkButton(
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    circled: Boolean = false,
    circle: Color = Color.Transparent,
) {
    val icon = @Composable {
        Icon(
            imageVector = if (saved) BookmarkFilled else BookmarkOutline,
            contentDescription = stringResource(
                if (saved) R.string.bookmark_remove else R.string.bookmark_add,
            ),
            tint = Color.White,
            modifier = Modifier.size(if (circled) 22.dp else size),
        )
    }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        if (circled) {
            Box(
                modifier = modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(circle)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        } else {
            Box(
                modifier = modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
    }
}
