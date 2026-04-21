# Day 7: PostgreSQL切替・本番準備（Python/FastAPI版）

## 学習目標

- SQLiteからPostgreSQLへのデータベース切替手順を理解する
- 環境変数による設定管理（12-Factor App原則）を実践できる
- 本番デプロイに向けた構成変更を行える

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| PostgreSQLセットアップ | 20分 |
| 環境変数管理の導入 | 15分 |
| 接続切替・動作確認 | 15分 |
| テスト修正・全体確認 | 10分 |

---

## 1. PostgreSQLのセットアップ

### インストール（未インストールの場合）

Windows: PostgreSQLの公式サイト（https://www.postgresql.org/download/windows/）からインストーラをダウンロードしてインストールする。インストール時に設定するパスワードを控えておくこと。

### データベースとユーザーの作成

PostgreSQLに接続する。

```bash
psql -U postgres
```

Windows PowerShellの場合:

```powershell
psql -U postgres
```

以下のSQL文を実行する。

```sql
CREATE USER bookuser WITH PASSWORD 'bookpass123';
CREATE DATABASE bookmanager OWNER bookuser;
GRANT ALL PRIVILEGES ON DATABASE bookmanager TO bookuser;
\q
```

接続確認を行う。

```bash
psql -U bookuser -d bookmanager -h localhost
```

Windows PowerShellの場合:

```powershell
psql -U bookuser -d bookmanager -h localhost
```

パスワードを求められたら `bookpass123` を入力する。接続できたら `\q` で抜ける。

---

## 2. 依存パッケージの追加

**requirements.txt** に PostgreSQL用ドライバを追加する。

```
fastapi==0.115.0
uvicorn==0.30.0
sqlalchemy==2.0.35
jinja2==3.1.4
python-multipart==0.0.12
pytest==8.3.0
httpx==0.27.0
psycopg2-binary==2.9.9
python-dotenv==1.0.1
```

```bash
pip install -r requirements.txt
```

Windows PowerShellの場合:

```powershell
python -m pip install -r requirements.txt
```

- `psycopg2-binary`: PostgreSQL用のPythonドライバ。SQLAlchemyがPostgreSQLに接続する際に内部で使用する。
- `python-dotenv`: `.env` ファイルから環境変数を読み込むライブラリ。

---

## 3. 環境変数による設定管理

これまでデータベースの接続先は `database.py` にハードコードしていた。本番環境では接続先やパスワードが異なるため、環境変数で管理する。これは「12-Factor App」と呼ばれる設計原則の1つで、設定をコードから分離することで環境ごとの切替を容易にする。

### .envファイルの作成

プロジェクトルートに `.env` ファイルを作成する。

**.env**

```
DATABASE_URL=postgresql://bookuser:bookpass123@localhost:5432/bookmanager
```

このファイルにはパスワード等の秘密情報が含まれるため、Gitで管理してはならない。`.gitignore` に追加すること。

**.gitignore**（プロジェクトルートに作成）

```
venv/
__pycache__/
*.db
.env
```

### 環境変数の補足

`.env` ファイルはローカル開発時の利便性のために使う。本番環境（AWS EC2等）ではOSの環境変数として設定するのが一般的であり、`.env` ファイルは配置しない。`python-dotenv` は `.env` ファイルが存在しない場合でも、OSの環境変数を読み取って動作する。

---

## 4. database.pyの更新

**database.py** を以下の内容で上書きする。

```python
import os

from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./books.db")

connect_args = {}
if DATABASE_URL.startswith("sqlite"):
    connect_args = {"check_same_thread": False}

engine = create_engine(DATABASE_URL, connect_args=connect_args)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

### コード解説

- `load_dotenv()`: `.env` ファイルの内容をOSの環境変数として読み込む。
- `os.getenv("DATABASE_URL", "sqlite:///./books.db")`: 環境変数 `DATABASE_URL` を取得する。設定されていない場合はSQLiteをフォールバックとして使う。
- `connect_args` の条件分岐: `check_same_thread` はSQLite固有の設定であり、PostgreSQLでは不要なため、SQLite使用時のみ設定する。

---

## 5. models.pyの微修正

PostgreSQLでは `func.now()` の `onupdate` が自動では動作しないため、Pythonレベルで現在時刻を設定するように変更する。

**models.py** を以下の内容で上書きする。

```python
from datetime import datetime, timezone

from sqlalchemy import String, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


def utcnow():
    return datetime.now(timezone.utc)


class Book(Base):
    __tablename__ = "books"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    author: Mapped[str] = mapped_column(String(100), nullable=False)
    publisher: Mapped[str | None] = mapped_column(String(100), nullable=True)
    published_date: Mapped[str | None] = mapped_column(String(10), nullable=True)
    isbn: Mapped[str | None] = mapped_column(String(13), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=utcnow, onupdate=utcnow
    )
```

### 変更点

- `server_default=func.now()` → `default=utcnow`: データベースサーバー側ではなく、Pythonアプリケーション側で現在時刻を生成するように変更。これによりSQLiteでもPostgreSQLでも同じ動作になる。

---

## 6. 動作確認

### PostgreSQLでの起動

```bash
uvicorn main:app --reload --port 8000
```

Windows PowerShellの場合:

```powershell
uvicorn main:app --reload --port 8000
```

エラーなく起動することを確認する。ブラウザで http://localhost:8000/ にアクセスし、書籍の登録・表示・更新・削除が全て動作することを確認する。

### テストの実行

テストは引き続きSQLiteで実行する（テスト用DBとしてSQLiteは十分）。`tests/conftest.py` で `DATABASE_URL` を `sqlite:///./test_books.db` に設定しているため、`.env` にPostgreSQLの接続先を書いていても、テスト時はSQLiteが使われる。

```bash
pytest -v
```

Windows PowerShellの場合:

```powershell
pytest -v
```

全てのテストがPASSEDになることを確認する。

### PostgreSQLにデータが入っていることの確認

```bash
psql -U bookuser -d bookmanager -h localhost
```

Windows PowerShellの場合:

```powershell
psql -U bookuser -d bookmanager -h localhost
```

```sql
SELECT * FROM books;
\q
```

ブラウザから登録した書籍がPostgreSQLに保存されていることを確認する。

---

## 7. 本日の確認ポイント

1. PostgreSQLにデータベースとユーザーが作成されている
2. `.env` ファイルで `DATABASE_URL` を管理している
3. アプリケーションがPostgreSQL経由で動作している
4. `.env` を削除してもSQLiteにフォールバックする（確認は任意）
5. テストが全てPASSEDである

---

## Day 7 完了時のファイル構成

```
book-manager/
├── venv/
├── main.py
├── database.py          ← 更新（環境変数対応）
├── models.py            ← 更新（utcnow対応）
├── schemas.py
├── crud.py
├── routers/
│   ├── __init__.py
│   └── books.py
├── templates/
│   └── index.html
├── static/
│   └── style.css
├── tests/
│   ├── __init__.py
│   ├── conftest.py
│   ├── test_crud.py
│   └── test_api.py
├── .env                 ← 新規（Git管理外）
├── .gitignore           ← 新規
├── books.db
└── requirements.txt     ← 更新
```
