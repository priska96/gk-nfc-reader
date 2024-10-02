import { NativeEventEmitter, NativeModules } from "react-native";
const { RNGKNFCReader, RNEventEmitter } = NativeModules;
if (!RNEventEmitter || !RNGKNFCReader) {
    console.error("Native module not found");
}
export const eventEmitter = new NativeEventEmitter(RNEventEmitter);
export var State;
(function (State) {
    State["idle"] = "idle";
    State["loading"] = "loading";
    State["success"] = "success";
    State["error"] = "error";
})(State || (State = {}));
export const readPersonalDataNative = (options) => {
    return RNGKNFCReader.readPersonalData(options);
};
//# sourceMappingURL=RNGKNFCReader.js.map