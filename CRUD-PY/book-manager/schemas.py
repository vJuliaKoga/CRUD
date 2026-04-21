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
