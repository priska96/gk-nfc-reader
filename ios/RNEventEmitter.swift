//
//  RNEventEmitter.swift
//
//  Created by Priska Kohnen on 20.09.24.
//

import Foundation
import React

@objc(RNEventEmitter)
class RNEventEmitter : RCTEventEmitter {
    static var emitter:RNEventEmitter?
    
    private var supportedEventNames: Set<String> = ["onStatusChange"]
    private var hasAttachedListener = false
    
    // Allows a shared EventEmitter instance to avoid initializing without the RNBridge
    // Without this step, you'll run into errors talking aobut a missing bridge
    override init() {
        super.init()
        RNEventEmitter.emitter = self
    }
    
    override class func requiresMainQueueSetup() -> Bool {
        return false
    }
    
    // These functions make sure that there is an attached listener so that events are
    // only sent when a listener is attached
    override func startObserving() {
        hasAttachedListener = true
    }
    override func stopObserving() {
        hasAttachedListener = false
    }
    
    // Must return an array of the supported events. Any unsupported events will throw errors
    // if they are passed in to `sendEvent`
    override func supportedEvents() -> [String] {
        return Array(supportedEventNames)
    }
    
    // Allows sending of supported events and adds protections for when either no listeners
    // ar attached or the specified event isn't a supported event
    func emitEvent(withName name: String, body: Any!) {
        if hasAttachedListener && supportedEventNames.contains(name) {
            sendEvent(withName: name, body: body)
        }
    }
}