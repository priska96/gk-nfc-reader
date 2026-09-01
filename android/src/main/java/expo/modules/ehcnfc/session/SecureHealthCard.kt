package expo.modules.ehcnfc.session

import android.util.Log
import expo.modules.ehcnfc.apdu.ResponseStatus
import expo.modules.ehcnfc.cardaccess.FileControlParameter
import expo.modules.ehcnfc.cardobjects.DedicatedFile
import expo.modules.ehcnfc.cardobjects.EgkFileSystem
import expo.modules.ehcnfc.cardobjects.FileIdentifier
import expo.modules.ehcnfc.healthcard.HealthCardError
import expo.modules.ehcnfc.healthcard.HealthCardResponse
import expo.modules.ehcnfc.healthcard.HealthCardType
import expo.modules.ehcnfc.healthcardcommand.Read
import expo.modules.ehcnfc.healthcardcommand.Select
import expo.modules.ehcnfc.helpers.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides high-level secure operations on the health card, such as selecting files and reading personal data.
 *
 * @property healthCard The health card interface for APDU communication.
 * @property secureMessaging The secure messaging handler for encryption/decryption.
 */
class SecureHealthCard(
    private val healthCard: HealthCardType,
    private val secureMessaging: SecureMessaging
) {

    /**
     * Selects the personal data file (EF.hcaPD) and reads its contents.
     *
     * @return The file data as a ByteArray.
     * @throws HealthCardError.ReadErrorFcpMissingReadSizeException If FCP has no readSize.
     */
    @Throws(HealthCardError.ReadErrorFcpMissingReadSizeException::class)
    suspend fun selectAndReadHcaPD(): ByteArray {
        Log.d("SecureHealthCard", "Select Personal Data and Read file")

        val dedicatedFile = DedicatedFile(
            EgkFileSystem.DF.HCA.aid,
            EgkFileSystem.EF.hcaPD.fid // Personal Data of health card holder
        )
        try {
            val response = selectDedicatedAsync(dedicatedFile, true)
            val resStatus = response.first
            val fcp = response.second
            val readSize = fcp?.readSize ?: throw HealthCardError.ReadErrorFcpMissingReadSizeException(resStatus)
            val data = readSelectedFileAsync(readSize.toInt())
            return data
        } catch (e: Exception) {
            Log.e("SecureHealthCard", "failed to select EF.hcaPD: ${e.message}")
            throw e
        }
    }

    /**
     * Selects a dedicated file (DF/EF) on the card, optionally requesting its File Control Parameter (FCP).
     *
     * @param file The file to select.
     * @param requestByFcp Whether to request the File Control Parameter.
     * @param length Expected FCP length (only used if requestByFcp is true).
     * @return Pair of ResponseStatus("success"/"eof"/etc.) and the parsed FileControlParameter (if requested).
     * @throws HealthCardError.ReadErrorNoDataException If reading selected file has no data.
     * @throws HealthCardError.SelectErrorFailedToSelectAidException If selection of aid fails.
     * @throws HealthCardError.SelectErrorFailedToSelectFidException If selection of fid fails.
     */
    @Throws(
        HealthCardError.ReadErrorNoDataException::class,
        HealthCardError.SelectErrorFailedToSelectAidException::class,
        HealthCardError.SelectErrorFailedToSelectFidException::class
    )
    suspend fun selectDedicatedAsync(
        file: DedicatedFile,
        requestByFcp: Boolean = false,
        length: Int = 256
    ): Pair<ResponseStatus, FileControlParameter?> = withContext(Dispatchers.IO) {
        var selectDfResponseStatus: ResponseStatus = ResponseStatus.Unknown(-1)
        try {
            Log.d("SecureHealthCard", "selectDedicatedAsync START SELECT DF")

            val selectDfCommand = Select.selectFile(file.aid)
            Log.d("SecureHealthCard", "selectDedicatedAsync selectDfCommand: ${selectDfCommand.bytes.toHexString()}")
            val encrptedCmd = secureMessaging.encrypt(selectDfCommand)
            Log.d("SecureHealthCard", "selectDedicatedAsync encrptedCmd: ${encrptedCmd.bytes.toHexString()}")
            val selectDfResponse = HealthCardResponse.from(healthCard.transmitApdu(encrptedCmd))
            selectDfResponseStatus = selectDfResponse.responseStatus
            selectDfResponse.expect(ResponseStatus.Success)

            Log.d("SecureHealthCard", "selectDedicatedAsync selectDfResponse data without SW: ${selectDfResponse.data.toHexString()}")
            Log.d("SecureHealthCard", "selectDedicatedAsync selectDfResponseApdu: ${selectDfResponse}")

            val decryptedSelectDfResponse = HealthCardResponse.from(secureMessaging.decrypt(selectDfResponse.apdu))
            selectDfResponse.expect(ResponseStatus.Success)

            Log.d("SecureHealthCard", "selectDedicatedAsync decryptedSelectDfResponse data without SW: ${decryptedSelectDfResponse.data.toHexString()}")
        } catch (e: Exception) {
            throw HealthCardError.SelectErrorFailedToSelectAidException(file.aid, selectDfResponseStatus)
        }

        Log.d("SecureHealthCard", "selectDedicatedAsync START SELECT EF AND REQUEST FCP")
        var fid: FileIdentifier? = null
        if(file.fid == null) {
            return@withContext Pair(selectDfResponseStatus, null)
        }

        fid = file.fid

        var selectEfResponseStatus: ResponseStatus = ResponseStatus.Unknown(-1)
        try {
            val selectEfCommand = if (requestByFcp) {
                Select.selectEfRequestingFcp(fid, length)
            } else {
                Select.selectEf(fid)
            }
            Log.d("SecureHealthCard", "selectDedicatedAsync selectEfCommand : ${selectEfCommand.bytes.toHexString()}")
            val encryptedSelectEfCmd = secureMessaging.encrypt(selectEfCommand)
            Log.d("SecureHealthCard", "selectDedicatedAsync encryptedSelectEfCmd: ${encryptedSelectEfCmd.bytes.toHexString()}")
            val selectEfResponse = HealthCardResponse.from(healthCard.transmitApdu(encryptedSelectEfCmd))
            selectEfResponseStatus = selectEfResponse.responseStatus
            selectEfResponse.expect(ResponseStatus.Success)

            Log.d("SecureHealthCard", "selectDedicatedAsync selectEfResponse data without SW: ${selectEfResponse.data.toHexString()}")
            val decryptedSelectEfResponse = HealthCardResponse.from(secureMessaging.decrypt(selectEfResponse.apdu))
            decryptedSelectEfResponse.expect(ResponseStatus.Success)
            Log.d("SecureHealthCard", "selectDedicatedAsync decryptedSelectEfResponse data without SW: ${decryptedSelectEfResponse.data.toHexString()}")

            if (requestByFcp) {
                try {
                    val fcpData = decryptedSelectEfResponse.data
                    if (fcpData.isEmpty()) {
                        throw HealthCardError.ReadErrorNoDataException(selectEfResponseStatus)
                    }
                    val fcp = FileControlParameter.parse(fcpData)
                    return@withContext Pair(selectEfResponseStatus, fcp)
                } catch (e: Exception) {
                    Log.e("SecureHealthCard", "Parsing FCP from EF failed: ${e.message}")
                    throw e
                }
            } else {
                return@withContext Pair(selectEfResponseStatus, null)
            }
        } catch (e: Exception) {
            throw HealthCardError.SelectErrorFailedToSelectFidException(fid, selectEfResponseStatus)
        }
    }

    /**
     * Reads the currently selected DF/EF file from the card.
     *
     * Reads until the expected number of bytes is received or an end-of-file warning is returned.
     * If the file is larger than the channel's max response length, reads in chunks.
     *
     * @param expectedSize The expected file size (must be greater than 0 or null).
     * @param failOnEndOfFileWarning Whether to fail on end-of-file warning (default: true).
     * @param offset The offset to start reading from (default: 0).
     * @return The file data as a ByteArray.
     * @throws HealthCardError.ReadErrorNoDataException If reading returned no data.
     * @throws HealthCardError.ReadErrorUnexpectedResponseException If reading fails .
     */
    @Throws(HealthCardError.ReadErrorNoDataException::class, HealthCardError.ReadErrorUnexpectedResponseException::class)
    suspend fun readSelectedFileAsync(
        expectedSize: Int?,
        failOnEndOfFileWarning: Boolean = true,
        offset: Int = 0
    ): ByteArray = withContext(Dispatchers.IO){
        try {
            val maxResponseLength =
                healthCard.maxMessageLength - 2 // allow for 2 status bytes sw1, sw2
            val expectedResponseLength = expectedSize ?: 0x10000
            val responseLength = minOf(maxResponseLength, expectedResponseLength)

            val readFileCommand = Read.readFileCommand(responseLength, offset)
            Log.d(
                "SecureHealthCard",
                "readSelectedFileAsync readFileCommand: ${readFileCommand.bytes.toHexString()}"
            )

            val encryptedCmd = secureMessaging.encrypt(readFileCommand)
            Log.d("SecureHealthCard", "readSelectedFileAsync encryptedCmd: ${encryptedCmd}")
            Log.d(
                "SecureHealthCard",
                "readSelectedFileAsync encryptedCmd: ${encryptedCmd.bytes.toHexString()}"
            )

            val readFileResponse = HealthCardResponse.from(healthCard.transmitApdu(encryptedCmd))
            val readFileResponseStatus = readFileResponse.responseStatus

            if (readFileResponseStatus !is ResponseStatus.Success &&
                (failOnEndOfFileWarning || readFileResponseStatus !is ResponseStatus.EndOfFileWarning)) {
                throw HealthCardError.ReadErrorUnexpectedResponseException(readFileResponseStatus)
            }

            Log.d(
                "SecureHealthCard",
                "readSelectedFileAsync readFileResponse data without SW: ${readFileResponse.data.toHexString()}"
            )

            val decryptedReadFileResponse = HealthCardResponse.from(secureMessaging.decrypt(readFileResponse.apdu))
            decryptedReadFileResponse.expect(ResponseStatus.Success)

            Log.d(
                "SecureHealthCard",
                "readSelectedFileAsync decryptedReadFileResponse data without SW: ${decryptedReadFileResponse.data.toHexString()}"
            )
            val responseData = decryptedReadFileResponse.data

            if (responseData.isEmpty()) {
                throw HealthCardError.ReadErrorNoDataException(readFileResponseStatus)
            }
            val continueReading = responseData.size < expectedResponseLength &&
                    readFileResponseStatus != ResponseStatus.EndOfFileWarning

            if (continueReading) {
                var newExpectedSize: Int? = null
                if (expectedSize != null) {
                    newExpectedSize = expectedResponseLength - responseData.size
                }
                // Continue reading
                val continued = readSelectedFileAsync(newExpectedSize, failOnEndOfFileWarning, offset + responseData.size)

                return@withContext responseData + continued
            } else {
                // Done
                return@withContext responseData
            }
        } catch (e: Exception) {
            Log.e("SecureHealthCard","failed to read selected file. Error: ${e.message}")
            throw e
        }
    }

}
