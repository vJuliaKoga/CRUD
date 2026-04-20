# Day 4: Update / Delete API実装（Python/FastAPI版）

## 学習目標

- 書籍の更新処理と削除処理を実装できる
- リクエストバリデーション（入力値検証）を追加できる
- CRUDの全4操作が揃い、APIとして完成する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| crud.py にupdate/delete追加 | 15分 |
| ルーターにエンドポイント追加 | 15分 |
| バリデーション追加 | 15分 |
| 動作確認 | 15分 |

---

## 1. crud.pyにUpdate/Delete関数を追加

**crud.py** を開き、末尾に以下の2つの関数を追加する。

```python
from schemas import BookCreate, BookUpdate


def update_book(db: Session, book_id: int, book_data: BookUpdate) -> Book | None:
    db_book = db.query(Book).filter(Book.id == book_id).first()
    if db_book is None:
        return None

    update_fields = book_data.model_dump(exclude_unset=True)
    for key, value in update_fields.items():
        setattr(db_book, key, value)

    db.commit()
    db.refresh(db_book)
    return db_book


def delete_book(db: Session, book_id: int) -> bool:
    db_book = db.query(Book).filter(Book.id == book_id).first()
    if db_book is None:
        return False

    db.delete(db_book)
    db.commit()
    return True
```

なお、ファイル冒頭の import 文を以下のように修正する（`BookUpdate` を追加）。

```python
from sqlalchemy.orm import Session

from models import Book
from schemas import BookCreate, BookUpdate
```

### コード解説

- `book_data.model_dump(exclude_unset=True)`: Pydanticモデルを辞書に変換する。`exclude_unset=True` を指定すると、リクエストで**送信されなかったフィールドは辞書に含まれない**。これにより部分更新（PATCHのような動作）が可能になる。
- `setattr(db_book, key, value)`: Pythonの組み込み関数。オブジェクトの属性を動的に設定する。`db_book.title = "新しいタイトル"` と同じ効果だが、属性名を変数で指定できる。
- `db.delete(db_book)`: セッションからオブジェクトを削除対象としてマークする。`db.commit()` で実際にDELETE文が発行される。

---

## 2. ルーターにUpdate/Deleteエンドポイントを追加

**routers/books.py** を開き、以下のimportとエンドポイントを追加する。

importの修正（`BookUpdate` と `update_book`, `delete_book` を追加）:

```python
from schemas import BookCreate, BookUpdate, BookResponse
from crud import get_books, get_book, create_book, update_book, delete_book
```

ファイル末尾に以下の2つのエンドポイントを追加する。

```python
@router.put("/{book_id}", response_model=BookResponse)
def modify_book(
    book_id: int, book_data: BookUpdate, db: Session = Depends(get_db)
):
    book = update_book(db, book_id, book_data)
    if book is None:
        raise HTTPException(status_code=404, detail="Book not found")
    return book


@router.delete("/{book_id}", status_code=204)
def remove_book(book_id: int, db: Session = Depends(get_db)):
    success = delete_book(db, book_id)
    if not success:
        raise HTTPException(status_code=404, detail="Book not found")
    return None
```

### コード解説

- `@router.put(...)`: HTTP PUTメソッドに対応するエンドポイント。リソースの更新に使う。
- `@router.delete(...)`: HTTP DELETEメソッドに対応するエンドポイント。
- `status_code=204`: 「No Content（成功したがレスポンスボディなし）」を意味する。削除成功時は返すデータがないため204を使う。

---

## 3. バリデーションの追加

現在の `BookCreate` スキーマでは、空文字でもリクエストが通ってしまう。Pydanticの `Field` を使って入力値の検証を追加する。

**schemas.py** を以下のように更新する。

```python
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class BookCreate(BaseModel):
    title: str = Field(..., min_length=1, max_length=200, examples=["リーダブルコード"])
    author: str = Field(..., min_length=1, max_length=100, examples=["Dustin Boswell"])
    publisher: str | None = Field(None, max_length=100)
    published_date: str | None = Field(None, pattern=r"^\d{4}-\d{2}-\d{2}$")
    isbn: str | None = Field(None, pattern=r"^\d{10}(\d{3})?$")


class BookUpdate(BaseModel):
    title: str | None = Field(None, min_length=1, max_length=200)
    author: str | None = Field(None, min_length=1, max_length=100)
    publisher: str | None = Field(None, max_length=100)
    published_date: str | None = Field(None, pattern=r"^\d{4}-\d{2}-\d{2}$")
    isbn: str | None = Field(None, pattern=r"^\d{10}(\d{3})?$")


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

- `Field(...)`: `...` はPydanticで「必須」を意味する特別な値（Ellipsis）。
- `min_length=1`: 最小文字数。空文字を拒否する。
- `max_length=200`: 最大文字数。データベースのカラム長と合わせる。
- `pattern=r"^\d{4}-\d{2}-\d{2}$"`: 正規表現パターン。`published_date` は `YYYY-MM-DD` 形式のみ受け付ける。
- `pattern=r"^\d{10}(\d{3})?$"`: ISBNは10桁または13桁の数字のみ受け付ける。
- `examples=["リーダブルコード"]`: Swagger UIで表示されるサンプル値。

---

## 4. 動作確認

サーバーを再起動する（`--reload` を使っている場合は自動再起動される）。

### 4-1. 書籍の更新（Update）

まず登録されている書籍を確認する。

```bash
curl http://localhost:8000/books/1
```

タイトルと出版社を更新する。

```bash
curl -X PUT http://localhost:8000/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "リーダブルコード 改訂版", "publisher": "オライリー・ジャパン"}'
```

更新後の書籍を確認する。

```bash
curl http://localhost:8000/books/1
```

`title` と `publisher` が更新され、`updated_at` が変わっていることを確認する。`author` など送信しなかったフィールドは元の値が維持されている。

### 4-2. 書籍の削除（Delete）

```bash
curl -X DELETE http://localhost:8000/books/2
```

レスポンスボディは空で、HTTPステータスコード204が返る。

削除されたことを確認する。

```bash
curl http://localhost:8000/books/2
```

404エラーが返ることを確認する。

### 4-3. バリデーションの確認

空のタイトルで登録を試みる。

```bash
curl -X POST http://localhost:8000/books/ \
  -H "Content-Type: application/json" \
  -d '{"title": "", "author": "テスト"}'
```

422 Validation Error が返ることを確認する。

不正な日付形式で登録を試みる。

```bash
curl -X POST http://localhost:8000/books/ \
  -H "Content-Type: application/json" \
  -d '{"title": "テスト本", "author": "テスト著者", "published_date": "2024/01/01"}'
```

`YYYY-MM-DD` 形式ではないため、こちらも422エラーが返る。

---

## 5. 本日の確認ポイント

1. `PUT /books/{id}` で書籍を部分更新でき、送信しなかったフィールドは変わらない
2. `DELETE /books/{id}` で書籍を削除でき、204が返る
3. 必須フィールドが空のリクエストに対して422エラーが返る
4. 不正な形式のデータに対してバリデーションエラーが返る
5. CRUD全4操作がSwagger UI（`/docs`）で確認できる

---

## Day 4 完了時のファイル構成

```
book-manager/
├── venv/
├── main.py
├── database.py
├── models.py
├── schemas.py           ← 更新（バリデーション追加）
├── crud.py              ← 更新（update/delete追加）
├── routers/
│   ├── __init__.py
│   └── books.py         ← 更新（PUT/DELETE追加）
├── books.db
└── requirements.txt
```

これでCRUDのAPI実装は完了である。Day 5ではブラウザから操作できるHTML画面を追加する。
