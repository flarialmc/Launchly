package com.zeuroux.launchly.gplay

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.getSystemService
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.helpers.UserProfileHelper
import com.zeuroux.launchly.auth.AuthRepository
import com.zeuroux.launchly.model.MINECRAFT_PACKAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Properties
import java.io.IOException

data class GPlayArtifact(
    val url: String,
    val name: String,
    val expectedSize: Long
)

data class GPlayProfile(
    val displayName: String?,
    val email: String?,
    val artworkUrl: String?
)

class GPlayDeliveryException(
    val type: String,
    override val message: String,
    val retryable: Boolean
) : Exception(message)

class GPlayService(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    suspend fun getArtifacts(versionCode: Long): List<GPlayArtifact> = withContext(Dispatchers.IO) {
        try {
            val files = PurchaseHelper(authData()).purchase(
                MINECRAFT_PACKAGE,
                versionCode.toInt(),
                0
            )
            files.mapNotNull { file ->
                val url = file.url.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val name = file.name.takeIf(String::isNotBlank) ?: return@mapNotNull null
                GPlayArtifact(url, name, file.size)
            }
        } catch (failure: GPlayDeliveryException) {
            throw failure
        } catch (failure: IOException) {
            throw GPlayDeliveryException("OFFLINE", "Google delivery is unavailable. Check your connection.", true)
        } catch (failure: Exception) {
            val detail = failure.message.orEmpty()
            val normalized = detail.lowercase(Locale.US)
            when {
                listOf("auth", "token", "401", "403").any(normalized::contains) ->
                    throw GPlayDeliveryException("AUTH_EXPIRED", "Your Google session expired. Sign in again.", false)
                listOf("purchase", "owned", "entitlement", "license", "licence").any(normalized::contains) ->
                    throw GPlayDeliveryException(
                        "ACCOUNT_NOT_ENTITLED",
                        "Google could not confirm that this account owns Minecraft.",
                        false
                    )
                else -> throw GPlayDeliveryException(
                    "DELIVERY_FAILURE",
                    detail.ifBlank { "Google delivery could not prepare this version." },
                    true
                )
            }
        }
    }

    suspend fun getProfile(): GPlayProfile = withContext(Dispatchers.IO) {
        val profile = UserProfileHelper(authData()).getUserProfile()
        GPlayProfile(profile?.name, profile?.email, profile?.artwork?.url)
    }

    private fun authData(): AuthData {
        val session = authRepository.currentSession()
            ?: throw GPlayDeliveryException("AUTH_EXPIRED", "Your Google session expired. Sign in again.", false)
        return AuthHelper.build(
            email = session.email,
            token = session.aasToken,
            tokenType = AuthHelper.Token.AAS,
            properties = nativeDeviceProperties()
        )
    }

    private fun nativeDeviceProperties(): Properties = Properties().apply {
        val configuration = context.resources.configuration
        val metrics = context.resources.displayMetrics
        val activityManager = context.getSystemService<ActivityManager>()
        setProperty("UserReadableName", "${Build.MANUFACTURER} ${Build.MODEL}")
        setProperty("Build.HARDWARE", Build.HARDWARE)
        setProperty("Build.RADIO", Build.getRadioVersion() ?: "unknown")
        setProperty("Build.FINGERPRINT", Build.FINGERPRINT)
        setProperty("Build.BRAND", Build.BRAND)
        setProperty("Build.DEVICE", Build.DEVICE)
        setProperty("Build.VERSION.SDK_INT", Build.VERSION.SDK_INT.toString())
        setProperty("Build.VERSION.RELEASE", Build.VERSION.RELEASE)
        setProperty("Build.MODEL", Build.MODEL)
        setProperty("Build.MANUFACTURER", Build.MANUFACTURER)
        setProperty("Build.PRODUCT", Build.PRODUCT)
        setProperty("Build.ID", Build.ID)
        setProperty("Build.BOOTLOADER", Build.BOOTLOADER)
        setProperty("TouchScreen", configuration.touchscreen.toString())
        setProperty("Keyboard", configuration.keyboard.toString())
        setProperty("Navigation", configuration.navigation.toString())
        setProperty("ScreenLayout", (configuration.screenLayout and 15).toString())
        setProperty("HasHardKeyboard", (configuration.keyboard == Configuration.KEYBOARD_QWERTY).toString())
        setProperty("HasFiveWayNavigation", (configuration.navigation == Configuration.NAVIGATIONHIDDEN_YES).toString())
        setProperty("Screen.Density", metrics.densityDpi.toString())
        setProperty("Screen.Width", metrics.widthPixels.toString())
        setProperty("Screen.Height", metrics.heightPixels.toString())
        setProperty("Platforms", Build.SUPPORTED_ABIS.joinToString(","))
        setProperty("Features", context.packageManager.systemAvailableFeatures.mapNotNull { it.name }.joinToString(","))
        setProperty("Locales", context.assets.locales.map { it.replace("-", "_") }.joinToString(","))
        setProperty("SharedLibraries", context.packageManager.systemSharedLibraryNames.orEmpty().joinToString(","))
        setProperty("GL.Version", activityManager?.deviceConfigurationInfo?.reqGlEsVersion?.toString() ?: "131072")
        setProperty("GL.Extensions", "")
        setProperty("Client", "android-google")
        setProperty("GSF.version", "203019037")
        setProperty("Vending.version", "82151710")
        setProperty("Vending.versionString", "21.5.17-21 [0] [PR] 326734551")
        setProperty("Roaming", "mobile-notroaming")
        setProperty("TimeZone", java.util.TimeZone.getDefault().id)
        setProperty("CellOperator", "310")
        setProperty("SimOperator", "38")
        setProperty("Locale", Locale.getDefault().toLanguageTag())
    }
}
