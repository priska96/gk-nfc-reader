package expo.modules.ehcnfc.healthcard

import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduResponse
import java.io.IOException

/** Represents a health card that can be communicated with */
interface HealthCardType {

    /** The current status of the card */
    val status: HealthCardStatus

    /** Maximum transceive length for this card */
    val maxMessageLength: Int

    /** Whether extended APDU length commands/responses are supported */
    val extendedLengthSupported: Boolean

    /**
     * Disconnects from the card.
     * @param reset Not used in Android; included for API similarity
     */
    @Throws(IOException::class)
    fun disconnect(reset: Boolean = false)

    /**
     * Transmits a command to the card and returns the response.
     * Wraps the low-level IsoDep.transceive operation.
     */
    @Throws(IOException::class)
    fun transmit(command: ByteArray): ByteArray

    @Throws(IOException::class)
    fun transmitApdu(apdu: ApduCommand): ApduResponse
}
