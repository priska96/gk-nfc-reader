import { NativeEventEmitter, NativeModules } from "react-native";
const { RNNFCLoginController, RNEventEmitter } = NativeModules;

export interface PState {
  state: "idle" | "loading" | "value" | "error";
  value?: boolean;
  error?: string;
}
console.log("emitter", { RNEventEmitter });

let eventEmitterRaw: NativeEventEmitter | null = null;
if (!RNEventEmitter) {
  console.error("Native module not found");
} else {
  eventEmitterRaw = new NativeEventEmitter(RNEventEmitter);
}
export const eventEmitter = eventEmitterRaw;

export interface ReadPersonalDataOptions {
  can: string;
  pin?: string;
  checkBrainpoolAlgorithm?: boolean;
}

export const readPersonalDataNative = (
  options: ReadPersonalDataOptions
): Promise<string> => {
  return RNNFCLoginController.readPersonalData(options);
};
