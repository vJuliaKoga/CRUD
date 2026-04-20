# Day 3: Create / Read API実装（Java/Spring Boot版）

## 学習目標

- サービス層でビジネスロジックを実装し、コントローラと分離できる
- REST APIエンドポイント（POST / GET）を実装できる
- curlコマンドでAPIの動作を確認できる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| サービスクラス作成 | 20分 |
| APIコントローラ作成 | 20分 |
| 動作確認 | 20分 |

---

## 1. サービスクラスの作成

Spring Bootでは、ビジネスロジック（データの加工・検証・取得などの処理）をサービス層に配置する。コントローラはHTTPリクエストの受付とレスポンスの返却に専念し、サービスがデータ操作の実処理を担当する。

**src/main/java/com/training/bookmanager/service/BookService.java** を新規作成する。

```java
package com.training.bookmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.training.bookmanager.dto.BookRequest;
import com.training.bookmanager.model.Book;
import com.training.bookmanager.repository.BookRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }

    public Book create(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublishedDate(request.getPublishedDate());
        book.setIsbn(request.getIsbn());
        return bookRepository.save(book);
    }
}
```

### コード解説

- `@Service`: このクラスがサービス層のコンポーネントであることをSpringに伝える。Springが自動的にインスタンスを管理する（DI: 依存性注入）。
- コンストラクタインジェクション: `BookService(BookRepository bookRepository)` により、Springが `BookRepository` のインスタンスを自動的に注入する。Pythonの `Depends(get_db)` に相当する仕組み。
- `findById(id).orElse(null)`: `Optional<Book>` 型の戻り値。値が存在すればそのBookオブジェクトを、存在しなければ `null` を返す。
- `bookRepository.save(book)`: 新規保存と更新の両方に使える。`id` が `null` なら新規保存（INSERT）、値があれば更新（UPDATE）として動作する。

---

## 2. APIコントローラの作成

**src/main/java/com/training/bookmanager/controller/BookApiController.java** を新規作成する。

```java
package com.training.bookmanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.training.bookmanager.dto.BookRequest;
import com.training.bookmanager.dto.BookResponse;
import com.training.bookmanager.model.Book;
import com.training.bookmanager.service.BookService;

@RestController
@RequestMapping("/books")
public class BookApiController {

    private final BookService bookService;

    public BookApiController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> listBooks() {
        return bookService.findAll().stream()
                .map(BookResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
        Book book = bookService.findById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BookResponse.fromEntity(book));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(
            @Validated @RequestBody BookRequest request) {
        Book created = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookResponse.fromEntity(created));
    }
}
```

### コード解説

- `@RequestMapping("/books")`: このコントローラ内の全エンドポイントに `/books` プレフィックスを付ける。FastAPIの `APIRouter(prefix="/books")` に相当。
- `@GetMapping`: HTTP GETメソッドに対応。引数なしは `/books` そのもの、`"/{id}"` 付きは `/books/1` のようなパスに対応。
- `@PathVariable Long id`: URLパスの `{id}` 部分を引数として受け取る。
- `@RequestBody BookRequest request`: HTTPリクエストボディのJSONを `BookRequest` オブジェクトに自動変換する。
- `@Validated`: `BookRequest` に定義したバリデーション（`@NotBlank`, `@Size` 等）を有効にする。
- `ResponseEntity<T>`: HTTPステータスコードとレスポンスボディを含むレスポンスオブジェクト。
- `.stream().map(BookResponse::fromEntity).toList()`: Java Streamを使い、`Book` のリストを `BookResponse` のリストに変換する。

---

## 3. HealthControllerのルートパスを一時変更

Day 1で作成した `HealthController` の `root()` メソッドの `/` パスは、Day 5でHTML画面に差し替えるまで一時的にそのままにしておく。

---

## 4. 動作確認

アプリケーションを起動する。

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

### 4-1. 書籍の登録（Create）

```bash
curl -X POST http://localhost:8000/books \
  -H "Content-Type: application/json" \
  -d '{"title": "トム・ソーヤーの冒険", "author": "マーク・トウェイン", "publisher": "新潮文庫"}'
```

Windows PowerShellの場合:

```powershell
Invoke-WebRequest -Uri "http://localhost:8000/books" -Method POST -ContentType "application/json" -Body '{"title": "トム・ソーヤーの冒険", "author": "マーク・トウェイン", "publisher": "新潮文庫"}'
```

期待されるレスポンス（status_code: 201）:

```json
{
  "id": 1,
  "title": "トム・ソーヤーの冒険",
  "author": "マーク・トウェイン",
  "publisher": "新潮文庫",
  "publishedDate": null,
  "isbn": null,
  "createdAt": "2025-04-01T10:00:00",
  "updatedAt": "2025-04-01T10:00:00"
}
```

もう1冊登録する。

```bash
curl -X POST http://localhost:8000/books \
  -H "Content-Type: application/json" \
  -d '{"title": "ハックルベリー・フィンの冒険", "author": "マーク・トウェイン", "publisher": "岩波文庫", "isbn": "9784003234020"}'
```

Windows PowerShellの場合:

```powershell
Invoke-WebRequest -Uri "http://localhost:8000/books" -Method POST -ContentType "application/json" -Body '{"title": "ハックルベリー・フィンの冒険", "author": "マーク・トウェイン", "publisher": "岩波文庫", "isbn": "9784003234020"}'
```

### 4-2. 書籍一覧の取得（Read - List）

```bash
curl http://localhost:8000/books
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books"
```

### 4-3. 書籍の個別取得（Read - Detail）

```bash
curl http://localhost:8000/books/1
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/1"
```

### 4-4. 存在しないIDの取得

```bash
curl -i http://localhost:8000/books/999
```

Windows PowerShellの場合:

```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8000/books/999"
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

`-i` オプションでHTTPヘッダも表示する。ステータスコード `404` が返ることを確認する。

---

## 5. 本日の確認ポイント

1. `POST /books` で書籍を登録でき、IDが自動採番される
2. `GET /books` で登録済みの全書籍が配列で返る
3. `GET /books/{id}` で指定したIDの書籍が返る
4. 存在しないIDを指定すると404が返る
5. コントローラ → サービス → リポジトリの3層構造を説明できる

---

## Day 3 完了時のファイル構成

```
book-manager/
├── pom.xml
├── src/main/java/com/training/bookmanager/
│   ├── BookManagerApplication.java
│   ├── controller/
│   │   ├── HealthController.java
│   │   └── BookApiController.java     ← 新規
│   ├── model/
│   │   └── Book.java
│   ├── repository/
│   │   └── BookRepository.java
│   ├── service/
│   │   └── BookService.java           ← 新規
│   └── dto/
│       ├── BookRequest.java
│       └── BookResponse.java
└── src/main/resources/
    └── application.properties
```
