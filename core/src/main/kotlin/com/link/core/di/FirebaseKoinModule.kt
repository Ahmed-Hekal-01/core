package com.link.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.link.core.data.firebase.auth.AuthService
import com.link.core.data.firebase.auth.FirebaseAuthService
import com.link.core.data.firebase.firestore.FirestoreService
import com.link.core.data.firebase.firestore.FirestoreServiceImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Optional Firebase bindings. Load only if the app uses Firebase; the app must also
 * depend on firebase-auth / firebase-firestore and apply google-services.
 */
public val firebaseKoinModule: Module = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<FirebaseFirestore> { FirebaseFirestore.getInstance() }
    single<AuthService> { FirebaseAuthService(get()) }
    single<FirestoreService> { FirestoreServiceImpl(get()) }
}
