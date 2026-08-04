package com.zeuroux.launchly.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun awaitSessionWaitsForEncryptedStoreRestoration() = runBlocking {
        val restored = CompletableDeferred<AuthSession?>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = DefaultAuthRepository(
            object : AuthStore {
                override suspend fun read(): AuthSession? = restored.await()
                override suspend fun write(session: AuthSession) = Unit
                override suspend fun clear() = Unit
            },
            scope
        )
        val expected = AuthSession("owner@example.test", "token", null, null, 1)

        val waiting = async { repository.awaitSession() }
        assertFalse(waiting.isCompleted)
        restored.complete(expected)

        assertEquals(expected, waiting.await())
        scope.cancel()
    }

    @Test
    fun awaitSessionDoesNotHangWhenEncryptedStoreReadFails() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = DefaultAuthRepository(
            object : AuthStore {
                override suspend fun read(): AuthSession? = error("corrupt store")
                override suspend fun write(session: AuthSession) = Unit
                override suspend fun clear() = Unit
            },
            scope
        )

        assertEquals(null, repository.awaitSession())
        scope.cancel()
    }

    @Test
    fun signInWaitsForRestorationAndCannotBeOverwrittenByIt() = runBlocking {
        val restored = CompletableDeferred<AuthSession?>()
        var persisted: AuthSession? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = DefaultAuthRepository(
            object : AuthStore {
                override suspend fun read(): AuthSession? = restored.await()
                override suspend fun write(session: AuthSession) {
                    persisted = session
                }
                override suspend fun clear() = Unit
            },
            scope
        )
        val old = AuthSession("old@example.test", "old-token", null, null, 1)
        val replacement = AuthSession("new@example.test", "new-token", null, null, 2)

        val signingIn = async(start = CoroutineStart.UNDISPATCHED) { repository.signIn(replacement) }
        assertFalse(signingIn.isCompleted)
        restored.complete(old)

        assertEquals(AuthResult.Success, signingIn.await())
        assertEquals(replacement, persisted)
        assertEquals(replacement, repository.currentSession())
        scope.cancel()
    }
}