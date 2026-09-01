// swiftlint:disable:this file_name
//
//  Copyright (c) 2023 gematik GmbH
//
//  Licensed under the Apache License, Version 2.0 (the License);
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an 'AS IS' BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

import CardReaderProviderApi
import Foundation

extension CardError: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .securityError(error):
            return String(format: NSLocalizedString("card_error_security", comment: ""), error?.localizedDescription ?? "nil")
        case let .connectionError(error):
            return String(format: NSLocalizedString("card_error_connection", comment: ""), error?.localizedDescription ?? "nil")
        case let .illegalState(error):
            return String(format: NSLocalizedString("card_error_illegal_state", comment: ""), error?.localizedDescription ?? "nil")
        case let .objcError(exception):
            return String(format: NSLocalizedString("card_error_objc", comment: ""), exception?.description ?? "nil")
        @unknown default:
            return NSLocalizedString("card_error_unknown", comment: "")
        }
    }
}

extension APDU.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .commandBodyDataTooLarge:
            return NSLocalizedString("apdu_error_command_body_data_too_large", comment: "")
        case .expectedResponseLengthOutOfBounds:
            return NSLocalizedString("apdu_error_expected_response_length_out_of_bounds", comment: "")
        case let .insufficientResponseData(data):
            return String(format: NSLocalizedString("apdu_error_insufficient_response_data", comment: ""), "\(data)")
        @unknown default:
            return NSLocalizedString("apdu_error_unknown", comment: "")
        }
    }
}
