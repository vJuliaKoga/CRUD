# Day 5: 簡易HTML画面作成（Python/FastAPI版）

## 学習目標

- Jinja2テンプレートエンジンを使ったサーバーサイドレンダリングを理解する
- HTML + JavaScript（fetch API）でCRUD操作を行うフロントエンドを作成できる
- 静的ファイル（CSS）の配信設定を理解する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| テンプレート・静的ファイル設定 | 10分 |
| HTMLテンプレート作成 | 30分 |
| 画面用ルーター・動作確認 | 20分 |

---

## 1. 依存パッケージの追加

**requirements.txt** に以下を追加する。

```
fastapi==0.115.0
uvicorn==0.30.0
sqlalchemy==2.0.35
jinja2==3.1.4
python-multipart==0.0.12
```

インストールを実行する。

```bash
pip install -r requirements.txt
```

- `jinja2`: HTMLテンプレートエンジン。テンプレート内にPythonの変数やループを埋め込める。
- `python-multipart`: HTMLフォームからのデータ送信を処理するために必要。

---

## 2. ディレクトリの作成

```bash
mkdir templates static
```

---

## 3. CSSファイルの作成

**static/style.css** を新規作成する。

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

## 4. HTMLテンプレートの作成

**templates/index.html** を新規作成する。このファイルが最も長い。焦らず正確に入力すること。

```html
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>書籍管理システム</title>
    <link rel="stylesheet" href="/static/style.css">
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
                <input type="text" id="published_date" placeholder="出版日（YYYY-MM-DD）">
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
            const res = await fetch(API_BASE + "/");
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
                    <td>${escapeHtml(book.published_date || "")}</td>
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
                published_date: document.getElementById("published_date").value || null,
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
                res = await fetch(API_BASE + "/", {
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
                showMessage("エラー: " + JSON.stringify(err.detail), "error");
            }
        }

        async function editBook(id) {
            const res = await fetch(`${API_BASE}/${id}`);
            const book = await res.json();

            document.getElementById("edit-id").value = book.id;
            document.getElementById("title").value = book.title;
            document.getElementById("author").value = book.author;
            document.getElementById("publisher").value = book.publisher || "";
            document.getElementById("published_date").value = book.published_date || "";
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
            document.getElementById("published_date").value = "";
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

### コード解説（HTML/JavaScript部分）

- `fetch(API_BASE + "/")`: JavaScriptのFetch APIを使って、先日作成したREST APIにリクエストを送る。
- `escapeHtml()`: ユーザーの入力値をHTMLに表示する際、XSS（Cross-Site Scripting）攻撃を防ぐためにHTMLエスケープを行う関数。
- `confirm("...")`: ブラウザの確認ダイアログを表示する。「OK」を押した場合のみ処理を続行する。
- `loadBooks()`: ページ読み込み時に書籍一覧を取得してテーブルに表示する。

---

## 5. main.pyの更新

テンプレートと静的ファイルの配信設定を追加する。

**main.py** を以下の内容で上書きする。

```python
from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from database import engine
from models import Base
from routers import books

Base.metadata.create_all(bind=engine)

app = FastAPI(title="書籍管理システム", version="1.0.0")

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

app.include_router(books.router)


@app.get("/health")
def health_check():
    return {"status": "ok", "message": "Book Manager API is running"}


@app.get("/")
def root(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})
```

### コード解説

- `app.mount("/static", ...)`: `/static` というURLパスに `static/` ディレクトリの中身を配信する設定。CSSやJavaScriptファイルをブラウザが読み込めるようになる。
- `Jinja2Templates(directory="templates")`: テンプレートファイルの格納先ディレクトリを指定。
- `templates.TemplateResponse(...)`: テンプレートファイルをレンダリングしてHTMLレスポンスを返す。`request` はFastAPIの仕様上必須の引数。

---

## 6. 動作確認

```bash
uvicorn main:app --reload --port 8000
```

ブラウザで http://localhost:8000/ にアクセスする。

以下の操作が全てブラウザ上でできることを確認する。

1. フォームに書籍情報を入力して「保存」をクリック → テーブルに追加される
2. テーブルの「編集」ボタンをクリック → フォームに値が入り、「書籍を編集する」に切り替わる
3. 編集フォームで内容を変更して「保存」→ テーブルが更新される
4. 「キャンセル」をクリック → フォームが初期状態に戻る
5. 「削除」ボタンをクリック → 確認ダイアログ後に削除される

---

## 7. 本日の確認ポイント

1. ブラウザで http://localhost:8000/ にアクセスすると書籍管理画面が表示される
2. 画面から書籍の登録・一覧表示・編集・削除が全てできる
3. CSSが適用されて見た目が整っている
4. バリデーションエラー時にメッセージが画面に表示される

---

## Day 5 完了時のファイル構成

```
book-manager/
├── venv/
├── main.py              ← 更新
├── database.py
├── models.py
├── schemas.py
├── crud.py
├── routers/
│   ├── __init__.py
│   └── books.py
├── templates/
│   └── index.html       ← 新規
├── static/
│   └── style.css        ← 新規
├── books.db
└── requirements.txt     ← 更新
```

Phase 1（CRUDシステム開発）はこれで完了である。Day 6からはPhase 2に入り、テストとデプロイ準備を行う。
