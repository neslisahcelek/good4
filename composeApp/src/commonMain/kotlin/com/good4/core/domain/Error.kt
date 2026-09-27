package com.good4.core.domain

interface Error {
    val message: String
}

data class NetworkError(override val message: String) : Error
data class DatabaseError(override val message: String) : Error
data class ValidationError(override val message: String) : Error
data class UnknownError(override val message: String) : Error