package expo.modules.ehcnfc.cardaccess

import android.nfc.tech.IsoDep
import android.util.Log
import expo.modules.ehcnfc.apdu.ApduConstants
import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.apdu.ResponseStatus
import expo.modules.ehcnfc.cardobjects.CardAid
import expo.modules.ehcnfc.healthcard.CardVersion2
import expo.modules.ehcnfc.healthcard.HealthCardPropertyType
import expo.modules.ehcnfc.healthcard.HealthCardResponse
import expo.modules.ehcnfc.healthcardcommand.Read
import expo.modules.ehcnfc.healthcardcommand.Select
import expo.modules.ehcnfc.helpers.prettyPrint
import expo.modules.ehcnfc.helpers.toHexString
import expo.modules.ehcnfc.pace.PACEObjectIdentifier
import java.io.IOException
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
/**
 * Utility class to access and parse EF structures from the Health Card.
 *
 * Handles:
 * - Selecting the Master File (MF) and determining the card's AID.
 * - Reading EF.CardAccess to determine supported key agreement algorithms (PACE).
 * - Reading EF.Version2 to identify the card type.
 *
 * @param isoDep a connected [IsoDep] NFC channel to the card.
 */
class CardAccessV2(private val isoDep: IsoDep) {

    /**
     * Determines the `CardAid` of the card by
     * selecting the MF.root application and requesting its application identifier.
     *
     * @return the parsed [CardAid], or `null` if it cannot be determined.
     * @throws IOException if communication with the card fails.
     * @throws IllegalStateException if response parsing fails.
     */
    @Throws(IOException::class)
    fun determineCardAid(): CardAid? {
        val selectMfCommand = Select.selectRootRequestingFcp(ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT)
        val response = HealthCardResponse.from(ApduResponse(isoDep.transceive(selectMfCommand.bytes)))
        response.expect(ResponseStatus.Success)

        // Strip SW1/SW2
        val fcpData = response.data
        val fcp = FileControlParameterV2.parse(fcpData) ?: run {
            Log.e("CardAccess", "FCP parsing returned null")
            return null
        }

        val aidData = fcp.applicationIdentifier
            ?: throw IllegalStateException("SELECT MF returned no AID")

        return CardAid.fromBytesOrNull(aidData.rawValue)
            ?: throw IllegalStateException("Unsupported AID: ${aidData.rawValue.toHexString()}")
    }

    fun printFields(fcp: FileControlParameter) {
        Log.d("CardAccess", "status = ${fcp.status}")
        Log.d("CardAccess", "size = ${fcp.size}")
        Log.d("CardAccess", "readSize = ${fcp.readSize}")
        Log.d("CardAccess", "fileDescriptor = ${fcp.fileDescriptor}")
        Log.d("CardAccess", "fileIdentifier = ${fcp.fileIdentifier}")
        Log.d("CardAccess", "applicationIdentifier = ${fcp.applicationIdentifier}")
        Log.d("CardAccess", "shortFileIdentifier = ${fcp.shortFileIdentifier}")
    }
    /**
     * Reads EF.CardAccess and initializes PACE information.
     *
     * @param cardAid the [CardAid] of the card.
     * @return the raw EF.CardAccess file contents, or `null` if unsupported algorithm.
     * @throws IOException if transceive fails.
     * @throws Exception if response parsing fails.
     */
    fun readKeyAgreementAlgorithm(cardAid: CardAid?): ByteArray? {
        if (cardAid == null) {
            throw Exception("readKeyAgreementAlgorithm failed: cardAid is null")
        }
        // 1. SELECT the application (by CardAID)
        val selectCommand = Select.selectFile(aid=cardAid.aid)
        val selectResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(selectCommand.bytes)))
        selectResponse.expect(ResponseStatus.Success)

        // 2. READ EF.CardAccess which contains asn1 sequence of security info, e.g.
        // SecurityInfo ::= CHOICE {
        //    PACEInfo                [0] SEQUENCE { protocol OBJECT IDENTIFIER, version INTEGER, parameterId INTEGER OPTIONAL },
        //    ChipAuthenticationInfo  [1] ...
        //    TerminalAuthenticationInfo [2] ...
        //    ...
        //}
        val readCommand = Read.readFileCommand(
            sfid= cardAid.efCardAccess,
            ne= ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT
        )
        val readResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(readCommand.bytes)))
        readResponse.expect(ResponseStatus.Success)

        // 4. Extract ASN.1 structure
        val efCardAccess = readResponse.data
        val asn1Set = parseResponseToAsn1Set(efCardAccess)
        val protocolOid = extractProtocolOid(asn1Set)

        // 5. Check if the OID is one of the known PACE algorithms
        // only return if it matches known PACE algorithm
        return if (protocolOid.getId()== PACEObjectIdentifier.id_PACE_DH_GM_AES_CBC_CMAC_128) {
            efCardAccess
        } else null
    }
    /**
     * Reads EF.Version2 to determine the [HealthCardPropertyType].
     *
     * @param cardAid the [CardAid] of the card.
     * @return the resolved [HealthCardPropertyType].
     * @throws IOException if transceive fails.
     */
    fun readCardType(cardAid: CardAid?): HealthCardPropertyType{
        if (cardAid == null) {
            throw Exception("readKeyAgreementAlgorithm failed: cardAid is null")
        }
        // 1. SELECT the application (by CardAID)
        val selectCommand = Select.selectFile(aid=cardAid.aid)
        val selectResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(selectCommand.bytes)))
        selectResponse.expect(ResponseStatus.Success)

        // 2. READ EF.Version2
        val readCommand = Read.readFileCommand(
            sfid= cardAid.efVersion2Sfi,
            ne= ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT
        )
        val readResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(readCommand.bytes)))
        readResponse.expect(ResponseStatus.Success)

        val cardVersion2 = CardVersion2.fromAsn1(readResponse.data)
        val healthCardPropertyType = HealthCardPropertyType.fromCardAid(cardAid, cardVersion2)

        return healthCardPropertyType
    }

    // ---------- Internal helpers ----------

    /**
     * Parses the response data to and asn1 set that holds a sequence of security infos
     *
     * @param responseData the asn1Data that should be parsed
     * @throws Asn1ParseException if the ASN.1 structure could not be decoded correctly
     * @returns parsed ASN1Set
     */
    @Throws(Asn1ParseException::class)
    private fun parseResponseToAsn1Set(responseData: ByteArray): ASN1Set {
        val asn1: ASN1Primitive = try {
            ASN1Primitive.fromByteArray(responseData)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to decode ASN.1 data from card", e)
        }

        Log.d("CardAccess", "asn1Doc.prettyPrint(), ${prettyPrint(asn1)}")

        if (asn1 !is ASN1Set) {
            throw IllegalArgumentException("Expected ASN1Set but got ${asn1::class.java.simpleName}")
        }

        return asn1
    }



    /**
     * Extracts the protocol OID from an EF.CardAccess TLV blob.
     *
     * Expected structure:
     *   SET {
     *     SEQUENCE {
     *       OBJECT IDENTIFIER
     *       ...
     *     }
     *   }
     */
    @Throws(IllegalArgumentException::class)
    private fun extractProtocolOid(asn1Set: ASN1Set): ASN1ObjectIdentifier {
        // 1. Get first element of the Set -> Sequence
        val seq = asn1Set.getObjectAt(0) as? ASN1Sequence
            ?: throw IllegalArgumentException("Expected SEQUENCE inside SET")

        // 2. Get first element of the Sequence -> ObjectIdentifier
        val oid = seq.getObjectAt(0) as? ASN1ObjectIdentifier
            ?: throw IllegalArgumentException("Expected OBJECT IDENTIFIER inside SEQUENCE")

        return oid
    }
}






