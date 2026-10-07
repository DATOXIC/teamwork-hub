#!/usr/bin/env node
/**
 * Sinh docs/LINK_MAP.md: bảng nối giữa Servlet (Java) và JSP/JS.
 *
 *   node docs/tools/gen_link_map.js
 *
 * Quét code thật (không viết tay) nên số dòng luôn đúng — chạy lại sau khi sửa code.
 *   - Servlet → JSP : request/session.setAttribute("tên") ↔ ${tên} trong JSP
 *   - JSP/JS → Servlet : action=..., getParameter("tên") ↔ name="tên" / fetch(...)
 *   - Chuyển trang  : forward (hiển thị JSP) / sendRedirect (đổi URL, gọi lại Servlet)
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const JAVA_DIR = path.join(ROOT, 'src/main/java/com/teamwork/controllers');
const WEBAPP = path.join(ROOT, 'src/main/webapp');
const OUT = path.join(ROOT, 'docs/LINK_MAP.md');

const rel = p => path.relative(ROOT, p).replace(/\\/g, '/');
const base = p => path.basename(p);
const walk = (dir, ext, out = []) => {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walk(full, ext, out);
    else if (e.name.endsWith(ext)) out.push(full);
  }
  return out;
};
const lines = f => fs.readFileSync(f, 'utf8').split(/\r?\n/);

// ---------- 1. Quét Java ----------
const servlets = {}; // key = tên Servlet chính
const javaFiles = walk(JAVA_DIR, '.java');

function ownerOf(file) {
  // controllers/task/*.java thuộc về TaskServlet
  return file.includes(`${path.sep}task${path.sep}`) ? 'TaskServlet' : base(file).replace('.java', '');
}
for (const f of javaFiles) {
  const owner = ownerOf(f);
  const s = servlets[owner] || (servlets[owner] = {
    url: '', attrs: {}, params: {}, actions: [], forwards: [], redirects: []
  });
  lines(f).forEach((ln, i) => {
    const at = `${base(f)}:${i + 1}`;
    let m;
    if ((m = ln.match(/@WebServlet\((?:.*urlPatterns\s*=\s*\{)?\s*"([^"]+)"/)) && base(f) === owner + '.java') s.url = m[1];
    const reSet = /([\w.()]+)\.setAttribute\("([^"]+)"/g;
    while ((m = reSet.exec(ln))) {
      const scope = /session/i.test(m[1]) ? 'session' : 'request';
      ((s.attrs[`${scope}|${m[2]}`]) || (s.attrs[`${scope}|${m[2]}`] = [])).push(at);
    }
    const reP = /getParameter\("([^"]+)"\)/g;
    while ((m = reP.exec(ln))) ((s.params[m[1]]) || (s.params[m[1]] = [])).push(at);
    if ((m = ln.match(/^\s*case "([^"]+)"\s*:/))) s.actions.push([m[1], at]);
    if ((m = ln.match(/getRequestDispatcher\("([^"]+)"\)/))) s.forwards.push([m[1], at]);
    if ((m = ln.match(/sendRedirect\((.*)\)/))) {
      const t = (m[1].match(/"([^"]*)"/) || [])[1];
      if (t) s.redirects.push([t, at]);
    }
  });
}

// ---------- 2. Quét JSP + JS ----------
// .jspf = mảnh JSP được include tĩnh (vd WEB-INF/jspf/tasks/*.jspf tách ra từ tasks.jsp)
const jspFiles = [...walk(WEBAPP, '.jsp'), ...walk(WEBAPP, '.jspf')];
const jsFiles = walk(path.join(WEBAPP, 'js'), '.js');
const RESERVED = new Set(['empty', 'not', 'and', 'or', 'eq', 'ne', 'lt', 'gt', 'le', 'ge', 'div', 'mod', 'true', 'false',
  'null', 'fn', 'param', 'paramValues', 'header', 'cookie', 'initParam', 'pageContext', 'requestScope', 'sessionScope',
  'applicationScope', 'pageScope', 'instanceof']);
const jspUse = {};   // tên → ["tasks.jsp:12", ...]
const addUse = (name, at) => ((jspUse[name]) || (jspUse[name] = [])).push(at);

for (const f of jspFiles) {
  lines(f).forEach((ln, i) => {
    const at = `${base(f)}:${i + 1}`;
    let m;
    const reEl = /\$\{([^}]*)\}/g;
    while ((m = reEl.exec(ln))) {
      const expr = m[1].replace(/'[^']*'|"[^"]*"/g, ''); // bỏ chuỗi literal
      const reId = /(?<![.\w])([A-Za-z_]\w*)/g;
      let id;
      while ((id = reId.exec(expr))) if (!RESERVED.has(id[1])) addUse(id[1], at);
      const reScope = /(?:requestScope|sessionScope)\.(\w+)/g;
      while ((id = reScope.exec(expr))) addUse(id[1], at);
    }
    const reGet = /getAttribute\("([^"]+)"\)/g; // scriptlet còn sót
    while ((m = reGet.exec(ln))) addUse(m[1], at);
  });
}

// Tham số: tìm `name="x"` trong JSP và x= / append('x' trong JS
const paramSend = {};
const addSend = (name, at) => ((paramSend[name]) || (paramSend[name] = [])).push(at);
const allFront = [...jspFiles, ...jsFiles];
const wanted = new Set();
Object.values(servlets).forEach(s => Object.keys(s.params).forEach(p => wanted.add(p)));
for (const f of allFront) {
  lines(f).forEach((ln, i) => {
    for (const p of wanted) {
      if (new RegExp(`name=["']${p}["']|[?&'"]${p}=|append\\(["']${p}["']|["']${p}["']\\s*:`).test(ln)) {
        addSend(p, `${base(f)}:${i + 1}`);
      }
    }
  });
}

// ---------- 3. Sinh Markdown ----------
const cap = (arr, n = 4) => {
  if (!arr || !arr.length) return '—';
  const u = [...new Set(arr)];
  return u.slice(0, n).map(x => '`' + x + '`').join(' · ') + (u.length > n ? ` … (+${u.length - n})` : '');
};
// gộp "Handler.java:12, Handler.java:18" → giữ tối đa 3
const out = [];
out.push('# LINK_MAP — Servlet ↔ JSP nối với nhau ở đâu');
out.push('');
out.push('> **File tự sinh** bởi `node docs/tools/gen_link_map.js` — đừng sửa tay; sửa code xong chạy lại để số dòng luôn đúng.');
out.push('> Cách đọc: tìm **tên** (vd `todoTasks`) trong cột Java rồi sang cột JSP. Mô hình chung:');
out.push('');
out.push('```');
out.push('Trình duyệt ──GET/POST /url?action=x&param=y──▶ Servlet  (getParameter)');
out.push('Servlet ──request.setAttribute("tên", dữ liệu)──▶ forward ──▶ JSP  (${tên})');
out.push('Servlet ──session.setAttribute("toastSuccess", …)──▶ sendRedirect ──▶ Servlet khác ──▶ JSP');
out.push('```');
out.push('');
out.push('- **forward**: server chuyển nội bộ sang JSP, URL không đổi, `request` còn nguyên → dùng để *hiển thị*.');
out.push('- **sendRedirect**: bảo trình duyệt gọi URL mới (PRG), `request` mất → dùng sau khi *ghi DB*; muốn nhắn lại thì dùng `session` (toast).');
out.push('- **Ký hiệu trong code:** `▶` = "đầu này đẩy dữ liệu / gọi sang phía bên kia", `◀` = "đầu này nhận dữ liệu từ Servlet". Ctrl+F `▶` hoặc `◀` (hay Ctrl+F đúng tên attribute) để nhảy giữa Servlet ↔ JSP.');
out.push('- Một số thao tác (kéo thả, subtask, chat) **không qua JSP**: JS `fetch` gọi Servlet và Servlet trả JSON.');
out.push('');

const order = Object.keys(servlets).filter(k => servlets[k].url).sort();
out.push('## Mục lục');
out.push('');
out.push('| URL | Servlet | Hiển thị bằng JSP |');
out.push('|---|---|---|');
for (const k of order) {
  const s = servlets[k];
  const jsps = [...new Set(s.forwards.map(f => f[0].replace(/^\//, '')))];
  out.push(`| \`${s.url}\` | [${k}](#${k.toLowerCase()}) | ${jsps.length ? jsps.map(j => '`' + j + '`').join(', ') : '_không forward JSP (trả JSON/redirect)_'} |`);
}
out.push('');

for (const k of order) {
  const s = servlets[k];
  out.push(`## ${k}`);
  out.push('');
  out.push(`URL \`${s.url}\` — mã nguồn \`${k === 'TaskServlet' ? 'controllers/TaskServlet.java + controllers/task/*.java' : 'controllers/' + k + '.java'}\``);
  out.push('');

  // forward / redirect
  if (s.forwards.length) {
    const fw = {};
    s.forwards.forEach(([j, at]) => (fw[j] = fw[j] || []).push(at));
    out.push('**Forward (hiển thị):** ' + Object.keys(fw).map(j => `\`${j}\` ← ${cap(fw[j], 7)}`).join(' · '));
    out.push('');
  }
  const rd = {};
  s.redirects.forEach(([t, at]) => (rd[t] = rd[t] || []).push(at));
  const rdKeys = Object.keys(rd);
  if (rdKeys.length) {
    out.push('**Redirect (sau khi ghi / điều hướng):** ' + rdKeys.slice(0, 8).map(t => `\`${t}…\` ← ${cap(rd[t], 2)}`).join(' · ') + (rdKeys.length > 8 ? ` … (+${rdKeys.length - 8})` : ''));
    out.push('');
  }

  // attribute
  const attrKeys = Object.keys(s.attrs).sort((a, b) => a.localeCompare(b));
  const withJsp = [], noJsp = [];
  for (const key of attrKeys) {
    const [scope, name] = key.split('|');
    (jspUse[name] ? withJsp : noJsp).push([scope, name, s.attrs[key]]);
  }
  if (withJsp.length) {
    out.push('**Servlet → JSP** (`setAttribute` ↔ `${…}`):');
    out.push('');
    out.push('| scope | tên | Java set | JSP dùng |');
    out.push('|---|---|---|---|');
    for (const [scope, name, at] of withJsp) out.push(`| ${scope} | \`${name}\` | ${cap(at, 2)} | ${cap(jspUse[name], 4)} |`);
    out.push('');
  }
  if (noJsp.length) {
    out.push('_Set nhưng JSP không đọc bằng `${…}` (dùng nội bộ Java / Servlet khác / JS):_ ' +
      noJsp.map(([sc, n]) => `\`${n}\`(${sc})`).join(', '));
    out.push('');
  }

  // actions
  if (s.actions.length) {
    out.push('**Action** (`?action=…` → nhánh `case`): ' + s.actions.map(([a, at]) => `\`${a}\`·${at.split(':')[1]}`).join(', '));
    out.push('');
  }

  // params
  const pKeys = Object.keys(s.params).sort();
  if (pKeys.length) {
    out.push('**Tham số Servlet đọc** (`getParameter` ← form/JS gửi lên):');
    out.push('');
    out.push('| tham số | Java đọc | JSP/JS gửi (gợi ý) |');
    out.push('|---|---|---|');
    for (const p of pKeys) out.push(`| \`${p}\` | ${cap(s.params[p], 2)} | ${p === 'action' ? '_mọi link/form/fetch có `action=…`_' : cap(paramSend[p], 3)} |`);
    out.push('');
  }
}

fs.writeFileSync(OUT, out.join('\n') + '\n', 'utf8');
console.log(`Đã ghi ${rel(OUT)} (${out.length} dòng, ${order.length} servlet)`);
