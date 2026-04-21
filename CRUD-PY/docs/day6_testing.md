# Day 6: テスト実装（Python/FastAPI版）

## 学習目標

- pytestを使った自動テストの基本を理解する
- CRUD操作の単体テストを作成できる
- FastAPIのTestClientを使ったAPI結合テストを作成できる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| pytest導入・テスト設計 | 10分 |
| 単体テスト作成 | 20分 |
| API結合テスト作成 | 20分 |
| テスト実行・修正 | 10分 |

---

## 1. テスト用パッケージの追加

**requirements.txt** に以下を追加する。

```
fastapi==0.115.0
uvicorn==0.30.0
sqlalchemy==2.0.35
jinja2==3.1.4
python-multipart==0.0.12
pytest==8.3.0
httpx==0.27.0
```

インストールを実行する。

```bash
pip install -r requirements.txt
```

Windows PowerShellの場合:

```powershell
python -m pip install -r requirements.txt
```

- `pytest`: Python標準のテストフレームワーク。テスト関数を自動検出して実行する。
- `httpx`: FastAPIのTestClientが内部で使うHTTPクライアントライブラリ。

---

## 2. テストディレクトリの作成

```bash
mkdir tests
```

Windows PowerShellの場合:

```powershell
New-Item -Path "tests" -ItemType Directory
```

**tests/__init__.py** を空ファイルとして作成する。

```bash
touch tests/__init__.py
```

Windows PowerShellの場合:

```powershell
New-Item -Path "tests\__init__.py" -ItemType File
```

---

## 3. テスト用のデータベース設定

本番のデータベース（`books.db`）をテストで使ってしまうと、テストのたびにデータが変わってしまう。テスト専用のデータベースを使うように設定する。

**tests/conftest.py** を新規作成する。

```python
import os

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from fastapi.testclient import TestClient

TEST_DATABASE_URL = "sqlite:///./test_books.db"
os.environ["DATABASE_URL"] = TEST_DATABASE_URL

from database import Base, get_db
from main import app

test_engine = create_engine(
    TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
)
TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)


@pytest.fixture(autouse=True)
def setup_database():
    Base.metadata.create_all(bind=test_engine)
    yield
    Base.metadata.drop_all(bind=test_engine)


@pytest.fixture
def db_session():
    session = TestSessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def client(setup_database):
    def override_get_db():
        session = TestSessionLocal()
        try:
            yield session
        finally:
            session.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
```

### コード解説

- `@pytest.fixture`: pytestの「フィクスチャ」。テスト関数に必要な前準備や後始末を定義する。テスト関数の引数に名前を書くだけで自動的に注入される。
- `autouse=True`: 全てのテストで自動的にこのフィクスチャが実行される。
- `setup_database`: 各テストの前にテーブルを作成し（`create_all`）、テスト後にテーブルを削除する（`drop_all`）。`yield` の前が前準備、後が後始末。
- `os.environ["DATABASE_URL"] = TEST_DATABASE_URL`: `.env` にPostgreSQLの接続先が書かれていても、テスト実行時はSQLiteのテスト用DBを使うように固定する。
- `app.dependency_overrides[get_db] = override_get_db`: FastAPIの依存性注入をテスト用に差し替える。本番用の `get_db` の代わりにテスト用の `override_get_db` が呼ばれるようになる。

---

## 4. 単体テストの作成

CRUD関数を直接テストする。

**tests/test_crud.py** を新規作成する。

```python
from crud import create_book, get_books, get_book, update_book, delete_book
from schemas import BookCreate, BookUpdate


def test_create_book(db_session):
    book_data = BookCreate(
        title="テスト書籍",
        author="テスト著者",
        publisher="テスト出版社",
    )
    book = create_book(db_session, book_data)

    assert book.id is not None
    assert book.title == "テスト書籍"
    assert book.author == "テスト著者"
    assert book.publisher == "テスト出版社"


def test_get_books_empty(db_session):
    books = get_books(db_session)
    assert books == []


def test_get_books_after_create(db_session):
    create_book(db_session, BookCreate(title="本A", author="著者A"))
    create_book(db_session, BookCreate(title="本B", author="著者B"))

    books = get_books(db_session)
    assert len(books) == 2


def test_get_book_by_id(db_session):
    created = create_book(
        db_session, BookCreate(title="検索対象", author="検索著者")
    )
    found = get_book(db_session, created.id)

    assert found is not None
    assert found.title == "検索対象"


def test_get_book_not_found(db_session):
    result = get_book(db_session, 9999)
    assert result is None


def test_update_book(db_session):
    created = create_book(
        db_session, BookCreate(title="更新前", author="著者")
    )
    updated = update_book(
        db_session, created.id, BookUpdate(title="更新後")
    )

    assert updated is not None
    assert updated.title == "更新後"
    assert updated.author == "著者"


def test_update_book_not_found(db_session):
    result = update_book(db_session, 9999, BookUpdate(title="存在しない"))
    assert result is None


def test_delete_book(db_session):
    created = create_book(
        db_session, BookCreate(title="削除対象", author="著者")
    )
    result = delete_book(db_session, created.id)

    assert result is True
    assert get_book(db_session, created.id) is None


def test_delete_book_not_found(db_session):
    result = delete_book(db_session, 9999)
    assert result is False
```

### コード解説

- `def test_xxx(db_session)`: 関数名が `test_` で始まるものをpytestが自動的にテスト関数として検出する。引数 `db_session` はフィクスチャから自動注入される。
- `assert`: テストの検証文。条件が `False` の場合にテストが失敗する。
- 各テストは独立している。`setup_database` フィクスチャにより、テストごとにデータベースが初期化される。

---

## 5. API結合テストの作成

HTTPリクエストを模倣してAPIの動作をテストする。

**tests/test_api.py** を新規作成する。

```python
def test_health_check(client):
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_create_book_via_api(client):
    res = client.post("/books/", json={
        "title": "API経由の書籍",
        "author": "API著者",
    })
    assert res.status_code == 201
    data = res.json()
    assert data["title"] == "API経由の書籍"
    assert data["id"] is not None


def test_list_books_via_api(client):
    client.post("/books/", json={"title": "本1", "author": "著者1"})
    client.post("/books/", json={"title": "本2", "author": "著者2"})

    res = client.get("/books/")
    assert res.status_code == 200
    assert len(res.json()) == 2


def test_get_book_via_api(client):
    create_res = client.post("/books/", json={
        "title": "個別取得テスト",
        "author": "著者",
    })
    book_id = create_res.json()["id"]

    res = client.get(f"/books/{book_id}")
    assert res.status_code == 200
    assert res.json()["title"] == "個別取得テスト"


def test_get_book_not_found_via_api(client):
    res = client.get("/books/9999")
    assert res.status_code == 404


def test_update_book_via_api(client):
    create_res = client.post("/books/", json={
        "title": "更新前タイトル",
        "author": "著者",
    })
    book_id = create_res.json()["id"]

    res = client.put(f"/books/{book_id}", json={"title": "更新後タイトル"})
    assert res.status_code == 200
    assert res.json()["title"] == "更新後タイトル"
    assert res.json()["author"] == "著者"


def test_delete_book_via_api(client):
    create_res = client.post("/books/", json={
        "title": "削除対象",
        "author": "著者",
    })
    book_id = create_res.json()["id"]

    res = client.delete(f"/books/{book_id}")
    assert res.status_code == 204

    res = client.get(f"/books/{book_id}")
    assert res.status_code == 404


def test_create_book_validation_error(client):
    res = client.post("/books/", json={"title": "", "author": "著者"})
    assert res.status_code == 422


def test_create_book_missing_field(client):
    res = client.post("/books/", json={"title": "タイトルのみ"})
    assert res.status_code == 422
```

---

## 6. テストの実行

```bash
pytest -v
```

Windows PowerShellの場合:

```powershell
pytest -v
```

`-v` は verbose（詳細表示）オプション。各テストの結果が1行ずつ表示される。

全てのテストが `PASSED` と表示されれば成功。

```
tests/test_crud.py::test_create_book PASSED
tests/test_crud.py::test_get_books_empty PASSED
...
tests/test_api.py::test_health_check PASSED
tests/test_api.py::test_create_book_via_api PASSED
...

==================== 17 passed ====================
```

テストが失敗した場合は、エラーメッセージを読んで原因を特定し修正する。

---

## 7. 本日の確認ポイント

1. `pytest -v` で全てのテストがPASSEDになる
2. 単体テスト（crud関数の直接テスト）とAPIテスト（HTTPリクエストのテスト）の違いを説明できる
3. フィクスチャによるテストデータベースの初期化の仕組みを理解している
4. `dependency_overrides` による依存性の差し替えを理解している

---

## Day 6 完了時のファイル構成

```
book-manager/
├── venv/
├── main.py
├── database.py
├── models.py
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
│   ├── __init__.py      ← 新規
│   ├── conftest.py      ← 新規
│   ├── test_crud.py     ← 新規
│   └── test_api.py      ← 新規
├── books.db
├── test_books.db        ← テスト実行時に自動生成
└── requirements.txt     ← 更新
```
