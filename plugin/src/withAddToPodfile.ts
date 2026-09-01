import { ConfigPlugin, withDangerousMod } from 'expo/config-plugins';
import { resolve } from 'path';
import { readFileSync, writeFileSync } from 'fs';

/**
 * this function adds the gk-nfc-reader pod to the Podfile of example app.
 * It's only needed when developing the plugin locally
 * @param config current configPlugin
 * @returns updated configPlugin
 */
export const withAddToPodfile: ConfigPlugin = (config) => {
  return withDangerousMod(config, [
    'ios',
    (cfg) => {
      const { platformProjectRoot } = cfg.modRequest;
      const podfile = resolve(platformProjectRoot, 'Podfile');
      const contents = readFileSync(podfile, 'utf-8');
      const lines = contents.split('\n');
      const index = lines.findIndex((line) =>
        /\s+use_expo_modules!/.test(line)
      );

      writeFileSync(
        podfile,
        [
          ...lines.slice(0, index),
          `  pod 'ExpoEhcNfc', :path => '../../ios', :testspecs => ['Tests']`,
          ...lines.slice(index),
        ].join('\n')
      );

      return cfg;
    },
  ]);
};
