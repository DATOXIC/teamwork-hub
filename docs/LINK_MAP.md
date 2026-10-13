# LINK_MAP — Servlet ↔ JSP nối với nhau ở đâu

> **File tự sinh** bởi `node docs/tools/gen_link_map.js` — đừng sửa tay; sửa code xong chạy lại để số dòng luôn đúng.
> Cách đọc: tìm **tên** (vd `todoTasks`) trong cột Java rồi sang cột JSP. Mô hình chung:

```
Trình duyệt ──GET/POST /url?action=x&param=y──▶ Servlet  (getParameter)
Servlet ──request.setAttribute("tên", dữ liệu)──▶ forward ──▶ JSP  (${tên})
Servlet ──session.setAttribute("toastSuccess", …)──▶ sendRedirect ──▶ Servlet khác ──▶ JSP
```

- **forward**: server chuyển nội bộ sang JSP, URL không đổi, `request` còn nguyên → dùng để *hiển thị*.
- **sendRedirect**: bảo trình duyệt gọi URL mới (PRG), `request` mất → dùng sau khi *ghi DB*; muốn nhắn lại thì dùng `session` (toast).
- **Ký hiệu trong code:** `▶` = "đầu này đẩy dữ liệu / gọi sang phía bên kia", `◀` = "đầu này nhận dữ liệu từ Servlet". Ctrl+F `▶` hoặc `◀` (hay Ctrl+F đúng tên attribute) để nhảy giữa Servlet ↔ JSP.
- Một số thao tác (kéo thả, subtask, chat) **không qua JSP**: JS `fetch` gọi Servlet và Servlet trả JSON.

## Mục lục

| URL | Servlet | Hiển thị bằng JSP |
|---|---|---|
| `/auth` | [AuthServlet](#authservlet) | `login.jsp`, `forgot-password.jsp` |
| `/chat` | [ChatServlet](#chatservlet) | `chat.jsp` |
| `/doc` | [DocServlet](#docservlet) | `docs.jsp` |
| `/error` | [ErrorServlet](#errorservlet) | `500.jsp` |
| `/meeting` | [MeetingServlet](#meetingservlet) | `meeting.jsp` |
| `/notification` | [NotificationServlet](#notificationservlet) | _không forward JSP (trả JSON/redirect)_ |
| `/profile` | [ProfileServlet](#profileservlet) | `profile.jsp` |
| `/analytics` | [ProjectAnalyticsServlet](#projectanalyticsservlet) | `project-analytics.jsp` |
| `/invite` | [ProjectInviteServlet](#projectinviteservlet) | _không forward JSP (trả JSON/redirect)_ |
| `/project` | [ProjectServlet](#projectservlet) | `projects.jsp`, `project_report.jsp` |
| `/task-file` | [TaskFileServlet](#taskfileservlet) | _không forward JSP (trả JSON/redirect)_ |
| `/task` | [TaskServlet](#taskservlet) | `tasks.jsp` |
| `/timeline` | [TimelineServlet](#timelineservlet) | `timeline.jsp` |
| `/whiteboard` | [WhiteboardServlet](#whiteboardservlet) | `whiteboard.jsp` |

## AuthServlet

URL `/auth` — mã nguồn `controllers/AuthServlet.java`

**Forward (hiển thị):** `/login.jsp` ← `AuthServlet.java:157` · `AuthServlet.java:208` · `AuthServlet.java:258` · `AuthServlet.java:273` · `AuthServlet.java:320` · `AuthServlet.java:690` · `/forgot-password.jsp` ← `AuthServlet.java:653`

**Redirect (sau khi ghi / điều hướng):** `/project?action=list…` ← `AuthServlet.java:115` · `AuthServlet.java:307` · `/auth?action=viewLogin…` ← `AuthServlet.java:419` · `AuthServlet.java:469` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeTab` | `AuthServlet.java:688` | `login.jsp:199` · `login.jsp:204` · `login.jsp:219` · `login.jsp:313` |
| request | `errorMessage` | `AuthServlet.java:136` · `AuthServlet.java:255` … (+2) | `login.jsp:182` · `login.jsp:184` |
| request | `forgotError` | `AuthServlet.java:487` · `AuthServlet.java:502` … (+11) | `forgot-password.jsp:100` · `forgot-password.jsp:102` |
| request | `forgotInfo` | `AuthServlet.java:547` | `forgot-password.jsp:94` · `forgot-password.jsp:96` |
| request | `forgotStep` | `AuthServlet.java:651` | `forgot-password.jsp:12` |
| request | `forgotUsername` | `AuthServlet.java:501` · `AuthServlet.java:516` … (+3) | `forgot-password.jsp:116` · `forgot-password.jsp:146` |
| request | `regEmail` | `AuthServlet.java:686` | `login.jsp:341` |
| request | `regError` | `AuthServlet.java:342` · `AuthServlet.java:360` … (+6) | `login.jsp:190` · `login.jsp:192` |
| request | `regFullName` | `AuthServlet.java:684` | `login.jsp:325` |
| request | `regUsername` | `AuthServlet.java:682` | `login.jsp:333` |
| request | `rememberChecked` | `AuthServlet.java:149` · `AuthServlet.java:249` | `login.jsp:251` |
| request | `showDemoAccounts` | `AuthServlet.java:109` | `login.jsp:274` · `login.jsp:276` |
| request | `successMessage` | `AuthServlet.java:125` | `login.jsp:174` · `login.jsp:176` |
| request | `username` | `AuthServlet.java:130` · `AuthServlet.java:147` … (+3) | `login.jsp:229` |
| session | `currentUser` | `AuthServlet.java:289` | `chat.jsp:15` · `chat.jsp:231` · `chat.jsp:257` · `chat.jsp:302` … (+59) |
| session | `successMessage` | `AuthServlet.java:417` · `AuthServlet.java:637` | `login.jsp:174` · `login.jsp:176` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `registeredUsername`(session)

**Action** (`?action=…` → nhánh `case`): `forgot`·99, `viewLogin`·104, `login`·188, `logout`·191, `register`·194, `forgotRequest`·197, `forgotVerify`·200, `forgotReset`·203

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `AuthServlet.java:91` · `AuthServlet.java:182` | _mọi link/form/fetch có `action=…`_ |
| `confirmPassword` | `AuthServlet.java:353` · `AuthServlet.java:616` | `forgot-password.jsp:174` · `login.jsp:364` |
| `email` | `AuthServlet.java:355` | `login.jsp:340` |
| `fullName` | `AuthServlet.java:354` | `login.jsp:324` · `profile.jsp:423` |
| `otp` | `AuthServlet.java:570` | `forgot-password.jsp:133` |
| `password` | `AuthServlet.java:243` · `AuthServlet.java:352` … (+1) | `forgot-password.jsp:161` · `login.jsp:238` · `login.jsp:350` |
| `remember` | `AuthServlet.java:244` | `login.jsp:250` |
| `username` | `AuthServlet.java:242` · `AuthServlet.java:351` … (+1) | `forgot-password.jsp:115` · `forgot-password.jsp:146` · `login.jsp:228` … (+1) |

## ChatServlet

URL `/chat` — mã nguồn `controllers/ChatServlet.java`

**Forward (hiển thị):** `/chat.jsp` ← `ChatServlet.java:239`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `ChatServlet.java:78` · `ChatServlet.java:147` · `/chat?action=view&projectId=…` ← `ChatServlet.java:181` · `ChatServlet.java:262` … (+7) · `/project?action=list…` ← `ChatServlet.java:199` · `/task?action=list&projectId=…` ← `ChatServlet.java:297` · `ChatServlet.java:323` … (+4)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `ChatServlet.java:235` | `navbar.jsp:42` |
| request | `chatSig` | `ChatServlet.java:227` | `chat.jsp:602` |
| request | `docList` | `ChatServlet.java:229` | `chat.jsp:12` · `chat.jsp:134` · `chat.jsp:149` · `chat.jsp:150` … (+3) |
| request | `messageList` | `ChatServlet.java:225` | `chat.jsp:14` · `chat.jsp:53` · `chat.jsp:228` · `chat.jsp:336` |
| request | `project` | `ChatServlet.java:223` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `taskList` | `ChatServlet.java:231` | `chat.jsp:13` · `chat.jsp:142` · `chat.jsp:170` · `chat.jsp:171` … (+1) |
| request | `userList` | `ChatServlet.java:233` | `chat.jsp:11` · `chat.jsp:47` · `chat.jsp:77` · `chat.jsp:83` … (+11) |
| session | `toastError` | `ChatServlet.java:335` · `ChatServlet.java:417` … (+4) | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ChatServlet.java:436` · `ChatServlet.java:517` | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

**Action** (`?action=…` → nhánh `case`): `view`·96, `poll`·102, `sendProjectMessage`·164, `editProjectMessage`·168, `sendTaskComment`·172, `delete`·176

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ChatServlet.java:89` · `ChatServlet.java:157` | _mọi link/form/fetch có `action=…`_ |
| `content` | `ChatServlet.java:251` · `ChatServlet.java:315` … (+1) | `chat.jsp:420` · `chat.jsp:551` · `docs.jsp:274` … (+5) |
| `source` | `ChatServlet.java:295` · `ChatServlet.java:459` | — |

## DocServlet

URL `/doc` — mã nguồn `controllers/DocServlet.java`

**Forward (hiển thị):** `/docs.jsp` ← `DocServlet.java:207`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `DocServlet.java:65` · `DocServlet.java:107` · `/doc?action=list&projectId=…` ← `DocServlet.java:137` · `DocServlet.java:221` … (+5) · `/project?action=list…` ← `DocServlet.java:157` · `/doc?action=view&projectId=…` ← `DocServlet.java:300` · `DocServlet.java:337` … (+2)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `DocServlet.java:203` | `navbar.jsp:42` |
| request | `docs` | `DocServlet.java:197` | `docs.jsp:11` · `docs.jsp:62` · `docs.jsp:79` · `docs.jsp:103` … (+3) |
| request | `project` | `DocServlet.java:195` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `relatedTasks` | `DocServlet.java:201` | `docs.jsp:13` · `docs.jsp:178` · `docs.jsp:182` · `docs.jsp:184` … (+1) |
| request | `selectedDoc` | `DocServlet.java:199` | `docs.jsp:12` · `docs.jsp:82` · `docs.jsp:87` · `docs.jsp:88` … (+11) |
| session | `toastError` | `DocServlet.java:232` · `DocServlet.java:247` … (+4) | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `DocServlet.java:243` · `DocServlet.java:296` … (+1) | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

**Action** (`?action=…` → nhánh `case`): `list`·83, `view`·84, `create`·124, `update`·128, `delete`·132

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `DocServlet.java:76` · `DocServlet.java:117` | _mọi link/form/fetch có `action=…`_ |
| `content` | `DocServlet.java:264` · `DocServlet.java:313` | `chat.jsp:420` · `chat.jsp:551` · `docs.jsp:274` … (+5) |
| `title` | `DocServlet.java:263` · `DocServlet.java:312` | `docs.jsp:264` · `docs.jsp:323` · `timeline.jsp:361` … (+7) |

## ErrorServlet

URL `/error` — mã nguồn `controllers/ErrorServlet.java`

**Forward (hiển thị):** `/500.jsp` ← `ErrorServlet.java:77`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `errorId` | `ErrorServlet.java:76` | `500.jsp:20` · `500.jsp:22` |

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `ajax` | `ErrorServlet.java:45` | `automation-modal.jspf:129` · `tasks-board.js:503` · `tasks-board.js:554` … (+6) |
| `isAjax` | `ErrorServlet.java:46` | `timeline.js:251` · `timeline.js:670` · `timeline.js:713` |

## MeetingServlet

URL `/meeting` — mã nguồn `controllers/MeetingServlet.java`

**Forward (hiển thị):** `/meeting.jsp` ← `MeetingServlet.java:63`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `MeetingServlet.java:43` · `/project?action=list…` ← `MeetingServlet.java:54`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `project` | `MeetingServlet.java:59` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `roomName` | `MeetingServlet.java:61` | `meeting.jsp:6` · `meeting.jsp:39` |

## NotificationServlet

URL `/notification` — mã nguồn `controllers/NotificationServlet.java`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `NotificationServlet.java:56` · `/project?action=list…` ← `NotificationServlet.java:62` · `NotificationServlet.java:80` … (+6) · `redirect…` ← `NotificationServlet.java:115`

**Action** (`?action=…` → nhánh `case`): `poll`·67, `read`·73, `readAll`·76, `delete`·83

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `NotificationServlet.java:60` | _mọi link/form/fetch có `action=…`_ |
| `id` | `NotificationServlet.java:106` · `NotificationServlet.java:138` | `navbar.jsp:133` · `index.jsp:126` · `inbox-drawer.jspf:42` |
| `redirect` | `NotificationServlet.java:115` | `navbar.jsp:134` · `inbox-drawer.jspf:43` |

## ProfileServlet

URL `/profile` — mã nguồn `controllers/ProfileServlet.java`

**Forward (hiển thị):** `/profile.jsp` ← `ProfileServlet.java:169`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `ProfileServlet.java:76` · `ProfileServlet.java:179` · `/project?action=list…` ← `ProfileServlet.java:88` · `ProfileServlet.java:111` · `/profile…` ← `ProfileServlet.java:187` · `ProfileServlet.java:200` · `/profile?userId=…` ← `ProfileServlet.java:207` · `ProfileServlet.java:220` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `availableProjectsToInvite` | `ProfileServlet.java:164` | `profile.jsp:174` · `profile.jsp:517` · `profile.jsp:543` |
| request | `completedSubTasks` | `ProfileServlet.java:160` | `profile.jsp:246` |
| request | `completionRate` | `ProfileServlet.java:162` | `profile.jsp:258` · `profile.jsp:261` · `profile.jsp:270` · `profile.jsp:272` |
| request | `isOwner` | `ProfileServlet.java:166` | `profile.jsp:93` · `profile.jsp:101` · `profile.jsp:163` · `profile.jsp:401` … (+3) |
| request | `leadTaskCount` | `ProfileServlet.java:156` | `profile.jsp:229` |
| request | `profileUser` | `ProfileServlet.java:152` | `profile.jsp:24` · `profile.jsp:69` · `profile.jsp:70` · `profile.jsp:78` … (+22) |
| request | `toastError` | `ProfileServlet.java:146` | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| request | `toastSuccess` | `ProfileServlet.java:140` | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |
| request | `totalSubTasks` | `ProfileServlet.java:158` | `profile.jsp:246` |
| request | `userProjects` | `ProfileServlet.java:154` | `profile.jsp:214` · `profile.jsp:288` · `profile.jsp:294` · `profile.jsp:333` … (+2) |
| session | `currentUser` | `ProfileServlet.java:247` | `chat.jsp:15` · `chat.jsp:231` · `chat.jsp:257` · `chat.jsp:302` … (+59) |
| session | `toastError` | `ProfileServlet.java:87` · `ProfileServlet.java:110` … (+2) | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ProfileServlet.java:249` | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProfileServlet.java:183` | _mọi link/form/fetch có `action=…`_ |
| `bio` | `ProfileServlet.java:213` | `profile.jsp:440` |
| `fullName` | `ProfileServlet.java:211` | `login.jsp:324` · `profile.jsp:423` |
| `githubUrl` | `ProfileServlet.java:215` | `profile.jsp:470` |
| `linkedinUrl` | `ProfileServlet.java:216` | `profile.jsp:477` |
| `role` | `ProfileServlet.java:212` | `profile.jsp:430` |
| `skills` | `ProfileServlet.java:214` | `profile.jsp:450` |

## ProjectAnalyticsServlet

URL `/analytics` — mã nguồn `controllers/ProjectAnalyticsServlet.java`

**Forward (hiển thị):** `/project-analytics.jsp` ← `ProjectAnalyticsServlet.java:172`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeSubNav` | `ProjectAnalyticsServlet.java:169` | `project_subnav.jsp:15` |
| request | `burndownData` | `ProjectAnalyticsServlet.java:152` | `project-analytics.jsp:110` · `project-analytics.jsp:117` · `project-analytics.jsp:123` · `project-analytics.jsp:128` … (+9) |
| request | `burndownJson` | `ProjectAnalyticsServlet.java:158` | `project-analytics.jsp:532` |
| request | `currentNav` | `ProjectAnalyticsServlet.java:170` | `project_subnav.jsp:50` · `project_subnav.jsp:58` · `project_subnav.jsp:66` · `project_subnav.jsp:74` … (+4) |
| request | `project` | `ProjectAnalyticsServlet.java:151` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `qualityJson` | `ProjectAnalyticsServlet.java:159` | `project-analytics.jsp:533` |
| request | `qualityMetrics` | `ProjectAnalyticsServlet.java:153` | `project-analytics.jsp:72` · `project-analytics.jsp:77` · `project-analytics.jsp:90` · `project-analytics.jsp:94` … (+17) |
| request | `workloadAnalytics` | `ProjectAnalyticsServlet.java:155` | `project-analytics.jsp:432` · `project-analytics.jsp:437` |
| request | `workloadsJson` | `ProjectAnalyticsServlet.java:167` | `project-analytics.jsp:534` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `workloadList`(request)

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProjectAnalyticsServlet.java:125` | _mọi link/form/fetch có `action=…`_ |

## ProjectInviteServlet

URL `/invite` — mã nguồn `controllers/ProjectInviteServlet.java`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `ProjectInviteServlet.java:39` · `ProjectInviteServlet.java:60` · `/project?action=list…` ← `ProjectInviteServlet.java:45` · `ProjectInviteServlet.java:50` … (+26) · `/task?action=list&projectId=…` ← `ProjectInviteServlet.java:116` · `ProjectInviteServlet.java:130` … (+14)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| session | `toastError` | `ProjectInviteServlet.java:108` · `ProjectInviteServlet.java:115` … (+27) | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ProjectInviteServlet.java:204` · `ProjectInviteServlet.java:294` … (+6) | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

**Action** (`?action=…` → nhánh `case`): `sendInvite`·71, `requestJoin`·74, `accept`·77, `reject`·80, `revoke`·83, `leave`·86, `kick`·89

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProjectInviteServlet.java:43` · `ProjectInviteServlet.java:64` | _mọi link/form/fetch có `action=…`_ |
| `projectCode` | `ProjectInviteServlet.java:215` | `projects.jsp:437` · `projects.jsp:493` · `create-project.jspf:40` … (+1) |
| `usernameOrEmail` | `ProjectInviteServlet.java:113` | `profile.jsp:531` · `invite-member.jspf:41` |

## ProjectServlet

URL `/project` — mã nguồn `controllers/ProjectServlet.java`

**Forward (hiển thị):** `/projects.jsp` ← `ProjectServlet.java:203` · `/project_report.jsp` ← `ProjectServlet.java:624`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=viewLogin…` ← `ProjectServlet.java:68` · `/auth?action=login…` ← `ProjectServlet.java:102` · `/project?action=list…` ← `ProjectServlet.java:119` · `ProjectServlet.java:268` … (+6) · `redirectUrl…` ← `ProjectServlet.java:309` · `ProjectServlet.java:346` · `/task?action=list&projectId=…` ← `ProjectServlet.java:362`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `ProjectServlet.java:201` | `navbar.jsp:42` |
| request | `blockerCount` | `ProjectServlet.java:608` | `project_report.jsp:151` · `project_report.jsp:156` · `project_report.jsp:164` |
| request | `criticalBlockers` | `ProjectServlet.java:606` | `project_report.jsp:170` |
| request | `docCount` | `ProjectServlet.java:571` | `project_report.jsp:787` |
| request | `docs` | `ProjectServlet.java:569` | `docs.jsp:11` · `docs.jsp:62` · `docs.jsp:79` · `docs.jsp:103` … (+3) |
| request | `doneCount` | `ProjectServlet.java:578` | `project_report.jsp:257` · `project_report.jsp:339` · `project_report.jsp:360` · `project_report.jsp:590` … (+1) |
| request | `errorMessage` | `ProjectServlet.java:220` · `ProjectServlet.java:228` … (+1) | `login.jsp:182` · `login.jsp:184` |
| request | `generatedAt` | `ProjectServlet.java:620` | `project_report.jsp:139` |
| request | `healthBadgeClass` | `ProjectServlet.java:614` | `project_report.jsp:102` |
| request | `healthDescription` | `ProjectServlet.java:617` | `project_report.jsp:132` |
| request | `healthLabel` | `ProjectServlet.java:612` | `project_report.jsp:116` |
| request | `highPriorityCount` | `ProjectServlet.java:597` | `project_report.jsp:379` · `project_report.jsp:382` · `project_report.jsp:384` · `project_report.jsp:386` |
| request | `inProgressCount` | `ProjectServlet.java:580` | `project_report.jsp:275` · `project_report.jsp:354` · `project_report.jsp:579` · `timeline.jsp:69` |
| request | `kpiDoneTasks` | `ProjectServlet.java:161` | `projects.jsp:108` |
| request | `kpiPmCount` | `ProjectServlet.java:157` | `projects.jsp:78` · `projects.jsp:228` · `projects.jsp:231` |
| request | `kpiTotalTasks` | `ProjectServlet.java:159` | `projects.jsp:108` |
| request | `lowPriorityCount` | `ProjectServlet.java:601` | `project_report.jsp:413` · `project_report.jsp:416` · `project_report.jsp:418` · `project_report.jsp:420` |
| request | `mediumPriorityCount` | `ProjectServlet.java:599` | `project_report.jsp:396` · `project_report.jsp:399` · `project_report.jsp:401` · `project_report.jsp:403` |
| request | `memberCount` | `ProjectServlet.java:565` | `project_report.jsp:242` · `project_report.jsp:441` · `invite-member.jspf:49` · `invite-member.jspf:53` … (+4) |
| request | `memberCountMap` | `ProjectServlet.java:174` | `projects.jsp:312` |
| request | `members` | `ProjectServlet.java:563` | `project_report.jsp:871` · `timeline.jsp:152` · `timeline.jsp:397` |
| request | `memberStats` | `ProjectServlet.java:604` | `project_report.jsp:462` · `project_report.jsp:530` |
| request | `messageCount` | `ProjectServlet.java:573` | `project_report.jsp:832` · `project_report.jsp:837` · `project_report.jsp:842` |
| request | `myProjects` | `ProjectServlet.java:141` | `projects.jsp:36` · `projects.jsp:76` · `projects.jsp:225` · `projects.jsp:231` … (+3) |
| request | `overdueCount` | `ProjectServlet.java:592` | `project_report.jsp:303` · `project_report.jsp:311` · `project_report.jsp:314` · `project_report.jsp:592` … (+5) |
| request | `pendingInvites` | `ProjectServlet.java:181` | `projects.jsp:92` · `projects.jsp:123` · `projects.jsp:135` · `projects.jsp:142` … (+1) |
| request | `planningCount` | `ProjectServlet.java:584` | `project_report.jsp:293` · `project_report.jsp:295` · `project_report.jsp:353` · `project_report.jsp:582` |
| request | `progressPercentage` | `ProjectServlet.java:594` | `project_report.jsp:260` · `project_report.jsp:336` · `project_report.jsp:344` · `project_report.jsp:346` |
| request | `project` | `ProjectServlet.java:561` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `projectHealth` | `ProjectServlet.java:610` | `project_report.jsp:105` · `project_report.jsp:108` |
| request | `rejectedCount` | `ProjectServlet.java:590` | `project_report.jsp:359` · `project_report.jsp:584` · `project_report.jsp:586` |
| request | `reviseCount` | `ProjectServlet.java:588` | `project_report.jsp:357` · `project_report.jsp:584` · `project_report.jsp:586` |
| request | `submittedCount` | `ProjectServlet.java:582` | `project_report.jsp:293` · `project_report.jsp:295` · `project_report.jsp:355` · `project_report.jsp:582` … (+2) |
| request | `tasks` | `ProjectServlet.java:567` | `project_report.jsp:563` · `project_report.jsp:623` · `project_report.jsp:758` |
| request | `toastError` | `ProjectServlet.java:195` | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| request | `toastSuccess` | `ProjectServlet.java:189` | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |
| request | `todoCount` | `ProjectServlet.java:586` | `project_report.jsp:352` · `project_report.jsp:576` · `timeline.jsp:71` |
| request | `totalTasks` | `ProjectServlet.java:576` | `project_report.jsp:239` · `project_report.jsp:339` · `project_report.jsp:382` · `project_report.jsp:386` … (+8) |
| session | `toastError` | `ProjectServlet.java:267` · `ProjectServlet.java:307` … (+2) | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ProjectServlet.java:272` · `ProjectServlet.java:332` … (+1) | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `healthIcon`(request)

**Action** (`?action=…` → nhánh `case`): `list`·78, `detail`·81, `report`·84, `create`·112, `update`·115

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProjectServlet.java:72` · `ProjectServlet.java:106` | _mọi link/form/fetch có `action=…`_ |
| `description` | `ProjectServlet.java:213` · `ProjectServlet.java:289` | `projects.jsp:500` · `timeline.jsp:405` · `task-pane.jspf:62` … (+3) |
| `name` | `ProjectServlet.java:212` · `ProjectServlet.java:288` | `projects.jsp:482` · `create-project.jspf:29` · `project-settings.jspf:26` … (+1) |
| `projectCode` | `ProjectServlet.java:214` · `ProjectServlet.java:215` | `projects.jsp:437` · `projects.jsp:493` · `create-project.jspf:40` … (+1) |
| `projectId` | `ProjectServlet.java:285` · `ProjectServlet.java:357` … (+1) | `chat.jsp:414` · `chat.jsp:544` · `docs.jsp:17` … (+114) |
| `projectType` | `ProjectServlet.java:241` · `ProjectServlet.java:290` | `projects.jsp:514` · `projects.jsp:526` · `create-project.jspf:61` … (+4) |
| `redirectUrl` | `ProjectServlet.java:309` · `ProjectServlet.java:346` | — |

## TaskFileServlet

URL `/task-file` — mã nguồn `controllers/TaskFileServlet.java`

## TaskServlet

URL `/task` — mã nguồn `controllers/TaskServlet.java + controllers/task/*.java`

**Forward (hiển thị):** `/tasks.jsp` ← `TaskBoardHandler.java:348`

**Redirect (sau khi ghi / điều hướng):** `/project?action=list…` ← `SubTaskHandler.java:83` · `SubTaskHandler.java:96` … (+22) · `/task?action=list&projectId=…` ← `SubTaskHandler.java:134` · `SubTaskHandler.java:153` … (+49) · `/auth?action=login…` ← `SubTaskHandler.java:450` · `TaskCrudHandler.java:419` · `/auth?action=viewLogin…` ← `TaskServlet.java:141` · `TaskServlet.java:196` … (+1) · `/timeline?projectId=…` ← `TaskServlet.java:167`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `TaskBoardHandler.java:344` | `navbar.jsp:42` |
| request | `activityLogs` | `TaskBoardHandler.java:336` | `activity-view.jspf:33` · `activity-view.jspf:40` · `activity-view.jspf:52` |
| request | `allProjectTasks` | `TaskBoardHandler.java:300` | `tasks.jsp:139` · `main-header.jspf:73` |
| request | `currentView` | `TaskBoardHandler.java:338` | `tasks.jsp:91` · `activity-view.jspf:7` · `main-header.jspf:85` · `sidebar.jspf:121` … (+5) |
| request | `docList` | `TaskBoardHandler.java:306` | `chat.jsp:12` · `chat.jsp:134` · `chat.jsp:149` · `chat.jsp:150` … (+3) |
| request | `doneTasks` | `TaskBoardHandler.java:298` | `tasks.jsp:14` · `board-view.jspf:7` · `board-view.jspf:347` · `board-view.jspf:355` … (+4) |
| request | `inProgressTasks` | `TaskBoardHandler.java:296` | `risk_panel.jsp:8` · `risk_panel.jsp:27` · `tasks.jsp:13` · `board-view.jspf:7` … (+6) |
| request | `memberCount` | `TaskBoardHandler.java:326` | `project_report.jsp:242` · `project_report.jsp:441` · `invite-member.jspf:49` · `invite-member.jspf:53` … (+4) |
| request | `project` | `TaskBoardHandler.java:292` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `projectAutomations` | `TaskBoardHandler.java:239` | `automation-modal.jspf:42` |
| request | `projectInviteList` | `TaskBoardHandler.java:324` | `project-team.jspf:214` · `project-team.jspf:219` · `project-team.jspf:226` |
| request | `projectLabels` | `TaskBoardHandler.java:302` | `add-task.jspf:64` |
| request | `projectMemberList` | `TaskBoardHandler.java:322` | `project-team.jspf:38` · `project-team.jspf:137` · `project-team.jspf:144` |
| request | `submittedCount` | `TaskBoardHandler.java:320` | `project_report.jsp:293` · `project_report.jsp:295` · `project_report.jsp:355` · `project_report.jsp:582` … (+2) |
| request | `subtaskMode` | `TaskBoardHandler.java:342` | `tasks.jsp:122` · `list-view.jspf:105` · `list-view.jspf:106` · `list-view.jspf:168` … (+8) |
| request | `taskCommentsMap` | `TaskBoardHandler.java:310` | `task-pane.jspf:364` · `task-pane.jspf:368` · `task-pane.jspf:384` |
| request | `taskDocsMap` | `TaskBoardHandler.java:308` | `board-view.jspf:134` · `board-view.jspf:135` · `board-view.jspf:137` · `board-view.jspf:295` … (+9) |
| request | `taskHealthMap` | `TaskBoardHandler.java:196` | `health_badge.jsp:4` · `risk_panel.jsp:6` · `risk_panel.jsp:8` · `risk_panel.jsp:19` … (+7) |
| request | `taskProgressMap` | `TaskBoardHandler.java:316` | `tasks.jsp:15` · `board-view.jspf:56` · `board-view.jspf:104` · `board-view.jspf:105` … (+16) |
| request | `taskSubmissionsMap` | `TaskBoardHandler.java:314` | `task-gates.jspf:157` |
| request | `taskSubTasksMap` | `TaskBoardHandler.java:312` | `board-view.jspf:55` · `board-view.jspf:103` · `board-view.jspf:125` · `board-view.jspf:126` … (+32) |
| request | `taskView` | `TaskBoardHandler.java:340` | `board-view.jspf:5` · `list-view.jspf:5` · `main-header.jspf:90` · `main-header.jspf:93` |
| request | `toastError` | `TaskBoardHandler.java:223` | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| request | `toastSuccess` | `TaskBoardHandler.java:216` | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |
| request | `todoTasks` | `TaskBoardHandler.java:294` | `risk_panel.jsp:6` · `risk_panel.jsp:18` · `tasks.jsp:12` · `board-view.jspf:7` … (+6) |
| request | `unreadNotifCount` | `TaskBoardHandler.java:330` | `navbar.jsp:91` · `navbar.jsp:97` · `navbar.jsp:99` · `navbar.jsp:113` … (+5) |
| request | `userList` | `TaskBoardHandler.java:304` | `chat.jsp:11` · `chat.jsp:47` · `chat.jsp:77` · `chat.jsp:83` … (+11) |
| request | `userNotifications` | `TaskBoardHandler.java:332` | `tasks.jsp:18` · `inbox-drawer.jspf:16` · `inbox-drawer.jspf:30` · `inbox-drawer.jspf:38` |
| request | `userProjects` | `TaskBoardHandler.java:328` | `profile.jsp:214` · `profile.jsp:288` · `profile.jsp:294` · `profile.jsp:333` … (+2) |
| request | `userWorkloadList` | `TaskBoardHandler.java:318` | `main-header.jspf:155` · `main-header.jspf:170` · `main-header.jspf:171` · `main-header.jspf:172` … (+8) |
| session | `toastError` | `SubTaskHandler.java:54` · `SubTaskHandler.java:132` … (+52) | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `SubTaskHandler.java:209` · `SubTaskHandler.java:431` … (+15) | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `projectChatMessages`(request)

**Action** (`?action=…` → nhánh `case`): `list`·156, `exportCsv`·162, `timeline`·166, `add`·212, `createLabel`·216, `updateStatus`·220, `quickAddParentTask`·224, `quickAddSubTask`·228, `addSubTask`·232, `toggleSubTask`·236, `submitSubTask`·240, `approveSubTask`·244, `reviseSubTask`·248, `rejectSubTask`·252, `submitParentTask`·256, `submitPlanningRequest`·260, `pmApprovePlanning`·264, `pmRejectPlanning`·268, `pmApproveTask`·272, `pmReviseTask`·276, `pmRejectTask`·280, `deleteSubTask`·284, `editTask`·288, `editSubTask`·292, `delete`·296, `toggleAutomation`·301

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `TaskServlet.java:150` · `TaskServlet.java:180` | _mọi link/form/fetch có `action=…`_ |
| `ajax` | `TaskJson.java:49` · `TaskServlet.java:321` | `automation-modal.jspf:129` · `tasks-board.js:503` · `tasks-board.js:554` … (+6) |
| `assigneeId` | `SubTaskHandler.java:100` · `SubTaskHandler.java:457` … (+4) | `timeline.jsp:395` · `subtask-panes.jspf:49` · `task-pane.jspf:97` … (+9) |
| `codeUrl` | `TaskWorkflowHandler.java:94` | `task-gates.jspf:365` |
| `color` | `TaskCrudHandler.java:528` | `tasks.js:593` |
| `completed` | `SubTaskHandler.java:233` | `tasks-board.js:502` · `tasks-board.js:1687` |
| `deliverableNote` | `TaskWorkflowHandler.java:97` | — |
| `demoUrl` | `TaskWorkflowHandler.java:93` | `task-gates.jspf:362` |
| `description` | `TaskCrudHandler.java:63` · `TaskCrudHandler.java:426` | `projects.jsp:500` · `timeline.jsp:405` · `task-pane.jspf:62` … (+3) |
| `dueDate` | `SubTaskHandler.java:74` · `SubTaskHandler.java:458` … (+4) | `timeline.jsp:377` · `subtask-panes.jspf:57` · `task-pane.jspf:89` … (+6) |
| `enabled` | `TaskServlet.java:318` | `automation-modal.jspf:128` |
| `feedback` | `TaskWorkflowHandler.java:307` · `TaskWorkflowHandler.java:362` … (+3) | `task-gates.jspf:122` · `task-gates.jspf:143` · `task-gates.jspf:412` … (+2) |
| `feedbackNote` | `SubTaskHandler.java:684` · `SubTaskHandler.java:738` | `subtask-panes.jspf:227` · `subtask-panes.jspf:245` |
| `hasRequiresGateControl` | `TaskCrudHandler.java:164` · `TaskCrudHandler.java:498` | `task-pane.jspf:116` |
| `labels` | `TaskCrudHandler.java:146` | `add-task.jspf:71` |
| `name` | `TaskCrudHandler.java:527` | `projects.jsp:482` · `create-project.jspf:29` · `project-settings.jspf:26` … (+1) |
| `newStatus` | `TaskCrudHandler.java:204` | `task-gates.jspf:480` · `task-gates.jspf:494` · `task-gates.jspf:505` … (+3) |
| `planningNote` | `TaskWorkflowHandler.java:241` | `task-gates.jspf:35` |
| `priority` | `TaskCrudHandler.java:64` · `TaskCrudHandler.java:427` … (+1) | `timeline.jsp:385` · `task-pane.jspf:67` · `task-pane.jspf:73` … (+5) |
| `projectId` | `SubTaskHandler.java:71` · `SubTaskHandler.java:231` … (+21) | `chat.jsp:414` · `chat.jsp:544` · `docs.jsp:17` … (+114) |
| `qualityRating` | `TaskWorkflowHandler.java:416` | `task-gates.jspf:403` |
| `recipeId` | `TaskServlet.java:317` | `automation-modal.jspf:127` |
| `requiresGate` | `TaskCrudHandler.java:159` · `TaskCrudHandler.java:500` … (+1) | `task-pane.jspf:119` · `add-task.jspf:207` |
| `status` | `TaskCrudHandler.java:206` · `TaskCrudHandler.java:565` | `tasks-board.js:649` |
| `subTaskId` | `SubTaskHandler.java:232` · `SubTaskHandler.java:402` … (+5) | `subtask-panes.jspf:41` · `subtask-panes.jspf:171` · `subtask-panes.jspf:201` … (+5) |
| `submissionNote` | `SubTaskHandler.java:531` | `subtask-panes.jspf:176` |
| `summary` | `TaskWorkflowHandler.java:92` | `task-gates.jspf:358` |
| `taskId` | `SubTaskHandler.java:72` · `SubTaskHandler.java:789` … (+10) | `project_report.jsp:729` · `task-gates.jspf:34` · `task-gates.jspf:119` … (+22) |
| `taskView` | `TaskBoardHandler.java:250` | — |
| `testResult` | `TaskWorkflowHandler.java:95` | — |
| `testingGuide` | `TaskWorkflowHandler.java:96` | — |
| `title` | `SubTaskHandler.java:73` · `SubTaskHandler.java:456` … (+4) | `docs.jsp:264` · `docs.jsp:323` · `timeline.jsp:361` … (+7) |
| `view` | `TaskBoardHandler.java:244` | — |

## TimelineServlet

URL `/timeline` — mã nguồn `controllers/TimelineServlet.java`

**Forward (hiển thị):** `/timeline.jsp` ← `TimelineServlet.java:223`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=viewLogin…` ← `TimelineServlet.java:63` · `/project?action=list…` ← `TimelineServlet.java:82` · `TimelineServlet.java:698` · `/timeline?projectId=…` ← `TimelineServlet.java:274` · `TimelineServlet.java:650` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `TimelineServlet.java:219` | `navbar.jsp:42` |
| request | `allTasks` | `TimelineServlet.java:180` | `timeline.jsp:241` · `timeline.jsp:451` · `timeline.jsp:460` |
| request | `dependenciesJson` | `TimelineServlet.java:211` | `timeline.jsp:517` |
| request | `doneCount` | `TimelineServlet.java:194` | `project_report.jsp:257` · `project_report.jsp:339` · `project_report.jsp:360` · `project_report.jsp:590` … (+1) |
| request | `inProgressCount` | `TimelineServlet.java:196` | `project_report.jsp:275` · `project_report.jsp:354` · `project_report.jsp:579` · `timeline.jsp:69` |
| request | `isOwner` | `TimelineServlet.java:217` | `profile.jsp:93` · `profile.jsp:101` · `profile.jsp:163` · `profile.jsp:401` … (+3) |
| request | `members` | `TimelineServlet.java:178` | `project_report.jsp:871` · `timeline.jsp:152` · `timeline.jsp:397` |
| request | `overallProgress` | `TimelineServlet.java:202` | `timeline.jsp:101` · `timeline.jsp:106` |
| request | `overdueCount` | `TimelineServlet.java:200` | `project_report.jsp:303` · `project_report.jsp:311` · `project_report.jsp:314` · `project_report.jsp:592` … (+5) |
| request | `project` | `TimelineServlet.java:176` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `scheduledCount` | `TimelineServlet.java:190` | `timeline.jsp:55` |
| request | `taskProgressMap` | `TimelineServlet.java:183` | `tasks.jsp:15` · `board-view.jspf:56` · `board-view.jspf:104` · `board-view.jspf:105` … (+16) |
| request | `tasksJson` | `TimelineServlet.java:209` | `timeline.jsp:515` |
| request | `todayDate` | `TimelineServlet.java:205` | `timeline.jsp:248` · `timeline.jsp:371` · `timeline.jsp:378` · `timeline.jsp:510` |
| request | `todoCount` | `TimelineServlet.java:198` | `project_report.jsp:352` · `project_report.jsp:576` · `timeline.jsp:71` |
| request | `totalTasks` | `TimelineServlet.java:188` | `project_report.jsp:239` · `project_report.jsp:339` · `project_report.jsp:382` · `project_report.jsp:386` … (+8) |
| request | `unscheduledCount` | `TimelineServlet.java:192` | `timeline.jsp:55` · `timeline.jsp:228` · `timeline.jsp:233` |
| session | `toastError` | `TimelineServlet.java:693` | `docs.jsp:14` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `TimelineServlet.java:648` · `TimelineServlet.java:692` | `docs.jsp:14` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:125` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `completedSubtaskMap`(request), `dependencies`(request), `horizonEndDate`(request), `horizonStartDate`(request), `subtaskCountMap`(request), `subtasksByTask`(request)

**Action** (`?action=…` → nhánh `case`): `updateDueDate`·258, `updateDates`·261, `addDependency`·264, `deleteDependency`·267, `quickAddTask`·270

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `TimelineServlet.java:73` · `TimelineServlet.java:240` | _mọi link/form/fetch có `action=…`_ |
| `dependencyType` | `TimelineServlet.java:423` | — |
| `description` | `TimelineServlet.java:579` | `projects.jsp:500` · `timeline.jsp:405` · `task-pane.jspf:62` … (+3) |
| `dueDate` | `TimelineServlet.java:349` · `TimelineServlet.java:576` | `timeline.jsp:377` · `subtask-panes.jspf:57` · `task-pane.jspf:89` … (+6) |
| `isAjax` | `TimelineServlet.java:706` | `timeline.js:251` · `timeline.js:670` · `timeline.js:713` |
| `newDueDate` | `TimelineServlet.java:286` | — |
| `priority` | `TimelineServlet.java:577` | `timeline.jsp:385` · `task-pane.jspf:67` · `task-pane.jspf:73` … (+5) |
| `projectId` | `TimelineServlet.java:789` | `chat.jsp:414` · `chat.jsp:544` · `docs.jsp:17` … (+114) |
| `startDate` | `TimelineServlet.java:348` · `TimelineServlet.java:607` | `timeline.jsp:370` · `timeline.js:249` |
| `title` | `TimelineServlet.java:575` | `docs.jsp:264` · `docs.jsp:323` · `timeline.jsp:361` … (+7) |

## WhiteboardServlet

URL `/whiteboard` — mã nguồn `controllers/WhiteboardServlet.java`

**Forward (hiển thị):** `/whiteboard.jsp` ← `WhiteboardServlet.java:68`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `WhiteboardServlet.java:36` · `/project?action=list…` ← `WhiteboardServlet.java:47`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `project` | `WhiteboardServlet.java:64` | `chat.jsp:10` · `chat.jsp:257` · `chat.jsp:260` · `chat.jsp:302` … (+190) |
| request | `whiteboardJson` | `WhiteboardServlet.java:66` | `whiteboard.jsp:5` · `whiteboard.jsp:33` |

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `format` | `WhiteboardServlet.java:53` | `whiteboard.js:6` · `whiteboard.js:81` |

