//
//  NfcReader.swift
//
//  Created by Priska Kohnen on 05.09.24.
//

import CardReaderProviderApi
import Combine
import CoreNFC
import Foundation
import HealthCardAccess
import HealthCardControl
import Helper
import NFCCardReaderProvider
import OSLog

public class NfcReader: NSObject {
    public enum Error: Swift.Error, LocalizedError {
        // In case the PIN or CAN could not be constructed from input
        case invalidCanOrPinFormat
        case generalError(String)
        
        public var errorDescription: String? {
            switch self {
            case .invalidCanOrPinFormat:
                return NSLocalizedString("invalid_can_or_pin_format", comment: "")
            case let .generalError(message):
                return message
            }
        }
    }
    
    @Published
    private var nfcReaderState: ViewState<ViewResult, Swift.Error> = .idle
    var results: [ReadingResult] = []
    var cancellable: AnyCancellable?
    
    override init() {
        super.init()
        // Monitor changes to statusMessage
        cancellable = $nfcReaderState.sink { [weak self] newValue in
            self?.sendStatusUpdateToReactNativeWrapper(state: newValue)
        }
    }
    
    // Method to trigger status update in React Native
    func sendStatusUpdateToReactNativeWrapper(state: ViewState<ViewResult, Swift.Error>) {
        var event: [String: Any] = [:]
        // Send event or method call to React Native here
        switch state {
        case .idle:
            event["state"] = "idle"
        case .loading:
            event["state"] = "loading"
        case .value(let value):
            event["state"] = "success"
            switch value {
            case .dictionary(let dict):
                event["value"] = dict.toDictionary() ?? [:]
            case .bool(let boolValue):
                event["value"] = boolValue
            }
        case .error(let error):
            event["state"] = "failure"
            event["error"] = error.localizedDescription
        }
        RNEhcNfc.rnEhcNfc?.sendStatusUpdateToReactNative(event: event)
    }
    
    func getResults() -> String {
        var readingResults: [ReadingResult] {
            (self.results)
                .sorted { $0.timestamp > $1.timestamp }
        }
        let res = readingResults.map { $0.formattedDescription() }
        return res.joined(separator: "\n\n")
    }
    
    let messages = NFCHealthCardSession<Data>.Messages(
        discoveryMessage: NSLocalizedString("nfc_txt_discoveryMessage", comment: ""),
        connectMessage: NSLocalizedString("nfc_txt_connectMessage", comment: ""),
        secureChannelMessage: NSLocalizedString("nfc_txt_secureChannel", comment: ""),
        noCardMessage: NSLocalizedString("nfc_txt_noCardMessage", comment: ""),
        multipleCardsMessage: NSLocalizedString("nfc_txt_multipleCardsMessage", comment: ""),
        unsupportedCardMessage: NSLocalizedString("nfc_txt_unsupportedCardMessage", comment: ""),
        connectionErrorMessage: NSLocalizedString("nfc_txt_connectionErrorMessage", comment: "")
    )
    
    @MainActor
    func readPersonalData(_ readPersonalDataOptions: ReadPersonalDataOptions) async throws -> String {
        let can = readPersonalDataOptions.can
        
        if case .loading = self.nfcReaderState { return getResults() }
        self.nfcReaderState = .loading(nil)
        
        // try open session and init operation
        // tag::nfcHealthCardSession_init[]
        guard let nfcHealthCardSession = NFCHealthCardSession(
            messages: messages,
            can: can,
            operation: { session in
                session.updateAlert(message: NSLocalizedString("nfc_txt_selectHca", comment: ""))
                let outcome = try await session.card.selectAndReadHcaPD()
                
                return outcome
            }
        )
        // handle the case the Session could not be initialized
        else {
            // end::nfcHealthCardSession_init[]
            self.nfcReaderState = .error(NFCHealthCardSessionError.couldNotInitializeSession)
            let result = ReadingResult(result: self.nfcReaderState, commands: CommandLogger.commands)
            self.results.append(result)
            
            return getResults()
        }
        
        if can.count != 6 {
            self.nfcReaderState = .error(Error.invalidCanOrPinFormat)
            let result = ReadingResult(result: self.nfcReaderState, commands: CommandLogger.commands)
            self.results.append(result)
            
            return getResults()
            
        }
        
        let personalDataRaw: Data
        let personalDataDict: [String:String]
        let personalData : PersonalData
        
        // excetue previously inited operation
        do {
            // tag::nfcHealthCardSession_execute[]
            personalDataRaw = try await nfcHealthCardSession.executeOperation()
            // end::nfcHealthCardSession_execute[]
            
            personalDataDict = try parseData(dataRaw: personalDataRaw)
            personalData = PersonalData(dictionary: personalDataDict)
            
            self.nfcReaderState = .value(.dictionary(personalData))
            
            let result = ReadingResult(result: self.nfcReaderState, commands: CommandLogger.commands)
            self.results.append(result)
            
            // tag::nfcHealthCardSession_errorHandling[]
        } catch NFCHealthCardSessionError.coreNFC(.userCanceled) {
            // error type is always `NFCHealthCardSessionError`
            // here we especially handle when the user canceled the session
            
            Logger.nfcDemo.debug("User cancled session. Reset state to idle")
            self.nfcReaderState = .idle
            // Do some view-property update
            // Calling .invalidateSession() is not strictly necessary
            //  since nfcHealthCardSession does it while it's de-initializing.
            nfcHealthCardSession.invalidateSession(with: nil)
            throw Error.generalError(getResults())
        } catch {
            self.nfcReaderState = .error(error)
            let result = ReadingResult(result: self.nfcReaderState, commands: CommandLogger.commands)
            self.results.append(result)
            
            nfcHealthCardSession.invalidateSession(with: error.localizedDescription)
            throw Error.generalError(getResults())
        }
        // end::nfcHealthCardSession_errorHandling[]
        Logger.nfcDemo.debug("Perosnal Data: \(personalData)")
        return getResults()
    }
    
    func parseData(dataRaw: Data) throws -> [String:String] {
        var data = dataRaw
        
        print("compressedData before cut: \(dataRaw.hexa)")
        // remove 2 bytes indicating length
        data.removeFirst(2)
        
        print("compressedData: \(data.hexa)")
        // uncompress and decode
        do {
            let uncompressedData = try decompressZlib(data)
            print("uncompressedData: \(uncompressedData.hexa)")
            
            do {
                let xmlString = String(data: uncompressedData, encoding: .isoLatin1)
                print("Read XML OutputData of MF/DF.HCA/EF.hcaPD: \(xmlString ?? "")")
                do {
                    let extractor = XMLDataExtractor()
                    var parsedData = try extractor.parse(xmlString: xmlString!)
                    if let dateString = parsedData?["Geburtsdatum"], dateString.count == 8 {
                        let formattedDate = String(dateString.prefix(4)) + "-" + String(dateString.dropFirst(4).prefix(2)) + "-" + String(dateString.suffix(2))
                        
                        parsedData?["Geburtsdatum"] = formattedDate
                    }
                    print("Extracted Data: \(parsedData ?? ["":""])")
                    return parsedData!
                } catch let error as XMLParsingError {
                    throw ParseResponseDataError.xmlParserError(error)
                }
            } catch {
                throw ParseResponseDataError.convertToXMLString
            }
        } catch let error as ZlibError {
            throw ParseResponseDataError.zLibError(error)
        } catch {
            throw error // Handle other unexpected errors
        }
    }
}

extension HealthCardType {
    func selectAndReadHcaPD() async throws -> Data {
        CommandLogger.commands.append(Command(message: "Select Personal Data and Read file", type: .description))
        
        let dedicatedFile = DedicatedFile(
            aid: EgkFileSystem.DF.HCA.aid,
            fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
        )
        let (responseStatus, fileControlParameter) = try await self.selectDedicatedAsync(file: dedicatedFile, fcp: true )
        
        guard let fcp = fileControlParameter, let readSize = fcp.readSize
        else {
            throw ReadError.fcpMissingReadSize(state: responseStatus)
        }
        let data = try await self.readSelectedFileAsync(expected: Int(readSize))
        print("readHcaPD: \(data.hexa)")
        return data
    }
}
