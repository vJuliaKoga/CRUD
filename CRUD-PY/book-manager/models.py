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
