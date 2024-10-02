//
//  ReadingResult.swift
//
//  Created by Priska Kohnen on 18.09.24.
//

import Foundation
import Helper


extension Dictionary{
  
  func toJSON() -> String {
    if let jsonData = try? JSONSerialization.data(withJSONObject: self, options: .prettyPrinted),
       let jsonString = String(data: jsonData, encoding: .utf8) {
      return jsonString
    }
    return ""
  }
}

enum ViewResult {
  case dictionary(PersonalData)
  case bool(Bool)
}

struct ReadingResult: Identifiable {
  let id: UUID // swiftlint:disable:this identifier_name
  let timestamp: Date
  let result: ViewState<ViewResult, Error>
  let commands: [Command]
  
  init(
    identifier: UUID = UUID(),
    timestamp: Date = Date(),
    result: ViewState<ViewResult, Error>,
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
      switch success {
      case .dictionary(let dict):
        description += "Finished process with success: '\(dict.isEmpty() ? "No Data" : dict.toDictionary().toJSON())'\n"
      case .bool(let boolValue):
        description += "Finished process with state loading: '\(boolValue)'\n"
      }
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
