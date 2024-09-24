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
import Combine
import CoreNFC
import Foundation
import HealthCardAccess
import HealthCardControl
import Helper
import NFCCardReaderProvider
import OSLog

@objc(RNNFCLoginController)
public class RNNFCLoginController: NSObject {
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
  private var pState: ViewState<Bool, Swift.Error> = .idle
  var state: Published<ViewState<Bool, Swift.Error>>.Publisher {
    $pState
  }
  
  
  var results: [ReadingResult] = []
  
  var cancellable: AnyCancellable?
  
  
  override init() {
    super.init()
    
    // Monitor changes to statusMessage
    cancellable = $pState.sink { [weak self] newValue in
      self?.sendStatusUpdateToReactNativeWrapper(state: newValue)
    }
  }
  
  // Method to trigger status update in React Native
  func sendStatusUpdateToReactNativeWrapper(state: ViewState<Bool, Swift.Error>) {
    print("sendStatusUpdateToReactNativeWrapper")
    // Send event or method call to React Native here
    switch state {
    case .idle:
      print("idle")
      self.sendStatusUpdateToReactNative(["state": "idle"])
    case .loading:
      print("loading")
      self.sendStatusUpdateToReactNative(["state": "loading"])
    case .value(let value):
      print("value")
      self.sendStatusUpdateToReactNative(["state": "success", "value": value])
    case .error(let error):
      print("erro")
      self.sendStatusUpdateToReactNative(["state": "failure", "error": error.localizedDescription])
    }  }
  
  // Method to trigger status update in React Native
  @objc func sendStatusUpdateToReactNative(_ body: NSDictionary) {
    print("sendStatusUpdateToReactNative")
    
    guard let myEmitter =  RNEventEmitter.emitter else {
      print("myEmitter is nil")
      return
    }
    myEmitter.sendEvent(withName: "onStatusChange", body: body)
  }
  
  // Expose this method to React Native
  @objc
  func getPState(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    //Task{@MainActor in
    // Return the current state as a dictionary
    switch pState {
    case .idle:
      resolve(["state": "idle"])
    case .loading:
      resolve(["state": "loading"])
    case .value(let value):
      resolve(["state": "success", "value": value])
    case .error(let error):
      resolve(["state": "failure", "error": error.localizedDescription])
    }
    //}
  }
  
  @objc func getResults() -> String {
    var readingResults: [ReadingResult] {
      (self.results)
        .sorted { $0.timestamp > $1.timestamp }
    }
    
    let res = readingResults.map { $0.formattedDescription() }
    return res.joined(separator: "\n\n")
  }
  
  @MainActor
  func dismissError() async {
    if pState.error != nil {
      pState = .idle
    }
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
    print("data desc: ", readPersonalDataOptions.can!)
    Task{
      @MainActor in
      await self._readPersonalData(readPersonalDataOptions:readPersonalDataOptions)
      
      print("state after readPersonalData ", await self.pState)
      if self.pState.error != nil {
        reject("error", getResults(), pState.error)
      }
      else{
        print("resolve")
        resolve(getResults())
      }
    }
  }
  
  @MainActor
  func _readPersonalData(readPersonalDataOptions: ReadPersonalDataOptions) async {
    let can = readPersonalDataOptions.can //RCTConvert.nsString(readPersonalDataOptions["can"]) ?? "123456"
    
    print("can: ", can!)
    if case .loading = await pState { return }
    self.pState = .loading(nil)
    
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
      
      print("got error. update state")
      self.pState = .error(NFCHealthCardSessionError.couldNotInitializeSession)
      let result = ReadingResult(result: self.pState, commands: CommandLogger.commands)
      self.results.append(result)
      
      return
    }
    
    let personalData: Data
    // excetue previously inited operation
    do {
      
      // tag::nfcHealthCardSession_execute[]
      personalData = try await nfcHealthCardSession.executeOperation()
      // end::nfcHealthCardSession_execute[]
      
      print("got result. update state ad results")
      self.pState = .value(true)
      let result = ReadingResult(result: self.pState, commands: CommandLogger.commands)
      self.results.append(result)
      
      // tag::nfcHealthCardSession_errorHandling[]
    } catch NFCHealthCardSessionError.coreNFC(.userCanceled) {
      // error type is always `NFCHealthCardSessionError`
      // here we especially handle when the user canceled the session
      
      Logger.nfcDemo.debug("User cancled session. Reset state to idle")
      self.pState = .idle
      // Do some view-property update
      // Calling .invalidateSession() is not strictly necessary
      //  since nfcHealthCardSession does it while it's de-initializing.
      nfcHealthCardSession.invalidateSession(with: nil)
      return
    } catch {
      print("got error. update state")
      self.pState = .error(error)
      let result = ReadingResult(result: self.pState, commands: CommandLogger.commands)
      self.results.append(result)
      
      nfcHealthCardSession.invalidateSession(with: error.localizedDescription)
      return
    }
    // end::nfcHealthCardSession_errorHandling[]
    Logger.nfcDemo.debug("Perosnal Data: \(personalData)")
  }
}


public enum HealthCardCommandError: Swift.Error, LocalizedError {
  /// In case the PIN or CAN could not be constructed from input
  case hcaPDUnavailable
  
  public var errorDescription: String? {
    switch self {
    case .hcaPDUnavailable:
      return NSLocalizedString("hca_pd_unavailable", comment: "")
    }
  }
}

extension HealthCardType {
  
  
  func dataFromHexString(hexString: String) -> Data? {
    var data = Data()
    var tempHexString = hexString
    
    // Ensure string length is even (2 chars per byte)
    if hexString.count % 2 != 0 {
      tempHexString = "0" + hexString
    }
    
    var currentIndex = tempHexString.startIndex
    
    while currentIndex < tempHexString.endIndex {
      let byteString = tempHexString[currentIndex..<tempHexString.index(currentIndex, offsetBy: 2)]
      let byte = UInt8(byteString, radix: 16)
      data.append(byte!)
      currentIndex = tempHexString.index(currentIndex, offsetBy: 2)
    }
    
    return data
  }
  
  func selectAndReadHcaPD() async throws -> Data {
    
    CommandLogger.commands.append(Command(message: "Select Personal Data and Read file", type: .description))
    let dedicatedFile = DedicatedFile(
      aid: EgkFileSystem.DF.HCA.aid,
      fid: EgkFileSystem.EF.hcaPD.fid // holds Personal Data of health card holder
    )
    let (responseStatus, fileControlParameter) = try await self.selectDedicatedAsync(file: dedicatedFile, fcp: true )
    print("responseStatus ", responseStatus)
    print("fileControlParam ", fileControlParameter ?? "nil")
    
    guard let fcp = fileControlParameter, let readSize = fcp.readSize
    else {
      throw ReadError.fcpMissingReadSize(state: responseStatus)
    }
    let data = try await self.readSelectedFileAsync(expected: Int(readSize))
    
    /*
     let str = try await self.readSelectedFileAsync(expected: Int(readSize))
     .map{ data in
     //print("data: " , data )
     return data
     }
     let myData = try data.hexString().hexa()
     let myData1 = dataFromHexString(hexString: data.hexString())
     print("myData", myData)
     print("myData1", myData1)
     */
    // unzip and decode here
    /*do{
     let lengthBytes = data.prefix(2)
     let length = UInt16(lengthBytes.withUnsafeBytes { $0.load(as: UInt16.self) })
     let compressedData = data.suffix(from: 1)//.prefix(Int(length))
     
     let uncompressedData = try data.gunzipped()
     
     if let xmlString = String(data: uncompressedData, encoding: .isoLatin1) {
     print("Read XML OutputData of MF/DF.HCA/EF.hcaPD: \(xmlString)")
     } else {
     fatalError("Error converting data to string")
     }
     }
     catch{
     fatalError("Error decompressing gzip data \(error)")
     }
     */
    
    print("data description", data.description)
    print("data base64", data.base64EncodedString())
    
    let cfEnc = CFStringEncodings.isoLatin9
    let nsEnc = CFStringConvertEncodingToNSStringEncoding(CFStringEncoding(cfEnc.rawValue))
    let isoLatin9encoding = String.Encoding(rawValue: nsEnc) // String.Encoding
    
    guard let xmlString = String(data: data, encoding: isoLatin9encoding)
    else {
      print("cannot be transformed ")
      throw ReadError.unexpectedResponse(state: responseStatus)
    }
    print("Read XML OutputData of MF/DF.HCA/EF.hcaPD: \(xmlString)")
    
    return data
  }
}

extension PSOAlgorithm {
  // [REQ:gemSpec_Krypt:A_17207] Assure only brainpoolP256r1 is used
  var isBp256r1: Bool {
    if case .signECDSA = self {
      return true
    }
    return false
  }
}

extension Bool {
  var asPinVerifyErrorMessage: String? {
    if self {
      return nil
    } else {
      return "False pincode (or blocked card)"
    }
  }
  
}

