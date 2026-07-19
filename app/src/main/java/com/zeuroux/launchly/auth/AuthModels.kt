package com.zeuroux.launchly.auth

data class AuthSession(
    val email: String,
    val aasToken: String,
    val displayName: String?,
    val profileArtworkUrl: String?,
    val createdAt: Long
)

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class Authenticated(
        val session: AuthSession,
        val profileError: String? = null
    ) : AuthState
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Offline(val message: String) : AuthResult
    data class Expired(val message: String) : AuthResult
    data class AccountNotEntitled(val message: String) : AuthResult
    data class Failure(val message: String) : AuthResult
}
