package com.link.core.domain.models

public sealed interface Resource<out T> {
    public data class Success<out T>(public val data: T) : Resource<T>
    public data class Error(public val error: NetworkError) : Resource<Nothing>
}
