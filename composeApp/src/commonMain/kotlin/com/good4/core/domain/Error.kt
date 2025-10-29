package com.good4.core.domain

interface Error

data class NetworkError(val message: String) : Error
data class DatabaseError(val message: String) : Error
data class ValidationError(val message: String) : Error
data class UnknownError(val message: String) : Error