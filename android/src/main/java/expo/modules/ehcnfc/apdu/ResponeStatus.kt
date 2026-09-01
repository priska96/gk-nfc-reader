package expo.modules.ehcnfc.apdu

/**
 * Represents APDU response status words (SW1SW2).
 * Covers known values (from gemSpec & ISO 7816) and falls back to [Unknown].
 */
sealed class ResponseStatus(val code: Int) {

    // --- Success & General ---
    object Success : ResponseStatus(0x9000)
    object UnknownException : ResponseStatus(0x6F00)
    object UnknownStatus : ResponseStatus(0x0000)

    // --- Warnings (62xx / 63xx) ---
    object DataTruncated : ResponseStatus(0x6200)
    object CorruptDataWarning : ResponseStatus(0x6281)
    object EndOfFileWarning : ResponseStatus(0x6282)
    object EndOfRecordWarning : ResponseStatus(0x6282)
    object UnsuccessfulSearch : ResponseStatus(0x6282)
    object FileDeactivated : ResponseStatus(0x6282)
    object FileTerminated : ResponseStatus(0x6283)
    object RecordDeactivated : ResponseStatus(0x6287)
    object TransportStatusTransportPin : ResponseStatus(0x62C1)
    object TransportStatusEmptyPin : ResponseStatus(0x62C7)
    object PasswordDisabled : ResponseStatus(0x62D0)

    object AuthenticationFailure : ResponseStatus(0x6300)
    object NoAuthentication : ResponseStatus(0x63CF)

    // Retry counters (grouped)
    object RetryCounter00 : ResponseStatus(0x63C0)
    object RetryCounter01 : ResponseStatus(0x63C1)
    object RetryCounter02 : ResponseStatus(0x63C2)
    object RetryCounter03 : ResponseStatus(0x63C3)
    object RetryCounter04 : ResponseStatus(0x63C4)
    object RetryCounter05 : ResponseStatus(0x63C5)
    object RetryCounter06 : ResponseStatus(0x63C6)
    object RetryCounter07 : ResponseStatus(0x63C7)
    object RetryCounter08 : ResponseStatus(0x63C8)
    object RetryCounter09 : ResponseStatus(0x63C9)
    object RetryCounter10 : ResponseStatus(0x63CA)
    object RetryCounter11 : ResponseStatus(0x63CB)
    object RetryCounter12 : ResponseStatus(0x63CC)
    object RetryCounter13 : ResponseStatus(0x63CD)
    object RetryCounter14 : ResponseStatus(0x63CE)
    object RetryCounter15 : ResponseStatus(0x63CF)

    // Update retry warnings (reuse same codes)
    // Wrong secret warnings (reuse same codes)

    // --- Execution / Memory errors ---
    object EncipherError : ResponseStatus(0x6400)
    object KeyInvalid : ResponseStatus(0x6400)
    object ObjectTerminated : ResponseStatus(0x6400)
    object ParameterMismatch : ResponseStatus(0x6400)
    object MemoryFailure : ResponseStatus(0x6581)

    // --- Wrong length / Channel issues ---
    object WrongRecordLength : ResponseStatus(0x6700)
    object ChannelClosed : ResponseStatus(0x6881)
    object NoMoreChannelsAvailable : ResponseStatus(0x6981)
    object VolatileKeyWithoutLcs : ResponseStatus(0x6981)
    object WrongFileType : ResponseStatus(0x6981)

    // --- Security / Key errors ---
    object SecurityStatusNotSatisfied : ResponseStatus(0x6982)
    object CommandBlocked : ResponseStatus(0x6983)
    object KeyExpired : ResponseStatus(0x6983)
    object PasswordBlocked : ResponseStatus(0x6983)
    object KeyAlreadyPresent : ResponseStatus(0x6985)
    object NoKeyReference : ResponseStatus(0x6985)
    object NoPrkReference : ResponseStatus(0x6985)
    object NoPukReference : ResponseStatus(0x6985)
    object NoRandom : ResponseStatus(0x6985)
    object NoRecordLifeCycleStatus : ResponseStatus(0x6985)
    object PasswordNotUsable : ResponseStatus(0x6985)
    object WrongRandomLength : ResponseStatus(0x6985)
    object WrongRandomOrNoKeyReference : ResponseStatus(0x6985)
    object WrongPasswordLength : ResponseStatus(0x6985)
    object NoCurrentEf : ResponseStatus(0x6986)
    object IncorrectSmDo : ResponseStatus(0x6988)

    // --- File & record issues ---
    object NewFileSizeWrong : ResponseStatus(0x6A80)
    object NumberPreconditionWrong : ResponseStatus(0x6A80)
    object NumberScenarioWrong : ResponseStatus(0x6A80)
    object VerificationError : ResponseStatus(0x6A80)
    object WrongCipherText : ResponseStatus(0x6A80)
    object WrongToken : ResponseStatus(0x6A80)

    object UnsupportedFunction : ResponseStatus(0x6A81)
    object FileNotFound : ResponseStatus(0x6A82)
    object RecordNotFound : ResponseStatus(0x6A83)
    object DataTooBig : ResponseStatus(0x6A84)
    object FullRecordList : ResponseStatus(0x6A84)
    object MessageTooLong : ResponseStatus(0x6A84)
    object OutOfMemory : ResponseStatus(0x6A84)
    object FullRecordListOrOutOfMemory : ResponseStatus(0x6A84)

    object InconsistentKeyReference : ResponseStatus(0x6A88)
    object WrongKeyReference : ResponseStatus(0x6A88)
    object KeyNotFound : ResponseStatus(0x6A88)
    object KeyOrPrkNotFound : ResponseStatus(0x6A88)
    object KeyOrPwdNotFound : ResponseStatus(0x6A88)
    object PasswordNotFound : ResponseStatus(0x6A88)
    object PrkNotFound : ResponseStatus(0x6A88)
    object PukNotFound : ResponseStatus(0x6A88)

    object DuplicatedObjects : ResponseStatus(0x6A89)
    object DfNameExists : ResponseStatus(0x6A8A)

    object OffsetTooBig : ResponseStatus(0x6B00)

    object InstructionNotSupported : ResponseStatus(0x6D00)

    object CustomError : ResponseStatus(0x0000)

    // --- Fallback for unmapped codes ---
    class Unknown(sw: Int) : ResponseStatus(sw) {
        override fun toString(): String = "Unknown(0x${code.toString(16)})"
    }

    companion object {
        /**
         * Parse a status word from SW1 + SW2 bytes.
         */
        fun from(sw1: Byte, sw2: Byte): ResponseStatus {
            val sw = ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
            return when (sw) {
                0x9000 -> Success
                0x6F00 -> UnknownException
                0x0000 -> UnknownStatus
                0x6200 -> DataTruncated
                0x6281 -> CorruptDataWarning
                0x6282 -> EndOfFileWarning
                0x6283 -> FileTerminated
                0x6287 -> RecordDeactivated
                0x62C1 -> TransportStatusTransportPin
                0x62C7 -> TransportStatusEmptyPin
                0x62D0 -> PasswordDisabled
                0x6300 -> AuthenticationFailure
                0x63C0 -> RetryCounter00
                0x63C1 -> RetryCounter01
                0x63C2 -> RetryCounter02
                0x63C3 -> RetryCounter03
                0x63C4 -> RetryCounter04
                0x63C5 -> RetryCounter05
                0x63C6 -> RetryCounter06
                0x63C7 -> RetryCounter07
                0x63C8 -> RetryCounter08
                0x63C9 -> RetryCounter09
                0x63CA -> RetryCounter10
                0x63CB -> RetryCounter11
                0x63CC -> RetryCounter12
                0x63CD -> RetryCounter13
                0x63CE -> RetryCounter14
                0x63CF -> RetryCounter15
                0x6400 -> EncipherError
                0x6581 -> MemoryFailure
                0x6700 -> WrongRecordLength
                0x6881 -> ChannelClosed
                0x6981 -> NoMoreChannelsAvailable
                0x6982 -> SecurityStatusNotSatisfied
                0x6983 -> CommandBlocked
                0x6985 -> KeyAlreadyPresent
                0x6986 -> NoCurrentEf
                0x6988 -> IncorrectSmDo
                0x6A80 -> NewFileSizeWrong
                0x6A81 -> UnsupportedFunction
                0x6A82 -> FileNotFound
                0x6A83 -> RecordNotFound
                0x6A84 -> DataTooBig
                0x6A88 -> InconsistentKeyReference
                0x6A89 -> DuplicatedObjects
                0x6A8A -> DfNameExists
                0x6B00 -> OffsetTooBig
                0x6D00 -> InstructionNotSupported
                else -> Unknown(sw)
            }
        }
    }

    override fun toString(): String = this::class.simpleName ?: "ResponseStatus"
}
