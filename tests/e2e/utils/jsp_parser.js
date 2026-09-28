/**
 * jsp_parser.js - Utility module for parsing and analyzing JSP files.
 * Provides resilient, opaque-box inspection of HTML tags, forms,
 * JSTL tags, EL expressions, inline styles, and JavaScript hooks.
 */

const fs = require('fs');
const path = require('path');

/**
 * Read file with UTF-8 encoding.
 */
function readJspFile(filePath) {
    if (!fs.existsSync(filePath)) {
        throw new Error(`File not found: ${filePath}`);
    }
    return fs.readFileSync(filePath, 'utf8');
}

/**
 * Extract attribute value from attribute string.
 * Handles both double-quoted and single-quoted attributes safely.
 */
function getAttr(attrStr, attrName) {
    const doubleQuoteRe = new RegExp(`\\b${attrName}\\s*=\\s*"([^"]*)"`, 'i');
    const singleQuoteRe = new RegExp(`\\b${attrName}\\s*=\\s*'([^']*)'`, 'i');
    const unquotedRe = new RegExp(`\\b${attrName}\\s*=\\s*([^\\s>]+)`, 'i');

    const mDouble = attrStr.match(doubleQuoteRe);
    if (mDouble) return mDouble[1];

    const mSingle = attrStr.match(singleQuoteRe);
    if (mSingle) return mSingle[1];

    const mUnquoted = attrStr.match(unquotedRe);
    if (mUnquoted) return mUnquoted[1];

    return null;
}

/**
 * Parse all forms from JSP content.
 */
function parseForms(content) {
    const forms = [];
    const formRegex = /<form\b([^>]*)>([\s\S]*?)<\/form>/gi;
    let match;

    while ((match = formRegex.exec(content)) !== null) {
        const formAttrStr = match[1];
        const formInner = match[2];

        const formId = getAttr(formAttrStr, 'id');
        const formAction = getAttr(formAttrStr, 'action');
        const formMethod = (getAttr(formAttrStr, 'method') || 'GET').toUpperCase();
        const formClass = getAttr(formAttrStr, 'class');

        const inputs = [];
        const controlRegex = /<(input|textarea|select|button)\b([^>]*)>/gi;
        let cMatch;

        while ((cMatch = controlRegex.exec(formInner)) !== null) {
            const tag = cMatch[1].toLowerCase();
            const attrStr = cMatch[2];

            const name = getAttr(attrStr, 'name');
            const type = getAttr(attrStr, 'type') || (tag === 'textarea' ? 'textarea' : (tag === 'select' ? 'select' : (tag === 'button' ? 'button' : 'text')));
            const id = getAttr(attrStr, 'id');
            const value = getAttr(attrStr, 'value');
            const required = /\brequired\b/i.test(attrStr);
            const placeholder = getAttr(attrStr, 'placeholder');

            inputs.push({
                tag,
                name,
                type: type.toLowerCase(),
                id,
                value,
                required,
                placeholder,
                raw: cMatch[0]
            });
        }

        forms.push({
            id: formId,
            action: formAction,
            method: formMethod,
            class: formClass,
            inputs,
            rawOpening: match[0].split('>')[0] + '>'
        });
    }

    return forms;
}

/**
 * Parse all EL expressions (${...}).
 */
function parseElExpressions(content) {
    const expressions = [];
    const regex = /\$\{([^}]*)\}/g;
    let match;

    while ((match = regex.exec(content)) !== null) {
        expressions.push({
            full: match[0],
            inner: match[1].trim(),
            index: match.index
        });
    }

    // Check for unbalanced ${ without closing }
    const unclosed = [];
    const openRegex = /\$\{/g;
    let oMatch;
    while ((oMatch = openRegex.exec(content)) !== null) {
        const after = content.slice(oMatch.index);
        const closeIdx = after.indexOf('}');
        const nextOpenIdx = after.slice(2).indexOf('${');
        if (closeIdx === -1 || (nextOpenIdx !== -1 && nextOpenIdx + 2 < closeIdx)) {
            unclosed.push({ index: oMatch.index, snippet: after.slice(0, 30) });
        }
    }

    return { expressions, unclosed };
}

/**
 * Match a tag safely while respecting quotes (avoiding premature ending on > inside quotes).
 */
function matchTagsQuoteAware(content, prefixList = ['c', 'fmt', 'fn', 'jsp']) {
    const tags = [];
    const prefixGroup = prefixList.join('|');
    // Start of tag
    const startRegex = new RegExp(`<(/?(?:${prefixGroup}):[a-zA-Z0-9_-]+)`, 'gi');
    let m;

    while ((m = startRegex.exec(content)) !== null) {
        const tagFullName = m[1];
        const startIndex = m.index;
        let inDouble = false;
        let inSingle = false;
        let endIndex = -1;

        for (let i = startIndex + m[0].length; i < content.length; i++) {
            const ch = content[i];
            if (ch === '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (ch === "'" && !inDouble) {
                inSingle = !inSingle;
            } else if (ch === '>' && !inDouble && !inSingle) {
                endIndex = i;
                break;
            }
        }

        if (endIndex !== -1) {
            const fullTag = content.slice(startIndex, endIndex + 1);
            const isClosing = tagFullName.startsWith('/');
            const cleanName = isClosing ? tagFullName.slice(1) : tagFullName;
            const parts = cleanName.split(':');
            const prefix = parts[0].toLowerCase();
            const tagName = parts[1];
            const isSelfClosing = fullTag.endsWith('/>');
            const attrs = fullTag.slice(m[0].length, endIndex - (isSelfClosing ? 1 : 0) - startIndex).trim();

            tags.push({
                full: fullTag,
                prefix,
                tagName,
                isClosing,
                isSelfClosing,
                attrs,
                index: startIndex
            });
        }
    }

    return tags;
}

/**
 * Parse JSTL directives and tags.
 */
function parseJstlTags(content) {
    const directives = [];
    const directiveRegex = /<%@\s*(page|taglib|include)\b([^%]*)%>/gi;
    let dMatch;

    while ((dMatch = directiveRegex.exec(content)) !== null) {
        directives.push({
            type: dMatch[1].toLowerCase(),
            attrs: dMatch[2].trim(),
            full: dMatch[0]
        });
    }

    const jstlTags = matchTagsQuoteAware(content, ['c', 'fmt', 'fn', 'jsp']);
    return { directives, jstlTags };
}

/**
 * Parse all inline styles (style="...").
 * Distinguishes between double-quoted and single-quoted style attributes.
 * Classifies them into static vs dynamic (containing EL ${...}).
 */
function parseInlineStyles(content) {
    const styles = [];
    const doubleQuoteStyleRe = /\bstyle\s*=\s*"([^"]*)"/gi;
    const singleQuoteStyleRe = /\bstyle\s*=\s*'([^']*)'/gi;

    let m;
    while ((m = doubleQuoteStyleRe.exec(content)) !== null) {
        const rawStyle = m[1];
        const isDynamic = /\$\{[^}]+\}/.test(rawStyle);
        styles.push({
            raw: rawStyle,
            isDynamic,
            index: m.index
        });
    }

    while ((m = singleQuoteStyleRe.exec(content)) !== null) {
        const rawStyle = m[1];
        const isDynamic = /\$\{[^}]+\}/.test(rawStyle);
        styles.push({
            raw: rawStyle,
            isDynamic,
            index: m.index
        });
    }

    return {
        all: styles,
        dynamicCount: styles.filter(s => s.isDynamic).length,
        staticCount: styles.filter(s => !s.isDynamic).length,
        staticList: styles.filter(s => !s.isDynamic),
        dynamicList: styles.filter(s => s.isDynamic)
    };
}

/**
 * Check tag balance for specified HTML tags.
 */
function checkTagBalance(content, tagNames = ['div', 'form', 'table', 'aside', 'main', 'nav', 'section', 'script', 'style']) {
    const results = {};

    for (const tag of tagNames) {
        const openRegex = new RegExp(`<${tag}\\b(?:[^"'>]|"[^"]*"|'[^']*')*?(?<!/)>`, 'gi');
        const closeRegex = new RegExp(`</${tag}>`, 'gi');

        const openMatches = content.match(openRegex) || [];
        const closeMatches = content.match(closeRegex) || [];

        results[tag] = {
            openCount: openMatches.length,
            closeCount: closeMatches.length,
            balanced: openMatches.length === closeMatches.length
        };
    }

    return results;
}

/**
 * Find all elements with specific attribute or class.
 */
function findDatasetAttributes(content) {
    const dataAttrs = new Set();
    const regex = /\bdata-([a-zA-Z0-9_-]+)=/g;
    let m;
    while ((m = regex.exec(content)) !== null) {
        dataAttrs.add(`data-${m[1]}`);
    }
    return Array.from(dataAttrs);
}

/**
 * Find all element IDs.
 */
function findElementIds(content) {
    const ids = new Set();
    const regex = /\bid\s*=\s*["']([^"']+)["']/g;
    let m;
    while ((m = regex.exec(content)) !== null) {
        ids.add(m[1]);
    }
    return Array.from(ids);
}

/**
 * Find all classes defined in CSS content.
 */
function parseCssClasses(cssContent) {
    const classes = new Set();
    const regex = /\.([a-zA-Z0-9_-]+)\s*(?:\{|:|,|\s|\.|\>|\+)/g;
    let m;
    while ((m = regex.exec(cssContent)) !== null) {
        classes.add(m[1]);
    }
    return classes;
}

module.exports = {
    readJspFile,
    getAttr,
    parseForms,
    parseElExpressions,
    parseJstlTags,
    parseInlineStyles,
    checkTagBalance,
    findDatasetAttributes,
    findElementIds,
    parseCssClasses
};
