from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from database import engine
from models import Base
from routers import books

Base.metadata.create_all(bind=engine)

app = FastAPI(title="書籍管理システム", version="1.0.0")

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

app.include_router(books.router)


@app.get("/health")
def health_check():
    return {"status": "ok", "message": "Book Manager API is running"}


@app.get("/")
def root(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})
