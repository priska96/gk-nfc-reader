package expo.modules.ehcnfc.cardobjects

import expo.modules.ehcnfc.helpers.hexToByteArray
import kotlin.Result

/**
 * Identifies an application on the smart-card to prevent misuse (gemSpec_COS#N010.200)
 */
class ApplicationIdentifier private constructor(override val rawValue: ByteArray): CardObjectIdentifier {

    /* ---------- error type ---------- */

    sealed class Error(message: String) : IllegalArgumentException(message) {
        /** When the Application Identifier value is not according to gemSpec_COS#N006.700, N006.900 */
        class IllegalArgument(msg: String) : Error(msg)

        /** When the Application Identifier argument is not 2-bytes long */
        class InvalidLength(val length: Int) :
            Error("Application Identifier must be between ${AID_MIN_LEN} and ${AID_MAX_LEN} bytes (got $length)")
    }

    /* ---------- construction ---------- */

    companion object {
            private const val AID_MIN_LEN = 5
            private const val AID_MAX_LEN = 16



        fun validate(value: ByteArray): Result<ByteArray>{
            if(value.size < AID_MIN_LEN || value.size > AID_MAX_LEN){
                return Result.failure(Error.InvalidLength(length=value.size))
            }
            return Result.success(value)
        }

        /**
         * Builds an instance from a  hex string (“01AF”).
         */
        @JvmStatic
        @Throws(Error::class)
        fun fromHex(text: String): ApplicationIdentifier {
            val bytes = text.hexToByteArray()
                ?: throw Error.IllegalArgument(
                    "Application Identifier is invalid (non-hex characters found). [$text]"
                )
            return fromBytes(bytes)
        }

        /**
         * Builds an instance from the *unsigned* 8-bit value (just like Swift’s `init(_ value: UInt8)`).
         */
        @JvmStatic
        @Throws(Error::class)
        fun fromBytes(data: ByteArray): ApplicationIdentifier =
            validate(data).getOrThrow().let { ApplicationIdentifier(it) }

    }
    /* — Interface requirement — */
    override fun isValid(value: ByteArray): Result<ByteArray> = Companion.validate(value)
    // simply delegate: instance-level call → companion-object logic

    /** Returns a hex string representation of the Application Identifier */
    override fun toString(): String {
        return rawValue.joinToString(separator = "") { "%02X".format(it) }
    }
}