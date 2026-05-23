package com.good4.core.util

expect object AppEnvironment {
    val isEmailVerificationRequired: Boolean
    val isBusinessApprovalRequired: Boolean
    val isDebug: Boolean
}
