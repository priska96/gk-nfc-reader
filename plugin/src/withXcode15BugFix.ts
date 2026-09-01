import { ConfigPlugin, withXcodeProject } from 'expo/config-plugins';

/**
 * this function adds a build phase to remove signature files for Xcode >=15.
 * It's a bug in xCode where signature files cannot be copied due to already existing files.
 * @param config current configPlugin
 * @returns updated configPlugin
 */
export const withXcode15BugFix: ConfigPlugin = (config) => {
  return withXcodeProject(config, async (cfg) => {
    const xcodeProject = cfg.modResults;
    const shellScript = `if [ "$XCODE_VERSION_MAJOR" -ge "1500" ]; then # Use -ge for numeric comparison
      echo "Remove signature files (Xcode >=15 workaround)"
      find "$BUILD_DIR/\${CONFIGURATION}-iphoneos" -name "*.signature" -type f | xargs -r rm
    fi`;

    xcodeProject.addBuildPhase(
      [],
      'PBXShellScriptBuildPhase',
      'Fix Xcode >=15 Bug',
      null,
      {
        shellPath: '/bin/sh',
        shellScript,
      }
    );

    return cfg;
  });
};
