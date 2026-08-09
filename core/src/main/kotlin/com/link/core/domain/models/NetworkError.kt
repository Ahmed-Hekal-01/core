package com.link.core.domain.models

public sealed interface NetworkError {
    public data object NoInternet : NetworkError
    public data object Unauthorized : NetworkError
    public data object Forbidden : NetworkError
    public data class NotFound(public val message: String? = null) : NetworkError
    public data class ServerError(public val code: Int?, public val message: String? = null) : NetworkError
    public data object Unknown : NetworkError
}
