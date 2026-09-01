package expo.modules.ehcnfc.session.tagobjects

import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.TagClass
import at.asitplus.signum.indispensable.asn1.Asn1Element.Tag.Template.Companion.withClass
import at.asitplus.signum.indispensable.asn1.encoding.*
import expo.modules.ehcnfc.helpers.Bytes.padData
import expo.modules.ehcnfc.helpers.toHexString
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter
import java.io.ByteArrayOutputStream

private const val DO_8E_TAG = 0x0E
private const val MAC_SIZE = 8
private const val BLOCK_SIZE = 16

/**
 * Mac object with TAG 8E (cryptographic checksum)
 *
 *
 * @param header byte array with extracted header from plain CommandApdu
 * @param commandDataOutput ByteArrayOutputStream with extracted data and expected length from plain CommandApdu
 * @param kMac byte array with Session key for message authentication
 * @param ssc byte array with send sequence counter
 */
class MacObject(
    private val header: ByteArray? = null,
    private val commandOutput: ByteArrayOutputStream,
    private val kMac: ByteArray,
    private val ssc: ByteArray
) {
    private var _mac: ByteArray = ByteArray(BLOCK_SIZE)
    val mac: ByteArray
        get() = _mac.copyOf()

    val taggedObject: Asn1Element
        get() =
            (Asn1.OctetString(_mac) withImplicitTag (DO_8E_TAG.toULong() withClass TagClass.CONTEXT_SPECIFIC))
    init {
        calculateMac()
    }

    private fun calculateMac() {
        val cbcMac = getCMac(ssc, kMac)

//        if (header != null) {
//            val paddedHeader = padData(header, BLOCK_SIZE)
//            cbcMac.update(paddedHeader, 0, paddedHeader.size)
//        }
//        if (commandOutput.size() > 0) {
//            val paddedData = padData(commandOutput.toByteArray(), BLOCK_SIZE)
//            cbcMac.update(paddedData, 0, paddedData.size)
//        }

        Log.d("SecureMessaging", "MacObject calculateMac ssc: ${ssc.toHexString()}")
        Log.d("SecureMessaging", "MacObject calculateMac header is null: ${header == null}")
        Log.d("SecureMessaging", "MacObject calculateMac commandOutput size: ${commandOutput.size()}")
        Log.d("SecureMessaging", "MacObject calculateMac commandOutput HEX: ${commandOutput.toByteArray().toHexString()}")
        if (header != null) { // case for cmd

            Log.d("SecureMessaging", "MacObject header HEX: ${header.toHexString()}")
            if(commandOutput.size() <= 0){
                val paddedHeader = padData(header, BLOCK_SIZE)
                cbcMac.update(paddedHeader, 0, paddedHeader.size)
            }
            else{
                val paddedHeader = padData(header, BLOCK_SIZE)
                Log.d("SecureMessaging", "MacObject paddedHeader HEX: ${paddedHeader.toHexString()}")
                val headerAndData = paddedHeader + commandOutput.toByteArray()
                val paddedHeaderAndData = padData(headerAndData, BLOCK_SIZE)
                Log.d("SecureMessaging", "MacObject headerAndPaddedData HEX: ${paddedHeaderAndData.toHexString()}")
                cbcMac.update(paddedHeaderAndData, 0, paddedHeaderAndData.size)
            }
        }
        else{ // case for res
            val padded = padData(commandOutput.toByteArray(), BLOCK_SIZE)
            cbcMac.update(padded, 0, padded.size)

        }
//        val macInput = ByteArrayOutputStream()
//        if (header != null) {
//            macInput.write(header)
//        }
//        if (commandOutput.size() > 0) {
//            macInput.write(commandOutput.toByteArray())
//        }
//
//        // apply ISO9797-1 padding to the whole thing
//        val padded = padData(macInput.toByteArray(), BLOCK_SIZE)
//
//        cbcMac.update(padded, 0, padded.size)
        cbcMac.doFinal(_mac, 0)

        _mac = _mac.copyOfRange(0, MAC_SIZE)
    }

    private fun getCMac(secureMessagingSSC: ByteArray, kMac: ByteArray): CMac =
        CMac(AESEngine()).apply {
            init(KeyParameter(kMac))
            update(secureMessagingSSC, 0, secureMessagingSSC.size)
        }
}
