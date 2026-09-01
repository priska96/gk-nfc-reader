import { NativeModule, requireNativeModule } from 'expo';
import { Platform } from 'react-native';

import {
  ExpoEhcNfcModuleEvents,
  NFCReaderState,
  NFCReaderStateAndroid,
  ReadPersonalDataOptions,
} from './ExpoEhcNfc.types';

declare class ExpoEhcNfcModule extends NativeModule<ExpoEhcNfcModuleEvents> {
  readPersonalData: (options: ReadPersonalDataOptions) => Promise<string>;
}

let ExpoEhcNfc: ExpoEhcNfcModule;

if (Platform.OS === 'ios') {
  console.warn("Don't load Android module instead use RNEhcNfcModule");

  ExpoEhcNfc = {
    readPersonalData: async (_options: ReadPersonalDataOptions) => {
      console.warn('readPersonalData called on iOS mock');
      return Promise.resolve('Mocked NFC data (iOS)');
    },
    addListener: () => {
      console.warn('addListener called on iOS mock');
      return { remove: () => {} };
    },
    removeAllListeners: () => {},
  } as unknown as ExpoEhcNfcModule;
} else {
  // Android: require the actual native module
  ExpoEhcNfc = requireNativeModule<ExpoEhcNfcModule>('ExpoEhcNfc');

  if (!ExpoEhcNfc) {
    console.error('ExpoEhcNfc Android native module not found');
  }
}

export function readPersonalDataNativeAndroid(
  options: ReadPersonalDataOptions
): Promise<string> {
  return Promise.resolve(ExpoEhcNfc.readPersonalData(options));
}
export function statusChangeListenerAndroid(
  callback: (nfcReaderState: NFCReaderState) => void
): { remove: () => void } | undefined {
  if (!ExpoEhcNfc) return;
  return ExpoEhcNfc.addListener(
    'onStatusChange',
    (nfcReaderState: NFCReaderStateAndroid) => {
      const parsedState = {
        ...nfcReaderState,
        value: JSON.parse(nfcReaderState.value ?? '{}'),
      };
      callback(parsedState);
    }
  );
}
