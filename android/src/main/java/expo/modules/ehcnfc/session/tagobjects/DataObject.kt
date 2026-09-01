package expo.modules.ehcnfc.session.tagobjects

import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag.Template.Companion.withClass
import at.asitplus.signum.indispensable.asn1.encoding.*

private const val DO_87_TAG = 0x07
private const val DO_81_EXTRACTED_TAG = 0x81
private const val DO_81_TAG = 0x01

/**
 * Data object with TAG 87
 *
 * @param data byte array with extracted data from plain CommandApdu or encrypted ResponseApdu
 * @param tag int with extracted tag number
 */
class DataObject(val data: ByteArray, val tag: ULong = 0u) {
    val taggedObject: Asn1Element
        get() =
            if (tag == DO_81_EXTRACTED_TAG.toULong()) {
                (Asn1.OctetString(data) withImplicitTag (DO_81_TAG.toULong() withClass TagClass.CONTEXT_SPECIFIC))
            } else {
                (Asn1.OctetString(data) withImplicitTag (DO_87_TAG.toULong() withClass TagClass.CONTEXT_SPECIFIC))
            }
}
