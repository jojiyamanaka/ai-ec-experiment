#!/usr/bin/env node
import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';

const require = createRequire(import.meta.url);

function detectLibc() {
  if (process.platform !== 'linux') return null;
  const report = process.report?.getReport?.();
  return report?.header?.glibcVersionRuntime ? 'gnu' : 'musl';
}

function targetKey() {
  if (process.platform === 'linux') {
    return `linux-${process.arch}-${detectLibc()}`;
  }
  return `${process.platform}-${process.arch}`;
}

const nativeDepsByTarget = {
  'darwin-arm64': {
    esbuild: '@esbuild/darwin-arm64',
    rollup: '@rollup/rollup-darwin-arm64',
    tailwindOxide: '@tailwindcss/oxide-darwin-arm64',
    lightningcss: 'lightningcss-darwin-arm64',
  },
  'darwin-x64': {
    esbuild: '@esbuild/darwin-x64',
    rollup: '@rollup/rollup-darwin-x64',
    tailwindOxide: '@tailwindcss/oxide-darwin-x64',
    lightningcss: 'lightningcss-darwin-x64',
  },
  'linux-arm64-gnu': {
    esbuild: '@esbuild/linux-arm64',
    rollup: '@rollup/rollup-linux-arm64-gnu',
    tailwindOxide: '@tailwindcss/oxide-linux-arm64-gnu',
    lightningcss: 'lightningcss-linux-arm64-gnu',
  },
  'linux-arm64-musl': {
    esbuild: '@esbuild/linux-arm64',
    rollup: '@rollup/rollup-linux-arm64-musl',
    tailwindOxide: '@tailwindcss/oxide-linux-arm64-musl',
    lightningcss: 'lightningcss-linux-arm64-musl',
  },
  'linux-x64-gnu': {
    esbuild: '@esbuild/linux-x64',
    rollup: '@rollup/rollup-linux-x64-gnu',
    tailwindOxide: '@tailwindcss/oxide-linux-x64-gnu',
    lightningcss: 'lightningcss-linux-x64-gnu',
  },
  'linux-x64-musl': {
    esbuild: '@esbuild/linux-x64',
    rollup: '@rollup/rollup-linux-x64-musl',
    tailwindOxide: '@tailwindcss/oxide-linux-x64-musl',
    lightningcss: 'lightningcss-linux-x64-musl',
  },
};

function exists(pkgName) {
  try {
    require.resolve(`${pkgName}/package.json`, { paths: [process.cwd()] });
    return true;
  } catch {
    return false;
  }
}

const key = targetKey();
const targets = nativeDepsByTarget[key];

if (!targets) {
  console.log(`[skip] No frontend native dependency mapping for ${key}`);
  process.exit(0);
}

const requiredPackages = Object.values(targets);
const missingPackages = requiredPackages.filter((pkgName) => !exists(pkgName));

if (missingPackages.length === 0) {
  console.log(`[ok] frontend native deps present for ${key}`);
  process.exit(0);
}

console.log(
  `[fix] Installing frontend native deps for ${key}: ${requiredPackages.join(', ')} (missing: ${missingPackages.join(', ')})`,
);

const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const result = spawnSync(npmCommand, ['i', '--no-save', ...requiredPackages], {
  stdio: 'inherit',
});

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}

console.log('[ok] frontend native deps installed');
