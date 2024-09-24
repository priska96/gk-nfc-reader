declare module 'gk-nfc-reader' {
  export interface PersonalDataOptions {
    can: string;
    pin?: string;
    checkBrainpoolAlgorithm?: boolean;
  }
  export interface PState {
    state: 'idle' | 'loading' | 'value' | 'error';
    value?: boolean;
    error?: string;
  }

  export function readPersonalData(
    options: PersonalDataOptions,
  ): Promise<string>;
}
