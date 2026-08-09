package com.link.core.presentation.utils

import com.link.core.R
import com.link.core.domain.models.NetworkError
import com.link.core.presentation.models.StringWrapper

public fun NetworkError.asStringWrapper(): StringWrapper {
    return when (this) {
        NetworkError.NoInternet -> StringWrapper.FromResource(R.string.no_internet_connection)
        NetworkError.Unauthorized -> StringWrapper.FromResource(R.string.unauthorized_session)
        NetworkError.Forbidden -> StringWrapper.FromResource(R.string.forbidden_access)
        is NetworkError.NotFound -> message?.let { StringWrapper.FromString(it) }
            ?: StringWrapper.FromResource(R.string.not_found)
        is NetworkError.ServerError -> message?.let { StringWrapper.FromString(it) }
            ?: StringWrapper.FromResource(R.string.something_went_wrong)
        NetworkError.Unknown -> StringWrapper.FromResource(R.string.something_went_wrong)
    }
}
