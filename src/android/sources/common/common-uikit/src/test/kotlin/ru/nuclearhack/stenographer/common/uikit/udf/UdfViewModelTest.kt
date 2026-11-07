package ru.nuclearhack.stenographer.common.uikit.udf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp

class UdfViewModelTest {

    private class CounterViewModel : UdfViewModel<Int, CounterViewModel.Action, String>(initialState = 0) {
        sealed interface Action {
            data object Increment : Action
            data object Report : Action
        }

        override fun onAction(action: Action) {
            when (action) {
                Action.Increment -> updateState { this + 1 }
                Action.Report -> sendEffect("count=$currentState")
            }
        }
    }

    @Test
    fun `actions update state and emit one-off effects`() = runTest {
        val viewModel = CounterViewModel()

        viewModel.onAction(CounterViewModel.Action.Increment)
        viewModel.onAction(CounterViewModel.Action.Increment)
        viewModel.onAction(CounterViewModel.Action.Report)

        assertEquals(2, viewModel.state.value)
        assertEquals("count=2", viewModel.effects.first())
    }

    @Test
    fun `timestamps are compact`() {
        assertEquals("00:01.2", formatTimestamp(1_250))
        assertEquals("02:05.0", formatTimestamp(125_000))
        assertEquals("1:02:05", formatTimestamp(3_725_010))
    }
}
