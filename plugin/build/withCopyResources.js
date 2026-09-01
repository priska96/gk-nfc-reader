"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.withCopyResources = void 0;
const config_plugins_1 = require("expo/config-plugins");
const path_1 = __importDefault(require("path"));
const withCopyResources = (config, development) => {
    const envPath = !development
        ? '../node_modules/gk-nfc-reader/ios/Resources'
        : '../../ios/Resources';
    return (0, config_plugins_1.withXcodeProject)(config, async (cfg) => {
        const resolvedFonts = ['de.lproj', 'en.lproj'].map((lang) => path_1.default.join(cfg.modRequest.platformProjectRoot, `${envPath}/${lang}/Localizable.strings`));
        const project = cfg.modResults;
        const platformProjectRoot = cfg.modRequest.platformProjectRoot;
        config_plugins_1.IOSConfig.XcodeUtils.ensureGroupRecursively(project, 'Resources');
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
exports.withCopyResources = withCopyResources;
function addResourceFile(project, platformRoot, f) {
    for (const font of f) {
        const fontPath = path_1.default.relative(platformRoot, font);
        config_plugins_1.IOSConfig.XcodeUtils.addResourceFileToGroup({
            filepath: fontPath,
            groupName: 'Resources',
            project,
            isBuildFile: true,
            verbose: true,
        });
    }
}
