import { ConfigPlugin } from 'expo/config-plugins';
/**
 * this plugin adds the languages to the Info.plist and sets the default language
 * @param config current configPlugin
 * @param languages string array of languages. The first language is the default language
 * @returns updated configPlugin
 */
export declare const withAddLanguages: ConfigPlugin<string[]>;
