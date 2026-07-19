package com.zeuroux.launchly.diagnostics

import android.os.Build
import com.zeuroux.launchly.LaunchlyApp
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class DiagnosticExporter(private val app: LaunchlyApp) {
    suspend fun createJson(): String {
        val downloads = app.container.downloads.downloads.first()
        val installed = app.container.packages.installedPackage.value
        return JSONObject()
            .put("generatedAt", System.currentTimeMillis())
            .put("appVersion", com.zeuroux.launchly.BuildConfig.VERSION_NAME)
            .put("appVersionCode", com.zeuroux.launchly.BuildConfig.VERSION_CODE)
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("abis", JSONArray(Build.SUPPORTED_ABIS.toList()))
            .put("installedMinecraftVersion", installed?.versionName)
            .put("installedMinecraftVersionCode", installed?.versionCode)
            .put("downloads", JSONArray(downloads.map { download ->
                JSONObject()
                    .put("managedVersionId", download.versionId)
                    .put("status", download.status.name)
                    .put("bytes", download.bytesDownloaded)
                    .put("total", download.totalBytes)
                    .put("failureType", download.failureType)
                    .put("failureMessage", download.failureMessage)
            }))
            .toString(2)
    }
}
