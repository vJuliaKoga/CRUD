# Day 1: 環境構築・Hello World API（TypeScript/Express版）

## 学習目標

- Node.jsプロジェクトを作成し、TypeScriptの開発環境を構築できる
- Express.jsの基本構造を理解し、最小限のAPIサーバーを起動できる
- curlまたはブラウザでAPIの動作確認ができる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| プロジェクト作成・TypeScript設定 | 20分 |
| Express.jsの基本コード作成 | 20分 |
| 動作確認・理解 | 20分 |

---

## 1. プロジェクトの作成

PowerShellを開き、以下のコマンドを順に実行する。

```powershell
mkdir book-manager
cd book-manager
```

## 2. Node.jsプロジェクトの初期化

```powershell
npm init -y
```

`package.json` が生成される。これはNode.jsプロジェクトの設定ファイルであり、依存パッケージやスクリプトを管理する。Pythonの `requirements.txt` とJavaの `pom.xml` を合わせたような役割を持つ。

---

## 3. 依存パッケージのインストール

```powershell
npm install express
npm install -D typescript ts-node @types/node @types/express nodemon
```

### 各パッケージの役割

**本番用（dependencies）**:
- `express`: Node.jsの定番Webフレームワーク。ルーティングやミドルウェアの仕組みを提供する。

**開発用（devDependencies）**:
- `typescript`: TypeScriptコンパイラ。TypeScriptコードをJavaScriptに変換する。
- `ts-node`: TypeScriptファイルを直接実行できるようにするツール。開発時にコンパイルを意識せずに済む。
- `@types/node`: Node.jsの型定義。TypeScriptがNode.jsの標準APIを理解するために必要。
- `@types/express`: Express.jsの型定義。TypeScriptがExpressのAPIを理解するために必要。
- `nodemon`: ファイル変更を検知して自動でサーバーを再起動するツール。FastAPIの `--reload` やSpring Bootのdevtoolsに相当。

### `npm install` と `-D` フラグ

`npm install パッケージ名` は本番環境でも使うパッケージ、`npm install -D パッケージ名` は開発時のみ使うパッケージとしてインストールする。`-D` は `--save-dev` の省略形。

---

## 4. TypeScriptの設定

TypeScriptの設定ファイル `tsconfig.json` を作成する。

```powershell
npx tsc --init
```

生成された `tsconfig.json` を開き、以下の内容で**上書き**する。

**tsconfig.json**

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
    "declaration": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

### 主要な設定項目の解説

- `target`: コンパイル先のJavaScriptバージョン。ES2022は十分にモダンで、ほぼ全ての環境で動作する。
- `module`: モジュールシステム。`commonjs` はNode.jsの標準的な形式。
- `outDir`: コンパイルしたJavaScriptの出力先ディレクトリ。
- `rootDir`: TypeScriptソースファイルのルートディレクトリ。
- `strict`: TypeScriptの厳密な型チェックを全て有効にする。型安全性を最大限に活かすために推奨。
- `esModuleInterop`: CommonJSモジュールをES Moduleのimport構文で読み込めるようにする。

---

## 5. ソースディレクトリの作成

```powershell
mkdir src
```

---

## 6. Hello World APIの作成

テキストエディタで `src/app.ts` を新規作成し、以下のコードを**手で入力**する。

**src/app.ts**

```typescript
import express, { Request, Response } from "express";

const app = express();

app.use(express.json());

app.get("/health", (req: Request, res: Response) => {
  res.json({ status: "ok", message: "Book Manager API is running" });
});

app.get("/", (req: Request, res: Response) => {
  res.json({ message: "Welcome to Book Manager" });
});

export default app;
```

**src/server.ts** を新規作成する。

```typescript
import app from "./app";

const PORT = process.env.PORT || 8000;

app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});
```

### コード解説

- `import express from "express"`: Express.jsのモジュールを読み込む。
- `Request, Response`: TypeScriptの型定義。リクエストとレスポンスの型を明示することで、エディタの補完とコンパイル時のエラーチェックが有効になる。
- `app.use(express.json())`: リクエストボディのJSONを自動的にパースするミドルウェアを登録する。これがないとPOSTリクエストのボディを読めない。
- `app.get("/health", ...)`: GETメソッドで `/health` パスへのリクエストを処理するルートを登録する。
- `res.json(...)`: JSON形式でレスポンスを返す。
- `app` と `server` を分離する理由: テスト時に `app` だけをインポートしてサーバーを起動せずにテストできるようにするため。

### なぜapp.tsとserver.tsを分けるのか

`app.ts` にはExpressアプリケーションの定義（ルーティング、ミドルウェア）のみを書き、`server.ts` にはサーバーの起動処理を書く。Day 6のテストでは `app.ts` だけをインポートし、実際にポートをlistenせずにAPIをテストする。この分離はNode.jsのテスト設計におけるベストプラクティスである。

---

## 7. 起動スクリプトの設定

**package.json** の `"scripts"` セクションを以下のように書き換える。

```json
"scripts": {
  "dev": "nodemon --exec ts-node src/server.ts",
  "build": "tsc",
  "start": "node dist/server.js"
}
```

### 各スクリプトの役割

- `dev`: 開発用。ファイル変更を検知して自動再起動する。
- `build`: TypeScriptをJavaScriptにコンパイルする。本番デプロイ時に使用。
- `start`: コンパイル済みのJavaScriptを実行する。本番環境で使用。

---

## 8. サーバーの起動

```powershell
npm run dev
```

起動に成功すると、以下のログが表示される。

```
Server is running on http://localhost:8000
```

---

## 9. 動作確認

サーバーを起動したまま、別のPowerShellウィンドウを開いて以下を実行する。

### PowerShellで確認

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/health"
```

期待されるレスポンス:

```
status message
------ -------
ok     Book Manager API is running
```

### curlで確認（curlが使える場合）

```powershell
curl http://localhost:8000/health
```

### ブラウザで確認

ブラウザで以下のURLにアクセスする。

- ヘルスチェック: http://localhost:8000/health
- ルート: http://localhost:8000/

---

## 10. 本日の確認ポイント

1. `node --version` で Node.js 18以上が表示される
2. `npx tsc --version` で TypeScriptバージョンが表示される
3. `npm run dev` でサーバーが起動する
4. `http://localhost:8000/health` で `{"status":"ok",...}` が返る
5. `src/app.ts` を編集して保存すると、サーバーが自動的に再起動する

---

## Day 1 完了時のファイル構成

```
book-manager/
├── node_modules/        # 依存パッケージ（Git管理対象外）
├── src/
│   ├── app.ts           # アプリケーション定義
│   └── server.ts        # サーバー起動
├── package.json         # プロジェクト設定
├── package-lock.json    # 依存バージョン固定
└── tsconfig.json        # TypeScript設定
```

---

## 自習課題（余裕がある場合）

- Express.js公式ドキュメント（https://expressjs.com/ja/）の「はじめに」を読む
- TypeScript公式ドキュメント（https://www.typescriptlang.org/docs/）の「TypeScript for JavaScript Programmers」を読む
- `app.get("/hello/:name", ...)` のようにパスパラメータを受け取るエンドポイントを自分で追加してみる（`req.params.name` で取得できる）
