package com.zeuroux.launchly.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signIn(session: AuthSession): AuthResult
    suspend fun signOut()
    suspend fun refreshProfile(): AuthResult
    suspend fun awaitSession(): AuthSession?
    fun currentSession(): AuthSession?
}

class DefaultAuthRepository(
    private val store: AuthStore,
    private val applicationScope: CoroutineScope
) : AuthRepository {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val state: StateFlow<AuthState> = _state.asStateFlow()
    private var profileLoader: (suspend () -> AuthSession)? = null

    private val restoration = applicationScope.async {
        val restored = try {
            store.read()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        _state.value = restored
            ?.let { AuthState.Authenticated(it) }
            ?: AuthState.SignedOut
    }

    override suspend fun signIn(session: AuthSession): AuthResult {
        restoration.await()
        return runCatching {
            require(session.email.isNotBlank() && session.aasToken.isNotBlank())
            store.write(session)
            _state.value = AuthState.Authenticated(session)
            AuthResult.Success
        }.getOrElse {
            AuthResult.Failure("The sign-in session could not be saved securely.")
        }
    }

    override suspend fun signOut() {
        restoration.await()
        store.clear()
        _state.value = AuthState.SignedOut
    }

    override suspend fun refreshProfile(): AuthResult {
        restoration.await()
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

    override suspend fun awaitSession(): AuthSession? {
        restoration.await()
        return currentSession()
    }

    override fun currentSession(): AuthSession? = (_state.value as? AuthState.Authenticated)?.session

    fun setProfileLoader(loader: suspend () -> AuthSession) {
        profileLoader = loader
    }
}
