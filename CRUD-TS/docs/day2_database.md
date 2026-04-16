# Day 2: データベース設計・モデル作成（TypeScript/Express版）

## 学習目標

- Prisma ORMのセットアップとスキーマ定義を理解する
- マイグレーションによるテーブル作成の仕組みを理解する
- Prisma Clientを使ったデータベース接続を確認できる

## 所要時間の目安

| 作業                           | 時間 |
| ------------------------------ | ---- |
| Prismaセットアップ             | 15分 |
| スキーマ定義・マイグレーション | 20分 |
| Prisma Client生成・動作確認    | 25分 |

---

## 1. Prismaのインストール

```powershell
npm install @prisma/client@6.19.3
npm install -D prisma@6.19.3
```

- `@prisma/client`: アプリケーションコードからデータベースを操作するためのクライアントライブラリ。
- `prisma`: スキーマ定義、マイグレーション、コード生成などの開発ツール。
- 本資料では Prisma 6.19 系を使用する。Prisma 7 系では設定方法が変わるため、研修ではバージョンを固定して進める。

---

## 2. Prismaの初期化

```powershell
npx prisma init --datasource-provider sqlite
```

このコマンドにより以下が生成される。

- `prisma/schema.prisma`: データベースのスキーマ定義ファイル
- `.env`: 環境変数ファイル（データベース接続先）

### Prismaとは

Prismaは、TypeScript/JavaScript向けのモダンなORM（Object-Relational Mapping）。
従来のORMと異なり、スキーマファイル（`schema.prisma`）でデータモデルを定義すると、型安全なクライアントコードが自動生成される点が特徴。SQLAlchemy（Python）やJPA（Java）に相当するが、TypeScriptの型システムとの統合度が非常に高い。

---

## 3. スキーマの定義

**prisma/schema.prisma** を以下の内容で上書きする。

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "sqlite"
  url      = env("DATABASE_URL")
}

model Book {
  id            Int       @id @default(autoincrement())
  title         String
  author        String
  publisher     String?
  publishedDate String?   @map("published_date")
  isbn          String?
  createdAt     DateTime  @default(now()) @map("created_at")
  updatedAt     DateTime  @updatedAt @map("updated_at")

  @@map("books")
}
```

### コード解説

- `generator client`: Prisma Clientのコード生成設定。`prisma generate` 実行時にTypeScriptの型定義付きクライアントが生成される。
- `datasource db`: データベースの接続先設定。`env("DATABASE_URL")` は `.env` ファイルから環境変数を読み込む。
- `model Book`: データベーステーブルに対応するモデル定義。
- `@id`: 主キーを示す。
- `@default(autoincrement())`: 自動採番。
- `String?`: `?` はNULL許容を意味する。`?` がないフィールドは必須（NOT NULL）。
- `@map("published_date")`: TypeScriptのキャメルケース（`publishedDate`）とデータベースのスネークケース（`published_date`）を対応付ける。
- `@default(now())`: レコード作成時に現在時刻を自動設定する。
- `@updatedAt`: レコード更新時にPrismaが自動的に現在時刻を設定する。
- `@@map("books")`: テーブル名を `books` に設定する。

---

## 4. 環境変数ファイルの確認

自動生成された `.env` ファイルを確認する。以下の内容になっているはず。

**.env**

```
DATABASE_URL="file:./dev.db"
```

`file:./dev.db` は「`prisma/` ディレクトリに `dev.db` というSQLiteファイルを作成/使用する」という意味。

### .gitignoreの作成

**.gitignore** をプロジェクトルートに作成する。

```
node_modules/
dist/
*.db
.env
```

---

## 5. マイグレーションの実行

マイグレーションとは、スキーマの変更をデータベースに反映する仕組みである。スキーマを変更するたびにマイグレーションを実行し、変更履歴を管理する。

```powershell
npx prisma migrate dev --name init
```

### コマンドの意味

- `migrate dev`: 開発環境用のマイグレーションを実行する。
- `--name init`: マイグレーション名。変更内容を示す名前を付ける。

実行すると以下が行われる。

1. `prisma/migrations/` ディレクトリにマイグレーションファイル（SQL）が生成される
2. SQLiteデータベースファイル `prisma/dev.db` が作成される
3. `books` テーブルが作成される
4. Prisma Clientのコードが再生成される

---

## 6. Prisma Clientのセットアップ

アプリケーションからPrisma Clientを使うための設定ファイルを作成する。

**src/database.ts** を新規作成する。

```typescript
import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

export default prisma;
```

### コード解説

- `PrismaClient`: Prismaが自動生成する型安全なデータベースクライアント。`prisma.book.findMany()` のようにメソッドチェーンでデータベース操作を行う。
- シングルトンパターン: `prisma` インスタンスを1つだけ作成し、アプリケーション全体で共有する。

---

## 7. Prisma Studioで確認

PrismaにはデータベースのGUI管理ツール「Prisma Studio」が付属している。

```powershell
npx prisma studio
```

ブラウザで http://localhost:5555 が自動的に開き、`Book` テーブルの中身を確認・編集できる。まだデータは0件だが、テーブルが作成されていることを確認する。

`Ctrl+C` でPrisma Studioを停止する。

---

## 8. 型定義の確認

Prismaが生成した型定義を確認してみる。TypeScriptの最大の利点は型安全性であり、Prismaはその恩恵を最大限に活かせるORMである。

`src/app.ts` を開き、以下のimportを一時的に追加して、エディタの補完を試してみる。

```typescript
import { Book } from "@prisma/client";
```

`Book` 型にマウスカーソルを合わせると、`schema.prisma` で定義したフィールドが全て型として表示される。これがPrismaの型安全性の核心であり、フィールド名のタイプミスやデータ型の不一致をコンパイル時に検出できる。

確認が終わったらこのimportは削除して良い（Day 3で正式に使用する）。

---

## 9. 本日の確認ポイント

1. `prisma/schema.prisma` に `Book` モデルが定義されている
2. `npx prisma migrate dev` でマイグレーションが成功し、`prisma/dev.db` が生成される
3. `npx prisma studio` でテーブル構造を確認できる
4. `src/database.ts` でPrismaClientのインスタンスが作成されている
5. ORMモデル（schema.prisma）とマイグレーションの関係を説明できる

---

## Day 2 完了時のファイル構成

```
book-manager/
├── node_modules/
├── prisma/
│   ├── schema.prisma      # スキーマ定義
│   ├── dev.db             # SQLiteデータベース（自動生成）
│   └── migrations/        # マイグレーション履歴（自動生成）
├── src/
│   ├── app.ts
│   ├── server.ts
│   └── database.ts        ← 新規
├── .env                   ← 自動生成
├── .gitignore             ← 新規
├── package.json
├── package-lock.json
└── tsconfig.json
```

---

## 自習課題（余裕がある場合）

- Prisma公式ドキュメント（https://www.prisma.io/docs）の「Quickstart」を読む
- `schema.prisma` にフィールドを追加して `npx prisma migrate dev --name add_xxx` を実行してみる
- Prisma Studioからデータを手動で追加して、データベースの動作を確認する
