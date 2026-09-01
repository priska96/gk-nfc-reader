package expo.modules.ehcnfc.session

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.encoding.parse
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduConstants
import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.cardaccess.Asn1ParseException
import expo.modules.ehcnfc.helpers.BCProvider
import expo.modules.ehcnfc.helpers.Bytes.padData
import expo.modules.ehcnfc.helpers.Bytes.unPadData
import expo.modules.ehcnfc.helpers.prettyPrint
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.pace.PaceKey
import expo.modules.ehcnfc.session.tagobjects.DataObject
import expo.modules.ehcnfc.session.tagobjects.DataObjectV2
import expo.modules.ehcnfc.session.tagobjects.LengthObject
import expo.modules.ehcnfc.session.tagobjects.LengthObjectV2
import expo.modules.ehcnfc.session.tagobjects.MacObject
import expo.modules.ehcnfc.session.tagobjects.MacObjectV2
import expo.modules.ehcnfc.session.tagobjects.StatusObject
import java.io.ByteArrayOutputStream
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.spec.AlgorithmParameterSpec

private const val SECURE_MESSAGING_COMMAND = 0x0C.toByte()
private val PADDING_INDICATOR = byteArrayOf(0x01.toByte())
private const val BLOCK_SIZE = 16
private const val MAC_SIZE = 8
private const val STATUS_SIZE: Int = 0x02
private const val MIN_RESPONSE_SIZE = 12
private const val HEADER_SIZE = 4

private const val DO_81_TAG = 0x81
private const val DO_87_TAG = 0x07
private const val DO_99_TAG = 0x99
private const val DO_8E_TAG = 0x8E
private const val LENGTH_TAG = 0x80
private const val BYTE_MASK = 0x0F
private const val MALFORMED_SECURE_MESSAGING_APDU = "Malformed Secure Messaging APDU"


/**
 * Secure Messaging implementation for PACE.
 *
 * Wraps/unwraps APDUs with encryption and MAC.
 */
class SecureMessaging(
    private val paceKey: PaceKey
) {
    private val secureMessagingSSC: ByteArray = ByteArray(BLOCK_SIZE)

    private fun incrementSSC() {
        for (i in secureMessagingSSC.indices.reversed()) {
            secureMessagingSSC[i]++
            if (secureMessagingSSC[i] != 0.toByte()) {
                break
            }
        }
    }

    /**
     * Encrypts a plain APDU
     *
     * @param apduCmd plain Command APDU
     * @return encrypted Command APDU
     */
    fun encrypt(apduCmd: ApduCommand): ApduCommand {
        val apduToEncrypt = apduCmd.bytes // copy
        Log.d("SecureMessaging", "encrypt apduToEncrypt: ${apduToEncrypt.toHexString()}")

        Log.d("SecureMessaging", "encrypt before increment: ${secureMessagingSSC.toHexString()}")
        incrementSSC()
        Log.d("SecureMessaging", "encrypt after increment: ${secureMessagingSSC.toHexString()}")

        require(apduToEncrypt.size >= HEADER_SIZE) { "APDU must be at least 4 bytes long" }

        val header = apduToEncrypt.copyOfRange(0, HEADER_SIZE)
        Log.d("SecureMessaging", "encrypt headerToEncrypt: ${header.toHexString()}")
        setSecureMessagingCommand(header)
        Log.d("SecureMessaging", "encrypt header after Encrypt: ${header.toHexString()}")

        val commandDataOutput = ByteArrayOutputStream()

        apduToEncrypt.copyOfRange(
            apduCmd.dataOffset,
            apduCmd.dataOffset + apduCmd.rawNc
        )
            .takeIf { it.isNotEmpty() }
            ?.let {
                var data = it
                data = padData(data, BLOCK_SIZE)
                data = encryptData(data)
                data = PADDING_INDICATOR + data
                //Log.d("SecureMessaging", "encrypt padded DataObject to encrypt: ${DataObject(data).taggedObject.prettyPrint()}")
                Log.d("SecureMessaging", "encrypt padded DataObject to encrypt: ${DataObject(data).taggedObject.derEncoded.toHexString()}")
                // write encrypted data to output
                commandDataOutput.write(DataObject(data).taggedObject.derEncoded)
            }

        val le = apduCmd.rawNe?.also {
            // write length object to output
            //Log.d("SecureMessaging", "encrypt LengthObject to encrypt: ${LengthObject(it).taggedObject.prettyPrint()}")
            Log.d("SecureMessaging", "encrypt LengthObject to Encrypt: ${LengthObject(it).taggedObject.derEncoded.toHexString()}")

            commandDataOutput.write(LengthObject(it).taggedObject.derEncoded)
        } ?: -1
        Log.d("SecureMessaging", "encrypt le after Encrypt: ${le}")

        val commandMacObject = MacObject(header, commandDataOutput, paceKey.mac, secureMessagingSSC)
        val commandMacObjectV2 = MacObjectV2(header, commandDataOutput, paceKey.mac, secureMessagingSSC)
        //Log.d("SecureMessaging", "encrypt commandMacObject to Encrypt: ${commandMacObject.taggedObject.prettyPrint()}")
        Log.d("SecureMessaging", "encrypt commandMacObject to Encrypt: ${commandMacObject.taggedObject.derEncoded.toHexString()}")

        Log.d("SecureMessaging", "encrypt before increment: ${secureMessagingSSC.toHexString()}")
        incrementSSC()
        Log.d("SecureMessaging", "encrypt after increment: ${secureMessagingSSC.toHexString()}")

        return createEncryptedCommand(
            le = le,
            data = commandDataOutput,
            do8E = commandMacObject.taggedObject,
            header = header
        )
    }
    private fun setSecureMessagingCommand(header: ByteArray) {
        require(header[0] != ((header[0].toInt() or SECURE_MESSAGING_COMMAND.toInt()).toByte())) {
            MALFORMED_SECURE_MESSAGING_APDU
        }
        header[0] = (header[0].toInt() or SECURE_MESSAGING_COMMAND.toInt()).toByte()
    }

    private fun encryptData(paddedData: ByteArray) =
        getCipher(ENCRYPT_MODE).doFinal(paddedData)

    private fun createEncryptedCommand(
        le: Int,
        data: ByteArrayOutputStream,
        do8E: Asn1Element,
        header: ByteArray
    ): ApduCommand {
        val tempData = data
        // write do8E to output
        data.write(do8E.derEncoded)

        val ne = if (tempData.size() < 1 && le == -1) {
            ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT
        } else if (tempData.size() < 1 && le > -1) {
            ApduConstants.EXPECTED_LENGTH_WILDCARD_EXTENDED
        } else if (tempData.size() > 0 && le < 0) {
            if (data.size() <= 255) {
                ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT
            } else {
                ApduConstants.EXPECTED_LENGTH_WILDCARD_EXTENDED
            }
        } else ApduConstants.EXPECTED_LENGTH_WILDCARD_EXTENDED

        // IMPORTANT:
        //  - We already encoded Le as DO97 in `data` if caller provided it.
        //  - Do NOT add an APDU-level Le.
        //  - Prefer short APDU if possible (<= 255).
        val body = data.toByteArray()

        Log.d("SecureMessaging", "calculated ne: ${ne}")
        return ApduCommand.ofOptions(
            cla = header[0],
            ins = header[1],
            p1 = header[2],
            p2 = header[3],
            data = body,
            ne = ne
        )
    }

    /**
     * Decrypts an encrypted Response APDU
     */
    fun decrypt(responseApdu: ApduResponse): ApduResponse {
        val apduResponseBytes = responseApdu.bytes // copy

        require(apduResponseBytes.size >= MIN_RESPONSE_SIZE) { MALFORMED_SECURE_MESSAGING_APDU }

        val responseData = responseApdu.data

        // except last 10 bytes
        val protectedData = responseData.copyOfRange(0,responseData.size-10)

        // get MAC Object and verify
        val macObjectData = getMacObject(responseData, protectedData)
        val macBytes = macObjectData.first
        val responseMacObject = macObjectData.second
        checkMac(responseMacObject.mac, macBytes)

        //get STATUS Object
        val statusObjectData = getStatusObject(protectedData)
        val statusBytes = statusObjectData.first
        val responseStatusObject = statusObjectData.second

        // get DATA Object
        val dataObjectData = getDataObject(protectedData)
        if(dataObjectData == null){
            return createDecryptedResponse(statusBytes, null)
        }
        return createDecryptedResponse(statusBytes, dataObjectData.second)
    }

    private fun checkMac(mac: ByteArray, macObject: ByteArray) {
        require(mac.contentEquals(macObject)) { "Secure Messaging MAC verification failed" }
    }

    private fun getMacObject(
        responseData: ByteArray, protectedData:ByteArray
    ): Pair<ByteArray,MacObject> {
        // last 10 bytes
        val tagData = responseData.copyOfRange(responseData.size-10,responseData.size)
        val asn1TagObj =  try {
            Asn1Element.parse(tagData) // should return base ASN1Element
        } catch (e: Exception) {
            throw Asn1ParseException(
                "ASN1Decoder did not return ASN1Object",
                e
            )
        }

        val asn1TagPrimitive = asn1TagObj.asPrimitive()
        require(asn1TagPrimitive.tag.tagValue == 0xE.toULong() && asn1TagPrimitive.length == MAC_SIZE) {MALFORMED_SECURE_MESSAGING_APDU+": tag != 14 or size != 8" }
        val macBytes = asn1TagPrimitive.content

        val responseDataOutput = ByteArrayOutputStream()

        responseDataOutput.write(protectedData)

        val responseMacObject = MacObject(
            commandOutput = responseDataOutput,
            kMac = paceKey.mac,
            ssc = secureMessagingSSC
        )

        Log.d("SecureMessaging", "getMacObject MacObject: ${responseMacObject.taggedObject.derEncoded.toHexString()}")
        Log.d("SecureMessaging", "getMacObject responseMacObject.mac: ${responseMacObject.mac.toHexString()}")
        Log.d("SecureMessaging", "getMacObject macBytes: ${macBytes.toHexString()}")
        return Pair(macBytes, responseMacObject)
    }

    private fun getStatusObject (
        protectedData:ByteArray
    ): Pair<ByteArray,StatusObject> {

        val statusData = protectedData.copyOfRange(protectedData.size-4,protectedData.size)


        val asn1StatusObj =  try {
            Asn1Element.parse(statusData) // should return base ASN1Element
        } catch (e: Exception) {
            throw Asn1ParseException(
                "ASN1Decoder did not return ASN1Object",
                e
            )
        }
        val asn1StatusPrimitive = asn1StatusObj.asPrimitive()
        require(asn1StatusPrimitive.tag.tagValue == 0x19.toULong() && asn1StatusPrimitive.length == STATUS_SIZE) {MALFORMED_SECURE_MESSAGING_APDU+": status != 25 or size != 2" }
        val statusBytes = asn1StatusPrimitive.content

        val responseStatusObject = StatusObject(statusBytes)

        Log.d("SecureMessaging", "getStatusObject StatusObject: ${responseStatusObject.taggedObject.derEncoded.toHexString()}")
        Log.d("SecureMessaging", "getStatusObject statusBytes: ${statusBytes.toHexString()}")
        return Pair(statusBytes, responseStatusObject)
    }

    private fun getDataObject(
        protectedData: ByteArray
    ): Pair<ByteArray, DataObject>?{
        val messageData = protectedData.copyOfRange(0,protectedData.size-4)

        if(messageData.isEmpty()){
            return null
        }

        val asn1MessageObj =  try {
            Asn1Element.parse(messageData) // should return base ASN1Element
        } catch (e: Exception) {
            throw Asn1ParseException(
                "ASN1Decoder did not return ASN1Object",
                e
            )
        }
        val asn1MessagePrimitive = asn1MessageObj.asPrimitive()
        val messageBytes = asn1MessagePrimitive.content
        val dataObject = DataObject(messageBytes, asn1MessagePrimitive.tag.tagValue)

        Log.d("SecureMessaging", "getDataObject DataObject: ${dataObject?.taggedObject?.derEncoded?.toHexString()}")
        Log.d("SecureMessaging", "getDataObject messageBytes: ${messageBytes.toHexString()}")
        return Pair(messageBytes, dataObject)
    }

    private fun createDecryptedResponse(
        statusBytes: ByteArray,
        dataObject: DataObject?
    ): ApduResponse {
        val outputStream = ByteArrayOutputStream()
        if (dataObject != null) {
            if (dataObject.tag == DO_87_TAG.toULong()) {
                val dataDecrypted = removePaddingIndicator(dataObject.data).let {
                    getCipher(DECRYPT_MODE).doFinal(it)
                }
                outputStream.write(unPadData(dataDecrypted))
            } else {
                outputStream.write(dataObject.data)
            }
        }
        outputStream.write(statusBytes)
        Log.d("SecureMessaging", "decrypt createDecryptedResponse outputstream: ${outputStream.toByteArray().toHexString()}")
        return ApduResponse(outputStream.toByteArray())
    }

    private fun removePaddingIndicator(dataBytes: ByteArray): ByteArray =
        dataBytes.copyOfRange(1, dataBytes.size)

    private fun getCipher(mode: Int): Cipher =
        Cipher.getInstance("AES/CBC/NoPadding", BCProvider).apply {
            val key: Key = SecretKeySpec(paceKey.enc, "AES")
            val iv = createCipherIV()
            val aps: AlgorithmParameterSpec = IvParameterSpec(iv)

            init(mode, key, aps)
        }

    private fun createCipherIV(): ByteArray =
        // ECB instead of CBC on purpose. COS doesn't support CBC for this.
        Cipher.getInstance("AES/ECB/NoPadding", BCProvider).let {
            val key: Key = SecretKeySpec(paceKey.enc, "AES")
            it.init(ENCRYPT_MODE, key)
            it.doFinal(secureMessagingSSC)
        }


}
private fun InputStream.readAndCheckExpectedLength(b: ByteArray, expected: Int) {
    val l = this.read(b, 0, expected)
    require(l == expected) { MALFORMED_SECURE_MESSAGING_APDU }
}