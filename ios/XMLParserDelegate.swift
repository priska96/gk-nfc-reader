//
//  XMLParserDelegate.swift
//
//  Created by Priska Kohnen on 01.10.24.
//

import Foundation

import Foundation

public enum XMLParsingError: Error {
    case dataConversionFailed
    case parsingFailed
    case unknownElement(String)

    public var description: String {
        switch self {
        case .dataConversionFailed:
          return NSLocalizedString("xml_data_conversion_failed", comment: "")
        case .parsingFailed:
          return NSLocalizedString("xml_parsing_failed", comment: "")
        case .unknownElement(let elementName):
          let localizedString = NSLocalizedString("xml_unknown_element", comment: "")
          return String(format: localizedString, elementName)
        }
    }
}

class XMLDataExtractor: NSObject, XMLParserDelegate {
    var currentElement: String = ""
    var parsedData: [String: String] = [:]
    var foundCharacters: String = ""

    let elementsToExtract = ["Versicherten_ID", "Geburtsdatum", "Vorname", "Nachname", "Geschlecht", "Postleitzahl", "Ort", "Strasse", "Hausnummer", "Wohnsitzlaendercode"]

    // Now the function is marked as 'throws' to propagate errors
    func parse(xmlString: String) throws -> [String: String]? {
        guard let data = xmlString.data(using: .utf8) else {
            throw XMLParsingError.dataConversionFailed
        }

        let parser = XMLParser(data: data)
        parser.delegate = self
        if parser.parse() {
            return parsedData
        } else {
            throw XMLParsingError.parsingFailed
        }
    }

    // Called when starting a new element
    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        currentElement = elementName
        foundCharacters = ""
        
        // Handle unknown elements if needed
        if !elementsToExtract.contains(currentElement) {
            print("Warning: \(XMLParsingError.unknownElement(currentElement).description)")
        }
    }

    // Called when characters are found inside an element
    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if elementsToExtract.contains(currentElement) {
            foundCharacters += string
        }
    }

    // Called when an element ends
    func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
        if elementsToExtract.contains(elementName) {
            parsedData[elementName] = foundCharacters.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    // Handle parser errors (optional)
    func parser(_ parser: XMLParser, parseErrorOccurred parseError: Error) {
        print("XML Parsing error: \(parseError.localizedDescription)")
    }
}
