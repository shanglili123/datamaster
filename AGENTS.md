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

## Commit & Pull Request Guidelines

This checkout does not include `.git`, so local history conventions are unavailable. Use short, imperative commit subjects such as `Fix asset mapper null handling` or `Add catalog task validation`. Pull requests should describe the change, affected modules, database or configuration impacts, and verification commands. Include screenshots for visible frontend changes.

## Security & Configuration Tips

Do not commit secrets, generated logs, or local runtime files. Treat `application-*-dev.yml`, `application-*-prod.yml`, Docker configs, and SQL migrations as environment-sensitive. Keep generated artifacts such as `target/`, `dist/`, and `node_modules/` out of review unless explicitly required.
