package expo.modules.ehcnfc.session

import android.nfc.tech.IsoDep
import android.util.Log
import expo.modules.ehcnfc.cardaccess.CardAccess
import expo.modules.ehcnfc.healthcard.HealthCard
import expo.modules.ehcnfc.healthcard.HealthCardStatus
import expo.modules.ehcnfc.healthcard.HealthCardType
import expo.modules.ehcnfc.keyagreement.KeyAgreement
import expo.modules.ehcnfc.pace.PaceKey

/**
 * High-level entrypoint for establishing a secure NFC session.
 *
 * Responsibilities:
 * - Connect to IsoDep
 * - Determine card type and EF.CardAccess
 * - Negotiate PACE session (returns a PaceKey)
 * - Initialize a SecureHealthCard instance
 */
class SecureNFCSession(
    private val isoDep: IsoDep,
    private val can: String
) {

    private lateinit var healthCard: HealthCardType

    /**
     * Establishes PACE session and returns a session key.
     */
    suspend fun openSecureNFCSessionAsync(): PaceKey {
        isoDep.connect()
        isoDep.timeout = 30_000 // 30 seconds

        try {
            val cardAccess = CardAccess(isoDep)
            Log.d("SecureNFCSession", "openSecureNFCSessionAsync CardAccess: $cardAccess")

            // Step 1: Determine application identifier
            val cardAid = cardAccess.determineCardAidAsync()
            Log.d("SecureNFCSession", "openSecureNFCSessionAsync cardAid: $cardAid")

            // Step 2: Read EF.CardAccess and determine the algorithm for the key agreement (e.g. PACE)
            val efCardAccessResData = cardAccess.readKeyAgreementAlgorithmAsync(cardAid)
            Log.d("SecureNFCSession", "openSecureNFCSessionAsync efCardAccessResData: ${efCardAccessResData}")

            // Step 3:Read EF.Version2 and determine health card type
            val cardType = cardAccess.readCardTypeAsync(cardAid)
            Log.d("SecureNFCSession", "openSecureNFCSessionAsync readCardTypeAsync: ${cardType}")


            // Step 4: Wrap in HealthCard object
            healthCard = HealthCard(isoDep, HealthCardStatus.Valid(cardType))

            // Step 5: Negotiate PACE session
            return KeyAgreement.Algorithm.IdPaceEcdhGmAesCbcCmac128
                .negotiateSessionKeyAsync(healthCard, can, efCardAccessResData)
        } catch(e: Exception){
            Log.e("SecureNFCSession", "Error while establishing secure session: ${e.message}")
            throw e
        }
    }

    /**
     * Initializes a SecureHealthCard for performing secure operations.
     */
    fun initializeSecureHealthCard(
        paceKey: PaceKey
    ): SecureHealthCard {
        val secureMessaging = SecureMessaging(paceKey)
        return SecureHealthCard(healthCard, secureMessaging)
    }

    fun close() {
        if (this::healthCard.isInitialized) {
            healthCard.disconnect()
        } else if (isoDep.isConnected) {
            isoDep.close()
        }
    }
}
