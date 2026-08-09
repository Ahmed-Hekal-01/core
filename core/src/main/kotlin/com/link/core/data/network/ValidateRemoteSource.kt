package com.link.core.data.network

import com.link.core.domain.models.NetworkError
import com.link.core.domain.models.Resource

internal interface ValidateRemoteSource {
    suspend fun <T> validate(apiCall: suspend () -> T): Resource<T>
}

internal class ValidateRemoteSourceImpl : ValidateRemoteSource {
    override suspend fun <T> validate(apiCall: suspend () -> T): Resource<T> {
        return try {
            val result = apiCall()
            Resource.Success(result)
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> Resource.Error(NetworkError.Unauthorized)
                403 -> Resource.Error(NetworkError.Forbidden)
                404 -> Resource.Error(NetworkError.NotFound(e.message()))
                else -> Resource.Error(NetworkError.ServerError(code = e.code(), message = e.message()))
            }
        } catch (_: Exception) {
            Resource.Error(NetworkError.Unknown)
        }
    }
}
