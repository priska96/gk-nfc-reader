package expo.modules.ehcnfc.session.tagobjects

import expo.modules.ehcnfc.apdu.ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag.Template.Companion.withClass
import at.asitplus.signum.indispensable.asn1.encoding.*

private const val DO_97_TAG = 0x17
private const val BYTE_MASK = 0xFF
private const val BYTE_VALUE = 8

/**
 * Length object with TAG 97
 *
 * @param le extracted expected length from plain CommandApdu
 */
class LengthObject(le: Int) {
    private var leData = ByteArray(0)
    val taggedObject: Asn1Element
        get() = (Asn1.OctetString(leData) withImplicitTag(DO_97_TAG.toULong() withClass TagClass.CONTEXT_SPECIFIC))

    init {
        if (le >= 0) {
            leData = when {
                le == EXPECTED_LENGTH_WILDCARD_SHORT -> {
                    byteArrayOf(0x00)
                }
                le > EXPECTED_LENGTH_WILDCARD_SHORT -> {
                    byteArrayOf(
                        (le shr BYTE_VALUE and BYTE_MASK).toByte(),
                        (le and BYTE_MASK).toByte()
                    )
                }
                else -> {
                    byteArrayOf(le.toByte())
                }
            }
        }
    }
}
