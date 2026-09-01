package expo.modules.ehcnfc.cardaccess

import java.lang.IllegalArgumentException

class CAN private constructor(val rawValue: ByteArray) {

    class InvalidCAN(message: String) : IllegalArgumentException(message)

    companion object {
        /**
         * Create and validate a CAN from a ByteArray.
         * @param data the CAN value. Must be 1..16 bytes long.
         * @throws InvalidCAN if the length is out of range
         */
        @Throws(InvalidCAN::class)
        fun from(data: ByteArray): CAN {
            if (data.size !in 1..16) {
                throw InvalidCAN("Illegal CAN length: ${data.size}. Expected 1..16 bytes.")
            }
            return CAN(data.copyOf()) // defensive copy
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is CAN && rawValue.contentEquals(other.rawValue)
    }

    override fun hashCode(): Int {
        return rawValue.contentHashCode()
    }

    override fun toString(): String {
        return "CAN(rawValue=${rawValue.joinToString(",")})"
    }
}