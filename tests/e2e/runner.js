#!/usr/bin/env node

/**
 * runner.js - Master Opaque-Box E2E Test Suite Runner
 *
 * Runs 4-Tier test coverage across all 7 TeamWork Hub JSPs:
 * - Tier 1: Feature & Input Preservation (40 forms, all inputs, IDs, actions)
 * - Tier 2: Boundary & Syntax Integrity (JSTL directives, EL syntax, tag balance)
 * - Tier 3: Semantic CSS & Hooks Integrity (CSS classes, dynamic styles, JS hooks)
 * - Tier 4: Build & Compilation (`mvn compile` with exit code 0)
 *
 * Usage:
 *   node tests/e2e/runner.js                 # Run full 4-tier test suite
 *   node tests/e2e/runner.js --tier 1        # Run Tier 1 only
 *   node tests/e2e/runner.js --page login    # Run tests matching login.jsp
 *   node tests/e2e/runner.js --strict        # Treat pending refactorings as failures
 *   node tests/e2e/runner.js --verbose       # Show verbose details for every test
 *   node tests/e2e/runner.js --json          # Output results as JSON
 */

const fs = require('fs');
const path = require('path');
const TestReporter = require('./utils/test_reporter');
const { runTier1Tests } = require('./specs/tier1_feature_input_preservation.spec');
const { runTier2Tests } = require('./specs/tier2_boundary_syntax_integrity.spec');
const { runTier3Tests } = require('./specs/tier3_semantic_css_hooks.spec');
const { runTier4Tests } = require('./specs/tier4_build_compilation.spec');

// Parse CLI arguments
const args = process.argv.slice(2);
let tierFilter = null;
let pageFilter = null;
let isStrict = false;
let isVerbose = false;
let isJson = false;

for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--tier' && args[i + 1]) {
        tierFilter = parseInt(args[i + 1], 10);
        i++;
    } else if (arg === '--page' && args[i + 1]) {
        pageFilter = args[i + 1];
        i++;
    } else if (arg === '--strict') {
        isStrict = true;
    } else if (arg === '--verbose') {
        isVerbose = true;
    } else if (arg === '--json') {
        isJson = true;
    } else if (arg === '--help' || arg === '-h') {
        printHelp();
        process.exit(0);
    }
}

function printHelp() {
    console.log(`
TeamWork Hub - Automated Opaque-Box E2E Test Suite Runner

Usage:
  node tests/e2e/runner.js [options]

Options:
  --tier <1-4>     Execute tests for a specific tier only
  --page <name>    Filter tests for a specific JSP page (e.g. login, tasks)
  --strict         Treat pending refactorings (M2/M3) as test failures
  --verbose        Display full verification details for all passing tests
  --json           Print test report output in structured JSON format
  --help, -h       Display this help documentation
`);
}

function main() {
    const reporter = new TestReporter({ verbose: isVerbose });

    if (!isJson) {
        console.log(`\n======================================================================`);
        console.log(`  TEAMWORK HUB - OPAQUE-BOX E2E TEST SUITE RUNNER`);
        console.log(`  Target: Jakarta EE 10 / Tomcat 10.1 / Java 21 / 7 JSP Views`);
        console.log(`  Filters: Tier=${tierFilter || 'ALL'}, Page=${pageFilter || 'ALL'}, Strict=${isStrict}`);
        console.log(`======================================================================`);
    }

    // Execute Selected Tiers
    if (!tierFilter || tierFilter === 1) {
        runTier1Tests(reporter, pageFilter);
    }

    if (!tierFilter || tierFilter === 2) {
        runTier2Tests(reporter, pageFilter);
    }

    if (!tierFilter || tierFilter === 3) {
        runTier3Tests(reporter, pageFilter, { strict: isStrict });
    }

    if (!tierFilter || tierFilter === 4) {
        // Tier 4 compiles entire project; skip if filtering to a single page unless explicitly asked
        if (!pageFilter || tierFilter === 4) {
            runTier4Tests(reporter);
        }
    }

    if (isJson) {
        const summaryData = {
            total: reporter.results.length,
            passed: reporter.results.filter(r => r.passed).length,
            failed: reporter.results.filter(r => !r.passed && !r.pending).length,
            pending: reporter.results.filter(r => r.pending).length,
            results: reporter.results
        };
        console.log(JSON.stringify(summaryData, null, 2));
        process.exit(summaryData.failed > 0 ? 1 : 0);
    } else {
        const summary = reporter.summary();
        if (summary.failed > 0) {
            process.exit(1);
        } else {
            process.exit(0);
        }
    }
}

main();
