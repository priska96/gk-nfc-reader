package expo.modules.ehcnfc.healthcardcommand

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.Asn1CustomStructure
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.encoding.*
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduConstants
import expo.modules.ehcnfc.helpers.prettyPrint
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.keyagreement.KeyAgreementError

/**
 * PACE (Password Authenticated Connection Establishment) APDU command builder.
 *
 * Supports steps 1a-4a as per gemSpec_COS #14.7.2.x.
 */
object PACE {
    private const val CLA: Byte = 0x10.toByte()
    private const val INS: Byte = 0x86.toByte()
    private const val P1: Byte = 0x0.toByte()
    private const val P2: Byte = 0x0.toByte()

    private fun buildPaceCommand(
        cla: Byte? = null,
        data: ByteArray? = null,
        ne: Int = 0
    ): ApduCommand {
        return try {
            val actualCla = cla ?: CLA
            val payload = data ?: byteArrayOf()
            val expectedLength = if (ne == 0) ApduConstants.EXPECTED_LENGTH_WILDCARD_EXTENDED else ne
            val apdu = ApduCommand.ofOptions(actualCla, INS, P1, P2, payload, expectedLength)
            Log.d("PACE", "Generated APDU: ${apdu}")
            Log.d("PACE", "Generated APDU HEX: ${apdu.bytes.toHexString()}")
            apdu
        } catch (e: Exception) {
            throw KeyAgreementError.PACEApduCommandException(
                "Failed to build PACE APDU command", e
            )
        }
    }

    /**
     * Step 1a - Start General Authenticate.
     */
    fun step1a(): ApduCommand {
        val data = byteArrayOf(0x7C.toByte(), 0x00)
        return buildPaceCommand(data = data, ne = 0)
    }
    /**
     * Step 2a - Send ephemeral public key from PCD (PK1).
     *
     * @param publicKey PCD PK1 bytes.
     */
    fun step2a(publicKey: ByteArray): ApduCommand {
        val tag = Tag(1u, constructed = false, tagClass = TagClass.CONTEXT_SPECIFIC)
        val data = derEncode(listOf(publicKey to tag))
        return buildPaceCommand(data = data, ne = 0)
    }

    /**
     * Step 3a - Send ephemeral public key from PCD (PK2).
     *
     * @param publicKey PCD PK2 bytes.
     */
    fun step3a(publicKey: ByteArray): ApduCommand {
        val tag = Tag(3u, constructed = false, tagClass = TagClass.CONTEXT_SPECIFIC)
        val data = derEncode(listOf(publicKey to tag))
        return buildPaceCommand(data = data, ne = 0)
    }

    /**
    *  Step 4a - Verify / exchange token.
    *
    * @param token 8-byte exchange token.
    */
    fun step4a(token: ByteArray): ApduCommand {
        require(token.size == 8) { "PACE step4a token must be 8 bytes" }

        val tag = Tag(5u, constructed = false, tagClass = TagClass.CONTEXT_SPECIFIC)
        val data = derEncode(listOf(token to tag))
        return buildPaceCommand(cla= 0x00.toByte(), data = data, ne = 0)
    }

    /**
     * Builds and DER-encodes a custom ASN.1 structure from the given list of
     * tagged byte arrays.
     *
     * Each (ByteArray, Tag) pair is wrapped in an [Asn1Primitive] and added as a
     * child element to an [Asn1CustomStructure]. The structure is encoded using
     * DER with application tag `0x1C`.
     *
     * @param objects A list of byte arrays with their associated [Tag]s to be encoded.
     * @return The DER-encoded byte array representing the ASN.1 structure.
     *  @throws KeyAgreementError.CreateASN1ObjectException If encoding fails for any reason.
     */
    private fun derEncode(objects: List<Pair<ByteArray, Tag>>): ByteArray {
        val children: List<Asn1Primitive> = objects.map { (data, tag) ->
            Asn1Primitive(tag, data)
        }
        val asn1CustomStructure = Asn1CustomStructure(
            children = children,
            tag = 0x1C.toULong(),
            tagClass = TagClass.APPLICATION,
            sortChildren = true,
            shouldBeSorted = true
        )
        Log.d("PACE", "derEncoded asn1CustomStructure: ${asn1CustomStructure.prettyPrint()}")
        Log.d(
            "PACE",
            "derEncoded asn1CustomStructure: ${asn1CustomStructure.derEncoded.toHexString()}"
        )
        return try {
            asn1CustomStructure.derEncoded
        } catch (e: Exception) {
            throw KeyAgreementError.CreateASN1ObjectException(
                "Could not DER-encode ASN1 structure: ${asn1CustomStructure.prettyPrint()}",
                e
            )
        }
    }
}