package expo.modules.ehcnfc.pace

/**
 * See BSI-TR-03110, version 2.10
 *
 */
object EACObjectIdentifier {
    /**
     * bsi-de OBJECT IDENTIFIER ::= {itu-t(0) identified-organization(4)
     * etsi(0) reserved(127) etsi-identified-organization(0) 7}
     */
    const val BSI_DE: String = "0.4.0.127.0.7"

    /**
     * id-PK OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 1}
     */
    const val ID_PK: String = "$BSI_DE.2.2.1"

    /**
     * id-TA OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 2}
     */
    const val ID_TA: String = "$BSI_DE.2.2.2"

    /**
     * id-CA OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 3}
     */
    const val ID_CA: String = "$BSI_DE.2.2.3"

    /**
     * id-PACE OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 4}
     */
    const val ID_PACE: String = "$BSI_DE.2.2.4"

    /**
     * Restricted Identification.
     * id-RI OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 5}
     */
    const val ID_RI: String = "$BSI_DE.2.2.5"

    /**
     * CardInfoLocator.
     * id-CI OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 6}
     */
    const val ID_CI: String = "$BSI_DE.2.2.6"

    /**
     * eIDSecurityInfo.
     * id-eIDSecurity OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 7}
     */
    const val ID_EID_SECURITY: String = "$BSI_DE.2.2.7"

    /**
     * PrivilegedTerminalInfo.
     * id-CI OBJECT IDENTIFIER ::= {bsi-de protocols(2) smartcard(2) 8}
     */
    const val ID_PT: String = "$BSI_DE.2.2.8"

    /**
     * Authenticated Auxiliary Data. BSI-TR-03110 section A.6.5.1.
     * id-AuxiliaryData OBJECT IDENTIFIER ::= {bsi-de applications(3) mrtd(1) 4}
     */
    const val ID_AUXILIARY_DATA: String = "$BSI_DE.3.1.4"

    /**
     * Age Verification. BSI-TR-03110 section A.6.5.2.
     * id-DateOfBirth OBJECT IDENTIFIER ::= {id-AuxiliaryData 1}
     */
    const val ID_DATE_OF_BIRTH: String = "$ID_AUXILIARY_DATA.1"

    /**
     * Document Validity Verification. BSI-TR-03110 section A.6.5.3.
     * id-DateOfExpiry OBJECT IDENTIFIER ::= {id-AuxiliaryData 2}
     */
    const val ID_DATE_OF_EXPIRY: String = "$ID_AUXILIARY_DATA.2"

    /**
     * Community ID Verification. BSI-TR-03110 section A.6.5.4.
     * id-CommunityID OBJECT IDENTIFIER ::= {id-AuxiliaryData 3}
     */
    const val ID_COMMUNITY_ID: String = "$ID_AUXILIARY_DATA.3"

    /**
     * Signature Format. BSI-TR-03110 section A.1.2.5.
     * id-SecurityObject OBJECT IDENTIFIER ::= {bsi-de applications(3) eID(2) 1}
     */
    const val ID_SECURITY_OBJECT: String = "$BSI_DE.3.2.1"

    /**
     * Standardized Domain Parameters. BSI-TR-03110 section A.2.1.1.
     * standardizedDomainParameters OBJECT IDENTIFIER ::= {bsi-de algorithms(1) 2}
     */
    const val STANDARDIZED_DOMAIN_PARAMETERS: String = "$BSI_DE.1.2"
}