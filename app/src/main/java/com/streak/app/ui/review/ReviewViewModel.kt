package com.streak.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.review.LoadReviewUseCase
import com.streak.app.domain.review.Review
import com.streak.app.domain.review.ReviewPeriod
import com.streak.app.domain.review.ReviewRange
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ReviewUiState(
    val range: ReviewRange? = null,
    val review: Review? = null,
    val today: LocalDate = LocalDate.now()
) {
    // Prihodnjih obdobij ni
    val canGoNext: Boolean get() = range != null && !range.next().start.isAfter(today)
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val loadReview: LoadReviewUseCase,
    private val dayClock: DayClock
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()
    private var loading: Job? = null

    init {
        viewModelScope.launch {
            val today = dayClock.today()
            _state.update { it.copy(today = today) }
            show(ReviewRange.of(ReviewPeriod.MONTH, today))
        }
    }

    fun setPeriod(period: ReviewPeriod) {
        val current = _state.value.range ?: return
        if (current.period != period) show(ReviewRange.of(period, current.start))
    }

    fun previous() {
        _state.value.range?.let { show(it.previous()) }
    }

    fun next() {
        if (_state.value.canGoNext) _state.value.range?.let { show(it.next()) }
    }

    private fun show(range: ReviewRange) {
        _state.update { it.copy(range = range, review = null) }
        loading?.cancel()
        loading = viewModelScope.launch {
            val review = loadReview(range)
            _state.update { if (it.range == range) it.copy(review = review) else it }
        }
    }
}
