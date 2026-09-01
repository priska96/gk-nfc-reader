package expo.modules.ehcnfc.healthcard

import expo.modules.ehcnfc.apdu.ResponseStatus
import expo.modules.ehcnfc.cardobjects.ApplicationIdentifier
import expo.modules.ehcnfc.cardobjects.FileIdentifier

sealed class HealthCardError(message: String?) : Exception(message) {
    class HealthCardException(
        val actual: ResponseStatus,
        val expected: ResponseStatus
    ) : Exception("Unexpected response. Expected=$expected, Actual=$actual")

    class ReadErrorUnexpectedResponseException(status: ResponseStatus): Exception("Unexpected read response. Got=$status")
    class ReadErrorNoDataException(status: ResponseStatus): Exception("Response has no data. ResponseStatus=$status")
    class ReadErrorFcpMissingReadSizeException(status: ResponseStatus): Exception("FCP has no readsize. ResponseStatus=$status")

    class SelectErrorFailedToSelectAidException(
        aid: ApplicationIdentifier,
        status: ResponseStatus
    ): Exception("Failed to select Aid with aid=$aid. ResponseStatus=$status")
    class SelectErrorFailedToSelectFidException(
        fid: FileIdentifier,
        status: ResponseStatus
    ): Exception("Failed to select Fid with aid=$fid. ResponseStatus=$status")
}
