/**
 * tier2_boundary_syntax_integrity.spec.js - Tier 2 E2E Test Suite
 *
 * Verifies HTML tag balance, JSTL directive syntax (<%@ taglib ... %>),
 * JSTL control flow pairing, EL syntax integrity (${...}),
 * UTF-8 encoding fidelity, and page-components.css reference integrity.
 */

const fs = require('fs');
const path = require('path');
const parser = require('../utils/jsp_parser');
const config = require('../config');

function runTier2Tests(reporter, pageFilter = null) {
    reporter.startTier(2, 'Boundary & Syntax Integrity');

    const pagesToTest = pageFilter
        ? config.TARGET_PAGES.filter(p => p.toLowerCase().includes(pageFilter.toLowerCase()))
        : config.TARGET_PAGES;

    // 1. Common Layout & Login Header Directives
    testHeaderDirectives(reporter);

    // 2. Per-Page JSTL, EL, and Tag Balance Validations
    for (const pageName of pagesToTest) {
        const filePath = path.join(config.WEBAPP_DIR, pageName);
        let content;
        try {
            content = parser.readJspFile(filePath);
        } catch (err) {
            reporter.record({
                tier: 2,
                name: `File Existence Check for ${pageName}`,
                page: pageName,
                passed: false,
                error: err.message
            });
            continue;
        }

        testPageJstlDirectives(reporter, pageName, content);
        testPageJstlTagBalance(reporter, pageName, content);
        testPageElExpressions(reporter, pageName, content);
        testPageHtmlTagBalance(reporter, pageName, content);
        testPageEncodingFidelity(reporter, pageName, content);
    }

    // 3. Central Stylesheet Reference & Whitelist Integrity
    testStylesheetReferenceIntegrity(reporter);
}

/**
 * 1. Test header directives in includes/header.jsp & login.jsp
 */
function testHeaderDirectives(reporter) {
    const headerPath = path.join(config.INCLUDES_DIR, 'header.jsp');
    if (fs.existsSync(headerPath)) {
        const content = parser.readJspFile(headerPath);
        const hasEncoding = /pageEncoding=["']UTF-8["']/i.test(content) && /contentType=["'][^"']*UTF-8["']/i.test(content);
        const hasCoreTaglib = /<%@\s*taglib\s+prefix=["']c["']\s+uri=["']jakarta\.tags\.core["']/i.test(content);
        const hasDoctype = /<!DOCTYPE\s+html>/i.test(content);

        reporter.record({
            tier: 2,
            name: 'includes/header.jsp: UTF-8 page directive declaration',
            page: 'header.jsp',
            passed: hasEncoding,
            details: hasEncoding ? 'UTF-8 page directive found' : 'Missing UTF-8 page directive'
        });

        reporter.record({
            tier: 2,
            name: 'includes/header.jsp: Jakarta JSTL Core taglib declaration (jakarta.tags.core)',
            page: 'header.jsp',
            passed: hasCoreTaglib,
            details: hasCoreTaglib ? 'Found jakarta.tags.core taglib' : 'Missing jakarta.tags.core taglib'
        });

        reporter.record({
            tier: 2,
            name: 'includes/header.jsp: Valid HTML5 DOCTYPE declaration',
            page: 'header.jsp',
            passed: hasDoctype,
            details: hasDoctype ? '<!DOCTYPE html> verified' : 'Missing <!DOCTYPE html>'
        });
    }

    const loginPath = path.join(config.WEBAPP_DIR, 'login.jsp');
    if (fs.existsSync(loginPath)) {
        const content = parser.readJspFile(loginPath);
        const hasEncoding = /pageEncoding=["']UTF-8["']/i.test(content) && /contentType=["'][^"']*UTF-8["']/i.test(content);
        const hasCoreTaglib = /<%@\s*taglib\s+prefix=["']c["']/i.test(content);
        const hasDoctype = /<!DOCTYPE\s+html>/i.test(content);

        reporter.record({
            tier: 2,
            name: 'login.jsp: Standalone UTF-8 page directive & DOCTYPE declaration',
            page: 'login.jsp',
            passed: hasEncoding && hasDoctype && hasCoreTaglib,
            details: `Encoding:${hasEncoding}, DOCTYPE:${hasDoctype}, Taglib:${hasCoreTaglib}`
        });
    }
}

/**
 * 2. Test JSTL directives on page
 */
function testPageJstlDirectives(reporter, pageName, content) {
    const usesCore = /<\/?c:[a-zA-Z0-9_-]+/i.test(content);
    const usesFmt = /<\/?fmt:[a-zA-Z0-9_-]+/i.test(content);

    const hasCoreDirective = /<%@\s*taglib\s+prefix=["']c["']/i.test(content);
    const hasFmtDirective = /<%@\s*taglib\s+prefix=["']fmt["']/i.test(content);

    if (usesCore) {
        reporter.record({
            tier: 2,
            name: 'JSTL Core Taglib Directive (<%@ taglib prefix="c" ... %>) Present',
            page: pageName,
            passed: hasCoreDirective,
            details: hasCoreDirective ? 'Core taglib directive found' : 'Taglib prefix="c" directive missing'
        });
    }

    if (usesFmt) {
        reporter.record({
            tier: 2,
            name: 'JSTL Fmt Taglib Directive (<%@ taglib prefix="fmt" ... %>) Present',
            page: pageName,
            passed: hasFmtDirective,
            details: hasFmtDirective ? 'Fmt taglib directive found' : 'Taglib prefix="fmt" directive missing'
        });
    }
}

/**
 * 3. Test JSTL Tag Balance
 */
function testPageJstlTagBalance(reporter, pageName, content) {
    const jstl = parser.parseJstlTags(content);

    const counts = {};
    for (const tag of jstl.jstlTags) {
        const key = `${tag.prefix}:${tag.tagName}`;
        if (!counts[key]) counts[key] = { open: 0, close: 0, self: 0 };
        if (tag.isSelfClosing) {
            counts[key].self++;
        } else if (tag.isClosing) {
            counts[key].close++;
        } else {
            counts[key].open++;
        }
    }

    // Check paired tags
    const pairedTags = ['c:choose', 'c:when', 'c:otherwise', 'c:if', 'c:forEach'];
    for (const pTag of pairedTags) {
        if (counts[pTag]) {
            const isBalanced = counts[pTag].open === counts[pTag].close;
            reporter.record({
                tier: 2,
                name: `JSTL Tag Balance <${pTag}> (open=${counts[pTag].open}, close=${counts[pTag].close})`,
                page: pageName,
                passed: isBalanced,
                details: isBalanced ? `Balanced ${pTag}` : `Unbalanced ${pTag}: open=${counts[pTag].open}, close=${counts[pTag].close}`
            });
        }
    }
}

/**
 * 4. Test EL Expressions
 */
function testPageElExpressions(reporter, pageName, content) {
    const el = parser.parseElExpressions(content);
    const zeroUnclosed = el.unclosed.length === 0;

    reporter.record({
        tier: 2,
        name: `EL Expression Syntax Integrity (total=${el.expressions.length}, unclosed=0)`,
        page: pageName,
        passed: zeroUnclosed,
        details: zeroUnclosed
            ? `Verified ${el.expressions.length} valid EL expressions`
            : `Detected ${el.unclosed.length} unclosed \${ expressions: ${JSON.stringify(el.unclosed[0])}`
    });
}

/**
 * 5. Test HTML Tag Balance
 */
function testPageHtmlTagBalance(reporter, pageName, content) {
    const tagsToCheck = ['form', 'script', 'style', 'table'];
    const balance = parser.checkTagBalance(content, tagsToCheck);

    for (const tag of tagsToCheck) {
        const item = balance[tag];
        reporter.record({
            tier: 2,
            name: `HTML Tag Balance <${tag}> (open=${item.openCount}, close=${item.closeCount})`,
            page: pageName,
            passed: item.balanced,
            details: item.balanced ? `Tag <${tag}> balanced` : `Tag <${tag}> unbalanced: open=${item.openCount}, close=${item.closeCount}`
        });
    }
}

/**
 * 6. Test UTF-8 Encoding Fidelity
 */
function testPageEncodingFidelity(reporter, pageName, content) {
    const hasReplacementChar = content.includes('\uFFFD');
    reporter.record({
        tier: 2,
        name: 'UTF-8 Character Fidelity (Zero \\uFFFD replacement characters)',
        page: pageName,
        passed: !hasReplacementChar,
        details: hasReplacementChar ? 'Corrupt multi-byte sequence found' : 'UTF-8 encoding clean'
    });
}

/**
 * 7. Test page-components.css Reference & Whitelist Integrity
 */
function testStylesheetReferenceIntegrity(reporter) {
    const cssPath = path.join(config.WEBAPP_DIR, config.COMPONENT_STYLESHEET);
    const cssExists = fs.existsSync(cssPath);
    let cssSize = 0;
    if (cssExists) {
        cssSize = fs.statSync(cssPath).size;
    }

    reporter.record({
        tier: 2,
        name: `File Existence: ${config.COMPONENT_STYLESHEET} on filesystem`,
        page: 'page-components.css',
        passed: cssExists && cssSize > 0,
        details: cssExists ? `Stylesheet exists (size: ${cssSize} bytes)` : 'Stylesheet not found on disk'
    });

    // Check header.jsp link
    const headerPath = path.join(config.INCLUDES_DIR, 'header.jsp');
    let headerHasLink = false;
    if (fs.existsSync(headerPath)) {
        const headerContent = parser.readJspFile(headerPath);
        headerHasLink = /href=["'][^"']*styles\/page-components\.css/i.test(headerContent);
    }

    reporter.record({
        tier: 2,
        name: 'includes/header.jsp references styles/page-components.css with contextPath',
        page: 'header.jsp',
        passed: headerHasLink,
        details: headerHasLink ? 'Link found in includes/header.jsp' : 'Link missing in includes/header.jsp'
    });

    // Check login.jsp link
    const loginPath = path.join(config.WEBAPP_DIR, 'login.jsp');
    let loginHasLink = false;
    if (fs.existsSync(loginPath)) {
        const loginContent = parser.readJspFile(loginPath);
        loginHasLink = /href=["'][^"']*styles\/page-components\.css/i.test(loginContent);
    }

    reporter.record({
        tier: 2,
        name: 'login.jsp references styles/page-components.css with contextPath',
        page: 'login.jsp',
        passed: loginHasLink,
        details: loginHasLink ? 'Link found in login.jsp' : 'Link missing in login.jsp'
    });

    // Check AuthFilter whitelist
    const authFilterPath = path.join(config.ROOT_DIR, 'src/main/java/com/teamwork/filters/AuthFilter.java');
    let authWhitelisted = false;
    if (fs.existsSync(authFilterPath)) {
        const authContent = fs.readFileSync(authFilterPath, 'utf8');
        authWhitelisted = authContent.includes('/styles/');
    }

    reporter.record({
        tier: 2,
        name: 'AuthFilter.java whitelists /styles/* without authentication',
        page: 'AuthFilter.java',
        passed: authWhitelisted,
        details: authWhitelisted ? 'Whitelist for /styles/* verified in AuthFilter' : 'Missing /styles/ whitelist in AuthFilter'
    });
}

module.exports = { runTier2Tests };
