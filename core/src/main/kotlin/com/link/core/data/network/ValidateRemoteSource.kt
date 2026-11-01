package com.link.core.data.network

import com.link.core.domain.models.NetworkError
import com.link.core.domain.models.Resource

import javax.inject.Inject

internal interface ValidateRemoteSource {
    suspend fun <T> validate(apiCall: suspend () -> T): Resource<T>
}

internal class ValidateRemoteSourceImpl @Inject constructor() : ValidateRemoteSource {
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
        } catch (e: io.ktor.client.plugins.ResponseException) {
            val status = e.response.status.value
            val message = e.response.status.description
            when (status) {
                401 -> Resource.Error(NetworkError.Unauthorized)
                403 -> Resource.Error(NetworkError.Forbidden)
                404 -> Resource.Error(NetworkError.NotFound(message))
                else -> Resource.Error(NetworkError.ServerError(code = status, message = message))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(NetworkError.Unknown)
        }
    }
}
