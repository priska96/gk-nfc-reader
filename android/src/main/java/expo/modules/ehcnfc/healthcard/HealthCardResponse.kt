package expo.modules.ehcnfc.healthcard

import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.apdu.ResponseStatus
import expo.modules.ehcnfc.helpers.toHexString

/**
 * A semantic wrapper around a raw [ApduResponse] that also provides a decoded [ResponseStatus].
 *
 * This class represents a response from a health card in the context of ISO 7816-4 APDU exchanges.
 * It encapsulates the low-level APDU response and associates it with a higher-level [ResponseStatus],
 * making it easier to interpret whether the command succeeded or failed.
 *
 * Typical usage:
 *
 * ```
 * val apduResponse = ApduResponse(rawBytes)
 * val healthCardResponse = HealthCardResponse.from(apduResponse)
 * healthCardResponse.expect(ResponseStatus.Success) // throws if not successful
 * ```
 *
 * @property apdu the underlying raw [ApduResponse] from the card.
 * @property responseStatus the semantic status derived from the status words (SW1/SW2).
 * @property data a direct copy of the raw APDU bytes without the last 2 status bytes.
 */
data class HealthCardResponse(
    val apdu: ApduResponse,
    val responseStatus: ResponseStatus
) {
    val data: ByteArray
        get() = apdu.bytes.copyOfRange(0, apdu.bytes.size - 2)

    companion object {
        /**
         * Factory method to create a [HealthCardResponse] from a raw [ApduResponse].
         *
         * @param apdu the APDU response to wrap.
         * @return a new [HealthCardResponse] containing both the APDU and its decoded [ResponseStatus].
         */
        fun from(apdu: ApduResponse): HealthCardResponse {
            return HealthCardResponse(
                apdu = apdu,
                responseStatus = ResponseStatus.from(apdu.sw1, apdu.sw2)
            )
        }
    }

    /**
     * Ensures that the [responseStatus] matches the expected value.
     *
     * If the status does not match, a [HealthCardException] is thrown.
     * This is useful for asserting command success when interpreting responses.
     *
     * @param expected the [ResponseStatus] that is considered valid.
     * @throws HealthCardError.HealthCardException if the actual [responseStatus] differs from [expected].
     */
    @Throws(HealthCardError.HealthCardException::class)
    fun expect(expected: ResponseStatus) {
        if (responseStatus != expected) {
            throw HealthCardError.HealthCardException(responseStatus, expected)
        }
    }

    override fun toString(): String =
        "HealthCardResponse(status=$responseStatus, data=[${data.toHexString()}])"
}
