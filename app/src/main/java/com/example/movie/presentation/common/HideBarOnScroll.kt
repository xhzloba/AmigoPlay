package com.example.movie.presentation.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs

/** Как в полном списке: вниз — прячем бар, вверх / к верху — показываем. */
@Composable
internal fun HideBarOnScroll(
    enabled: Boolean,
    listState: LazyListState,
    onHideBar: (Boolean) -> Unit,
) {
    LaunchedEffect(listState, enabled) {
        if (!enabled) {
            onHideBar(false)
            return@LaunchedEffect
        }
        var anchorIndex = 0
        var anchorOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val delta = if (index != anchorIndex) (index - anchorIndex) * 240 else offset - anchorOffset
                val atTop = index == 0 && offset <= 4
                if (atTop || abs(delta) > 28) {
                    onHideBar(!atTop && delta > 0)
                    anchorIndex = index
                    anchorOffset = offset
                }
            }
    }
}

@Composable
internal fun HideBarOnScroll(
    enabled: Boolean,
    gridState: LazyGridState,
    onHideBar: (Boolean) -> Unit,
) {
    LaunchedEffect(gridState, enabled) {
        if (!enabled) {
            onHideBar(false)
            return@LaunchedEffect
        }
        var anchorIndex = 0
        var anchorOffset = 0
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val delta = if (index != anchorIndex) (index - anchorIndex) * 240 else offset - anchorOffset
                val atTop = index == 0 && offset <= 4
                if (atTop || abs(delta) > 28) {
                    onHideBar(!atTop && delta > 0)
                    anchorIndex = index
                    anchorOffset = offset
                }
            }
    }
}
