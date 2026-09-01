package expo.modules.ehcnfc.cardaccess

import android.util.Log
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.cardobjects.FileIdentifier
import expo.modules.ehcnfc.cardobjects.ShortFileIdentifier
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.helpers.prettyPrint
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.encoding.parse
import at.asitplus.signum.indispensable.asn1.encoding.decodeToUInt
import at.asitplus.signum.indispensable.asn1.encoding.decodeFromAsn1ContentBytes


data class FileControlParameter (
    val status: LifeCycleState,
    val size: UInt,
    val readSize: UInt?,
    val fileDescriptor: String?,
    val fileIdentifier: FileIdentifier?,
    val applicationIdentifier: ApplicationIdentifier?,
    val shortFileIdentifier: ShortFileIdentifier?
) {

    /**
     * Describes the LifeCycle state of an EF/DF
     * according to the ISO/IEC 7816-4:2005(E) #5.3.3.2 Life cycle status byte.
     *
     *        b8|b7|b6|b5|b4|b3|b2|b1|meaning
     *        --|--|--|--|--|--|--|--|-------
     *        0|0|0|0|0|0|0|0|No information given
     *        0|0|0|0|0|0|0|1|Created
     *        0|0|0|0|0|0|1|1|Initialized
     *        0|0|0|0|0|1|-|1|Active
     *        0|0|0|0|0|1|-|0|Deactivated
     *        0|0|0|0|1|1|-|-|Terminated
     *        |||not all zero||x|x|x|x|Proprietary
     */
    enum class LifeCycleState {
        UNKNOWN, // No information given (when all bits are 0)
        CREATION, // No information given (when all bits are 0)
        INITIALISATION, // File is initialized (when bit 2 is set and 3 and 4 not
        ACTIVATED, // File is active [normal] (when bit 3 and 1 are set)
        DEACTIVATED, // File is not-active [hidden] (when bit 3 is set and bit 1 not)
        TERMINATED, // File is permanently `.deactivated` (when bit 4 and 3 are set)
        PROPRIETARY; // File is proprietary - values from 0x10 - 0xff
        companion object {
            /**
             * Parse a LifeCycleStatus Byte
             * @param byte the LCS byte
             * @return the LifeCycleState or null when invalid
             */
            fun parseLifeCycle(byte: UByte): LifeCycleState? {
                val b = byte.toInt()
                return when {
                    (b and 0b1111_0000) > 0 -> PROPRIETARY
                    (b and 0b1100) == 0b1100 -> TERMINATED
                    (b and 0b101) == 0b0101 -> ACTIVATED
                    (b and 0b101) == 0b100 -> DEACTIVATED
                    (b and 0b11) == 0b11 -> INITIALISATION
                    (b and 0b1) == 0b1 -> CREATION
                    b == 0x0 -> UNKNOWN
                    else -> null
                }
            }
        }
    }
    companion object {
        const val TAG: UInt = 0x02u

        /**
         * Parse FCP (File Control Parameter) from ASN.1 formatted data
         * @param data ASN.1 encoded bytes
         * @return FileControlParameter
         * @throws IllegalArgumentException if data is empty
         * @throws Asn1ParseException if parsing fails
         */
        @Throws(IllegalArgumentException::class, Asn1ParseException::class, InvalidCardException::class)
        fun parse(data: ByteArray): FileControlParameter? {
            try {
                if (data.isEmpty()) {
                    throw IllegalArgumentException("Non-empty value expected to create FileControlParameter")
                }

                val asn1Doc = try {
                    Asn1Element.parse(data) // should return base ASN1Element
                } catch (e: Exception) {
                    throw Asn1ParseException(
                        "ASN1Decoder did not return ApplicationSpecific ASN1Object",
                        e
                    )
                }

                Log.d("FileControlParameter", "asn1Doc.prettyPrint(), ${asn1Doc.prettyPrint()}")

                Log.d(
                    "FileControlParameter",
                    "asn1Doc.tag, ${asn1Doc.tag}, ${asn1Doc.tag.tagClass}"
                )
                if (asn1Doc.tag.tagClass != TagClass.APPLICATION) {
                    throw Asn1ParseException(
                        "ASN1Decoder did not return ApplicationSpecific ASN1Object",
                        null
                    )
                }


                Log.d("FileControlParameter", "asn1Doc.tag.tagValue, ${asn1Doc.tag.tagValue}")
                if (asn1Doc.tag.tagValue != TAG.toULong()) {
                    throw InvalidCardException(
                        "Unexpected tag [0x${asn1Doc.tag.hashCode()}] found, " +
                                "while converting response data to FCP. Expected: [0x${
                                    TAG.toString(
                                        16
                                    )
                                }]"
                    )
                }

                if (asn1Doc.asStructure().hasMoreChildren() == false) {
                    throw Asn1ParseException("No elements found in FCP Tag", null)
                }

                return extractFrom(asn1Doc)
            }catch (e: Exception){
                Log.e("FileControlParameter", e.toString())
            }
            return null
        }

        @Throws(IllegalArgumentException::class, Asn1ParseException::class, InvalidCardException::class)
        fun extractFrom(asn1: Asn1Element) :FileControlParameter {
            var numberOfOctets: UInt = 0u
            var fileDescriptor: String? = null
            var fileIdentifier: FileIdentifier? = null
            var applicationIdentifier: ApplicationIdentifier? = null
            var lifeCycleStatus: LifeCycleState? = null
            var shortFileIdentifier: ShortFileIdentifier? = null
            var readSize: UInt? = null

            val asn1Structure = asn1.asStructure()
            while (asn1Structure.hasMoreChildren()) {
                val child = asn1Structure.nextChild()
                // Skip constructed TLVs (like A1) — same as `guard !object.constructed` in Swift
                if (child.tag.isConstructed) {
                    Log.d("FileControlParameter", "Skipping constructed tag ${child.tag}")
                    continue
                }
                try { // <-- Wrap parsing that can fail

                    val primitive = child.asPrimitive()
                    when (child.tag.tagValue){
                       Tag.NUMBER_OF_OCTETS.value.toULong()->{
                            numberOfOctets =  UInt.decodeFromAsn1ContentBytes(primitive.content) ?: 0u
                        }
                        Tag.FILE_DESCRIPTOR.value.toULong() ->{
                            primitive?.let { fileDescriptor = it.content.toHexString() }
                        }
                        Tag.FILE_IDENTIFIER.value.toULong() -> {
                            primitive?.let { fileIdentifier = FileIdentifier.fromBytes(it.content) }
                        }
                        Tag.APPLICATION_IDENTIFIER.value.toULong() -> {
                            primitive?.let { applicationIdentifier = ApplicationIdentifier.fromBytes(it.content) }
                        }
                        Tag.SHORT_FILE_IDENTIFIER.value.toULong() -> {
                            primitive?.let { shortFileIdentifier = ShortFileIdentifier.fromAsn1(it.content) }
                        }
                        Tag.LIFE_CYCLE_STATUS.value.toULong() -> {
                            primitive?.content?.firstOrNull()?.toUByte()?.let { byte:UByte -> lifeCycleStatus = LifeCycleState.parseLifeCycle(byte) }
                        }
                        Tag.END_OF_FILE_POSITION.value.toULong() -> {
                            readSize = UInt.decodeFromAsn1ContentBytes(primitive.content) ?: 0u
                        }
                    }
                } catch (e: Exception) {
                    throw InvalidCardException("FCP could not be created due to invalid value: ${e.message}")
                }
            }
            val lcs = lifeCycleStatus ?:
                throw InvalidCardException("FCP could not be created because of missing parameter(s)")

            return FileControlParameter(
                lcs,
                numberOfOctets,
                readSize,
                fileDescriptor,
                fileIdentifier,
                applicationIdentifier,
                shortFileIdentifier,
            )
        }


        fun toString(fcp: FileControlParameter): String {
            return """
        status = ${fcp.status}
        size = ${fcp.size}
        readSize = ${fcp.readSize}
        fileDescriptor = ${fcp.fileDescriptor}
        fileIdentifier = ${fcp.fileIdentifier}
        applicationIdentifier = ${fcp.applicationIdentifier}
        shortFileIdentifier = ${fcp.shortFileIdentifier}
    """.trimIndent()
        }
    }

    enum class Tag(val value: UInt) {
        NUMBER_OF_OCTETS(0x00u),
        FILE_DESCRIPTOR(0x02u),
        FILE_IDENTIFIER(0x03u),
        APPLICATION_IDENTIFIER(0x04u),
        END_OF_FILE_POSITION(0x05u),
        SHORT_FILE_IDENTIFIER(0x08u),
        LIFE_CYCLE_STATUS(0x0Au);
    }
}


/**
 * Custom exceptions for clarity
 */
class Asn1ParseException(message: String, cause: Throwable?) : Exception(message, cause)
class InvalidCardException(message: String) : Exception(message)