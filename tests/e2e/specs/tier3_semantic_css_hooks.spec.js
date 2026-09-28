/**
 * tier3_semantic_css_hooks.spec.js - Tier 3 E2E Test Suite
 *
 * Verifies that inline styles are extracted into semantic CSS classes,
 * dynamic EL styles (such as progress bar percentages) are preserved,
 * required semantic classes exist in page-components.css or dedicated sheets,
 * and JavaScript DOM hooks, event triggers, and dataset attributes remain intact.
 */

const fs = require('fs');
const path = require('path');
const parser = require('../utils/jsp_parser');
const config = require('../config');

function runTier3Tests(reporter, pageFilter = null, options = {}) {
    reporter.startTier(3, 'Semantic CSS & JavaScript Hooks Integrity');

    const isStrict = !!options.strict;
    const pagesToTest = pageFilter
        ? config.TARGET_PAGES.filter(p => p.toLowerCase().includes(pageFilter.toLowerCase()))
        : config.TARGET_PAGES;

    // 1. Dynamic Inline Style Preservation
    testDynamicStylesPreservation(reporter);

    // 2. Static Inline Style Extraction Audit
    for (const pageName of pagesToTest) {
        testStaticInlineStyleExtraction(reporter, pageName, isStrict);
    }

    // 3. Semantic CSS Classes Existence in Stylesheets
    testSemanticCssClasses(reporter, isStrict);

    // 4. JavaScript Hooks & DOM Selectors Integrity
    for (const pageName of pagesToTest) {
        testJavaScriptHooks(reporter, pageName);
    }
}

/**
 * 1. Test Dynamic Inline Style Preservation
 * Ensures dynamic styles computing runtime measurements (like width: ${pct}%) are preserved.
 */
function testDynamicStylesPreservation(reporter) {
    const reportPath = path.join(config.WEBAPP_DIR, 'project_report.jsp');
    if (fs.existsSync(reportPath)) {
        const content = parser.readJspFile(reportPath);
        const styles = parser.parseInlineStyles(content);
        const hasDynamicProgress = styles.dynamicList.some(s => s.raw.includes('${progressPercentage}%') || s.raw.includes('${stat.completionRate}%'));

        reporter.record({
            tier: 3,
            name: 'project_report.jsp: Dynamic EL progress style preserved (width: ${progressPercentage}%)',
            page: 'project_report.jsp',
            passed: hasDynamicProgress,
            details: hasDynamicProgress ? `Preserved ${styles.dynamicCount} dynamic EL styles` : 'Dynamic progress bar style missing'
        });
    }

    const tasksPath = path.join(config.WEBAPP_DIR, 'tasks.jsp');
    if (fs.existsSync(tasksPath)) {
        const content = parser.readJspFile(tasksPath);
        const styles = parser.parseInlineStyles(content);
        const hasDynamicWidth = styles.dynamicList.some(s => s.raw.includes('${pctDone}%') || s.raw.includes('${pctInProg}%') || s.raw.includes('${progTotal'));

        reporter.record({
            tier: 3,
            name: 'tasks.jsp: Dynamic EL progress style preserved (width: ${pctDone}%, etc.)',
            page: 'tasks.jsp',
            passed: hasDynamicWidth,
            details: hasDynamicWidth ? `Preserved ${styles.dynamicCount} dynamic EL styles` : 'Dynamic progress styles missing'
        });
    }
}

/**
 * 2. Test Static Inline Style Extraction Audit
 */
function testStaticInlineStyleExtraction(reporter, pageName, isStrict) {
    const filePath = path.join(config.WEBAPP_DIR, pageName);
    let content;
    try {
        content = parser.readJspFile(filePath);
    } catch (err) {
        reporter.record({
            tier: 3,
            name: `File Existence Check for ${pageName}`,
            page: pageName,
            passed: false,
            error: err.message
        });
        return;
    }

    const styles = parser.parseInlineStyles(content);
    const staticCount = styles.staticCount;

    // Completed milestones (M1: login.jsp, profile.jsp) MUST have 0 static styles
    if (pageName === 'login.jsp' || pageName === 'profile.jsp') {
        const zeroStatic = staticCount === 0;
        reporter.record({
            tier: 3,
            name: `M1 Semantic Extraction: ${pageName} contains 0 static inline styles`,
            page: pageName,
            passed: zeroStatic,
            details: zeroStatic
                ? `100% extracted: 0 static inline styles found`
                : `${staticCount} static inline styles remaining`
        });
    } else {
        // Pending milestones (M2: projects.jsp, project_report.jsp; M3: docs.jsp, chat.jsp, tasks.jsp)
        const isExtracted = staticCount === 0;
        if (isExtracted) {
            reporter.record({
                tier: 3,
                name: `Semantic Style Extraction: ${pageName} contains 0 static inline styles`,
                page: pageName,
                passed: true,
                details: '100% extracted: 0 static inline styles found'
            });
        } else if (isStrict) {
            reporter.record({
                tier: 3,
                name: `Semantic Style Extraction: ${pageName} contains 0 static inline styles`,
                page: pageName,
                passed: false,
                details: `${staticCount} static inline styles remain to be extracted in M2/M3`
            });
        } else {
            reporter.record({
                tier: 3,
                name: `Semantic Style Extraction [M2/M3 Target]: ${pageName}`,
                page: pageName,
                passed: false,
                pending: true,
                details: `${staticCount} static inline styles pending extraction during upcoming milestone`
            });
        }
    }
}

/**
 * 3. Test Semantic CSS Classes Existence in Stylesheets
 */
function testSemanticCssClasses(reporter, isStrict) {
    const cssPath = path.join(config.WEBAPP_DIR, config.COMPONENT_STYLESHEET);
    let classSet = new Set();

    if (fs.existsSync(cssPath)) {
        const cssContent = fs.readFileSync(cssPath, 'utf8');
        classSet = parser.parseCssClasses(cssContent);
    }

    // Also check page-specific sheets if any (login.css, report.css)
    const reportCssPath = path.join(config.STYLES_DIR, 'report.css');
    let reportClassSet = new Set();
    if (fs.existsSync(reportCssPath)) {
        reportClassSet = parser.parseCssClasses(fs.readFileSync(reportCssPath, 'utf8'));
    }

    // M1 Common & Login & Profile Classes (MUST PASS)
    const m1Classes = [
        ...config.SEMANTIC_CLASSES.common,
        ...config.SEMANTIC_CLASSES.login,
        ...config.SEMANTIC_CLASSES.profile
    ];

    for (const cls of m1Classes) {
        const exists = classSet.has(cls);
        reporter.record({
            tier: 3,
            name: `M1 Component Class .${cls} defined in page-components.css`,
            page: 'page-components.css',
            passed: exists,
            details: exists ? `Found .${cls}` : `Class .${cls} missing in page-components.css`
        });
    }

    // M2 Projects Classes
    for (const cls of config.SEMANTIC_CLASSES.projects) {
        const exists = classSet.has(cls);
        if (exists) {
            reporter.record({
                tier: 3,
                name: `M2 Project Class .${cls} defined in page-components.css`,
                page: 'page-components.css',
                passed: true,
                details: `Found .${cls}`
            });
        } else if (isStrict) {
            reporter.record({
                tier: 3,
                name: `M2 Project Class .${cls} defined in page-components.css`,
                page: 'page-components.css',
                passed: false,
                details: `Class .${cls} missing in page-components.css (pending M2)`
            });
        } else {
            reporter.record({
                tier: 3,
                name: `M2 Project Class [Target]: .${cls}`,
                page: 'page-components.css',
                passed: false,
                pending: true,
                details: `Class .${cls} scheduled for addition in Milestone M2`
            });
        }
    }

    // M2 Report Classes
    for (const cls of config.SEMANTIC_CLASSES.report) {
        const exists = classSet.has(cls) || reportClassSet.has(cls);
        if (exists) {
            reporter.record({
                tier: 3,
                name: `M2 Report Class .${cls} defined in page-components.css or report.css`,
                page: 'report.css',
                passed: true,
                details: `Found .${cls}`
            });
        } else if (isStrict) {
            reporter.record({
                tier: 3,
                name: `M2 Report Class .${cls} defined in page-components.css or report.css`,
                page: 'report.css',
                passed: false,
                details: `Class .${cls} missing (pending M2)`
            });
        } else {
            reporter.record({
                tier: 3,
                name: `M2 Report Class [Target]: .${cls}`,
                page: 'report.css',
                passed: false,
                pending: true,
                details: `Class .${cls} scheduled for addition in Milestone M2`
            });
        }
    }

    // M3 Tasks & Workspace Classes
    for (const cls of config.SEMANTIC_CLASSES.tasksAndWorkspace) {
        const exists = classSet.has(cls);
        if (exists) {
            reporter.record({
                tier: 3,
                name: `M3 Workspace Class .${cls} defined in page-components.css`,
                page: 'page-components.css',
                passed: true,
                details: `Found .${cls}`
            });
        } else if (isStrict) {
            reporter.record({
                tier: 3,
                name: `M3 Workspace Class .${cls} defined in page-components.css`,
                page: 'page-components.css',
                passed: false,
                details: `Class .${cls} missing (pending M3)`
            });
        } else {
            reporter.record({
                tier: 3,
                name: `M3 Workspace Class [Target]: .${cls}`,
                page: 'page-components.css',
                passed: false,
                pending: true,
                details: `Class .${cls} scheduled for addition in Milestone M3`
            });
        }
    }
}

/**
 * 4. Test JavaScript Hooks & DOM Selectors Integrity
 */
function testJavaScriptHooks(reporter, pageName) {
    const filePath = path.join(config.WEBAPP_DIR, pageName);
    const content = parser.readJspFile(filePath);
    const hooks = config.JS_HOOKS[pageName];

    if (!hooks) return;

    // Functions
    if (hooks.functions) {
        for (const fnName of hooks.functions) {
            const hasFn = content.includes(fnName);
            reporter.record({
                tier: 3,
                name: `JS Event Hook: ${fnName}() preserved`,
                page: pageName,
                passed: hasFn,
                details: hasFn ? `Found function invocation or declaration: ${fnName}` : `Function ${fnName} not found`
            });
        }
    }

    // IDs
    if (hooks.ids) {
        for (const id of hooks.ids) {
            const hasId = content.includes(`id="${id}"`) || content.includes(`id='${id}'`);
            reporter.record({
                tier: 3,
                name: `DOM Element ID: #${id} preserved`,
                page: pageName,
                passed: hasId,
                details: hasId ? `Found element with id="${id}"` : `Element id="${id}" missing`
            });
        }
    }

    // Data Attributes
    if (hooks.dataAttrs) {
        for (const dataAttr of hooks.dataAttrs) {
            const hasData = content.includes(`${dataAttr}=`);
            reporter.record({
                tier: 3,
                name: `Dataset Attribute: [${dataAttr}] preserved`,
                page: pageName,
                passed: hasData,
                details: hasData ? `Found attribute ${dataAttr}` : `Attribute ${dataAttr} missing`
            });
        }
    }
}

module.exports = { runTier3Tests };
