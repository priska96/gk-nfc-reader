"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withAddNFCEntitlements = void 0;
const config_plugins_1 = require("expo/config-plugins");
/**
 * this function adds the NFC entitlements formats to the entitlements plist
 * @param config current configPlugin
 * @returns updated configPlugin
 */
const withAddNFCEntitlements = (config) => {
    return (0, config_plugins_1.withEntitlementsPlist)(config, (config) => {
        // Modify the entitlements plist here
        config.modResults['com.apple.developer.nfc.readersession.formats'] = [
            'TAG',
        ];
        return config;
    });
};
exports.withAddNFCEntitlements = withAddNFCEntitlements;
