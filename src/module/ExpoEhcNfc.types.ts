export type ExpoEhcNfcModuleEvents = {
  onStatusChange(event: any): void;
};

export type ReadPersonalDataOptions = {
  can: string;
  pin?: string;
  checkBrainpoolAlgorithm?: boolean;
};
export interface PersonalData {
  title: string;
  lastname: string;
  firstname: string;
  nameAddition: string;
  birthday: string;
  gender: string;
  street: string;
  housenumber: string;
  zipCode: string;
  city: string;
  countryCode: string;
  insuranceId: string;
}

export enum State {
  idle = 'idle',
  loading = 'loading',
  success = 'success',
  failure = 'failure',
}
export interface NFCReaderState {
  state: State;
  value?: PersonalData;
  error?: string;
}
export interface NFCReaderStateAndroid {
  state: State;
  value?: string;
  error?: string;
}
