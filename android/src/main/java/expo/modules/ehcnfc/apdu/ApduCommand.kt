package expo.modules.ehcnfc.apdu

import android.util.Log
import expo.modules.ehcnfc.apdu.asUnsignedInt
import expo.modules.ehcnfc.apdu.bytesToInt
import expo.modules.ehcnfc.apdu.toHexString
import java.io.ByteArrayOutputStream

/**
 * Value for when wildcardShort for expected length encoding is needed
 */
const val EXPECTED_LENGTH_WILDCARD_EXTENDED: Int = 65536
const val EXPECTED_LENGTH_WILDCARD_SHORT: Int = 256

private fun encodeDataLengthExtended(nc: Int): ByteArray =
    byteArrayOf(0x0, (nc shr 8).toByte(), (nc and 0xFF).toByte())

private fun encodeDataLengthShort(nc: Int): ByteArray =
    byteArrayOf(nc.toByte())

private fun encodeExpectedLengthExtended(ne: Int): ByteArray =
    if (ne != EXPECTED_LENGTH_WILDCARD_EXTENDED) { // == 65536
        byteArrayOf((ne shr 8).toByte(), (ne and 0xFF).toByte()) // l1, l2
    } else {
        byteArrayOf(0x0, 0x0)
    }

private fun encodeExpectedLengthShort(ne: Int): ByteArray =
    byteArrayOf(
        if (ne != EXPECTED_LENGTH_WILDCARD_EXTENDED) {
            ne.toByte()
        } else {
            0x0
        }
    )

/**
 * An APDU (Application Protocol Data Unit) Command per ISO/IEC 7816-4.
 * Command APDU encoding options:
 *
 * ```
 * case 1:  |CLA|INS|P1 |P2 |                                 len = 4
 * case 2s: |CLA|INS|P1 |P2 |LE |                             len = 5
 * case 3s: |CLA|INS|P1 |P2 |LC |...BODY...|                  len = 6..260
 * case 4s: |CLA|INS|P1 |P2 |LC |...BODY...|LE |              len = 7..261
 * case 2e: |CLA|INS|P1 |P2 |00 |LE1|LE2|                     len = 7
 * case 3e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|          len = 8..65542
 * case 4e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|LE1|LE2|  len =10..65544
 *
 * LE, LE1, LE2 may be 0x00.
 * LC must not be 0x00 and LC1|LC2 must not be 0x00|0x00
 * ```
 */
class ApduCommand(
    apduBytes: ByteArray,
    val rawNc: Int,
    val rawNe: Int?,
    val dataOffset: Int,
    val onlyData: ByteArray,
) {
    private val _apduBytes = apduBytes.copyOf()
    val bytes
        get() = _apduBytes.copyOf()

    companion object {
        fun ofOptions(
            cla: Byte,
            ins: Byte,
            p1: Byte,
            p2: Byte,
            ne: Int?
        ) = ofOptions(cla = cla, ins = ins, p1 = p1, p2 = p2, data = null, ne = ne)

        @Suppress("CyclomaticComplexMethod")
        fun ofOptions(
            cla: Byte,
            ins: Byte,
            p1: Byte,
            p2: Byte,
            data: ByteArray?,
            ne: Int?
        ): ApduCommand {
            require(!(cla.toUnsignedInt() < 0 || ins.toUnsignedInt() < 0 ||
                    p1.toUnsignedInt() < 0 || p2.toUnsignedInt() < 0)) {
                "APDU header fields must not be less than 0"
            }

            require(!(cla.toUnsignedInt() > 0xFF || ins.toUnsignedInt() > 0xFF ||
                    p1.toUnsignedInt() > 0xFF || p2.toUnsignedInt() > 0xFF)) {
                "APDU header fields must not be greater than 255 (0xFF)"
            }
            ne?.let {
                require(ne <= EXPECTED_LENGTH_WILDCARD_EXTENDED || ne >= 0) {
                    "APDU response length is out of bounds [0, 65536]"
                }
            }

            val bytes = ByteArrayOutputStream()
            // write header |CLA|INS|P1 |P2 |
            bytes.write(byteArrayOf(cla, ins, p1, p2))

            return if (data != null) {
                val nc = data.size
                require(nc <= 65535) { "ADPU cmd data length must not exceed 65535 bytes" }

                var dataOffset: Int
                var le: Int? // le1, le2
                if (ne != null) {
                    le = ne
                    // case 4s or 4e
                    if (nc <= 255 && ne <= EXPECTED_LENGTH_WILDCARD_SHORT) {
                        // case 4s
                        dataOffset = 5
                        bytes.write(encodeDataLengthShort(nc))
                        bytes.write(data)
                        bytes.write(encodeExpectedLengthShort(ne))
                    } else {
                        // case 4e
                        dataOffset = 7
                        bytes.write(encodeDataLengthExtended(nc))
                        bytes.write(data)
                        bytes.write(encodeExpectedLengthExtended(ne))
                    }
                } else {
                    // case 3s or 3e
                    le = null
                    if (nc <= 255) {
                        // case 3s
                        dataOffset = 5
                        bytes.write(encodeDataLengthShort(nc))
                    } else {
                        // case 3e
                        dataOffset = 7
                        bytes.write(encodeDataLengthExtended(nc))
                    }
                    bytes.write(data)
                }

                ApduCommand(
                    apduBytes = bytes.toByteArray(),
                    rawNc = nc,
                    rawNe = le,
                    dataOffset = dataOffset,
                    onlyData = data
                )
            } else {
                // data empty
                if (ne != null) {
                    // case 2s or 2e
                    if (ne <= EXPECTED_LENGTH_WILDCARD_SHORT) {
                        // case 2s
                        // 256 is encoded 0x0
                        bytes.write(encodeExpectedLengthShort(ne))
                    } else {
                        // case 2e
                        bytes.write(0x0)
                        bytes.write(encodeExpectedLengthExtended(ne))
                    }

                    ApduCommand(
                        apduBytes = bytes.toByteArray(),
                        rawNc = 0,
                        rawNe = ne,
                        dataOffset = 0, onlyData = byteArrayOf()
                    )
                } else {
                    // case 1
                    ApduCommand(
                        apduBytes = bytes.toByteArray(),
                        rawNc = 0,
                        rawNe = null,
                        dataOffset = 0, onlyData = byteArrayOf()
                    )
                }
            }
        }
    }

    // Extract header fields from APDU bytes
    val commandClass: Byte get() = _apduBytes[0]
    val instruction: Byte get() = _apduBytes[1]
    val parameter1: Byte get() = _apduBytes[2]
    val parameter2: Byte get() = _apduBytes[3]
    val data: ByteArray
        get() = if (_apduBytes.size > dataOffset) {
            _apduBytes.copyOfRange(dataOffset, _apduBytes.size)
        } else {
            byteArrayOf() // empty if no data
        }
    override fun toString() =
        "ApduCommand(CLA=0x${commandClass.toHexString()}, " +
        "INS=0x${instruction.toHexString()}, " +
        "P1=0x${parameter1.toHexString()}, P2=0x${parameter2.toHexString()}, " +
        "dataOffset=$dataOffset, Nc=$rawNc, Ne=$rawNe, " +
        "data=[${data.toHexString()}])"
}


fun Byte.toUnsignedInt() = this.toInt() and 0xFF