# Day 3: Create / Read API実装（TypeScript/Express版）

## 学習目標

- Prisma Clientを使ったデータベースへの登録・取得処理を実装できる
- Expressのルーター分割を理解し、エンドポイントを構造的に整理できる
- curlまたはPowerShellでPOST/GETリクエストを送り、動作を確認できる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| ルーター作成 | 25分 |
| app.tsへの統合 | 10分 |
| 動作確認 | 25分 |

---

## 1. ルーターディレクトリの作成

```powershell
mkdir src/routes
```

---

## 2. 書籍ルーターの作成

**src/routes/books.ts** を新規作成する。

```typescript
import { Router, Request, Response } from "express";
import prisma from "../database";

const router = Router();

// 書籍一覧取得
router.get("/", async (req: Request, res: Response) => {
  const books = await prisma.book.findMany({
    orderBy: { id: "asc" },
  });
  res.json(books);
});

// 書籍個別取得
router.get("/:id", async (req: Request, res: Response) => {
  const id = parseInt(req.params.id);

  if (isNaN(id)) {
    res.status(400).json({ detail: "Invalid ID" });
    return;
  }

  const book = await prisma.book.findUnique({
    where: { id },
  });

  if (!book) {
    res.status(404).json({ detail: "Book not found" });
    return;
  }

  res.json(book);
});

// 書籍登録
router.post("/", async (req: Request, res: Response) => {
  const { title, author, publisher, publishedDate, isbn } = req.body;

  if (!title || !author) {
    res.status(422).json({
      detail: "title and author are required",
    });
    return;
  }

  const book = await prisma.book.create({
    data: {
      title,
      author,
      publisher: publisher || null,
      publishedDate: publishedDate || null,
      isbn: isbn || null,
    },
  });

  res.status(201).json(book);
});

export default router;
```

### コード解説

- `Router()`: Expressのルーター。関連するエンドポイントをグループ化する。FastAPIの `APIRouter` やSpring Bootの `@RequestMapping` に相当。
- `async / await`: 非同期処理。データベース操作は時間がかかるため、`await` で完了を待つ。Pythonの `async/await` と同じ概念。
- `prisma.book.findMany()`: `books` テーブルの全レコードを取得する。`SELECT * FROM books` に相当。
- `prisma.book.findUnique({ where: { id } })`: 主キーで1件取得する。該当なしの場合は `null` を返す。
- `prisma.book.create({ data: {...} })`: 新しいレコードを作成する。`INSERT INTO books ...` に相当。
- `parseInt(req.params.id)`: URLパスパラメータは文字列で渡されるため、数値に変換する。
- `isNaN(id)`: 変換結果が数値でない場合（例: `/books/abc`）、400エラーを返す。
- `req.body`: POSTリクエストのJSONボディ。`app.use(express.json())` により自動的にパースされている。
- 分割代入 `const { title, author, ... } = req.body`: オブジェクトからプロパティを個別の変数に取り出す構文。

---

## 3. app.tsの更新

**src/app.ts** を以下の内容で上書きする。

```typescript
import express, { Request, Response } from "express";
import booksRouter from "./routes/books";

const app = express();

app.use(express.json());

app.use("/books", booksRouter);

app.get("/health", (req: Request, res: Response) => {
  res.json({ status: "ok", message: "Book Manager API is running" });
});

app.get("/", (req: Request, res: Response) => {
  res.json({ message: "Welcome to Book Manager" });
});

export default app;
```

### コード解説

- `app.use("/books", booksRouter)`: `/books` パスに書籍ルーターをマウントする。ルーター内の `router.get("/")` は `/books/` として、`router.get("/:id")` は `/books/1` のようにアクセスできる。

---

## 4. 動作確認

サーバーを起動する。

```powershell
npm run dev
```

### 4-1. 書籍の登録（Create）

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books" -Method POST -ContentType "application/json" -Body '{"title": "リーダブルコード", "author": "Dustin Boswell", "publisher": "オライリージャパン"}'
```

curl が使える場合:

```powershell
curl -X POST http://localhost:8000/books -H "Content-Type: application/json" -d "{\"title\": \"リーダブルコード\", \"author\": \"Dustin Boswell\", \"publisher\": \"オライリージャパン\"}"
```

期待されるレスポンス（status_code: 201）:

```json
{
  "id": 1,
  "title": "リーダブルコード",
  "author": "Dustin Boswell",
  "publisher": "オライリージャパン",
  "publishedDate": null,
  "isbn": null,
  "createdAt": "2025-04-01T10:00:00.000Z",
  "updatedAt": "2025-04-01T10:00:00.000Z"
}
```

もう1冊登録する。

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books" -Method POST -ContentType "application/json" -Body '{"title": "プログラミング作法", "author": "Brian Kernighan", "publisher": "ASCII", "isbn": "9784756136497"}'
```

### 4-2. 書籍一覧の取得（Read - List）

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books"
```

登録した2冊が配列で返ってくることを確認する。

### 4-3. 書籍の個別取得（Read - Detail）

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/1"
```

### 4-4. 存在しないIDの取得

```powershell
try { Invoke-RestMethod -Uri "http://localhost:8000/books/999" } catch { $_.Exception.Response.StatusCode }
```

404が返ることを確認する。

---

## 5. 本日の確認ポイント

1. `POST /books` で書籍を登録でき、IDが自動採番される
2. `GET /books` で登録済みの全書籍が配列で返る
3. `GET /books/:id` で指定したIDの書籍が返る
4. 存在しないIDを指定すると404エラーが返る
5. Expressのルーター分割の仕組みを説明できる

---

## Day 3 完了時のファイル構成

```
book-manager/
├── node_modules/
├── prisma/
│   ├── schema.prisma
│   ├── dev.db
│   └── migrations/
├── src/
│   ├── app.ts             ← 更新
│   ├── server.ts
│   ├── database.ts
│   └── routes/
│       └── books.ts       ← 新規
├── .env
├── .gitignore
├── package.json
├── package-lock.json
└── tsconfig.json
```
