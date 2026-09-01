import { ConfigPlugin, withDangerousMod } from 'expo/config-plugins';
import { join, resolve } from 'path';
import { readFileSync, writeFileSync } from 'fs';

/**
 * this function adds the gk-nfc-reader pod to the Podfile of example app.
 * It's only needed when developing the plugin locally
 * @param config current configPlugin
 * @returns updated configPlugin
 */
export const withAddToExpoSqlitePodspec: ConfigPlugin = (config) => {
  return withDangerousMod(config, [
    'ios',
    (cfg) => {
      const { platformProjectRoot } = cfg.modRequest;
      const podspecPath = join(
        platformProjectRoot,
        '..',
        'node_modules',
        'expo-sqlite',
        'ios'
      );

      const podspec = resolve(podspecPath, 'ExpoSQLite.podspec');
      let contents = readFileSync(podspec, 'utf8');
      const lines = contents.split('\n');
      const index = lines.findIndex((line) =>
        /\s+s.dependency 'ExpoModulesCore'/.test(line)
      );

      writeFileSync(
        podspec,
        [
          ...lines.slice(0, index + 1),
          `  s.static_framework = true`,
          ...lines.slice(index + 1),
        ].join('\n')
      );

      return cfg;
    },
  ]);
};
