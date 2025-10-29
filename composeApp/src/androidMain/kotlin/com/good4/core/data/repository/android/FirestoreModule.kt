package com.good4.core.data.repository.android

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryImpl
import org.koin.dsl.module

val firestoreModule = module {
    single<FirestoreRepository> { FirestoreRepositoryImpl() } // Replace with actual Firestore implementation
}
