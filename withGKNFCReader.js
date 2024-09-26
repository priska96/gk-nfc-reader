const {
  withInfoPlist,
  withEntitlementsPlist,
  withXcodeProject,
  createRunOncePlugin,
} = require('@expo/config-plugins');

const withGKNFCReader = config => {
  // iOS Configuration
  config = withInfoPlist(config, config => {
    config.modResults['NFCReaderUsageDescription'] = 'NFC Test';
    config.modResults[
      'com.apple.developer.nfc.readersession.iso7816.select-identifiers'
    ] = [
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

  config = withEntitlementsPlist(config, config => {
    // Modify the entitlements plist here
    config.modResults['com.apple.developer.nfc.readersession.formats'] = [
      'TAG',
    ];

    return config;
  });

  config = withXcodeProject(config, config => {
    const deploymentTarget = '14.0';
    const {project} = config.modResults;

    // Update the iOS deployment target in the Xcode project
    project.updateBuildProperty(
      'IPHONEOS_DEPLOYMENT_TARGET',
      deploymentTarget,
      'Release',
    );
    project.updateBuildProperty(
      'IPHONEOS_DEPLOYMENT_TARGET',
      deploymentTarget,
      'Debug',
    );

    return config;
  });
  return config;
};

// Plugin metadata to avoid re-running the plugin multiple times
module.exports = createRunOncePlugin(
  withGKNFCReader,
  'with-gk-nfc-reader',
  '1.0.9',
);
