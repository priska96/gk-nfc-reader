import { EventSubscription as ExpoSubscription } from 'expo-modules-core';
import {
  Platform,
  EventSubscription as RNEventSubscription,
} from 'react-native';

import { ReadPersonalDataOptions } from './ExpoEhcNfc.types';
import {
  readPersonalDataNativeAndroid,
  statusChangeListenerAndroid,
} from './ExpoEhcNfcModule';
import {
  readPersonalDataNativeIos,
  statusChangeListenerIos,
} from './RNEhcNfcModule';

export type AnySubscription = RNEventSubscription | ExpoSubscription;

export let readPersonalDataNative: (
  options: ReadPersonalDataOptions
) => Promise<string>;
export let statusChangeListener: (
  callback: (event: any) => void
) => AnySubscription | undefined;

if (Platform.OS === 'ios') {
  readPersonalDataNative = readPersonalDataNativeIos;
  statusChangeListener = statusChangeListenerIos;
}
if (Platform.OS === 'android') {
  readPersonalDataNative = readPersonalDataNativeAndroid;
  statusChangeListener = statusChangeListenerAndroid;
}
