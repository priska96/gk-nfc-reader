//
//  ParseResponseError+LocalizedError.swift
//
//  Created by Priska Kohnen on 01.10.24.
//

import Foundation

public enum ParseResponseDataError: Swift.Error, LocalizedError {
  
  case zLibError(ZlibError)
  case convertToXMLString
  case xmlParserError(XMLParsingError)
  
  public var errorDescription: String? {
    switch self {
    case let .zLibError(error: error):
      switch error {
      case .initializationFailed:
        return error.localizedDescription
      case .streamError:
        return error.localizedDescription
        
      case .decompressionFailed:
        return error.localizedDescription
      case .incompleteDecompression:
        return error.localizedDescription
      }
    case .convertToXMLString:
      return NSLocalizedString("unzipped_to_xml_string", comment: "")
    case let .xmlParserError(error: error):
      switch error {
      case .dataConversionFailed:
        return error.localizedDescription
      case .parsingFailed:
        return error.localizedDescription
        
      case .unknownElement:
        return error.localizedDescription
      }
    }
  }
}
