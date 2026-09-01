package expo.modules.ehcnfc.readingresult

import expo.modules.ehcnfc.helpers.Command
import expo.modules.ehcnfc.helpers.CommandType
import java.util.Date
import java.util.UUID



enum class ReaderState {
    IDLE,
    LOADING,
    SUCCESS,
    FAILURE
}

sealed class ViewResult {
    data class Dictionary(val personalData: PersonalData) : ViewResult()
    data class Bool(val value: Boolean) : ViewResult()
}


data class ReadingResult(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Date = Date(),
    val result: Result<PersonalData>,
    val commands: List<Command>
) {
    fun formattedDescription(): String {
        val sb = StringBuilder()
        sb.append("# SMART CARD\n\n")
        sb.append("Date: $timestamp\n\n")

        sb.append("# RESULT\n\n")

        result.isFailure.let { error ->
            sb.append("Finished with error message: '${result.exceptionOrNull()}'\n")
            sb.append("error: $error\n")
        }

        result.isSuccess.let { success ->
            //when (success) {
                //is ViewResult.Dictionary -> {
                    val dict = result.getOrDefault(PersonalData.EMPTY_PD)
                    sb.append("Finished process with success: '")
                    sb.append(if (dict.isEmpty()) "No Data" else dict.toJson())
                    sb.append("'\n")
              //  }
               // is ViewResult.Bool -> {
                //    sb.append("Finished process with state loading: '${success.value}'\n")
                //}
           // }
        }

        sb.append("\n# COMMANDS\n\n")

        if (commands.isEmpty()) {
            sb.append("No commands between smart card and device have been sent!\n")
            return sb.toString()
        }

        for (command in commands) {
            when (command.type) {
                CommandType.SEND -> {
                    sb.append("SEND:\n${command.message}\n")
                }
                CommandType.SEND_SECURE_CHANNEL -> {
                    sb.append("SEND (secure channel, header only):\n${command.message}\n\n")
                }
                CommandType.RESPONSE -> {
                    sb.append("\nRESPONSE:\n${command.message}\n\n")
                }
                CommandType.RESPONSE_SECURE_CHANNEL -> {
                    sb.append("RESPONSE (secure channel):\n${command.message}\n\n")
                }
                CommandType.DESCRIPTION -> {
                    sb.append("\n\n*** ${command.message} ***\n\n")
                }
            }
        }
        return sb.toString()
    }
}