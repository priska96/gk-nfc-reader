//
//  XMLParserDelegate.swift
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

@objc(RNGKNFCReader)
public class RNGKNFCReader: NSObject {
  public enum Error: Swift.Error, LocalizedError {
    /// In case the PIN or CAN could not be constructed from input
    case cardError(NFCHealthCardSessionError)
    case invalidCanOrPinFormat
    
    public var errorDescription: String? {
      switch self {
      case let .cardError(error):
        return error.localizedDescription
      case .invalidCanOrPinFormat:
        return NSLocalizedString("invalid_can_or_pin_format", comment: "")
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
    // Send event or method call to React Native here
    switch state {
    case .idle:
      self.sendStatusUpdateToReactNative(["state": "idle"])
    case .loading:
      self.sendStatusUpdateToReactNative(["state": "loading"])
    case .value(let value):
      switch value{
      case .dictionary(let dict):
        self.sendStatusUpdateToReactNative(["state": "success", "value": dict.toDictionary() ?? ["":""]])
      case .bool(let boolValue):
        self.sendStatusUpdateToReactNative(["state": "success", "value": boolValue])
      }
    case .error(let error):
      self.sendStatusUpdateToReactNative(["state": "failure", "error": error.localizedDescription])
    }  }
  
  // Method to trigger status update in React Native
  @objc func sendStatusUpdateToReactNative(_ body: NSDictionary) {
    guard let myEmitter =  RNEventEmitter.emitter else {
      return
    }
    myEmitter.emitEvent(withName: "onStatusChange", body: body)
  }
  
  @objc func getResults() -> String {
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
  
  @objc func readPersonalData(_ readPersonalDataOptions: ReadPersonalDataOptions, resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    Task{
      @MainActor in
      await self._readPersonalData(readPersonalDataOptions:readPersonalDataOptions)
      if self.nfcReaderState.error != nil {
        reject("error", getResults(), nfcReaderState.error)
      }
      else{
        resolve(getResults())
      }
    }
  }
  
  @MainActor
  func _readPersonalData(readPersonalDataOptions: ReadPersonalDataOptions) async {
    let can = readPersonalDataOptions.can //RCTConvert.nsString(readPersonalDataOptions["can"]) ?? "123456"
    
    if case .loading = await nfcReaderState { return }
    self.nfcReaderState = .loading(nil)
    
    // try open session and init operation
    // tag::nfcHealthCardSession_init[]
    guard let nfcHealthCardSession = NFCHealthCardSession(
      messages: messages,
      can: can!,
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
      
      return
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
      return
    } catch {
      self.nfcReaderState = .error(error)
      let result = ReadingResult(result: self.nfcReaderState, commands: CommandLogger.commands)
      self.results.append(result)
      
      nfcHealthCardSession.invalidateSession(with: error.localizedDescription)
      return
    }
    // end::nfcHealthCardSession_errorHandling[]
    Logger.nfcDemo.debug("Perosnal Data: \(personalData)")
  }
  
  func parseData(dataRaw: Data) throws -> [String:String]{
    var data = dataRaw
    // remove 2 bytes indicating length
    data.removeFirst(2)
    
    // uncompress and decode
    do{
      let uncompressedData = try decompressZlib(data)
      
      do{
        let xmlString = String(data: uncompressedData, encoding: .utf8)
        print("Read XML OutputData of MF/DF.HCA/EF.hcaPD: \(xmlString ?? "")")
        
        do{
          let extractor = XMLDataExtractor()
          let parsedData = try extractor.parse(xmlString: xmlString!)
          
          print("Extracted Data: \(parsedData ?? ["":""])")
          return parsedData!
        } catch {
          throw ParseResponseDataError.xmlParserError(error as! XMLParsingError)
        }
      } catch {
        throw ParseResponseDataError.convertToXMLString
      }
    }
    catch{
      throw ParseResponseDataError.zLibError(error as! ZlibError)
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
    return data
    
  }
}
