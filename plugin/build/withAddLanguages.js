"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withAddLanguages = void 0;
const config_plugins_1 = require("expo/config-plugins");
/**
 * this plugin adds the languages to the Info.plist and sets the default language
 * @param config current configPlugin
 * @param languages string array of languages. The first language is the default language
 * @returns updated configPlugin
 */
const withAddLanguages = (config, languages) => {
    return (0, config_plugins_1.withInfoPlist)(config, (config) => {
        config.modResults['CFBundleDevelopmentRegion'] = languages[0]; // default language is the first one
        config.modResults['CFBundleLocalizations'] = languages;
        return config;
    });
};
exports.withAddLanguages = withAddLanguages;
