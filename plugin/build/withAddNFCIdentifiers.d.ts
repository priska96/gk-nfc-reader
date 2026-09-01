import { ConfigPlugin } from 'expo/config-plugins';
/**
 * this function sets the NFC reader usage description and adds the NFC identifiers in the Info.plist
 * @param config current configPlugin
 * @param nfcReaderUsageDescription string description for the NFC reader usage field in the Info.plist
 * @returns updated configPlugin
 */
export declare const withAddNFCIdentifiers: ConfigPlugin<string>;
