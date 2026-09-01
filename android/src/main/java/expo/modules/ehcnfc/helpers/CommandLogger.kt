package expo.modules.ehcnfc.helpers

// Command type
enum class CommandType {
    SEND,
    SEND_SECURE_CHANNEL,
    RESPONSE,
    RESPONSE_SECURE_CHANNEL,
    DESCRIPTION
}

data class Command(
    val type: CommandType,
    val message: String
)


// Singleton-style logger for commands
object CommandLogger {
    val commands: MutableList<Command> = mutableListOf()

    fun log(message: String, type: CommandType) {
        val command = Command(message = message, type = type)
        commands.add(command)
    }

    fun clear() {
        commands.clear()
    }
}