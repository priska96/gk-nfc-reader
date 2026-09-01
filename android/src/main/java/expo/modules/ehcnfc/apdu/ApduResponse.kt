package expo.modules.ehcnfc.apdu

import expo.modules.ehcnfc.apdu.toHexString
import java.util.Objects

/**
 * An APDU Response. SW bytes are not validated.
 */
class ApduResponse(apdu: ByteArray?) {

    private val apdu : ByteArray

    init {
        if (apdu == null || apdu.size < 2) {
            throw ApduException("Invalid APDU response: must be at least 2 bytes")
        }
        this.apdu = apdu.copyOf()
    }


    val nr: Int
        get() = apdu.size - 2

    val data: ByteArray
        get() = apdu.copyOfRange(0, apdu.size - 2)

    val sw1: Byte
        get() = apdu[apdu.size - 2]

    val sw2: Byte
        get() = apdu[apdu.size - 1]

    val sw: ByteArray
        get() = byteArrayOf(sw1,sw2)

    val bytes: ByteArray
        get() = apdu.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApduResponse) return false

        if (sw1 != other.sw1) return false
        if (sw2 != other.sw2) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode() = Objects.hash(sw1, sw2, data.contentHashCode())

    override fun toString() =
        "ApduResponse(sw1=0x${sw1.toHexString()}, sw2=0x${sw2.toHexString()}, data=[${data.toHexString()}])"
}