//
//  RNEhcNfc.swift
//
//  Created by Priska Kohnen on 05.09.24.
//

import Foundation
import React

@objc(RNEhcNfc)
public class RNEhcNfc: NSObject {
    static var rnEhcNfc: RNEhcNfc?
    
    var nfcReader: NfcReader = NfcReader()
    
    override init() {
        super.init()
        RNEhcNfc.rnEhcNfc = self
    }
    
    // Method to trigger status update in React Native
    func sendStatusUpdateToReactNative(event: [String: Any]) {
        guard let myEmitter = RNEventEmitter.emitter else {
            return
        }
        myEmitter.emitEvent(withName: "onStatusChange", body: event)
    }
    
    @objc
    func readPersonalData(
        _ readPersonalDataOptions: ReadPersonalDataOptions,
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        Task { @MainActor in
            do {
                let result = try await nfcReader.readPersonalData(readPersonalDataOptions)
                resolve(result)
            } catch {
                reject("error", error.localizedDescription, error)
            }
        }
    }
}
