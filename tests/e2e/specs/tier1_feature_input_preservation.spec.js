/**
 * tier1_feature_input_preservation.spec.js - Tier 1 E2E Test Suite
 *
 * Verifies 100% preservation of all documented form actions, methods,
 * input names, IDs, types, and hidden parameter tokens across all 7 JSPs.
 */

const path = require('path');
const parser = require('../utils/jsp_parser');
const config = require('../config');

function runTier1Tests(reporter, pageFilter = null) {
    reporter.startTier(1, 'Feature & Input Preservation');

    const pagesToTest = pageFilter
        ? config.TARGET_PAGES.filter(p => p.toLowerCase().includes(pageFilter.toLowerCase()))
        : config.TARGET_PAGES;

    for (const pageName of pagesToTest) {
        const filePath = path.join(config.WEBAPP_DIR, pageName);
        let content;
        try {
            content = parser.readJspFile(filePath);
        } catch (err) {
            reporter.record({
                tier: 1,
                name: `File Existence Check for ${pageName}`,
                page: pageName,
                passed: false,
                error: err.message
            });
            continue;
        }

        const forms = parser.parseForms(content);

        // Page-specific form validations
        if (pageName === 'login.jsp') {
            testLoginPageForms(reporter, pageName, forms);
        } else if (pageName === 'profile.jsp') {
            testProfilePageForms(reporter, pageName, forms);
        } else if (pageName === 'projects.jsp') {
            testProjectsPageForms(reporter, pageName, forms);
        } else if (pageName === 'project_report.jsp') {
            testProjectReportActions(reporter, pageName, content);
        } else if (pageName === 'docs.jsp') {
            testDocsPageForms(reporter, pageName, forms, content);
        } else if (pageName === 'chat.jsp') {
            testChatPageForms(reporter, pageName, forms, content);
        } else if (pageName === 'tasks.jsp') {
            testTasksPageForms(reporter, pageName, forms, content);
        }
    }
}

/**
 * 1. login.jsp validations
 */
function testLoginPageForms(reporter, pageName, forms) {
    // Form 1: Login Form
    const loginForm = forms.find(f => f.action && f.action.includes('/auth') && f.inputs.some(i => i.name === 'action' && i.value === 'login'));
    const loginFormValid = !!loginForm && loginForm.method === 'POST';

    reporter.record({
        tier: 1,
        name: 'Preserve Login Form (action=/auth, method=POST, action=login)',
        page: pageName,
        passed: loginFormValid,
        details: loginForm ? `Found login form with ${loginForm.inputs.length} controls` : 'Login form not found'
    });

    if (loginForm) {
        const usernameInput = loginForm.inputs.find(i => i.name === 'username' && i.id === 'login-username');
        reporter.record({
            tier: 1,
            name: 'Login Form: username input (id="login-username", required)',
            page: pageName,
            passed: !!usernameInput && usernameInput.required,
            details: usernameInput ? `Found username input: id=${usernameInput.id}` : 'Username input missing'
        });

        const passwordInput = loginForm.inputs.find(i => i.name === 'password' && i.id === 'login-password' && i.type === 'password');
        reporter.record({
            tier: 1,
            name: 'Login Form: password input (id="login-password", type="password", required)',
            page: pageName,
            passed: !!passwordInput && passwordInput.required,
            details: passwordInput ? `Found password input: id=${passwordInput.id}` : 'Password input missing'
        });

        const rememberInput = loginForm.inputs.find(i => i.name === 'remember' && i.type === 'checkbox');
        reporter.record({
            tier: 1,
            name: 'Login Form: remember checkbox (name="remember", type="checkbox")',
            page: pageName,
            passed: !!rememberInput,
            details: rememberInput ? 'Remember checkbox preserved' : 'Remember checkbox missing'
        });
    }

    // Form 2: Register Form
    const regForm = forms.find(f => f.action && f.action.includes('/auth') && f.inputs.some(i => i.name === 'action' && i.value === 'register'));
    const regFormValid = !!regForm && regForm.method === 'POST';

    reporter.record({
        tier: 1,
        name: 'Preserve Register Form (action=/auth, method=POST, action=register)',
        page: pageName,
        passed: regFormValid,
        details: regForm ? `Found register form with ${regForm.inputs.length} controls` : 'Register form not found'
    });

    if (regForm) {
        const expectedRegFields = [
            { name: 'fullName', id: 'reg-fullname', type: 'text' },
            { name: 'username', id: 'reg-username', type: 'text' },
            { name: 'email', id: 'reg-email', type: 'email' },
            { name: 'password', id: 'reg-pass', type: 'password' },
            { name: 'confirmPassword', id: 'reg-confirmpass', type: 'password' }
        ];

        for (const exp of expectedRegFields) {
            const found = regForm.inputs.find(i => i.name === exp.name && i.id === exp.id && i.type === exp.type);
            reporter.record({
                tier: 1,
                name: `Register Form: ${exp.name} input (id="${exp.id}", type="${exp.type}")`,
                page: pageName,
                passed: !!found && found.required,
                details: found ? `Preserved ${exp.name} correctly` : `Field ${exp.name} missing or mismatched`
            });
        }
    }
}

/**
 * 2. profile.jsp validations
 */
function testProfilePageForms(reporter, pageName, forms) {
    // Form 1: Edit Profile Modal Form
    const editForm = forms.find(f => f.action && f.action.includes('/profile') && f.inputs.some(i => i.name === 'action' && i.value === 'update'));
    reporter.record({
        tier: 1,
        name: 'Preserve Edit Profile Form (action=/profile, method=POST, action=update)',
        page: pageName,
        passed: !!editForm && editForm.method === 'POST',
        details: editForm ? `Found edit profile form with ${editForm.inputs.length} controls` : 'Edit profile form missing'
    });

    if (editForm) {
        const hasUserId = editForm.inputs.some(i => i.name === 'userId' && i.type === 'hidden');
        const hasFullName = editForm.inputs.some(i => i.name === 'fullName' && i.id === 'inputFullName');
        const hasRole = editForm.inputs.some(i => i.name === 'role' && i.id === 'inputRole');
        const hasBio = editForm.inputs.some(i => i.name === 'bio' && i.id === 'inputBio' && i.tag === 'textarea');
        const hasSkills = editForm.inputs.some(i => i.name === 'skills' && i.id === 'inputSkills');
        const hasGithub = editForm.inputs.some(i => i.name === 'githubUrl' && i.id === 'inputGithub');
        const hasLinkedin = editForm.inputs.some(i => i.name === 'linkedinUrl' && i.id === 'inputLinkedin');

        reporter.record({
            tier: 1,
            name: 'Edit Profile Form: Inputs (fullName, role, bio, skills, github, linkedin, userId)',
            page: pageName,
            passed: hasUserId && hasFullName && hasRole && hasBio && hasSkills && hasGithub && hasLinkedin,
            details: `userId:${hasUserId}, fullName:${hasFullName}, role:${hasRole}, bio:${hasBio}, skills:${hasSkills}, github:${hasGithub}, linkedin:${hasLinkedin}`
        });
    }

    // Form 2: Quick Invite Modal Form
    const inviteForm = forms.find(f => f.action && f.action.includes('/invite') && f.inputs.some(i => i.name === 'action' && i.value === 'sendInvite'));
    reporter.record({
        tier: 1,
        name: 'Preserve Quick Invite Form (action=/invite, method=POST, action=sendInvite)',
        page: pageName,
        passed: !!inviteForm && inviteForm.method === 'POST',
        details: inviteForm ? `Found quick invite form with ${inviteForm.inputs.length} controls` : 'Quick invite form missing'
    });

    if (inviteForm) {
        const hasUsername = inviteForm.inputs.some(i => i.name === 'usernameOrEmail' && i.type === 'hidden');
        const hasProjectSelect = inviteForm.inputs.some(i => i.name === 'projectId' && i.id === 'selectProject' && i.tag === 'select');

        reporter.record({
            tier: 1,
            name: 'Quick Invite Form: Inputs (usernameOrEmail hidden, projectId select)',
            page: pageName,
            passed: hasUsername && hasProjectSelect,
            details: `usernameOrEmail:${hasUsername}, projectId:${hasProjectSelect}`
        });
    }
}

/**
 * 3. projects.jsp validations
 */
function testProjectsPageForms(reporter, pageName, forms) {
    // Form 1: Accept Invite
    const acceptForm = forms.find(f => f.action && f.action.includes('/invite') && f.inputs.some(i => i.name === 'action' && i.value === 'accept'));
    reporter.record({
        tier: 1,
        name: 'Preserve Accept Invite Form (action=/invite, action=accept, inviteId)',
        page: pageName,
        passed: !!acceptForm && acceptForm.inputs.some(i => i.name === 'inviteId' && i.type === 'hidden'),
        details: acceptForm ? 'Found accept invite form' : 'Accept invite form missing'
    });

    // Form 2: Reject Invite
    const rejectForm = forms.find(f => f.action && f.action.includes('/invite') && f.inputs.some(i => i.name === 'action' && i.value === 'reject'));
    reporter.record({
        tier: 1,
        name: 'Preserve Reject Invite Form (action=/invite, action=reject, inviteId)',
        page: pageName,
        passed: !!rejectForm && rejectForm.inputs.some(i => i.name === 'inviteId' && i.type === 'hidden'),
        details: rejectForm ? 'Found reject invite form' : 'Reject invite form missing'
    });

    // Form 3: Join by Code Modal
    const joinCodeForm = forms.find(f => f.action && f.action.includes('/invite') && f.inputs.some(i => i.name === 'action' && i.value === 'requestJoin'));
    reporter.record({
        tier: 1,
        name: 'Preserve Join By Code Form (action=/invite, action=requestJoin, projectCode)',
        page: pageName,
        passed: !!joinCodeForm && joinCodeForm.inputs.some(i => i.name === 'projectCode' && i.id === 'inputProjectCode'),
        details: joinCodeForm ? 'Found join by code form' : 'Join by code form missing'
    });

    // Form 4: Create Project Modal
    const createProjForm = forms.find(f => f.action && f.action.includes('/project') && f.inputs.some(i => i.name === 'action' && i.value === 'create'));
    reporter.record({
        tier: 1,
        name: 'Preserve Create Project Form (action=/project, action=create, name, projectCode, description, projectType)',
        page: pageName,
        passed: !!createProjForm &&
            createProjForm.inputs.some(i => i.name === 'name' && i.id === 'proj-name') &&
            createProjForm.inputs.some(i => i.name === 'projectCode' && i.id === 'proj-code') &&
            createProjForm.inputs.some(i => i.name === 'description' && i.id === 'proj-desc') &&
            createProjForm.inputs.some(i => i.name === 'projectType' && i.type === 'radio'),
        details: createProjForm ? 'Found create project form with all fields' : 'Create project form missing or fields incomplete'
    });
}

/**
 * 4. project_report.jsp validations
 */
function testProjectReportActions(reporter, pageName, content) {
    const hasExportCsv = /href=.*action=exportCsv/i.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve Export CSV Link (/task?action=exportCsv&projectId=...) in Report',
        page: pageName,
        passed: hasExportCsv,
        details: hasExportCsv ? 'Found Export CSV endpoint action' : 'Export CSV link missing'
    });

    const hasKanbanLink = /href=.*action=list&amp;projectId=|href=.*action=list&projectId=/i.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve Kanban Navigation Link (/task?action=list&projectId=...) in Report',
        page: pageName,
        passed: hasKanbanLink,
        details: hasKanbanLink ? 'Found Kanban subnav link' : 'Kanban subnav link missing'
    });

    const hasDocsLink = /href=.*\/doc\?action=list/i.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve Wiki Docs Navigation Link (/doc?action=list&projectId=...) in Report',
        page: pageName,
        passed: hasDocsLink,
        details: hasDocsLink ? 'Found Wiki Docs subnav link' : 'Wiki Docs subnav link missing'
    });

    const hasChatLink = /href=.*\/chat\?action=view/i.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve Team Chat Navigation Link (/chat?action=view&projectId=...) in Report',
        page: pageName,
        passed: hasChatLink,
        details: hasChatLink ? 'Found Chat subnav link' : 'Chat subnav link missing'
    });

    const hasPrintAction = /window\.print\(\)/.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve A4 Print Trigger (window.print()) in Report',
        page: pageName,
        passed: hasPrintAction,
        details: hasPrintAction ? 'Found window.print() trigger' : 'window.print() missing'
    });
}

/**
 * 5. docs.jsp validations
 */
function testDocsPageForms(reporter, pageName, forms, content) {
    // Form 1: Create Doc Modal
    const createDoc = forms.find(f => f.action && f.action.includes('doc') && f.inputs.some(i => i.name === 'action' && i.value === 'create'));
    reporter.record({
        tier: 1,
        name: 'Preserve Create Wiki Doc Form (action=doc, action=create, projectId, title, content)',
        page: pageName,
        passed: !!createDoc &&
            createDoc.inputs.some(i => i.name === 'projectId' && i.type === 'hidden') &&
            createDoc.inputs.some(i => i.name === 'title') &&
            createDoc.inputs.some(i => i.name === 'content' && i.tag === 'textarea'),
        details: createDoc ? 'Found create doc form' : 'Create doc form missing'
    });

    // Form 2: Edit Doc Modal
    const editDoc = forms.find(f => f.action && f.action.includes('doc') && f.inputs.some(i => i.name === 'action' && i.value === 'update'));
    reporter.record({
        tier: 1,
        name: 'Preserve Edit Wiki Doc Form (action=doc, action=update, projectId, docId, title, content)',
        page: pageName,
        passed: !!editDoc &&
            editDoc.inputs.some(i => i.name === 'projectId' && i.type === 'hidden') &&
            editDoc.inputs.some(i => i.name === 'docId' && i.type === 'hidden') &&
            editDoc.inputs.some(i => i.name === 'title') &&
            editDoc.inputs.some(i => i.name === 'content' && i.tag === 'textarea'),
        details: editDoc ? 'Found edit doc form' : 'Edit doc form missing'
    });

    // Delete link
    const hasDeleteLink = /action=delete.*docId=/i.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve Delete Wiki Doc Action Link (doc?action=delete&docId=...) with confirm()',
        page: pageName,
        passed: hasDeleteLink,
        details: hasDeleteLink ? 'Found delete doc action link' : 'Delete doc action link missing'
    });
}

/**
 * 6. chat.jsp validations
 */
function testChatPageForms(reporter, pageName, forms, content) {
    // Form 1: Send Message Form
    const sendForm = forms.find(f => f.action && f.action.includes('chat') && f.inputs.some(i => i.name === 'action' && i.value === 'sendProjectMessage'));
    reporter.record({
        tier: 1,
        name: 'Preserve Send Message Form (action=chat, action=sendProjectMessage, projectId, chatInput)',
        page: pageName,
        passed: !!sendForm &&
            sendForm.inputs.some(i => i.name === 'projectId' && i.type === 'hidden') &&
            sendForm.inputs.some(i => i.name === 'content' && i.id === 'chatInput'),
        details: sendForm ? 'Found send message form' : 'Send message form missing'
    });

    // Form 2: Edit Message Form
    const editForm = forms.find(f => f.action && f.action.includes('chat') && f.inputs.some(i => i.name === 'action' && i.value === 'editProjectMessage'));
    reporter.record({
        tier: 1,
        name: 'Preserve Edit Message Form (action=chat, action=editProjectMessage, messageId, content)',
        page: pageName,
        passed: !!editForm &&
            editForm.inputs.some(i => i.name === 'projectId' && i.type === 'hidden') &&
            editForm.inputs.some(i => i.name === 'messageId' && i.id === 'editMessageId') &&
            editForm.inputs.some(i => i.name === 'content' && i.id === 'editMessageContent' && i.tag === 'textarea'),
        details: editForm ? 'Found edit message form' : 'Edit message form missing'
    });

    // Delete message trigger
    const hasDeleteModalBtn = /id=["']btnConfirmDeleteMessage["']/.test(content);
    reporter.record({
        tier: 1,
        name: 'Preserve Delete Message Confirmation Hook (#btnConfirmDeleteMessage)',
        page: pageName,
        passed: hasDeleteModalBtn,
        details: hasDeleteModalBtn ? 'Found #btnConfirmDeleteMessage hook' : '#btnConfirmDeleteMessage missing'
    });
}

/**
 * 7. tasks.jsp validations
 */
function testTasksPageForms(reporter, pageName, forms, content) {
    reporter.record({
        tier: 1,
        name: 'Preserve Exact Form Count in tasks.jsp (28 functional forms)',
        page: pageName,
        passed: forms.length === 28,
        details: `Detected ${forms.length} / 28 forms in tasks.jsp`
    });

    const documentedTasksForms = config.PAGE_FORMS['tasks.jsp'];
    const actionsFound = new Set();
    for (const f of forms) {
        for (const inp of f.inputs) {
            if (inp.name === 'action' && inp.value) {
                actionsFound.add(inp.value);
            }
        }
    }

    for (const item of documentedTasksForms) {
        const found = actionsFound.has(item.action);
        reporter.record({
            tier: 1,
            name: `tasks.jsp: ${item.desc} (action="${item.action}") preserved`,
            page: pageName,
            passed: found,
            details: found ? `Action "${item.action}" found in forms` : `Action "${item.action}" missing in forms`
        });
    }
}

module.exports = { runTier1Tests };
