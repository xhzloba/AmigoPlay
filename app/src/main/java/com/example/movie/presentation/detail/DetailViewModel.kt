package com.example.movie.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.usecase.GetMovieDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val movieId: String,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val loadFranchise: suspend (String) -> Franchise?,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState(phase = DetailPhase.Loading)
            getMovieDetail(movieId)
                .onSuccess { detail ->
                    _state.value = DetailUiState(detail = detail, phase = DetailPhase.Ready)
                    val extra = loadFranchise(movieId) ?: return@onSuccess
                    val current = _state.value.detail
                    if (current?.id == movieId) {
                        _state.value = _state.value.copy(detail = current.copy(franchise = extra))
                    }
                }
                .onFailure { error ->
                    _state.value = DetailUiState(phase = DetailPhase.Failed(error.asDataError()))
                }
        }
    }
}

private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.Unknown()
