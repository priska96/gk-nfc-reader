package expo.modules.ehcnfc.healthcardcommand

import android.util.Log
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.cardobjects.ShortFileIdentifier
import expo.modules.ehcnfc.helpers.toHexString


/**
 * Utility for creating ISO-7816-4 Read Binary APDU commands.
 *
 * Provides methods for reading files either by offset or using Short File Identifiers (SFID).
 * Automatically handles P1/P2 computation and expected response length.
 */
object Read {

    private const val CLA: Byte = 0x0.toByte()
    private const val INS: Byte = 0xB0.toByte()
    private const val SFID_MARKER: Byte = 0x80.toByte()
    private const val BYTE_MODULO = 256

    /**
     * Builds a Read Binary APDU command without SFID.
     *
     * @param ne Number of bytes expected to be read from the card. Must be > 0.
     * @param offset Optional offset in the file. Defaults to 0.
     * @return Serialized APDU command.
     */
    fun readFileCommand(ne: Int, offset: Int = 0): ApduCommand {
        require(ne > 0) { "Expected response length (Ne) must be greater than zero" }

        val p2 = (offset % BYTE_MODULO).toByte()
        val p1 = ((offset - p2.toInt()) / BYTE_MODULO).toByte()

        val apdu = ApduCommand.ofOptions(
            cla = CLA,
            ins = INS,
            p1 = p1,
            p2 = p2,
            ne = ne
        )

        Log.d("Read", "Generated APDU: ${apdu}")
        Log.d("Read", "Generated APDU HEX: ${apdu.bytes.toHexString()}")
        return apdu
    }

    /**
     * Builds a Read Binary APDU command for Short File Identifier (SFID) files.
     *
     * @param sfid The short file identifier.
     * @param ne Number of bytes expected to be read from the card. Must be > 0.
     * @param offset Optional offset within the SFID file. Defaults to 0.
     * @return Serialized APDU command.
     */
    fun readFileCommand(sfid: ShortFileIdentifier, ne: Int, offset: Int = 0): ApduCommand {
        require(ne > 0) { "Expected response length (Ne) must be greater than zero" }

        val p1 = (SFID_MARKER + sfid.rawValue[0]).toByte()
        val p2 = offset.toByte()

        val apdu = ApduCommand.ofOptions(
            cla = CLA,
            ins = INS,
            p1 = p1,
            p2 = p2,
            ne = ne
        )

        Log.d("Read", "Generated APDU with SFID: ${apdu}")
        Log.d("Read", "Generated APDU  with SFID HEX: ${apdu.bytes.toHexString()}")
        return apdu
    }
}