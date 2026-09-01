package expo.modules.ehcnfc.cardobjects

import expo.modules.ehcnfc.helpers.hexToByteArray
import expo.modules.ehcnfc.helpers.toHexString
import kotlin.Result


/**
 * Identifies a file on the smart-card (gemSpec_COS#N007.000)
 */
class ShortFileIdentifier private constructor(override val rawValue: ByteArray): CardObjectIdentifier {

    /* ---------- error type ---------- */

    sealed class Error(message: String) : IllegalArgumentException(message) {
        class IllegalArgument(msg: String) : Error(msg)
    }

    /* ---------- construction ---------- */

    companion object {
       private const val SFID_MIN = 1
       private const val SFID_MAX = 30


       /* ---------- validation helpers (Swift: static func isValid…) ---------- */

       fun isValid(_value: ByteArray): Result<ByteArray> {
           if (_value.size != 1) {
               return Result.failure(
                   Error.IllegalArgument("Short File Identifier is invalid: [0x${_value.toHexString()}]")
               )
           }
           return validateByte(_value[0])
       }

       fun validateByte(value: Byte): Result<ByteArray> {
           val unsigned = value.toInt() and 0xFF         // Byte is signed in Kotlin

           if (!(unsigned >= SFID_MIN && unsigned <= SFID_MAX)) {
               return Result.failure(
                   Error.IllegalArgument(
                       "Short File Identifier is invalid: [0x%02X]".format(unsigned)
                   )
               )
           }
           return Result.success(byteArrayOf(value))
       }

       /**
        * Builds an instance from an ASN.1 *primitive* (one byte whose 3 top bits are tag info).
        * Swift: `init(asn1 data: Data)` which right-shifted by 3.
        *
        * @param asn1 one-byte ASN.1 field
        */
       @Throws(Error::class)
       fun fromAsn1(asn1: ByteArray): ShortFileIdentifier{
           if(asn1.size != 1){
               throw Error.IllegalArgument(
                   "Parsing [ASN.1] Short File Identifier is invalid: [0x${asn1.toHexString()}]"
               )
           }
           val sfid = (asn1[0].toInt() and 0xFF) ushr 3 // unsigned shift
           return fromByte(sfid.toByte())
       }

       /**
        * Builds an instance from a *single-byte* hex string ("1E").
        */
       @Throws(Error::class)
       fun fromHex(hex: String): ShortFileIdentifier{
           val bytes = hex.hexToByteArray()
               ?: throw Error.IllegalArgument(
                   "Short File Identifier is invalid (non-hex characters found). [$hex]"
               )
           if(bytes.size !=1){
               throw Error.IllegalArgument("Short File Identifier is invalid: [$hex] (must encode exactly 1 byte)")
           }
           return fromByte(bytes[0])
       }

       /**
        * Builds an instance from the *unsigned* 8-bit value (just like Swift’s `init(_ value: UInt8)`).
        */
       @Throws(Error::class)
       fun fromByte(value: Byte): ShortFileIdentifier =
           validateByte(value).getOrThrow().let { ShortFileIdentifier(it) }
   }
    /* — Interface requirement — */
    override fun isValid(value: ByteArray): Result<ByteArray> = Companion.isValid(value)
    // simply delegate: instance-level call → companion-object logic
}

