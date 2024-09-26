import {
  withEntitlementsPlist,
  withInfoPlist,
  ConfigPlugin,
  withXcodeProject,
} from "expo/config-plugins";

const withGKNFCReader: ConfigPlugin<{
  nfcReaderUsageDescription: string;
  deploymentTarget: string;
}> = (config, { nfcReaderUsageDescription, deploymentTarget }) => {
  config = withInfoPlist(config, (config) => {
    // Add your Info.plist modifications here
    config.modResults["NFCReaderUsageDescription"] = nfcReaderUsageDescription;
    config.modResults[
      "com.apple.developer.nfc.readersession.iso7816.select-identifiers"
    ] = [
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

  config = withEntitlementsPlist(config, (config) => {
    // Modify the entitlements plist here
    config.modResults["com.apple.developer.nfc.readersession.formats"] = [
      "TAG",
    ];

    return config;
  });

  // config = withXcodeProject(config, (config) => {
  //   const { project } = config.modResults;
  //   if (!project) {
  //     throw new Error("Xcode project is not properly initialized.");
  //   }

  //   // Update the iOS Deployment Target
  //   const configurations = project.pbxXCBuildConfigurationSection();
  //   for (const key in configurations) {
  //     const configuration = configurations[key];
  //     if (typeof configuration.buildSettings !== "undefined") {
  //       // Set deployment target for each configuration (e.g., Debug, Release)
  //       configuration.buildSettings.IPHONEOS_DEPLOYMENT_TARGET =
  //         deploymentTarget;
  //     }
  //   }

  //   return config;
  // });

  return config;
};

export default withGKNFCReader;
