package expo.modules.ehcnfc

import android.util.Log
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.healthcardcommand.Select
import expo.modules.ehcnfc.helpers.BCProvider
import expo.modules.ehcnfc.helpers.Bytes.padData
import expo.modules.ehcnfc.session.tagobjects.MacObject
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.session.tagobjects.DataObject
import expo.modules.ehcnfc.session.tagobjects.LengthObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.security.Key
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.Cipher.ENCRYPT_MODE
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


private val PADDING_INDICATOR = byteArrayOf(0x01.toByte())
private const val BLOCK_SIZE = 16

fun incrementSSC(ssc:ByteArray) {
    for (i in ssc.indices.reversed()) {
        ssc[i]++
        if (ssc[i] != 0.toByte()) {
            break
        }
    }
}

private fun encryptData(paddedData: ByteArray, paceKeyEnc: ByteArray, ssc:ByteArray) =
    getCipher(ENCRYPT_MODE,paceKeyEnc,ssc).doFinal(paddedData)

private fun getCipher(mode: Int, paceKeyEnc: ByteArray, ssc:ByteArray): Cipher =
    Cipher.getInstance("AES/CBC/NoPadding", BCProvider).apply {
        val key: Key = SecretKeySpec(paceKeyEnc, "AES")
        val iv = createCipherIV(paceKeyEnc,ssc)
        val aps: AlgorithmParameterSpec = IvParameterSpec(iv)

        init(mode, key, aps)
    }

private fun createCipherIV(paceKeyEnc: ByteArray, ssc: ByteArray): ByteArray =
    // ECB instead of CBC on purpose. COS doesn't support CBC for this.
    Cipher.getInstance("AES/ECB/NoPadding", BCProvider).let {
        val key: Key = SecretKeySpec(paceKeyEnc, "AES")
        it.init(ENCRYPT_MODE, key)
        it.doFinal(ssc)
    }

class MacObjectTest {

//    @Test
//    fun `calculates deterministic MAC with header and data`() {
//        val kMac = "5B875E29351213BEEFF15BF593312834".hexToByteArray()
//        val ssc = "0000000000000001".hexToByteArray() // SSC = 1
//        val header = "0CA4040C".hexToByteArray()
//        val commandData = "D27600000102".hexToByteArray()
//
//        val outputStream = ByteArrayOutputStream().apply {
//            write(commandData)
//        }
//
//        val macObject1 = MacObject(header, outputStream, kMac, ssc)
//        val macObject2 = MacObject(header, outputStream, kMac, ssc)
//
//        println("MAC1 = ${macObject1.mac.toHexString()}")
//        println("MAC2 = ${macObject2.mac.toHexString()}")
//
//        // 1. Determinism
//        assertArrayEquals(macObject1.mac, macObject2.mac)
//
//        // 2. Known expected value (replace with your real expected)
//        val expectedMac = "2C7344E79200".hexToByteArray() // <-- example, adapt!
//        assertTrue(macObject1.mac.isNotEmpty())
//        //assertArrayEquals(expectedMac, macObject1.mac) // enable once known
//    }

    @Test
    fun `calculates deterministic MAC with only data`() {
        val kMac = "5B875E29351213BEEFF15BF593312834".hexToByteArray()
        val ssc = "0000000000000001".hexToByteArray()

        val outputStream = ByteArrayOutputStream().apply {
            write("99029000".hexToByteArray())
        }

        val macObject = MacObject(null, outputStream, kMac, ssc)

        println("MAC = ${macObject.mac.toHexString()}")
        assertTrue(macObject.mac.isNotEmpty())
    }

    @Test
    fun `taggedObject has correct tag`() {
        val kMac = "5B875E29351213BEEFF15BF593312834".hexToByteArray()
        val ssc = "0000000000000001".hexToByteArray()
        val outputStream = ByteArrayOutputStream()

        val macObject = MacObject(null, outputStream, kMac, ssc)

        assertEquals(0x0E, macObject.taggedObject.tag.tagValue.toInt())
    }


    @Test
    fun `calculates MAC withvalues from swift`() {
        val kMac = "3D3BB10D2CCAABA2C4125CD932582ADC".hexToByteArray()
        val kEnc = "CE366D71D740E8CCBDA976B9A3B21CC9".hexToByteArray()

        val ssc = ByteArray(BLOCK_SIZE)
        incrementSSC(ssc)

        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val apduCmd = Select.selectFile(aid)

        val commandDataOutput = ByteArrayOutputStream()

        val header = "00A4040C".hexToByteArray()
        val apduToEncrypt = apduCmd.bytes // copy
        apduToEncrypt.copyOfRange(
            apduCmd.dataOffset,
            apduCmd.dataOffset + apduCmd.rawNc
        )
            .takeIf { it.isNotEmpty() }
            ?.let {
                var data = it
                data = padData(data, BLOCK_SIZE)
                data = encryptData(data,kEnc,ssc)
                data = PADDING_INDICATOR + data
                println("DataObject to encrypt: ${DataObject(data).taggedObject.derEncoded.toHexString()}")

                commandDataOutput.write(DataObject(data).taggedObject.derEncoded)
            }

        val macObject = MacObject(header, commandDataOutput, kMac, ssc)
        val expectedMacHex = "22E1433056DBF06C"
        println("MAC = ${macObject.taggedObject.derEncoded.toHexString()}")
        assertEquals(expectedMacHex, macObject.taggedObject.derEncoded.toHexString())
    }

}