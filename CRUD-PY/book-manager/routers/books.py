from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from crud import get_books, get_book, create_book, update_book, delete_book
from database import get_db
from schemas import BookCreate, BookResponse as BookResponseSchema, BookUpdate

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


@router.put("/{book_id}", response_model=BookResponseSchema)
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
