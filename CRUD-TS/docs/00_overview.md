# 書籍管理CRUDシステム開発 研修カリキュラム（TypeScript/Express版）

## 研修概要

本研修では「書籍管理システム」を題材に、TypeScript + Node.js によるWebアプリケーション開発の基礎からAWSデプロイまでを8日間（1日1時間）で体験する。

---

## 対象者

- プログラミングの基本文法（変数、条件分岐、ループ、関数）を理解している方
- JavaScriptの基本的な読み書きができる方（TypeScript未経験でも可）
- PowerShellの基本操作ができる方
- CRUD開発やWeb API開発は未経験でも可

---

## 研修ルール

### 禁止事項

1. **AIによるコーディング禁止**: GitHub Copilot、Codex、ChatGPTへのコード生成依頼など、AIにコードを書かせる行為は一切禁止とする。コードは全て自分の手で入力すること。
2. **コード質問の制限**: AIチャットでコードの書き方を直接質問することは禁止。エラーメッセージの意味や概念の説明を聞くことは可とする。

### 推奨事項

1. 分からないことはまず公式ドキュメントやWeb検索で調べる
2. エラーが出たらエラーメッセージを読み、自分で原因を考える
3. 各Dayの冒頭にある「学習目標」を意識しながら進める
4. 手を動かす前にコードの意味を理解してから写経する

---

## フェーズ構成

| フェーズ | 期間 | 内容 | ゴール |
|---------|------|------|--------|
| Phase 1 | Day 1 - Day 5 | CRUDシステム開発 | 書籍の登録・一覧・更新・削除がローカルで動作する |
| Phase 2 | Day 6 - Day 7 | テスト・DB切替 | 自動テストが通る / PostgreSQLで動作する |
| Phase 3 | Day 8 | AWSデプロイ | EC2上でアプリケーションが公開される |

---

## 日程別カリキュラム

| Day | テーマ | 主な成果物 | 技術スタック |
|-----|--------|-----------|-------------|
| Day 1 | 環境構築・Hello World API | プロジェクト雛形 + `/health` エンドポイント | Node.js 20+, TypeScript 5.x, Express.js, ts-node, nodemon |
| Day 2 | データベース設計・モデル作成 | booksテーブル + Prismaモデル定義 | Prisma ORM, SQLite, Prisma Migrate, Prisma Studio |
| Day 3 | Create / Read API実装 | `POST /books`, `GET /books`, `GET /books/:id` | Express Router, Prisma Client, async/await |
| Day 4 | Update / Delete API実装 | `PUT /books/:id`, `DELETE /books/:id` + バリデーション | 自作バリデーション関数, Express Error Handler |
| Day 5 | 簡易HTML画面作成 | ブラウザから操作できるCRUD画面 | express.static, HTML/CSS/JavaScript (fetch API) |
| Day 6 | テスト実装 | API結合テスト | Vitest, supertest |
| Day 7 | PostgreSQL切替・本番準備 | PostgreSQL接続 + 環境変数管理 + ビルド | Prisma (PostgreSQL provider), dotenv (.env), tsc build |
| Day 8 | AWS EC2デプロイ | EC2上での公開・動作確認 | Amazon Linux 2023, Node.js 20, PostgreSQL 16, npm, nohup |

---

## 技術スタック

| 項目 | 使用技術 |
|------|---------|
| 言語 | TypeScript 5.x (Node.js 20+) |
| Webフレームワーク | Express.js |
| ORM | Prisma |
| バリデーション | 自作関数（型ガード + 正規表現） |
| フロントエンド | 静的HTML + CSS + JavaScript (fetch API) |
| テストフレームワーク | Vitest |
| HTTPテストクライアント | supertest |
| DB（開発） | SQLite (Prisma内蔵ドライバ) |
| DB（本番） | PostgreSQL 16 (Prisma内蔵ドライバ) |
| ビルドツール | tsc (TypeScript Compiler) |
| パッケージ管理 | npm (package.json) |
| 開発サーバー | nodemon + ts-node |
| 本番実行 | node (コンパイル済みJS) |
| 環境変数管理 | Prisma (.env) |

---

## 完成イメージ

### API一覧

| メソッド | パス | 機能 |
|---------|------|------|
| GET | `/health` | ヘルスチェック |
| GET | `/books` | 書籍一覧取得 |
| GET | `/books/:id` | 書籍個別取得 |
| POST | `/books` | 書籍登録 |
| PUT | `/books/:id` | 書籍更新 |
| DELETE | `/books/:id` | 書籍削除 |
| GET | `/` | HTML画面（トップページ） |

### 書籍データ構造

| フィールド | TypeScript型 | DB型 | 説明 |
|-----------|-------------|------|------|
| id | number | Int (自動採番) | 主キー |
| title | string | String (必須) | 書籍タイトル |
| author | string | String (必須) | 著者名 |
| publisher | string \| null | String? (任意) | 出版社 |
| publishedDate | string \| null | String? (任意) | 出版日 (YYYY-MM-DD) |
| isbn | string \| null | String? (任意) | ISBN番号 |
| createdAt | Date | DateTime (自動) | 登録日時 |
| updatedAt | Date | DateTime (自動) | 更新日時 |

---

## ディレクトリ構成（完成時）

```
book-manager/
├── src/
│   ├── app.ts               # アプリケーション定義（ルーティング・ミドルウェア）
│   ├── server.ts            # サーバー起動（エントリポイント）
│   ├── database.ts          # Prisma Client インスタンス
│   ├── routes/
│   │   └── books.ts         # 書籍APIルーター（CRUD全操作）
│   ├── utils/
│   │   └── validation.ts    # バリデーション関数
│   ├── public/
│   │   ├── index.html       # 書籍管理画面
│   │   └── style.css        # スタイルシート
│   └── tests/
│       ├── setup.ts         # テストDB初期化
│       └── app.test.ts      # APIテスト
├── prisma/
│   ├── schema.prisma        # データベーススキーマ定義
│   └── migrations/          # マイグレーション履歴
├── dist/                    # コンパイル済みJS（ビルド生成物）
├── package.json             # プロジェクト設定・依存パッケージ
├── package-lock.json        # 依存バージョン固定
├── tsconfig.json            # TypeScript設定
├── vitest.config.ts         # テスト設定
├── .env                     # 環境変数（Git管理外）
└── .gitignore
```

### ディレクトリ構成の設計意図

- `src/app.ts` と `src/server.ts` の分離: テスト時に `app.ts` のみをインポートし、ポートを占有せずにAPIテストを実行するため。
- `src/routes/`: エンドポイントの定義をルーターとして分離。機能が増えてもファイルを追加するだけで対応できる。
- `src/utils/`: バリデーションなどの汎用ロジック。ルーターから切り出すことでテスト容易性を高める。
- `prisma/`: スキーマ定義とマイグレーション履歴。Prisma CLIが管理するディレクトリ。
- `dist/`: TypeScriptのコンパイル出力先。本番環境ではこのディレクトリのJSファイルを実行する。

---

## 事前準備チェックリスト

- [ ] Node.js 20以上がインストールされている (`node --version`)
- [ ] npm が使える (`npm --version`)
- [ ] テキストエディタ（VSCode推奨）がインストールされている
- [ ] PowerShellが使える
- [ ] curl が使える（PowerShellの `Invoke-RestMethod` でも可）

### Day 7以降で必要

- [ ] PostgreSQL がインストールされている（Day 7の資料で手順を説明）

### Day 8で必要

- [ ] AWSアカウントを持っている（研修用アカウントが発行されている）
- [ ] SSH接続用のターミナルが使える

---

## CRUDとは何か

CRUDは、データ操作の4つの基本機能の頭文字をとったものである。

| 操作 | 意味 | HTTPメソッド | SQL文 | Prismaメソッド |
|------|------|-------------|-------|---------------|
| **C**reate | 作成 | POST | INSERT | `prisma.book.create()` |
| **R**ead | 読み取り | GET | SELECT | `prisma.book.findMany()` / `findUnique()` |
| **U**pdate | 更新 | PUT / PATCH | UPDATE | `prisma.book.update()` |
| **D**elete | 削除 | DELETE | DELETE | `prisma.book.delete()` |

ほぼ全てのWebアプリケーション（ECサイト、SNS、業務システム等）はこの4操作の組み合わせで成り立っている。本研修で書籍管理システムを通じてCRUDを実装することで、あらゆるWebアプリケーション開発の基礎力を身につける。

### REST APIとは

REST（Representational State Transfer）は、Web APIの設計原則である。以下のルールに従ってAPIを設計する。

- URLで「リソース（データの種類）」を表す: `/books`
- HTTPメソッドで「操作」を表す: GET, POST, PUT, DELETE
- レスポンスはJSON形式で返す

例えば「ID=1の書籍を取得する」場合:

```
GET /books/1
```

レスポンス:

```json
{
  "id": 1,
  "title": "トム・ソーヤーの冒険",
  "author": "マーク・トウェイン",
  "publisher": "新潮文庫"
}
```

このように、URLとHTTPメソッドの組み合わせだけで、何のデータに何をするかが明確になる設計がRESTである。

---

## TypeScriptとは何か

TypeScriptは、JavaScriptに「型」を追加した言語である。Microsoft社が開発・メンテナンスしている。

### JavaScriptとの違い

JavaScript（型なし）:

```javascript
function add(a, b) {
  return a + b;
}
add(1, "2"); // "12" になる（意図しない動作）
```

TypeScript（型あり）:

```typescript
function add(a: number, b: number): number {
  return a + b;
}
add(1, "2"); // コンパイルエラー（実行前にミスを検出）
```

TypeScriptはコンパイル時に型チェックを行い、実行前にバグを発見できる。コンパイル後は通常のJavaScriptになるため、Node.jsでそのまま実行できる。

### 本研修でTypeScriptを使う理由

- 型があることでエディタの補完が強力に効き、学習効率が上がる
- Prisma ORMとの相性が非常に良く、データベース操作でもタイプミスをコンパイル時に検出できる
- 実務でのTypeScript採用率が年々増加しており、習得する価値が高い
