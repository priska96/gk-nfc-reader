import {NativeEventEmitter, NativeModules} from 'react-native';
const {RNNFCLoginController, RNEventEmitter} = NativeModules;

export interface PState {
  state: 'idle' | 'loading' | 'value' | 'error';
  value?: boolean;
  error?: string;
}
console.log('emitter', {RNEventEmitter});

export const eventEmitter = new NativeEventEmitter(RNEventEmitter);

export interface ReadPersonalDataOptions {
  can: string;
  pin?: string;
  checkBrainpoolAlgorithm?: boolean;
}

export const readPersonalDataOptions = (
  options: ReadPersonalDataOptions,
): Promise<string> => {
  return RNNFCLoginController.readPersonalDataOptions(options);
};

export default RNNFCLoginController;
