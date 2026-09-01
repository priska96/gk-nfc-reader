import { ConfigPlugin, withInfoPlist } from 'expo/config-plugins';

/**
 * this plugin adds the languages to the Info.plist and sets the default language
 * @param config current configPlugin
 * @param languages string array of languages. The first language is the default language
 * @returns updated configPlugin
 */
export const withAddLanguages: ConfigPlugin<string[]> = (config, languages) => {
  return withInfoPlist(config, (config) => {
    config.modResults['CFBundleDevelopmentRegion'] = languages[0]; // default language is the first one
    config.modResults['CFBundleLocalizations'] = languages;
    return config;
  });
};
