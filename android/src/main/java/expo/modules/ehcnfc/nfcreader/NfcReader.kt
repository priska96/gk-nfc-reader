package expo.modules.ehcnfc.nfcreader

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.util.Log
import expo.modules.ehcnfc.apdu.ApduException
import expo.modules.ehcnfc.cardaccess.Asn1ParseException
import expo.modules.ehcnfc.session.SecureNFCSession
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.helpers.zlibDecompress
import expo.modules.ehcnfc.keyagreement.KeyAgreementError
import expo.modules.ehcnfc.readingresult.PersonalData
import expo.modules.ehcnfc.xmlparser.XMLDataExtractor

import java.nio.charset.Charset

class NfcReader(private var currentOptions: Options,
                private val messages: SessionMessages) {

    suspend fun readIsoDepTag(tag: Tag, onStatus: (String) -> Unit): PersonalData {

        onStatus(messages.connectMessage)
        val isoDep = IsoDep.get(tag)
            ?: throw IllegalArgumentException("Tag is not ISO-DEP (ISO7816)")

        val session = SecureNFCSession(isoDep, currentOptions.can)
        try {
            onStatus(messages.secureChannelMessage)
            // Step 1: Establish secure session
            val paceKey = session.openSecureNFCSessionAsync()

            //Step 2: Initialize SecureHealthCard
            val secureCard = session.initializeSecureHealthCard(paceKey)

            onStatus(messages.readingMessage)
            // Step 3: Perform secure operation
            val personalDataRaw = secureCard.selectAndReadHcaPD()

            Log.d("NfcReader", "PersonalDataRaw: ${personalDataRaw.toHexString()}")

            val personalDataDict = parseData(personalDataRaw)
            onStatus(messages.successMessage)
            Log.d("NfcReader", "PersonalData: ${personalDataDict}")
            return PersonalData.fromMap(personalDataDict)

        } catch(e: ApduException){
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            throw Exception(messages.connectionErrorMessage)
        }  catch(e: KeyAgreementError){
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            val distinguishedErrMsg = when (e) {
                is KeyAgreementError.MacPcdVerificationFailedOnCard,
                is KeyAgreementError.MacPiccVerificationFailedLocally -> {
                    messages.wrongCanMessage
                }

                is KeyAgreementError.UnexpectedFormedAnswerFromCard -> {
                    messages.unexpectedAnswerFromCardMessage
                }

                else -> {
                    messages.connectionErrorMessage
                }
            }
            throw Exception(distinguishedErrMsg)
        } catch(e: Asn1ParseException){
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            throw Exception(messages.connectionErrorMessage)
        } catch(e: TagLostException) {
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            throw Exception(messages.connectionErrorMessage)
        } catch(e: IllegalStateException){
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            throw Exception(messages.unsupportedCardMessage)
        } catch(e: IllegalArgumentException){
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            throw Exception(messages.unsupportedCardMessage)
        }   catch(e: Exception){
            Log.e("NfcReader", "readIsoDepTag: ${e.message}")
            throw Exception(messages.readingErrorMessage)
        }  finally {
            session.close()
        }
    }

    fun parseData(dataRaw: ByteArray?) : Map<String,String>?{
        if(dataRaw == null){
            return null
        }
        // remove 2 bytes indicating length
        val data = dataRaw.copyOfRange(2,dataRaw.size)

        // uncompress and decode
        try {
            val uncompressedData = data.zlibDecompress()//decompressZlib(data)
            try {
                // Decode to ISO-8859-1 (Latin1)
                val xmlString = uncompressedData.toString(Charset.forName("ISO-8859-1"))
                Log.d("NfcReader", "parseData Read XML OutputData of MF/DF.HCA/EF.hcaPD:${xmlString}")
                try {
                    val extractor = XMLDataExtractor()
                    val parsedData = extractor.parse(xmlString)
                    val dateString = parsedData["Geburtsdatum"]
                    if(dateString?.length == 8){
                        val formattedDate = "${dateString.substring(0, 4)}-" +
                                "${dateString.substring(4, 6)}-" +
                                dateString.substring(6, 8)
                        parsedData["Geburtsdatum"] = formattedDate
                    }
                        Log.d("NfcReader", "Extracted Data: ${parsedData}")
                        return parsedData
                    }catch(e:Exception){
                    Log.e("NfcReader", "ParseResponseDataError.xmlParserError(error): ${e.message}")
                }

                } catch(e:Exception){
                Log.e("NfcReader", "ParseResponseDataError.convertToXMLString: ${e.message}")
            }

            } catch(e:Exception){
            Log.e("NfcReader", "ParseResponseDataError.zLibError(error): ${e.message}")

        }catch(e:Exception){
            Log.e("NfcReader", "parseData error: ${e.message}")
        }
        return mapOf("" to "")
    }
}