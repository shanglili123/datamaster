# DataMaster UI Implementation Plan

Last updated: 2026-05-30

This document is the implementation ledger for the `datamaster-ui` refactor. Every completed item should be recorded here with changed files and verification results.

## Status Legend

| Status | Meaning |
| --- | --- |
| Done | Implemented and verified. |
| In Progress | Currently being implemented. |
| Pending | Not started. |
| Blocked | Waiting for backend data, API, product decision, or other dependency. |

## Implementation Table

| No. | Area | Task | Status | Implementation Record | Verification |
| --- | --- | --- | --- | --- | --- |
| 1 | Project setup | Copy `datamaster-view` to `datamaster-ui` and rename package. | Done | Created `datamaster-ui`; updated `package.json` name and description. | `npm ci`; `npm run build:prod` passed. |
| 2 | UI standard | Write unified UI guideline combining DolphinScheduler interaction structure with a more polished DataMaster visual style. | Done | Added `docs/datamaster-ui-guideline.md`. | Document reviewed locally. |
| 3 | Theme foundation | Add global DataMaster UI theme tokens and shell styles. | Done | Added `src/assets/system/styles/datamaster-ui-theme.scss`; imported it in global style entry. | `npm run build:prod` passed. |
| 4 | Home page | Replace old home page with project workspace home. | Done | Rebuilt `src/views/sys/index.vue` with authorized project list, super-admin create action, project stats, table row counts, and project entry action. | Targeted ESLint passed; production build passed. |
| 5 | Home/project menu mode | Implement project-first menu logic. | Done | Updated `src/store/system/permission.js`; home shows only home/system/log menus, project mode loads project menus after entering a project. | Targeted ESLint passed; production build passed. |
| 6 | Menu restructuring | Merge base management into system management; rename system monitor to log management; rename data governance to metadata management; move quality menus under metadata management. | Done | Implemented route/menu normalization in `src/store/system/permission.js`. | Targeted ESLint passed; production build passed. |
| 7 | Top navigation compatibility | Make top navigation handle top-level routes without child menus. | Done | Updated `src/components/TopNav/index.vue`. | Targeted ESLint passed; production build passed. |
| 8 | Project selector logic | Stop auto-selecting the first project after login; only show project selector inside project workspace. | Done | Updated `src/layout/components/Navbar.vue`. | Production build passed; local dev server available at `http://localhost:82/`. |
| 9 | Route prefix migration | Use new frontend route prefixes according to README table-prefix mapping. | Done | Added `src/utils/moduleRoute.js`; normalized static routes, dynamic menu routes, project-entry routes, and legacy-route redirects. Added `/dg` to `/cat` compatibility for legacy data-governance routes. Updated guideline route-prefix rules. | Targeted ESLint passed; `npm run build:prod` passed. |
| 10 | Visual shell polish | Make global shell closer to DolphinScheduler workbench style while keeping DataMaster more refined. | Done | Added sidebar, navbar, tags-view, layout background, card and button refinements in `datamaster-ui-theme.scss`. | `npm run build:prod` passed. |
| 11 | Browser verification after login | Verify home and project-entry flow in the logged-in browser. | Pending | Need test with current user/project data through in-app browser. | Pending. |
| 12 | Backend menu data alignment | Update backend menu SQL/data so stored menu paths use new route prefixes directly. | Pending | Frontend currently normalizes old prefixes for compatibility. Backend SQL/menu rows still need migration. | Pending. |
| 13 | Static hidden route cleanup | Convert hidden/static routes from old prefixes to new prefixes at source files where practical. | Done | Updated static hidden route sources and primary page navigation strings in `datamaster-ui/src/router/**`, collection/asset/quality/service/model/standard/metadata pages, plus `/dg` legacy metadata routes. Directory imports and backend API paths were left unchanged. | `npm run build:prod` passed with existing warnings. |
| 14 | Module page UI refactor: data collection | Refactor collection/integration pages under `/col` to new UI standard. | Pending | Scope to be broken down by page after route/menu verification. | Pending. |
| 15 | Module page UI refactor: data modeling | Refactor modeling pages under `/mdl` to new UI standard. | Pending | Scope to be broken down by page after route/menu verification. | Pending. |
| 16 | Module page UI refactor: data service | Refactor service/API pages under `/svc` to new UI standard. | Pending | Scope to be broken down by page after route/menu verification. | Pending. |
| 17 | Module page UI refactor: metadata catalog | Refactor metadata pages under `/cat` and quality-task placement under metadata management. | Pending | Scope to be broken down by page after route/menu verification. | Pending. |
| 18 | Frontend warning cleanup | Gradually address existing `::v-deep`, missing `D.png`, CSS `.3-text`, and large chunk warnings. | Pending | These warnings are pre-existing and do not block current builds. | Pending. |
| 19 | Default login convenience | Pre-fill the refactor frontend login form with `admin` / `admin123` and keep remember-password enabled for faster local login. | Done | Updated `datamaster-ui/src/views/sys/login.vue`; default credentials now use the requested admin account while preserving captcha fields after cookie hydration. | `npm run build:prod` passed with existing warnings; browser check at `http://127.0.0.1:82/login` confirmed username `admin`, password `admin123`, and remember-password checked. |
| 20 | Project route direct access | Fix direct access 404 for project-scoped routes such as `/tax/rule/auditRule`. | Done | Updated `datamaster-ui/src/store/system/permission.js` so project menus loaded by `updateTopbarRoutes` are registered in Vue Router; updated `datamaster-ui/src/permission.js` to auto-load saved project routes for project-scoped direct URLs. | `npm run build:prod` passed with existing warnings. |
| 21 | Home API and project menu errors | Stop the new home page from calling the missing `/home` stats API until that backend contract exists; fix project menu registration by importing `isHttp` in `src/store/system/permission.js`; add backend and Vite dev `/home` compatibility fallbacks so cached or older frontend code no longer receives 404. | Done | Updated `datamaster-ui/src/views/sys/index.vue`, `datamaster-ui/src/store/system/permission.js`, `datamaster-ui/vite/plugins/index.js`, and added `datamaster-system/datamaster-system-biz/src/main/java/com/datamaster/module/system/controller/admin/system/SysHomeController.java`. | `npx eslint vite/plugins/index.js src/views/sys/index.vue src/store/system/permission.js` passed; `npm run build:prod` passed with existing warnings; `mvn -pl datamaster-system/datamaster-system-biz -am -DskipTests package` passed; restarted port 82 and verified `http://127.0.0.1:82/dev-api/home?projectId=1&projectCode=152317790975712` returns 200. Running IDEA backend must still be restarted to load the new backend `/home` endpoint. |
| 22 | API prefix migration | Convert new UI API request paths from old module prefixes to the new module prefixes and keep backend compatibility aligned. | Done | Rewrote `datamaster-ui/src/api/**` request URLs from `/da`, `/dpp`, `/ds`, `/dm`, `/mc`, `/dg`, `/dp`, `/att` to `/ast`, `/col`, `/svc`, `/mdl`, `/cat`, `/std`, `/tax`; added `datamaster-ui/src/utils/apiPrefix.js` and request interceptor normalization; updated `datamaster-ui/vite.config.js` dev proxy compatibility; added `datamaster-server/src/main/java/com/datamaster/server/config/ApiPrefixCompatibilityFilter.java` for backend new-prefix forwarding. `/cat` is disambiguated to old `/mc` or `/dg` by second path segment. | Frontend API scan found old-prefix count 0 and new-prefix counts: `/ast` 181, `/col` 106, `/svc` 20, `/mdl` 21, `/cat` 169, `/std` 57, `/tax` 134. `npx eslint vite.config.js src/utils/apiPrefix.js src/utils/request.js` passed; `npm run build:prod` passed with existing warnings; `mvn -pl datamaster-server -am -DskipTests package` passed. Restarted port 82 with a single Vite process and smoke-tested `/dev-api/ast/asset/list`, `/dev-api/cat/domain/list`, `/dev-api/cat/dataCategory/list`, and `/dev-api/tax/project/currentUser/list`: all returned backend responses instead of 404. Direct backend smoke tests for `http://127.0.0.1:8080/ast/asset/list`, `/cat/domain/list`, and `/cat/dataCategory/list` also reached backend handlers instead of 404. |
| 23 | Data quality category navigation | Fix blank/wrong page after moving data quality category under Metadata Management > Data Quality. | Done | Updated `datamaster-ui/src/store/system/permission.js` so `数据质量类目` is only nested under the actual `数据质量` parent menu instead of accidentally under `数据质量任务`; normalized the flattened router path to `/cat/quality/qualityCat`; ordered `数据质量类目` as the first child under `数据质量`; updated `datamaster-ui/src/layout/components/Sidebar/SidebarItem.vue` so absolute child paths are not re-prefixed by the parent sidebar path; updated `datamaster-ui/src/layout/components/Sidebar/Link.vue` and `datamaster-ui/src/layout/components/TagsView/index.vue` so clicking the current route re-enters through `/redirect` instead of becoming a no-op; removed a leftover debug banner from `datamaster-ui/src/views/att/cat/qualityCat/index.vue`. | Browser verification on `http://127.0.0.1:82` confirmed the quality menu order is `数据质量类目`, `数据质量任务`, `质量任务日志`; direct navigation to `/cat/quality/qualityCat` keeps that URL, renders the category query form and table, and reports no browser errors. `npx eslint src/layout/components/Sidebar/Link.vue src/layout/components/TagsView/index.vue src/store/system/permission.js` passed; `npm run build:prod` passed with existing warnings. `SidebarItem.vue` and `qualityCat/index.vue` still have pre-existing lint issues unrelated to this fix. |

## Change Log

| Date | Item | Notes |
| --- | --- | --- |
| 2026-05-30 | 1-10 | Completed initial `datamaster-ui` split, UI guideline, home page, menu logic, visual shell polish, and new route prefix compatibility layer. |
| 2026-05-30 | User directive | "admin admin123 把这个登录账号写道默认登录上，以后直接登录 ，然后继续重构前端，详细文档看doc下的md,如果修改路由和菜单就连接数据库获取信息和修改，不要查看sql脚本，把我这些话也写到改造计划里" |
| 2026-05-30 | 13, 19 | Added default admin login credentials and completed static hidden route source cleanup for new route prefixes. No database menu rows were changed in this step. |
| 2026-05-30 | 20 | Fixed `/tax/rule/auditRule` direct-access 404 caused by project menus updating store state without registering their dynamic routes in Vue Router. |
| 2026-05-30 | 21 | Fixed the new UI home/project-entry errors reported at `http://127.0.0.1:82`: removed the missing `/home` stats request from the home page, restored project menu route registration, and added backend plus Vite dev `/home` compatibility fallbacks. |
| 2026-05-30 | 22 | Synchronized new UI API request prefixes with the new module prefix table, added frontend request normalization, Vite dev proxy rewrites, and backend compatibility forwarding for new API prefixes. |
| 2026-05-30 | 23 | Fixed the data quality category page opening blank/wrong content after being moved under Metadata Management > Data Quality by correcting menu nesting, flattened route path normalization, sidebar absolute-path resolution, same-route click refresh behavior, menu order, and a leftover debug banner. |

## Current User Constraints

- Default local login should use `admin` / `admin123` so future login does not require retyping credentials.
- Continue the frontend refactor by following the Markdown documents under `docs` (user referred to `doc`).
- If route or menu data must be changed, connect to the database to inspect and modify menu information; do not inspect SQL scripts for this work.
- Record these instructions in this implementation plan and keep later route/menu migration notes explicit.

## Recording Rules

- Update the row status immediately after each implemented task.
- Add changed file paths to `Implementation Record`.
- Add exact verification commands or browser checks to `Verification`.
- If a task is split, add child rows instead of hiding details in one broad row.
- Keep compatibility notes explicit, especially when frontend temporarily adapts backend menu data.
