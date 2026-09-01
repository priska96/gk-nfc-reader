package expo.modules.ehcnfc.cardaccess

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag.Template.Companion.withClass
import at.asitplus.signum.indispensable.asn1.encoding.*
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.TagClass
import expo.modules.ehcnfc.keyagreement.KeyAgreementError
import expo.modules.ehcnfc.helpers.prettyPrint
import expo.modules.ehcnfc.helpers.toHexString
import java.security.MessageDigest
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.BERTags
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter

enum class KeyFuncType(val length: Int, val checksumLength: Int) {
    AES128(length = 16, checksumLength = 20)
}

enum class Mode(val lastByte: UByte) {
    ENC(1u),
    MAC(2u),
    PASSWORD(3u)
}

object KeyDerivationFunction {

    private const val MAX = 64

    private const val TAG_6 = 6
    private const val TAG_49 = 0x49

    fun deriveKey(
        from: ByteArray,
        funcType: KeyFuncType = KeyFuncType.AES128,
        mode: Mode
    ): ByteArray {
        require(from.isNotEmpty()) { "Secret data to derive a key from must not be empty!" }

        // pad according to key deriving function
        val padded = from + ByteArray(maxOf(0, funcType.checksumLength - funcType.length))

        // replace last byte according to mode of usage
        val paddedWithMode = padded.copyOf()
        paddedWithMode[paddedWithMode.size - 1] = mode.lastByte.toByte()

        // SHA-1 hash
        val sha1 = MessageDigest.getInstance("SHA-1")
        val hash = sha1.digest(paddedWithMode)

        // truncate or pad result according to key deriving function
        val result = hash.copyOfRange(0, minOf(hash.size, funcType.length)) +
                ByteArray(maxOf(0, funcType.length - funcType.checksumLength))

        return result
    }

    /**
     * Builds ASN.1 Authentication Token (AuthToken) for MAC derivation.
     *
     * AuthToken ::= [APPLICATION 73] SEQUENCE {
     *    protocolID OBJECT IDENTIFIER,
     *    publicKey  [CONTEXT 6] OCTET STRING
     * }
     *
     * @param ecPoint ECPoint from the card.
     * @param protocolID protocolID from the used algorithm.
     * @return the DER-encoded auth token.
     */
    private fun createAsn1AuthToken(ecPoint: ByteArray, protocolID: String?): ByteArray{
        requireNotNull(protocolID) { throw KeyAgreementError.IllegalArgument("protocolID", "cannot not be null") }
        val derTaggedObject = Asn1.Sequence{
            +ObjectIdentifier(protocolID)
            +(Asn1.OctetString(ecPoint) withImplicitTag (6uL withClass TagClass.CONTEXT_SPECIFIC))
        }withImplicitTag (0x49uL withClass TagClass.APPLICATION)

        Log.d("KeyAgreement","createAsn1AuthToken: ${derTaggedObject.prettyPrint()}")
        return derTaggedObject.derEncoded
    }

//    private fun createAsn1AuthToken(ecPoint: ByteArray, protocolID: String?): ByteArray {
//        requireNotNull(protocolID) { throw KeyAgreementError.IllegalArgument("protocolID", "cannot not be null") }
//
//        val asn1EncodableVector = ASN1EncodableVector()
//        asn1EncodableVector.add(ASN1ObjectIdentifier(protocolID))
//        asn1EncodableVector.add(
//            DERTaggedObject(
//                false,
//                TAG_6,
//                DEROctetString(ecPoint)
//            )
//        )
//        val derTaggedObject = DERTaggedObject(false, BERTags.APPLICATION, TAG_49, DERSequence(asn1EncodableVector))
//        Log.d("KeyAgreement","createAsn1AuthToken: ${prettyPrint(derTaggedObject)}")
//        return derTaggedObject.encoded
//    }


    /**
     * Derives MAC value using AES-CMAC over ASN.1 AuthToken.
     *
     * @param mac MAC key
     * @param publicKey EC public key (raw uncompressed bytes)
     * @param protocolID OID of the PACE protocol
     */
    fun deriveMac(mac: ByteArray, publicKey: ByteArray, protocolID: String): ByteArray =
        CMac(AESEngine(), MAX).apply {
            init(KeyParameter(mac))

            try {
                val authToken = createAsn1AuthToken(publicKey, protocolID)
                update(authToken, 0, authToken.size)
            }
            catch (e: Exception){
                throw e
            }
        }.let {
            ByteArray(it.macSize).apply {
                it.doFinal(this, 0)
            }
        }

}
