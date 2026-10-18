import { spawnSync } from "node:child_process";
const windows = process.platform === "win32";
const result = spawnSync(windows ? "gradlew.bat" : "./gradlew", ["test"], { cwd: new URL("../apps/backend/", import.meta.url), stdio: "inherit", shell: windows });
process.exit(result.status ?? 1);
