"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withAddNFCIdentifiers = void 0;
const config_plugins_1 = require("expo/config-plugins");
/**
 * this function sets the NFC reader usage description and adds the NFC identifiers in the Info.plist
 * @param config current configPlugin
 * @param nfcReaderUsageDescription string description for the NFC reader usage field in the Info.plist
 * @returns updated configPlugin
 */
const withAddNFCIdentifiers = (config, nfcReaderUsageDescription) => {
    return (0, config_plugins_1.withInfoPlist)(config, (config) => {
        // Add your Info.plist modifications here
        config.modResults['NFCReaderUsageDescription'] = nfcReaderUsageDescription;
        config.modResults['com.apple.developer.nfc.readersession.iso7816.select-identifiers'] = [
            'D2760001448000',
            'D27600014601',
            'D27600014606',
            'D27600000102',
            'A000000167455349474E',
            'D27600006601',
            'D27600014602',
            'E828BD080FA000000167455349474E',
            'E828BD080FD27600006601',
            'D27600014603',
            'A0000002471001',
            'A00000030800001000',
        ];
        return config;
    });
};
exports.withAddNFCIdentifiers = withAddNFCIdentifiers;
