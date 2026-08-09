package com.link.core.data.firebase.auth

import com.link.core.domain.models.Resource

public interface AuthService {
    public val currentUserId: String?
    public val isLoggedIn: Boolean
    public suspend fun login(email: String, password: String): Resource<Unit>
    public suspend fun register(email: String, password: String): Resource<String>
    public fun logout()
}
