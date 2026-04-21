# Day 2: データベース設計・モデル作成（Python/FastAPI版）

## 学習目標

- リレーショナルデータベースとSQLの基本概念を理解する
- SQLAlchemy ORMを使ってデータベーステーブルをPythonクラスとして定義できる
- Pydanticスキーマによるリクエスト/レスポンスの型定義を理解する

## 所要時間の目安

| 作業                         | 時間 |
| ---------------------------- | ---- |
| DB概念の理解・パッケージ追加 | 10分 |
| database.py / models.py 作成 | 25分 |
| schemas.py 作成              | 15分 |
| 動作確認                     | 10分 |

---

## 1. 依存パッケージの追加

`requirements.txt` を以下のように更新する。

**requirements.txt**

```
fastapi==0.115.0
uvicorn==0.30.0
sqlalchemy==2.0.35
```

追加したパッケージをインストールする。

```bash
pip install -r requirements.txt
```

Windows PowerShellの場合:

```powershell
python -m pip install -r requirements.txt
```

### SQLAlchemyとは

SQLAlchemy は Python の ORM（Object-Relational Mapping）ライブラリ。
ORMとは、データベースのテーブルをPythonのクラスとして扱い、SQL文を直接書かずにPythonコードでデータベース操作を行う仕組み。

例えば、SQLで `INSERT INTO books (title, author) VALUES ('テスト', '太郎')` と書く代わりに、Pythonで `Book(title="テスト", author="太郎")` のようにオブジェクトを作成してデータベースに保存できる。

---

## 2. データベース接続設定の作成

**database.py** を新規作成する。

```python
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase

SQLALCHEMY_DATABASE_URL = "sqlite:///./books.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
)

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

- `create_engine`: データベースへの接続を管理するエンジンを作成する。`sqlite:///./books.db` は「現在のディレクトリに `books.db` というSQLiteファイルを作成/使用する」という意味。
- `connect_args={"check_same_thread": False}`: SQLiteは本来シングルスレッドだが、FastAPIはマルチスレッドで動くため、この設定でスレッド制限を解除する。
- `SessionLocal`: データベースとの「会話」を管理するセッションのファクトリ。1回のAPIリクエストにつき1つのセッションを使う。
- `Base`: 全てのモデルクラスの親クラス。このクラスを継承してテーブル定義を行う。
- `get_db()`: FastAPIの依存性注入（Dependency Injection）で使う関数。`yield` を使うことで、リクエスト処理後に必ず `db.close()` が呼ばれることを保証する。

---

## 3. モデルの作成

**models.py** を新規作成する。

```python
from datetime import datetime

from sqlalchemy import String, DateTime, func
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class Book(Base):
    __tablename__ = "books"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    author: Mapped[str] = mapped_column(String(100), nullable=False)
    publisher: Mapped[str | None] = mapped_column(String(100), nullable=True)
    published_date: Mapped[str | None] = mapped_column(String(10), nullable=True)
    isbn: Mapped[str | None] = mapped_column(String(13), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now()
    )
```

### コード解説

- `__tablename__ = "books"`: データベース上のテーブル名を指定する。
- `Mapped[int]`: SQLAlchemy 2.0の新しい型アノテーション方式。Pythonの型ヒントとSQLAlchemyのカラム定義を統合している。
- `mapped_column(primary_key=True, autoincrement=True)`: 主キーかつ自動採番。データ登録時にIDを指定する必要がない。
- `nullable=False`: NULL（空値）を許可しない。つまり必須項目。
- `Mapped[str | None]`: `None` を許容する型。Python 3.10以降の Union 記法。
- `server_default=func.now()`: データベースサーバー側で現在時刻を自動設定する。
- `onupdate=func.now()`: レコード更新時に自動で現在時刻を設定する。

---

## 4. スキーマの作成

FastAPIでは、Pydanticという型検証ライブラリを使って、APIのリクエストとレスポンスの形式（スキーマ）を定義する。ORMのモデル（models.py）は「データベースのテーブル構造」であるのに対し、スキーマ（schemas.py）は「APIでやり取りするデータの形式」を表す。

**schemas.py** を新規作成する。

```python
from datetime import datetime

from pydantic import BaseModel, ConfigDict


class BookCreate(BaseModel):
    title: str
    author: str
    publisher: str | None = None
    published_date: str | None = None
    isbn: str | None = None


class BookUpdate(BaseModel):
    title: str | None = None
    author: str | None = None
    publisher: str | None = None
    published_date: str | None = None
    isbn: str | None = None


class BookResponse(BaseModel):
    id: int
    title: str
    author: str
    publisher: str | None
    published_date: str | None
    isbn: str | None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
```

### コード解説

- `BookCreate`: 書籍を新規登録する際のリクエスト形式。`title` と `author` は必須、それ以外は任意。
- `BookUpdate`: 書籍を更新する際のリクエスト形式。全てのフィールドが任意（部分更新に対応）。
- `BookResponse`: APIからのレスポンス形式。`id`, `created_at`, `updated_at` はサーバー側で自動設定されるため、リクエストには含めない。
- `model_config = ConfigDict(from_attributes=True)`: SQLAlchemyのモデルオブジェクトから自動的にPydanticモデルに変換するための設定。

### なぜモデルとスキーマを分けるのか

実務では、データベースに保存する情報とAPIで公開する情報は異なることが多い。例えばパスワードはDBに保存するがAPIレスポンスには含めない。この分離により、内部構造の変更がAPIに影響しにくくなる。

---

## 5. main.pyの更新

`main.py` を以下のように更新する。テーブル自動作成の処理を追加する。

```python
from fastapi import FastAPI

from database import engine
from models import Base

Base.metadata.create_all(bind=engine)

app = FastAPI(title="書籍管理システム", version="1.0.0")


@app.get("/health")
def health_check():
    return {"status": "ok", "message": "Book Manager API is running"}


@app.get("/")
def root():
    return {"message": "Welcome to Book Manager"}
```

### コード解説

- `Base.metadata.create_all(bind=engine)`: `Base` を継承した全てのモデルクラスに対応するテーブルを、データベースに自動作成する。テーブルが既に存在する場合は何もしない。

---

## 6. 動作確認

サーバーを起動する。

```bash
uvicorn main:app --reload --port 8000
```

Windows PowerShellの場合:

```powershell
uvicorn main:app --reload --port 8000
```

起動後、プロジェクトディレクトリに `books.db` というファイルが生成されていることを確認する。

```bash
ls -la books.db
```

Windows PowerShellの場合:

```powershell
Get-Item books.db
```

SQLiteのCLIツールがインストールされている場合は、テーブルが作成されていることを確認できる。

```bash
sqlite3 books.db ".tables"
```

Windows PowerShellの場合:

```powershell
sqlite3 books.db ".tables"
```

`books` と表示されれば成功。

```bash
sqlite3 books.db ".schema books"
```

Windows PowerShellの場合:

```powershell
sqlite3 books.db ".schema books"
```

テーブルのカラム定義が表示される。

---

## 7. 本日の確認ポイント

1. `database.py`, `models.py`, `schemas.py` の3ファイルが作成されている
2. サーバー起動時に `books.db` ファイルが自動生成される
3. ORMモデルとPydanticスキーマの違いを説明できる
4. 各フィールドの `nullable` / 必須の意図を理解している

---

## Day 2 完了時のファイル構成

```
book-manager/
├── venv/
├── main.py
├── database.py          ← 新規
├── models.py            ← 新規
├── schemas.py           ← 新規
├── books.db             ← 自動生成
└── requirements.txt     ← 更新
```

---

## 自習課題（余裕がある場合）

- SQLAlchemy公式ドキュメントの「Mapped Column」セクションを読む
- Pydantic公式ドキュメント（https://docs.pydantic.dev/）の「Models」セクションを読む
- `published_date` を `String` ではなく `Date` 型にする方法を調べてみる
