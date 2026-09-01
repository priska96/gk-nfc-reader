import { NFCReaderState, ReadPersonalDataOptions } from './module/ExpoEhcNfc.types';
export declare const useReadPersonalData: () => {
    nfcReaderState: NFCReaderState;
    readPersonalData: ({ can, pin, checkBrainpoolAlgorithm, }: ReadPersonalDataOptions) => Promise<void>;
};
//# sourceMappingURL=useReadPersonalData.d.ts.map