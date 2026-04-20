# Day 6: テスト実装（Java/Spring Boot版）

## 学習目標

- JUnit 5を使った自動テストの基本を理解する
- サービス層の単体テストを作成できる
- MockMvcを使ったAPI結合テストを作成できる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| テスト構成の理解 | 10分 |
| サービス単体テスト作成 | 20分 |
| API結合テスト作成 | 20分 |
| テスト実行・修正 | 10分 |

---

## 1. テスト構成の理解

Spring Bootプロジェクトでは、テストコードは `src/test/java/` 以下に配置する。テスト用の依存関係（JUnit 5, Spring Test, Mockito等）は `pom.xml` の `spring-boot-starter-test` に全て含まれているため、追加のインストールは不要。

テスト用のディレクトリを作成する。

```bash
mkdir -p src/test/java/com/training/bookmanager
```

Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path "src/test/java/com/training/bookmanager"
```

### テスト用application.properties

テスト時はインメモリH2データベースを使う。テスト専用の設定ファイルを作成する。

```bash
mkdir -p src/test/resources
```

Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path "src/test/resources"
```

**src/test/resources/application.properties** を新規作成する。

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

`jdbc:h2:mem:testdb` はインメモリデータベースであり、テスト終了後にデータが自動的に破棄される。`create-drop` はテスト開始時にテーブルを作成し、終了時に削除する設定。

---

## 2. サービス層の単体テスト

**src/test/java/com/training/bookmanager/BookServiceTest.java** を新規作成する。

```java
package com.training.bookmanager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.training.bookmanager.dto.BookRequest;
import com.training.bookmanager.dto.BookUpdateRequest;
import com.training.bookmanager.model.Book;
import com.training.bookmanager.repository.BookRepository;
import com.training.bookmanager.service.BookService;

@SpringBootTest
class BookServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void testCreateBook() {
        BookRequest request = new BookRequest();
        request.setTitle("テスト書籍");
        request.setAuthor("テスト著者");
        request.setPublisher("テスト出版社");

        Book created = bookService.create(request);

        assertNotNull(created.getId());
        assertEquals("テスト書籍", created.getTitle());
        assertEquals("テスト著者", created.getAuthor());
        assertEquals("テスト出版社", created.getPublisher());
    }

    @Test
    void testFindAllEmpty() {
        List<Book> books = bookService.findAll();
        assertTrue(books.isEmpty());
    }

    @Test
    void testFindAllAfterCreate() {
        BookRequest req1 = new BookRequest();
        req1.setTitle("本A");
        req1.setAuthor("著者A");
        bookService.create(req1);

        BookRequest req2 = new BookRequest();
        req2.setTitle("本B");
        req2.setAuthor("著者B");
        bookService.create(req2);

        List<Book> books = bookService.findAll();
        assertEquals(2, books.size());
    }

    @Test
    void testFindById() {
        BookRequest request = new BookRequest();
        request.setTitle("検索対象");
        request.setAuthor("検索著者");
        Book created = bookService.create(request);

        Book found = bookService.findById(created.getId());
        assertNotNull(found);
        assertEquals("検索対象", found.getTitle());
    }

    @Test
    void testFindByIdNotFound() {
        Book result = bookService.findById(9999L);
        assertNull(result);
    }

    @Test
    void testUpdateBook() {
        BookRequest request = new BookRequest();
        request.setTitle("更新前");
        request.setAuthor("著者");
        Book created = bookService.create(request);

        BookUpdateRequest updateReq = new BookUpdateRequest();
        updateReq.setTitle("更新後");
        Book updated = bookService.update(created.getId(), updateReq);

        assertNotNull(updated);
        assertEquals("更新後", updated.getTitle());
        assertEquals("著者", updated.getAuthor());
    }

    @Test
    void testUpdateBookNotFound() {
        BookUpdateRequest updateReq = new BookUpdateRequest();
        updateReq.setTitle("存在しない");
        Book result = bookService.update(9999L, updateReq);
        assertNull(result);
    }

    @Test
    void testDeleteBook() {
        BookRequest request = new BookRequest();
        request.setTitle("削除対象");
        request.setAuthor("著者");
        Book created = bookService.create(request);

        boolean result = bookService.delete(created.getId());
        assertTrue(result);
        assertNull(bookService.findById(created.getId()));
    }

    @Test
    void testDeleteBookNotFound() {
        boolean result = bookService.delete(9999L);
        assertFalse(result);
    }
}
```

### コード解説

- `@SpringBootTest`: Spring Bootのアプリケーションコンテキスト全体を読み込んでテストする。実際のDB接続やDIが行われる。
- `@Autowired`: Springの依存性注入。テストクラスでもサービスやリポジトリのインスタンスを注入できる。
- `@BeforeEach`: 各テストメソッドの実行前に呼ばれる。データベースを初期化して、テスト間の影響を排除する。
- `@Test`: JUnit 5のテストメソッドアノテーション。pytestの `test_` プレフィックスに相当。
- `assertEquals(期待値, 実際の値)`: 2つの値が等しいことを検証する。
- `assertNotNull(値)`: 値がnullでないことを検証する。
- `assertTrue(条件)` / `assertFalse(条件)`: 条件がtrue/falseであることを検証する。

---

## 3. API結合テスト

MockMvcを使って、実際のHTTPリクエストを模倣してAPIの動作をテストする。

**src/test/java/com/training/bookmanager/BookApiControllerTest.java** を新規作成する。

```java
package com.training.bookmanager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.training.bookmanager.repository.BookRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testCreateBook() throws Exception {
        String json = """
                {"title": "API経由の書籍", "author": "API著者"}
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("API経由の書籍"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void testListBooks() throws Exception {
        String json1 = """
                {"title": "本1", "author": "著者1"}
                """;
        String json2 = """
                {"title": "本2", "author": "著者2"}
                """;

        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON).content(json1));
        mockMvc.perform(post("/books")
                .contentType(MediaType.APPLICATION_JSON).content(json2));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetBookNotFound() throws Exception {
        mockMvc.perform(get("/books/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBook() throws Exception {
        String createJson = """
                {"title": "更新前タイトル", "author": "著者"}
                """;

        MvcResult result = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // IDを簡易的に抽出
        String idStr = responseBody.split("\"id\":")[1].split(",")[0].trim();

        String updateJson = """
                {"title": "更新後タイトル"}
                """;

        mockMvc.perform(put("/books/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("更新後タイトル"))
                .andExpect(jsonPath("$.author").value("著者"));
    }

    @Test
    void testDeleteBook() throws Exception {
        String createJson = """
                {"title": "削除対象", "author": "著者"}
                """;

        MvcResult result = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String idStr = responseBody.split("\"id\":")[1].split(",")[0].trim();

        mockMvc.perform(delete("/books/" + idStr))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/books/" + idStr))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateBookValidationError() throws Exception {
        String json = """
                {"title": "", "author": "著者"}
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testCreateBookMissingField() throws Exception {
        String json = """
                {"title": "タイトルのみ"}
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }
}
```

### コード解説

- `@AutoConfigureMockMvc`: Spring MVCのテスト用HTTPクライアント `MockMvc` を自動設定する。
- `mockMvc.perform(get("/health"))`: GET /health リクエストを模倣する。
- `.andExpect(status().isOk())`: レスポンスのステータスコードが200であることを検証する。
- `.andExpect(jsonPath("$.status").value("ok"))`: レスポンスJSONの `status` フィールドが `"ok"` であることを検証する。`$` はJSONルートを表す。
- テキストブロック `"""..."""`: Java 15以降で使えるヒアドキュメント。複数行の文字列を可読性よく記述できる。

---

## 4. テストの実行

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

全てのテストが成功すると、以下のような出力が表示される。

```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

テストが失敗した場合は、エラーメッセージを読んで原因を特定し修正する。

---

## 5. 本日の確認ポイント

1. `.\mvnw.cmd test` で全てのテストが成功する
2. `@SpringBootTest` によるSpringコンテキスト統合テストの仕組みを理解している
3. `MockMvc` によるHTTPリクエストの模倣方法を理解している
4. `@BeforeEach` によるテストデータの初期化の重要性を理解している

---

## Day 6 完了時のファイル構成

```
book-manager/
├── src/test/java/com/training/bookmanager/
│   ├── BookManagerApplicationTests.java  # 既存
│   ├── BookServiceTest.java             ← 新規
│   └── BookApiControllerTest.java       ← 新規
├── src/test/resources/
│   └── application.properties           ← 新規
└── ...
```
