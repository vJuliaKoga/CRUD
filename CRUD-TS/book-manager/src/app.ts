import express, { NextFunction, Request, Response } from "express";
import path from "path";
import booksRouter from "./routes/books";

const app = express();

app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

app.use("/books", booksRouter);

app.get("/health", (req: Request, res: Response) => {
  res.json({ status: "ok", message: "Book Manager API is running" });
});

app.get("/", (req: Request, res: Response) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
  console.error("Unexpected error:", err);
  res.status(500).json({ detail: "Internal Server Error" });
});

export default app;
