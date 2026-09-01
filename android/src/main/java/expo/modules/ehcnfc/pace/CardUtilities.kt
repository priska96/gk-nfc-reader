package expo.modules.ehcnfc.pace

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.encoding.parseFirst
import expo.modules.ehcnfc.keyagreement.KeyAgreementError
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger

/**
 * Utility class for card functions
 */
object CardUtilities {
    private const val UNCOMPRESSED_POINT_MARKER = 0x04

    /**
     * Decodes an uncompressed ECPoint from byte array (0x04 || x || y).
     *
     * @param byteArray The encoded EC point.
     * @param curve The curve on which the point lies.
     * @return ECPoint decoded from input.
     * @throws [IllegalArgumentException] if encoding is invalid.
     */
    @Throws(IllegalArgumentException::class)
    fun byteArrayToECPoint(byteArray: ByteArray, curve: ECCurve): ECPoint {
        if (byteArray[0] != UNCOMPRESSED_POINT_MARKER.toByte()) {
            throw IllegalArgumentException("Expected uncompressed point (0x04) but got ${byteArray[0]}")
        }

        val coordLength = (byteArray.size - 1) / 2
        val x = byteArray.copyOfRange(1, 1 + coordLength)
        val y = byteArray.copyOfRange(1 + coordLength, byteArray.size)

        return curve.createPoint(BigInteger(1, x), BigInteger(1, y))
    }

    /**
     * Extracts the content of a primitive child with a specific tag from an ASN.1 structure.
     *
     * @param constructedAsn1 ASN.1 encoded data
     * @param targetTagClass the tag class to match (default CONTEXT_SPECIFIC)
     * @param targetTagValue the tag value to match
     * @throws [KeyAgreementError.Asn1ParseException] if asn1 parsing fails.
     * @return the primitive content of the matching child
     */
    @Throws(KeyAgreementError.Asn1ParseException::class)
    fun extractPrimitiveByTag(
        constructedAsn1: ByteArray,
        targetTagClass: TagClass? = null // optional: if null, ignore tag class
    ): ByteArray? {
        val asn1Pair: Pair<Asn1Element, ByteArray> = try {
            Asn1Element.parseFirst(constructedAsn1)
        } catch (e: Exception) {
            throw KeyAgreementError.Asn1ParseException("Parsing from responseData to ASN1 failed:", e)
        }
        val asn1Doc = asn1Pair.first

        val asn1Structure = asn1Doc.asStructure()
        Log.d("KeyAgreement", "ASN.1 structure: ${asn1Structure.prettyPrint()}")

        val matchingChild = asn1Structure.children.firstOrNull { child ->
            targetTagClass == null || child.tag.tagClass == targetTagClass
        } ?: throw KeyAgreementError.Asn1ParseException("Finding ASN1 that matches tagClass failed", null)

        return when (matchingChild) {
            is Asn1Primitive -> matchingChild.content
            else -> (matchingChild.asStructure().children.firstOrNull() as? Asn1Primitive)?.content
                ?: throw KeyAgreementError.Asn1ParseException("Getting content from primitive failed", null)
        }
    }

}