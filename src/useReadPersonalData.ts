import { useEffect, useState } from 'react';

import { readPersonalDataNative, statusChangeListener } from './module';
import {
  NFCReaderState,
  ReadPersonalDataOptions,
  State,
} from './module/ExpoEhcNfc.types';

export const useReadPersonalData = () => {
  const [nfcReaderState, setNfcReaderState] = useState<NFCReaderState>({
    state: State.idle,
    value: undefined,
    error: undefined,
  });

  useEffect(() => {
    // Subscribe to the event emitter
    // Update the local state with the received event
    const subscription = statusChangeListener(setNfcReaderState);

    // Clean up the subscription on component unmount
    return () => {
      subscription?.remove();
    };
  }, [statusChangeListener]);

  const readPersonalData = async ({
    can,
    pin,
    checkBrainpoolAlgorithm,
  }: ReadPersonalDataOptions) => {
    try {
      const res = await readPersonalDataNative({
        can,
        pin,
        checkBrainpoolAlgorithm,
      });
      console.log('success res:', res);
    } catch (error: unknown) {
      console.log('error', error);
    }
  };

  return { nfcReaderState, readPersonalData };
};
