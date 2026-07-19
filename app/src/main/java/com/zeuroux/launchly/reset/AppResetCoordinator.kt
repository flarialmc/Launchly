package com.zeuroux.launchly.reset

import android.content.Context
import android.webkit.CookieManager
import androidx.work.WorkManager
import androidx.core.content.edit
import com.zeuroux.launchly.auth.EncryptedAuthStore
import com.zeuroux.launchly.download.DownloadCoordinator
import java.io.File
import java.util.concurrent.TimeUnit

data class ResetResult(val didResetLegacyData: Boolean)

class AppResetCoordinator(private val context: Context) {
    private val epochPreferences = context.getSharedPreferences(EPOCH_PREFERENCES, Context.MODE_PRIVATE)

    fun runBeforeRepositories(): ResetResult {
        if (epochPreferences.getInt(KEY_DATA_EPOCH, 0) >= DATA_EPOCH) {
            return ResetResult(epochPreferences.getBoolean(KEY_RESET_NOTICE, false))
        }

        val hadLegacyData = listOf(
            context.getDatabasePath("version.db"),
            File(context.filesDir, "versions"),
            File(context.applicationInfo.dataDir, "shared_prefs/accountData.xml"),
            File(context.applicationInfo.dataDir, "shared_prefs/onboarding.xml")
        ).any(File::exists)

        runCatching {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(DownloadCoordinator.LAUNCHLY_WORK_TAG)
                .result.get(10, TimeUnit.SECONDS)
        }

        listOf("accountData", "onboarding", "ui_prefs", "version_list_prefs").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit(commit = true) { clear() }
        }
        context.deleteDatabase("version.db")
        context.deleteDatabase("launchly.db")
        File(context.filesDir, "versions").deleteRecursivelySafely(context.filesDir)
        File(context.noBackupFilesDir, EncryptedAuthStore.SESSION_FILE).delete()
        File(context.filesDir.parentFile, "datastore/launchly.preferences_pb").delete()
        clearWebCookies()

        epochPreferences.edit(commit = true) {
            putInt(KEY_DATA_EPOCH, DATA_EPOCH)
            putBoolean(KEY_RESET_NOTICE, hadLegacyData)
        }
        return ResetResult(hadLegacyData)
    }

    fun consumeResetNotice() {
        epochPreferences.edit { putBoolean(KEY_RESET_NOTICE, false) }
    }

    private fun clearWebCookies() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    private fun File.deleteRecursivelySafely(allowedParent: File) {
        val canonicalParent = allowedParent.canonicalFile
        val canonicalTarget = canonicalFile
        if (canonicalTarget.parentFile == canonicalParent && canonicalTarget.name == "versions") {
            canonicalTarget.deleteRecursively()
        }
    }

    companion object {
        const val DATA_EPOCH = 2
        private const val EPOCH_PREFERENCES = "launchly_data_epoch"
        private const val KEY_DATA_EPOCH = "data_epoch"
        private const val KEY_RESET_NOTICE = "reset_notice_pending"
    }
}
