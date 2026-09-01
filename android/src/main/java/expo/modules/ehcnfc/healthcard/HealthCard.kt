package expo.modules.ehcnfc.healthcard

import android.nfc.tech.IsoDep
import expo.modules.ehcnfc.apdu.ApduCommand
import expo.modules.ehcnfc.apdu.ApduResponse
import java.io.IOException

// is basically just a wrapper around an already connected isoDep and a healthCard status
class HealthCard(
    private val isoDep: IsoDep,
    initialStatus: HealthCardStatus = HealthCardStatus.Unknown
) : HealthCardType {

    override var status: HealthCardStatus = initialStatus
        private set

    /** Maximum transceive length for this card */
    override val maxMessageLength: Int
        get() = isoDep.maxTransceiveLength

    /** Whether extended APDU length commands/responses are supported */
    override val extendedLengthSupported: Boolean
        get() = isoDep.maxTransceiveLength > 255

    /** Connect to the card if not already connected */
    @Throws(IOException::class)
    fun connect() {
        if (!isoDep.isConnected) isoDep.connect()
    }

    /** Disconnect from the card */
    @Throws(IOException::class)
    override fun disconnect(reset: Boolean) {
        if (isoDep.isConnected) isoDep.close()
    }

    /** Transmit a command to the card and receive the response */
    @Throws(IOException::class)
    override fun transmit(command: ByteArray): ByteArray {
        if (!isoDep.isConnected) throw IOException("Card is not connected")
        return isoDep.transceive(command)
    }

    @Throws(IOException::class)
    override fun transmitApdu(apdu: ApduCommand): ApduResponse {
        val responseBytes = transmit(apdu.bytes)
        return ApduResponse(responseBytes)
    }
}