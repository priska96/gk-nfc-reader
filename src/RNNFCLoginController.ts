import { NativeEventEmitter, NativeModules } from "react-native";
const { RNGKNFCReader, RNEventEmitter } = NativeModules;

if (!RNEventEmitter || !RNGKNFCReader) {
  console.error("Native module not found");
}

export const eventEmitter = new NativeEventEmitter(RNEventEmitter);

export interface PState {
  state: "idle" | "loading" | "value" | "error";
  value?: boolean;
  error?: string;
}
export interface ReadPersonalDataOptions {
  can: string;
  pin?: string;
  checkBrainpoolAlgorithm?: boolean;
}

export const readPersonalDataNative = (
  options: ReadPersonalDataOptions
): Promise<string> => {
  return RNGKNFCReader.readPersonalData(options);
};
