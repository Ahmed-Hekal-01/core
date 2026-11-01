package com.link.core.domain

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide signal emitted when an opted-in Ktor client receives HTTP 401
 * (see `KtorClientFactory.createClient(notifyOnUnauthorized = true)`).
 *
 * Never suspends or blocks the network call: the buffer holds one event and drops the oldest.
 */
@Singleton
public class SessionExpiredSignal @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits once per notified 401. Collect at app level to log the user out. */
    public val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Non-suspending; safe to call from any thread. */
    public fun notifyExpired() {
        _events.tryEmit(Unit)
    }
}
