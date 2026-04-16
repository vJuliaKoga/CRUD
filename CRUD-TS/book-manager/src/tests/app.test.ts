import request from "supertest";
import app from "../app";

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
        .send({ title: "テスト書籍", author: "テスト著者" });

      expect(res.status).toBe(201);
      expect(res.body.title).toBe("テスト書籍");
      expect(res.body.author).toBe("テスト著者");
      expect(res.body.id).toBeDefined();
    });

    test("タイトル未指定は422エラー", async () => {
      const res = await request(app).post("/books").send({ author: "著者のみ" });

      expect(res.status).toBe(422);
    });

    test("空のタイトルは422エラー", async () => {
      const res = await request(app)
        .post("/books")
        .send({ title: "", author: "テスト著者" });

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
      await request(app).post("/books").send({ title: "本A", author: "著者A" });
      await request(app).post("/books").send({ title: "本B", author: "著者B" });

      const res = await request(app).get("/books");

      expect(res.status).toBe(200);
      expect(res.body).toHaveLength(2);
    });
  });

  describe("GET /books/:id", () => {
    test("個別の書籍を取得できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "個別取得テスト", author: "著者" });

      const res = await request(app).get(`/books/${created.body.id}`);

      expect(res.status).toBe(200);
      expect(res.body.title).toBe("個別取得テスト");
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
        .send({ title: "更新前", author: "著者" });

      const res = await request(app)
        .put(`/books/${created.body.id}`)
        .send({ title: "更新後" });

      expect(res.status).toBe(200);
      expect(res.body.title).toBe("更新後");
      expect(res.body.author).toBe("著者");
    });

    test("存在しないIDは404を返す", async () => {
      const res = await request(app).put("/books/9999").send({ title: "存在しない" });

      expect(res.status).toBe(404);
    });
  });

  describe("DELETE /books/:id", () => {
    test("書籍を削除できる", async () => {
      const created = await request(app)
        .post("/books")
        .send({ title: "削除対象", author: "著者" });

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
