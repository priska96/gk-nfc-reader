package expo.modules.ehcnfc

import android.util.Log
import com.google.gson.Gson
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.cardaccess.FileControlParameter
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.cardobjects.FileIdentifier
import expo.modules.ehcnfc.healthcardcommand.Read
import expo.modules.ehcnfc.healthcardcommand.Select
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.helpers.zlibDecompress
import expo.modules.ehcnfc.pace.PaceKey
import expo.modules.ehcnfc.session.SecureMessaging
import org.junit.Test
import org.junit.Assert.assertArrayEquals
import kotlin.test.assertEquals

class SecureCommandsTest {
    private val testData: TestData by lazy {
        val json = this::class.java.classLoader!!
            .getResource("test_data.json")
            .readText()
        Gson().fromJson(json, TestData::class.java)
    }

    fun printLikeExpectedApdu(apdu: ApduCommand): String {
        val header = apdu.bytes.take(4).toByteArray() // CLA INS P1 P2
        val dataBytes = apdu.onlyData // actual data
        val allBytes = header + dataBytes

        println("onlyData = ${dataBytes.toHexString()}")
        return allBytes.toHexString()
    }


    @Test
    fun testBuildSelectDFCommand() {

        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectFileCommand = Select.selectFile(aid)

        val expectedCmd = testData.apdus["selectDf"]?.expectedCmd
        println("Expected cmd = ${expectedCmd}")
        println("Actual cmd = ${selectFileCommand.bytes.toHexString()}")

        assertEquals(expectedCmd,selectFileCommand.bytes.toHexString())
    }
    @Test
    fun testBuildSelectDFCommandEncrypted() {

        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectFileCommand = Select.selectFile(aid)

        val paceKey = PaceKey(testData.paceKey.enc.hexToByteArray(), testData.paceKey.mac.hexToByteArray())
        val sm = SecureMessaging(paceKey)


        val encryptedCmd =  sm.encrypt(selectFileCommand)

        val expectedCmd = testData.apdus["selectDf"]?.expectedEncryptedCmd
        println("Expected cmd = ${expectedCmd}")
        println("Actual cmd = ${printLikeExpectedApdu(encryptedCmd)}")

        assertEquals(expectedCmd,printLikeExpectedApdu(encryptedCmd))
    }


    @Test
    fun testBuildSelectEFByFidCommand() {

        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)

        val expectedCmd = testData.apdus["selectEfByFid"]?.expectedCmd
        println("Expected cmd = ${expectedCmd}")
        println("Actual cmd = ${selectFileCommand.bytes.toHexString()}")

        assertEquals(expectedCmd,selectFileCommand.bytes.toHexString())
    }

    @Test
    fun testBuildSelectEFByFidCommandEncrypted() {
        // select DF and encrypt in order to increment ssc correctly
        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectDF = Select.selectFile(aid)

        val paceKey = PaceKey(testData.paceKey.enc.hexToByteArray(), testData.paceKey.mac.hexToByteArray())
        val sm = SecureMessaging(paceKey)

        val encryptedCmdDF =  sm.encrypt(selectDF)

        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)

        val encryptedCmd =  sm.encrypt(selectFileCommand)

        val expectedCmd = testData.apdus["selectEfByFid"]?.expectedEncryptedCmd
        println("Expected cmd = ${expectedCmd}")
        println("Actual cmd = ${printLikeExpectedApdu(encryptedCmd)}")

        assertEquals(expectedCmd,printLikeExpectedApdu(encryptedCmd))
    }

    @Test
    fun testDecryptEFByFIDResponse() {
        // select DF and encrypt in order to increment ssc correctly
        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectDF = Select.selectFile(aid)

        val paceKey = PaceKey(testData.paceKey.enc.hexToByteArray(), testData.paceKey.mac.hexToByteArray())
        val sm = SecureMessaging(paceKey)

        val encryptedCmdDF =  sm.encrypt(selectDF)
        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)

        val encryptedCmdEF =  sm.encrypt(selectFileCommand)

        val encryptedRes = testData.apdus["selectEfByFid"]?.expectedEncryptedRes?.hexToByteArray()

        require(encryptedRes?.size!! >= 2) { "Response is nil or too short" }

        // Extract SW1 and SW2
        val sw1 = encryptedRes[encryptedRes.size - 2]
        val sw2 = encryptedRes[encryptedRes.size - 1]

        // Extract data without SW1/SW2
        val dataWithoutSW = encryptedRes.copyOfRange(0,encryptedRes.size - 2)

        val res = ApduResponse(encryptedRes)
        val decryptedRes = sm.decrypt(res)

        val expectedRes = testData.apdus["selectEfByFid"]?.expectedRes
        // Print encrypted APDU
        println("Expected res = ${expectedRes}")

        val stringyfiedRes = decryptedRes.data.toHexString()
        println("Actual res = ${stringyfiedRes}")
        assertEquals(stringyfiedRes, expectedRes)
    }

    @Test
    fun testCreateFCPFromEF() {
        // select DF and encrypt in order to increment ssc correctly
        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectDF = Select.selectFile(aid)

        val paceKey = PaceKey(testData.paceKey.enc.hexToByteArray(), testData.paceKey.mac.hexToByteArray())
        val sm = SecureMessaging(paceKey)

        val encryptedCmdDF =  sm.encrypt(selectDF)
        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)

        val encryptedCmdEF =  sm.encrypt(selectFileCommand)

        val encryptedRes = testData.apdus["selectEfByFid"]?.expectedEncryptedRes?.hexToByteArray()

        require(encryptedRes?.size!! >= 2) { "Response is nil or too short" }

        // Extract SW1 and SW2
        val sw1 = encryptedRes[encryptedRes.size - 2]
        val sw2 = encryptedRes[encryptedRes.size - 1]

        // Extract data without SW1/SW2
        val dataWithoutSW = encryptedRes.copyOfRange(0,encryptedRes.size - 2)

        val res = ApduResponse(encryptedRes)
        val decryptedRes = sm.decrypt(res)

        val decryptedSelectEfResponseRaw = decryptedRes.bytes

        val fcpData = decryptedSelectEfResponseRaw.copyOfRange(
            0,
            decryptedSelectEfResponseRaw.size - 2
        )
        val fcp = FileControlParameter.parse(fcpData)
    }

    @Test
    fun testDecryptReadResponse() {
        // select DF and encrypt in order to increment ssc correctly
        val aid = ApplicationIdentifier.fromHex("D27600000102")
        val selectDF = Select.selectFile(aid)

        val paceKey = PaceKey(testData.paceKey.enc.hexToByteArray(), testData.paceKey.mac.hexToByteArray())
        val sm = SecureMessaging(paceKey)

        val encryptedCmdDF =  sm.encrypt(selectDF)
        val fid = FileIdentifier.fromHex("D001")
        val selectFileCommand = Select.selectEfRequestingFcp(fid,256)

        val encryptedCmdEF =  sm.encrypt(selectFileCommand)

        val readFileCommand = Read.readFileCommand(testData.fcp.readSize, 0)
        val encryptedCmdReadFileCommand=  sm.encrypt(readFileCommand)


        val encryptedRes = testData.apdus["readSelectedFile"]?.expectedEncryptedRes?.hexToByteArray()

        require(encryptedRes?.size!! >= 2) { "Response is nil or too short" }

        // Extract SW1 and SW2
        val sw1 = encryptedRes[encryptedRes.size - 2]
        val sw2 = encryptedRes[encryptedRes.size - 1]

        // Extract data without SW1/SW2
        val dataWithoutSW = encryptedRes.copyOfRange(0,encryptedRes.size - 2)

        val res = ApduResponse(encryptedRes)
        val decryptedRes = sm.decrypt(res)

        val expectedRes = testData.apdus["readSelectedFile"]?.expectedRes
        // Print encrypted APDU
        println("Expected res = ${expectedRes}")

        val stringyfiedRes = decryptedRes.data.toHexString()
        println("Actual res = ${stringyfiedRes}")
        assertEquals(stringyfiedRes, expectedRes)
    }

    @Test
    fun testParseData(){
        val personalDataRaw = testData.personalData.compressed.hexToByteArray()

        val data = personalDataRaw.copyOfRange(2,personalDataRaw.size)
        val decompressed = data.zlibDecompress()
        val expectedDecompress = testData.personalData.decompressed.hexToByteArray()
        assertArrayEquals(decompressed, expectedDecompress)
    }
}