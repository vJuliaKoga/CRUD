# Day 5: 簡易HTML画面作成（Java/Spring Boot版）

## 学習目標

- Thymeleafテンプレートエンジンの基本的な使い方を理解する
- HTML + JavaScript（fetch API）でCRUD操作を行うフロントエンドを作成できる
- 画面用コントローラとAPI用コントローラの分離を理解する

## 所要時間の目安

| 作業                   | 時間 |
| ---------------------- | ---- |
| CSS作成                | 5分  |
| HTMLテンプレート作成   | 35分 |
| 画面用コントローラ作成 | 10分 |
| 動作確認               | 10分 |

---

## 1. CSSファイルの作成

**src/main/resources/static/style.css** を新規作成する。

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Helvetica Neue", Arial, "Hiragino Sans", sans-serif;
  background-color: #f5f5f5;
  color: #333;
  line-height: 1.6;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #2c3e50;
}

.form-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.form-section h2 {
  margin-bottom: 15px;
  font-size: 1.2em;
}

.form-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.form-row input {
  flex: 1;
  min-width: 150px;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #fff;
}

.btn-primary {
  background-color: #3498db;
}
.btn-primary:hover {
  background-color: #2980b9;
}
.btn-danger {
  background-color: #e74c3c;
}
.btn-danger:hover {
  background-color: #c0392b;
}
.btn-warning {
  background-color: #f39c12;
}
.btn-warning:hover {
  background-color: #e67e22;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

th,
td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  background-color: #2c3e50;
  color: #fff;
}

tr:hover {
  background-color: #f0f0f0;
}

.actions {
  display: flex;
  gap: 5px;
}

.message {
  padding: 10px 15px;
  margin-bottom: 15px;
  border-radius: 4px;
  display: none;
}

.message.success {
  background-color: #d4edda;
  color: #155724;
}
.message.error {
  background-color: #f8d7da;
  color: #721c24;
}
```

---

## 2. HTMLテンプレートの作成

**src/main/resources/templates/index.html** を新規作成する。

Thymeleafテンプレートだが、今回はJavaScriptで動的にAPIを呼び出す方式のため、Thymeleaf固有の記法は最小限に留めている。

```html
<!DOCTYPE html>
<html lang="ja" xmlns:th="http://www.thymeleaf.org">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>書籍管理システム</title>
    <link rel="stylesheet" href="/style.css" />
  </head>
  <body>
    <div class="container">
      <h1>書籍管理システム</h1>

      <div id="message" class="message"></div>

      <div class="form-section">
        <h2 id="form-title">書籍を登録する</h2>
        <input type="hidden" id="edit-id" />
        <div class="form-row">
          <input type="text" id="title" placeholder="タイトル（必須）" />
          <input type="text" id="author" placeholder="著者（必須）" />
        </div>
        <div class="form-row">
          <input type="text" id="publisher" placeholder="出版社" />
          <input
            type="text"
            id="publishedDate"
            placeholder="出版日（YYYY-MM-DD）"
          />
          <input type="text" id="isbn" placeholder="ISBN" />
        </div>
        <div class="form-row">
          <button class="btn btn-primary" onclick="saveBook()">保存</button>
          <button
            class="btn btn-warning"
            onclick="cancelEdit()"
            id="cancel-btn"
            style="display:none;"
          >
            キャンセル
          </button>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>タイトル</th>
            <th>著者</th>
            <th>出版社</th>
            <th>出版日</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody id="book-list"></tbody>
      </table>
    </div>

    <script>
      const API_BASE = "/books";

      async function loadBooks() {
        const res = await fetch(API_BASE);
        const books = await res.json();
        const tbody = document.getElementById("book-list");
        tbody.innerHTML = "";

        books.forEach((book) => {
          const tr = document.createElement("tr");
          tr.innerHTML = `
                    <td>${book.id}</td>
                    <td>${escapeHtml(book.title)}</td>
                    <td>${escapeHtml(book.author)}</td>
                    <td>${escapeHtml(book.publisher || "")}</td>
                    <td>${escapeHtml(book.publishedDate || "")}</td>
                    <td class="actions">
                        <button class="btn btn-primary" onclick="editBook(${book.id})">編集</button>
                        <button class="btn btn-danger" onclick="deleteBook(${book.id})">削除</button>
                    </td>
                `;
          tbody.appendChild(tr);
        });
      }

      async function saveBook() {
        const editId = document.getElementById("edit-id").value;
        const data = {
          title: document.getElementById("title").value,
          author: document.getElementById("author").value,
          publisher: document.getElementById("publisher").value || null,
          publishedDate: document.getElementById("publishedDate").value || null,
          isbn: document.getElementById("isbn").value || null,
        };

        let res;
        if (editId) {
          res = await fetch(`${API_BASE}/${editId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
          });
        } else {
          res = await fetch(API_BASE, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
          });
        }

        if (res.ok) {
          showMessage(editId ? "更新しました" : "登録しました", "success");
          clearForm();
          loadBooks();
        } else {
          const err = await res.json();
          const details = err.details
            ? Object.values(err.details).join(", ")
            : JSON.stringify(err);
          showMessage("エラー: " + details, "error");
        }
      }

      async function editBook(id) {
        const res = await fetch(`${API_BASE}/${id}`);
        const book = await res.json();

        document.getElementById("edit-id").value = book.id;
        document.getElementById("title").value = book.title;
        document.getElementById("author").value = book.author;
        document.getElementById("publisher").value = book.publisher || "";
        document.getElementById("publishedDate").value =
          book.publishedDate || "";
        document.getElementById("isbn").value = book.isbn || "";

        document.getElementById("form-title").textContent = "書籍を編集する";
        document.getElementById("cancel-btn").style.display = "inline-block";
      }

      async function deleteBook(id) {
        if (!confirm("この書籍を削除しますか？")) return;

        const res = await fetch(`${API_BASE}/${id}`, { method: "DELETE" });
        if (res.ok) {
          showMessage("削除しました", "success");
          loadBooks();
        } else {
          showMessage("削除に失敗しました", "error");
        }
      }

      function clearForm() {
        document.getElementById("edit-id").value = "";
        document.getElementById("title").value = "";
        document.getElementById("author").value = "";
        document.getElementById("publisher").value = "";
        document.getElementById("publishedDate").value = "";
        document.getElementById("isbn").value = "";
        document.getElementById("form-title").textContent = "書籍を登録する";
        document.getElementById("cancel-btn").style.display = "none";
      }

      function cancelEdit() {
        clearForm();
      }

      function showMessage(text, type) {
        const el = document.getElementById("message");
        el.textContent = text;
        el.className = "message " + type;
        el.style.display = "block";
        setTimeout(() => {
          el.style.display = "none";
        }, 3000);
      }

      function escapeHtml(str) {
        const div = document.createElement("div");
        div.textContent = str;
        return div.innerHTML;
      }

      loadBooks();
    </script>
  </body>
</html>
```

JavaScript部分はPython版とほぼ同一。
異なる点はフィールド名がJavaのキャメルケース（`publishedDate`）になっていること。

---

## 3. 画面用コントローラの作成

**src/main/java/com/training/bookmanager/controller/BookViewController.java** を新規作成する。

```java
package com.training.bookmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BookViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
```

### コード解説

- `@Controller`: `@RestController` ではなくて `@Controller` を使う。`@RestController` は戻り値をJSONとして返すが、`@Controller` は戻り値をテンプレート名として解釈し、対応するHTMLファイルをレンダリングして返す。
- `return "index"`: `src/main/resources/templates/index.html` を返す。拡張子 `.html` は不要。

---

## 4. HealthControllerのルートパスを削除

`HealthController.java` の `root()` メソッドと `BookViewController` の `index()` メソッドが同じ `/` パスで競合するため、`HealthController.java` から `root()` メソッドを削除する。

`HealthController.java` を以下の内容にする。

```java
package com.training.bookmanager.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
            "status", "ok",
            "message", "Book Manager API is running"
        );
    }
}
```

---

## 5. 動作確認

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

ブラウザで http://localhost:8000/ にアクセスし、Python版と同じ操作が全てできることを確認する。

1. 書籍の登録
2. 一覧表示
3. 編集
4. 削除
5. バリデーションエラー表示

---

## 6. 本日の確認ポイント

1. ブラウザで http://localhost:8000/ にアクセスすると書籍管理画面が表示される
2. 画面からCRUD操作が全てできる
3. `@Controller` と `@RestController` の違いを説明できる

---

## Day 5 完了時のファイル構成

```
book-manager/
├── src/main/java/com/training/bookmanager/
│   ├── controller/
│   │   ├── HealthController.java         ← 更新
│   │   ├── BookApiController.java
│   │   ├── BookViewController.java       ← 新規
│   │   └── GlobalExceptionHandler.java
│   ├── ...
├── src/main/resources/
│   ├── templates/
│   │   └── index.html                    ← 新規
│   ├── static/
│   │   └── style.css                     ← 新規
│   └── application.properties
└── ...
```

Phase 1（CRUDシステム開発）はこれで完了。
Day 6からはPhase 2に入り、テストとデプロイ準備を行う。
