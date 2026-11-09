package com.example.movie.data.cast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TvCastUi(
    val message: String = "",
    val busy: Boolean = false,
    val devices: List<DlnaDevice> = emptyList(),
    val browserUrl: String? = null,
    val streamUrl: String? = null,
    val playingOn: String? = null,
    val hls: Boolean = false,
    val running: Boolean = false,
    val needsPin: Boolean = false,
)

object TvCastHub {
    private val mutable = MutableStateFlow(TvCastUi())
    val state: StateFlow<TvCastUi> = mutable.asStateFlow()

    internal fun update(block: (TvCastUi) -> TvCastUi) {
        mutable.update(block)
    }
}
