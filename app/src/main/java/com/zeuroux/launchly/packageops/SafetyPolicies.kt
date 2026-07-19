package com.zeuroux.launchly.packageops

internal object DowngradePolicy {
    fun requiresConfirmation(currentVersionCode: Long?, targetVersionCode: Long): Boolean =
        currentVersionCode != null && targetVersionCode < currentVersionCode
}

internal sealed interface SignerTrustDecision {
    data object Accept : SignerTrustDecision
    data object AcceptAndPin : SignerTrustDecision
    data class Reject(val message: String) : SignerTrustDecision
}

internal object SignerTrustPolicy {
    fun decide(installed: String?, pinned: String?, downloaded: String): SignerTrustDecision = when {
        installed != null && downloaded != installed -> SignerTrustDecision.Reject(
            "The APK signer does not match the installed Minecraft app."
        )
        installed == null && pinned != null && downloaded != pinned -> SignerTrustDecision.Reject(
            "The APK signer does not match the previously trusted Minecraft signer."
        )
        installed == null && pinned == null -> SignerTrustDecision.AcceptAndPin
        else -> SignerTrustDecision.Accept
    }
}
