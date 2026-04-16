# Day 6: テスト実装（TypeScript/Express版）

## 学習目標

- Vitestを使った自動テストの基本を理解する
- supertestを使ったAPI結合テストを作成できる
- テスト用データベースの分離方法を理解する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| Vitest/supertest導入・設定 | 15分 |
| テストヘルパー作成 | 10分 |
| APIテスト作成 | 25分 |
| テスト実行・修正 | 10分 |

---

## 1. テスト用パッケージのインストール

```powershell
npm install -D vitest supertest @types/supertest
```

- `vitest`: Viteベースの高速テストフレームワーク。TypeScriptをネイティブサポートするため設定が少なくて済む。
- `supertest`: HTTPリクエストを模倣してExpressアプリをテストするライブラリ。FastAPIのTestClient、Spring BootのMockMvcに相当。
- `@types/supertest`: supertest用のTypeScript型定義。

### VitestとJestの違い

Jestは長年の定番だが、TypeScriptのサポートには `ts-jest` などの追加設定が必要になる。Vitestは最初からTypeScriptとESModulesに対応しており、設定ファイルが最小限で済む点が利点である。APIはJestとほぼ互換のため、Jest経験者でも違和感なく使える。

---

## 2. Vitest設定ファイルの作成

**vitest.config.ts** をプロジェクトルートに新規作成する。

```typescript
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    globals: true,
    environment: "node",
    include: ["src/**/*.test.ts"],
    setupFiles: ["src/tests/setup.ts"],
  },
});
```

### 設定項目の解説

- `globals: true`: `describe`, `test`, `expect` などをimportなしで使えるようにする。
- `environment: "node"`: Node.js環境でテストを実行する。
- `include: ["src/**/*.test.ts"]`: `.test.ts` で終わるファイルをテスト対象とする。
- `setupFiles`: テスト実行前に読み込まれるセットアップファイル。

---

## 3. TypeScript型定義の追加

`globals: true` でVitestのAPIをimportなしで使うため、TypeScriptに型を認識させる設定を追加する。

**tsconfig.json** の `compilerOptions` に以下を追加する。

```json
"types": ["node", "vitest/globals"]
```

修正後の `tsconfig.json` 全体:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "commonjs",
    "lib": ["ES2022"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "types": ["node", "vitest/globals"]
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

---

## 4. package.jsonのテストスクリプト追加

**package.json** の `"scripts"` セクションにテストコマンドを追加する。

```json
"scripts": {
  "dev": "nodemon --exec ts-node src/server.ts",
  "build": "tsc",
  "start": "node dist/server.js",
  "test": "vitest run"
}
```

- `vitest run`: テストを1回実行して終了する。`vitest`（引数なし）にするとファイル変更を監視して自動再実行するウォッチモードになる。

---

## 5. テスト用データベース設定

テスト時は本番のデータベースを使わず、テスト専用のSQLiteファイルを使用する。

テストヘルパーを作成する。テストごとにデータベースを初期化する仕組みを用意する。

```powershell
mkdir src/tests
```

**src/tests/setup.ts** を新規作成する。

```typescript
import { PrismaClient } from "@prisma/client";
import { execSync } from "child_process";
import { beforeAll, beforeEach, afterAll } from "vitest";

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: "file:./test.db",
    },
  },
});

beforeAll(async () => {
  execSync("npx prisma migrate deploy", {
    env: {
      ...process.env,
      DATABASE_URL: "file:./test.db",
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

### コード解説

- テスト専用のデータベースファイル `test.db` に接続するPrismaClientを作成する。
- `beforeAll`: 全テストの実行前に1度だけ呼ばれる。マイグレーションを実行してテーブルを作成する。
- `beforeEach`: 各テストの実行前に呼ばれる。データを全削除してテスト間の影響を排除する。
- `afterAll`: 全テスト完了後にDB接続を切断する。

---

## 6. APIテストの作成

**src/tests/app.test.ts** を新規作成する。

```typescript
import request from "supertest";
import app from "../app";
import { prisma } from "./setup";

describe("Health Check", () => {
  test("GET /health は200を返す", async () => {
    const res = await request(app).get("/health");
    expect(res.status).toBe(200);
    expect(res.body.status).toBe("ok");
  });
});

describe("Books API", () => {
  describe("POST /books", () => {
    test("書籍を登録できる", async () => {
      const res = await request(app)
        .post("/books")
        .send({ title: "テスト書籍", author: "テスト著者" });

      expect(res.status).toBe(201);
      expect(res.body.title).toBe("テスト書籍");
      expect(res.body.author).toBe("テスト著者");
      expect(res.body.id).toBeDefined();
    });

    test("タイトル未指定は422エラー", async () => {
      const res = await request(app)
        .post("/books")
        .send({ author: "著者のみ" });

      expect(res.status).toBe(422);
    });

    test("空のタイトルは422エラー", async () => {
      const res = await request(app)
        .post("/books")
        .send({ title: "", author: "テスト著者" });

      expect(res.status).toBe(422);
    });
  });

  describe("GET /books", () => {
    test("空の一覧を取得できる", async () => {
      const res = await request(app).get("/books");
      expect(res.status).toBe(200);
      expect(res.body).toEqual([]);
    });

    test("登録後の一覧を取得できる", async () => {
      await request(app).post("/books").send({ title: "本A", author: "著者A" });
      await request(app).post("/books").send({ title: "本B", author: "著者B" });

      const res = await request(app).get("/books");
      expect(res.status).toBe(200);
      expect(res.body).toHaveLength(2);
    });
  });

  describe("GET /books/:id", () => {
    test("個別の書籍を取得できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "個別取得テスト", author: "著者" });

      const res = await request(app).get(`/books/${created.body.id}`);
      expect(res.status).toBe(200);
      expect(res.body.title).toBe("個別取得テスト");
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app).get("/books/9999");
      expect(res.status).toBe(404);
    });
  });

  describe("PUT /books/:id", () => {
    test("書籍を部分更新できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "更新前", author: "著者" });

      const res = await request(app)
        .put(`/books/${created.body.id}`)
        .send({ title: "更新後" });

      expect(res.status).toBe(200);
      expect(res.body.title).toBe("更新後");
      expect(res.body.author).toBe("著者");
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app)
        .put("/books/9999")
        .send({ title: "存在しない" });

      expect(res.status).toBe(404);
    });
  });

  describe("DELETE /books/:id", () => {
    test("書籍を削除できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "削除対象", author: "著者" });

      const deleteRes = await request(app).delete(`/books/${created.body.id}`);
      expect(deleteRes.status).toBe(204);

      const getRes = await request(app).get(`/books/${created.body.id}`);
      expect(getRes.status).toBe(404);
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app).delete("/books/9999");
      expect(res.status).toBe(404);
    });
  });
});
```

### コード解説

- `request(app)`: supertestがExpressアプリに対してHTTPリクエストを模倣する。実際にポートをlistenせずにテストできる（app.tsとserver.tsを分離した理由がここで活きる）。
- `describe("...", () => {...})`: テストをグループ化する。テスト結果の表示が見やすくなる。
- `test("...", async () => {...})`: 個別のテストケース。pytestの `test_xxx` やJUnitの `@Test` に相当。
- `expect(値).toBe(期待値)`: 値の一致を検証する。
- `expect(値).toBeDefined()`: 値が `undefined` でないことを検証する。
- `expect(配列).toHaveLength(数)`: 配列の要素数を検証する。
- `expect(値).toEqual(期待値)`: オブジェクトや配列の深い比較。

---

## 7. テストの実行

```powershell
npm test
```

全てのテストが成功すると、以下のような出力が表示される。

```
 ✓ src/tests/app.test.ts (12)
   ✓ Health Check (1)
     ✓ GET /health は200を返す
   ✓ Books API (11)
     ✓ POST /books (3)
       ✓ 書籍を登録できる
       ✓ タイトル未指定は422エラー
       ✓ 空のタイトルは422エラー
     ✓ GET /books (2)
       ✓ 空の一覧を取得できる
       ✓ 登録後の一覧を取得できる
     ✓ GET /books/:id (2)
       ✓ 個別の書籍を取得できる
       ✓ 存在しないIDは404を返す
     ✓ PUT /books/:id (2)
       ✓ 書籍を部分更新できる
       ✓ 存在しないIDは404を返す
     ✓ DELETE /books/:id (2)
       ✓ 書籍を削除できる
       ✓ 存在しないIDは404を返す

 Test Files  1 passed (1)
      Tests  12 passed (12)
```

---

## 8. 本日の確認ポイント

1. `npm test` で全てのテストがpassedになる
2. テスト用データベースと本番データベースが分離されている
3. `describe` / `test` / `expect` によるテスト構造を理解している
4. supertestによるHTTPリクエストの模倣方法を理解している

---

## Day 6 完了時のファイル構成

```
book-manager/
├── src/
│   ├── app.ts
│   ├── server.ts
│   ├── database.ts
│   ├── routes/
│   │   └── books.ts
│   ├── utils/
│   │   └── validation.ts
│   ├── public/
│   │   ├── index.html
│   │   └── style.css
│   └── tests/
│       ├── setup.ts         ← 新規
│       └── app.test.ts      ← 新規
├── prisma/
│   ├── schema.prisma
│   ├── dev.db
│   └── migrations/
├── vitest.config.ts         ← 新規
├── tsconfig.json            ← 更新
├── package.json             ← 更新
└── ...
```
