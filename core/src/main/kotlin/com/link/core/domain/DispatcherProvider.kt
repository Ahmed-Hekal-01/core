package com.link.core.domain

import kotlinx.coroutines.CoroutineDispatcher

public interface DispatcherProvider {
    public val io: CoroutineDispatcher
    public val main: CoroutineDispatcher
    public val default: CoroutineDispatcher
}
