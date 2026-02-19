#!/usr/bin/env node
import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';

const require = createRequire(import.meta.url);
const INSTALL_TIMEOUT_MS = Number(process.env.FRONTEND_NATIVE_DEPS_TIMEOUT_MS ?? '120000');

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
    esbuild: {
      packageName: '@esbuild/darwin-arm64',
      installSpec: '@esbuild/darwin-arm64@0.27.3',
    },
    rollup: {
      packageName: '@rollup/rollup-darwin-arm64',
      installSpec: '@rollup/rollup-darwin-arm64@4.57.1',
    },
    tailwindOxide: {
      packageName: '@tailwindcss/oxide-darwin-arm64',
      installSpec: '@tailwindcss/oxide-darwin-arm64@4.1.18',
    },
    lightningcss: {
      packageName: 'lightningcss-darwin-arm64',
      installSpec: 'lightningcss-darwin-arm64@1.30.2',
    },
  },
  'darwin-x64': {
    esbuild: {
      packageName: '@esbuild/darwin-x64',
      installSpec: '@esbuild/darwin-x64@0.27.3',
    },
    rollup: {
      packageName: '@rollup/rollup-darwin-x64',
      installSpec: '@rollup/rollup-darwin-x64@4.57.1',
    },
    tailwindOxide: {
      packageName: '@tailwindcss/oxide-darwin-x64',
      installSpec: '@tailwindcss/oxide-darwin-x64@4.1.18',
    },
    lightningcss: {
      packageName: 'lightningcss-darwin-x64',
      installSpec: 'lightningcss-darwin-x64@1.30.2',
    },
  },
  'linux-arm64-gnu': {
    esbuild: {
      packageName: '@esbuild/linux-arm64',
      installSpec: '@esbuild/linux-arm64@0.27.3',
    },
    rollup: {
      packageName: '@rollup/rollup-linux-arm64-gnu',
      installSpec: '@rollup/rollup-linux-arm64-gnu@4.57.1',
    },
    tailwindOxide: {
      packageName: '@tailwindcss/oxide-linux-arm64-gnu',
      installSpec: '@tailwindcss/oxide-linux-arm64-gnu@4.1.18',
    },
    lightningcss: {
      packageName: 'lightningcss-linux-arm64-gnu',
      installSpec: 'lightningcss-linux-arm64-gnu@1.30.2',
    },
  },
  'linux-arm64-musl': {
    esbuild: {
      packageName: '@esbuild/linux-arm64',
      installSpec: '@esbuild/linux-arm64@0.27.3',
    },
    rollup: {
      packageName: '@rollup/rollup-linux-arm64-musl',
      installSpec: '@rollup/rollup-linux-arm64-musl@4.57.1',
    },
    tailwindOxide: {
      packageName: '@tailwindcss/oxide-linux-arm64-musl',
      installSpec: '@tailwindcss/oxide-linux-arm64-musl@4.1.18',
    },
    lightningcss: {
      packageName: 'lightningcss-linux-arm64-musl',
      installSpec: 'lightningcss-linux-arm64-musl@1.30.2',
    },
  },
  'linux-x64-gnu': {
    esbuild: {
      packageName: '@esbuild/linux-x64',
      installSpec: '@esbuild/linux-x64@0.27.3',
    },
    rollup: {
      packageName: '@rollup/rollup-linux-x64-gnu',
      installSpec: '@rollup/rollup-linux-x64-gnu@4.57.1',
    },
    tailwindOxide: {
      packageName: '@tailwindcss/oxide-linux-x64-gnu',
      installSpec: '@tailwindcss/oxide-linux-x64-gnu@4.1.18',
    },
    lightningcss: {
      packageName: 'lightningcss-linux-x64-gnu',
      installSpec: 'lightningcss-linux-x64-gnu@1.30.2',
    },
  },
  'linux-x64-musl': {
    esbuild: {
      packageName: '@esbuild/linux-x64',
      installSpec: '@esbuild/linux-x64@0.27.3',
    },
    rollup: {
      packageName: '@rollup/rollup-linux-x64-musl',
      installSpec: '@rollup/rollup-linux-x64-musl@4.57.1',
    },
    tailwindOxide: {
      packageName: '@tailwindcss/oxide-linux-x64-musl',
      installSpec: '@tailwindcss/oxide-linux-x64-musl@4.1.18',
    },
    lightningcss: {
      packageName: 'lightningcss-linux-x64-musl',
      installSpec: 'lightningcss-linux-x64-musl@1.30.2',
    },
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

const targetPackages = Object.values(targets);
const requiredPackages = targetPackages.map((pkg) => pkg.packageName);
const installSpecs = targetPackages.map((pkg) => pkg.installSpec);
const missingPackages = requiredPackages.filter((pkgName) => !exists(pkgName));

if (missingPackages.length === 0) {
  console.log(`[ok] frontend native deps present for ${key}`);
  process.exit(0);
}

console.log(
  `[fix] Installing frontend native deps for ${key}: ${requiredPackages.join(', ')} (missing: ${missingPackages.join(', ')})`,
);

const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const result = spawnSync(
  npmCommand,
  ['i', '--no-save', '--no-audit', '--no-fund', ...installSpecs],
  {
  stdio: 'inherit',
    timeout: INSTALL_TIMEOUT_MS,
  },
);

if (result.error?.code === 'ETIMEDOUT') {
  console.error(
    `[error] Timed out after ${INSTALL_TIMEOUT_MS}ms while installing native deps for ${key}.`,
  );
  console.error(`[hint] Try running manually: ${npmCommand} i --no-save --no-audit --no-fund ${installSpecs.join(' ')}`);
  process.exit(1);
}

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}

console.log('[ok] frontend native deps installed');
