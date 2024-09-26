import { NativeEventEmitter } from "react-native";
export interface PState {
    state: "idle" | "loading" | "value" | "error";
    value?: boolean;
    error?: string;
}
export declare const eventEmitter: NativeEventEmitter | null;
export interface ReadPersonalDataOptions {
    can: string;
    pin?: string;
    checkBrainpoolAlgorithm?: boolean;
}
export declare const readPersonalDataNative: (options: ReadPersonalDataOptions) => Promise<string>;
//# sourceMappingURL=RNNFCLoginController.d.ts.map