package expo.modules.ehcnfc.pace

import android.util.Log

import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.symmetric.*
import at.asitplus.signum.supreme.symmetric.*
import expo.modules.ehcnfc.apdu.ApduException
import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.apdu.ResponseStatus
import expo.modules.ehcnfc.cardaccess.CAN
import expo.modules.ehcnfc.cardaccess.Key
import expo.modules.ehcnfc.cardaccess.KeyDerivationFunction
import expo.modules.ehcnfc.cardaccess.Mode
import expo.modules.ehcnfc.healthcard.HealthCardResponse
import expo.modules.ehcnfc.healthcard.HealthCardType
import expo.modules.ehcnfc.healthcardcommand.ManageSE
import expo.modules.ehcnfc.healthcardcommand.PACE
import expo.modules.ehcnfc.helpers.Bytes
import expo.modules.ehcnfc.helpers.secureRandomInstance
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.keyagreement.KeyAgreementError
import expo.modules.ehcnfc.pace.CardUtilities.byteArrayToECPoint
import expo.modules.ehcnfc.pace.CardUtilities.extractPrimitiveByTag
import org.bouncycastle.crypto.params.*
import java.io.IOException

import java.math.BigInteger
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}



/**
 * Represents a single PACE negotiation session.
 *
 *
 * @param card The HealthCard instance to communicate with.
 * @param paceInfo PACE protocol parameters (protocolID, curve, base point, etc.).
 * @param can The Card Access Number (CAN) used for key derivation.
 * Holds all context needed for negotiation: card, PACE parameters, CAN.
 * Step functions are now methods of the session, so they can access shared state.
 */
internal class PaceSession(
    private val card: HealthCardType,
    private val paceInfo: PaceInfo,
    private val can: CAN
){

    companion object {
        private val randomGenerator: SecureRandom by lazy { secureRandomInstance() }
        private const val BYTE_LENGTH = 8
        private const val MAC_TOKEN_PREFIX_SIZE = 8

        // predefined by the card specification: tells the card which key slot to use for this PACE algorithm.
        private const val AFFECTED_KEY: UByte = 0x2u
    }

    /**
     * Executes all steps of the PACE negotiation.
     *
     * @return PaceKey agreed upon with the card.
     */
    suspend fun performNegotiationAsync(): PaceKey {
        // Step 0: Set security environment
        initializePaceSecurityEnvironment()

        // Step 1: Request nonceS from card
        val nonceS = paceStep1ExchangeNonce()

        Log.d("KeyAgreement", "negotiateSessionKeyAsync nonceS ${nonceS?.toHexString()}")
        // Step 2: Generate PK1_PCD and get PK1_PICC from card
        val firstKeyPair = paceStep2KeyExchange(nonceS)
        if (firstKeyPair == null) {
            Log.e("KeyAgreement", "negotiateSessionKeyAsync step2Res is null")
            throw Exception("KeyAgreement: negotiateSessionKeyAsync step2Res is null")
        }
        val keyPair2 = firstKeyPair.first
        val pk2Pcd = firstKeyPair.second

        Log.d("KeyAgreement", "negotiateSessionKeyAsync keyPair2 ${keyPair2.toHexString()}")
        Log.d("KeyAgreement", "negotiateSessionKeyAsync pk2Pcd ${pk2Pcd}")
        // Step 3: Send PK2_PCD, receive PK2_PICC, derive PaceKey
        val secondKeyPair = derivePaceSessionKeys(keyPair2, pk2Pcd)
        if (secondKeyPair == null) {
            Log.e("KeyAgreement", "negotiateSessionKeyAsync step2Res is null")
            throw Exception("KeyAgreement: negotiateSessionKeyAsync step2Res is null")
        }
        val piccMacKeyPair = secondKeyPair.first
        val piccMacDerived = piccMacKeyPair.first
        val pcdMac = piccMacKeyPair.second
        val paceKey = secondKeyPair.second

        Log.d(
            "KeyAgreement",
            "negotiateSessionKeyAsync piccMacDerived ${piccMacDerived.toHexString()}"
        )
        Log.d("KeyAgreement", "negotiateSessionKeyAsync pcdMac ${pcdMac.toHexString()}")
        Log.d(
            "KeyAgreement",
            "negotiateSessionKeyAsync paceKey.enc ${paceKey.enc.toHexString()}"
        )
        Log.d(
            "KeyAgreement",
            "negotiateSessionKeyAsync paceKey.mac ${paceKey.mac.toHexString()}"
        )

        // Step 4: Verify MAC_PICC
        val verifyMacPicc = performPaceMutualAuthentication(piccMacDerived, pcdMac)
        if (!verifyMacPicc) throw KeyAgreementError.MacPiccVerificationFailedLocally

        return paceKey
    }

    /**
     * Step 0 of PACE key agreement: sets the PACE security environment on the card.
     *
     * This sends a `SELECT PACE` APDU to the card, using the algorithm's Object Identifier,
     * and validates the card's response.
     *
     * @return The raw APDU response from the card including SW1SW2, or null on failure.
     * @throws [ApduException] if response parsing fails.
     * @throws [IOException] if transceive fails.
     * @throws [KeyAgreementError.IllegalArgument] if any argument is null.
     * @throws [KeyAgreementError.UnexpectedFormedAnswerFromCard] if card response is invalid.
     */
    @Throws(
        ApduException::class,
        IOException::class,
        KeyAgreementError.IllegalArgument::class,
        KeyAgreementError.UnexpectedFormedAnswerFromCard::class
    )
    private suspend fun initializePaceSecurityEnvironment(): HealthCardResponse? = withContext(Dispatchers.IO)  {
        try {
            val key = Key(AFFECTED_KEY)

            val apduCommand =
                ManageSE.selectPACE(key, dfSpecific = false, oidEncodedBytes = paceInfo.paceInfoProtocolBytes)
            Log.d("KeyAgreement", "APDU command for selectPACE : ${apduCommand.bytes.toHexString()}")

            val response = HealthCardResponse.from(card.transmitApdu(apduCommand))
            response.expect(ResponseStatus.Success)

            Log.d(
                "KeyAgreement",
                "initializePaceSecurityEnvironment paceStepResponse0 data without SW: ${response.data.toHexString()}"
            )
            response
        } catch (e: Exception) {
            Log.e("KeyAgreement", "initializePaceSecurityEnvironment failed", e)
            null
        }
    }


    /**
     * Step 1 of PACE key agreement: exchange nonce with the card.
     *
     * Sends the first PACE APDU command and retrieves the card's nonce (nonceZ).
     * Decrypts nonceZ using a derived key from the CAN to obtain nonceS.
     *
     * @return The decrypted nonceS from the card.
     * @throws [ApduException] if response parsing fails.
     * @throws [IOException] if transceive fails.
     * @throws [KeyAgreementError.Asn1ParseException] if asn1 parsing fails.
     * @throws [KeyAgreementError.DecryptionException] on decryption failure.
     * @throws [KeyAgreementError.IllegalArgument] if card or CAN is invalid.
     * @throws [KeyAgreementError.UnexpectedFormedAnswerFromCard] if ASN.1 parsing or decryption fails.
     */
    @Throws(
        ApduException::class,
        IOException::class,
        KeyAgreementError.Asn1ParseException::class,
        KeyAgreementError.IllegalArgument::class,
        KeyAgreementError.UnexpectedFormedAnswerFromCard::class
    )
    private suspend fun paceStep1ExchangeNonce(
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val step1Command = PACE.step1a()
            Log.d(
                "KeyAgreement",
                "paceStep1ExchangeNonce step1Command: ${step1Command.bytes.toHexString()}"
            )
            // Send APDU and validate response
            val step1Response = HealthCardResponse.from(card.transmitApdu(step1Command))
            step1Response.expect(ResponseStatus.Success)
            Log.d(
                "KeyAgreement",
                "paceStep1ExchangeNonce response data without SW: ${step1Response.data.toHexString()}"
            )

            // Extract nonceZ from ASN.1 structure
            val nonceZ = extractPrimitiveByTag(step1Response.data, TagClass.CONTEXT_SPECIFIC)
                ?: throw KeyAgreementError.UnexpectedFormedAnswerFromCard // in case it's null throw

            // Derive AES key from CAN
            val derivedKey = KeyDerivationFunction.deriveKey(can.rawValue, mode = Mode.PASSWORD)

            // Decrypt nonceZ to get nonceS
            val nonceS = aesCbc128DecryptNonce(derivedKey, nonceZ)
                ?: throw KeyAgreementError.UnexpectedFormedAnswerFromCard // in case it's null throw

            nonceS
        }catch (e: Exception) {
            Log.e("KeyAgreement", "paceStep1ExchangeNonce failed", e)
            throw KeyAgreementError.UnexpectedFormedAnswerFromCard
        }
    }

    /**
     * AES/CBC/NoPadding decryption of a nonce.
     *
     * @param keyData The 128-bit AES key.
     * @param nonceData The ciphertext to decrypt.
     * @return The decrypted nonce bytes.
     * @throws [KeyAgreementError.DecryptionException] on decryption failure.
     */
    @Throws(KeyAgreementError.DecryptionException::class)
    private fun aesCbc128DecryptNonce(keyData: ByteArray, nonceData: ByteArray): ByteArray? {
        try {
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val skeySpec = SecretKeySpec(keyData, "AES")
            val ivSpec = IvParameterSpec(ByteArray(16))
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec)
            return cipher.doFinal(nonceData) // use full nonce, don't trim
        } catch (e: Exception) {
            Log.e("KeyAgreement", "decryptNonce : ${e.message}");
            throw KeyAgreementError.DecryptionException("Decrypting nonceZ failed: ", e)
        }
    }


    /**
     * Step 2 of PACE key agreement: perform first Diffie-Hellman exchange and compute gTilde.
     *
     * - Generate first ephemeral key pair (PK1_PCD).
     * - Send PK1_PCD to the card and receive PK1_PICC.
     * - Compute shared secret and gTilde = G * nonceS + (PK1_PICC * pcdSkX1).
     * - Generate second ephemeral key pair (pcdSkX2, PK2_PCD).
     *
     * @param nonceS Decrypted nonce from Step 1.
     * @return A pair of (PK2_PCD encoded point, pcdSkX2 private scalar).
     * @throws [ApduException] if response parsing fails.
     * @throws [IOException] if transceive fails.
     * @throws [KeyAgreementError.IllegalArgument] if input parameters are null.
     * @throws [KeyAgreementError.UnexpectedFormedAnswerFromCard] if ASN.1 parsing fails.
     */
    @Throws(
        ApduException::class,
        IOException::class,
        KeyAgreementError.IllegalArgument::class,
        KeyAgreementError.UnexpectedFormedAnswerFromCard::class
    )
    private suspend fun paceStep2KeyExchange(
        nonceS: ByteArray?,
    ): Pair<ByteArray, BigInteger> = withContext(Dispatchers.IO) {
        requireNotNull(nonceS) {
            throw KeyAgreementError.IllegalArgument(
                "nonceS",
                "Cannot be null"
            )
        }
        requireNotNull(paceInfo) {
            throw KeyAgreementError.IllegalArgument(
                "paceInfo",
                "Cannot be null"
            )
        }

        try {
            val curveOrder = paceInfo.ecCurve.order
            val rnd = randomGenerator

            // --- First ephemeral private key (pcdSkX1) ---
            val pcdSkX1 = secureRandomScalar(curveOrder, rnd)
            val pcdPkSkX1 = paceInfo.ecPointG.multiply(pcdSkX1)
            val pcdPk1 = pcdPkSkX1.getEncoded(false)

            // --- Send PK1_PCD to card ---
            val step2Command = PACE.step2a(pcdPk1)
            Log.d(
                "KeyAgreement",
                "paceStep2KeyExchange APDU command: ${step2Command.bytes.toHexString()}"
            )

            val pk1PiccResponse = HealthCardResponse.from(card.transmitApdu(step2Command))
            pk1PiccResponse.expect(ResponseStatus.Success)
            Log.d(
                "KeyAgreement",
                "paceStep2KeyExchange pk1PiccResponse data without SW ${pk1PiccResponse.data.toHexString()}"
            )

            // Extract ASN.1 content without SW1SW2
            val pk1PiccBytes =
                extractPrimitiveByTag(pk1PiccResponse.data, TagClass.CONTEXT_SPECIFIC)
                    ?: throw KeyAgreementError.UnexpectedFormedAnswerFromCard

            val pk1PiccPoint = byteArrayToECPoint(pk1PiccBytes, paceInfo.ecCurve)

            // --- Compute shared secret and gTilde ---
            val sharedSecret = pk1PiccPoint.multiply(pcdSkX1)

            val nonceInt = BigInteger(1, nonceS).mod(curveOrder).takeIf { it.signum() > 0 }
                ?: BigInteger.ONE

            val gTilde = paceInfo.ecPointG.multiply(nonceInt).add(sharedSecret)


            // --- Second ephemeral private key (pcdSkX2) ---
            val pcdSkX2 = secureRandomScalar(curveOrder, rnd) // private
            val pcdPkSkX2 = gTilde.multiply(pcdSkX2)
            val pcdPkS2 = pcdPkSkX2.getEncoded(false) // public

            Pair(pcdPkS2, pcdSkX2) // return public key first, private key second
        } catch (e: KeyAgreementError) {
            throw e
        } catch (e: Exception) {
            Log.e("KeyAgreement", "paceStep2KeyExchange failed", e)
            throw KeyAgreementError.UnexpectedFormedAnswerFromCard
        }
    }

    /**
     * Generate a random scalar k ∈ (0, n).
     *
     * @param n The curve order.
     * @param rnd SecureRandom instance.
     * @return A random positive scalar < n.
     */
    private fun secureRandomScalar(n: BigInteger, rnd: SecureRandom): BigInteger {
        val nBitLen = n.bitLength()
        while (true) {
            val k = BigInteger(nBitLen, rnd)
            if (k.signum() > 0 && k < n) return k
        }
    }

    /**
     * Step 3 of PACE key agreement: perform second Diffie-Hellman exchange and derive paceKey.
     *
     * - Send the second ephemeral public key (PK2_PCD) to the card
     * - Receive the card's second public key (PK2_PICC)
     * - Derive the final shared secret K
     * - Derive PACE session keys (ENC and MAC)
     * - Compute MACs for mutual authentication
     *
     * @param pcdPkS2 the terminal's second ephemeral public key (uncompressed EC point)
     * @param pcdSkX2 the terminal's second ephemeral private scalar
     * @return a pair containing:
     *   - Pair(piccMacDerived, pcdMac) → MACs for mutual authentication
     *   - PaceKey(enc, mac) → derived session keys
     * @throws [ApduException] if response parsing fails.
     * @throws [IOException] if transceive fails.
     * @throws [KeyAgreementError.IllegalArgument] if provided parameters are wrong.
     * @throws [KeyAgreementError.UnexpectedFormedAnswerFromCard] if ASN.1 parsing fails.
     */
    @Throws(KeyAgreementError.UnexpectedFormedAnswerFromCard::class)
    private suspend fun derivePaceSessionKeys(
        pcdPkS2: ByteArray,
        pcdSkX2: BigInteger
    ): Pair<Pair<ByteArray, ByteArray>, PaceKey> = withContext(Dispatchers.IO) {

        requireNotNull(paceInfo) {
            throw KeyAgreementError.IllegalArgument(
                "paceInfo",
                "cannot not be null"
            )
        }

        try {
            // --- Send PK2_PCD to card ---
            val step3Command = PACE.step3a(pcdPkS2)
            Log.d(
                "KeyAgreement",
                "derivePaceSessionKeys paceStepCommand3a: ${step3Command.bytes.toHexString()}"
            )

            val pk2PiccResponse = HealthCardResponse.from(card.transmitApdu(step3Command))
            pk2PiccResponse.expect(ResponseStatus.Success)
            Log.d(
                "KeyAgreement",
                "derivePaceSessionKeys pk2PiccResponse data without SW ${pk2PiccResponse.data.toHexString()}"
            )

            // --- Extract ASN.1 content without SW1SW2 ---
            val pk2PiccData =
                extractPrimitiveByTag(pk2PiccResponse.data, TagClass.CONTEXT_SPECIFIC)
                    ?: throw KeyAgreementError.UnexpectedFormedAnswerFromCard

            // --- Convert to ECPoint ---
            val piccPk2ECPoint = byteArrayToECPoint(pk2PiccData, paceInfo.ecCurve)

            // --- Shared secret: K = PK2_PICC * SK2_PCD ---
            val sharedSecretK = piccPk2ECPoint.multiply(pcdSkX2)
            sharedSecretK.normalize()

            //  --- Extract X-coordinate of shared secret as key material ---
            // +7 to round up the number of bits to the next full byte.
            val fieldSizeBytes = (paceInfo.ecCurve.fieldSize + 7) / BYTE_LENGTH
            val xBytes = Bytes.bigIntToByteArray(sharedSecretK.normalize().xCoord.toBigInteger())

            val sharedSecretKBytes = ByteArray(fieldSizeBytes)
            System.arraycopy(
                xBytes,
                0,
                sharedSecretKBytes,
                fieldSizeBytes - xBytes.size, // right-align, zero-padded
                xBytes.size
            )

            // --- Derive ENC and MAC session keys ---
            val paceKey = PaceKey(
                KeyDerivationFunction.deriveKey(sharedSecretKBytes, mode = Mode.ENC),
                KeyDerivationFunction.deriveKey(sharedSecretKBytes, mode = Mode.MAC)
            )

            // --- Compute MACs for mutual authentication ---
            val pcdMac =
                KeyDerivationFunction.deriveMac(paceKey.mac, pk2PiccData, paceInfo.protocolID)
            val piccMacDerived =
                KeyDerivationFunction.deriveMac(paceKey.mac, pcdPkS2, paceInfo.protocolID)

            Pair(Pair(piccMacDerived, pcdMac), paceKey)
        } catch (e: KeyAgreementError) {
            throw e
        } catch (e: Exception) {
            Log.e("KeyAgreement", "derivePaceSessionKeys failed", e)
            throw KeyAgreementError.UnexpectedFormedAnswerFromCard
        }
    }

    /**
     * Step 4 of PACE key agreement: (Mutual Authentication):
     *
     * Sends the PCD's authentication token (MAC_PCD) to the card and verifies
     * the card's response (MAC_PICC). This step ensures that both parties
     * derived the same shared secret and confirms mutual authentication.
     *
     * @param piccMacDerived The expected MAC from the PICC, derived locally.
     * @param pcdMac The MAC value computed from the PCD side.
     * @return True if the MAC from the card matches the locally derived value, false otherwise.
     * @throws [ApduException] if response parsing fails.
     * @throws [IOException] if transceive fails.
     * @throws [KeyAgreementError.MacPcdVerificationFailedOnCard] if the MAC PCD verification failed.
     * @throws [KeyAgreementError.MacPiccVerificationFailedLocally] if the MAC PICC verification failed.
     */
    @Throws(KeyAgreementError.UnexpectedFormedAnswerFromCard::class)
    private suspend fun performPaceMutualAuthentication(
        piccMacDerived: ByteArray,
        pcdMac: ByteArray
    ): Boolean {
        try {
            // Only the first 8 bytes of the MAC are used as authentication token
            val pcdMacToken = pcdMac.take(MAC_TOKEN_PREFIX_SIZE).toByteArray()

            val piccMacResponse: HealthCardResponse
            try {
                // --- Send MAC_PCD token to card ---
                val step4Command = PACE.step4a(pcdMacToken)
                Log.d(
                    "KeyAgreement",
                    "performPaceMutualAuthentication step4Command: ${step4Command.bytes.toHexString()}"
                )
                piccMacResponse = HealthCardResponse.from(card.transmitApdu(step4Command))
                piccMacResponse.expect(ResponseStatus.Success)
            } catch (e: Exception) {
                Log.e("KeyAgreement",
                    "performPaceMutualAuthentication step4Command: ${e.message}"
                )
                throw KeyAgreementError.MacPcdVerificationFailedOnCard
            }

            Log.d(
                "KeyAgreement",
                "performPaceMutualAuthentication piccMacResponse data: ${piccMacResponse.data.toHexString()}"
            )

            // --- Extract ASN.1 content without SW1SW2 ---
            val piccMacData =
                extractPrimitiveByTag(piccMacResponse.data, TagClass.CONTEXT_SPECIFIC)
                    ?: throw KeyAgreementError.UnexpectedFormedAnswerFromCard // throw if null

            Log.d(
                "KeyAgreement",
                "performPaceMutualAuthentication MAC_PICC extracted bytes: ${piccMacData.toHexString()}"
            )

            val isEqual = piccMacData.contentEquals(piccMacDerived)
            Log.d(
                "KeyAgreement",
                "performPaceMutualAuthentication MAC verification result: $isEqual"
            )

            return when (isEqual) {
                true -> true
                else -> throw KeyAgreementError.MacPiccVerificationFailedLocally
            }

        } catch (e: KeyAgreementError) {
            throw e
        } catch (e: Exception) {
            Log.e("KeyAgreement", "performPaceMutualAuthentication failed", e)
            throw KeyAgreementError.UnexpectedFormedAnswerFromCard
        }
    }

}