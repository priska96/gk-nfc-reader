import { ConfigPlugin } from '@expo/config-plugins';
export type AndroidGradleProperties = {
    minSdkVersion?: number;
    kotlinVersion?: string;
};
/**
 * A config plugin that modifies Android Gradle properties for Expo projects.
 *
 * This plugin performs the following actions:
 * - Removes any existing `android.packagingOptions.pickFirsts` property and adds a new one
 *   with the value `META-INF/versions/9/OSGI-INF/MANIFEST.MF`.
 * - Removes any existing `android.minSdkVersion` property and sets it to the provided `minSdkVersion`
 *   (defaults to 30 if not specified).
 *
 * @param config - The Expo configuration object.
 * @param AndroidGradleProperties - The minimum SDK version to set for Android. Defaults to 30 if not provided. Also the kotlin version to set for Android. Defaults to 2.0.21 if not provided.
 * @returns The modified Expo configuration object with updated Gradle properties.
 */
export declare const withAndroidGradleProperties: ConfigPlugin<AndroidGradleProperties>;
