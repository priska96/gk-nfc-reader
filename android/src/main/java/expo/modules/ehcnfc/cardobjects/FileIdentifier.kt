package expo.modules.ehcnfc.cardobjects

import expo.modules.ehcnfc.helpers.hexToByteArray
import expo.modules.ehcnfc.helpers.toHexString
import kotlin.Result

/**
 * Identifies a file on the smart-card (gemSpec_COS 8.1.1 #N006.700, N006.900)
 */
class FileIdentifier private constructor(override val rawValue: ByteArray): CardObjectIdentifier {

    /* ---------- error type ---------- */

    sealed class Error(message: String) : IllegalArgumentException(message) {
        /** When the File Identifier value is not according to gemSpec_COS#N006.700, N006.900 */
        class IllegalArgument(msg: String) : Error(msg)

        /** When the File Identifier argument is not 2-bytes long */
        class InvalidLength(val length: Int) :
            Error("File Identifier must be exactly 2 bytes (got $length)")
    }

    /* ---------- construction ---------- */

    companion object {
        /** Builds a FileIdentifier from the two raw bytes. */
        @JvmStatic
        @Throws(Error::class)
        fun fromBytes(data: ByteArray): FileIdentifier =
            validate(data).getOrThrow().let { FileIdentifier(it) }

        /**
         * Builds an instance from a  hex string (“01AF”).
         */
        @JvmStatic
        @Throws(Error::class)
        fun fromHex(text: String): FileIdentifier {
            val bytes = text.hexToByteArray()
                ?: throw Error.IllegalArgument(
                    "File Identifier is invalid (non-hex characters found). [$text]"
                )
            return fromBytes(bytes)
        }

        /** Returns Success(bytes) if *bytes* could represent a FID, otherwise Failure(reason). */
        fun validate(value: ByteArray): Result<ByteArray> {
            // 1. exactly two bytes?
            if (value.size != 2) {
                return Result.failure(Error.InvalidLength(value.size))
            }

            // 2. convert the two bytes to an unsigned 16-bit big-endian value
            val fid: Int =
                ((value[0].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)

            // 3. range check
            return if ((fid in 0x1000..0xFEFF && fid != 0x3FFF) || fid == 0x011C) {
                Result.success(value)
            } else {
                Result.failure(
                    Error.IllegalArgument("File Identifier invalid: [0x${value.toHexString()}]")
                )
            }
        }
    }

    /* — Interface requirement — */
    override fun isValid(value: ByteArray): Result<ByteArray> = Companion.validate(value)
    // simply delegate: instance-level call → companion-object logic

    /** Returns a hex string representation of the Application Identifier */
    override fun toString(): String {
        return rawValue.joinToString(separator = "") { "%02X".format(it) }
    }
}
