package com.link.core.data.firebase.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.link.core.domain.models.Resource
import com.link.core.domain.models.NetworkError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreService {

    override suspend fun <T : Any> add(path: String, docId: String?, item: T): Resource<Unit> = try {
        val collection = firestore.collection(path)
        if (docId != null) {
            collection.document(docId).set(item).await()
        } else {
            collection.add(item).await()
        }
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(NetworkError.ServerError(code = null, message = e.message))
    }

    override suspend fun <T : Any> getAll(path: String, clazz: Class<T>): Resource<List<T>> = try {
        val snapshot = firestore.collection(path).get().await()
        val data = snapshot.toObjects(clazz)
        Resource.Success(data)
    } catch (e: Exception) {
        Resource.Error(NetworkError.ServerError(code = null, message = e.message))
    }

    override suspend fun delete(path: String, docId: String): Resource<Unit> = try {
        firestore.collection(path).document(docId).delete().await()
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(NetworkError.ServerError(code = null, message = e.message))
    }

    override fun <T : Any> observe(path: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val listener = firestore.collection(path).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(clazz).orEmpty())
        }
        awaitClose { listener.remove() }
    }
}
