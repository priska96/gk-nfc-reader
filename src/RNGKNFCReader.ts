import { NativeEventEmitter, NativeModules } from "react-native";
const { RNGKNFCReader, RNEventEmitter } = NativeModules;

if (!RNEventEmitter || !RNGKNFCReader) {
  console.error("Native module not found");
}

export const eventEmitter = new NativeEventEmitter(RNEventEmitter);
export interface PersonalData {
  lastname: string;
  firstname: string;
  birthday: string;
  gender: string;
  street: string;
  housenumber: string;
  zipCode: string;
  city: string;
  counrtyCode: string;
  insuranceId: string;
}

export enum State {
  idle = "idle",
  loading = "loading",
  success = "success",
  error = "error",
}
export interface NFCReaderState {
  state: State;
  value?: PersonalData;
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
