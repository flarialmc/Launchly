package com.zeuroux.launchly.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EncryptedAuthStoreTest {
    @Test
    fun sessionRoundTripsAndCorruptionSignsOut() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = EncryptedAuthStore(context)
        store.clear()
        val session = AuthSession("owner@example.test", "secret", "Owner", null, 42)
        store.write(session)
        assertEquals(session, store.read())

        File(context.noBackupFilesDir, EncryptedAuthStore.SESSION_FILE).writeText("corrupt")
        assertNull(store.read())
    }
}
