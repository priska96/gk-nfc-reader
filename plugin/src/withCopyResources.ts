import {
  ConfigPlugin,
  IOSConfig,
  withXcodeProject,
  XcodeProject,
} from 'expo/config-plugins';
import path from 'path';

export const withCopyResources: ConfigPlugin<boolean> = (
  config,
  development
) => {
  const envPath = !development
    ? '../node_modules/gk-nfc-reader/ios/Resources'
    : '../../ios/Resources';

  return withXcodeProject(config, async (cfg) => {
    const resolvedFonts = ['de.lproj', 'en.lproj'].map((lang) =>
      path.join(
        cfg.modRequest.platformProjectRoot,
        `${envPath}/${lang}/Localizable.strings`
      )
    );
    const project = cfg.modResults;
    const platformProjectRoot = cfg.modRequest.platformProjectRoot;
    IOSConfig.XcodeUtils.ensureGroupRecursively(project, 'Resources');

    // todo: figure out how to delete existing en.lproj/Localizable.strings file
    // » ios: ios-xcode-project: Skipped adding duplicate file "../../Resources/en.lproj/Localizable.strings" to PBXGroup named "Resources"
    // const fontPath = path.relative(
    //   platformProjectRoot,
    //   path.join(
    //     cfg.modRequest.platformProjectRoot,
    //     `${envPath}/en.lproj/Localizable.strings`
    //   )
    // );

    // const group = project.pbxGroupByName("Resources");
    // console.log("group", group);
    // const conflictingFile = group.children.find(
    //   //@ts-ignore
    //   (child) => child.comment === path.basename(fontPath)
    // );
    // console.log("conflicting ", conflictingFile);
    // if (conflictingFile) {
    //   project.removeResourceFile(conflictingFile);
    // }
    addResourceFile(project, platformProjectRoot, resolvedFonts);
    return cfg;
  });
};

function addResourceFile(
  project: XcodeProject,
  platformRoot: string,
  f: string[]
) {
  for (const font of f) {
    const fontPath = path.relative(platformRoot, font);
    IOSConfig.XcodeUtils.addResourceFileToGroup({
      filepath: fontPath,
      groupName: 'Resources',
      project,
      isBuildFile: true,
      verbose: true,
    });
  }
}
