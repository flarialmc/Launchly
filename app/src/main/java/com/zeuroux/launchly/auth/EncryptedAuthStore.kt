package com.zeuroux.launchly.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AuthStore {
    suspend fun read(): AuthSession?
    suspend fun write(session: AuthSession)
    suspend fun clear()
}

class EncryptedAuthStore(context: Context) : AuthStore {
    private val sessionFile = File(context.noBackupFilesDir, SESSION_FILE)
    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    override suspend fun read(): AuthSession? = withContext(Dispatchers.IO) {
        if (!sessionFile.isFile) return@withContext null
        runCatching {
            val envelope = JSONObject(sessionFile.readText(Charsets.UTF_8))
            val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            }
            sessionFromJson(JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8)))
        }.getOrElse {
            clearInternal(deleteKey = true)
            null
        }
    }

    override suspend fun write(session: AuthSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val ciphertext = cipher.doFinal(sessionToJson(session).toString().toByteArray(Charsets.UTF_8))
        val envelope = JSONObject()
            .put("version", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        sessionFile.parentFile?.mkdirs()
        val temporary = File(sessionFile.parentFile, "${sessionFile.name}.tmp")
        temporary.writeText(envelope.toString(), Charsets.UTF_8)
        Files.move(
            temporary.toPath(),
            sessionFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
        Unit
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        clearInternal(deleteKey = false)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun clearInternal(deleteKey: Boolean) {
        sessionFile.delete()
        File(sessionFile.parentFile, "${sessionFile.name}.tmp").delete()
        if (deleteKey && keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun sessionToJson(value: AuthSession) = JSONObject()
        .put("email", value.email)
        .put("aasToken", value.aasToken)
        .put("displayName", value.displayName)
        .put("profileArtworkUrl", value.profileArtworkUrl)
        .put("createdAt", value.createdAt)

    private fun sessionFromJson(value: JSONObject) = AuthSession(
        email = value.getString("email"),
        aasToken = value.getString("aasToken"),
        displayName = value.optString("displayName").takeIf { it.isNotBlank() && it != "null" },
        profileArtworkUrl = value.optString("profileArtworkUrl").takeIf { it.isNotBlank() && it != "null" },
        createdAt = value.getLong("createdAt")
    )

    companion object {
        const val SESSION_FILE = "auth_session.enc"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "launchly_auth_session_v2"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
    }
}
