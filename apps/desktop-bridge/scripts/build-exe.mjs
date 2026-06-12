import { copyFile, mkdir, rm, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
import { build } from "esbuild";

const bridgeRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = resolve(bridgeRoot, "../..");
const workDir = join(bridgeRoot, ".exe-build");
const releaseDir = join(bridgeRoot, "release");
const bundlePath = join(workDir, "bridge.cjs");
const seaConfigPath = join(workDir, "sea-config.json");
const seaBlobPath = join(workDir, "bridge.blob");
const exePath = join(releaseDir, "CodeCCX-Bridge.exe");
const npmCli = process.env.npm_execpath || join(dirname(process.execPath), "node_modules", "npm", "bin", "npm-cli.js");
const postjectCli = join(repoRoot, "node_modules", "postject", "dist", "cli.js");

await rm(workDir, { recursive: true, force: true });
await mkdir(workDir, { recursive: true });
await mkdir(releaseDir, { recursive: true });

run(process.execPath, [npmCli, "--workspace", "@codex-companion/protocol", "run", "build"]);

await build({
  entryPoints: [join(bridgeRoot, "src", "index.ts")],
  outfile: bundlePath,
  bundle: true,
  platform: "node",
  format: "cjs",
  target: "node20",
  sourcemap: false,
  logLevel: "info"
});

await writeFile(
  seaConfigPath,
  `${JSON.stringify(
    {
      main: bundlePath,
      output: seaBlobPath,
      disableExperimentalSEAWarning: true,
      useCodeCache: true,
      assets: {
        "public/index.html": join(bridgeRoot, "public", "index.html"),
        "public/app.js": join(bridgeRoot, "public", "app.js"),
        "public/styles.css": join(bridgeRoot, "public", "styles.css")
      }
    },
    null,
    2
  )}\n`,
  "utf8"
);

run(process.execPath, ["--experimental-sea-config", seaConfigPath]);
await copyFile(process.execPath, exePath);

if (!existsSync(postjectCli)) {
  throw new Error(`postject was not found at ${postjectCli}. Run npm install first.`);
}

run(process.execPath, [
  postjectCli,
  exePath,
  "NODE_SEA_BLOB",
  seaBlobPath,
  "--sentinel-fuse",
  "NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2",
  "--overwrite"
]);

console.log(`\nCreated ${exePath}`);

function run(command, args) {
  const result = spawnSync(command, args, {
    cwd: repoRoot,
    stdio: "inherit"
  });
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(" ")} failed with exit code ${result.status ?? "unknown"}`);
  }
}
