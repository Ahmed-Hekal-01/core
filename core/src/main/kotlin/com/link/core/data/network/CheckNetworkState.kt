package com.link.core.data.network

internal fun interface CheckNetworkState {
    fun isConnected(): Boolean
}
