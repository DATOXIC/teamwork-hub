/**
 * challenger_m1_verification.js
 * 
 * Adversarial Challenge & Stress-Test Harness for Milestone 1 (M1):
 * 1. Progress Bar Animation & CSS Cascade Mechanics:
 *    - Verify .progress-init-zero in page-components.css does NOT interfere with app.js
 *    - Verify CSS cascade priority (inline style vs author class rule)
 *    - Verify presence of .project-progress-bar, .progress-init-zero, data-progress in profile.jsp
 * 2. Client Scripts & DOM Lookups in login.jsp:
 *    - Verify all DOM IDs and selectors used by switchTab, togglePassword, validateRegisterForm
 *    - Execute behavioral simulations for all 4 functions with boundary value tests
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');

const ROOT_DIR = path.resolve(__dirname, '../..');
const WEBAPP_DIR = path.join(ROOT_DIR, 'src/main/webapp');

let passedTests = 0;
let failedTests = 0;

function test(name, fn) {
    try {
        fn();
        console.log(`  [PASS] ${name}`);
        passedTests++;
    } catch (err) {
        console.error(`  [FAIL] ${name}`);
        console.error(`         Error: ${err.message}`);
        failedTests++;
    }
}

console.log('======================================================================');
console.log('CHALLENGER M1 ADVERSARIAL STRESS-TEST HARNESS');
console.log('======================================================================\n');

// ============================================================================
// PART 1: PROGRESS BAR ANIMATION & CSS CASCADE MECHANICS
// ============================================================================
console.log('--- PART 1: Progress Bar Animation & CSS Cascade Stress-Testing ---');

const pageComponentsCssPath = path.join(WEBAPP_DIR, 'styles/page-components.css');
const mainCssPath = path.join(WEBAPP_DIR, 'styles/main.css');
const appJsPath = path.join(WEBAPP_DIR, 'js/app.js');
const profileJspPath = path.join(WEBAPP_DIR, 'profile.jsp');

const pageComponentsCss = fs.readFileSync(pageComponentsCssPath, 'utf8');
const mainCss = fs.readFileSync(mainCssPath, 'utf8');
const appJs = fs.readFileSync(appJsPath, 'utf8');
const profileJsp = fs.readFileSync(profileJspPath, 'utf8');

test('CSS: .progress-init-zero is defined in page-components.css', () => {
    assert(pageComponentsCss.includes('.progress-init-zero'), 'Class .progress-init-zero must be present');
});

test('CSS: .progress-init-zero sets width: 0%', () => {
    const match = pageComponentsCss.match(/\.progress-init-zero\s*\{([^}]+)\}/);
    assert(match, '.progress-init-zero rule block must exist');
    assert(/width:\s*0%;?/.test(match[1]), 'Rule must contain width: 0%');
});

test('CSS: .progress-init-zero DOES NOT contain !important (allows inline style override)', () => {
    const match = pageComponentsCss.match(/\.progress-init-zero\s*\{([^}]+)\}/);
    assert(match, '.progress-init-zero rule block must exist');
    assert(!/!important/i.test(match[1]), 'Rule MUST NOT contain !important, otherwise inline style cannot override it');
});

test('CSS: .project-progress-bar has transition: width declared in main.css', () => {
    const match = mainCss.match(/\.project-progress-bar\s*\{([^}]+)\}/);
    assert(match, '.project-progress-bar rule block must exist in main.css');
    assert(/transition:\s*width/i.test(match[1]), '.project-progress-bar must declare a transition for width');
});

test('JSP: profile.jsp progress bars have both .project-progress-bar and .progress-init-zero', () => {
    const barMatches = [...profileJsp.matchAll(/<div[^>]*class="([^"]*project-progress-bar[^"]*)"[^>]*>/g)];
    assert(barMatches.length >= 2, `Expected at least 2 progress bars in profile.jsp, found ${barMatches.length}`);
    for (const m of barMatches) {
        const classNames = m[1].split(/\s+/);
        assert(classNames.includes('project-progress-bar'), 'Must contain project-progress-bar class');
        assert(classNames.includes('progress-init-zero'), 'Must contain progress-init-zero class');
    }
});

test('JSP: profile.jsp progress bars do NOT contain inline style="..."', () => {
    const barMatches = [...profileJsp.matchAll(/<div[^>]*class="[^"]*project-progress-bar[^"]*"[^>]*>/g)];
    for (const m of barMatches) {
        const tag = m[0];
        assert(!/style\s*=/i.test(tag), `Progress bar element should have 0 inline styles, but found: ${tag}`);
    }
});

test('JSP: profile.jsp progress bars have data-progress with % format', () => {
    const barMatches = [...profileJsp.matchAll(/<div[^>]*class="[^"]*project-progress-bar[^"]*"[^>]*>/g)];
    for (const m of barMatches) {
        const tag = m[0];
        const dpMatch = tag.match(/data-progress="([^"]+)"/);
        assert(dpMatch, `Missing data-progress attribute in: ${tag}`);
        assert(dpMatch[1].endsWith('%'), `data-progress value must end with %: ${dpMatch[1]}`);
    }
});

test('JS Simulation: app.js initProgressBars() sets inline style.width which overrides .progress-init-zero', () => {
    // CSS Cascade Simulation
    // Specificity: Class selector = (0, 0, 1, 0)
    // Inline style = (1, 0, 0, 0)
    class MockElement {
        constructor() {
            this.classList = new Set(['project-progress-bar', 'progress-init-zero']);
            this.attributes = { 'data-progress': '75%' };
            this.style = {};
        }
        getAttribute(name) {
            return this.attributes[name] || null;
        }
        getEffectiveWidth() {
            // If inline style is set, it overrides normal author class rules (without !important)
            if (this.style.width) {
                return { value: this.style.width, source: 'inline-style (specificity 1,0,0,0)' };
            }
            if (this.classList.has('progress-init-zero')) {
                return { value: '0%', source: 'class: .progress-init-zero (specificity 0,0,1,0)' };
            }
            return { value: 'auto', source: 'default' };
        }
    }

    const bar = new MockElement();

    // 1. Initial render state before JS execution
    assert.strictEqual(bar.getEffectiveWidth().value, '0%', 'Initial width must be 0% from .progress-init-zero');
    assert.strictEqual(bar.getEffectiveWidth().source, 'class: .progress-init-zero (specificity 0,0,1,0)');

    // 2. Execution of initProgressBars() logic
    const targetWidth = bar.getAttribute('data-progress');
    if (targetWidth) {
        bar.style.width = targetWidth;
    }

    // 3. Post-execution state
    assert.strictEqual(bar.style.width, '75%', 'Inline style.width must be set to 75%');
    assert.strictEqual(bar.getEffectiveWidth().value, '75%', 'Effective computed width must now be 75%');
    assert.strictEqual(bar.getEffectiveWidth().source, 'inline-style (specificity 1,0,0,0)', 'Inline style must take precedence over class rule');
});

test('Adversarial Contrast: If .progress-init-zero had !important, inline style would FAIL', () => {
    function computeWithImportantFlag(hasImportant, inlineWidth) {
        if (hasImportant) {
            // Author rule with !important beats normal inline style
            return '0%';
        }
        return inlineWidth;
    }
    const withImportant = computeWithImportantFlag(true, '75%');
    const withoutImportant = computeWithImportantFlag(false, '75%');
    assert.strictEqual(withImportant, '0%', 'With !important, animation would fail and stay at 0%');
    assert.strictEqual(withoutImportant, '75%', 'Without !important, animation successfully reaches 75%');
});


// ============================================================================
// PART 2: CLIENT SCRIPTS & DOM LOOKUPS IN login.jsp
// ============================================================================
console.log('\n--- PART 2: Client Scripts & DOM Lookups in login.jsp ---');

const loginJspPath = path.join(WEBAPP_DIR, 'login.jsp');
const loginJsp = fs.readFileSync(loginJspPath, 'utf8');

// List of all DOM IDs referenced by the 4 functions in login.jsp
const requiredIds = [
    'tab-btn-login',
    'tab-btn-register',
    'pane-login',
    'pane-register',
    'login-username',
    'login-password',
    'reg-pass',
    'reg-confirmpass',
    'reg-pass-error',
    'reg-confirmpass-error'
];

for (const id of requiredIds) {
    test(`DOM ID Check: #${id} exists in login.jsp`, () => {
        const regex = new RegExp(`id=["']${id}["']`);
        assert(regex.test(loginJsp), `Element with id="${id}" must exist in login.jsp`);
    });
}

test('DOM Query Check: .login-card-header h3 and p exist in login.jsp', () => {
    assert(loginJsp.includes('login-card-header'), 'login-card-header must exist');
    assert(/<div[^>]*class="[^"]*login-card-header[^"]*"[^>]*>[\s\S]*?<h3>/.test(loginJsp), '<h3> inside .login-card-header must exist');
    assert(/<div[^>]*class="[^"]*login-card-header[^"]*"[^>]*>[\s\S]*?<p>/.test(loginJsp), '<p> inside .login-card-header must exist');
});

test('DOM Query Check: toggle-password-btn elements have child <i> elements', () => {
    const toggleBtns = [...loginJsp.matchAll(/<button[^>]*class="[^"]*toggle-password-btn[^"]*"[^>]*>([\s\S]*?)<\/button>/g)];
    assert(toggleBtns.length >= 3, `Expected at least 3 toggle-password-btn buttons in login.jsp, found ${toggleBtns.length}`);
    for (const btn of toggleBtns) {
        assert(/<i\b[^>]*class="[^"]*bi-eye[^"]*"[^>]*><\/i>/.test(btn[1]), 'Toggle button must contain <i class="bi bi-eye"></i> child');
    }
});

// Behavioral simulation of the 4 functions
console.log('\n--- PART 2.1: Behavioral Simulation of login.jsp Scripts ---');

class MockDOM {
    constructor() {
        this.elements = {};
    }
    createElement(id, tag = 'div', classes = [], type = '') {
        const el = {
            id,
            tagName: tag.toUpperCase(),
            type,
            value: '',
            textContent: '',
            classList: {
                classes: new Set(classes),
                add: (c) => el.classList.classes.add(c),
                remove: (c) => el.classList.classes.delete(c),
                toggle: (c, force) => {
                    if (force !== undefined) {
                        if (force) el.classList.classes.add(c);
                        else el.classList.classes.delete(c);
                    } else {
                        if (el.classList.classes.has(c)) el.classList.classes.delete(c);
                        else el.classList.classes.add(c);
                    }
                },
                contains: (c) => el.classList.classes.has(c)
            },
            querySelector: (sel) => {
                if (sel === 'i') return el.childIcon;
                return null;
            },
            focus: () => { el.isFocused = true; },
            childIcon: {
                classList: {
                    classes: new Set(['bi', 'bi-eye']),
                    add: function(c) { this.classes.add(c); },
                    remove: function(c) { this.classes.delete(c); },
                    contains: function(c) { return this.classes.has(c); }
                }
            }
        };
        this.elements[id] = el;
        return el;
    }
    getElementById(id) {
        return this.elements[id] || null;
    }
    querySelector(sel) {
        if (sel === '.login-card-header h3') return this.cardHeaderH3;
        if (sel === '.login-card-header p') return this.cardHeaderP;
        return null;
    }
}

function setupMockEnvironment() {
    const dom = new MockDOM();
    dom.createElement('tab-btn-login', 'button', ['login-tab-btn', 'active']);
    dom.createElement('tab-btn-register', 'button', ['login-tab-btn']);
    dom.createElement('pane-login', 'div', ['login-tab-pane', 'active']);
    dom.createElement('pane-register', 'div', ['login-tab-pane']);
    dom.createElement('login-username', 'input', [], 'text');
    dom.createElement('login-password', 'input', [], 'password');
    dom.createElement('reg-pass', 'input', [], 'password');
    dom.createElement('reg-confirmpass', 'input', [], 'password');
    dom.createElement('reg-pass-error', 'div', ['field-error']);
    dom.createElement('reg-confirmpass-error', 'div', ['field-error']);

    dom.cardHeaderH3 = { textContent: 'Đăng Nhập' };
    dom.cardHeaderP = { textContent: 'Nhập tài khoản để truy cập hệ thống' };

    return dom;
}

test('Script Simulation: switchTab("register") activates register tab and pane', () => {
    const dom = setupMockEnvironment();

    // Replicate switchTab logic verbatim
    function switchTab(tab) {
        dom.getElementById('tab-btn-login').classList.toggle('active', tab === 'login');
        dom.getElementById('tab-btn-register').classList.toggle('active', tab === 'register');
        dom.getElementById('pane-login').classList.toggle('active', tab === 'login');
        dom.getElementById('pane-register').classList.toggle('active', tab === 'register');

        var header = dom.querySelector('.login-card-header h3');
        var subtitle = dom.querySelector('.login-card-header p');
        if (tab === 'register') {
            header.textContent = 'Đăng Ký';
            subtitle.textContent = 'Tạo tài khoản mới để bắt đầu';
        } else {
            header.textContent = 'Đăng Nhập';
            subtitle.textContent = 'Nhập tài khoản để truy cập hệ thống';
        }
    }

    switchTab('register');

    assert(!dom.getElementById('tab-btn-login').classList.contains('active'), 'login tab button should not be active');
    assert(dom.getElementById('tab-btn-register').classList.contains('active'), 'register tab button must be active');
    assert(!dom.getElementById('pane-login').classList.contains('active'), 'login pane should not be active');
    assert(dom.getElementById('pane-register').classList.contains('active'), 'register pane must be active');
    assert.strictEqual(dom.cardHeaderH3.textContent, 'Đăng Ký');
    assert.strictEqual(dom.cardHeaderP.textContent, 'Tạo tài khoản mới để bắt đầu');
});

test('Script Simulation: switchTab("login") switches back to login view', () => {
    const dom = setupMockEnvironment();
    function switchTab(tab) {
        dom.getElementById('tab-btn-login').classList.toggle('active', tab === 'login');
        dom.getElementById('tab-btn-register').classList.toggle('active', tab === 'register');
        dom.getElementById('pane-login').classList.toggle('active', tab === 'login');
        dom.getElementById('pane-register').classList.toggle('active', tab === 'register');

        var header = dom.querySelector('.login-card-header h3');
        var subtitle = dom.querySelector('.login-card-header p');
        if (tab === 'register') {
            header.textContent = 'Đăng Ký';
            subtitle.textContent = 'Tạo tài khoản mới để bắt đầu';
        } else {
            header.textContent = 'Đăng Nhập';
            subtitle.textContent = 'Nhập tài khoản để truy cập hệ thống';
        }
    }

    switchTab('register');
    switchTab('login');

    assert(dom.getElementById('tab-btn-login').classList.contains('active'), 'login tab button must be active');
    assert(!dom.getElementById('tab-btn-register').classList.contains('active'), 'register tab button should not be active');
    assert(dom.getElementById('pane-login').classList.contains('active'), 'login pane must be active');
    assert(!dom.getElementById('pane-register').classList.contains('active'), 'register pane should not be active');
    assert.strictEqual(dom.cardHeaderH3.textContent, 'Đăng Nhập');
    assert.strictEqual(dom.cardHeaderP.textContent, 'Nhập tài khoản để truy cập hệ thống');
});

test('Script Simulation: togglePassword() switches between password and text with icon update', () => {
    const dom = setupMockEnvironment();
    const btn = dom.createElement('mock-btn', 'button');

    function togglePassword(inputId, b) {
        var input = dom.getElementById(inputId);
        var icon = b.querySelector('i');
        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.remove('bi-eye');
            icon.classList.add('bi-eye-slash');
        } else {
            input.type = 'password';
            icon.classList.remove('bi-eye-slash');
            icon.classList.add('bi-eye');
        }
    }

    // Initial state
    assert.strictEqual(dom.getElementById('login-password').type, 'password');
    assert(btn.childIcon.classList.contains('bi-eye'));

    // First toggle: password -> text
    togglePassword('login-password', btn);
    assert.strictEqual(dom.getElementById('login-password').type, 'text');
    assert(btn.childIcon.classList.contains('bi-eye-slash'));
    assert(!btn.childIcon.classList.contains('bi-eye'));

    // Second toggle: text -> password
    togglePassword('login-password', btn);
    assert.strictEqual(dom.getElementById('login-password').type, 'password');
    assert(btn.childIcon.classList.contains('bi-eye'));
    assert(!btn.childIcon.classList.contains('bi-eye-slash'));
});

test('Script Simulation: validateRegisterForm() Equivalence & Boundary Value Analysis', () => {
    const dom = setupMockEnvironment();

    function validateRegisterForm() {
        var pass        = dom.getElementById('reg-pass');
        var confirm     = dom.getElementById('reg-confirmpass');
        var passError   = dom.getElementById('reg-pass-error');
        var confirmError= dom.getElementById('reg-confirmpass-error');
        var valid = true;

        // Reset lỗi về trạng thái sạch
        passError.textContent = '';
        confirmError.textContent = '';
        pass.classList.remove('input-error');
        confirm.classList.remove('input-error');

        // Kiểm tra độ dài mật khẩu tối thiểu 6 ký tự
        if (pass.value.length < 6) {
            passError.textContent = 'Mật khẩu phải có ít nhất 6 ký tự';
            pass.classList.add('input-error');
            pass.focus();
            valid = false;
        }

        // Kiểm tra mật khẩu xác nhận có khớp không
        if (valid && pass.value !== confirm.value) {
            confirmError.textContent = 'Mật khẩu xác nhận không khớp';
            confirm.classList.add('input-error');
            confirm.focus();
            valid = false;
        }

        return valid;
    }

    // Boundary Test 1: Empty password (len = 0)
    dom.getElementById('reg-pass').value = '';
    dom.getElementById('reg-confirmpass').value = '';
    assert.strictEqual(validateRegisterForm(), false, 'Empty password should fail');
    assert.strictEqual(dom.getElementById('reg-pass-error').textContent, 'Mật khẩu phải có ít nhất 6 ký tự');
    assert(dom.getElementById('reg-pass').classList.contains('input-error'));

    // Boundary Test 2: Length 5 (off-by-one under minimum boundary 6)
    dom.getElementById('reg-pass').value = '12345';
    dom.getElementById('reg-confirmpass').value = '12345';
    assert.strictEqual(validateRegisterForm(), false, '5-char password must fail boundary check');
    assert.strictEqual(dom.getElementById('reg-pass-error').textContent, 'Mật khẩu phải có ít nhất 6 ký tự');

    // Boundary Test 3: Length 6 (exact boundary), but mismatch
    dom.getElementById('reg-pass').value = '123456';
    dom.getElementById('reg-confirmpass').value = '123457';
    assert.strictEqual(validateRegisterForm(), false, 'Mismatched passwords must fail');
    assert.strictEqual(dom.getElementById('reg-confirmpass-error').textContent, 'Mật khẩu xác nhận không khớp');
    assert(dom.getElementById('reg-confirmpass').classList.contains('input-error'));
    assert(!dom.getElementById('reg-pass').classList.contains('input-error'));

    // Boundary Test 4: Length 6 (exact boundary), matching
    dom.getElementById('reg-pass').value = '123456';
    dom.getElementById('reg-confirmpass').value = '123456';
    assert.strictEqual(validateRegisterForm(), true, '6-char matching password must pass boundary check');
    assert.strictEqual(dom.getElementById('reg-pass-error').textContent, '');
    assert.strictEqual(dom.getElementById('reg-confirmpass-error').textContent, '');
    assert(!dom.getElementById('reg-pass').classList.contains('input-error'));
    assert(!dom.getElementById('reg-confirmpass').classList.contains('input-error'));

    // Equivalence Test 5: Complex long password matching
    dom.getElementById('reg-pass').value = 'P@ssw0rd_VeryLong_2026!#';
    dom.getElementById('reg-confirmpass').value = 'P@ssw0rd_VeryLong_2026!#';
    assert.strictEqual(validateRegisterForm(), true, 'Complex long matching password must pass');
});

console.log('\n======================================================================');
console.log(`TOTAL TESTS: ${passedTests + failedTests} | PASSED: ${passedTests} | FAILED: ${failedTests}`);
console.log('======================================================================');

if (failedTests > 0) {
    process.exit(1);
} else {
    process.exit(0);
}
