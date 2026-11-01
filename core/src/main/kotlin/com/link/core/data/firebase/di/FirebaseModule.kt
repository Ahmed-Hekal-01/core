package com.link.core.data.firebase.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.link.core.data.firebase.auth.AuthService
import com.link.core.data.firebase.auth.FirebaseAuthService
import com.link.core.data.firebase.firestore.FirestoreService
import com.link.core.data.firebase.firestore.FirestoreServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
public object FirebaseProvidesModule {
    @Provides
    @Singleton
    public fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    public fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
public abstract class FirebaseBindsModule {
    @Binds
    @Singleton
    public abstract fun bindAuthService(impl: FirebaseAuthService): AuthService

    @Binds
    @Singleton
    public abstract fun bindFirestoreService(impl: FirestoreServiceImpl): FirestoreService
}
