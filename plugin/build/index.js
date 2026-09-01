"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ios_1 = require("expo-build-properties/build/ios");
const withStaticDynamic_1 = require("./withStaticDynamic");
const withAddToPodfile_1 = require("./withAddToPodfile");
const withXcode15BugFix_1 = require("./withXcode15BugFix");
const withAddNFCIdentifiers_1 = require("./withAddNFCIdentifiers");
const withAddNFCEntitlements_1 = require("./withAddNFCEntitlements");
const withAddLanguages_1 = require("./withAddLanguages");
const withCopyResources_1 = require("./withCopyResources");
const withAndroidGradleProperties_1 = require("./withAndroidGradleProperties");
const withAddToExpoSqlitePodspec_1 = require("./withAddToExpoSqlitePodspec");
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
const withGKNFCReader = (config, { nfcReaderUsageDescription, deploymentTarget, staticLibraryList, languages, development = false, minSdkVersion = 30, kotlinVersion = '2.0.21', }) => {
    config = (0, withAddNFCIdentifiers_1.withAddNFCIdentifiers)(config, nfcReaderUsageDescription);
    config = (0, withAddNFCEntitlements_1.withAddNFCEntitlements)(config);
    // only needed when developing the plugin locally
    if (development) {
        config = (0, withAddToPodfile_1.withAddToPodfile)(config);
    }
    config = (0, withAddToExpoSqlitePodspec_1.withAddToExpoSqlitePodspec)(config);
    config = (0, ios_1.withIosDeploymentTarget)(config, { ios: { deploymentTarget } });
    if (staticLibraryList) {
        config = (0, withStaticDynamic_1.withStaticDynamic)(config, staticLibraryList);
    }
    if (languages) {
        config = (0, withAddLanguages_1.withAddLanguages)(config, languages);
    }
    config = (0, withCopyResources_1.withCopyResources)(config, development);
    config = (0, withXcode15BugFix_1.withXcode15BugFix)(config);
    config = (0, withAndroidGradleProperties_1.withAndroidGradleProperties)(config, {
        minSdkVersion,
        kotlinVersion,
    });
    return config;
};
exports.default = withGKNFCReader;
