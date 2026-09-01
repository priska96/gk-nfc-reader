package expo.modules.ehcnfc.healthcardcommand

import android.util.Log
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.cardobjects.FileIdentifier
import expo.modules.ehcnfc.helpers.toHexString

/**
 * Utility for creating ISO-7816-4 SELECT APDU commands.
 *
 * Supports selecting the root, files by AID, and EFs by FID.
 * Handles FCP requests, first/next occurrence, and extended length automatically.
 */
object Select {

    private const val CLA: Byte = 0x00
    private const val INS: Byte = 0xA4.toByte()

    private const val P1_SELECTION_MODE_PARENT: Byte = 0x03
    private const val P1_SELECTION_MODE_AID: Byte = 0x04
    private const val P1_SELECTION_MODE_DF_WITH_FID: Byte = 0x01
    private const val P1_SELECTION_MODE_EF_WITH_FID: Byte = 0x02

    private const val RESPONSE_TYPE_FCP: Byte = 0x04
    private const val RESPONSE_TYPE_NO_RESPONSE: Byte = 0x0C
    private const val FILE_OCCURRENCE_FIRST: Byte = 0x00
    private const val FILE_OCCURRENCE_NEXT: Byte = 0x02

    /**
     * Builds a SELECT APDU command.
     *
     * @param p1 P1 parameter.
     * @param p2 P2 parameter.
     * @param data Optional data field (e.g., AID or FID).
     * @param expectedLength Optional expected response length.
     */
    private fun buildCommand(p1: Byte, p2: Byte, data: ByteArray?=null, expectedLength: Int?=null): ApduCommand {
        val apdu = ApduCommand.ofOptions(
            cla = CLA,
            ins = INS,
            p1 = p1,
            p2 = p2,
            data = data,
            ne = expectedLength
        )

        Log.d("Select", "Generated APDU: ${apdu}")
        Log.d("Select", "Generated APDU HEX: ${apdu.bytes.toHexString()}")
        return apdu
    }

    /**
     * Selects the root of the card object system (MF).
     */
    fun selectRoot(): ApduCommand {
        val p2 = calculateP2(fcp = false, nextOccurrence = false)
        return buildCommand(P1_SELECTION_MODE_AID, p2)
    }

    /**
     * Selects the root and requests File Control Parameters (FCP).
     *
     * @param expectedLength Number of bytes expected in the response. Must be > 0.
     */
    fun selectRootRequestingFcp(expectedLength: Int): ApduCommand {
        require(expectedLength > 0) { "Expected response length must be greater than zero" }

        val p2 = calculateP2(fcp = true, nextOccurrence = false)
        return buildCommand(P1_SELECTION_MODE_AID, p2, expectedLength = expectedLength)
    }

    /**
     * Selects an application by its Application Identifier (AID).
     *
     * @param aid Application Identifier.
     * @param nextOccurrence If true, selects the next occurrence of the application.
     */
    fun selectFile(aid: ApplicationIdentifier, nextOccurrence: Boolean = false): ApduCommand {
        val p2 = calculateP2(fcp = false, nextOccurrence = nextOccurrence)
        return buildCommand(P1_SELECTION_MODE_AID, p2, data = aid.rawValue)
    }


    /**
     * Selects an Elementary File (EF) by its File Identifier (FID).
     */
    fun selectEf(fid: FileIdentifier): ApduCommand {
        val p2 = calculateP2(fcp = false, nextOccurrence = false)
        return buildCommand(P1_SELECTION_MODE_EF_WITH_FID, p2, data = fid.rawValue)
    }

    /// Use case Select EF with File Identifier requesting FCP gemSpec_Cos#14.2.6.14
    fun selectEfRequestingFcp(fid: FileIdentifier, expectedLength: Int):ApduCommand{
        require(expectedLength > 0) { "Expected response length must be greater than zero" }
        val p2 =RESPONSE_TYPE_FCP// calculateP2(fcp = true, nextOccurrence = false)
        return buildCommand(P1_SELECTION_MODE_EF_WITH_FID, p2, data = fid.rawValue, expectedLength=expectedLength)
    }

    /**
     * Computes the P2 parameter based on whether FCP is requested and whether next occurrence is needed.
     */
    private fun calculateP2(fcp: Boolean, nextOccurrence: Boolean): Byte {
        val responseType = if (fcp) RESPONSE_TYPE_FCP else RESPONSE_TYPE_NO_RESPONSE
        val occurrence = if (nextOccurrence) FILE_OCCURRENCE_NEXT else FILE_OCCURRENCE_FIRST
        return (responseType + occurrence).toByte()
    }
}