# Repository Guidelines

## Project Structure & Module Organization

This repository is a Java 8 multi-module Maven project with a Vue 3 frontend. The root `pom.xml` aggregates backend modules such as `datamaster-common`, `datamaster-system`, `datamaster-assets`, `datamaster-collector`, `datamaster-service`, `datamaster-catalog`, `datamaster-quality`, `datamaster-etl`, and `datamaster-server`. The backend entry point is `datamaster-server/src/main/java/com/datamaster/server/DataMasterApplication.java`.

Backend source follows Maven layout: `src/main/java`, `src/main/resources`, and `src/test`. Frontend code lives in `datamaster-view/src`; Vite configuration is in `datamaster-view/vite.config.js` and plugins are under `datamaster-view/vite`. Database scripts are under `sql/`, deployment assets under `docker/`, and runtime uploads under `upload/`.

## Build, Test, and Development Commands

- `mvn clean package`: build all Maven modules from the repository root.
- `mvn test`: run backend tests across modules.
- `mvn -pl datamaster-server -am package`: build the server and required dependencies.
- `mvn -pl datamaster-etl test`: run ETL tests, including JUnit 5 tests configured there.
- `cd datamaster-view && npm run dev`: start the Vite frontend dev server.
- `cd datamaster-view && npm run build:prod`: produce the production frontend build.
- `cd datamaster-view && npm run eslint:lint`: lint frontend JavaScript and Vue files.

## Coding Style & Naming Conventions

Use UTF-8 and Java 8-compatible code. Keep Java package names under `com.datamaster`, matching the owning module. Follow existing suffixes: `*Controller`, `*Service`, `*Mapper`, `*Convert`, and `*DO`. Vue files use the established style in `datamaster-view/src`; run ESLint before submitting frontend changes.

## Testing Guidelines

Backend modules use JUnit dependencies, with `datamaster-etl` configured for JUnit 5. Place tests in the matching module under `src/test/java` and name them after the unit under test, for example `ValueParserUtilsTest`. Add focused tests for parsing, SQL generation, ETL behavior, and service logic. No frontend test runner is defined; use linting and production build as minimum verification.

Do not proactively run compilation, build, lint, or test commands after making changes unless the user explicitly requests verification. The user will run verification locally.

## Commit & Pull Request Guidelines

This checkout does not include `.git`, so local history conventions are unavailable. Use short, imperative commit subjects such as `Fix asset mapper null handling` or `Add catalog task validation`. Pull requests should describe the change, affected modules, database or configuration impacts, and verification commands. Include screenshots for visible frontend changes.

## Security & Configuration Tips

Do not commit secrets, generated logs, or local runtime files. Treat `application-*-dev.yml`, `application-*-prod.yml`, Docker configs, and SQL migrations as environment-sensitive. Keep generated artifacts such as `target/`, `dist/`, and `node_modules/` out of review unless explicitly required.

## Local DolphinScheduler Runtime

The local DolphinScheduler environment runs as Docker containers inside the `Ubuntu` WSL distribution. Use `wsl -d Ubuntu -e docker ...` from PowerShell when inspecting or restarting components.

The DolphinScheduler containers are:

- `dolphinscheduler-zookeeper`
- `dolphinscheduler-master`
- `dolphinscheduler-worker`
- `dolphinscheduler-alert`
- `dolphinscheduler-api`

CHUNJUN tasks execute in `dolphinscheduler-worker`. Its important bind mounts are:

- `D:\dev\package\dolphinscheduler\soft` -> `/opt/soft`
- `D:\dev\package\dolphinscheduler\conf\worker` -> `/opt/dolphinscheduler/conf`
- `D:\dev\package\dolphinscheduler\logs` -> `/opt/dolphinscheduler/logs`
- `D:\dev\package\dolphinscheduler\resource` -> `/dolphinscheduler`

The worker exports `CHUNJUN_HOME=/opt/soft/chunjun` and `FLINK_HOME=/opt/soft/flink`. The latter is a symlink to `/opt/soft/flink-1.16.2`. CHUNJUN is launched by `/opt/soft/chunjun/bin/start-chunjun`. That wrapper initializes `FLINK_LOG_DIR` because it invokes Java directly instead of using Flink's own launcher script. It also supplies `classloader.parent-first-patterns.additional=com.dtstack.chunjun.` through `-confProp` for local mode because CHUNJUN does not load `FLINK_HOME/conf/flink-conf.yaml` on that path.

The mounted CHUNJUN binary requires a local compatibility patch for Flink 1.16.2: `PluginUtil.setPipelineOptionsToEnvConfig()` must store local jar paths in `pipeline.jars` as `file:` URLs. The patched jar is `D:\dev\package\dolphinscheduler\soft\chunjun\chunjun-core.jar`; the original backup is `chunjun-core.jar.bak-before-flink116-url-fix` in the same directory.
