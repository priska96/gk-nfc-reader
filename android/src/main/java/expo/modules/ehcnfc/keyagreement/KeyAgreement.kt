package expo.modules.ehcnfc.keyagreement

import at.asitplus.signum.indispensable.symmetric.*
import at.asitplus.signum.supreme.symmetric.*

import expo.modules.ehcnfc.cardaccess.CAN
import expo.modules.ehcnfc.healthcard.HealthCardType
import expo.modules.ehcnfc.pace.PaceInfo
import expo.modules.ehcnfc.pace.PaceKey
import expo.modules.ehcnfc.pace.PaceSession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.bouncycastle.crypto.params.*

// KeyAgreement sealed class
sealed class KeyAgreement {
    // Algorithm as nested sealed class / object
    sealed class Algorithm {
        /**
         * PACE algorithm: id-PACE-ECDH-GM-AES-CBC-CMAC-128
         */
        object IdPaceEcdhGmAesCbcCmac128 : Algorithm() {

            // Suspend function to negotiate session key
            suspend fun negotiateSessionKeyAsync(
                card: HealthCardType,
                canStr: String,
                efCardAccessResData: ByteArray?
            ): PaceKey {

                requireNotNull(efCardAccessResData) {throw KeyAgreementError.EfCardAccessNotAvailable}
                val paceInfo = PaceInfo(efCardAccessResData)
                val can = CAN.from(canStr.toByteArray(Charsets.UTF_8))

                // Create a session instance
                val session = PaceSession(card, paceInfo, can)

                // Last expression in withContext is automatically returned
                return session.performNegotiationAsync()
            }
        }
    }
}