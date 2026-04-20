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

インストール後に `Stack Builder` が起動しても、この研修では追加コンポーネントは不要なので閉じて良い。

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

`psql` が見つからない場合は、次のようにフルパス指定でも接続できる。

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U bookuser -d bookmanager -h localhost
```

---

## 2. Prismaスキーマの変更

**prisma/schema.prisma** のデータソース部分を以下のように変更する。

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider          = "postgresql"
  url               = env("DATABASE_URL")
  shadowDatabaseUrl = env("SHADOW_DATABASE_URL")
}

model Book {
  id            Int      @id @default(autoincrement())
  title         String   @db.VarChar(200)
  author        String   @db.VarChar(100)
  publisher     String?  @db.VarChar(100)
  publishedDate String?  @map("published_date") @db.VarChar(10)
  isbn          String?  @db.VarChar(13)
  createdAt     DateTime @default(now()) @map("created_at")
  updatedAt     DateTime @updatedAt @map("updated_at")

  @@map("books")
}
```

### コード解説

- `provider = "postgresql"`: データベースプロバイダをPostgreSQLに変更する。
- `@db.VarChar(...)`: PostgreSQL固有のカラム型を指定する。文字列長を明示してテーブル定義を分かりやすくする。
- `shadowDatabaseUrl`: `prisma migrate dev` 実行時にPrismaが内部的に使う確認用スキーマの接続先。`bookuser` に `CREATE DATABASE` 権限がなくてもマイグレーションを実行できるようにする。

---

## 3. 環境変数の更新

**.env** を以下の内容で上書きする。

```
DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager?schema=public"
SHADOW_DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager?schema=prisma_shadow"
```

### PostgreSQL接続URL の構造

```
postgresql://ユーザー名:パスワード@ホスト:ポート/データベース名?schema=スキーマ名
```

- `DATABASE_URL`: アプリケーション本体が使う接続先。`public` スキーマを使用する。
- `SHADOW_DATABASE_URL`: Prisma Migrate が内部チェック用に使う接続先。`prisma_shadow` スキーマを使用する。

---

## 4. マイグレーションの再作成

SQLiteからPostgreSQLへの切替では、マイグレーション履歴をリセットする。

既存のマイグレーションと開発用DBを削除する。

```powershell
Remove-Item -Recurse -Force prisma/migrations
Remove-Item -Force prisma/dev.db -ErrorAction SilentlyContinue
Remove-Item -Force prisma/test.db -ErrorAction SilentlyContinue
```

PostgreSQL用の新しいマイグレーションを作成する。

```powershell
npx prisma migrate dev --name init_postgresql
```

このコマンドにより以下が行われる。

1. PostgreSQLに `books` テーブルが作成される
2. `prisma/migrations/` に新しいマイグレーションファイルが生成される
3. Prisma Clientが再生成される
4. `prisma_shadow` スキーマを使ってマイグレーションの整合性チェックが行われる

---

## 5. 動作確認

### アプリケーションの起動

```powershell
npm run dev
```

ブラウザで http://localhost:8000/ にアクセスし、書籍の登録・表示・更新・削除が全て動作することを確認する。Day 3〜Day 5 で扱ってきた `トム・ソーヤーの冒険` や `ハックルベリー・フィンの冒険` を登録して確かめると流れを追いやすい。

### PostgreSQLにデータが入っていることの確認

```powershell
psql -U bookuser -d bookmanager -h localhost
```

```sql
SELECT * FROM books;
\q
```

ブラウザから登録した書籍がPostgreSQLに保存されていることを確認する。`トム・ソーヤーの冒険` や `ハックルベリー・フィンの冒険` の行が見えれば、SQLiteからPostgreSQLへ切り替わっても同じ操作感で扱えていると判断できる。

---

## 6. テストの実行

テストもPostgreSQLで実行する。ただし、本番用の `public` スキーマは使わず、同じ `bookmanager` データベース内の `bookmanager_test` スキーマをテスト専用に使う。

この方法なら、`bookuser` に `CREATE DATABASE` 権限がなくてもテスト環境を分離できる。

**src/tests/setup.ts** を以下の内容で上書きする。

```typescript
import { execFileSync } from "child_process";
import { PrismaClient } from "@prisma/client";
import { afterAll, beforeAll, beforeEach } from "vitest";

const testDatabaseUrl =
  "postgresql://bookuser:bookpass123@localhost:5432/bookmanager?schema=bookmanager_test";

process.env.DATABASE_URL = testDatabaseUrl;

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: testDatabaseUrl,
    },
  },
});

beforeAll(async () => {
  const env = {
    ...process.env,
    DATABASE_URL: testDatabaseUrl,
    RUST_LOG: "schema_engine=trace",
  };

  if (process.platform === "win32") {
    execFileSync(
      process.env.ComSpec ?? "cmd.exe",
      ["/d", "/s", "/c", "npx prisma db push --force-reset --skip-generate"],
      {
        env,
        stdio: "ignore",
      },
    );
    return;
  }

  execFileSync(
    "npx",
    ["prisma", "db", "push", "--force-reset", "--skip-generate"],
    {
      env,
      stdio: "ignore",
    },
  );
});

beforeEach(async () => {
  await prisma.book.deleteMany();
});

afterAll(async () => {
  await prisma.$disconnect();
});

export { prisma };
```

### コード解説

- `bookmanager?schema=bookmanager_test`: テスト専用スキーマを使い、本番用の `public` スキーマと分離する。
- `process.env.DATABASE_URL = testDatabaseUrl`: テスト実行中だけPrismaの接続先を差し替える。
- `npx prisma db push --force-reset --skip-generate`: テスト用スキーマを毎回作り直して、前回のテストデータを完全にリセットする。

追加のテスト用データベース作成は不要。そのままテストを実行する。

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
Copy-Item -Recurse -Force src/public dist/public
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
├── vitest.config.ts
├── package.json
└── tsconfig.json
```
