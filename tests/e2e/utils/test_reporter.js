/**
 * test_reporter.js - Formatted test reporting with ANSI colors,
 * structured tables, execution timing, and error details.
 */

const colors = {
    reset: '\x1b[0m',
    bold: '\x1b[1m',
    dim: '\x1b[2m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    cyan: '\x1b[36m',
    white: '\x1b[37m',
    bgBlue: '\x1b[44m',
    bgGreen: '\x1b[42m',
    bgRed: '\x1b[41m'
};

class TestReporter {
    constructor(options = {}) {
        this.verbose = !!options.verbose;
        this.results = [];
        this.startTime = Date.now();
    }

    startTier(tierNumber, tierTitle) {
        console.log(`\n${colors.bold}${colors.bgBlue}${colors.white} TIER ${tierNumber}: ${tierTitle.toUpperCase()} ${colors.reset}\n`);
    }

    record(test) {
        // test: { id, tier, name, page, passed, pending, error, details }
        this.results.push(test);

        const statusIcon = test.passed
            ? `${colors.green}✔ PASS${colors.reset}`
            : (test.pending ? `${colors.yellow}⧖ PEND${colors.reset}` : `${colors.red}✖ FAIL${colors.reset}`);

        const pageLabel = test.page ? `${colors.cyan}[${test.page}]${colors.reset} ` : '';

        console.log(`  ${statusIcon}  ${pageLabel}${test.name}`);

        if ((!test.passed || this.verbose) && test.details) {
            console.log(`     ${colors.dim}${test.details}${colors.reset}`);
        }

        if (!test.passed && test.error) {
            console.log(`     ${colors.red}Error: ${test.error}${colors.reset}`);
        }
    }

    summary() {
        const totalTime = ((Date.now() - this.startTime) / 1000).toFixed(2);
        const total = this.results.length;
        const passed = this.results.filter(r => r.passed).length;
        const failed = this.results.filter(r => !r.passed && !r.pending).length;
        const pending = this.results.filter(r => r.pending).length;

        console.log(`\n${'═'.repeat(70)}`);
        console.log(`${colors.bold}E2E TEST SUITE EXECUTION SUMMARY${colors.reset} (Completed in ${totalTime}s)`);
        console.log(`${'═'.repeat(70)}`);

        // Tier Breakdown
        const tiers = [1, 2, 3, 4];
        console.log(`\n${colors.bold}Tier Breakdown:${colors.reset}`);
        console.log(`  Tier | Description                              | Total | Pass | Fail | Pend`);
        console.log(`  -----+------------------------------------------+-------+------+------+-----`);

        const tierNames = {
            1: 'Tier 1: Feature & Input Preservation    ',
            2: 'Tier 2: Boundary & Syntax Integrity     ',
            3: 'Tier 3: Semantic CSS & Hooks Integrity  ',
            4: 'Tier 4: Build & Compilation (Maven)     '
        };

        for (const t of tiers) {
            const tierTests = this.results.filter(r => r.tier === t);
            const tTotal = tierTests.length;
            const tPass = tierTests.filter(r => r.passed).length;
            const tFail = tierTests.filter(r => !r.passed && !r.pending).length;
            const tPend = tierTests.filter(r => r.pending).length;

            const tName = tierNames[t] || `Tier ${t}                                `;
            console.log(`   ${t}   | ${tName} | ${String(tTotal).padStart(5)} | ${colors.green}${String(tPass).padStart(4)}${colors.reset} | ${tFail > 0 ? colors.red : ''}${String(tFail).padStart(4)}${colors.reset} | ${tPend > 0 ? colors.yellow : ''}${String(tPend).padStart(4)}${colors.reset}`);
        }

        console.log(`  -----+------------------------------------------+-------+------+------+-----`);
        console.log(`  TOTAL: ${total} tests | ${colors.green}Passed: ${passed}${colors.reset} | ${failed > 0 ? colors.red : ''}Failed: ${failed}${colors.reset} | ${pending > 0 ? colors.yellow : ''}Pending Refactor: ${pending}${colors.reset}`);
        console.log(`${'═'.repeat(70)}\n`);

        return {
            total,
            passed,
            failed,
            pending,
            duration: totalTime,
            allPassed: failed === 0
        };
    }
}

module.exports = TestReporter;
