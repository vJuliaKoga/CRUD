import request from "supertest";
import app from "../app";
import { prisma } from "./setup";

describe("Health Check", () => {
  test("GET /health は200を返す", async () => {
    const res = await request(app).get("/health");
    expect(res.status).toBe(200);
    expect(res.body.status).toBe("ok");
  });
});

describe("Books API", () => {
  describe("POST /books", () => {
    test("書籍を登録できる", async () => {
      const res = await request(app)
        .post("/books")
        .send({ title: "トム・ソーヤーの冒険", author: "マーク・トウェイン" });

      expect(res.status).toBe(201);
      expect(res.body.title).toBe("トム・ソーヤーの冒険");
      expect(res.body.author).toBe("マーク・トウェイン");
      expect(res.body.id).toBeDefined();
    });

    test("タイトル未指定は422エラー", async () => {
      const res = await request(app)
        .post("/books")
        .send({ author: "マーク・トウェイン" });

      expect(res.status).toBe(422);
    });

    test("空のタイトルは422エラー", async () => {
      const res = await request(app)
        .post("/books")
        .send({ title: "", author: "マーク・トウェイン" });

      expect(res.status).toBe(422);
    });
  });

  describe("GET /books", () => {
    test("空の一覧を取得できる", async () => {
      const res = await request(app).get("/books");
      expect(res.status).toBe(200);
      expect(res.body).toEqual([]);
    });

    test("登録後の一覧を取得できる", async () => {
      await request(app)
        .post("/books")
        .send({ title: "トム・ソーヤーの冒険", author: "マーク・トウェイン" });
      await request(app)
        .post("/books")
        .send({ title: "ハックルベリー・フィンの冒険", author: "マーク・トウェイン" });

      const res = await request(app).get("/books");
      expect(res.status).toBe(200);
      expect(res.body).toHaveLength(2);
    });
  });

  describe("GET /books/:id", () => {
    test("個別の書籍を取得できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "トム・ソーヤーの冒険", author: "マーク・トウェイン" });

      const res = await request(app).get(`/books/${created.body.id}`);
      expect(res.status).toBe(200);
      expect(res.body.title).toBe("トム・ソーヤーの冒険");
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app).get("/books/9999");
      expect(res.status).toBe(404);
    });
  });

  describe("PUT /books/:id", () => {
    test("書籍を部分更新できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "トム・ソーヤーの冒険", author: "マーク・トウェイン" });

      const res = await request(app)
        .put(`/books/${created.body.id}`)
        .send({ title: "トム・ソーヤーの冒険 改訂版" });

      expect(res.status).toBe(200);
      expect(res.body.title).toBe("トム・ソーヤーの冒険 改訂版");
      expect(res.body.author).toBe("マーク・トウェイン");
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app)
        .put("/books/9999")
        .send({ title: "存在しない" });

      expect(res.status).toBe(404);
    });
  });

  describe("DELETE /books/:id", () => {
    test("書籍を削除できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "ハックルベリー・フィンの冒険", author: "マーク・トウェイン" });

      const deleteRes = await request(app).delete(`/books/${created.body.id}`);
      expect(deleteRes.status).toBe(204);

      const getRes = await request(app).get(`/books/${created.body.id}`);
      expect(getRes.status).toBe(404);
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app).delete("/books/9999");

      expect(res.status).toBe(404);
    });
  });
});
