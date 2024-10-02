import { NativeEventEmitter } from "react-native";
export declare const eventEmitter: NativeEventEmitter;
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
export declare enum State {
    idle = "idle",
    loading = "loading",
    success = "success",
    error = "error"
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
export declare const readPersonalDataNative: (options: ReadPersonalDataOptions) => Promise<string>;
//# sourceMappingURL=RNGKNFCReader.d.ts.map