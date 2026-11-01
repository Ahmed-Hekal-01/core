package com.link.core.data.firebase.auth

import com.google.firebase.auth.FirebaseAuth
import com.link.core.domain.models.Resource
import com.link.core.domain.models.NetworkError
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthService {

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun login(email: String, password: String): Resource<Unit> = try {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(NetworkError.ServerError(code = null, message = e.message))
    }

    override suspend fun register(email: String, password: String): Resource<String> = try {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("No UID returned")
        Resource.Success(uid)
    } catch (e: Exception) {
        Resource.Error(NetworkError.ServerError(code = null, message = e.message))
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
