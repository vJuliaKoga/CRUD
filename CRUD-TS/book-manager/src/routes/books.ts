import { Router, Request, Response } from "express";
import prisma from "../database";
import { validateBookCreate, validateBookUpdate } from "../utils/validation";

const router = Router();

// 書籍一覧取得
router.get("/", async (req: Request, res: Response) => {
  const books = await prisma.book.findMany({
    orderBy: { id: "asc" },
  });
  res.json(books);
});

// 書籍個別取得
router.get("/:id", async (req: Request, res: Response) => {
  const id = parseInt(req.params.id as string, 10);

  if (isNaN(id)) {
    res.status(400).json({ detail: "Invalid ID" });
    return;
  }

  const book = await prisma.book.findUnique({
    where: { id },
  });

  if (!book) {
    res.status(404).json({ detail: "Book not found" });
    return;
  }
  res.json(book);
});

// 書籍登録
router.post("/", async (req: Request, res: Response) => {
  const errors = validateBookCreate(req.body);
  if (errors.length > 0) {
    res.status(422).json({ status: 422, error: "Validation Error", details: errors });
    return;
  }

  const { title, author, publisher, publishedDate, isbn } = req.body;

  const book = await prisma.book.create({
    data: {
      title,
      author,
      publisher: publisher || null,
      publishedDate: publishedDate || null,
      isbn: isbn || null,
    },
  });
  res.status(201).json(book);
});

// 書籍更新
router.put("/:id", async (req: Request, res: Response) => {
  const id = parseInt(req.params.id as string, 10);

  if (isNaN(id)) {
    res.status(400).json({ detail: "Invalid ID" });
    return;
  }

  const errors = validateBookUpdate(req.body);
  if (errors.length > 0) {
    res.status(422).json({ status: 422, error: "Validation Error", details: errors });
    return;
  }

  const existing = await prisma.book.findUnique({ where: { id } });
  if (!existing) {
    res.status(404).json({ detail: "Book not found" });
    return;
  }

  const updateData: any = {};
  if (req.body.title !== undefined) updateData.title = req.body.title;
  if (req.body.author !== undefined) updateData.author = req.body.author;
  if (req.body.publisher !== undefined) updateData.publisher = req.body.publisher || null;
  if (req.body.publishedDate !== undefined) updateData.publishedDate = req.body.publishedDate || null;
  if (req.body.isbn !== undefined) updateData.isbn = req.body.isbn || null;

  const book = await prisma.book.update({
    where: { id },
    data: updateData,
  });
  res.json(book);
});

// 書籍削除
router.delete("/:id", async (req: Request, res: Response) => {
  const id = parseInt(req.params.id as string, 10);

  if (isNaN(id)) {
    res.status(400).json({ detail: "Invalid ID" });
    return;
  }

  const existing = await prisma.book.findUnique({ where: { id } });
  if (!existing) {
    res.status(404).json({ detail: "Book not found" });
    return;
  }

  await prisma.book.delete({ where: { id } });
  res.status(204).send();
});

export default router;
