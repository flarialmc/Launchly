package com.zeuroux.launchly.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zeuroux.launchly.model.Architecture
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

    suspend fun catalogEtag(): String? = context.launchlyDataStore.data
        .map { it[CATALOG_ETAG] }
        .first()

    suspend fun setCatalogEtag(value: String?) {
        context.launchlyDataStore.edit { preferences ->
            if (value == null) preferences.remove(CATALOG_ETAG) else preferences[CATALOG_ETAG] = value
        }
    }

    val catalogSource: Flow<String> = context.launchlyDataStore.data.map { values ->
        values[CATALOG_SOURCE]?.takeUnless { it.contains("{abi}") } ?: DEFAULT_CATALOG_SOURCE
    }

    suspend fun currentCatalogSource(): String = catalogSource.first()

    suspend fun setCatalogSource(source: String) {
        context.launchlyDataStore.edit { preferences ->
            preferences[CATALOG_SOURCE] = source
        }
    }

    suspend fun clearCatalogEtags() {
        context.launchlyDataStore.edit { preferences ->
            preferences.remove(CATALOG_ETAG)
            Architecture.entries.forEach { architecture ->
                preferences.remove(stringPreferencesKey("$CATALOG_ETAG_PREFIX${architecture.abi}"))
            }
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
        private val CATALOG_SOURCE = stringPreferencesKey("catalog_source")
        private val CATALOG_ETAG = stringPreferencesKey("catalog_etag")
        private val INSTALLER_SESSION_ID = stringPreferencesKey("installer_session_id")
        private val INSTALLER_VERSION_ID = stringPreferencesKey("installer_version_id")
        private const val CATALOG_ETAG_PREFIX = "catalog_etag_"
        const val DEFAULT_CATALOG_SOURCE =
            "https://raw.githubusercontent.com/flarialmc/newcdn/refs/heads/main/Android/versions.json"
    }
}
