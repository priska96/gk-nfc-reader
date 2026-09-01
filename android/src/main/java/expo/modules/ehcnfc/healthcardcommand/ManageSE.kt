package expo.modules.ehcnfc.healthcardcommand

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.TagClass
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.cardaccess.Key
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.keyagreement.KeyAgreementError

/**
 * Builds APDU commands for the Manage Security Environment (MSE) instructions.
 * Used for selecting PACE keys and other key management operations.
 */
object ManageSE {

    private const val CLA: Byte = 0x00
    private const val INS: Byte = 0x22

    /**
     * Builds a generic MSE APDU command with the given parameters.
     */
    private fun buildCommand(p1: Byte, p2: Byte, data: ByteArray?=null): ApduCommand = try {
        val apdu = ApduCommand.ofOptions(CLA, INS, p1, p2, data,null)
        Log.d("ManageSE", "Generated APDU: ${apdu.toString()}")
        Log.d("ManageSE", "Generated APDU HEX: ${apdu.bytes.toHexString()}")
        apdu
    } catch (e: Exception) {
        throw KeyAgreementError.PACEApduCommandException("Failed to build MSE APDU command", e)
    }

    /**
     * Select a symmetric key for PACE authentication without specifying the curve.
     * gemSpec_COS#14.9.9.7
     *
     * @param symmetricKey The key identifier to select.
     * @param dfSpecific Whether the key is DF-specific or global.
     * @param oidEncodedBytes The PACE Object Identifier asn1 encoded (N102.440).
     * @throws [KeyAgreementError.IllegalArgument] if any argument is null.
     * @return The serialized APDU command to select the PACE key.
     */
    @Throws(KeyAgreementError.IllegalArgument::class)
    fun selectPACE(symmetricKey: Key?, dfSpecific: Boolean, oidEncodedBytes: ByteArray?): ApduCommand {
        requireNotNull(symmetricKey) { throw KeyAgreementError.IllegalArgument("symmetricKey", "cannot not be null") }
        requireNotNull(oidEncodedBytes) { throw KeyAgreementError.IllegalArgument("oid", "cannot not be null") }

        // Build ASN.1 OID wrapper context-specific tag 0
        val oidTag = Tag(0u, constructed = false, tagClass = TagClass.CONTEXT_SPECIFIC)
        val asn1Oid = Asn1Primitive(tag = oidTag, content = oidEncodedBytes)
        val serializedOid = asn1Oid.derEncoded
        Log.d("ManageSE", "Serialized OID: ${serializedOid.toHexString()}")

        // Build ASN.1 key wrapper context-specific tag 0
        val keyTag = Tag(3u, constructed = false, tagClass = TagClass.CONTEXT_SPECIFIC)
        val keyBytes = symmetricKey.keyReferenceBytes(dfSpecific)
        val asn1Key = Asn1Primitive(tag = keyTag, content = keyBytes)
        val serializedKey = asn1Key.derEncoded
        Log.d("ManageSE", "Serialized Key: ${serializedKey.toHexString()}")

        // Build APDU
        val p1: Byte = 0xC1.toByte()
        val p2: Byte = 0xA4.toByte()
        val combinedData = serializedOid + serializedKey
        return buildCommand(p1, p2, combinedData)
    }
}

