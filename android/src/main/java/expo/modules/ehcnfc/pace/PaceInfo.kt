package expo.modules.ehcnfc.pace

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.Asn1Integer
import at.asitplus.signum.indispensable.asn1.Asn1Sequence
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.encoding.parse
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import expo.modules.ehcnfc.helpers.toHexString
import org.bouncycastle.jce.ECNamedCurveTable
import java.math.BigInteger

private const val PARAMETER256 = 13
private const val PARAMETER384 = 16
private const val PARAMETER512 = 17

/**
 * Extracts PACE Information from CardAccess
 */
class PaceInfo(cardAccess: ByteArray) {
    private val protocol: ObjectIdentifier
    private val parameterID: Int

    /**
     * Returns PACE info protocol bytes as ASN1Primitive from ASN1 serializing
     */
    val paceInfoProtocolBytes: ByteArray

    init {
        // Parse the byte array into an ASN.1 element
        val asn1Set = Asn1Element.parse(cardAccess).asSet()
        Log.d("PaceInfo", "Parsed ASN.1 Set:\n${asn1Set.prettyPrint()}")

        // Extract first sequence inside the set
        val seq = asn1Set.children.firstOrNull() as? Asn1Sequence
            ?: throw IllegalArgumentException("Expected Sequence inside ASN.1 Set")

        // Extract protocol OID
        val asn1Oid = seq.children.firstOrNull() as? Asn1Primitive
            ?: throw IllegalArgumentException("Expected Primitive (OID) inside ASN.1 Sequence")

        protocol = ObjectIdentifier.doDecode(asn1Oid)
            ?: throw IllegalArgumentException("Failed to decode protocol OID")

        // Extract parameter ID (3rd element in the sequence)
        val paramPrimitive = seq.children.getOrNull(2) as? Asn1Primitive
            ?: throw IllegalArgumentException("Expected Asn1Primitive at index 2")

        // Convert the raw content bytes to a BigInteger
        parameterID = BigInteger(1, paramPrimitive.content).toInt()


        // Extract protocol bytes from OID
        paceInfoProtocolBytes = protocol.encodeToTlv().content
    }

    /**
     * PACE info protocol ID
     */
    val protocolID: String = protocol.toString()
    val protocolIdentifierHex: String = protocol.bytes.toHexString()

    private val ecNamedCurveParameterSpec = ECNamedCurveTable.getParameterSpec(
        when (parameterID) {
            PARAMETER256 -> "BrainpoolP256r1"
            PARAMETER384 -> "BrainpoolP384r1"
            PARAMETER512 -> "BrainpoolP512r1"
            else -> ""
        }
    )

    val ecCurve = ecNamedCurveParameterSpec.curve
    val ecPointG = ecNamedCurveParameterSpec.g

    fun convertECPoint(ecPoint: ByteArray) =
        CardUtilities.byteArrayToECPoint(ecPoint, ecCurve)
}
