package com.zeuroux.launchly.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signIn(session: AuthSession): AuthResult
    suspend fun signOut()
    suspend fun refreshProfile(): AuthResult
    fun currentSession(): AuthSession?
}

class DefaultAuthRepository(
    private val store: AuthStore,
    private val applicationScope: CoroutineScope
) : AuthRepository {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()
    private var profileLoader: (suspend () -> AuthSession)? = null

    init {
        applicationScope.launch {
            _state.value = store.read()?.let { AuthState.Authenticated(it) } ?: AuthState.SignedOut
        }
    }

    override suspend fun signIn(session: AuthSession): AuthResult = runCatching {
        require(session.email.isNotBlank() && session.aasToken.isNotBlank())
        store.write(session)
        _state.value = AuthState.Authenticated(session)
        AuthResult.Success
    }.getOrElse {
        AuthResult.Failure("The sign-in session could not be saved securely.")
    }

    override suspend fun signOut() {
        store.clear()
        _state.value = AuthState.SignedOut
    }

    override suspend fun refreshProfile(): AuthResult {
        val current = currentSession() ?: return AuthResult.Expired("Sign in again to refresh your profile.")
        val loader = profileLoader ?: return AuthResult.Failure("Profile refresh is not ready yet.")
        return runCatching {
            val updated = loader()
            store.write(updated)
            _state.value = AuthState.Authenticated(updated)
            AuthResult.Success
        }.getOrElse { failure ->
            val message = failure.message ?: "Profile refresh failed."
            _state.value = AuthState.Authenticated(current, message)
            AuthResult.Failure(message)
        }
    }

    override fun currentSession(): AuthSession? = (_state.value as? AuthState.Authenticated)?.session

    fun setProfileLoader(loader: suspend () -> AuthSession) {
        profileLoader = loader
    }
}
