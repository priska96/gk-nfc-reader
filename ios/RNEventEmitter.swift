import Foundation
import React

@objc(RNEventEmitter)
class RNEventEmitter: RCTEventEmitter {
    
    /*static let sharedInstance = RNEventEmitter()
    
    private override init() {
        super.init()
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    private var hasListeners = false
    
    override func startObserving() {
        hasListeners = true
    }

    override func stopObserving() {
        hasListeners = false
    }
    
    override func sendEvent(withName name: String, body: Any) {
        if hasListeners {
            self.sendEvent(withName: name, body: body)
        }
    }*/
  
    public static var emitter: RCTEventEmitter!

    override init() {
      super.init()
      RNEventEmitter.emitter = self
    }

    @objc open override func supportedEvents() -> [String]! {
        return ["onStatusChange"]
    }
  
  @objc(requiresMainQueueSetup)
    override static func requiresMainQueueSetup() -> Bool {
      return false
    }
}
