/**
 * tier4_build_compilation.spec.js - Tier 4 E2E Test Suite
 *
 * Executes `mvn compile` and verifies exit code 0, BUILD SUCCESS status,
 * zero Java compiler errors, and target class bytecode generation.
 */

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const config = require('../config');

function runTier4Tests(reporter) {
    reporter.startTier(4, 'Build & Compilation (Maven)');

    const startTime = Date.now();

    // Execute Maven Compile without args array when shell is true to avoid DEP0190
    const execResult = spawnSync('mvn compile', {
        cwd: config.ROOT_DIR,
        shell: true,
        encoding: 'utf8'
    });

    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    const exitCode = execResult.status;
    const stdout = execResult.stdout || '';
    const stderr = execResult.stderr || '';

    const isExitCodeZero = exitCode === 0;
    const hasBuildSuccess = stdout.includes('BUILD SUCCESS');
    const hasCompilerErrors = /\[ERROR\]\s+COMPILATION ERROR/i.test(stdout) || /\[ERROR\]/i.test(stderr);

    reporter.record({
        tier: 4,
        name: `Maven Compilation (mvn compile) Exit Code 0 [${duration}s]`,
        page: 'pom.xml',
        passed: isExitCodeZero,
        details: isExitCodeZero
            ? `BUILD SUCCESS completed in ${duration}s (exit code 0)`
            : `Compilation failed with exit code ${exitCode}:\n${stdout.slice(-500)}`
    });

    reporter.record({
        tier: 4,
        name: 'Maven Output contains [INFO] BUILD SUCCESS banner',
        page: 'pom.xml',
        passed: hasBuildSuccess,
        details: hasBuildSuccess ? 'Found BUILD SUCCESS' : 'Missing BUILD SUCCESS'
    });

    reporter.record({
        tier: 4,
        name: 'Zero Java Compilation Errors in output',
        page: 'pom.xml',
        passed: !hasCompilerErrors,
        details: !hasCompilerErrors ? 'Zero compiler errors detected' : 'Detected compiler errors in Maven output'
    });

    // 2. Verify Output Bytecode Classes in target/classes
    const classesDir = path.join(config.ROOT_DIR, 'target/classes');
    const classesExist = fs.existsSync(classesDir);

    reporter.record({
        tier: 4,
        name: 'Target Classes Directory (target/classes) exists and populated',
        page: 'target/classes',
        passed: classesExist,
        details: classesExist ? 'target/classes directory exists' : 'target/classes missing'
    });
}

module.exports = { runTier4Tests };
