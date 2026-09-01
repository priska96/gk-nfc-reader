import { useEffect, useState } from 'react';
import { readPersonalDataNative, statusChangeListener } from './module';
import { State, } from './module/ExpoEhcNfc.types';
export const useReadPersonalData = () => {
    const [nfcReaderState, setNfcReaderState] = useState({
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
    const readPersonalData = async ({ can, pin, checkBrainpoolAlgorithm, }) => {
        try {
            const res = await readPersonalDataNative({
                can,
                pin,
                checkBrainpoolAlgorithm,
            });
            console.log('success res:', res);
        }
        catch (error) {
            console.log('error', error);
        }
    };
    return { nfcReaderState, readPersonalData };
};
//# sourceMappingURL=useReadPersonalData.js.map