package com.link.core.data.firebase.firestore

import com.link.core.domain.models.Resource
import kotlinx.coroutines.flow.Flow

public interface FirestoreService {
    public suspend fun <T : Any> add(path: String, docId: String? = null, item: T): Resource<Unit>
    public suspend fun <T : Any> getAll(path: String, clazz: Class<T>): Resource<List<T>>
    public suspend fun delete(path: String, docId: String): Resource<Unit>
    public fun <T : Any> observe(path: String, clazz: Class<T>): Flow<List<T>>
}

public suspend inline fun <reified T : Any> FirestoreService.getAll(path: String): Resource<List<T>> {
    return this.getAll(path, T::class.java)
}

public inline fun <reified T : Any> FirestoreService.observe(path: String): Flow<List<T>> {
    return this.observe(path, T::class.java)
}
