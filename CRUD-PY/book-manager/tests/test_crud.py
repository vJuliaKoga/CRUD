from crud import create_book, delete_book, get_book, get_books, update_book
from schemas import BookCreate, BookUpdate


def test_create_book(db_session):
    book_data = BookCreate(
        title="トム・ソーヤーの冒険",
        author="マーク・トウェイン",
        publisher="新潮文庫",
    )
    book = create_book(db_session, book_data)

    assert book.id is not None
    assert book.title == "トム・ソーヤーの冒険"
    assert book.author == "マーク・トウェイン"
    assert book.publisher == "新潮文庫"


def test_get_books_empty(db_session):
    books = get_books(db_session)
    assert books == []


def test_get_books_after_create(db_session):
    create_book(
        db_session,
        BookCreate(title="トム・ソーヤーの冒険", author="マーク・トウェイン"),
    )
    create_book(
        db_session,
        BookCreate(title="ハックルベリー・フィンの冒険", author="マーク・トウェイン"),
    )

    books = get_books(db_session)
    assert len(books) == 2


def test_get_book_by_id(db_session):
    created = create_book(
        db_session,
        BookCreate(title="トム・ソーヤーの冒険", author="マーク・トウェイン"),
    )
    found = get_book(db_session, created.id)

    assert found is not None
    assert found.title == "トム・ソーヤーの冒険"


def test_get_book_not_found(db_session):
    result = get_book(db_session, 9999)
    assert result is None


def test_update_book(db_session):
    created = create_book(
        db_session,
        BookCreate(title="トム・ソーヤーの冒険", author="マーク・トウェイン"),
    )
    updated = update_book(
        db_session, created.id, BookUpdate(title="トム・ソーヤーの冒険 改訂版")
    )

    assert updated is not None
    assert updated.title == "トム・ソーヤーの冒険 改訂版"
    assert updated.author == "マーク・トウェイン"


def test_update_book_not_found(db_session):
    result = update_book(
        db_session, 9999, BookUpdate(title="存在しない書籍")
    )
    assert result is None


def test_delete_book(db_session):
    created = create_book(
        db_session,
        BookCreate(title="ハックルベリー・フィンの冒険", author="マーク・トウェイン"),
    )
    result = delete_book(db_session, created.id)

    assert result is True
    assert get_book(db_session, created.id) is None


def test_delete_book_not_found(db_session):
    result = delete_book(db_session, 9999)
    assert result is False
