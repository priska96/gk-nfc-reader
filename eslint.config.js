const { createRequire } = require('module');

const expoRequire = createRequire(
  require.resolve('expo-module-scripts/package.json')
);
const universeRequire = createRequire(
  expoRequire.resolve('eslint-config-universe/package.json')
);
const typescriptParser = universeRequire.resolve('@typescript-eslint/parser');

// eslint-plugin-import resolves configured parsers from the project root, while
// npm installs Expo's parser below expo-module-scripts. Give the import plugin
// the resolved parser path so imported TypeScript modules can be analyzed.
const nativeConfig = expoRequire('eslint-config-universe/flat/native').map(
  (config) => {
    const parsers = config.settings?.['import/parsers'];

    if (!parsers?.['@typescript-eslint/parser']) {
      return config;
    }

    const { ['@typescript-eslint/parser']: extensions, ...otherParsers } =
      parsers;

    return {
      ...config,
      settings: {
        ...config.settings,
        'import/parsers': {
          ...otherParsers,
          [typescriptParser]: extensions,
        },
      },
    };
  }
);

module.exports = [
  ...nativeConfig,
  {
    files: ['src/**/*.{js,ts,tsx}', 'plugin/src/**/*.{js,ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'warn',
        {
          patterns: ['fbjs/*', 'fbjs'],
        },
      ],
      'no-redeclare': 'off',
      '@typescript-eslint/no-redeclare': 'off',
      'no-unused-expressions': 'off',
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-expressions': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
    },
  },
];
