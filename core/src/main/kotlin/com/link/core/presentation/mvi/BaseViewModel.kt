package com.link.core.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

public abstract class BaseViewModel<
    STATE : State, EVENT : Event, ONE_TIME_ACTION : OneTimeAction> : ViewModel() {

    protected abstract fun createInitialState(): STATE
    protected abstract fun onEvent(event: EVENT)

    /**
     * Called when [onEvent] throws. Default logs and keeps the ViewModel alive.
     * Override to report or rethrow.
     */
    protected open fun onEventError(event: EVENT, error: Throwable) {
        android.util.Log.e("BaseViewModel", "Unhandled error for event $event", error)
    }

    private val initialState by lazy { createInitialState() }

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    public val state: StateFlow<STATE> = _state

    private val _events: MutableSharedFlow<EVENT> = MutableSharedFlow()

    private val _oneTimeAction: Channel<ONE_TIME_ACTION> = Channel()
    public val oneTimeAction: kotlinx.coroutines.flow.Flow<ONE_TIME_ACTION> = _oneTimeAction.receiveAsFlow()

    public val currentState: STATE get() = state.value

    init { subscribeToEvents() }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _events.collect { event ->
                try {
                    onEvent(event)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // One bad event must not kill event processing for this ViewModel.
                    onEventError(event, e)
                }
            }
        }
    }

    public fun sendEvent(event: EVENT) {
        viewModelScope.launch { _events.emit(event) }
    }

    protected fun sendOneTimeAction(action: ONE_TIME_ACTION) {
        viewModelScope.launch { _oneTimeAction.send(action) }
    }

    protected fun setState(reduce: STATE.() -> STATE) {
        _state.update { it.reduce() }
    }
}
