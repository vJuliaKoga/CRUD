# Day 7: PostgreSQL切替・本番準備（TypeScript/Express版）

## 学習目標

- SQLiteからPostgreSQLへのデータベース切替手順を理解する
- Prismaのマルチデータソース設定を実践できる
- 本番デプロイに向けたビルドと構成変更を行える

## 所要時間の目安

| 作業                                 | 時間 |
| ------------------------------------ | ---- |
| PostgreSQLセットアップ               | 20分 |
| Prismaスキーマ・マイグレーション変更 | 15分 |
| 動作確認・ビルド                     | 15分 |
| テスト確認                           | 10分 |

---

## 1. PostgreSQLのセットアップ

### インストール（未インストールの場合）

Windows: PostgreSQLの公式サイト（https://www.postgresql.org/download/windows/）からインストーラをダウンロードしてインストールする。
インストール時に設定するパスワードを控えておくこと。

### データベースとユーザーの作成

1. Windows のスタートメニューで SQL Shell (psql) を起動。

```shell
Server [localhost]:
Database [postgres]:
Port [5432]:
Username [postgres]:
Client Encoding [SJIS]:
ユーザー postgres のパスワード:[インストール時に設定したパスワードを入力]
```

postgres=#
が表示されたら、以下のSQL文を1行ずつ実行する。

```sql
CREATE USER bookuser WITH PASSWORD 'bookpass123';
CREATE DATABASE bookmanager OWNER bookuser;
GRANT ALL PRIVILEGES ON DATABASE bookmanager TO bookuser;
\q
```

2. 環境変数を設定

Windowsのユーザー環境変数に

```
PATH：C:\Program Files\PostgreSQL\18\bin
```

を登録する。

3. PowerShellを開き、接続確認

```powershell
psql -U bookuser -d bookmanager -h localhost
```

パスワード `bookpass123` を入力して接続できたら `\q` で抜ける。

---

## 2. Prismaスキーマの変更

**prisma/schema.prisma** のデータソース部分を以下のように変更する。

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

model Book {
  id            Int       @id @default(autoincrement())
  title         String    @db.VarChar(200)
  author        String    @db.VarChar(100)
  publisher     String?   @db.VarChar(100)
  publishedDate String?   @map("published_date") @db.VarChar(10)
  isbn          String?   @db.VarChar(13)
  createdAt     DateTime  @default(now()) @map("created_at")
  updatedAt     DateTime  @updatedAt @map("updated_at")

  @@map("books")
}
```

### 変更点

- `provider = "sqlite"` → `provider = "postgresql"`: データベースプロバイダをPostgreSQLに変更。
- `@db.VarChar(200)`: PostgreSQL固有のカラム型を指定。SQLiteではこの指定は不要だったが、PostgreSQLでは文字列長を明示するのが一般的。

---

## 3. 環境変数の更新

**.env** を以下の内容で上書きする。

```
DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager"
```

### PostgreSQL接続URL の構造

```
postgresql://ユーザー名:パスワード@ホスト:ポート/データベース名
```

---

## 4. マイグレーションの再作成

SQLiteからPostgreSQLへの切替では、マイグレーション履歴をリセットする。

既存のマイグレーションと開発用DBを削除する。

```powershell
Remove-Item -Recurse -Force prisma/migrations
Remove-Item -Force prisma/dev.db -ErrorAction SilentlyContinue
```

PostgreSQL用の新しいマイグレーションを作成する。

```powershell
npx prisma migrate dev --name init_postgresql
```

このコマンドにより以下が行われる。

1. PostgreSQLに `books` テーブルが作成される
2. `prisma/migrations/` に新しいマイグレーションファイルが生成される
3. Prisma Clientが再生成される

---

## 5. 動作確認

### アプリケーションの起動

```powershell
npm run dev
```

ブラウザで http://localhost:8000/ にアクセスし、書籍の登録・表示・更新・削除が全て動作することを確認する。

### PostgreSQLにデータが入っていることの確認

```powershell
psql -U bookuser -d bookmanager -h localhost
```

```sql
SELECT * FROM books;
\q
```

ブラウザから登録した書籍がPostgreSQLに保存されていることを確認する。

---

## 6. テストの実行

テストは引き続きSQLiteで実行する。`src/tests/setup.ts` でテスト用の接続先を `file:./test.db` と指定しているため、本番のPostgreSQLには影響しない。

ただし、Prismaスキーマのプロバイダが `postgresql` に変更されたため、テスト用のSQLite接続では `prisma migrate deploy` が動作しない。テスト用にはPrismaの `db push` を使うように `setup.ts` を修正する。

**src/tests/setup.ts** を以下の内容で上書きする。

```typescript
import { PrismaClient } from "@prisma/client";
import { execSync } from "child_process";

const testDatabaseUrl =
  "postgresql://bookuser:bookpass123@localhost:5432/bookmanager_test";

// テスト用データベースをPostgreSQLに変更
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: testDatabaseUrl,
    },
  },
});

beforeAll(async () => {
  // テスト用データベースを作成（存在しない場合）
  try {
    execSync(
      `psql -U bookuser -h localhost -c "CREATE DATABASE bookmanager_test;" postgres`,
      {
        env: { ...process.env, PGPASSWORD: "bookpass123" },
        stdio: "ignore",
      },
    );
  } catch {
    // 既に存在する場合はエラーを無視
  }

  // テスト用DBにスキーマを適用
  execSync("npx prisma db push --force-reset", {
    env: {
      ...process.env,
      DATABASE_URL: testDatabaseUrl,
    },
  });
});

beforeEach(async () => {
  await prisma.book.deleteMany();
});

afterAll(async () => {
  await prisma.$disconnect();
});

export { prisma };
```

### 変更点

- テスト用データベースもPostgreSQLを使用するように変更（`bookmanager_test` という別のデータベース）。
- `npx prisma db push --force-reset`: マイグレーション履歴を使わず、スキーマを直接データベースに適用するコマンド。テスト用には十分。

テスト用データベースを作成してからテストを実行する。

```powershell
npm test
```

全てのテストが成功することを確認する。

---

## 7. 本番ビルド

TypeScriptをJavaScriptにコンパイルし、本番用のファイルを生成する。

```powershell
npm run build
```

`dist/` ディレクトリにコンパイル済みのJavaScriptファイルが生成される。

静的ファイルをdistにコピーする。

```powershell
Copy-Item -Recurse src/public dist/public
```

ビルド済みファイルで起動確認する。

```powershell
node dist/server.js
```

正常に起動することを確認したら `Ctrl+C` で停止する。

---

## 8. 本日の確認ポイント

1. PostgreSQLにデータベースとユーザーが作成されている
2. アプリケーションがPostgreSQL経由で動作している
3. テストが全て成功する
4. `npm run build` でコンパイルが成功し、`dist/` にファイルが生成される
5. `node dist/server.js` でビルド済みアプリケーションが起動する

---

## Day 7 完了時のファイル構成

```
book-manager/
├── dist/                    ← ビルド生成物
│   ├── app.js
│   ├── server.js
│   ├── database.js
│   ├── routes/
│   ├── utils/
│   └── public/              ← 手動コピー
├── src/
│   ├── app.ts
│   ├── server.ts
│   ├── database.ts
│   ├── routes/
│   ├── utils/
│   ├── public/
│   └── tests/
│       ├── setup.ts         ← 更新
│       └── app.test.ts
├── prisma/
│   ├── schema.prisma        ← 更新（PostgreSQL）
│   └── migrations/          ← 再作成
├── .env                     ← 更新
├── .gitignore
├── jest.config.ts
├── package.json
└── tsconfig.json
```
