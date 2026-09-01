import { ConfigPlugin } from 'expo/config-plugins';
/**
 * this function adds a pre_install hook to the Podfile to set the build type of the static libraries.
 * By default all libraries dynamic use_frameworks
 * @param config current configPlugin
 * @param staticLibraryList string list of static libraries
 * @returns updated configPlugin
 */
export declare const withStaticDynamic: ConfigPlugin<string[]>;
