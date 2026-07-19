package com.zeuroux.launchly.packageops

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.android.apksig.ApkVerifier
import com.android.apksig.apk.ApkUtils
import com.android.apksig.util.DataSources
import com.zeuroux.launchly.data.AppPreferences
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.MINECRAFT_PACKAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

class ApkValidationException(message: String) : Exception(message)

class ApkSetValidator(
    private val context: Context,
    private val preferences: AppPreferences
) {
    suspend fun validate(
        files: List<File>,
        expectedVersionCode: Long,
        expectedArchitecture: Architecture
    ) = withContext(Dispatchers.IO) {
        if (files.isEmpty()) throw ApkValidationException("The download did not contain any APK files.")
        var hasBase = false
        var expectedSigner: String? = null
        val discoveredAbis = mutableSetOf<String>()

        files.forEach { file ->
            if (!file.isFile || file.length() <= 0L) throw ApkValidationException("${file.name} is missing or empty.")
            val verification = ApkVerifier.Builder(file).build().verify()
            if (!verification.isVerified) {
                val reason = verification.errors.firstOrNull()?.toString() ?: "signature verification failed"
                throw ApkValidationException("${file.name} is not a valid signed APK: $reason")
            }
            val signer = verification.signerCertificates
                .map { sha256(it.encoded) }
                .sorted()
                .joinToString(":")
            if (signer.isBlank()) throw ApkValidationException("${file.name} has no verified signer.")
            if (expectedSigner == null) expectedSigner = signer
            if (signer != expectedSigner) throw ApkValidationException("The APK splits are signed by different certificates.")

            val manifest = runCatching {
                RandomAccessFile(file, "r").use { randomAccessFile ->
                    ApkUtils.getAndroidManifest(DataSources.asDataSource(randomAccessFile))
                }
            }.getOrElse { throw ApkValidationException("${file.name} could not be parsed as an Android package.") }
            if (ApkUtils.getPackageNameFromBinaryAndroidManifest(manifest.duplicate()) != MINECRAFT_PACKAGE) {
                throw ApkValidationException("${file.name} is not the Minecraft package.")
            }
            val versionCode = ApkUtils.getLongVersionCodeFromBinaryAndroidManifest(manifest.duplicate())
            if (versionCode != expectedVersionCode) {
                throw ApkValidationException("${file.name} has Minecraft version $versionCode, not $expectedVersionCode.")
            }
            if (file.name.equals("base.apk", true)) hasBase = true
            ZipFile(file).use { zip ->
                zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("lib/") }
                    .mapNotNull { it.split('/').getOrNull(1) }
                    .forEach(discoveredAbis::add)
            }
        }

        if (!hasBase) throw ApkValidationException("The APK set does not include a base APK.")
        if (discoveredAbis.isNotEmpty() && expectedArchitecture.abi !in discoveredAbis) {
            throw ApkValidationException("The APK set is not compatible with ${expectedArchitecture.abi}.")
        }
        val signer = expectedSigner ?: throw ApkValidationException("No signer could be established.")
        val installedSigner = installedMinecraftSigner()
        val trustedSigner = preferences.trustedMinecraftSigner()
        when (val decision = SignerTrustPolicy.decide(installedSigner, trustedSigner, signer)) {
            SignerTrustDecision.Accept -> Unit
            SignerTrustDecision.AcceptAndPin -> preferences.setTrustedMinecraftSigner(signer)
            is SignerTrustDecision.Reject -> throw ApkValidationException(decision.message)
        }
        Unit
    }

    private fun installedMinecraftSigner(): String? = runCatching {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(MINECRAFT_PACKAGE, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(MINECRAFT_PACKAGE, flags)
        }
        val signatures = if (info.signingInfo?.hasMultipleSigners() == true) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signingInfo?.signingCertificateHistory
        }.orEmpty()
        signatures.map { sha256(it.toByteArray()) }.sorted().joinToString(":").takeIf(String::isNotBlank)
    }.getOrNull()

    private fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(Locale.ROOT, it) }
}
