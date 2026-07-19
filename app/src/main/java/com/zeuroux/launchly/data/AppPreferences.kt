package com.zeuroux.launchly.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launchlyDataStore by preferencesDataStore(name = "launchly")

class AppPreferences(private val context: Context) {
    val onboardingComplete: Flow<Boolean> = context.launchlyDataStore.data.map { values ->
        values[ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.launchlyDataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun trustedMinecraftSigner(): String? = context.launchlyDataStore.data
        .map { it[TRUSTED_MINECRAFT_SIGNER] }
        .first()

    suspend fun setTrustedMinecraftSigner(sha256: String) {
        context.launchlyDataStore.edit { it[TRUSTED_MINECRAFT_SIGNER] = sha256.lowercase() }
    }

    suspend fun catalogEtag(abi: String): String? = context.launchlyDataStore.data
        .map { it[stringPreferencesKey("catalog_etag_$abi")] }
        .first()

    suspend fun setCatalogEtag(abi: String, value: String?) {
        context.launchlyDataStore.edit { preferences ->
            val key = stringPreferencesKey("catalog_etag_$abi")
            if (value == null) preferences.remove(key) else preferences[key] = value
        }
    }

    suspend fun installerSessionId(): String? = context.launchlyDataStore.data
        .map { it[INSTALLER_SESSION_ID] }
        .first()

    suspend fun installerVersionId(): String? = context.launchlyDataStore.data
        .map { it[INSTALLER_VERSION_ID] }
        .first()

    suspend fun setInstallerSession(sessionId: String?, versionId: String?) {
        context.launchlyDataStore.edit { preferences ->
            if (sessionId == null) preferences.remove(INSTALLER_SESSION_ID) else preferences[INSTALLER_SESSION_ID] = sessionId
            if (versionId == null) preferences.remove(INSTALLER_VERSION_ID) else preferences[INSTALLER_VERSION_ID] = versionId
        }
    }

    companion object {
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val TRUSTED_MINECRAFT_SIGNER = stringPreferencesKey("trusted_minecraft_signer_sha256")
        private val INSTALLER_SESSION_ID = stringPreferencesKey("installer_session_id")
        private val INSTALLER_VERSION_ID = stringPreferencesKey("installer_version_id")
    }
}
