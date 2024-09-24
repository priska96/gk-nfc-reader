

import Foundation
import Helper

struct ReadingResult: Identifiable {
    let id: UUID // swiftlint:disable:this identifier_name
    let timestamp: Date
    let result: ViewState<Bool, Error>
    let commands: [Command]

    init(
        identifier: UUID = UUID(),
        timestamp: Date = Date(),
        result: ViewState<Bool, Error>,
        commands: [Command]
    ) {
        id = identifier
        self.timestamp = timestamp
        self.result = result
        self.commands = commands
    }

    func formattedDescription() -> String {
        var description = "# SMART CARD\n\n"

        description += "Date: \(timestamp.description)\n"

        description += "\n# RESULT\n\n"

        if let error = result.error {
            description += "Finished with error message: '\(error.localizedDescription)'\n"
            description += "error: \(error)\n"
        }

        if let success = result.value {
            description += "Finished process with success: '\(success == true ? "true" : "false")'\n"
        }

        description += "\n# COMMANDS\n\n"

        guard !commands.isEmpty else {
            description += "No commands between smart card and device have been sent!\n"
            return description
        }

        for command in commands {
            switch command.type {
            case .send:
                description += "SEND:\n"
                description += "\(command.message)\n"
            case .sendSecureChannel:
                description += "SEND (secure channel, header only):\n"
                description += "\(command.message)\n\n"
            case .response:
                description += "\nRESPONSE:\n"
                description += "\(command.message)\n\n"
            case .responseSecureChannel:
                description += "RESPONSE (secure channel):\n"
                description += "\(command.message)\n\n"
            case .description:
                description += "\n\n*** \(command.message) ***\n\n"
            default: break
            }
        }
        return description
    }
}
