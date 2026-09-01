package expo.modules.ehcnfc.helpers

import android.util.Log
import expo.modules.ehcnfc.cardaccess.Mode
import org.bouncycastle.asn1.*
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.util.zip.Deflater
import java.util.zip.Inflater


import org.bouncycastle.crypto.digests.SHA1Digest
import java.util.zip.DataFormatException
import java.util.zip.GZIPInputStream

/* -------------------------------------------------------------------------- */
/* Helper extensions (same as we used for the previous FileIdentifier sample) */
/* -------------------------------------------------------------------------- */

private val HEX_REGEX = Regex("^[0-9A-Fa-f]+$")

/**
 * Converts a hex string (without separators) to a ByteArray,
 * or returns null if the string length is odd or contains non-hex chars.
 */
fun String.hexToByteArray(): ByteArray? {
    if (length % 2 != 0 || !HEX_REGEX.matches(this)) return null
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

/** Returns the uppercase hex string representation of a ByteArray. */
fun ByteArray.toHexString() =
    joinToString("") { "%02X".format(it) }



fun prettyPrint(obj: ASN1Primitive, indent: String = ""): String {
    fun getTag(asn1Obj: ASN1Primitive): Int = when (asn1Obj) {
        is ASN1Integer -> 2
        is ASN1Boolean -> 1
        is ASN1OctetString -> 4
        is ASN1Null -> 5
        is ASN1ObjectIdentifier -> 6
        is ASN1Sequence -> 16
        is ASN1Set -> 17
        is DERBitString -> 3
        is DERIA5String -> 22
        is DERUTF8String -> 12
        else -> -1
    }

    fun getLength(asn1Obj: ASN1Primitive): Int {
        val bytes = asn1Obj.encoded
        return bytes.size
    }

    val tag = getTag(obj)
    val length = getLength(obj)

    return when (obj) {
        is ASN1Sequence -> {
            val sb = StringBuilder()
            sb.append("${indent}Sequence(tag=$tag CONSTRUCTED, length=$length)\n")
            for (i in 0 until obj.size()) {
                sb.append(prettyPrint(obj.getObjectAt(i).toASN1Primitive(), "$indent  "))
            }
            sb.toString()
        }
        is ASN1Set -> {
            val sb = StringBuilder()
            sb.append("${indent}Set(tag=$tag CONSTRUCTED, length=$length)\n")
            for (i in 0 until obj.size()) {
                sb.append(prettyPrint(obj.getObjectAt(i).toASN1Primitive(), "$indent  "))
            }
            sb.toString()
        }
        is ASN1ObjectIdentifier -> "${indent}Primitive(tag=$tag, length=$length) ${obj.id}\n"
        is ASN1Integer -> "${indent}Primitive(tag=$tag, length=$length) ${obj.value}\n"
        is ASN1Boolean -> "${indent}Primitive(tag=$tag, length=$length) ${obj.isTrue}\n"
        is ASN1OctetString -> "${indent}Primitive(tag=$tag, length=$length) ${obj.octets.joinToString("") { "%02X".format(it) }}\n"
        else -> "${indent}Primitive(tag=$tag, length=$length) $obj\n"
    }
}

enum class ModeV2 {
    ENC, // key for encryption/decryption
    MAC, // key for MAC
    PASSWORD // encryption keys from a password
}
/**
 * derive AES-128 key
 *
 * @param sharedSecretK byte array with shared secret value.
 * @param mode key derivation for ENC, MAC or derivation from password
 * @return byte array with AES-128 key
 */
fun getAES128Key(sharedSecretK: ByteArray, mode: ModeV2): ByteArray {
     val CHECKSUMLENGTH = 20
   val OFFSETLENGTH = 4
   val AES128LENGTH = 16
    val checksum = ByteArray(CHECKSUMLENGTH)
    val data = replaceLastKeyByte(sharedSecretK, mode,OFFSETLENGTH)
    SHA1Digest().apply {
        update(data, 0, data.size)
        doFinal(checksum, 0)
    }
    return checksum.copyOf(AES128LENGTH)
}
private fun replaceLastKeyByte(key: ByteArray, mode: ModeV2,OFFSETLENGTH:Int): ByteArray =
    ByteArray(key.size + OFFSETLENGTH).apply {

        val ENCLASTBYTE = 1
        val MACLASTBYTE = 2
       val PASSWORDLASTBYTE = 3
        key.copyInto(this)
        this[this.size - 1] = when (mode) {
            ModeV2.ENC -> ENCLASTBYTE.toByte()
            ModeV2.MAC -> MACLASTBYTE.toByte()
            ModeV2.PASSWORD -> PASSWORDLASTBYTE.toByte()
        }
    }

/**
 * Decompress a byte array using ZLIB or GZIP.
 *
 * @return an UTF-8 encoded string.
 */
fun ByteArray.zlibDecompress(): ByteArray {
    return when {
        isGzip() -> GZIPInputStream(this.inputStream()).readBytes()
        isLikelyRawDeflate() -> {
            val inflater = Inflater(true) // nowrap = true
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            inflater.setInput(this)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                out.write(buffer, 0, count)
            }
            inflater.end()
            out.toByteArray()
        }
        else -> { // zlib wrapper
            val inflater = Inflater()
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            inflater.setInput(this)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                out.write(buffer, 0, count)
            }
            inflater.end()
            out.toByteArray()
        }
    }
}

private fun ByteArray.isGzip() =
    size > 2 && this[0] == 0x1f.toByte() && this[1] == 0x8b.toByte()

private fun ByteArray.isLikelyRawDeflate(): Boolean =
    !isGzip() && (this[0].toInt() and 0x0F == 0x08)