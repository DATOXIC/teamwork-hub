# Project: TeamWork Hub JSP Semantic Simplification & UI Separation

## Architecture
- **Web Application Stack**: Jakarta Servlet 6.0, Jakarta EE 10, JSTL 3.0, Java 21, Maven WAR packaging (`teamwork-hub.war`).
- **Webapp Root**: `src/main/webapp/`
- **Layout Inclusions**:
  - `src/main/webapp/includes/header.jsp` (includes Bootstrap 5.3.3, Google Fonts, `main.css`, `command-palette.css`, and dynamic `extraCss`).
  - `src/main/webapp/includes/footer.jsp` (Bootstrap JS bundle, `app.js`, `command-palette.js`).
  - Standalone `<head>` in `src/main/webapp/login.jsp`.
- **CSS Architecture**:
  - Central semantic component stylesheet: `src/main/webapp/styles/page-components.css`.
  - Whitelisted for public static access in `AuthFilter.java` under `/styles/*`.
  - Linked in `includes/header.jsp` via `${pageContext.request.contextPath}/styles/page-components.css?v=<%= System.currentTimeMillis() %>` (serves 6 JSPs).
  - Linked in `login.jsp` `<head>` via identical link tag (serves standalone login page).
  - Page-specific existing sheets: `styles/login.css` (auth page), `styles/report.css` (print & report page), `styles/chat.css` (glassmorphic chat).
- **Core Refactoring Principles**:
  - Extract inline styles (`style="..."`) and long utility class chains into semantic CSS classes.
  - Preserve 100% of HTML forms, action URLs, methods, input names, IDs, types, values, and hidden tokens.
  - Retain all JSTL tags (`<c:if>`, `<c:forEach>`, `<c:choose>`, etc.) and EL expressions (`${...}`).
  - Retain all JavaScript selectors, event listeners, and dataset attributes (`data-*`).
  - Preserve dynamic runtime inline styles (such as progress widths `style="width: ${pctDone}%;"`).
  - Add bilingual (Vietnamese - English) MVC skeleton comments at major UI sections explaining data exchanged with Servlets.

## Code Layout
- `src/main/webapp/styles/page-components.css` — Shared semantic CSS classes for components.
- `src/main/webapp/includes/header.jsp` — Central layout include linking `page-components.css`.
- `src/main/webapp/login.jsp` — Authentication & registration view (M1).
- `src/main/webapp/profile.jsp` — User profile & quick invite view (M1).
- `src/main/webapp/projects.jsp` — Project dashboard, invitations, and creation view (M2).
- `src/main/webapp/project_report.jsp` — Quality audit & analytics report view (M2).
- `src/main/webapp/tasks.jsp` — Kanban board, task list, side-peek drawer, and quality gates view (M3).
- `src/main/webapp/chat.jsp` — Team messaging & mention shelf view (M3).
- `src/main/webapp/docs.jsp` — Project knowledge base & markdown document view (M3).
- `tests/e2e/` — Opaque-box E2E test runner and automated validation scripts (E2E Track).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| FI-01 | Shared CSS Architecture Setup | Create `styles/page-components.css` and link it in `header.jsp` & `login.jsp` | M1 | Survey (Explorer 1) |
| FI-02 | Login Page Refactoring | Extract inline styles, preserve forms/scripts/EL, add bilingual MVC comments in `login.jsp` | M1 | Survey (Explorer 2) |
| FI-03 | Profile Page Refactoring | Extract inline styles & utility classes, preserve 2 modal forms, add bilingual MVC comments in `profile.jsp` | M1 | Survey (Explorer 2) |
| FI-04 | Projects Page Refactoring | Extract 52 inline styles & utility chains, preserve 4 forms, search/filter JS hooks, add bilingual MVC comments in `projects.jsp` | M2 | Survey (Explorer 2) |
| FI-05 | Project Report Refactoring | Extract 55 inline styles, preserve theme switcher, KPI filters, print styling, dynamic EL bars, add bilingual MVC comments in `project_report.jsp` | M2 | Survey (Explorer 2) |
| FI-06 | Docs Page Refactoring | Extract inline styles, preserve 2 modal forms, sidebar doc list scroll, add bilingual MVC comments in `docs.jsp` | M3 | Survey (Explorer 3) |
| FI-07 | Chat Page Refactoring | Extract 30 inline styles, preserve 2 forms, emoji drawer, mention shelves, add bilingual MVC comments in `chat.jsp` | M3 | Survey (Explorer 3) |
| FI-08 | Tasks Page Refactoring | Extract static inline styles & style block, preserve 28 forms, Kanban drag-and-drop, side drawer, quality gates, add bilingual MVC comments in `tasks.jsp` | M3 | Survey (Explorer 3) |
| FI-09 | E2E Test Suite & Test Harness | Independent test suite (Tiers 1-4) testing HTML integrity, form inputs, EL/JSTL syntax, CSS links, and Maven compilation | M-TEST | E2E Track |
| FI-10 | Final Verification & Adversarial Hardening | 100% E2E test pass, Tier 5 adversarial testing, `mvn compile` verification, and Forensic Integrity Audit | M-FINAL | Implementation Track |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M-TEST | E2E Testing Track | Test harness, Tiers 1-4 test cases, publish `TEST_READY.md` | none | DONE |
| M1 | Authentication & Profile | FI-01, FI-02, FI-03 (`page-components.css`, `header.jsp`, `login.jsp`, `profile.jsp`) | none | DONE |
| M2 | Project Management & Reporting | FI-04, FI-05 (`projects.jsp`, `project_report.jsp`) | M1 | IN_PROGRESS |
| M3 | Task Board & Collaboration | FI-06, FI-07, FI-08 (`tasks.jsp`, `chat.jsp`, `docs.jsp`) | M1 | PLANNED |
| M-FINAL | Final Verification & Audit | FI-10 (Pass 100% E2E tests, Tier 5 adversarial hardening, Forensic Audit) | M-TEST, M1, M2, M3 | PLANNED |

## Interface Contracts
### Common Layout ↔ Page Views
- `includes/header.jsp` provides the global CSS imports. It loads `${pageContext.request.contextPath}/styles/page-components.css` after `command-palette.css`.
- Any page including `includes/header.jsp` automatically gains access to all `.page-*`, `.auth-*`, `.project-*`, `.kanban-*`, `.chat-*`, `.doc-*` classes defined in `page-components.css`.
- `login.jsp` does not include `header.jsp`; it includes `page-components.css` directly in its `<head>` tag.
- Static assets under `/styles/*` are whitelisted by `AuthFilter.java` without requiring session authentication.

### Dynamic Styles Contract
- Dynamic EL expressions computing runtime measurements (e.g. `style="width: ${progressPercentage}%;"`, `style="width: ${pctDone}%;"`) MUST be preserved as runtime inline attributes.
- Static presentation rules (colors, paddings, borders, flexbox properties, display modes, scrollbar heights) MUST be extracted into classes in `page-components.css` or dedicated stylesheets.

### JavaScript DOM Hooks Contract
- Any ID, class name, or `data-*` attribute targeted by `js/app.js`, `js/tasks.js`, `js/chat.js`, or inline scripts MUST remain unchanged.
- Form field names (`name="..."`), hidden actions (`action="..."`), and form IDs MUST NOT be altered.
