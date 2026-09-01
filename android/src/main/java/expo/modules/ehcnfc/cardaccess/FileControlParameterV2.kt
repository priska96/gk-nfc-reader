package expo.modules.ehcnfc.cardaccess

import android.util.Log
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.cardobjects.FileIdentifier
import expo.modules.ehcnfc.cardobjects.ShortFileIdentifier
import expo.modules.ehcnfc.helpers.prettyPrint
import org.bouncycastle.asn1.ASN1TaggedObject
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.BERTags
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1OctetString

data class FileControlParameterV2 (
    val status: LifeCycleStateV2,
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
    enum class LifeCycleStateV2 {
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
            fun parseLifeCycle(byte: UByte): LifeCycleStateV2? {
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
        const val TAG: Int = 0x02//u

        /**
         * Parse FCP (File Control Parameter) from ASN.1 formatted data
         * @param data ASN.1 encoded bytes
         * @return FileControlParameter
         * @throws IllegalArgumentException if data is empty
         * @throws Asn1ParseException if parsing fails
         */
        @Throws(IllegalArgumentException::class, Asn1ParseException::class, InvalidCardException::class)
        fun parse(data: ByteArray): FileControlParameterV2? {
            if (data.isEmpty()) {
                throw IllegalArgumentException("Non-empty value expected to create FileControlParameter")
            }

            val fcp = ASN1InputStream(data).use { asn1InputStream ->
                val asn1 = asn1InputStream.readObject() as ASN1TaggedObject
                Log.d("FileControlParameter", "asn1Doc.prettyPrint(), ${prettyPrint(asn1)}")

                if (asn1.getTagClass() != BERTags.APPLICATION) {
                    throw Asn1ParseException(
                        "ASN1Decoder did not return ApplicationSpecific ASN1Object",
                        null
                    )
                }
                if (asn1.getTagNo() != TAG) {
                    throw InvalidCardException(
                        "Unexpected tag [0x${asn1.getTagNo()}] found, " +
                                "while converting response data to FCP. Expected: [0x${
                                    TAG.toString(
                                        16
                                    )
                                }]"
                    )
                }

                // Extract the underlying object from the tagged object
                val underlyingObj = asn1.getBaseObject()  // <-- This is your sequence
                val seq = when (underlyingObj) {
                    is ASN1Sequence -> underlyingObj
                    else -> throw Asn1ParseException("Expected ASN1Sequence inside tagged object",null)
                }

                Log.d("FileControlParameter", "Underlying sequence prettyPrint: ${prettyPrint(seq)}")

                if (seq.size() <= 0) {
                    throw Asn1ParseException("No elements found in FCP Tag", null)
                }
                extractFrom(seq)
            }
            return fcp
        }

        @Throws(IllegalArgumentException::class, Asn1ParseException::class, InvalidCardException::class)
        fun extractFrom(seq: ASN1Sequence) :FileControlParameterV2 {
            var numberOfOctets: UInt = 0u
            var fileDescriptor: String? = null
            var fileIdentifier: FileIdentifier? = null
            var applicationIdentifier: ApplicationIdentifier? = null
            var lifeCycleStatus: LifeCycleStateV2? = null
            var shortFileIdentifier: ShortFileIdentifier? = null
            var readSize: UInt? = null

            val enum = seq.getObjects()
            while (enum.hasMoreElements()) {
                val child = ASN1TaggedObject.getInstance(enum.nextElement())
                val primitive = child.toASN1Primitive()
                try { // <-- Wrap parsing that can fail

                    Log.d("FileControlParameter", "extractFrom child prettyPrint: ${prettyPrint(child)}")
                    Log.d("FileControlParameter", "extractFrom primitive prettyPrint: ${prettyPrint(primitive)}")

                    when (child.getTagNo()){
                       Tag.NUMBER_OF_OCTETS.value->{
                            numberOfOctets = (primitive as ASN1Integer).intPositiveValueExact().toUInt() ?: 0u
                        }
                        Tag.FILE_DESCRIPTOR.value ->{
                            primitive?.let { fileDescriptor = (it as ASN1Integer).value.toString() }
                        }
                        Tag.FILE_IDENTIFIER.value -> {
                            primitive?.let { fileIdentifier = FileIdentifier.fromBytes(asBytes(it)) }
                        }
                        Tag.APPLICATION_IDENTIFIER.value -> {
                            primitive?.let { applicationIdentifier = ApplicationIdentifier.fromBytes(asBytes(it)) }
                        }
                        Tag.SHORT_FILE_IDENTIFIER.value -> {
                            primitive?.let { shortFileIdentifier = ShortFileIdentifier.fromAsn1(asBytes(it))}
                        }
                        Tag.LIFE_CYCLE_STATUS.value -> {
                            asBytes(primitive).firstOrNull()?.toUByte()?.let { byte:UByte -> lifeCycleStatus = LifeCycleStateV2.parseLifeCycle(byte) }
                        }
                        Tag.END_OF_FILE_POSITION.value-> {
                            readSize = (primitive as ASN1Integer).intPositiveValueExact().toUInt()
                        }
                    }
                } catch (e: Exception) {
                    throw InvalidCardException("FCP could not be created due to invalid value: ${e.message}")
                }
            }
            val lcs = lifeCycleStatus ?:
                throw InvalidCardException("FCP could not be created because of missing parameter(s)")

            return FileControlParameterV2(
                lcs,
                numberOfOctets,
                readSize,
                fileDescriptor,
                fileIdentifier,
                applicationIdentifier,
                shortFileIdentifier,
            )
        }

        fun asBytes(primitive: ASN1Encodable): ByteArray = when (val p = primitive.toASN1Primitive()) {
            is ASN1OctetString -> p.octets
            is ASN1Integer -> p.value.toByteArray() // integer converted to raw bytes
            is ASN1TaggedObject -> {
                val obj = p.getBaseObject() // unwrap the tagged object
                asBytes(obj) // recursively get the underlying bytes
            }
            else -> throw IllegalArgumentException("Unsupported ASN.1 type for raw bytes: $p")
        }

    }

    enum class Tag(val value: Int) {
        NUMBER_OF_OCTETS(0x00),
        FILE_DESCRIPTOR(0x02),
        FILE_IDENTIFIER(0x03),
        APPLICATION_IDENTIFIER(0x04),
        END_OF_FILE_POSITION(0x05),
        SHORT_FILE_IDENTIFIER(0x08),
        LIFE_CYCLE_STATUS(0x0A);
    }
}