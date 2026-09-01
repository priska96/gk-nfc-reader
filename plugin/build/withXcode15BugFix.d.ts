import { ConfigPlugin } from 'expo/config-plugins';
/**
 * this function adds a build phase to remove signature files for Xcode >=15.
 * It's a bug in xCode where signature files cannot be copied due to already existing files.
 * @param config current configPlugin
 * @returns updated configPlugin
 */
export declare const withXcode15BugFix: ConfigPlugin;
