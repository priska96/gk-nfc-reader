package expo.modules.ehcnfc.healthcard

import android.util.Log
import java.lang.Exception
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.encoding.parse
import expo.modules.ehcnfc.cardaccess.Asn1ParseException

/**
 * Represent the CardVersion2 information of HealthCard
 * gemSpec_Karten_Fach_TIP_G2_1_3_0_0 #2.3 EF.Version2
 */
data class CardVersion2(
    // Information of C0 with version of filling instruction for version2
    val fillingInstructionsVersion: ByteArray,
    // Information of C1 with version of card object system
    val objectSystemVersion: ByteArray,
    // Information of C2 with version of product identification object system
    val productIdentificationObjectSystemVersion: ByteArray,
    // Information of C4 with version of filling instruction for EF.GDO
    val fillingInstructionsEfGdoVersion: ByteArray,
    // Information of C5 with version of filling instruction for EF.ATR
    val fillingInstructionsEfAtrVersion: ByteArray,
    // Information of C6 with version of filling instruction for EF.KeyInfo
    // - Note: Only filled for gSMC-K and gSMC-KT
    val fillingInstructionsEfKeyInfoVersion: ByteArray?,
    // Information of C3 with version of filling instruction for Environment Settings
    // - Note: Only filled for gSMC-K
    val fillingInstructionsEfEnvironmentSettingsVersion: ByteArray?,
    // Information of C7 with version of filling instruction for EF.GDO
    // - Note: Only filled for egk
    val fillingInstructionsEfLoggingVersion: ByteArray?
) {

    class ParseException(message: String) : Exception(message)

    companion object {
        /**
         * Parse a CardVersion2 model from ASN.1 encoded data
         * - Parameter data: ASN.1 encoded CardVersions
         * - Throws: CardVersion2.Error
         */
        fun fromAsn1(data: ByteArray): CardVersion2? {
            val asn1Doc = try {
                Asn1Element.parse(data) // should return base ASN1Element
            } catch (e: Exception) {
                throw Asn1ParseException(
                    "ASN1Decoder did returned unexpectedFormedAnswerFromCard",
                    e
                )
            }

            val asn1Children = asn1Doc.asStructure().children
                    ?: throw ParseException("Unexpected ASN.1 format [root is not constructed]")

            Log.d("CardVersion2", "asn1Doc.prettyPrint(), ${asn1Doc.prettyPrint()}")

            Log.d(
                "CardVersion2",
                "asn1Doc.tag, ${asn1Doc.tag}, ${asn1Doc.tag.tagClass}"
            )

            //map tagValue -> asn1Element (each element should be a primitive)
            val taggedObjectsMap: Map<ULong, Asn1Element>  = asn1Children.mapNotNull { obj: Asn1Element ->
                obj.tag.tagValue?.let { tagValue: ULong -> tagValue to obj }
            }.toMap()

            for ((tag, asn1Element) in taggedObjectsMap) {
                Log.d(
                    "CardVersion2",
                    "taggedObjectsMap, ${tag}, ${asn1Element.prettyPrint()}"
                )
            }


            fun get(tag: ULong): ByteArray {
                val primitive = taggedObjectsMap[tag]?.asPrimitive()?.content
                return primitive
                    ?: throw ParseException("Unexpected ASN.1 format [object with tag: $tag not found or not primitive]")
            }

            return CardVersion2(
                fillingInstructionsVersion = get(0uL),
                objectSystemVersion = get(1uL),
                productIdentificationObjectSystemVersion = get(2uL),
                fillingInstructionsEfGdoVersion = get(4uL),
                fillingInstructionsEfAtrVersion = get(5uL),
                fillingInstructionsEfKeyInfoVersion = try {
                    get(6uL)
                } catch (e: Exception) {
                    null
                },
                fillingInstructionsEfEnvironmentSettingsVersion =  try {
                    get(3uL)
                } catch (e: Exception) {
                    null
                },
                fillingInstructionsEfLoggingVersion = try {
                    get(7uL)
                } catch (e: Exception) {
                    null
                },
            )
        }
    }

    fun generation(): CardGeneration? {
        return CardGeneration.parseCardGeneration(objectSystemVersion)
    }
}
