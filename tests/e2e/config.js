/**
 * config.js - Configuration and baseline contracts for E2E Test Suite.
 * Defines all 7 JSPs, expected forms, required semantic classes,
 * JS hooks, and compilation parameters.
 */

const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '../..');
const WEBAPP_DIR = path.join(ROOT_DIR, 'src/main/webapp');
const STYLES_DIR = path.join(WEBAPP_DIR, 'styles');
const INCLUDES_DIR = path.join(WEBAPP_DIR, 'includes');

const TARGET_PAGES = [
    'login.jsp',
    'profile.jsp',
    'projects.jsp',
    'project_report.jsp',
    'docs.jsp',
    'chat.jsp',
    'tasks.jsp'
];

const COMPONENT_STYLESHEET = 'styles/page-components.css';

module.exports = {
    ROOT_DIR,
    WEBAPP_DIR,
    STYLES_DIR,
    INCLUDES_DIR,
    TARGET_PAGES,
    COMPONENT_STYLESHEET,

    // Documented Form Contracts per Page
    PAGE_FORMS: {
        'login.jsp': [
            {
                name: 'Login Form',
                actionPattern: /\/auth\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'login', type: 'hidden' },
                    { name: 'username', id: 'login-username', type: 'text', required: true },
                    { name: 'password', id: 'login-password', type: 'password', required: true },
                    { name: 'remember', type: 'checkbox' }
                ]
            },
            {
                name: 'Registration Form',
                actionPattern: /\/auth\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'register', type: 'hidden' },
                    { name: 'fullName', id: 'reg-fullname', type: 'text', required: true },
                    { name: 'username', id: 'reg-username', type: 'text', required: true },
                    { name: 'email', id: 'reg-email', type: 'email', required: true },
                    { name: 'password', id: 'reg-pass', type: 'password', required: true },
                    { name: 'confirmPassword', id: 'reg-confirmpass', type: 'password', required: true }
                ]
            }
        ],
        'profile.jsp': [
            {
                name: 'Edit Profile Modal Form',
                actionPattern: /\/profile\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'update', type: 'hidden' },
                    { name: 'userId', type: 'hidden' },
                    { name: 'fullName', id: 'inputFullName', required: true },
                    { name: 'role', id: 'inputRole' },
                    { name: 'bio', id: 'inputBio', tag: 'textarea' },
                    { name: 'skills', id: 'inputSkills' },
                    { name: 'githubUrl', id: 'inputGithub' },
                    { name: 'linkedinUrl', id: 'inputLinkedin' }
                ]
            },
            {
                name: 'Quick Invite Modal Form',
                actionPattern: /\/invite\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'sendInvite', type: 'hidden' },
                    { name: 'usernameOrEmail', type: 'hidden' },
                    { name: 'projectId', id: 'selectProject', tag: 'select', required: true }
                ]
            }
        ],
        'projects.jsp': [
            {
                name: 'Accept Project Invite Form',
                actionPattern: /\/invite\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'accept', type: 'hidden' },
                    { name: 'inviteId', type: 'hidden' }
                ]
            },
            {
                name: 'Reject Project Invite Form',
                actionPattern: /\/invite\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'reject', type: 'hidden' },
                    { name: 'inviteId', type: 'hidden' }
                ]
            },
            {
                name: 'Join Project By Code Modal Form',
                actionPattern: /\/invite\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'requestJoin', type: 'hidden' },
                    { name: 'projectCode', id: 'inputProjectCode', required: true }
                ]
            },
            {
                name: 'Create Project Modal Form',
                actionPattern: /\/project\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'create', type: 'hidden' },
                    { name: 'name', id: 'proj-name', required: true },
                    { name: 'projectCode', id: 'proj-code' },
                    { name: 'description', id: 'proj-desc', tag: 'textarea' },
                    { name: 'projectType', type: 'radio' }
                ]
            }
        ],
        'docs.jsp': [
            {
                name: 'Create Wiki Doc Modal Form',
                actionPattern: /doc\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'create', type: 'hidden' },
                    { name: 'projectId', type: 'hidden' },
                    { name: 'title', required: true },
                    { name: 'content', tag: 'textarea', required: true }
                ]
            },
            {
                name: 'Edit Wiki Doc Modal Form',
                actionPattern: /doc\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'update', type: 'hidden' },
                    { name: 'projectId', type: 'hidden' },
                    { name: 'docId', type: 'hidden' },
                    { name: 'title', required: true },
                    { name: 'content', tag: 'textarea', required: true }
                ]
            }
        ],
        'chat.jsp': [
            {
                name: 'Send Project Message Form',
                actionPattern: /chat\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'sendProjectMessage', type: 'hidden' },
                    { name: 'projectId', type: 'hidden' },
                    { name: 'content', id: 'chatInput', required: true }
                ]
            },
            {
                name: 'Edit Project Message Form',
                actionPattern: /chat\b/,
                method: 'POST',
                requiredInputs: [
                    { name: 'action', value: 'editProjectMessage', type: 'hidden' },
                    { name: 'projectId', type: 'hidden' },
                    { name: 'messageId', id: 'editMessageId', type: 'hidden' },
                    { name: 'content', id: 'editMessageContent', tag: 'textarea', required: true }
                ]
            }
        ],
        'tasks.jsp': [
            { action: 'editTask', desc: 'Side-Peek Task Drawer Edit Form' },
            { action: 'addSubTask', desc: 'Inline Quick Add Subtask Form' },
            { action: 'updateStatus', desc: 'Inline Status Update Form' },
            { action: 'pmApprovePlanning', desc: 'Quality Gate 1 PM Approve Planning Form' },
            { action: 'pmRejectPlanning', desc: 'Quality Gate 1 PM Reject Planning Form' },
            { action: 'submitParentTask', desc: 'Quality Gate 2 Member Submit Deliverable Form' },
            { action: 'pmApproveTask', desc: 'Quality Gate 2 PM Approve Task & Rating Form' },
            { action: 'pmReviseTask', desc: 'Quality Gate 2 PM Request Revision Form' },
            { action: 'pmRejectTask', desc: 'Quality Gate 2 PM Reject Task Form' },
            { action: 'sendTaskComment', desc: 'Task Discussion Comment Form' },
            { action: 'editSubTask', desc: 'Edit Subtask Title & Assignee Form' },
            { action: 'submitSubTask', desc: 'Submit Subtask For Review Form' },
            { action: 'approveSubTask', desc: 'PM Approve Subtask Review Form' },
            { action: 'reviseSubTask', desc: 'PM Revise Subtask Form' },
            { action: 'rejectSubTask', desc: 'PM Reject Subtask Form' },
            { action: 'deleteSubTask', desc: 'Delete Subtask Form' },
            { action: 'add', desc: 'Modal Add Task Form' },
            { action: 'kick', desc: 'Remove Project Member Form' },
            { action: 'leave', desc: 'Leave Project Form' },
            { action: 'revoke', desc: 'Revoke Pending Invite Form' },
            { action: 'sendInvite', desc: 'Send Project Invite Form' },
            { action: 'update', desc: 'Update Project Settings Form' },
            { action: 'create', desc: 'Create Project Modal Form' },
            { action: 'requestJoin', desc: 'Join Project By Code Modal Form' }
        ]
    },

    // Semantic Classes Catalog
    SEMANTIC_CLASSES: {
        common: ['progress-init-zero', 'cursor-pointer'],
        login: ['login-body', 'login-navbar', 'login-brand-logo-icon', 'login-campus-caption', 'login-exit-link'],
        profile: ['profile-identity-card', 'profile-avatar-banner', 'profile-bio-text', 'profile-skill-badge', 'profile-skill-chip-interactive'],
        projects: ['workspace-header-bar', 'btn-join-code-trigger', 'invite-alert-pill', 'btn-invite-accept', 'btn-invite-reject', 'workspace-toolbar-card', 'badge-role-owner', 'badge-role-member', 'project-desc-line-clamp'],
        report: ['report-btn-back', 'report-code-badge', 'report-title-truncate', 'report-banner-content', 'report-card-surface', 'report-progress-pill'],
        tasksAndWorkspace: ['wiki-doc-list-scroll', 'chat-shelf-scroll', 'kanban-column', 'kanban-dropzone', 'kanban-card', 'task-drawer-panel', 'clickup-status-popover']
    },

    // JavaScript Hooks & DOM Selectors
    JS_HOOKS: {
        'login.jsp': {
            functions: ['switchTab', 'fillLogin', 'togglePassword', 'validateRegisterForm'],
            ids: ['login-username', 'login-password', 'reg-pass', 'reg-confirmpass', 'languageCurrent']
        },
        'profile.jsp': {
            functions: ['addSkill'],
            ids: ['inputSkills', 'editProfileModal', 'quickInviteModal'],
            dataAttrs: ['data-progress']
        },
        'projects.jsp': {
            functions: ['copyProjectCode', 'filterProjects', 'searchProjectsLive', 'clearProjectSearch', 'applyProjectFilters'],
            ids: ['projectSearchInput', 'myProjectsGrid', 'joinByCodeModal', 'createProjectModal'],
            dataAttrs: ['data-role', 'data-name', 'data-code']
        },
        'project_report.jsp': {
            functions: ['filterTasks', 'handleKpiCardClick', 'handleTaskSearch', 'applyTaskFilters', 'initReportTheme', 'toggleReportTheme'],
            ids: ['taskSearchInput', 'themeToggleBtn', 'kpiCardAll', 'kpiCardDone'],
            dataAttrs: ['data-status', 'data-overdue', 'data-search']
        },
        'docs.jsp': {
            functions: ['filterDocList'],
            ids: ['docSearchInput', 'docListContainer', 'createDocModal', 'editDocModal'],
            dataAttrs: ['data-doc-title']
        },
        'chat.jsp': {
            ids: ['chatMessageContainer', 'chatInput', 'btnSend', 'shelfDocs', 'shelfTasks', 'emojiDrawer', 'btnToggleEmoji'],
            dataAttrs: ['data-raw-content']
        },
        'tasks.jsp': {
            functions: ['openClickUpTask', 'switchTaskSubView', 'openStatusDropdown', 'toggleClickUpSidebar', 'toggleSubtasks'],
            ids: ['clickupSidebar', 'clickupTaskDrawer', 'clickupStatusPopover', 'addTaskModal'],
            dataAttrs: ['data-task-id', 'data-requires-gate', 'data-status']
        }
    }
};
