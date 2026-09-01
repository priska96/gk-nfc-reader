"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withAddToExpoSqlitePodspec = void 0;
const config_plugins_1 = require("expo/config-plugins");
const path_1 = require("path");
const fs_1 = require("fs");
/**
 * this function adds the gk-nfc-reader pod to the Podfile of example app.
 * It's only needed when developing the plugin locally
 * @param config current configPlugin
 * @returns updated configPlugin
 */
const withAddToExpoSqlitePodspec = (config) => {
    return (0, config_plugins_1.withDangerousMod)(config, [
        'ios',
        (cfg) => {
            const { platformProjectRoot } = cfg.modRequest;
            const podspecPath = (0, path_1.join)(platformProjectRoot, '..', 'node_modules', 'expo-sqlite', 'ios');
            const podspec = (0, path_1.resolve)(podspecPath, 'ExpoSQLite.podspec');
            let contents = (0, fs_1.readFileSync)(podspec, 'utf8');
            const lines = contents.split('\n');
            const index = lines.findIndex((line) => /\s+s.dependency 'ExpoModulesCore'/.test(line));
            (0, fs_1.writeFileSync)(podspec, [
                ...lines.slice(0, index + 1),
                `  s.static_framework = true`,
                ...lines.slice(index + 1),
            ].join('\n'));
            return cfg;
        },
    ]);
};
exports.withAddToExpoSqlitePodspec = withAddToExpoSqlitePodspec;
