package expo.modules.ehcnfc.cardaccess

import android.nfc.tech.IsoDep
import android.util.Log
import at.asitplus.signum.indispensable.asn1.Asn1Element
import at.asitplus.signum.indispensable.asn1.Asn1Sequence
import at.asitplus.signum.indispensable.asn1.Asn1Set
import at.asitplus.signum.indispensable.asn1.Asn1Primitive
import at.asitplus.signum.indispensable.asn1.encoding.parse
import at.asitplus.signum.indispensable.asn1.ObjectIdentifier
import at.asitplus.signum.indispensable.asn1.KnownOIDs
import expo.modules.ehcnfc.apdu.ApduConstants
import expo.modules.ehcnfc.apdu.ApduException
import expo.modules.ehcnfc.apdu.ApduResponse
import expo.modules.ehcnfc.apdu.ResponseStatus
import expo.modules.ehcnfc.cardobjects.CardAid
import expo.modules.ehcnfc.healthcard.CardVersion2
import expo.modules.ehcnfc.healthcard.HealthCardPropertyType
import expo.modules.ehcnfc.healthcard.HealthCardResponse
import expo.modules.ehcnfc.healthcardcommand.Read
import expo.modules.ehcnfc.healthcardcommand.Select
import expo.modules.ehcnfc.helpers.CommandLogger
import expo.modules.ehcnfc.helpers.CommandType
import expo.modules.ehcnfc.helpers.prettyPrint
import expo.modules.ehcnfc.helpers.toHexString
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
class CardAccess(private val isoDep: IsoDep) {

    /**
     * Determines the `CardAid` of the card by
     * selecting the MF.root application and requesting its application identifier.
     *
     * @return the parsed [CardAid], or `null` if it cannot be determined.
     * @throws [IOException] if communication with the card fails.
     * @throws [IllegalStateException] if response parsing fails.
     */
    @Throws(IOException::class, IllegalStateException::class)
    suspend fun determineCardAidAsync(): CardAid = withContext(Dispatchers.IO) {
        val selectMfCommand = Select.selectRootRequestingFcp(ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT)
        CommandLogger.log(selectMfCommand.bytes.toHexString(), CommandType.SEND)

        val response = HealthCardResponse.from(ApduResponse(isoDep.transceive(selectMfCommand.bytes)))
        CommandLogger.log(response.apdu.bytes.toHexString(), CommandType.RESPONSE)
        response.expect(ResponseStatus.Success)

        val fcpData = response.data
        val fcp = FileControlParameter.parse(fcpData)
            ?: throw IllegalStateException("FCP parsing returned null")

        val aidData = fcp.applicationIdentifier
            ?: throw IllegalStateException("SELECT MF returned no AID")

        CardAid.fromBytesOrNull(aidData.rawValue)
            ?: throw IllegalStateException("Unsupported AID: ${aidData.rawValue.toHexString()}")
    }

    /**
     * Reads EF.CardAccess returns it if algorithm used on card is pace
     *
     * @param cardAid the [CardAid] of the card.
     * @return the raw EF.CardAccess file contents, or `null` if unsupported algorithm.
     * @throws [ApduException] if response parsing fails.
     * @throws [Asn1ParseException] if asn1 parsing fails.
     * @throws [IllegalStateException] if no cardAid provided.
     * @throws [IOException] if transceive fails.
     */
    @Throws(ApduException::class, Asn1ParseException::class, IllegalStateException::class, IOException::class)
    suspend fun readKeyAgreementAlgorithmAsync(cardAid: CardAid?): ByteArray? = withContext(Dispatchers.IO) {
        cardAid  ?: throw IllegalStateException("No card AID provided")

        // 1. SELECT the application (by CardAID)
        val selectCommand = Select.selectFile(aid=cardAid.aid)
        CommandLogger.log(selectCommand.bytes.toHexString(), CommandType.SEND)

        val selectResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(selectCommand.bytes)))
        CommandLogger.log(selectResponse.apdu.bytes.toHexString(), CommandType.RESPONSE)
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
        CommandLogger.log(readCommand.bytes.toHexString(), CommandType.SEND)

        val readResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(readCommand.bytes)))
        CommandLogger.log(readResponse.apdu.bytes.toHexString(), CommandType.RESPONSE)
        readResponse.expect(ResponseStatus.Success)

        // 4. Extract ASN.1 structure
        val efCardAccess = readResponse.data
        val asn1Set = parseResponseToAsn1Set(efCardAccess)
        val protocolOid = extractProtocolOid(asn1Set)

        // 5. Check if the OID is one of the known PACE algorithms
        // only return if it matches known PACE algorithm
        if(protocolOid == KnownOIDs.bsiPACE_ECDH_GM_AES_CBC_CMAC_128) {
            efCardAccess
        } else null
    }

    /**
     * Reads EF.Version2 to determine the [HealthCardPropertyType].
     *
     * @param cardAid the [CardAid] of the card.
     * @return the resolved [HealthCardPropertyType].
     * @throws [ApduException] if response parsing fails.
     * @throws [IllegalArgumentException] if cardType can't be generated.
     * @throws [IllegalStateException] if no cardAid provided.
     * @throws [IOException] if transceive fails.
     */
    @Throws(ApduException::class, IllegalStateException::class, IOException::class, IllegalArgumentException::class)
    suspend fun readCardTypeAsync(cardAid: CardAid?): HealthCardPropertyType = withContext(Dispatchers.IO){
        cardAid  ?: throw IllegalStateException("No card AID provided")

        // 1. SELECT the application (by CardAID)
        val selectCommand = Select.selectFile(aid=cardAid.aid)
        CommandLogger.log(selectCommand.bytes.toHexString(), CommandType.SEND)
        val selectResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(selectCommand.bytes)))
        CommandLogger.log(selectResponse.apdu.bytes.toHexString(), CommandType.RESPONSE)
        selectResponse.expect(ResponseStatus.Success)


        // 2. READ EF.Version2
        val readCommand = Read.readFileCommand(
            sfid= cardAid.efVersion2Sfi,
            ne= ApduConstants.EXPECTED_LENGTH_WILDCARD_SHORT
        )
        CommandLogger.log(readCommand.bytes.toHexString(), CommandType.SEND)
        val readResponse = HealthCardResponse.from(ApduResponse(isoDep.transceive(readCommand.bytes)))
        CommandLogger.log(readResponse.apdu.bytes.toHexString(), CommandType.RESPONSE)
        readResponse.expect(ResponseStatus.Success)

        val cardVersion2 = CardVersion2.fromAsn1(readResponse.data)
        val healthCardPropertyType = HealthCardPropertyType.fromCardAid(cardAid, cardVersion2)

        healthCardPropertyType
    }

    // ---------- Internal helpers ----------

    /**
     * Parses the response data to and asn1 set that holds a sequence of security infos
     *
     * @param responseData the asn1Data that should be parsed
     * @throws [Asn1ParseException] if the ASN.1 structure could not be decoded correctly
     * @returns parsed ASN1Set
     */
    @Throws(Asn1ParseException::class)
    private fun parseResponseToAsn1Set(responseData: ByteArray): Asn1Set{
        val asn1Doc = try { // this one is a SET holding a SEQUENCE
            Asn1Element.parse(responseData) // should return base ASN1Element
        } catch (e: Exception) {
            throw Asn1ParseException(
                "ASN1Decoder did returned unexpectedFormedAnswerFromCard",
                e
            )
        }

        Log.d("CardAccess", "asn1Doc.prettyPrint(), ${asn1Doc.prettyPrint()}")

        val asn1Set = asn1Doc.asSet()
        return asn1Set
    }

    /**
     * Extracts the protocol OID from an EF.CardAccess TLV blob.
     *
     * @param asn1Set the asn1Set that contains the POID
     * @return the protocol object identifier
     * @throws [IllegalArgumentException] if wrong ASN.1 element was found (is not the expected SET/SEQ/OID).
     */
    @Throws(IllegalArgumentException::class)
    private fun extractProtocolOid(asn1Set: Asn1Set): ObjectIdentifier {
        // 1. Get first element of the Set -> Sequence
        val asn1FirstSet = asn1Set.children.firstOrNull() as? Asn1Sequence
            ?: throw IllegalArgumentException("Expected Sequence inside Set")

        // 2. Get first element of the Sequence -> OID
        val asn1Oid = asn1FirstSet.children.firstOrNull() as? Asn1Primitive
            ?: throw IllegalArgumentException("Expected Primitive inside Sequence")

        val protocolOid = ObjectIdentifier.doDecode(asn1Oid)

        return protocolOid
    }

}






