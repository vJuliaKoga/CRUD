from sqlalchemy.orm import Session

from models import Book
from schemas import BookCreate, BookUpdate


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
