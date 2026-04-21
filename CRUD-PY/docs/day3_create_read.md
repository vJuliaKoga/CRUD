# Day 3: Create / Read API実装（Python/FastAPI版）

## 学習目標

- CRUD操作関数を作成し、データベースへの登録・取得処理を実装できる
- FastAPIのルーター分割を理解し、エンドポイントを構造的に整理できる
- curlコマンドでPOST/GETリクエストを送り、動作を確認できる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| crud.py 作成 | 15分 |
| ルーター作成 | 20分 |
| main.py更新・動作確認 | 25分 |

---

## 1. CRUD操作関数の作成

データベース操作のロジックを `crud.py` にまとめる。APIのルーティング処理（routers/）とデータベース操作（crud.py）を分離することで、コードの見通しが良くなる。

**crud.py** を新規作成する。

```python
from sqlalchemy.orm import Session

from models import Book
from schemas import BookCreate


def get_books(db: Session, skip: int = 0, limit: int = 100) -> list[Book]:
    return db.query(Book).offset(skip).limit(limit).all()


def get_book(db: Session, book_id: int) -> Book | None:
    return db.query(Book).filter(Book.id == book_id).first()


def create_book(db: Session, book_data: BookCreate) -> Book:
    db_book = Book(
        title=book_data.title,
        author=book_data.author,
        publisher=book_data.publisher,
        published_date=book_data.published_date,
        isbn=book_data.isbn,
    )
    db.add(db_book)
    db.commit()
    db.refresh(db_book)
    return db_book
```

### コード解説

- `db.query(Book)`: SQLAlchemyのクエリビルダー。`SELECT * FROM books` に相当する。
- `.offset(skip).limit(limit)`: ページネーション用。`skip` 件飛ばして `limit` 件取得する。
- `.filter(Book.id == book_id)`: WHERE句に相当。`Book.id == book_id` という条件でフィルタリングする。
- `.first()`: 最初の1件を返す。該当なしの場合は `None` を返す。
- `db.add(db_book)`: セッションに新しいオブジェクトを追加する（まだDBには書き込まれない）。
- `db.commit()`: セッション内の変更をデータベースに確定（コミット）する。
- `db.refresh(db_book)`: コミット後、DBから最新の値（自動採番されたIDや自動設定されたcreated_at等）を取得してオブジェクトに反映する。

---

## 2. ルーターの作成

エンドポイントの定義を `main.py` に全て書くと、機能が増えるにつれてファイルが肥大化する。FastAPIの `APIRouter` を使って、書籍関連のエンドポイントを別ファイルに分離する。

ルーター用のディレクトリを作成する。

```bash
mkdir routers
```

Windows PowerShellの場合:

```powershell
New-Item -Path "routers" -ItemType Directory
```

**routers/__init__.py** を空ファイルとして作成する。

```bash
touch routers/__init__.py
```

Windows PowerShellの場合:

```powershell
New-Item -Path "routers\__init__.py" -ItemType File
```

**routers/books.py** を新規作成する。

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from database import get_db
from schemas import BookCreate, BookResponse as BookResponseSchema
from crud import get_books, get_book, create_book

router = APIRouter(prefix="/books", tags=["books"])


@router.get("/", response_model=list[BookResponseSchema])
def list_books(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    books = get_books(db, skip=skip, limit=limit)
    return books


@router.get("/{book_id}", response_model=BookResponseSchema)
def read_book(book_id: int, db: Session = Depends(get_db)):
    book = get_book(db, book_id)
    if book is None:
        raise HTTPException(status_code=404, detail="Book not found")
    return book


@router.post("/", response_model=BookResponseSchema, status_code=201)
def add_book(book_data: BookCreate, db: Session = Depends(get_db)):
    return create_book(db, book_data)
```

### コード解説

- `APIRouter(prefix="/books", tags=["books"])`: このルーターに登録されるエンドポイントは全て `/books` から始まる。`tags` はSwagger UIでのグループ分けに使われる。
- `response_model=list[BookResponseSchema]`: レスポンスの型を指定する。FastAPIがこの型に合わせてレスポンスを自動変換・検証する。
- `Depends(get_db)`: 依存性注入。FastAPIがリクエストごとに `get_db()` を呼び出し、その戻り値を引数 `db` に渡してくれる。リクエスト処理後に自動で `db.close()` が呼ばれる。
- `HTTPException(status_code=404, ...)`: 該当する書籍が見つからない場合、HTTP 404エラーを返す。
- `status_code=201`: 登録成功時のレスポンスコード。201は「Created（リソースが作成された）」を意味する。

---

## 3. main.pyの更新

ルーターを `main.py` に登録する。

**main.py** を以下の内容で上書きする。

```python
from fastapi import FastAPI

from database import engine
from models import Base
from routers import books

Base.metadata.create_all(bind=engine)

app = FastAPI(title="書籍管理システム", version="1.0.0")

app.include_router(books.router)


@app.get("/health")
def health_check():
    return {"status": "ok", "message": "Book Manager API is running"}


@app.get("/")
def root():
    return {"message": "Welcome to Book Manager"}
```

### コード解説

- `app.include_router(books.router)`: 先ほど作成したルーターをアプリケーションに登録する。これにより `/books` 以下のエンドポイントが有効になる。

---

## 4. 動作確認

サーバーを起動する。

```bash
uvicorn main:app --reload --port 8000
```

Windows PowerShellの場合:

```powershell
uvicorn main:app --reload --port 8000
```

### 4-1. 書籍の登録（Create）

```bash
curl -X POST http://localhost:8000/books/ \
  -H "Content-Type: application/json" \
  -d '{"title": "トム・ソーヤーの冒険", "author": "マーク・トウェイン", "publisher": "新潮文庫"}'
```

Windows PowerShellの場合:

```powershell
Invoke-WebRequest -Uri "http://localhost:8000/books/" -Method POST -ContentType "application/json" -Body '{"title": "トム・ソーヤーの冒険", "author": "マーク・トウェイン", "publisher": "新潮文庫"}'
```

期待されるレスポンス（status_code: 201）:

```json
{
  "id": 1,
  "title": "トム・ソーヤーの冒険",
  "author": "マーク・トウェイン",
  "publisher": "新潮文庫",
  "published_date": null,
  "isbn": null,
  "created_at": "2025-04-01T10:00:00",
  "updated_at": "2025-04-01T10:00:00"
}
```

もう1冊登録する。

```bash
curl -X POST http://localhost:8000/books/ \
  -H "Content-Type: application/json" \
  -d '{"title": "ハックルベリー・フィンの冒険", "author": "マーク・トウェイン", "publisher": "岩波文庫", "isbn": "9784003234020"}'
```

Windows PowerShellの場合:

```powershell
Invoke-WebRequest -Uri "http://localhost:8000/books/" -Method POST -ContentType "application/json" -Body '{"title": "ハックルベリー・フィンの冒険", "author": "マーク・トウェイン", "publisher": "岩波文庫", "isbn": "9784003234020"}'
```

### 4-2. 書籍一覧の取得（Read - List）

```bash
curl http://localhost:8000/books/
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/"
```

登録した2冊が配列で返ってくることを確認する。

### 4-3. 書籍の個別取得（Read - Detail）

```bash
curl http://localhost:8000/books/1
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/1"
```

ID=1の書籍が返ってくることを確認する。

### 4-4. 存在しないIDの取得

```bash
curl http://localhost:8000/books/999
```

Windows PowerShellの場合:

```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8000/books/999"
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

期待されるレスポンス（status_code: 404）:

```json
{"detail": "Book not found"}
```

### 4-5. Swagger UIで確認

ブラウザで http://localhost:8000/docs にアクセスし、登録したエンドポイントが表示されることを確認する。各エンドポイントの「Try it out」ボタンからブラウザ上でもAPIを試すことができる。

---

## 5. 本日の確認ポイント

1. `POST /books/` で書籍を登録でき、IDが自動採番される
2. `GET /books/` で登録済みの全書籍が配列で返る
3. `GET /books/{id}` で指定したIDの書籍が返る
4. 存在しないIDを指定すると404エラーが返る
5. `Depends(get_db)` による依存性注入の仕組みを説明できる

---

## Day 3 完了時のファイル構成

```
book-manager/
├── venv/
├── main.py              ← 更新
├── database.py
├── models.py
├── schemas.py
├── crud.py              ← 新規
├── routers/
│   ├── __init__.py      ← 新規
│   └── books.py         ← 新規
├── books.db
└── requirements.txt
```
