package expo.modules.ehcnfc.healthcardcommand

import android.util.Log
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduConstants
import expo.modules.ehcnfc.helpers.prettyPrint
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.keyagreement.KeyAgreementError
import org.bouncycastle.asn1.BERTags
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.DERTaggedObject


data class Asn1Spec(
    val data: ByteArray,
    val tagNo: Int,
    val tagClass: Int,
    val explicit: Boolean
)


/**
 * PACE (Password Authenticated Connection Establishment) APDU command builder.
 *
 * Supports steps 1a-4a as per gemSpec_COS #14.7.2.x.
 */
object PACEV2 {
    private const val CLA: Byte = 0x10.toByte()
    private const val INS: Byte = 0x86.toByte()
    private const val P1: Byte = 0x0.toByte()
    private const val P2: Byte = 0x0.toByte()

    private fun buildPaceCommand(
        cla: Byte? = null,
        data: ByteArray? = null,
        ne: Int = 0
    ): ByteArray {
        return try {
            val actualCla = cla ?: CLA
            val payload = data ?: byteArrayOf()
            val expectedLength = if (ne == 0) ApduConstants.EXPECTED_LENGTH_WILDCARD_EXTENDED else ne

            // 'ApduCommand.of' will automatically determine extended length
            val apdu = ApduCommand.ofOptions(actualCla, INS, P1, P2, payload, expectedLength)
            Log.d("PACE", "Generated APDU: ${apdu}")
            Log.d("PACE", "Generated APDU HEX: ${apdu.bytes.toHexString()}")
            apdu.bytes  // no need for forceExtended anymore
        } catch (e: Exception) {
            throw KeyAgreementError.PACEApduCommandException(
                "Failed to build PACE APDU command", e
            )
        }
    }

    /**
     * Step 1a - Start General Authenticate.
     */
    fun step1a(): ByteArray {
        val data = byteArrayOf(0x7C.toByte(), 0x00)
        return buildPaceCommand(data = data, ne = 0)
    }
    /**
     * Step 2a - Send ephemeral public key from PCD (PK1).
     *
     * @param publicKey PCD PK1 bytes.
     */
    fun step2a(publicKey: ByteArray): ByteArray {
        val data = derEncode(
            listOf(
                Asn1Spec(publicKey, 1, BERTags.CONTEXT_SPECIFIC, false) // [1] IMPLICIT OCTET STRING
            )
        )
        return buildPaceCommand(data = data, ne = 0)
    }

    /**
     * Step 3a - Send ephemeral public key from PCD (PK2).
     *
     * @param publicKey PCD PK2 bytes.
     */
    fun step3a(publicKey: ByteArray): ByteArray {
        val data = derEncode(
            listOf(
                Asn1Spec(publicKey, 3, BERTags.CONTEXT_SPECIFIC, false) // [3] IMPLICIT OCTET STRING
            )
        )
        return buildPaceCommand(data = data, ne = 0)
    }

    /**
     *  Step 4a - Verify / exchange token.
     *
     * @param token 8-byte exchange token.
     */
    fun step4a(token: ByteArray): ByteArray {
        require(token.size == 8) { "PACE step4a token must be 8 bytes" }
        val data = derEncode(
            listOf(
                Asn1Spec(token, 5, BERTags.CONTEXT_SPECIFIC, false) // [5] IMPLICIT OCTET STRING
            )
        )
        return buildPaceCommand(data = data, ne = 0)
    }

    /**
     * Build a DER-encoded ASN.1 structure from (data, tagNo, tagClass).
     *
     * @param objects List of ASN.1 elements defined by (data, tagNo, tagClass, explicit).
     * @param outerTagNo Tag number for the outer ApplicationSpecific wrapper (default = 0x1C).
     *  @throws KeyAgreementError.CreateASN1ObjectException If encoding fails for any reason.
     */
    fun derEncode(
        objects: List<Asn1Spec>,
        outerTagNo: Int = 0x1C
    ): ByteArray {
        val children = objects.map { (data, tagNo, tagClass, explicit) ->
            when (tagClass) {
                BERTags.UNIVERSAL -> DEROctetString(data) // rarely used directly
                BERTags.APPLICATION -> DERTaggedObject(explicit, tagNo, DEROctetString(data))
                BERTags.CONTEXT_SPECIFIC, BERTags.PRIVATE ->
                    DERTaggedObject(explicit, tagNo, DEROctetString(data))
                else -> throw IllegalArgumentException("Unsupported tag class $tagClass")
            }
        }

        // Wrap children in a sequence inside an [APPLICATION outerTagNo]
        val sequence = DERSequence(children.toTypedArray())
        val appSpecific = DERTaggedObject(false, outerTagNo, sequence)

        Log.d("PACE","derEncoded appSpecific: ${prettyPrint(appSpecific)}")
        Log.d("PACE","derEncoded appSpecific: ${appSpecific.encoded.toHexString()}")
        return appSpecific.encoded
    }


}