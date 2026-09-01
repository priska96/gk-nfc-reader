import { ConfigPlugin } from 'expo/config-plugins';
/**
 * Configures the Expo project for NFC reader functionality using the GKNFCReader plugin.
 *
 * This config plugin sets up necessary iOS and Android configurations, including NFC entitlements,
 * deployment targets, static libraries, language support, resource copying, and platform-specific bug fixes.
 * It also provides options for local development and minimum Android SDK version.
 *
 * @param config - The existing Expo config to modify.
 * @param options - Configuration options for the plugin.
 * @param options.nfcReaderUsageDescription - Description for NFC reader usage (iOS privacy string).
 * @param options.deploymentTarget - Minimum iOS deployment target version.
 * @param options.staticLibraryList - Optional list of static libraries to link.
 * @param options.languages - Optional list of supported languages.
 * @param options.minSdkVersion - Optional minimum Android SDK version (default: 30).
 * @param options.kotlinVersion - Optional minimum Android SDK version (default: 30).
 * @param options.development - Whether to enable local development mode for the plugin.
 * @returns The modified Expo config with NFC reader support.
 */
declare const withGKNFCReader: ConfigPlugin<{
    nfcReaderUsageDescription: string;
    deploymentTarget: string;
    staticLibraryList?: string[];
    languages?: string[];
    minSdkVersion?: number;
    kotlinVersion?: string;
    development: boolean;
}>;
export default withGKNFCReader;
