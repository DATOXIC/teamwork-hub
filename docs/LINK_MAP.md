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
| `/meeting` | [MeetingServlet](#meetingservlet) | `meeting.jsp` |
| `/notification` | [NotificationServlet](#notificationservlet) | _không forward JSP (trả JSON/redirect)_ |
| `/profile` | [ProfileServlet](#profileservlet) | `profile.jsp` |
| `/invite` | [ProjectInviteServlet](#projectinviteservlet) | _không forward JSP (trả JSON/redirect)_ |
| `/project` | [ProjectServlet](#projectservlet) | `projects.jsp`, `project_report.jsp` |
| `/task` | [TaskServlet](#taskservlet) | `tasks.jsp` |
| `/timeline` | [TimelineServlet](#timelineservlet) | `timeline.jsp` |
| `/whiteboard` | [WhiteboardServlet](#whiteboardservlet) | `whiteboard.jsp` |

## AuthServlet

URL `/auth` — mã nguồn `controllers/AuthServlet.java`

**Forward (hiển thị):** `/login.jsp` ← `AuthServlet.java:142` · `AuthServlet.java:190` · `AuthServlet.java:238` · `AuthServlet.java:253` · `AuthServlet.java:293` · `AuthServlet.java:646` · `/forgot-password.jsp` ← `AuthServlet.java:609`

**Redirect (sau khi ghi / điều hướng):** `/project?action=list…` ← `AuthServlet.java:106` · `AuthServlet.java:280` · `/auth?action=viewLogin…` ← `AuthServlet.java:392` · `AuthServlet.java:442` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeTab` | `AuthServlet.java:644` | `login.jsp:192` · `login.jsp:197` · `login.jsp:212` · `login.jsp:274` |
| request | `errorMessage` | `AuthServlet.java:235` · `AuthServlet.java:249` … (+1) | `login.jsp:175` · `login.jsp:177` |
| request | `forgotError` | `AuthServlet.java:460` · `AuthServlet.java:475` … (+10) | `forgot-password.jsp:96` · `forgot-password.jsp:98` |
| request | `forgotInfo` | `AuthServlet.java:503` | `forgot-password.jsp:90` · `forgot-password.jsp:92` |
| request | `forgotStep` | `AuthServlet.java:607` | `forgot-password.jsp:12` |
| request | `forgotUsername` | `AuthServlet.java:474` · `AuthServlet.java:488` … (+2) | `forgot-password.jsp:112` · `forgot-password.jsp:142` |
| request | `regEmail` | `AuthServlet.java:642` | `login.jsp:302` |
| request | `regError` | `AuthServlet.java:315` · `AuthServlet.java:333` … (+6) | `login.jsp:183` · `login.jsp:185` |
| request | `regFullName` | `AuthServlet.java:640` | `login.jsp:286` |
| request | `regUsername` | `AuthServlet.java:638` | `login.jsp:294` |
| request | `rememberChecked` | `AuthServlet.java:134` | `login.jsp:244` |
| request | `successMessage` | `AuthServlet.java:116` | `login.jsp:167` · `login.jsp:169` |
| request | `username` | `AuthServlet.java:121` · `AuthServlet.java:132` … (+3) | `login.jsp:222` |
| session | `currentUser` | `AuthServlet.java:268` | `chat.jsp:14` · `chat.jsp:230` · `chat.jsp:256` · `chat.jsp:301` … (+60) |
| session | `successMessage` | `AuthServlet.java:390` · `AuthServlet.java:593` | `login.jsp:167` · `login.jsp:169` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `registeredUsername`(session)

**Action** (`?action=…` → nhánh `case`): `logout`·91, `forgot`·95, `viewLogin`·100, `login`·173, `register`·176, `forgotRequest`·179, `forgotVerify`·182, `forgotReset`·185

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `AuthServlet.java:85` · `AuthServlet.java:167` | _mọi link/form/fetch có `action=…`_ |
| `confirmPassword` | `AuthServlet.java:326` · `AuthServlet.java:572` | `forgot-password.jsp:170` · `login.jsp:325` |
| `email` | `AuthServlet.java:328` | `login.jsp:301` |
| `fullName` | `AuthServlet.java:327` | `login.jsp:285` · `profile.jsp:300` |
| `otp` | `AuthServlet.java:526` | `forgot-password.jsp:129` |
| `password` | `AuthServlet.java:225` · `AuthServlet.java:325` … (+1) | `forgot-password.jsp:157` · `login.jsp:231` · `login.jsp:311` |
| `remember` | `AuthServlet.java:226` | `login.jsp:243` |
| `username` | `AuthServlet.java:224` · `AuthServlet.java:324` … (+1) | `forgot-password.jsp:111` · `forgot-password.jsp:142` · `login.jsp:221` … (+1) |

## ChatServlet

URL `/chat` — mã nguồn `controllers/ChatServlet.java`

**Forward (hiển thị):** `/chat.jsp` ← `ChatServlet.java:237`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `ChatServlet.java:82` · `ChatServlet.java:136` · `/project?action=list…` ← `ChatServlet.java:89` · `ChatServlet.java:99` … (+3) · `/chat?action=view&projectId=…` ← `ChatServlet.java:180` · `ChatServlet.java:253` … (+7) · `/task?action=list&projectId=…` ← `ChatServlet.java:277` · `ChatServlet.java:298` … (+4)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `ChatServlet.java:233` | `navbar.jsp:41` |
| request | `docList` | `ChatServlet.java:227` | `chat.jsp:11` · `chat.jsp:133` · `chat.jsp:148` · `chat.jsp:149` … (+3) |
| request | `messageList` | `ChatServlet.java:225` | `chat.jsp:13` · `chat.jsp:52` · `chat.jsp:227` · `chat.jsp:335` |
| request | `project` | `ChatServlet.java:223` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `taskList` | `ChatServlet.java:229` | `chat.jsp:12` · `chat.jsp:141` · `chat.jsp:169` · `chat.jsp:170` … (+1) |
| request | `userList` | `ChatServlet.java:231` | `chat.jsp:10` · `chat.jsp:46` · `chat.jsp:76` · `chat.jsp:82` … (+10) |
| session | `toastError` | `ChatServlet.java:97` · `ChatServlet.java:149` … (+6) | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ChatServlet.java:385` · `ChatServlet.java:444` | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

**Action** (`?action=…` → nhánh `case`): `view`·111, `delete`·115, `sendProjectMessage`·163, `editProjectMessage`·167, `sendTaskComment`·171, `delete`·175

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ChatServlet.java:104` · `ChatServlet.java:156` | _mọi link/form/fetch có `action=…`_ |
| `content` | `ChatServlet.java:249` · `ChatServlet.java:295` … (+1) | `chat.jsp:410` · `chat.jsp:541` · `docs.jsp:273` … (+2) |
| `messageId` | `ChatServlet.java:352` · `ChatServlet.java:410` | `chat.jsp:535` · `chat.js:210` · `chat.js:218` |
| `projectId` | `ChatServlet.java:87` · `ChatServlet.java:140` | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |
| `source` | `ChatServlet.java:275` · `ChatServlet.java:395` | — |
| `taskId` | `ChatServlet.java:294` | `tasks.jsp:2119` · `tasks.jsp:2301` · `tasks.jsp:2450` … (+15) |

## DocServlet

URL `/doc` — mã nguồn `controllers/DocServlet.java`

**Forward (hiển thị):** `/docs.jsp` ← `DocServlet.java:247`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `DocServlet.java:82` · `DocServlet.java:137` · `/project?action=list…` ← `DocServlet.java:89` · `DocServlet.java:99` … (+3) · `/doc?action=list&projectId=…` ← `DocServlet.java:177` · `DocServlet.java:261` … (+5) · `/doc?action=view&projectId=…` ← `DocServlet.java:341` · `DocServlet.java:378` … (+2)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `DocServlet.java:243` | `navbar.jsp:41` |
| request | `docs` | `DocServlet.java:237` | `docs.jsp:10` · `docs.jsp:61` · `docs.jsp:78` · `docs.jsp:102` … (+3) |
| request | `project` | `DocServlet.java:235` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `relatedTasks` | `DocServlet.java:241` | `docs.jsp:12` · `docs.jsp:177` · `docs.jsp:181` · `docs.jsp:183` … (+1) |
| request | `selectedDoc` | `DocServlet.java:239` | `docs.jsp:11` · `docs.jsp:81` · `docs.jsp:86` · `docs.jsp:87` … (+11) |
| session | `toastError` | `DocServlet.java:97` · `DocServlet.java:150` … (+6) | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `DocServlet.java:284` · `DocServlet.java:337` … (+1) | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

**Action** (`?action=…` → nhánh `case`): `list`·111, `view`·112, `delete`·116, `create`·164, `update`·168, `delete`·172

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `DocServlet.java:104` · `DocServlet.java:157` | _mọi link/form/fetch có `action=…`_ |
| `content` | `DocServlet.java:305` · `DocServlet.java:354` | `chat.jsp:410` · `chat.jsp:541` · `docs.jsp:273` … (+2) |
| `docId` | `DocServlet.java:205` · `DocServlet.java:258` … (+1) | `docs.jsp:17` · `docs.jsp:20` · `docs.jsp:80` … (+4) |
| `projectId` | `DocServlet.java:87` · `DocServlet.java:141` | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |
| `title` | `DocServlet.java:304` · `DocServlet.java:353` | `docs.jsp:263` · `docs.jsp:322` · `tasks.jsp:2123` … (+6) |

## MeetingServlet

URL `/meeting` — mã nguồn `controllers/MeetingServlet.java`

**Forward (hiển thị):** `/meeting.jsp` ← `MeetingServlet.java:74`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `MeetingServlet.java:53` · `/project?action=list…` ← `MeetingServlet.java:59` · `MeetingServlet.java:65`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `project` | `MeetingServlet.java:70` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `roomName` | `MeetingServlet.java:72` | `meeting.jsp:5` · `meeting.jsp:38` |

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `projectId` | `MeetingServlet.java:57` | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |

## NotificationServlet

URL `/notification` — mã nguồn `controllers/NotificationServlet.java`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `NotificationServlet.java:44` · `/project?action=list…` ← `NotificationServlet.java:50` · `NotificationServlet.java:65` … (+4)

**Action** (`?action=…` → nhánh `case`): `read`·55, `readAll`·58, `delete`·61

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `NotificationServlet.java:48` | _mọi link/form/fetch có `action=…`_ |
| `id` | `NotificationServlet.java:86` · `NotificationServlet.java:136` | `navbar.jsp:127` · `index.jsp:126` · `tasks.jsp:2009` |
| `redirect` | `NotificationServlet.java:95` | `navbar.jsp:127` · `tasks.jsp:2009` |

## ProfileServlet

URL `/profile` — mã nguồn `controllers/ProfileServlet.java`

**Forward (hiển thị):** `/profile.jsp` ← `ProfileServlet.java:181`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `ProfileServlet.java:91` · `ProfileServlet.java:191` · `/project?action=list…` ← `ProfileServlet.java:103` · `/profile…` ← `ProfileServlet.java:199` · `ProfileServlet.java:212` · `/profile?userId=…` ← `ProfileServlet.java:219` · `ProfileServlet.java:232` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `availableProjectsToInvite` | `ProfileServlet.java:176` | `profile.jsp:129` · `profile.jsp:378` · `profile.jsp:404` |
| request | `completedSubTasks` | `ProfileServlet.java:172` | `profile.jsp:194` |
| request | `completionRate` | `ProfileServlet.java:174` | `profile.jsp:209` · `profile.jsp:211` |
| request | `isOwner` | `ProfileServlet.java:178` | `profile.jsp:69` · `profile.jsp:119` · `profile.jsp:280` · `profile.jsp:378` … (+1) |
| request | `leadTaskCount` | `ProfileServlet.java:168` | `profile.jsp:178` |
| request | `profileUser` | `ProfileServlet.java:164` | `profile.jsp:23` · `profile.jsp:60` · `profile.jsp:62` · `profile.jsp:66` … (+19) |
| request | `toastError` | `ProfileServlet.java:158` | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| request | `toastSuccess` | `ProfileServlet.java:152` | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |
| request | `totalSubTasks` | `ProfileServlet.java:170` | `profile.jsp:194` |
| request | `userProjects` | `ProfileServlet.java:166` | `profile.jsp:163` · `profile.jsp:225` · `profile.jsp:229` · `profile.jsp:261` … (+2) |
| session | `currentUser` | `ProfileServlet.java:259` | `chat.jsp:14` · `chat.jsp:230` · `chat.jsp:256` · `chat.jsp:301` … (+60) |
| session | `toastError` | `ProfileServlet.java:102` · `ProfileServlet.java:218` … (+1) | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ProfileServlet.java:261` | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProfileServlet.java:195` | _mọi link/form/fetch có `action=…`_ |
| `bio` | `ProfileServlet.java:225` | `profile.jsp:313` |
| `fullName` | `ProfileServlet.java:223` | `login.jsp:285` · `profile.jsp:300` |
| `githubUrl` | `ProfileServlet.java:227` | `profile.jsp:335` |
| `linkedinUrl` | `ProfileServlet.java:228` | `profile.jsp:339` |
| `role` | `ProfileServlet.java:224` | `profile.jsp:304` |
| `skills` | `ProfileServlet.java:226` | `profile.jsp:321` |
| `userId` | `ProfileServlet.java:96` · `ProfileServlet.java:210` | `profile.jsp:294` · `tasks.jsp:3774` |

## ProjectInviteServlet

URL `/invite` — mã nguồn `controllers/ProjectInviteServlet.java`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `ProjectInviteServlet.java:56` · `ProjectInviteServlet.java:89` · `/project?action=list…` ← `ProjectInviteServlet.java:62` · `ProjectInviteServlet.java:77` … (+26) · `/task?action=list&projectId=…` ← `ProjectInviteServlet.java:145` · `ProjectInviteServlet.java:159` … (+13)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| session | `toastError` | `ProjectInviteServlet.java:137` · `ProjectInviteServlet.java:144` … (+26) | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ProjectInviteServlet.java:229` · `ProjectInviteServlet.java:315` … (+6) | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

**Action** (`?action=…` → nhánh `case`): `accept`·67, `reject`·70, `revoke`·73, `sendInvite`·100, `requestJoin`·103, `accept`·106, `reject`·109, `revoke`·112, `leave`·115, `kick`·118

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProjectInviteServlet.java:60` · `ProjectInviteServlet.java:93` | _mọi link/form/fetch có `action=…`_ |
| `inviteId` | `ProjectInviteServlet.java:326` · `ProjectInviteServlet.java:416` … (+1) | `projects.jsp:190` · `projects.jsp:201` · `tasks.jsp:3840` |
| `projectCode` | `ProjectInviteServlet.java:240` | `projects.jsp:475` · `projects.jsp:531` · `tasks.jsp:4063` … (+1) |
| `projectId` | `ProjectInviteServlet.java:134` · `ProjectInviteServlet.java:488` … (+1) | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |
| `userId` | `ProjectInviteServlet.java:543` | `profile.jsp:294` · `tasks.jsp:3774` |
| `usernameOrEmail` | `ProjectInviteServlet.java:142` | `profile.jsp:392` · `tasks.jsp:3915` |

## ProjectServlet

URL `/project` — mã nguồn `controllers/ProjectServlet.java`

**Forward (hiển thị):** `/projects.jsp` ← `ProjectServlet.java:219` · `/project_report.jsp` ← `ProjectServlet.java:655`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=viewLogin…` ← `ProjectServlet.java:67` · `/auth?action=login…` ← `ProjectServlet.java:101` · `/project?action=list…` ← `ProjectServlet.java:118` · `ProjectServlet.java:295` … (+6) · `/task?action=list&projectId=…` ← `ProjectServlet.java:334` · `ProjectServlet.java:374` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `ProjectServlet.java:217` | `navbar.jsp:41` |
| request | `blockerCount` | `ProjectServlet.java:639` | `project_report.jsp:144` · `project_report.jsp:149` · `project_report.jsp:157` |
| request | `criticalBlockers` | `ProjectServlet.java:637` | `project_report.jsp:163` |
| request | `docCount` | `ProjectServlet.java:602` | `project_report.jsp:768` |
| request | `docs` | `ProjectServlet.java:600` | `docs.jsp:10` · `docs.jsp:61` · `docs.jsp:78` · `docs.jsp:102` … (+3) |
| request | `doneCount` | `ProjectServlet.java:609` | `project_report.jsp:250` · `project_report.jsp:332` · `project_report.jsp:353` · `project_report.jsp:583` … (+1) |
| request | `errorMessage` | `ProjectServlet.java:236` · `ProjectServlet.java:244` … (+1) | `login.jsp:175` · `login.jsp:177` |
| request | `generatedAt` | `ProjectServlet.java:651` | `project_report.jsp:132` |
| request | `healthBadgeClass` | `ProjectServlet.java:645` | `project_report.jsp:95` |
| request | `healthDescription` | `ProjectServlet.java:648` | `project_report.jsp:125` |
| request | `healthLabel` | `ProjectServlet.java:643` | `project_report.jsp:109` |
| request | `highPriorityCount` | `ProjectServlet.java:628` | `project_report.jsp:372` · `project_report.jsp:375` · `project_report.jsp:377` · `project_report.jsp:379` |
| request | `inProgressCount` | `ProjectServlet.java:611` | `project_report.jsp:268` · `project_report.jsp:347` · `project_report.jsp:572` · `timeline.jsp:67` |
| request | `kpiDoneTasks` | `ProjectServlet.java:182` | `projects.jsp:107` |
| request | `kpiPmCount` | `ProjectServlet.java:178` | `projects.jsp:77` · `projects.jsp:227` · `projects.jsp:230` |
| request | `kpiTotalTasks` | `ProjectServlet.java:180` | `projects.jsp:107` |
| request | `lowPriorityCount` | `ProjectServlet.java:632` | `project_report.jsp:406` · `project_report.jsp:409` · `project_report.jsp:411` · `project_report.jsp:413` |
| request | `mediumPriorityCount` | `ProjectServlet.java:630` | `project_report.jsp:389` · `project_report.jsp:392` · `project_report.jsp:394` · `project_report.jsp:396` |
| request | `memberCount` | `ProjectServlet.java:596` | `project_report.jsp:235` · `project_report.jsp:434` · `tasks.jsp:3610` · `tasks.jsp:3923` … (+4) |
| request | `memberCountMap` | `ProjectServlet.java:190` | `projects.jsp:311` · `projects.jsp:413` |
| request | `members` | `ProjectServlet.java:594` | `project_report.jsp:852` · `timeline.jsp:150` · `timeline.jsp:363` |
| request | `memberStats` | `ProjectServlet.java:635` | `project_report.jsp:455` · `project_report.jsp:523` |
| request | `messageCount` | `ProjectServlet.java:604` | `project_report.jsp:813` · `project_report.jsp:818` · `project_report.jsp:823` |
| request | `myProjects` | `ProjectServlet.java:159` | `projects.jsp:35` · `projects.jsp:75` · `projects.jsp:224` · `projects.jsp:230` … (+3) |
| request | `otherProjects` | `ProjectServlet.java:161` | `projects.jsp:91` · `projects.jsp:389` · `projects.jsp:397` · `projects.jsp:431` |
| request | `overdueCount` | `ProjectServlet.java:623` | `project_report.jsp:296` · `project_report.jsp:304` · `project_report.jsp:307` · `project_report.jsp:585` … (+5) |
| request | `pendingInvites` | `ProjectServlet.java:197` | `projects.jsp:122` · `projects.jsp:134` · `projects.jsp:141` · `projects.jsp:149` |
| request | `planningCount` | `ProjectServlet.java:615` | `project_report.jsp:286` · `project_report.jsp:288` · `project_report.jsp:346` · `project_report.jsp:575` |
| request | `progressPercentage` | `ProjectServlet.java:625` | `project_report.jsp:253` · `project_report.jsp:329` · `project_report.jsp:337` · `project_report.jsp:339` |
| request | `project` | `ProjectServlet.java:592` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `projectHealth` | `ProjectServlet.java:641` | `project_report.jsp:98` · `project_report.jsp:101` |
| request | `rejectedCount` | `ProjectServlet.java:621` | `project_report.jsp:352` · `project_report.jsp:577` · `project_report.jsp:579` |
| request | `reviseCount` | `ProjectServlet.java:619` | `project_report.jsp:350` · `project_report.jsp:577` · `project_report.jsp:579` |
| request | `submittedCount` | `ProjectServlet.java:613` | `project_report.jsp:286` · `project_report.jsp:288` · `project_report.jsp:348` · `project_report.jsp:575` … (+2) |
| request | `tasks` | `ProjectServlet.java:598` | `project_report.jsp:556` · `project_report.jsp:616` · `project_report.jsp:739` |
| request | `toastError` | `ProjectServlet.java:211` | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| request | `toastSuccess` | `ProjectServlet.java:205` | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |
| request | `todoCount` | `ProjectServlet.java:617` | `project_report.jsp:345` · `project_report.jsp:569` · `timeline.jsp:69` |
| request | `totalTasks` | `ProjectServlet.java:607` | `project_report.jsp:232` · `project_report.jsp:332` · `project_report.jsp:375` · `project_report.jsp:379` … (+8) |
| session | `toastError` | `ProjectServlet.java:328` · `ProjectServlet.java:366` … (+2) | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `ProjectServlet.java:293` · `ProjectServlet.java:357` … (+1) | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `healthIcon`(request), `projects`(request)

**Action** (`?action=…` → nhánh `case`): `list`·77, `detail`·80, `report`·83, `create`·111, `update`·114

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `ProjectServlet.java:71` · `ProjectServlet.java:105` | _mọi link/form/fetch có `action=…`_ |
| `description` | `ProjectServlet.java:229` · `ProjectServlet.java:310` | `projects.jsp:538` · `tasks.jsp:2127` · `tasks.jsp:3397` … (+3) |
| `name` | `ProjectServlet.java:228` · `ProjectServlet.java:309` | `projects.jsp:520` · `tasks.jsp:3968` · `tasks.jsp:4052` … (+1) |
| `projectCode` | `ProjectServlet.java:230` · `ProjectServlet.java:231` | `projects.jsp:475` · `projects.jsp:531` · `tasks.jsp:4063` … (+1) |
| `projectId` | `ProjectServlet.java:306` · `ProjectServlet.java:385` … (+1) | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |
| `projectType` | `ProjectServlet.java:257` · `ProjectServlet.java:311` | `projects.jsp:552` · `projects.jsp:564` · `tasks.jsp:3980` … (+4) |
| `redirectUrl` | `ProjectServlet.java:330` · `ProjectServlet.java:370` | — |

## TaskServlet

URL `/task` — mã nguồn `controllers/TaskServlet.java + controllers/task/*.java`

**Forward (hiển thị):** `/tasks.jsp` ← `TaskBoardHandler.java:346`

**Redirect (sau khi ghi / điều hướng):** `/project?action=list…` ← `SubTaskHandler.java:78` · `SubTaskHandler.java:87` … (+23) · `/task?action=list&projectId=…` ← `SubTaskHandler.java:121` · `SubTaskHandler.java:136` … (+40) · `/auth?action=login…` ← `SubTaskHandler.java:351` · `TaskCrudHandler.java:392` · `/auth?action=viewLogin…` ← `TaskServlet.java:170` · `TaskServlet.java:230` · `/timeline?projectId=…` ← `TaskServlet.java:201`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `TaskBoardHandler.java:342` | `navbar.jsp:41` |
| request | `activityLogs` | `TaskBoardHandler.java:334` | `tasks.jsp:1914` · `tasks.jsp:1921` · `tasks.jsp:1933` |
| request | `allProjectTasks` | `TaskBoardHandler.java:298` | `tasks.jsp:392` · `tasks.jsp:2068` |
| request | `currentView` | `TaskBoardHandler.java:336` | `tasks.jsp:177` · `tasks.jsp:213` · `tasks.jsp:222` · `tasks.jsp:241` … (+5) |
| request | `docList` | `TaskBoardHandler.java:304` | `chat.jsp:11` · `chat.jsp:133` · `chat.jsp:148` · `chat.jsp:149` … (+3) |
| request | `doneTasks` | `TaskBoardHandler.java:296` | `tasks.jsp:13` · `tasks.jsp:394` · `tasks.jsp:865` · `tasks.jsp:869` … (+4) |
| request | `inProgressTasks` | `TaskBoardHandler.java:294` | `risk_panel.jsp:8` · `risk_panel.jsp:27` · `tasks.jsp:12` · `tasks.jsp:396` … (+6) |
| request | `inviteCandidates` | `TaskBoardHandler.java:324` | `tasks.jsp:3898` · `tasks.jsp:3903` |
| request | `memberCount` | `TaskBoardHandler.java:322` | `project_report.jsp:235` · `project_report.jsp:434` · `tasks.jsp:3610` · `tasks.jsp:3923` … (+4) |
| request | `project` | `TaskBoardHandler.java:290` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `projectInviteList` | `TaskBoardHandler.java:320` | `tasks.jsp:3805` · `tasks.jsp:3810` · `tasks.jsp:3817` |
| request | `projectLabels` | `TaskBoardHandler.java:300` | `tasks.jsp:3420` |
| request | `projectMemberList` | `TaskBoardHandler.java:318` | `tasks.jsp:3629` · `tasks.jsp:3728` · `tasks.jsp:3735` |
| request | `submittedCount` | `TaskBoardHandler.java:316` | `project_report.jsp:286` · `project_report.jsp:288` · `project_report.jsp:348` · `project_report.jsp:575` … (+2) |
| request | `subtaskMode` | `TaskBoardHandler.java:340` | `tasks.jsp:435` · `tasks.jsp:436` · `tasks.jsp:647` · `tasks.jsp:648` … (+8) |
| request | `taskCommentsMap` | `TaskBoardHandler.java:308` | `tasks.jsp:2818` · `tasks.jsp:2822` · `tasks.jsp:2838` |
| request | `taskDocsMap` | `TaskBoardHandler.java:306` | `tasks.jsp:1112` · `tasks.jsp:1113` · `tasks.jsp:1115` · `tasks.jsp:1269` … (+9) |
| request | `taskHealthMap` | `TaskBoardHandler.java:190` | `health_badge.jsp:4` · `risk_panel.jsp:6` · `risk_panel.jsp:8` · `risk_panel.jsp:19` … (+7) |
| request | `taskProgressMap` | `TaskBoardHandler.java:312` | `tasks.jsp:14` · `tasks.jsp:778` · `tasks.jsp:1034` · `tasks.jsp:1082` … (+16) |
| request | `taskSubTasksMap` | `TaskBoardHandler.java:310` | `tasks.jsp:646` · `tasks.jsp:657` · `tasks.jsp:658` · `tasks.jsp:659` … (+32) |
| request | `taskView` | `TaskBoardHandler.java:338` | `tasks.jsp:409` · `tasks.jsp:412` · `tasks.jsp:549` · `tasks.jsp:985` |
| request | `toastError` | `TaskBoardHandler.java:227` | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| request | `toastSuccess` | `TaskBoardHandler.java:220` | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |
| request | `todoTasks` | `TaskBoardHandler.java:292` | `risk_panel.jsp:6` · `risk_panel.jsp:18` · `tasks.jsp:11` · `tasks.jsp:398` … (+6) |
| request | `unreadNotifCount` | `TaskBoardHandler.java:328` | `navbar.jsp:94` · `navbar.jsp:96` · `navbar.jsp:110` · `navbar.jsp:111` … (+4) |
| request | `userList` | `TaskBoardHandler.java:302` | `chat.jsp:10` · `chat.jsp:46` · `chat.jsp:76` · `chat.jsp:82` … (+10) |
| request | `userNotifications` | `TaskBoardHandler.java:330` | `tasks.jsp:17` · `tasks.jsp:1986` · `tasks.jsp:2000` · `tasks.jsp:2008` |
| request | `userProjects` | `TaskBoardHandler.java:326` | `profile.jsp:163` · `profile.jsp:225` · `profile.jsp:229` · `profile.jsp:261` … (+2) |
| request | `userWorkloadList` | `TaskBoardHandler.java:314` | `tasks.jsp:468` · `tasks.jsp:483` · `tasks.jsp:484` · `tasks.jsp:485` … (+8) |
| session | `toastError` | `SubTaskHandler.java:118` · `SubTaskHandler.java:133` … (+39) | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `SubTaskHandler.java:159` · `SubTaskHandler.java:332` … (+14) | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `projectChatMessages`(request), `cached_system_users`(session)

**Action** (`?action=…` → nhánh `case`): `list`·188, `delete`·192, `exportCsv`·196, `timeline`·200, `add`·248, `createLabel`·252, `updateStatus`·256, `quickAddParentTask`·260, `quickAddSubTask`·264, `addSubTask`·268, `toggleSubTask`·272, `submitSubTask`·276, `approveSubTask`·280, `reviseSubTask`·284, `rejectSubTask`·288, `submitParentTask`·292, `submitPlanningRequest`·296, `pmApprovePlanning`·300, `pmRejectPlanning`·304, `pmApproveTask`·308, `pmReviseTask`·312, `pmRejectTask`·316, `deleteSubTask`·320, `editTask`·324, `editSubTask`·328

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `TaskServlet.java:182` · `TaskServlet.java:214` | _mọi link/form/fetch có `action=…`_ |
| `ajax` | `TaskJson.java:91` | `tasks-board.js:441` · `tasks-board.js:492` · `tasks-board.js:592` … (+1) |
| `assigneeId` | `SubTaskHandler.java:91` · `SubTaskHandler.java:358` … (+4) | `tasks.jsp:2162` · `tasks.jsp:2169` · `tasks.jsp:2176` … (+8) |
| `codeUrl` | `TaskWorkflowHandler.java:77` | `tasks.jsp:2659` |
| `color` | `TaskCrudHandler.java:503` | `tasks.js:526` |
| `completed` | `SubTaskHandler.java:178` | `tasks-board.js:440` |
| `deliverableFile` | `TaskWorkflowHandler.java:81` | `tasks.jsp:2663` |
| `deliverableNote` | `TaskWorkflowHandler.java:80` | — |
| `demoUrl` | `TaskWorkflowHandler.java:76` | `tasks.jsp:2656` |
| `description` | `TaskCrudHandler.java:85` · `TaskCrudHandler.java:399` | `projects.jsp:538` · `tasks.jsp:2127` · `tasks.jsp:3397` … (+3) |
| `dueDate` | `SubTaskHandler.java:74` · `SubTaskHandler.java:359` … (+4) | `tasks.jsp:2154` · `tasks.jsp:2319` · `tasks.jsp:2929` … (+4) |
| `feedback` | `TaskWorkflowHandler.java:244` · `TaskWorkflowHandler.java:297` … (+3) | `tasks.jsp:2532` · `tasks.jsp:2553` · `tasks.jsp:2696` … (+2) |
| `feedbackNote` | `SubTaskHandler.java:565` · `SubTaskHandler.java:617` | `tasks.jsp:3099` · `tasks.jsp:3117` |
| `hasRequiresGateControl` | `TaskCrudHandler.java:177` · `TaskCrudHandler.java:473` | `tasks.jsp:2181` |
| `labels` | `TaskCrudHandler.java:159` | `tasks.jsp:3427` |
| `name` | `TaskCrudHandler.java:502` | `projects.jsp:520` · `tasks.jsp:3968` · `tasks.jsp:4052` … (+1) |
| `newStatus` | `TaskCrudHandler.java:216` | `tasks.jsp:2451` · `tasks.jsp:2764` · `tasks.jsp:2778` … (+3) |
| `planningNote` | `TaskWorkflowHandler.java:181` | — |
| `priority` | `TaskCrudHandler.java:86` · `TaskCrudHandler.java:400` … (+1) | `tasks.jsp:2132` · `tasks.jsp:2138` · `tasks.jsp:2144` … (+5) |
| `projectId` | `SubTaskHandler.java:71` · `SubTaskHandler.java:176` … (+21) | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |
| `qualityRating` | `TaskWorkflowHandler.java:349` | `tasks.jsp:2687` |
| `requiresGate` | `TaskCrudHandler.java:172` · `TaskCrudHandler.java:475` … (+1) | `tasks.jsp:2184` · `tasks.jsp:3563` |
| `status` | `TaskCrudHandler.java:218` · `TaskCrudHandler.java:540` | `tasks-board.js:587` |
| `subTaskId` | `SubTaskHandler.java:177` · `SubTaskHandler.java:303` … (+5) | `tasks.jsp:2913` · `tasks.jsp:3043` · `tasks.jsp:3073` … (+4) |
| `submissionNote` | `SubTaskHandler.java:432` | `tasks.jsp:3048` |
| `summary` | `TaskWorkflowHandler.java:75` | `tasks.jsp:2652` |
| `taskId` | `SubTaskHandler.java:72` · `SubTaskHandler.java:666` … (+10) | `tasks.jsp:2119` · `tasks.jsp:2301` · `tasks.jsp:2450` … (+15) |
| `taskView` | `TaskBoardHandler.java:248` | — |
| `testResult` | `TaskWorkflowHandler.java:78` | — |
| `testingGuide` | `TaskWorkflowHandler.java:79` | — |
| `title` | `SubTaskHandler.java:73` · `SubTaskHandler.java:357` … (+4) | `docs.jsp:263` · `docs.jsp:322` · `tasks.jsp:2123` … (+6) |
| `view` | `TaskBoardHandler.java:242` | — |

## TimelineServlet

URL `/timeline` — mã nguồn `controllers/TimelineServlet.java`

**Forward (hiển thị):** `/timeline.jsp` ← `TimelineServlet.java:219`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=viewLogin…` ← `TimelineServlet.java:59` · `/project?action=list…` ← `TimelineServlet.java:66` · `TimelineServlet.java:76` … (+2) · `/timeline?projectId=…` ← `TimelineServlet.java:255` · `TimelineServlet.java:348` … (+1)

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `activeNav` | `TimelineServlet.java:215` | `navbar.jsp:41` |
| request | `allTasks` | `TimelineServlet.java:180` | `timeline.jsp:232` |
| request | `doneCount` | `TimelineServlet.java:194` | `project_report.jsp:250` · `project_report.jsp:332` · `project_report.jsp:353` · `project_report.jsp:583` … (+1) |
| request | `inProgressCount` | `TimelineServlet.java:196` | `project_report.jsp:268` · `project_report.jsp:347` · `project_report.jsp:572` · `timeline.jsp:67` |
| request | `isOwner` | `TimelineServlet.java:213` | `profile.jsp:69` · `profile.jsp:119` · `profile.jsp:280` · `profile.jsp:378` … (+1) |
| request | `members` | `TimelineServlet.java:178` | `project_report.jsp:852` · `timeline.jsp:150` · `timeline.jsp:363` |
| request | `overallProgress` | `TimelineServlet.java:202` | `timeline.jsp:99` · `timeline.jsp:104` |
| request | `overdueCount` | `TimelineServlet.java:200` | `project_report.jsp:296` · `project_report.jsp:304` · `project_report.jsp:307` · `project_report.jsp:585` … (+5) |
| request | `project` | `TimelineServlet.java:176` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `scheduledCount` | `TimelineServlet.java:190` | `timeline.jsp:53` |
| request | `taskProgressMap` | `TimelineServlet.java:183` | `tasks.jsp:14` · `tasks.jsp:778` · `tasks.jsp:1034` · `tasks.jsp:1082` … (+16) |
| request | `tasksJson` | `TimelineServlet.java:209` | `timeline.jsp:399` |
| request | `todayDate` | `TimelineServlet.java:205` | `timeline.jsp:235` · `timeline.jsp:347` · `timeline.jsp:394` |
| request | `todoCount` | `TimelineServlet.java:198` | `project_report.jsp:345` · `project_report.jsp:569` · `timeline.jsp:69` |
| request | `totalTasks` | `TimelineServlet.java:188` | `project_report.jsp:232` · `project_report.jsp:332` · `project_report.jsp:375` · `project_report.jsp:379` … (+8) |
| request | `unscheduledCount` | `TimelineServlet.java:192` | `timeline.jsp:53` · `timeline.jsp:219` · `timeline.jsp:224` |
| session | `toastError` | `TimelineServlet.java:74` · `TimelineServlet.java:391` | `docs.jsp:13` · `toast.jsp:8` · `toast.jsp:20` |
| session | `toastSuccess` | `TimelineServlet.java:346` · `TimelineServlet.java:390` | `docs.jsp:13` · `toast.jsp:7` · `toast.jsp:17` · `tasks.jsp:2054` |

_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ `completedSubtaskMap`(request), `horizonEndDate`(request), `horizonStartDate`(request), `subtaskCountMap`(request), `subtasksByTask`(request)

**Action** (`?action=…` → nhánh `case`): `updateDueDate`·248, `quickAddTask`·251

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `action` | `TimelineServlet.java:236` | _mọi link/form/fetch có `action=…`_ |
| `assigneeId` | `TimelineServlet.java:317` | `tasks.jsp:2162` · `tasks.jsp:2169` · `tasks.jsp:2176` … (+8) |
| `description` | `TimelineServlet.java:318` | `projects.jsp:538` · `tasks.jsp:2127` · `tasks.jsp:3397` … (+3) |
| `dueDate` | `TimelineServlet.java:315` | `tasks.jsp:2154` · `tasks.jsp:2319` · `tasks.jsp:2929` … (+4) |
| `isAjax` | `TimelineServlet.java:404` | `timeline.js:610` |
| `newDueDate` | `TimelineServlet.java:266` | `timeline.js:609` |
| `priority` | `TimelineServlet.java:316` | `tasks.jsp:2132` · `tasks.jsp:2138` · `tasks.jsp:2144` … (+5) |
| `projectId` | `TimelineServlet.java:241` · `TimelineServlet.java:459` | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |
| `taskId` | `TimelineServlet.java:265` | `tasks.jsp:2119` · `tasks.jsp:2301` · `tasks.jsp:2450` … (+15) |
| `title` | `TimelineServlet.java:314` | `docs.jsp:263` · `docs.jsp:322` · `tasks.jsp:2123` … (+6) |

## WhiteboardServlet

URL `/whiteboard` — mã nguồn `controllers/WhiteboardServlet.java`

**Forward (hiển thị):** `/whiteboard.jsp` ← `WhiteboardServlet.java:69`

**Redirect (sau khi ghi / điều hướng):** `/auth?action=login…` ← `WhiteboardServlet.java:46` · `/project?action=list…` ← `WhiteboardServlet.java:52` · `WhiteboardServlet.java:58`

**Servlet → JSP** (`setAttribute` ↔ `${…}`):

| scope | tên | Java set | JSP dùng |
|---|---|---|---|
| request | `project` | `WhiteboardServlet.java:65` | `chat.jsp:9` · `chat.jsp:256` · `chat.jsp:259` · `chat.jsp:301` … (+176) |
| request | `whiteboardJson` | `WhiteboardServlet.java:67` | `whiteboard.jsp:5` · `whiteboard.jsp:27` |

**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):

| tham số | Java đọc | JSP/JS gửi (gợi ý) |
|---|---|---|
| `projectId` | `WhiteboardServlet.java:50` · `WhiteboardServlet.java:86` | `chat.jsp:404` · `chat.jsp:534` · `docs.jsp:16` … (+93) |

