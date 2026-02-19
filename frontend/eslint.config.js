import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import boundaries from 'eslint-plugin-boundaries'
import importPlugin from 'eslint-plugin-import'
import { defineConfig, globalIgnores } from 'eslint/config'

const FSD_LAYERS = ['app', 'pages', 'widgets', 'features', 'entities', 'shared']

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    plugins: { boundaries, import: importPlugin },
    settings: {
      'boundaries/elements': FSD_LAYERS.map((layer) => ({
        type: layer,
        pattern: `src/${layer}/*`,
      })),
    },
    rules: {
      // ルール①: レイヤー間の依存方向（上位→下位のみ）
      'boundaries/element-types': [
        'error',
        {
          default: 'disallow',
          rules: [
            { from: 'app',      allow: ['pages', 'widgets', 'features', 'entities', 'shared'] },
            { from: 'pages',    allow: ['widgets', 'features', 'entities', 'shared'] },
            { from: 'widgets',  allow: ['features', 'entities', 'shared'] },
            { from: 'features', allow: ['entities', 'shared'] },
            { from: 'entities', allow: ['shared'] },
            { from: 'shared',   allow: [] },
          ],
        },
      ],
      // ルール②: スライス内部パスへの直接アクセス禁止（index.ts 経由を強制）
      'import/no-internal-modules': [
        'error',
        {
          allow: [
            '@app/**',
            '@pages/*/*',
            '@widgets/*',
            '@features/*',
            '@entities/*',
            '@shared/**',
            'react-dom/client',
          ],
        },
      ],
    },
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
  },
])
