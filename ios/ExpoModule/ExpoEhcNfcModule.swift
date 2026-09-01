import ExpoModulesCore

struct ReadPersonalDataOptionsV2: Record {
  @Field
  var can: String = ""

  @Field
  var pin: Int?

  @Field
  var checkBrainpoolAlgorithm: Bool?
}

public class ExpoEhcNfcModule: Module {
    public static var shared: ExpoEhcNfcModule?
  public func definition() -> ModuleDefinition {
    Name("ExpoEhcNfc")

    Events("onStatusChange")
      
    AsyncFunction("readPersonalData") { (readPersonalDataOptions: ReadPersonalDataOptionsV2, promise: Promise) in
        let nfcReader = NfcReaderV2()
        do {
          let result = try await nfcReader.readPersonalData(readPersonalDataOptions)
          promise.resolve(result)
        } catch {
          promise.reject("error", error.localizedDescription)
        }
    }
  }
    
    func sendStateChange(event: [String:Any]) {
        sendEvent("onStatusChange",  event)
    }
}
