# 書籍管理CRUDシステム開発 研修カリキュラム

## 研修概要

本研修では「書籍管理システム」を題材に、Webアプリケーション開発の基礎からAWSデプロイまでを8日間（1日1時間）で体験する。
研修者は Python/FastAPI 版または Java/Spring Boot 版のいずれかを選択して取り組む。

---

## 対象者

- プログラミングの基本文法（変数、条件分岐、ループ、関数）を理解している方
- ターミナル/コマンドプロンプトの基本操作ができる方
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

| フェーズ | 期間          | 内容             | ゴール                                           |
| -------- | ------------- | ---------------- | ------------------------------------------------ |
| Phase 1  | Day 1 - Day 5 | CRUDシステム開発 | 書籍の登録・一覧・更新・削除がローカルで動作する |
| Phase 2  | Day 6 - Day 7 | テスト・DB切替   | 自動テストが通る / PostgreSQLで動作する          |
| Phase 3  | Day 8         | AWSデプロイ      | EC2上でアプリケーションが公開される              |

---

## 日程別カリキュラム

| Day   | テーマ                       | 主な成果物                                               |
| ----- | ---------------------------- | -------------------------------------------------------- |
| Day 1 | 環境構築・Hello World API    | プロジェクト雛形 + `/health` エンドポイント              |
| Day 2 | データベース設計・モデル作成 | booksテーブル + ORMモデル定義                            |
| Day 3 | Create / Read API実装        | `POST /books`, `GET /books`, `GET /books/{id}`           |
| Day 4 | Update / Delete API実装      | `PUT /books/{id}`, `DELETE /books/{id}` + バリデーション |
| Day 5 | 簡易HTML画面作成             | ブラウザから操作できるCRUD画面                           |
| Day 6 | テスト実装                   | 単体テスト + API結合テスト                               |
| Day 7 | PostgreSQL切替・本番準備     | PostgreSQL接続 + 環境変数管理                            |
| Day 8 | AWS EC2デプロイ              | EC2上での公開・動作確認                                  |

---

## 技術スタック比較

| 項目                    | Python版       | Java版                      |
| ----------------------- | -------------- | --------------------------- |
| 言語バージョン          | Python 3.11+   | Java 17+                    |
| Webフレームワーク       | FastAPI        | Spring Boot 3.x             |
| ORM                     | SQLAlchemy     | Spring Data JPA (Hibernate) |
| テンプレートエンジン    | Jinja2         | Thymeleaf                   |
| テストフレームワーク    | pytest         | JUnit 5                     |
| DBドライバ (SQLite)     | sqlite3 (標準) | H2 Database                 |
| DBドライバ (PostgreSQL) | psycopg2       | PostgreSQL JDBC             |
| ビルドツール            | pip + venv     | Maven                       |
| 開発サーバー            | uvicorn        | 組込みTomcat                |

※ Java版はローカル開発時にH2 Database（SQLite相当の組込みDB）を使用し、Day 7でPostgreSQLに切り替える。

---

## 完成イメージ

### API一覧（両言語共通）

| メソッド | パス          | 機能                     |
| -------- | ------------- | ------------------------ |
| GET      | `/health`     | ヘルスチェック           |
| GET      | `/books`      | 書籍一覧取得             |
| GET      | `/books/{id}` | 書籍個別取得             |
| POST     | `/books`      | 書籍登録                 |
| PUT      | `/books/{id}` | 書籍更新                 |
| DELETE   | `/books/{id}` | 書籍削除                 |
| GET      | `/`           | HTML画面（トップページ） |

### 書籍データ構造

| フィールド     | 型              | 説明         |
| -------------- | --------------- | ------------ |
| id             | 整数 (自動採番) | 主キー       |
| title          | 文字列 (必須)   | 書籍タイトル |
| author         | 文字列 (必須)   | 著者名       |
| publisher      | 文字列 (任意)   | 出版社       |
| published_date | 日付 (任意)     | 出版日       |
| isbn           | 文字列 (任意)   | ISBN番号     |
| created_at     | 日時 (自動)     | 登録日時     |
| updated_at     | 日時 (自動)     | 更新日時     |

---

## ディレクトリ構成（完成時）

### Python/FastAPI版

```
book-manager/
├── main.py              # アプリケーションエントリポイント
├── database.py          # DB接続設定
├── models.py            # SQLAlchemyモデル
├── schemas.py           # リクエスト/レスポンス定義
├── crud.py              # CRUD操作関数
├── routers/
│   └── books.py         # 書籍APIルーター
├── templates/
│   ├── base.html        # ベーステンプレート
│   └── index.html       # 書籍一覧画面
├── static/
│   └── style.css        # スタイルシート
├── tests/
│   ├── test_crud.py     # 単体テスト
│   └── test_api.py      # APIテスト
├── requirements.txt     # 依存パッケージ
└── .env                 # 環境変数（Git管理外）
```

### Java/Spring Boot版

```
book-manager/
├── pom.xml                              # Maven設定
├── src/main/java/com/training/bookmanager/
│   ├── BookManagerApplication.java      # エントリポイント
│   ├── model/
│   │   └── Book.java                    # エンティティ
│   ├── repository/
│   │   └── BookRepository.java          # リポジトリ
│   ├── service/
│   │   └── BookService.java             # ビジネスロジック
│   ├── controller/
│   │   ├── BookApiController.java       # REST API
│   │   └── BookViewController.java     # 画面コントローラ
│   └── dto/
│       ├── BookRequest.java             # リクエストDTO
│       └── BookResponse.java            # レスポンスDTO
├── src/main/resources/
│   ├── application.properties           # 設定ファイル
│   ├── templates/
│   │   └── index.html                   # Thymeleaf画面
│   └── static/
│       └── style.css                    # スタイルシート
└── src/test/java/com/training/bookmanager/
    ├── BookServiceTest.java             # 単体テスト
    └── BookApiControllerTest.java       # APIテスト
```

---

## 事前準備チェックリスト

### Python版を選択する場合

- [ ] Python 3.11以上がインストールされている (`python --version`)
- [ ] pip が使える (`pip --version`)
- [ ] テキストエディタ（VSCode推奨）がインストールされている
- [ ] ターミナル/PowerShellが使える
- [ ] curl が使える（Windows: PowerShellのInvoke-WebRequestでも可）

### Java版を選択する場合

- [ ] JDK 17以上がインストールされている (`java -version`)
- [ ] Maven がインストールされている (`mvn -version`)
- [ ] テキストエディタ（VSCode推奨、IntelliJ IDEA Community Editionも可）
- [ ] ターミナル/PowerShellが使える
- [ ] curl が使える

### Day 7以降で必要

- [ ] PostgreSQL がインストールされている（Day 7の資料で手順を説明）

### Day 8で必要

- [ ] AWSアカウントを持っている（研修用アカウントが発行されている）
- [ ] SSH接続用のターミナルが使える

---

## CRUDとは何か

CRUDは、データ操作の4つの基本機能の頭文字をとったもの。

| 操作       | 意味     | HTTPメソッド | SQL文  |
| ---------- | -------- | ------------ | ------ |
| **C**reate | 作成     | POST         | INSERT |
| **R**ead   | 読み取り | GET          | SELECT |
| **U**pdate | 更新     | PUT / PATCH  | UPDATE |
| **D**elete | 削除     | DELETE       | DELETE |

ほぼ全てのWebアプリケーション（ECサイト、SNS、業務システム等）はこの4操作の組み合わせで成り立っている。
本研修では、書籍管理システムを通じてCRUDを実装することで、あらゆるWebアプリケーション開発の基礎力を身につける事を目的とする。

### REST APIとは

REST（Representational State Transfer）は、Web APIの設計原則。以下のルールに従ってAPIを設計する。

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

このように、URLとHTTPメソッドの組み合わせだけで、何のデータに何をするかが明確になる設計がREST。
