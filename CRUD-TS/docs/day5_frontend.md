# Day 5: 簡易HTML画面作成（TypeScript/Express版）

## 学習目標

- Expressの静的ファイル配信機能を使ってHTMLとCSSを提供できる
- HTML + JavaScript（fetch API）でCRUD操作を行うフロントエンドを作成できる
- フロントエンドとバックエンドAPIの連携を理解する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| 静的ファイル配信設定 | 5分 |
| CSS作成 | 5分 |
| HTML作成 | 30分 |
| app.ts更新・動作確認 | 20分 |

---

## 1. 静的ファイルディレクトリの作成

```powershell
mkdir src/public
```

---

## 2. CSSファイルの作成

**src/public/style.css** を新規作成する。

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

.btn-primary { background-color: #3498db; }
.btn-primary:hover { background-color: #2980b9; }
.btn-danger { background-color: #e74c3c; }
.btn-danger:hover { background-color: #c0392b; }
.btn-warning { background-color: #f39c12; }
.btn-warning:hover { background-color: #e67e22; }

table {
    width: 100%;
    border-collapse: collapse;
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

th, td {
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

.message.success { background-color: #d4edda; color: #155724; }
.message.error { background-color: #f8d7da; color: #721c24; }
```

---

## 3. HTMLファイルの作成

**src/public/index.html** を新規作成する。

```html
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>書籍管理システム</title>
    <link rel="stylesheet" href="/style.css">
</head>
<body>
    <div class="container">
        <h1>書籍管理システム</h1>

        <div id="message" class="message"></div>

        <div class="form-section">
            <h2 id="form-title">書籍を登録する</h2>
            <input type="hidden" id="edit-id">
            <div class="form-row">
                <input type="text" id="title" placeholder="タイトル（必須）">
                <input type="text" id="author" placeholder="著者（必須）">
            </div>
            <div class="form-row">
                <input type="text" id="publisher" placeholder="出版社">
                <input type="text" id="publishedDate" placeholder="出版日（YYYY-MM-DD）">
                <input type="text" id="isbn" placeholder="ISBN">
            </div>
            <div class="form-row">
                <button class="btn btn-primary" onclick="saveBook()">保存</button>
                <button class="btn btn-warning" onclick="cancelEdit()" id="cancel-btn" style="display:none;">キャンセル</button>
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
            <tbody id="book-list">
            </tbody>
        </table>
    </div>

    <script>
        const API_BASE = "/books";

        async function loadBooks() {
            const res = await fetch(API_BASE);
            const books = await res.json();
            const tbody = document.getElementById("book-list");
            tbody.innerHTML = "";

            books.forEach(book => {
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
                    ? err.details.map(d => d.message).join(", ")
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
            document.getElementById("publishedDate").value = book.publishedDate || "";
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
            setTimeout(() => { el.style.display = "none"; }, 3000);
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

---

## 4. app.tsの更新

静的ファイル配信とHTMLページのルーティングを追加する。

**src/app.ts** を以下の内容で上書きする。

```typescript
import express, { Request, Response, NextFunction } from "express";
import path from "path";
import booksRouter from "./routes/books";

const app = express();

app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

app.use("/books", booksRouter);

app.get("/health", (req: Request, res: Response) => {
  res.json({ status: "ok", message: "Book Manager API is running" });
});

app.get("/", (req: Request, res: Response) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

// グローバルエラーハンドラ
app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
  console.error("Unexpected error:", err);
  res.status(500).json({ detail: "Internal Server Error" });
});

export default app;
```

### コード解説

- `express.static(path.join(__dirname, "public"))`: `src/public/` ディレクトリの中身を静的ファイルとして配信する。CSSやJavaScriptファイルがブラウザから読み込めるようになる。
- `path.join(__dirname, "public")`: `__dirname` は現在のファイルのディレクトリパス。`path.join` で安全にパスを結合する。
- `res.sendFile(...)`: HTMLファイルを直接レスポンスとして返す。テンプレートエンジンを使わないシンプルな方式。

---

## 5. 動作確認

```powershell
npm run dev
```

ブラウザで http://localhost:8000/ にアクセスする。

以下の操作が全てブラウザ上でできることを確認する。

1. フォームに書籍情報を入力して「保存」→ テーブルに追加される
2. 「編集」ボタンをクリック → フォームに値が入る
3. 編集して「保存」→ テーブルが更新される
4. 「キャンセル」→ フォームが初期状態に戻る
5. 「削除」→ 確認後に削除される

---

## 6. 本日の確認ポイント

1. ブラウザで http://localhost:8000/ にアクセスすると書籍管理画面が表示される
2. 画面からCRUD操作が全てできる
3. CSSが適用されて見た目が整っている
4. バリデーションエラー時にメッセージが画面に表示される

---

## Day 5 完了時のファイル構成

```
book-manager/
├── src/
│   ├── app.ts              ← 更新
│   ├── server.ts
│   ├── database.ts
│   ├── routes/
│   │   └── books.ts
│   ├── utils/
│   │   └── validation.ts
│   └── public/
│       ├── index.html      ← 新規
│       └── style.css       ← 新規
├── prisma/
│   ├── schema.prisma
│   ├── dev.db
│   └── migrations/
└── ...
```

Phase 1（CRUDシステム開発）はこれで完了である。Day 6からはPhase 2に入り、テストとデプロイ準備を行う。
