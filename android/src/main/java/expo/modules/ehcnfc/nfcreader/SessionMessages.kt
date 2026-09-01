package expo.modules.ehcnfc.nfcreader

data class SessionMessages(
    val discoveryMessage: String,
    val connectMessage: String,
    val secureChannelMessage: String,
    val noCardMessage: String,
    val multipleCardsMessage: String,
    val unsupportedCardMessage: String,
    val connectionErrorMessage: String,
    val wrongCanMessage: String,
    val unexpectedAnswerFromCardMessage: String,
    val readingErrorMessage: String,
    val cancelMessage: String,
    val readingMessage: String,
    val successMessage: String
)
