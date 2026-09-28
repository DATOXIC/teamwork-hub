# SỔ TAY BẢO VỆ ĐỒ ÁN — TeamWork Hub

> Mở file này ở một tab riêng khi demo. Mọi câu trả lời đều có **số dòng chính xác** để mở code ngay.
>
> **Nguyên tắc vàng:** đừng đọc thuộc lòng. Mỗi câu hỏi đều có phần *"chỉ vào đâu"* — cứ mở đúng file,
> đúng dòng rồi **đọc code ra thành lời**. Giảng viên đánh giá việc bạn hiểu luồng dữ liệu, không phải
> bạn nhớ được bao nhiêu thuật ngữ.
>
> ⚠️ Số dòng trong file này khớp với code **sau khi đã dọn scriptlet** (xem Mục 5). Nếu bạn sửa code
> tiếp, số dòng sẽ lệch — chạy `/demo-where Project` để lấy lại số dòng mới.

---

## 0. CHUẨN BỊ TRƯỚC KHI VÀO PHÒNG

| Việc | Lệnh / Cách làm |
|---|---|
| Chạy `build_and_run.bat`, đăng nhập sẵn, mở sẵn trang Workspace | |
| **Kiểm tra 4 chỉ số KPI** vẫn đúng sau khi dọn scriptlet (xem Mục 5) | |
| **Test thử `hot_jsp.bat`** — sửa 1 chữ trong `projects.jsp`, chạy, F5 xem có đổi không | `hot_jsp.bat projects.jsp` |
| Mở sẵn 4 tab editor: `ProjectServlet.java`, `projects.jsp`, `Project.java`, `ProjectDB.java` | |
| `git commit` để luôn có điểm rollback | |

### ⚠️ Cạm bẫy chí tử: đừng chạy `build_and_run.bat` giữa buổi demo

Script đó làm những việc sau ([build_and_run.bat:98-134](../build_and_run.bat)):
- `taskkill /F /IM java.exe` → **tắt Tomcat**
- `rmdir /S /Q` deploy dir → **xoá sạch ứng dụng**
- biên dịch lại **toàn bộ** file .java
- xoá cache `work\Catalina` → **JSP phải dịch lại từ đầu**

Hệ quả khi demo: mất ~1 phút, **bạn bị đăng xuất**, phải login lại, mất hết trạng thái đang trình bày.

**Quy tắc:**

| Bạn sửa gì | Dùng lệnh nào | Mất bao lâu | Có mất đăng nhập? |
|---|---|---|---|
| Chỉ file `.jsp` / `.css` / `.js` | `hot_jsp.bat projects.jsp` | ~2 giây | **Không** |
| Có sửa file `.java` | `build_and_run.bat` | ~60 giây | **Có** |

👉 **Ưu tiên tuyệt đối mọi demo "thêm tính năng" bằng cách chỉ sửa JSP.** Xem Mục 1.

---

## 1. THỬ THÁCH: "Tạo thêm một thứ gì đó và hiển thị nó lên Web"

Có 3 cấp độ. **Luôn chọn cấp thấp nhất mà vẫn thoả yêu cầu của cô.**

### 🟢 Cấp A — Chỉ sửa JSP (an toàn nhất, ~20 giây, KHÔNG mất đăng nhập)

Dữ liệu đã có sẵn trong request scope, bạn chỉ cần *hiển thị thêm*. Đây là 95% trường hợp.

**Paste đoạn này vào [projects.jsp](../src/main/webapp/projects.jsp) ngay sau dòng 303** (giữa dòng
"Tiến độ" và thanh progress bar):

```jsp
<!-- DEMO: Số việc còn lại — tính trực tiếp bằng EL, không cần Java, không cần DB -->
<div class="d-flex justify-content-between align-items-center fs-8 mb-1-5">
    <span style="color: #627D98;">
        <i class="bi bi-hourglass-split me-1"></i> Việc còn lại
    </span>
    <c:choose>
        <c:when test="${p.totalTasks - p.doneTasks == 0}">
            <span class="badge rounded-pill bg-success-subtle text-success px-2 py-1 fs-9">
                Đã xong hết
            </span>
        </c:when>
        <c:otherwise>
            <strong class="text-danger">${p.totalTasks - p.doneTasks} việc</strong>
        </c:otherwise>
    </c:choose>
</div>
```

Rồi chạy:

```bash
hot_jsp.bat projects.jsp
```

F5 → xuất hiện ngay.

**Lời thuyết trình kèm theo** (quan trọng hơn cả đoạn code):
> "Em thêm một chỉ số mới là *Việc còn lại*. Em không cần sửa Java hay database, vì đối tượng
> `Project` đã nằm trong request scope rồi. Em dùng EL `${p.totalTasks - p.doneTasks}` để tính
> trực tiếp trên tầng View, và dùng `<c:choose>` của JSTL để hiển thị khác nhau giữa trường hợp
> còn việc và đã xong hết."

### 🟡 Cấp B — Thêm getter dẫn xuất trong `Project.java` (~60 giây, có restart)

Dùng khi cô yêu cầu *"thêm vào tầng Java/model"*. Thêm vào
[Project.java](../src/main/java/com/teamwork/business/Project.java) cạnh `getProgressPercentage()` (dòng 97):

```java
    /**
     * DEMO: Số việc còn lại — thuộc tính dẫn xuất, không lưu trong database
     */
    public int getRemainingTasks() {
        return Math.max(0, totalTasks - doneTasks);
    }

    /**
     * DEMO: Nhãn trạng thái tiến độ của dự án
     */
    public String getProgressLabel() {
        int percent = getProgressPercentage();
        if (percent >= 100) return "Hoàn thành";
        if (percent >= 50)  return "Đang bám tiến độ";
        if (percent > 0)    return "Mới bắt đầu";
        return "Chưa khởi động";
    }
```

Dùng trong JSP: `${p.remainingTasks}` và `${p.progressLabel}`.

**Điểm ăn tiền khi thuyết trình:**
> "Đây là thuộc tính *dẫn xuất* (derived). Nó không có cột trong database — giống hệt
> `getProgressPercentage()` ở dòng 97 mà em đã làm. EL gọi được `${p.remainingTasks}` là nhờ
> chuẩn JavaBeans: `${p.remainingTasks}` tự động gọi `getRemainingTasks()`."

Rồi `build_and_run.bat`, login lại.

### 🔴 Cấp C — Thực thể mới hoàn chỉnh (chỉ khi cô bắt buộc)

Cần bảng DB mới + Entity + DAO + Servlet + JSP. **Đừng làm tay** — gõ trong Claude Code:

```bash
/demo-add-entity Announcement
```

Lệnh sinh đủ 5 tầng theo đúng convention dự án. **Hãy tập chạy thử ở nhà ít nhất 1 lần.**

---

## 2. CÂU HỎI: "Servlet này xử lý đối tượng Project, vậy JSP của em dùng đối tượng này ở đâu?"

Đây là câu hỏi về **luồng dữ liệu xuyên 3 tầng**. Trả lời bằng cách chỉ vào 4 điểm, theo đúng thứ tự.

### Mạch trả lời 4 bước — học thuộc thứ tự này

```
[1] DAO lấy Project từ DB     →  ProjectServlet.java:114
[2] Servlet đặt vào request    →  ProjectServlet.java:138
[3] Servlet forward sang JSP   →  ProjectServlet.java:188
[4] JSP lấy ra và dùng         →  projects.jsp:239  và  projects.jsp:277
```

### Bước 1 — Lấy dữ liệu: [ProjectServlet.java:114](../src/main/java/com/teamwork/controllers/ProjectServlet.java)

```java
List<Project> allProjects = ProjectDB.selectAll();
```

> "Servlet gọi tầng DAO là `ProjectDB.selectAll()`, nhận về một `List<Project>` — tức là một danh
> sách các đối tượng `Project` thực sự trong bộ nhớ, chứ không còn là dòng dữ liệu SQL nữa."

### Bước 2 — Đặt vào request scope: [ProjectServlet.java:138-140](../src/main/java/com/teamwork/controllers/ProjectServlet.java)

```java
request.setAttribute("myProjects", myProjects);
request.setAttribute("otherProjects", otherProjects);
request.setAttribute("projects", allProjects);
```

> "Servlet **không tự in HTML**. Nó đóng gói dữ liệu vào `request` dưới cái tên `myProjects`.
> Cái tên chuỗi `"myProjects"` này chính là chiếc cầu nối — bên JSP em sẽ gọi lại đúng tên đó."

☝️ **Đây là câu chốt quan trọng nhất của toàn bộ câu hỏi này.** Tên attribute = hợp đồng giữa 2 tầng.

### Bước 3 — Chuyển quyền sang View: [ProjectServlet.java:188](../src/main/java/com/teamwork/controllers/ProjectServlet.java)

```java
request.getRequestDispatcher("/projects.jsp").forward(request, response);
```

> "`forward` chuyển quyền xử lý sang `projects.jsp`, và **mang theo nguyên object `request`** —
> nhờ vậy mọi attribute em vừa đặt ở trên vẫn còn nguyên khi sang JSP."

### Bước 4 — JSP dùng đối tượng: [projects.jsp:239](../src/main/webapp/projects.jsp)

```jsp
<c:forEach items="${myProjects}" var="p">
```

> "`${myProjects}` lấy đúng cái List em vừa đặt. `var="p"` nghĩa là mỗi vòng lặp, `p` **chính là
> một đối tượng `Project`**."

Rồi chỉ vào các dòng dùng `p` — **mỗi dòng là một lần gọi getter**:

| Dòng JSP | Code EL | Thực chất gọi hàm Java nào |
|---|---|---|
| **251** | `${p.projectCode}` | `getProjectCode()` — [Project.java:112](../src/main/java/com/teamwork/business/Project.java) |
| **277** | `${p.name}` | `getName()` — [Project.java:126](../src/main/java/com/teamwork/business/Project.java) |
| **282** | `${p.description}` | `getDescription()` — [Project.java:133](../src/main/java/com/teamwork/business/Project.java) |
| **291** | `${p.projectTypeBadgeClass}` | `getProjectTypeBadgeClass()` — [Project.java:187](../src/main/java/com/teamwork/business/Project.java) |
| **293** | `${p.projectTypeLabel}` | `getProjectTypeLabel()` — [Project.java:183](../src/main/java/com/teamwork/business/Project.java) |
| **301** | `${p.doneTasks}/${p.totalTasks}` | `getDoneTasks()` / `getTotalTasks()` |
| **302** | `${p.progressPercentage}` | `getProgressPercentage()` — [Project.java:97](../src/main/java/com/teamwork/business/Project.java) |

### 🎯 Bằng chứng "không thể chối" — nếu cô vẫn chưa tin

Chỉ vào [projects.jsp:302](../src/main/webapp/projects.jsp): `${p.progressPercentage}`

> "Dạ đây là bằng chứng rõ nhất ạ. Trong class `Project` **không có biến nào tên
> `progressPercentage`** — cô có thể xem toàn bộ phần khai báo field từ dòng 24 đến 55 của
> `Project.java`. Chỉ có **method** `getProgressPercentage()` ở dòng 97.
>
> Nghĩa là `${p.progressPercentage}` **buộc phải** đang gọi method của một đối tượng `Project`
> thật. Nếu `p` là kiểu khác thì EL không tìm thấy method này và trang sẽ báo lỗi
> `PropertyNotFoundException` ngay. Trang đang chạy bình thường, tức là `p` đúng là `Project` ạ."

Củng cố thêm: `${p.projectTypeLabel}`, `${p.projectTypeBadgeClass}`, `${p.projectTypeIcon}`
(dòng 291-293) cũng là **3 getter chỉ tồn tại trong `Project`** ([Project.java:183-193](../src/main/java/com/teamwork/business/Project.java)).

**Nếu muốn chứng minh trực quan ngay trên màn hình** (rất ấn tượng, nhưng **phải test ở nhà trước**):
tạm thêm vào trong `<c:forEach>` dòng sau, chạy `hot_jsp.bat projects.jsp`, F5:

```jsp
<small class="text-muted">Kiểu đối tượng: ${p['class'].name}</small>
```

Nếu chạy được, trang sẽ in ra đúng chữ `com.teamwork.business.Project` cạnh mỗi thẻ. Nhớ xoá sau khi demo.

### 💡 Câu ghi điểm cộng nếu còn thời gian

`${p.progressPercentage}` **không phải cột trong database**:
- [Project.java:51-55](../src/main/java/com/teamwork/business/Project.java): `totalTasks`, `doneTasks` đánh dấu `@Transient` → JPA **không** map xuống DB
- [ProjectDB.java:63-81](../src/main/java/com/teamwork/data/ProjectDB.java): `populateTaskStats()` đếm bằng JPQL rồi set vào object
- [Project.java:97](../src/main/java/com/teamwork/business/Project.java): getter tự chia ra phần trăm

> "Nên `${p.progressPercentage}` là một phép tính chạy trong tầng Business, JSP chỉ hiển thị kết quả."

---

## 3. CÂU HỎI: "Đoạn code JSP nào giúp em trình bày được nhiều project như vậy?"

### Câu trả lời một dòng

**[projects.jsp:239](../src/main/webapp/projects.jsp)** — thẻ `<c:forEach>` của JSTL.

```jsp
<c:forEach items="${myProjects}" var="p">
    ...  <!-- toàn bộ HTML của MỘT thẻ dự án, từ dòng 240 đến 323 -->
</c:forEach>     <!-- dòng 324 -->
```

### Cách diễn giải cho ăn điểm

> "Dạ, em **chỉ viết HTML cho đúng một thẻ dự án** — từ dòng 240 đến dòng 323. Sau đó em bọc nó
> trong `<c:forEach>` ở dòng 239. Thẻ này lặp qua từng phần tử của list `myProjects`; mỗi vòng lặp
> nó gán một đối tượng `Project` vào biến `p` rồi sinh ra một khối HTML mới.
>
> Nên nếu database có 5 dự án thì `<c:forEach>` chạy 5 vòng và sinh ra 5 thẻ. Em **không hề viết
> HTML 5 lần** — số thẻ hiển thị do dữ liệu quyết định, không do code quyết định ạ."

### Chỉ thêm 3 điểm này để thể hiện chiều sâu

**a) Có 2 vòng lặp riêng cho 2 nhóm** — [:239](../src/main/webapp/projects.jsp) cho `myProjects`,
[:372](../src/main/webapp/projects.jsp) cho `otherProjects`. Việc phân loại làm ở Servlet
([ProjectServlet.java:126-132](../src/main/java/com/teamwork/controllers/ProjectServlet.java)),
không làm ở JSP → đúng tinh thần MVC.

**b) Xử lý danh sách rỗng** — [:336](../src/main/webapp/projects.jsp):
```jsp
<c:if test="${empty myProjects}">   <!-- hiện empty state thay vì trang trắng -->
```

**c) Nội dung mỗi thẻ thay đổi theo dữ liệu**, không chỉ lặp cứng:
- [:257-272](../src/main/webapp/projects.jsp) `<c:choose>` → badge "Trưởng nhóm" hay "Thành viên",
  dựa vào `${p.ownerId == sessionScope.currentUser.id}`
- [:289](../src/main/webapp/projects.jsp) `${memberCountMap[p.id]}` → **tra Map bằng khoá** `p.id`

> "Ở dòng 289 em tra một `Map` bằng khoá: `${memberCountMap[p.id]}`. Map này được Servlet dựng sẵn
> ở dòng 159-164. Em làm vậy vì EL không gọi được hàm có tham số như `countMembers(p.id)`, nên em
> chuẩn bị sẵn dữ liệu ở Controller rồi JSP chỉ việc tra cứu."

### ✅ Câu hỏi đi kèm: "Sao em dùng JSTL mà không dùng scriptlet?"

Trước đây file này **có** một scriptlet. Đã dọn sạch (Mục 5), nên giờ bạn trả lời được một cách
rất mạnh — **hãy chủ động nói luôn, đừng đợi cô hỏi**:

> "Dạ toàn bộ `projects.jsp` của em **không còn một scriptlet `<% %>` nào** — cô có thể Ctrl+F tìm
> `<%` trong file, chỉ còn 2 directive `<%@ page %>`, `<%@ taglib %>` ở đầu file và các comment
> `<%-- --%>`.
>
> Ban đầu em có tính toán mấy chỉ số KPI bằng scriptlet ngay trong JSP, nhưng em nhận ra như vậy là
> đưa logic Java vào tầng View, sai nguyên tắc MVC. Nên em đã chuyển toàn bộ phần tính toán đó về
> `ProjectServlet` ở dòng 142-157, rồi JSP chỉ đọc kết quả bằng EL."

Đây là kiểu trả lời **rất được điểm**: cho thấy bạn tự nhìn ra điểm yếu và tự sửa.

---

## 4. 15 TÌNH HUỐNG KHÁC — LUYỆN TẬP

<details>
<summary><b>Q1. forward và sendRedirect khác gì nhau? Vì sao em dùng cả hai?</b></summary>

Dự án bạn dùng đúng cả hai:

| | `forward` | `sendRedirect` |
|---|---|---|
| Nơi dùng | [ProjectServlet.java:188](../src/main/java/com/teamwork/controllers/ProjectServlet.java) | [ProjectServlet.java:262](../src/main/java/com/teamwork/controllers/ProjectServlet.java) |
| Số request | 1 (server nội bộ) | 2 (server bảo browser gọi lại) |
| URL trên browser | **không đổi** | **đổi** |
| Attribute trong request | **còn** | **mất** |

> "Khi hiển thị danh sách (dòng 188) em dùng `forward` vì cần giữ các attribute vừa `setAttribute`.
> Còn sau khi **tạo dự án thành công** (dòng 262) em dùng `sendRedirect` theo mẫu
> **POST-Redirect-GET**: nếu dùng forward, URL vẫn là POST nên người dùng bấm F5 sẽ **tạo trùng
> dự án lần nữa**. Redirect biến nó thành GET nên F5 chỉ tải lại danh sách."

**Câu nối tiếp gần như chắc chắn: "Redirect làm mất attribute, sao thông báo thành công vẫn hiện?"**

Đây là **flash scope** tự cài, chỉ vào 2 chỗ:
- [ProjectServlet.java:260](../src/main/java/com/teamwork/controllers/ProjectServlet.java) — lưu vào
  **session** (vì session sống qua nhiều request): `session.setAttribute("toastSuccess", ...)`
- [ProjectServlet.java:173-185](../src/main/java/com/teamwork/controllers/ProjectServlet.java) —
  request sau đọc ra, **rồi `removeAttribute` ngay** để lần F5 sau không hiện lại nữa.
</details>

<details>
<summary><b>Q2. <code>${p.name}</code> hoạt động thế nào? Nó lấy biến private <code>name</code> à?</b></summary>

**Không.** Đây là câu rất nhiều bạn trả lời sai.

> "Dạ `${p.name}` **không** đọc trực tiếp biến `private name`. EL tuân theo chuẩn **JavaBeans**:
> nó viết hoa chữ đầu, thêm tiền tố `get`, tạo thành `getName()`, rồi gọi method đó.
>
> Bằng chứng: `${p.progressPercentage}` ở [projects.jsp:302](../src/main/webapp/projects.jsp) vẫn
> chạy, mặc dù trong `Project.java` **không có biến nào tên `progressPercentage`** — chỉ có method
> `getProgressPercentage()` ở dòng 97. Nếu EL đọc biến thì dòng này phải lỗi."

Đó là lập luận đóng đinh. Với `boolean`, EL nhận cả `isXxx()` — ví dụ `${p.soloProject}` gọi
`isSoloProject()` ([Project.java:175](../src/main/java/com/teamwork/business/Project.java)).
</details>

<details>
<summary><b>Q3. Vì sao chọn JSTL thay vì scriptlet?</b></summary>

> "Ba lý do: (1) tách biệt vai trò — View chỉ trình bày, không chứa logic Java; (2) code ngắn và dễ
> đọc, người làm frontend không biết Java vẫn sửa được; (3) tránh lặp code nhờ các thẻ lặp và
> điều kiện có sẵn."

Nếu cô hỏi về XSS: **cẩn thận, đừng nói quá.** `${...}` trong JSP **không** tự escape HTML. Chỉ
`<c:out value="...">` mới escape. Nói an toàn:
> "JSTL có cung cấp `<c:out>` để escape HTML khi cần hiển thị dữ liệu người dùng nhập ạ."
</details>

<details>
<summary><b>Q4. request scope, session scope, application scope khác nhau ra sao?</b></summary>

| Scope | Sống bao lâu | Dự án bạn dùng ở đâu |
|---|---|---|
| **request** | 1 lần request–response | `myProjects` — [ProjectServlet.java:138](../src/main/java/com/teamwork/controllers/ProjectServlet.java) |
| **session** | Suốt phiên của 1 user | `currentUser` — đọc ở [ProjectServlet.java:45](../src/main/java/com/teamwork/controllers/ProjectServlet.java) |
| **application** | Toàn ứng dụng, mọi user | dự án chưa dùng |

> "Danh sách dự án em để ở request vì mỗi lần load lại phải lấy mới. Còn `currentUser` phải ở
> session vì user đăng nhập một lần rồi đi qua nhiều trang, không thể bắt đăng nhập lại mỗi request."
</details>

<details>
<summary><b>Q5. Làm sao server biết URL <code>/project</code> chạy Servlet nào?</b></summary>

[ProjectServlet.java:35](../src/main/java/com/teamwork/controllers/ProjectServlet.java):
```java
@WebServlet("/project")
```

> "Em dùng annotation `@WebServlet("/project")`. Từ Servlet 3.0 trở đi, container tự quét annotation
> này khi khởi động và tự đăng ký mapping, nên em không cần khai báo `<servlet-mapping>` trong
> `web.xml` nữa."
</details>

<details>
<summary><b>Q6. Một Servlet xử lý nhiều việc (list/detail/report) — em làm sao?</b></summary>

Mẫu **Front Controller** theo tham số `action`:
- [ProjectServlet.java:51-54](../src/main/java/com/teamwork/controllers/ProjectServlet.java) — đọc
  `action`, mặc định `"list"` nếu thiếu
- [ProjectServlet.java:56-69](../src/main/java/com/teamwork/controllers/ProjectServlet.java) —
  `switch` phân nhánh sang từng method riêng
- Bên JSP, form gửi `action` qua hidden input —
  [projects.jsp:486](../src/main/webapp/projects.jsp): `<input type="hidden" name="action" value="create">`

> "Em tách GET và POST rõ ràng: GET là xem (list/detail/report), POST là thay đổi dữ liệu
> (create/update). Mỗi nhánh gọi một method private riêng nên `doGet` vẫn ngắn và dễ đọc."
</details>

<details>
<summary><b>Q7. <code>${sessionScope.currentUser.fullName}</code> — <code>currentUser</code> ở đâu ra?</b></summary>

Từ `AuthServlet` khi đăng nhập thành công (`session.setAttribute("currentUser", user)`).
JSP đọc lại ở [projects.jsp:21](../src/main/webapp/projects.jsp).

> "`sessionScope` là implicit object của EL, cho phép em đọc trực tiếp attribute trong session mà
> không cần Servlet `setAttribute` lại vào request."
</details>

<details>
<summary><b>Q8. Người chưa đăng nhập gõ thẳng URL <code>/project</code> thì sao?</b></summary>

**Hai lớp bảo vệ** — trả lời được cả hai sẽ rất ấn tượng:

1. `AuthFilter` ([filters/AuthFilter.java](../src/main/java/com/teamwork/filters/AuthFilter.java)) —
   chặn ở tầng ngoài, trước khi tới Servlet
2. Bản thân Servlet cũng tự kiểm tra —
   [ProjectServlet.java:44-49](../src/main/java/com/teamwork/controllers/ProjectServlet.java):
```java
User currentUserCheck = (sessionCheck != null) ? (User) sessionCheck.getAttribute("currentUser") : null;
if (currentUserCheck == null) {
    response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
    return;
}
```

> "Em dùng `getSession(false)` — tham số `false` nghĩa là **không tạo session mới** nếu chưa có.
> Nếu dùng `getSession()` thì nó luôn tạo session rỗng, biến việc kiểm tra thành vô nghĩa ạ."

Điểm cộng: `doPost` cũng chặn riêng
([:79-83](../src/main/java/com/teamwork/controllers/ProjectServlet.java)) để chống giả mạo POST, và
`updateProject` còn kiểm tra quyền sở hữu
([:288](../src/main/java/com/teamwork/controllers/ProjectServlet.java)
`project.getOwnerId() == currentUser.getId()`).
</details>

<details>
<summary><b>Q9. Em chống SQL Injection thế nào?</b></summary>

Dự án dùng **JPA/Hibernate với JPQL + tham số có tên**, không nối chuỗi:
[ProjectDB.java:132-135](../src/main/java/com/teamwork/data/ProjectDB.java)
```java
String jpql = "SELECT p FROM Project p WHERE UPPER(p.projectCode) = UPPER(:code)";
em.createQuery(jpql, Project.class).setParameter("code", code.trim())
```

> "Em không nối chuỗi vào câu truy vấn. `:code` là tham số có tên, `setParameter` truyền giá trị
> tách biệt khỏi câu lệnh, nên dữ liệu người dùng không bao giờ được hiểu là mã SQL."

Thêm: validate bằng regex `PROJECT_CODE_PATTERN`
([khai báo ở :37](../src/main/java/com/teamwork/controllers/ProjectServlet.java),
[kiểm tra ở :211](../src/main/java/com/teamwork/controllers/ProjectServlet.java)).
</details>

<details>
<summary><b>Q10. Dự án dùng JDBC hay JPA? (dễ trả lời sai!)</b></summary>

**JPA / Hibernate** — không phải JDBC thuần. Chỉ vào
[ProjectDB.java:86-101](../src/main/java/com/teamwork/data/ProjectDB.java): `EntityManager`,
`em.createQuery`, `em.persist`, `em.merge`.

Và `Project` là **Entity**:
[Project.java:20-21](../src/main/java/com/teamwork/business/Project.java) `@Entity @Table(name = "projects")`.

Nếu cô hỏi *"sao còn hàm `mapResultSetToProject` nhận `ResultSet`?"* →
[ProjectDB.java:25](../src/main/java/com/teamwork/data/ProjectDB.java):
> "Đó là hàm giữ tương thích ngược từ giai đoạn đầu dự án em còn dùng JDBC. Luồng chính hiện tại
> đã chuyển hết sang JPA, hàm đó còn lại để không phá vỡ code cũ ạ."

(Trung thực — và đúng như comment ở dòng 23 trong code.)
</details>

<details>
<summary><b>Q11. Servlet có bao nhiêu instance? Có an toàn đa luồng không?</b></summary>

> "Container tạo **duy nhất một instance** cho mỗi Servlet, rồi dùng nhiều thread để phục vụ nhiều
> request đồng thời. Nên biến instance của Servlet là **chia sẻ giữa các thread** và không an toàn.
>
> `ProjectServlet` của em an toàn vì nó **không có biến instance nào** — chỉ có một hằng
> `static final PROJECT_CODE_PATTERN` ở dòng 37 (bất biến nên vô hại). Mọi dữ liệu theo từng người
> dùng em để trong biến cục bộ của method hoặc trong request/session."

Vòng đời: `init()` → `service()` (gọi `doGet`/`doPost`) nhiều lần → `destroy()`.
</details>

<details>
<summary><b>Q12. JSP thực chất là gì? Nó chạy ra sao?</b></summary>

> "JSP là **một Servlet được viết ngược lại**. Tomcat có bộ dịch Jasper: lần đầu có request tới
> `projects.jsp`, nó **dịch file JSP thành mã Java** của một class servlet với method
> `_jspService()`, biên dịch thành `.class`, rồi mới chạy. Các lần sau dùng luôn bản đã biên dịch
> nên nhanh."

Bằng chứng sống: [build_and_run.bat:132-134](../build_and_run.bat) xoá thư mục
`work\Catalina\localhost\teamwork-hub` — đó chính là nơi Tomcat để các file `.java`/`.class` sinh
ra từ JSP.
> "Cô có thể vào thư mục `work` của Tomcat để xem file `projects_jsp.java` mà Jasper sinh ra ạ."

Câu này gây ấn tượng rất tốt vì rất ít sinh viên dám chỉ vào thư mục `work`.

**Đây cũng chính là lý do `hot_jsp.bat` chạy được:** chỉ cần copy file `.jsp` mới vào, Jasper thấy
file mới hơn bản đã biên dịch thì tự dịch lại ở request kế tiếp — không cần restart Tomcat.
</details>

<details>
<summary><b>Q13. <code>&lt;jsp:include&gt;</code> và <code>&lt;%@ include %&gt;</code> khác gì?</b></summary>

Bạn dùng `<jsp:include>` ở [projects.jsp:6, 7, 46, 681](../src/main/webapp/projects.jsp).

| | `<%@ include file %>` | `<jsp:include page />` |
|---|---|---|
| Thời điểm | lúc **dịch** (static) | lúc **chạy** (dynamic) |
| Cách ghép | dán mã nguồn vào rồi mới dịch | gọi sang trang khác, lấy kết quả |
| Sửa file con | có thể phải dịch lại trang cha | nhận thay đổi ngay |

> "Em chọn `<jsp:include>` cho header/navbar/footer vì nó gọi lúc chạy, nên khi em sửa `navbar.jsp`
> thì mọi trang đều nhận thay đổi ngay mà không phải dịch lại."
</details>

<details>
<summary><b>Q14. Nếu <code>myProjects</code> bị null thì JSP có lỗi không?</b></summary>

Không — và bạn đã phòng ở **cả hai tầng**:
- Servlet: [ProjectServlet.java:115](../src/main/java/com/teamwork/controllers/ProjectServlet.java)
  khởi tạo `new ArrayList<>()` ngay, và
  [:133-136](../src/main/java/com/teamwork/controllers/ProjectServlet.java) có nhánh `else` cho
  trường hợp `currentUser == null` (comment trong code ghi rõ *"Safe Code → Giúp JSP không bị lỗi"*)
- JSP: `<c:forEach>` với `items` null thì **lặp 0 vòng**, không ném exception; và EL in ra chuỗi
  rỗng thay vì `NullPointerException`

> "Đó là lý do em luôn khởi tạo list rỗng ở Servlet chứ không để null — JSP sẽ hiển thị empty state
> ở dòng 336 thay vì trang lỗi."
</details>

<details>
<summary><b>Q15. Mô hình MVC của em: đâu là M, V, C?</b></summary>

Vẽ nhanh ra giấy/bảng:

```
Browser
   │  GET /project?action=list
   ▼
[C] ProjectServlet.java        ← controllers/  : nhận request, điều phối
   │  gọi
   ▼
[M] ProjectDB.java  ──────►  Project.java      ← data/ + business/
   │  trả List<Project>        (Entity/JavaBean)
   ▼
[C] request.setAttribute("myProjects", ...) ; forward
   │
   ▼
[V] projects.jsp               ← webapp/       : chỉ hiển thị, dùng JSTL + EL
   │
   ▼  HTML
Browser
```

| Tầng | Thư mục | Nhiệm vụ |
|---|---|---|
| **Model** | `business/` (JavaBean/Entity) + `data/` (DAO) | dữ liệu & truy xuất DB |
| **View** | `webapp/*.jsp` | chỉ trình bày |
| **Controller** | `controllers/` | nhận request, gọi Model, chọn View |

> "Điểm em muốn nhấn mạnh là **Servlet không chứa một dòng HTML nào**, và **JSP không gọi database**.
> Hai tầng chỉ giao tiếp qua attribute trong request."
</details>

---

## 5. ĐÃ DỌN SCRIPTLET — NHỮNG GÌ ĐÃ THAY ĐỔI

Trước đây `projects.jsp` có một khối scriptlet `<% ... %>` tính 3 chỉ số KPI ngay trong JSP.
**Đã chuyển về Servlet.** Cụ thể:

| File | Thay đổi |
|---|---|
| [ProjectServlet.java:142-157](../src/main/java/com/teamwork/controllers/ProjectServlet.java) | **Thêm** mục `// 2.` tính `kpiPmCount`, `kpiTotalTasks`, `kpiDoneTasks` rồi `setAttribute` |
| [ProjectServlet.java](../src/main/java/com/teamwork/controllers/ProjectServlet.java) | Đánh số lại các mục comment: 2→3, 3→4, 4→5 |
| [projects.jsp:51-52](../src/main/webapp/projects.jsp) | **Xoá** khối scriptlet 20 dòng, thay bằng comment `<%-- --%>` giải thích |

Tên 3 attribute **giữ nguyên**, nên 4 chỗ dùng trong JSP không phải sửa: dòng 63, 90, 205, 208.

### ✅ Bắt buộc kiểm tra sau khi `build_and_run.bat`

Mở trang Workspace và so 4 con số này với trước khi sửa — phải **giống hệt**:

- [ ] KPI "Dự án của bạn" → số trong ngoặc `(… làm Trưởng nhóm)` (dòng 63)
- [ ] KPI "Tiến độ chung" → `đã xong / tổng` (dòng 90)
- [ ] Nút filter "Tôi làm Trưởng nhóm (…)" (dòng 205)
- [ ] Nút filter "Tôi là Thành viên (…)" (dòng 208)

> Đã biên dịch thử toàn bộ 39 file `.java` — **không có lỗi compile**. Nhưng vẫn phải xem bằng mắt
> 4 con số trên, vì compile được không có nghĩa là logic đúng.

---

## 6. BẢNG TRA NHANH — DÁN LÊN MÀN HÌNH

| Cô hỏi | Mở file | Dòng |
|---|---|---|
| Servlet lấy Project ở đâu | `ProjectServlet.java` | **114** |
| Servlet đưa sang JSP thế nào | `ProjectServlet.java` | **138**, **188** |
| JSP dùng Project ở đâu | `projects.jsp` | **239**, **277** |
| Bằng chứng JSP biết kiểu Project | `projects.jsp` **302** + `Project.java` **97** | |
| Code hiển thị nhiều project | `projects.jsp` | **239** (`<c:forEach>`) |
| HTML của một thẻ dự án | `projects.jsp` | **240-323** |
| Form tạo dự án gửi đi đâu | `projects.jsp` | **485-486** |
| Servlet nhận form tạo dự án | `ProjectServlet.java` | **73** → **92** → **194** |
| Thuộc tính tính toán, không có trong DB | `Project.java` | **97** |
| Chống truy cập trái phép | `ProjectServlet.java` | **44-49** |
| Chống SQL Injection | `ProjectDB.java` | **132-135** |
| POST-Redirect-GET | `ProjectServlet.java` | **262** |
| KPI tính ở Controller (không còn scriptlet) | `ProjectServlet.java` | **142-157** |
| Map tra theo khoá trong EL | `projects.jsp` **289** + `ProjectServlet.java` **159-164** | |

---

## 7. BA CÂU "CỨU NGUY" KHI BÍ

1. **Cần thời gian suy nghĩ:**
   > "Dạ cô cho em mở file để chỉ chính xác cho cô ạ." *(vừa được điểm chủ động, vừa có thời gian)*

2. **Không biết câu trả lời:**
   > "Dạ chỗ này em chưa tìm hiểu sâu. Em hiểu tới mức [nói phần bạn thật sự biết]. Em sẽ đọc thêm ạ."
   >
   > **Đừng bao giờ bịa.** Giảng viên hỏi tiếp một câu là lộ ngay, và mất điểm nặng hơn nhiều so
   > với việc thừa nhận.

3. **Code chạy lỗi khi demo:**
   > "Dạ cô cho em một phút, em rollback về bản ổn định ạ."
   >
   > Rồi: `git checkout -- .` và `build_and_run.bat`. **Hãy commit trước khi vào phòng demo.**

---

## 8. CHECKLIST 5 PHÚT CUỐI TRƯỚC KHI VÀO

- [ ] `git status` sạch, đã commit (để rollback được nếu demo lỗi)
- [ ] Đã chạy `build_and_run.bat` sau khi dọn scriptlet, và **đã kiểm tra 4 con số KPI ở Mục 5**
- [ ] Tomcat đang chạy, **đã đăng nhập**, đang ở trang Workspace
- [ ] Đã test `hot_jsp.bat projects.jsp` thành công ít nhất 1 lần
- [ ] Database có **ít nhất 3 dự án** (để `<c:forEach>` thể hiện rõ là lặp nhiều lần)
- [ ] Có ít nhất 1 dự án bạn làm Trưởng nhóm + 1 dự án làm Thành viên (để badge ở dòng 257 khác nhau)
- [ ] Mở sẵn file này ở tab riêng
- [ ] Đã đọc lại Mục 2 (mạch 4 bước) và Mục 3 (`<c:forEach>`) — 2 câu chắc chắn bị hỏi
