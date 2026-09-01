"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withStaticDynamic = void 0;
const config_plugins_1 = require("expo/config-plugins");
const path_1 = require("path");
const fs_1 = require("fs");
/**
 * this function adds a pre_install hook to the Podfile to set the build type of the static libraries.
 * By default all libraries dynamic use_frameworks
 * @param config current configPlugin
 * @param staticLibraryList string list of static libraries
 * @returns updated configPlugin
 */
const withStaticDynamic = (config, staticLibraryList) => {
    return (0, config_plugins_1.withDangerousMod)(config, [
        'ios',
        (cfg) => {
            const { platformProjectRoot } = cfg.modRequest;
            const podfile = (0, path_1.resolve)(platformProjectRoot, 'Podfile');
            const contents = (0, fs_1.readFileSync)(podfile, 'utf-8');
            (0, fs_1.writeFileSync)(podfile, addUseStatic(contents, staticLibraryList));
            return cfg;
        },
    ]);
};
exports.withStaticDynamic = withStaticDynamic;
const addUseStatic = (contents, libraries) => {
    // Split the file into lines
    const lines = contents.split('\n');
    const index = lines.findIndex((line) => /\s+post_install do \|installer\|/.test(line));
    // The lines to insert
    const newLines = [
        'pre_install do |installer|',
        '  installer.pod_targets.each do |pod|',
        '    # Array of library names to check',
        `    library_names = [${libraries.map((lib) => `'${lib}'`).join(', ')}]`,
        '    # Check if pod.name is in the array',
        '    if library_names.include?(pod.name)',
        '      def pod.build_type',
        '        Pod::BuildType.static_library',
        '      end',
        '    end',
        '  end',
        'end',
    ];
    // Insert the new lines
    const modifiedContents = [
        ...lines.slice(0, index),
        ...newLines,
        ...lines.slice(index),
    ].join('\n');
    return modifiedContents;
};
