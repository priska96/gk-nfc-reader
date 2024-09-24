import {useEffect, useState} from 'react';
import RNNFCLoginController, {
  eventEmitter,
  PState,
  ReadPersonalDataOptions,
} from './RNNFCLoginController';

export const useReadPersonalData = () => {
  const [res, setRes] = useState('');
  const [state, setState] = useState<PState>({
    state: 'idle',
    value: false,
    error: '',
  });

  useEffect(() => {
    // Subscribe to the event emitter
    const subscription = eventEmitter.addListener('onStatusChange', event => {
      console.log('State updated:', event);
      setState(event); // Update the local state with the received event
    });

    // Clean up the subscription on component unmount
    return () => {
      subscription.remove();
    };
  }, []);

  const readPersonalData = ({
    can,
    pin,
    checkBrainpoolAlgorithm,
  }: ReadPersonalDataOptions) => {
    RNNFCLoginController.readPersonalData({
      can,
      pin,
      checkBrainpoolAlgorithm,
    })
      .then((result: string) => {
        setRes(result); // Store the result in state
        console.log('success');
      })
      .catch((error: unknown) => {
        console.error('error', error);
      });
  };

  return {res, state, readPersonalData};
};
