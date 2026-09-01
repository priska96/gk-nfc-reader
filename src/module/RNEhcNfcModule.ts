import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

import { NFCReaderState, ReadPersonalDataOptions } from './ExpoEhcNfc.types';

let RNEhcNfc: any;
let RNEventEmitter: any;

if (Platform.OS === 'android') {
  console.warn("Don't load ios module instead use ExpoEhcNfcModule");

  RNEhcNfc = {
    readPersonalData: async (_options: ReadPersonalDataOptions) => {
      console.warn('readPersonalData called on iOS mock');
      return Promise.resolve('Mocked NFC data (iOS)');
    },
    addListener: () => {
      console.warn('addListener called on iOS mock');
      return { remove: () => {} };
    },
    removeAllListeners: () => {},
  } as unknown as typeof RNEhcNfc;

  RNEventEmitter = {
    addListener: () => ({
      remove: () => {},
    }),
    removeAllListeners: () => {},
  };
} else {
  const { RNEhcNfc: iosModule, RNEventEmitter: iosEventEmitter } =
    NativeModules;

  if (!iosModule) {
    console.error('RNEhcNfc ios native module not found');
  }

  if (!iosEventEmitter) {
    console.error('RNEventEmitter ios native module not found');
  }

  RNEhcNfc = iosModule ?? {};
  RNEventEmitter = iosEventEmitter ?? {};
}

export function statusChangeListenerIos(
  callback: (nfcReaderState: NFCReaderState) => void
) {
  const eventEmitter = new NativeEventEmitter(RNEventEmitter);
  if (!eventEmitter) return;
  return eventEmitter.addListener(
    'onStatusChange',
    (nfcReaderState: NFCReaderState) => {
      callback(nfcReaderState);
    }
  );
}
export function readPersonalDataNativeIos(
  options: ReadPersonalDataOptions
): Promise<string> {
  return RNEhcNfc.readPersonalData(options);
}
