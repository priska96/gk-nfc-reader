package expo.modules.ehcnfc.keyagreement

// Exceptions corresponding to Swift errors
sealed class KeyAgreementError(message: String) : Exception(message) {

    /**
     * Indicates that one of the input parameters was invalid.
     * @param parameterName The name of the parameter that was illegal.
     * @param details Optional additional info about why it was invalid.
     */
    class IllegalArgument(
        val parameterName: String,
        details: String? = null
    ) : KeyAgreementError(
        "Illegal argument: $parameterName${details?.let { " ($it)" } ?: ""}"
    )

    class Asn1ParseException(message: String, cause: Throwable?) : Exception(message, cause)

    class CreateASN1ObjectException(message: String, cause: Throwable?) : Exception(message, cause)

    class PACEApduCommandException(message: String, cause: Throwable?) : Exception(message, cause)

    class DecryptionException(message: String, cause: Throwable?) : Exception(message, cause)

    object UnexpectedFormedAnswerFromCard :
        KeyAgreementError("Unexpected answer from card")

    object ResultOfEcArithmeticWasInfinite :
        KeyAgreementError("Result of EC arithmetic was infinite")

    object MacPcdVerificationFailedOnCard :
        KeyAgreementError("MAC PCD verification failed on card")

    object MacPiccVerificationFailedLocally :
        KeyAgreementError("MAC PICC verification failed locally")

    object NoValidHealthCardStatus :
        KeyAgreementError("No valid health card status")

    object EfCardAccessNotAvailable :
        KeyAgreementError("EF.CardAccess not available")

    class UnsupportedKeyAgreementAlgorithm(val oid: String) :
        KeyAgreementError("Unsupported key agreement algorithm: $oid")
}
