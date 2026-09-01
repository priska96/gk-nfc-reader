"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withAddToPodfile = void 0;
const config_plugins_1 = require("expo/config-plugins");
const path_1 = require("path");
const fs_1 = require("fs");
/**
 * this function adds the gk-nfc-reader pod to the Podfile of example app.
 * It's only needed when developing the plugin locally
 * @param config current configPlugin
 * @returns updated configPlugin
 */
const withAddToPodfile = (config) => {
    return (0, config_plugins_1.withDangerousMod)(config, [
        'ios',
        (cfg) => {
            const { platformProjectRoot } = cfg.modRequest;
            const podfile = (0, path_1.resolve)(platformProjectRoot, 'Podfile');
            const contents = (0, fs_1.readFileSync)(podfile, 'utf-8');
            const lines = contents.split('\n');
            const index = lines.findIndex((line) => /\s+use_expo_modules!/.test(line));
            (0, fs_1.writeFileSync)(podfile, [
                ...lines.slice(0, index),
                `  pod 'ExpoEhcNfc', :path => '../../ios', :testspecs => ['Tests']`,
                ...lines.slice(index),
            ].join('\n'));
            return cfg;
        },
    ]);
};
exports.withAddToPodfile = withAddToPodfile;
