package expo.modules.ehcnfc.session.tagobjects

import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag.Template.Companion.withClass
import at.asitplus.signum.indispensable.asn1.encoding.*

private const val DO_99_TAG = 0x19

/**
 * Status object with TAG 99
 *
 * @param statusBytes byte array with extracted response status from encrypted ResponseApdu
 */
class StatusObject(private val statusBytes: ByteArray) {
    val taggedObject: Asn1Element = (Asn1.OctetString(statusBytes) withImplicitTag (DO_99_TAG.toULong() withClass TagClass.CONTEXT_SPECIFIC))

}
