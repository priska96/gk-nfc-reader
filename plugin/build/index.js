"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const config_plugins_1 = require("expo/config-plugins");
const ios_1 = require("expo-build-properties/build/ios");
const withGKNFCReader = (config, { nfcReaderUsageDescription, deploymentTarget }) => {
    config = (0, config_plugins_1.withInfoPlist)(config, (config) => {
        // Add your Info.plist modifications here
        config.modResults["NFCReaderUsageDescription"] = nfcReaderUsageDescription;
        config.modResults["com.apple.developer.nfc.readersession.iso7816.select-identifiers"] = [
            "D2760001448000",
            "D27600014601",
            "D27600014606",
            "D27600000102",
            "A000000167455349474E",
            "D27600006601",
            "D27600014602",
            "E828BD080FA000000167455349474E",
            "E828BD080FD27600006601",
            "D27600014603",
            "A0000002471001",
            "A00000030800001000",
        ];
        return config;
    });
    config = (0, config_plugins_1.withEntitlementsPlist)(config, (config) => {
        // Modify the entitlements plist here
        config.modResults["com.apple.developer.nfc.readersession.formats"] = [
            "TAG",
        ];
        return config;
    });
    // maybe only needed when developing the plugin locally
    /*config = withDangerousMod(config, [
      "ios",
      (cfg) => {
        const { platformProjectRoot } = cfg.modRequest;
        const podfile = resolve(platformProjectRoot, "Podfile");
        const contents = readFileSync(podfile, "utf-8");
        const lines = contents.split("\n");
        const index = lines.findIndex((line) =>
          /\s+use_expo_modules!/.test(line)
        );
  
        writeFileSync(
          podfile,
          [
            ...lines.slice(0, index),
            `  pod 'gk-nfc-reader', :path => '../..'`,
            ...lines.slice(index),
          ].join("\n")
        );
  
        return cfg;
      },
    ]);*/
    config = (0, ios_1.withIosDeploymentTarget)(config, { ios: { deploymentTarget } });
    return config;
};
exports.default = withGKNFCReader;
