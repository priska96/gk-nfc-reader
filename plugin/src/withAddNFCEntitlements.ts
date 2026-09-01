import { ConfigPlugin, withEntitlementsPlist } from 'expo/config-plugins';

/**
 * this function adds the NFC entitlements formats to the entitlements plist
 * @param config current configPlugin
 * @returns updated configPlugin
 */
export const withAddNFCEntitlements: ConfigPlugin = (config) => {
  return withEntitlementsPlist(config, (config) => {
    // Modify the entitlements plist here
    config.modResults['com.apple.developer.nfc.readersession.formats'] = [
      'TAG',
    ];

    return config;
  });
};
