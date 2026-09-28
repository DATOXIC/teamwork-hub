# TeamWork Hub — Automated Opaque-Box E2E Test Suite Infrastructure

## 1. Overview & Architectural Principles

The TeamWork Hub E2E Test Suite provides automated, opaque-box, regression-prevention testing across the entire JSP refactoring effort. The suite guarantees that:
- **Zero Functional Regression**: All 40 HTML forms, action endpoints, HTTP methods, input names, IDs, types, and hidden parameter tokens are strictly preserved across all 7 JSP views.
- **Zero Syntax & Runtime Breakage**: Every opening/closing tag, JSTL directive (`jakarta.tags.core`, `jakarta.tags.fmt`), control structure (`<c:if>`, `<c:choose>`, `<c:when>`, `<c:forEach>`), and EL expression (`${...}`) is syntactically sound, balanced, and free of corrupt characters.
- **Clean UI Separation & Semantic CSS**: Static inline styles are extracted into semantic CSS classes in `styles/page-components.css` while dynamic runtime styles (e.g. progress bar widths `style="width: ${pct}%;"`) and client JavaScript hooks are rigorously preserved.
- **Zero Build Regression**: The entire Java backend and Maven project compiles cleanly via `mvn compile` with exit code 0.

---

## 2. Directory Layout & Test Artifacts

```
tests/e2e/
├── runner.js                           # Master Test Runner (CLI flags, formatting, exit codes)
├── run_tests.bat                       # Windows Batch runner (UTF-8 chcp 65001, node detection)
├── run_tests.ps1                       # PowerShell runner (UTF-8, argument forwarding, exit code)
├── config.js                           # Target pages, form catalogs, semantic classes, JS hooks
├── utils/
│   ├── jsp_parser.js                   # Quote-aware JSP/HTML/EL/JSTL/CSS parser
│   └── test_reporter.js                # ANSI color-coded reporting, tabular breakdown, timings
└── specs/
    ├── tier1_feature_input_preservation.spec.js   # Tier 1: 54 verification points
    ├── tier2_boundary_syntax_integrity.spec.js     # Tier 2: 88 verification points
    ├── tier3_semantic_css_hooks.spec.js           # Tier 3: 108 verification points
    └── tier4_build_compilation.spec.js            # Tier 4: 4 verification points
```

---

## 3. The 4-Tier Test Architecture

### Tier 1: Feature & Input Preservation (54 Tests)
Validates that refactoring never breaks form submissions, Servlet routing, or model parameter bindings:
- **`login.jsp`**: Login form (`/auth`, `POST`, `action=login`, `username`, `password`, `remember`) and Registration form (`/auth`, `POST`, `action=register`, `fullName`, `username`, `email`, `password`, `confirmPassword`).
- **`profile.jsp`**: Edit Profile modal form (`/profile`, `POST`, `action=update`, `userId`, `fullName`, `role`, `bio`, `skills`, `githubUrl`, `linkedinUrl`) and Quick Invite modal form (`/invite`, `POST`, `action=sendInvite`, `usernameOrEmail`, `projectId`).
- **`projects.jsp`**: Accept invite form (`/invite`, `POST`, `action=accept`), Reject invite form (`/invite`, `POST`, `action=reject`), Join by code modal form (`/invite`, `POST`, `action=requestJoin`, `projectCode`), and Create project modal form (`/project`, `POST`, `action=create`, `name`, `projectCode`, `description`, `projectType`).
- **`project_report.jsp`**: CSV Export link (`/task?action=exportCsv&projectId=...`), subnav links to Kanban (`/task?action=list`), Wiki (`/doc?action=list`), Chat (`/chat?action=view`), and A4 print trigger (`window.print()`).
- **`docs.jsp`**: Create doc form (`doc`, `POST`, `action=create`, `projectId`, `title`, `content`), Edit doc form (`doc`, `POST`, `action=update`, `projectId`, `docId`, `title`, `content`), and Delete doc action link (`action=delete`).
- **`chat.jsp`**: Send project message form (`chat`, `POST`, `action=sendProjectMessage`, `projectId`, `content`), Edit message form (`chat`, `POST`, `action=editProjectMessage`, `projectId`, `messageId`, `content`), and Delete message confirmation hook (`#btnConfirmDeleteMessage`).
- **`tasks.jsp`**: Exact verification of all 28 functional forms: `editTask`, `addSubTask`, `updateStatus` (4 instances), `pmApprovePlanning`, `pmRejectPlanning`, `submitParentTask`, `pmApproveTask`, `pmReviseTask`, `pmRejectTask`, `sendTaskComment`, `editSubTask`, `submitSubTask`, `approveSubTask`, `reviseSubTask`, `rejectSubTask`, `deleteSubTask`, `add`, `kick`, `leave`, `revoke`, `sendInvite`, `update`, `create`, `requestJoin`.

### Tier 2: Boundary & Syntax Integrity (88 Tests)
Guarantees clean HTML/JSP compilation and layout rendering:
- **Directive Integrity**: `<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>` in `header.jsp` and `login.jsp`.
- **JSTL Core & Fmt Directives**: `<%@ taglib prefix="c" uri="jakarta.tags.core" %>` and `<%@ taglib prefix="fmt" ... %>` correctly declared in all consuming views.
- **JSTL Control Structure Pairing**: Balanced opening and closing tags for `<c:choose>`, `<c:when>`, `<c:otherwise>`, `<c:if>`, and `<c:forEach>`.
- **EL Expression Syntax Integrity**: Scans all 1,480+ EL expressions across all 7 views to confirm 0 unclosed `${` syntax errors.
- **HTML Tag Pairing Balance**: Verifies `<form>`, `<script>`, `<style>`, and `<table>` blocks are 100% paired with 0 orphaned tags.
- **UTF-8 Character Fidelity**: Confirms 0 corrupt multi-byte sequences (`\uFFFD`) across all JSPs.
- **Stylesheet Inclusion & Security Whitelist**: Verifies `styles/page-components.css` exists on filesystem, is included in `header.jsp` and `login.jsp` via `${pageContext.request.contextPath}/styles/...`, and is whitelisted in `AuthFilter.java`.

### Tier 3: Semantic CSS & JavaScript Hooks Integrity (108 Tests)
Enforces design system decoupling while safeguarding dynamic styling and client behavior:
- **Dynamic Runtime Style Preservation**: Strictly preserves dynamic styles computing runtime dimensions (e.g. `style="width: ${progressPercentage}%;"`, `style="width: ${pctDone}%;"`, `style="background-color: ${label.color};"`).
- **Static Inline Style Audit**: Confirms 0 static inline styles in completed M1 views (`login.jsp`, `profile.jsp`), and tracks pending extractions for M2 (`projects.jsp`, `project_report.jsp`) and M3 (`tasks.jsp`, `chat.jsp`, `docs.jsp`).
- **Semantic CSS Class Catalog**: Verifies existence of required classes in `page-components.css` or companion stylesheets:
  - Common: `.progress-init-zero`, `.cursor-pointer`
  - Login: `.login-body`, `.login-navbar`, `.login-brand-logo-icon`, `.login-campus-caption`, `.login-exit-link`
  - Profile: `.profile-identity-card`, `.profile-avatar-banner`, `.profile-bio-text`, `.profile-skill-badge`, `.profile-skill-chip-interactive`
  - Projects: `.workspace-header-bar`, `.btn-join-code-trigger`, `.invite-alert-pill`, `.btn-invite-accept`, `.btn-invite-reject`, `.workspace-toolbar-card`, `.badge-role-owner`, `.badge-role-member`, `.project-desc-line-clamp`
  - Report: `.report-btn-back`, `.report-code-badge`, `.report-title-truncate`, `.report-banner-content`, `.report-card-surface`, `.report-progress-pill`
  - Tasks/Workspace: `.wiki-doc-list-scroll`, `.chat-shelf-scroll`, `.kanban-column`, `.kanban-dropzone`, `.kanban-card`, `.task-drawer-panel`, `.clickup-status-popover`
- **JavaScript DOM Hooks & Dataset Attributes**: Verifies all critical functions (e.g. `switchTab`, `togglePassword`, `fillLogin`, `addSkill`, `copyProjectCode`, `filterProjects`, `searchProjectsLive`, `filterTasks`, `filterDocList`, `openClickUpTask`, `switchTaskSubView`, `openStatusDropdown`, `toggleClickUpSidebar`), IDs (e.g. `#taskSearchInput`, `#themeToggleBtn`, `#clickupSidebar`, `#clickupTaskDrawer`), and dataset attributes (`data-task-id`, `data-requires-gate`, `data-status`, `data-doc-title`, `data-progress`, `data-role`, `data-raw-content`).

### Tier 4: Build & Compilation (4 Tests)
Ensures backend compilation safety:
- Executes `mvn compile` via child process.
- Asserts exit code is strictly 0.
- Asserts Maven output contains `[INFO] BUILD SUCCESS`.
- Asserts 0 Java compiler errors.
- Verifies `target/classes` bytecode generation.

---

## 4. How to Run the Test Suite

### Command-Line Quick Start
```bash
# 1. Run full 4-tier test suite using Node.js
node tests/e2e/runner.js

# 2. Run via Windows Batch Wrapper (synchronizes chcp 65001)
tests\e2e\run_tests.bat

# 3. Run via PowerShell Wrapper
powershell -ExecutionPolicy Bypass -File tests/e2e/run_tests.ps1
```

### Selective & Modular Execution
```bash
# Run a specific Tier only
node tests/e2e/runner.js --tier 1
node tests/e2e/runner.js --tier 2
node tests/e2e/runner.js --tier 3
node tests/e2e/runner.js --tier 4

# Run tests for a specific JSP page
node tests/e2e/runner.js --page login
node tests/e2e/runner.js --page profile
node tests/e2e/runner.js --page projects
node tests/e2e/runner.js --page tasks

# Run in strict mode (fails if any M2/M3 target class is not yet added)
node tests/e2e/runner.js --strict

# Output full verbose details
node tests/e2e/runner.js --verbose

# Output machine-readable JSON (for CI/CD pipelines)
node tests/e2e/runner.js --json
```

---

## 5. Compliance with Project Guardrails (GEMINI.md)

1. **Zero Hardcoded Paths**: All file paths are dynamically resolved relative to `__dirname` and the repository root. No personal drive paths (`C:\Users\...`) appear anywhere in code or test scripts.
2. **UTF-8 Synchronization**: All runners (`run_tests.bat`, `run_tests.ps1`) configure console code page to UTF-8 (`chcp 65001`) and pass `-Dfile.encoding=UTF-8` compatibility standards.
3. **Smart Tooling Fallback**: Runtimes (`node`, `mvn`) are detected from the system path without assumption of specific installation directories.
4. **Self-Contained Isolation**: Built using native Node.js standard libraries (`node:fs`, `node:path`, `node:child_process`). Requires zero `npm install` and runs instantly in any environment with Node 18+.
