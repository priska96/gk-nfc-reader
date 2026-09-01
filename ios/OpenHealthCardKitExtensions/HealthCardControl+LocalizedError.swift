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

import Foundation
import HealthCardControl

extension KeyAgreement.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .illegalArgument:
            return NSLocalizedString("ka_illegal_argument", comment: "")
        case .unexpectedFormedAnswerFromCard:
            return NSLocalizedString("ka_unexpected_answer", comment: "")
        case .resultOfEcArithmeticWasInfinite:
            return NSLocalizedString("ka_infinite_ec_arithmetic", comment: "")
        case .macPcdVerificationFailedOnCard:
            return NSLocalizedString("ka_wrong_can", comment: "")
        case .macPiccVerificationFailedLocally:
            return NSLocalizedString("ka_mac_picc_failed", comment: "")
        case .noValidHealthCardStatus:
            return NSLocalizedString("ka_no_valid_status", comment: "")
        case .efCardAccessNotAvailable:
            return NSLocalizedString("ka_no_ef_card_access", comment: "")
        case let .unsupportedKeyAgreementAlgorithm(identifier):
            return String(
                format: NSLocalizedString("ka_unsupported_algorithm", comment: ""),
                String(describing: identifier)
            )
        @unknown default:
            return NSLocalizedString("ka_unknown_error", comment: "")
        }
    }
}
