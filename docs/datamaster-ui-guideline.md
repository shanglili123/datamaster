# DataMaster UI Guideline

Version: v0.1
Reference: `D:\dev\dolphinscheduler-dev\dolphinscheduler-ui`
Target frontend: `D:\dev\dataMaster\datamaster-ui`

## 1. Design Positioning

DataMaster UI should combine two directions:

- Use DolphinScheduler as the interaction and product-structure reference: project-first entry, top module navigation, project-side menus, task-oriented pages, compact tables, and clear runtime states.
- Improve the visual quality beyond DolphinScheduler: cleaner spacing, more polished cards, better hierarchy, quieter colors, stronger empty/loading states, and refined charts.

The product should feel like a modern data platform console, not a marketing page, data big-screen, or old admin template.

Keywords: professional, clean, compact, project-oriented, state-driven, calm, efficient.

## 2. Reference Boundaries

Use DolphinScheduler for:

- Project list as the main entry.
- Entering a project before showing project-scoped functions.
- Top navigation plus optional left project menu.
- Compact card + table page structure.
- Task and workflow status expression.
- Search/action bar above tables.

Do not copy DolphinScheduler directly for:

- Rough visual details.
- Sparse card polish.
- Naive UI implementation details.
- Overly plain default component rendering.

DataMaster keeps Vue SFC + Element Plus as the technical base. The DolphinScheduler source is a design and interaction reference, not a component migration target.

## 3. Theme Tokens

Use these tokens as the first UI foundation. They should be exposed in global CSS variables and reused by pages and components.

```scss
:root {
  --dm-bg-page: #f8f8fc;
  --dm-bg-layout: #f3f6fb;
  --dm-bg-card: #ffffff;
  --dm-bg-soft: #f5f8ff;

  --dm-color-primary: #1890ff;
  --dm-color-primary-hover: #40a9ff;
  --dm-color-primary-active: #096dd9;
  --dm-color-success: #52c41a;
  --dm-color-warning: #faad14;
  --dm-color-danger: #ff4d4f;
  --dm-color-info: #1890ff;

  --dm-text-main: #1f2329;
  --dm-text-regular: #4e5969;
  --dm-text-secondary: #86909c;
  --dm-text-placeholder: #a8abb2;

  --dm-border-color: #e5e7eb;
  --dm-border-light: #eef1f6;
  --dm-shadow-card: 0 6px 18px rgba(31, 35, 41, 0.06);
  --dm-shadow-popover: 0 10px 28px rgba(31, 35, 41, 0.12);

  --dm-radius-small: 4px;
  --dm-radius-base: 6px;
  --dm-radius-large: 8px;

  --dm-header-height: 64px;
  --dm-sidebar-width: 224px;
  --dm-content-padding-x: 22px;
  --dm-content-padding-y: 16px;
}
```

Element Plus primary color should align with `--dm-color-primary`. Avoid one-note purple, beige, brown, or heavy dark-blue palettes.

## 4. Layout System

### Global Layout

- Header height: `64px`.
- Page background: `#f8f8fc`.
- Content padding: `16px 22px`.
- Left side menu width: `224px`.
- Page sections should be full-width content areas or simple cards.
- Do not use nested cards.
- Do not use decorative gradient blobs, large hero blocks, or dashboard-style background images.

### Home State

Before entering a project:

- Show project workspace home.
- Show top-level access to `首页`, `系统管理`, and `日志管理`.
- Do not show project-scoped menus such as `数据建模`, `数据研发`, `数据服务`, or `元数据管理`.
- No left project-side menu unless a system page requires it.

### Project State

After selecting a project:

- Set current project context.
- Load project-scoped route/menu permissions.
- Enter project overview.
- Show the project-side menu.
- Keep a clear `返回首页` or `切换项目` action.

## 5. Navigation Rules

Adopt a DolphinScheduler-like active menu model:

- `activeMenu`: controls top navigation.
- `activeSide`: controls project-side navigation.
- `showSide`: controls whether the left side menu is visible.

DataMaster menu states:

- Home state: `首页`, `系统管理`, `日志管理`.
- Project state: `项目概览`, `数据建模`, `数据研发`, `数据服务`, `元数据管理`.

Menu reorganization:

- `基础管理` is merged into `系统管理`.
- `系统监控` is renamed to `日志管理`.
- `数据治理` is renamed to `元数据管理`.
- `数据资产 > 数据质量任务` is moved to `元数据管理`.

If menus are served by backend data, the frontend may temporarily transform display structure, but backend menu data and SQL initialization should be updated later to match the final product model.

Route prefix rules:

| Module | Old route prefix | New route prefix |
| --- | --- | --- |
| Data assets | `/da` | `/ast` |
| Data standards | `/dp` | `/std` |
| Data collection / integration | `/dpp` | `/col` |
| Data service | `/ds` | `/svc` |
| Data modeling | `/dm` | `/mdl` |
| Metadata catalog | `/mc` | `/cat` |
| Data governance legacy | `/dg` | `/cat` |
| Taxonomy / base classification | `/att` | `/tax` |

The route layer normalizes legacy prefixes for compatibility, but new pages and menu data should use only the new route prefixes.

## 6. Home Page Rules

The home page is a project workspace, not a generic dashboard.

Required areas:

- Current user's authorized project list.
- `新增项目` action visible only to super admin users.
- Project statistics:
  - Data integration task total.
  - Data integration failed task count.
  - Data development task total.
  - Data development failed task count.
  - Data service API calls.
  - Data service API failed/error calls.
  - Database table row counts.

Recommended layout:

- Top operation card: title, short context, create project button, search.
- Project list card: table or compact project cards.
- Statistics row: concise metric cards.
- Table data volume card: table or bar chart.

Avoid:

- Weather iframe.
- Decorative news blocks as primary content.
- Big-screen cockpit graphics.
- Large motivational text.
- Hard-coded fake statistics.

## 7. Card Rules

Cards should look refined but not decorative.

- Background: white.
- Radius: `6px`.
- Border: `1px solid var(--dm-border-light)`.
- Shadow: subtle, use `--dm-shadow-card` only where it improves separation.
- Header height: 44px to 52px.
- Header has a bottom border.
- Body padding: 12px to 16px.
- Titles: 14px to 16px, medium weight.

Use cards for:

- Search/action areas.
- Tables.
- Metric groups.
- Repeated project items.
- Dialog-like framed tools.

Do not use cards inside cards.

## 8. Table Page Rules

Most management pages should follow this structure:

```text
Page
  Action Card
    Left: primary actions
    Right: search and filters
  Table Card
    Header: table title and optional secondary actions
    Body: table
    Footer: centered pagination
```

Rules:

- Use compact table density.
- Keep action column fixed on the right where useful.
- Use tags for statuses.
- Use icon buttons or link buttons for common row actions.
- Keep delete/stop/offline actions visually dangerous and confirm before execution.
- Avoid oversized buttons and oversized form controls.

## 9. Form And Dialog Rules

- Use dialogs for create/edit forms that are not complex.
- Use full pages for task configuration, SQL editors, workflow-like editors, and multi-step forms.
- Dialog width should normally be 520px, 640px, or 780px.
- Label width should be consistent within a form.
- Footer actions are right-aligned.
- Primary action is always visually clear.
- Destructive actions require confirmation.

## 10. Status And Feedback Rules

Use consistent state colors:

- Running/processing: primary blue.
- Success/normal: green.
- Warning/waiting: orange/yellow.
- Failed/error: red.
- Disabled/offline: gray.

Every async-heavy page should have:

- Loading state.
- Empty state.
- Error state where API failure is possible.
- Permission-hidden actions rather than disabled actions when the user should not know they exist.

## 11. Chart Rules

Charts should support operational decisions. They are not decoration.

- Prefer bar, line, and pie charts only when they explain the data better than a table.
- Use the same status colors as tags.
- Keep chart cards compact and titled.
- Always provide numeric values near chart summaries.
- Avoid full-screen cockpit visuals unless explicitly building an operations screen.

## 12. Implementation Rules

- All new frontend work happens in `datamaster-ui`.
- `datamaster-view` remains the original reference implementation.
- Keep Element Plus and existing app infrastructure.
- Add shared DataMaster UI styles under `src/assets/system/styles`.
- Prefer reusable CSS utility classes and small layout wrappers over page-specific visual hacks.
- New pages should use the guideline before copying old page patterns.

## 13. First Refactor Targets

1. Create `datamaster-ui` from `datamaster-view`.
2. Add global DataMaster DS-inspired theme variables.
3. Replace the current home page with the project workspace.
4. Move project selector logic from the navbar into project-home entry behavior.
5. Add menu state filtering for home state and project state.
6. Reorganize menu display names and grouping.
7. Gradually restyle major list pages using the new card/table pattern.
