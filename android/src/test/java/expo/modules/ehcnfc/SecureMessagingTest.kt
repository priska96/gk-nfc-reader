package expo.modules.ehcnfc

import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.cardobjects.FileIdentifier
import expo.modules.ehcnfc.healthcardcommand.Select
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.pace.PaceKey
import expo.modules.ehcnfc.session.SecureMessaging
import org.junit.Test
import kotlin.test.assertEquals
import org.junit.Assert.assertArrayEquals


fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

class SecureMessagingTest {
    @Test
    fun `incrementSSC increases counter correctly`() {
        val paceKey = PaceKey(ByteArray(16), ByteArray(16))
        val sm = SecureMessaging(paceKey)

        // Access private field secureMessagingSSC
        val sscField = SecureMessaging::class.java.getDeclaredField("secureMessagingSSC")
        sscField.isAccessible = true
        val ssc = sscField.get(sm) as ByteArray

        // Initial value should be all zeros
        assert(ssc.all { it == 0.toByte() })

        // Access private method incrementSSC
        val method = SecureMessaging::class.java.getDeclaredMethod("incrementSSC")
        method.isAccessible = true

        method.invoke(sm)

        // After first increment, last byte should be 1
        assertEquals(1.toByte(), ssc[ssc.size - 1])

        // Manually set last byte to 0xFF and increment to test carry
        ssc[ssc.size - 1] = 0xFF.toByte()
        method.invoke(sm)
        assertEquals(0.toByte(), ssc[ssc.size - 1])
        assertEquals(1.toByte(), ssc[ssc.size - 2])
    }

    @Test
    fun `encrpyt selectDedicatedAsync EgkFileSystemDFHCAaid correctly`(){
        val paceKey = PaceKey("03F53774D53F5CE88B7C2D53DD3AB45C".hexToByteArray(), "5B875E29351213BEEFF15BF593312834".hexToByteArray())
        val sm = SecureMessaging(paceKey)

        val selectFileCommand = ApduCommand.ofOptions(cla=0x00.toByte(), ins=0xA4.toByte(), p1=0x04.toByte(), p2=0x0C.toByte(),data="D27600000102".hexToByteArray(), ne=null)

        val encryptedCmd =  sm.encrypt(selectFileCommand)

        println("DEBUG: [selectDedicatedAsync] encryptedCmd: ${encryptedCmd.bytes.toHexString()}")

        val expectedEncryptedCmd = ApduCommand(apduBytes="0CA4040C1D871101DF48BD86FF2CB78500CC32E1E7DD29598E0823A2302C7344E79200".hexToByteArray(), rawNc=29, rawNe=256, dataOffset=5, onlyData = byteArrayOf())

        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedEncryptedCmd.bytes.toHexString()}")
        assertArrayEquals(expectedEncryptedCmd.bytes,encryptedCmd.bytes)
    }

    @Test
    fun `decrpyt selectDedicatedAsync EgkFileSystemDFHCAaid correctly`(){
        val paceKey = PaceKey("03F53774D53F5CE88B7C2D53DD3AB45C".hexToByteArray(), "5B875E29351213BEEFF15BF593312834".hexToByteArray())
        val sm = SecureMessaging(paceKey)

        val selectFileCommand = ApduCommand.ofOptions(cla=0x00.toByte(), ins=0xA4.toByte(), p1=0x04.toByte(), p2=0x0C.toByte(),data="D27600000102".hexToByteArray(), ne=null)

        val encryptedCmd =  sm.encrypt(selectFileCommand)

        val encryptedResponse = ApduResponse(apdu="990290008E089C7F97841199CCAE9000".hexToByteArray())

        val decryptedRes =  sm.decrypt(encryptedResponse)

        println("DEBUG: [selectDedicatedAsync] encryptedResponse: ${encryptedResponse.bytes.toHexString()}")

        val expectedDecryptedRes = ApduResponse(apdu="9000".hexToByteArray())

        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedDecryptedRes.bytes.toHexString()}")
        assertArrayEquals(expectedDecryptedRes.bytes,decryptedRes.bytes)
    }

//
//
//    @Test
//    fun `encrpyt selectDedicatedAsync EgkFileSystemEFhcaPDfid correctly`(){
//        val paceKey = PaceKey("03F53774D53F5CE88B7C2D53DD3AB45C".hexToByteArray(), "5B875E29351213BEEFF15BF593312834".hexToByteArray())
//        val sm = SecureMessaging(paceKey)
//
//        val selectFileCommand = ApduCommand.ofOptions(cla=0x00.toByte(), ins=0xA4.toByte(), p1=0x04.toByte(), p2=0x0C.toByte(),data="D27600000102".hexToByteArray(), ne=null)
//
//        val encryptedCmd =  sm.encrypt(selectFileCommand)
//
//        println("DEBUG: [selectDedicatedAsync] encryptedCmd: ${encryptedCmd.bytes.toHexString()}")
//
//        //val expectedEncryptedCmd = ApduCommand.ofOptions(cla=0x0C.toByte(), ins=0xA4.toByte(), p1=0x04.toByte(), p2=0x0C.toByte(),data="871101DF48BD86FF2CB78500CC32E1E7DD29598E0823A2302C7344E79200".hexToByteArray(), ne=null)
//        val expectedEncryptedCmd = ApduCommand(apduBytes="0CA4040C1D871101DF48BD86FF2CB78500CC32E1E7DD29598E0823A2302C7344E79200".hexToByteArray(), rawNc=29, rawNe=256, dataOffset=5)
//
//        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedEncryptedCmd.bytes.toHexString()}")
//        assertArrayEquals(expectedEncryptedCmd.bytes,encryptedCmd.bytes)
//    }
    @Test
    fun `selectDedicatedAsync bulid selectCmd EgkFileSystemDFHCAaid correctly`(){
        val paceKey = PaceKey("CE366D71D740E8CCBDA976B9A3B21CC9".hexToByteArray(), "3D3BB10D2CCAABA2C4125CD932582ADC".hexToByteArray())
        val sm = SecureMessaging(paceKey)
        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectFileCommand = Select.selectFile(aid)

        val expectedCmdHex = "00A4040C06D27600000102"

        println("DEBUG: [selectDedicatedAsync] encryptedCmd: ${selectFileCommand.bytes.toHexString()}")
        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedCmdHex}")
        assertEquals(expectedCmdHex,selectFileCommand.bytes.toHexString())
    }

    @Test
    fun `encrpyt selectCmd EgkFileSystemDFHCAaid correctly`(){
        val paceKey = PaceKey("CE366D71D740E8CCBDA976B9A3B21CC9".hexToByteArray(), "3D3BB10D2CCAABA2C4125CD932582ADC".hexToByteArray())
        val sm = SecureMessaging(paceKey)
        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectFileCommand = Select.selectFile(aid)

        val encryptedCmd =  sm.encrypt(selectFileCommand)

        println("DEBUG: [selectDedicatedAsync] encryptedCmd: ${encryptedCmd.bytes.toHexString()}")
        val expectedEncryptedCmdHex = "0CA4040C8711014B68176BBDD160A97D976F257834F3AB8E0822E1433056DBF06C"

        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedEncryptedCmdHex}")
        assertEquals(expectedEncryptedCmdHex,encryptedCmd.bytes.toHexString())
    }

    @Test
    fun `selectDedicatedAsync bulid selectCmd EgkFileSystemEFhcaPDfid correctly`(){
        val paceKey = PaceKey("CE366D71D740E8CCBDA976B9A3B21CC9".hexToByteArray(), "3D3BB10D2CCAABA2C4125CD932582ADC".hexToByteArray())
        val sm = SecureMessaging(paceKey)
        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)//ApduCommand.ofOptions(cla=0x00.toByte(), ins=0xA4.toByte(), p1=0x04.toByte(), p2=0x0C.toByte(),data="D27600000102".hexToByteArray(), ne=null)

        val expectedCmdHex ="00A4020402D00100"

        println("DEBUG: [selectDedicatedAsync] encryptedCmd: ${selectFileCommand.bytes.toHexString()}")
        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedCmdHex}")
        assertEquals(expectedCmdHex,selectFileCommand.bytes.toHexString())
    }
    @Test
    fun `encrpyt selectDedicatedAsync EgkFileSystemEFhcaPDfid correctly`(){
        val paceKey = PaceKey("CE366D71D740E8CCBDA976B9A3B21CC9".hexToByteArray(), "3D3BB10D2CCAABA2C4125CD932582ADC".hexToByteArray())
        val sm = SecureMessaging(paceKey)
        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)//ApduCommand.ofOptions(cla=0x00.toByte(), ins=0xA4.toByte(), p1=0x04.toByte(), p2=0x0C.toByte(),data="D27600000102".hexToByteArray(), ne=null)

        val encryptedCmd =  sm.encrypt(selectFileCommand)

        println("DEBUG: [selectDedicatedAsync] encryptedCmd: ${encryptedCmd.bytes.toHexString()}")
        val expectedEncryptedCmdHexFromSwift = "0CA402048711017F2906225A7E63EB604B9AC32310EE539701008E088283DFDA9F572B9D"
        println("DEBUG: [selectDedicatedAsync] expectedEncryptedCmd: ${expectedEncryptedCmdHexFromSwift}")
        assertEquals(expectedEncryptedCmdHexFromSwift,encryptedCmd.bytes.toHexString())
    }
}