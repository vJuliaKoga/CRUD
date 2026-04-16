# Day 4: Update / Delete API実装（TypeScript/Express版）

## 学習目標

- 書籍の更新処理と削除処理を実装できる
- リクエストバリデーション（入力値検証）を体系的に追加できる
- CRUDの全4操作が揃い、APIとして完成する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| バリデーション関数の作成 | 15分 |
| Update/Deleteエンドポイント追加 | 20分 |
| エラーハンドリング追加 | 10分 |
| 動作確認 | 15分 |

---

## 1. バリデーション関数の作成

入力値の検証を行うユーティリティを作成する。

```powershell
mkdir src/utils
```

**src/utils/validation.ts** を新規作成する。

```typescript
export interface ValidationError {
  field: string;
  message: string;
}

export function validateBookCreate(body: any): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!body.title || typeof body.title !== "string" || body.title.trim() === "") {
    errors.push({ field: "title", message: "タイトルは必須です" });
  } else if (body.title.length > 200) {
    errors.push({ field: "title", message: "タイトルは200文字以内で入力してください" });
  }

  if (!body.author || typeof body.author !== "string" || body.author.trim() === "") {
    errors.push({ field: "author", message: "著者は必須です" });
  } else if (body.author.length > 100) {
    errors.push({ field: "author", message: "著者は100文字以内で入力してください" });
  }

  if (body.publisher && body.publisher.length > 100) {
    errors.push({ field: "publisher", message: "出版社は100文字以内で入力してください" });
  }

  if (body.publishedDate && !/^\d{4}-\d{2}-\d{2}$/.test(body.publishedDate)) {
    errors.push({ field: "publishedDate", message: "日付はYYYY-MM-DD形式で入力してください" });
  }

  if (body.isbn && !/^\d{10}(\d{3})?$/.test(body.isbn)) {
    errors.push({ field: "isbn", message: "ISBNは10桁または13桁の数字で入力してください" });
  }

  return errors;
}

export function validateBookUpdate(body: any): ValidationError[] {
  const errors: ValidationError[] = [];

  if (body.title !== undefined) {
    if (typeof body.title !== "string" || body.title.trim() === "") {
      errors.push({ field: "title", message: "タイトルは空にできません" });
    } else if (body.title.length > 200) {
      errors.push({ field: "title", message: "タイトルは200文字以内で入力してください" });
    }
  }

  if (body.author !== undefined) {
    if (typeof body.author !== "string" || body.author.trim() === "") {
      errors.push({ field: "author", message: "著者は空にできません" });
    } else if (body.author.length > 100) {
      errors.push({ field: "author", message: "著者は100文字以内で入力してください" });
    }
  }

  if (body.publisher !== undefined && body.publisher && body.publisher.length > 100) {
    errors.push({ field: "publisher", message: "出版社は100文字以内で入力してください" });
  }

  if (body.publishedDate !== undefined && body.publishedDate && !/^\d{4}-\d{2}-\d{2}$/.test(body.publishedDate)) {
    errors.push({ field: "publishedDate", message: "日付はYYYY-MM-DD形式で入力してください" });
  }

  if (body.isbn !== undefined && body.isbn && !/^\d{10}(\d{3})?$/.test(body.isbn)) {
    errors.push({ field: "isbn", message: "ISBNは10桁または13桁の数字で入力してください" });
  }

  return errors;
}
```

### コード解説

- `interface ValidationError`: TypeScriptのインターフェース。バリデーションエラーの型を定義する。
- `validateBookCreate` と `validateBookUpdate` の違い: Create時は `title` と `author` が必須だが、Update時は送信されたフィールドのみを検証する（`body.title !== undefined` で存在チェック）。
- 正規表現: `/^\d{4}-\d{2}-\d{2}$/` はYYYY-MM-DD形式、`/^\d{10}(\d{3})?$/` は10桁または13桁の数字にマッチする。

---

## 2. ルーターの更新

**src/routes/books.ts** を以下の内容で上書きする。Update/Deleteエンドポイントとバリデーション呼び出しを追加する。

```typescript
import { Router, Request, Response } from "express";
import prisma from "../database";
import { validateBookCreate, validateBookUpdate } from "../utils/validation";

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
  const errors = validateBookCreate(req.body);
  if (errors.length > 0) {
    res.status(422).json({ status: 422, error: "Validation Error", details: errors });
    return;
  }

  const { title, author, publisher, publishedDate, isbn } = req.body;

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

// 書籍更新
router.put("/:id", async (req: Request, res: Response) => {
  const id = parseInt(req.params.id);

  if (isNaN(id)) {
    res.status(400).json({ detail: "Invalid ID" });
    return;
  }

  const errors = validateBookUpdate(req.body);
  if (errors.length > 0) {
    res.status(422).json({ status: 422, error: "Validation Error", details: errors });
    return;
  }

  const existing = await prisma.book.findUnique({ where: { id } });
  if (!existing) {
    res.status(404).json({ detail: "Book not found" });
    return;
  }

  const updateData: any = {};
  if (req.body.title !== undefined) updateData.title = req.body.title;
  if (req.body.author !== undefined) updateData.author = req.body.author;
  if (req.body.publisher !== undefined) updateData.publisher = req.body.publisher || null;
  if (req.body.publishedDate !== undefined) updateData.publishedDate = req.body.publishedDate || null;
  if (req.body.isbn !== undefined) updateData.isbn = req.body.isbn || null;

  const book = await prisma.book.update({
    where: { id },
    data: updateData,
  });

  res.json(book);
});

// 書籍削除
router.delete("/:id", async (req: Request, res: Response) => {
  const id = parseInt(req.params.id);

  if (isNaN(id)) {
    res.status(400).json({ detail: "Invalid ID" });
    return;
  }

  const existing = await prisma.book.findUnique({ where: { id } });
  if (!existing) {
    res.status(404).json({ detail: "Book not found" });
    return;
  }

  await prisma.book.delete({ where: { id } });

  res.status(204).send();
});

export default router;
```

### コード解説

- `prisma.book.update({ where: { id }, data: updateData })`: 指定したIDのレコードを更新する。`data` に含まれるフィールドのみが更新される。
- `updateData` の構築: `req.body` に含まれるフィールドのみを `updateData` に追加することで部分更新を実現する。
- `prisma.book.delete({ where: { id } })`: 指定したIDのレコードを削除する。
- `res.status(204).send()`: ステータスコード204（No Content）を返す。ボディなしのレスポンスには `send()` を使う。

---

## 3. グローバルエラーハンドリングの追加

予期しないエラー（データベース接続エラー等）をキャッチするミドルウェアを追加する。

**src/app.ts** を以下の内容で上書きする。

```typescript
import express, { Request, Response, NextFunction } from "express";
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

// グローバルエラーハンドラ
app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
  console.error("Unexpected error:", err);
  res.status(500).json({ detail: "Internal Server Error" });
});

export default app;
```

### コード解説

- 引数が4つのミドルウェア関数は、Expressがエラーハンドラとして認識する。通常のリクエスト処理で例外が発生した場合にこのハンドラが呼ばれる。

---

## 4. 動作確認

```powershell
npm run dev
```

### 4-1. 書籍の更新（Update）

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/1" -Method PUT -ContentType "application/json" -Body '{"title": "リーダブルコード 改訂版"}'
```

`title` のみが更新され、他のフィールドは変わらないことを確認する。

### 4-2. 書籍の削除（Delete）

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/2" -Method DELETE
```

### 4-3. バリデーションエラーの確認

```powershell
try {
  Invoke-RestMethod -Uri "http://localhost:8000/books" -Method POST -ContentType "application/json" -Body '{"title": "", "author": "テスト"}'
} catch {
  $_.ErrorDetails.Message
}
```

422エラーとバリデーションエラーの詳細が返ることを確認する。

---

## 5. 本日の確認ポイント

1. `PUT /books/:id` で書籍を部分更新できる
2. `DELETE /books/:id` で書籍を削除でき、204が返る
3. バリデーションエラー時にJSON形式でエラー詳細が返る
4. CRUD全4操作が動作確認できる

---

## Day 4 完了時のファイル構成

```
book-manager/
├── src/
│   ├── app.ts              ← 更新（エラーハンドラ追加）
│   ├── server.ts
│   ├── database.ts
│   ├── routes/
│   │   └── books.ts        ← 更新（PUT/DELETE追加）
│   └── utils/
│       └── validation.ts   ← 新規
├── prisma/
│   ├── schema.prisma
│   ├── dev.db
│   └── migrations/
└── ...
```
