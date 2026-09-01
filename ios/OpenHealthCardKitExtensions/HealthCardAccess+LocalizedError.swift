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
import HealthCardAccess
import HealthCardControl

extension HealthCard.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case .operational:
            return NSLocalizedString("healthcard_error_operational", comment: "")
        case let .unexpectedResponse(actual, expected):
            return String(
                format: NSLocalizedString("healthcard_error_unexpected_response", comment: ""),
                "\(actual)", "\(expected)"
            )
        case let .unknownCardType(aid):
            return String(
                format: NSLocalizedString("healthcard_error_unknown_card_type", comment: ""),
                aid?.rawValue.hexString() ?? ""
            )
        case let .illegalGeneration(version):
            let description = version.generation()?.description ?? String(describing: version)
            return String(
                format: NSLocalizedString("healthcard_error_illegal_generation", comment: ""),
                description
            )
        case .unsupportedCardType:
            return NSLocalizedString("healthcard_error_unsupported_card_type", comment: "")
        @unknown default:
            return NSLocalizedString("healthcard_error_unknown", comment: "")
        }
    }
}


extension ApplicationIdentifier.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(
                format: NSLocalizedString("appid_error_illegal_argument", comment: ""),
                arg
            )
        case let .invalidLength(length):
            return String(
                format: NSLocalizedString("appid_error_invalid_length", comment: ""),
                length
            )
        @unknown default:
            return NSLocalizedString("appid_error_unknown", comment: "")
        }
    }
}


extension FileControlParameter.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(format: NSLocalizedString("fcp_error_illegal_argument", comment: ""), arg)
        case let .asn1ParseError(asn1, reason):
            return String(
                format: NSLocalizedString("fcp_error_asn1_parse", comment: ""),
                "\(asn1)", "\(reason)"
            )
        case let .invalidCard(description):
            return String(format: NSLocalizedString("fcp_error_invalid_card", comment: ""), description)
        @unknown default:
            return NSLocalizedString("fcp_error_unknown", comment: "")
        }
    }
}

extension FileIdentifier.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(format: NSLocalizedString("fid_error_illegal_argument", comment: ""), arg)
        case let .invalidLength(length):
            return String(format: NSLocalizedString("fid_error_invalid_length", comment: ""), length)
        @unknown default:
            return NSLocalizedString("fid_error_unknown", comment: "")
        }
    }
}

extension Format2Pin.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(format: NSLocalizedString("format2pin_error_illegal_argument", comment: ""), arg)
        @unknown default:
            return NSLocalizedString("format2pin_error_unknown", comment: "")
        }
    }
}

extension GemCvCertificate.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .unexpected(tag):
            return String(format: NSLocalizedString("gemcv_error_unexpected_tag", comment: ""), String(describing:tag))
        case let .missing(tag, source):
            return String(
                format: NSLocalizedString("gemcv_error_missing_tag", comment: ""),
                String(describing:tag), "\(source)"
            )
        case .missingTagParameter:
            return NSLocalizedString("gemcv_error_missing_tag_parameter", comment: "")
        @unknown default:
            return NSLocalizedString("gemcv_error_unknown", comment: "")
        }
    }
}

extension Key.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(format: NSLocalizedString("key_error_illegal_argument", comment: ""), arg)
        @unknown default:
            return NSLocalizedString("key_error_unknown", comment: "")
        }
    }
}

extension Password.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(format: NSLocalizedString("password_error_illegal_argument", comment: ""), arg)
        @unknown default:
            return NSLocalizedString("password_error_unknown", comment: "")
        }
    }
}

extension ShortFileIdentifier.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .illegalArgument(arg):
            return String(format: NSLocalizedString("sfid_error_illegal_argument", comment: ""), arg)
        @unknown default:
            return NSLocalizedString("sfid_error_unknown", comment: "")
        }
    }
}

extension CardVersion2.Error: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .parseError(error):
            return String(format: NSLocalizedString("cv2_error_parse", comment: ""), "\(error)")
        @unknown default:
            return NSLocalizedString("cv2_error_unknown", comment: "")
        }
    }
}

extension HealthCardCommandBuilder.InvalidArgument: LocalizedError {
    public var errorDescription: String? {
        switch self {
        case let .offsetOutOfBounds(offset, usingShortFileIdentifier: isShort):
            return String(
                format: NSLocalizedString("hccb_invalid_offset", comment: ""),
                offset, isShort.description
            )
        case let .recordDataSizeOutOfBounds(data):
            return String(format: NSLocalizedString("hccb_invalid_record_data_size", comment: ""), "\(data)")
        case .expectedLengthMustNotBeZero:
            return NSLocalizedString("hccb_expected_length_zero", comment: "")
        case let .expectedLengthNotAWildcardValue(length):
            return String(format: NSLocalizedString("hccb_expected_length_not_wildcard", comment: ""), length)
        case let .wrongMACLength(length):
            return String(format: NSLocalizedString("hccb_wrong_mac_length", comment: ""), length)
        case let .wrongHashLength(length, expected):
            return String(
                format: NSLocalizedString("hccb_wrong_hash_length", comment: ""),
                length, expected
            )
        case let .wrongSignatureLength(length, expected):
            return String(
                format: NSLocalizedString("hccb_wrong_signature_length", comment: ""),
                length, expected
            )
        case let .unsupportedKey(secKey):
            return String(format: NSLocalizedString("hccb_unsupported_key", comment: ""), "\(secKey)")
        case let .illegalSize(size, expected):
            return String(
                format: NSLocalizedString("hccb_illegal_size", comment: ""),
                size, expected
            )
        case let .illegalValue(value, for: string, expected):
            return String(
                format: NSLocalizedString("hccb_illegal_value", comment: ""),
                "\(value)", string, "\(expected)"
            )
        case let .illegalOid(oid):
            return String(format: NSLocalizedString("hccb_illegal_oid", comment: ""), String(describing: oid))
        @unknown default:
            return NSLocalizedString("hccb_unknown", comment: "")
        }
    }
}


extension ReadError {
  /// The response status did not match the expected response status
  public var errorDescription: String? {
    switch self {
    case let .unexpectedResponse(state: state):
      return NSLocalizedString("unexpected_response", comment: "")
      
    case let .noData(state: state):
      return NSLocalizedString("no_data", comment: "")
      
    case let .fcpMissingReadSize(state: state):
      return NSLocalizedString("fcp_missing_read_size", comment: "")
    }
    
  }
}

extension SelectError {
  public var errorDescription: String? {
    switch self {
    case let .failedToSelectAid(_: aid, status: status):
      return NSLocalizedString("unexpected_response", comment: "")
      
    case let .failedToSelectFid(_:fid, status: status):
      return NSLocalizedString("no_data", comment: "")
    }
  }
}
