import { NativeEventEmitter, NativeModules } from "react-native";
const { RNNFCLoginController, RNEventEmitter } = NativeModules;
console.log("emitter", { RNEventEmitter });
let eventEmitterRaw = null;
if (!RNEventEmitter) {
    console.error("Native module not found");
}
else {
    eventEmitterRaw = new NativeEventEmitter(RNEventEmitter);
}
export const eventEmitter = eventEmitterRaw;
export const readPersonalDataNative = (options) => {
    return RNNFCLoginController.readPersonalData(options);
};
//# sourceMappingURL=RNNFCLoginController.js.map