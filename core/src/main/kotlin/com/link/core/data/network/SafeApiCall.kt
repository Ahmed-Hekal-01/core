package com.link.core.data.network

import com.link.core.domain.models.Resource

public interface SafeApiCall {
    public suspend operator fun <T> invoke(apiCall: suspend () -> T): Resource<T>
}
