package com.zeuroux.launchly.packageops

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyPoliciesTest {
    @Test
    fun downgradeRequiresConfirmationOnlyWhenTargetIsLower() {
        assertTrue(DowngradePolicy.requiresConfirmation(200, 100))
        assertFalse(DowngradePolicy.requiresConfirmation(100, 100))
        assertFalse(DowngradePolicy.requiresConfirmation(null, 100))
    }

    @Test
    fun signerTrustUsesInstalledSignerThenPinThenTofu() {
        assertTrue(SignerTrustPolicy.decide("a", "old", "a") is SignerTrustDecision.Accept)
        assertTrue(SignerTrustPolicy.decide("a", null, "b") is SignerTrustDecision.Reject)
        assertTrue(SignerTrustPolicy.decide(null, "a", "b") is SignerTrustDecision.Reject)
        assertTrue(SignerTrustPolicy.decide(null, null, "a") is SignerTrustDecision.AcceptAndPin)
    }
}
