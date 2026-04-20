# Day 4: Update / Delete API実装（Java/Spring Boot版）

## 学習目標

- 書籍の更新処理と削除処理を実装できる
- バリデーションエラーのハンドリングを統一できる
- CRUDの全4操作が揃い、APIとして完成する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| サービスにupdate/delete追加 | 15分 |
| コントローラにエンドポイント追加 | 15分 |
| 例外ハンドリング追加 | 15分 |
| 動作確認 | 15分 |

---

## 1. 更新用DTOの作成

更新では全てのフィールドが任意（部分更新対応）にするため、`BookRequest` とは別にDTOを作成する。

**src/main/java/com/training/bookmanager/dto/BookUpdateRequest.java** を新規作成する。

```java
package com.training.bookmanager.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BookUpdateRequest {

    @Size(min = 1, max = 200, message = "タイトルは1~200文字で入力してください")
    private String title;

    @Size(min = 1, max = 100, message = "著者は1~100文字で入力してください")
    private String author;

    @Size(max = 100)
    private String publisher;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "日付はYYYY-MM-DD形式で入力してください")
    private String publishedDate;

    @Pattern(regexp = "^$|^\\d{10}(\\d{3})?$", message = "ISBNは10桁または13桁の数字で入力してください")
    private String isbn;

    // --- Getter / Setter ---

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
}
```

`BookRequest` との違いは `@NotBlank` がないこと。更新時は送信されたフィールドのみを変更するため、全て任意項目となる。

---

## 2. サービスにUpdate/Deleteメソッドを追加

**BookService.java** に以下のメソッドを追加する。

```java
    public Book update(Long id, BookUpdateRequest request) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return null;
        }

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getPublisher() != null) {
            book.setPublisher(request.getPublisher());
        }
        if (request.getPublishedDate() != null) {
            book.setPublishedDate(request.getPublishedDate());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }

        return bookRepository.save(book);
    }

    public boolean delete(Long id) {
        if (!bookRepository.existsById(id)) {
            return false;
        }
        bookRepository.deleteById(id);
        return true;
    }
```

importに `BookUpdateRequest` を追加する。

```java
import com.training.bookmanager.dto.BookUpdateRequest;
```

### コード解説

- `request.getTitle() != null` のチェック: JSONで送信されなかったフィールドは `null` になる。`null` でないフィールドのみを更新することで部分更新を実現する。
- `bookRepository.save(book)`: `id` が設定済みのエンティティを `save` に渡すと、INSERT（新規）ではなくUPDATE（更新）として動作する。
- `existsById(id)`: 指定したIDのレコードが存在するかをbooleanで返す。

---

## 3. コントローラにUpdate/Deleteエンドポイントを追加

**BookApiController.java** に以下のimportとメソッドを追加する。

import追加:

```java
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.training.bookmanager.dto.BookUpdateRequest;
```

メソッド追加:

```java
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Validated @RequestBody BookUpdateRequest request) {
        Book updated = bookService.update(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(BookResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        boolean deleted = bookService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
```

### コード解説

- `@PutMapping("/{id}")`: HTTP PUTメソッドに対応。
- `@DeleteMapping("/{id}")`: HTTP DELETEメソッドに対応。
- `ResponseEntity.noContent().build()`: ステータスコード204（No Content）のレスポンスを返す。

---

## 4. バリデーションエラーハンドリングの追加

現在、バリデーションエラーが発生するとSpring Bootのデフォルトエラーページが返される。JSON形式で分かりやすいエラーレスポンスを返すように、グローバル例外ハンドラを追加する。

**src/main/java/com/training/bookmanager/controller/GlobalExceptionHandler.java** を新規作成する。

```java
package com.training.bookmanager.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", 422);
        body.put("error", "Validation Error");
        body.put("details", fieldErrors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}
```

### コード解説

- `@RestControllerAdvice`: アプリケーション全体の例外を横断的に処理するクラス。個々のコントローラに例外処理を書く必要がなくなる。
- `@ExceptionHandler(MethodArgumentNotValidException.class)`: `@Validated` によるバリデーション失敗時に投げられる例外を捕捉する。
- フィールドごとのエラーメッセージを `Map` に詰めてJSON形式で返す。

---

## 5. 動作確認

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

### 5-1. 書籍の更新（Update）

```bash
curl -X PUT http://localhost:8000/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "トム・ソーヤーの冒険 改訂版"}'
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/1" -Method PUT -ContentType "application/json" -Body '{"title": "トム・ソーヤーの冒険 改訂版"}'
```

`title` のみが更新され、他のフィールドは変わらないことを確認する。

### 5-2. 書籍の削除（Delete）

```bash
curl -X DELETE http://localhost:8000/books/2
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/books/2" -Method DELETE
```

ステータスコード204が返ることを確認する。

```bash
curl -i http://localhost:8000/books/2
```

Windows PowerShellの場合:

```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8000/books/2"
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

404が返ることを確認する。

### 5-3. バリデーションエラーの確認

```bash
curl -X POST http://localhost:8000/books \
  -H "Content-Type: application/json" \
  -d '{"title": "", "author": "テスト"}'
```

Windows PowerShellの場合:

```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8000/books" -Method POST -ContentType "application/json" -Body '{"title": "", "author": "テスト"}'
} catch {
    $_.ErrorDetails.Message
}
```

以下のようなJSON形式のエラーが返ることを確認する。

```json
{
  "status": 422,
  "error": "Validation Error",
  "details": {
    "title": "タイトルは必須です"
  }
}
```

---

## 6. 本日の確認ポイント

1. `PUT /books/{id}` で書籍を部分更新できる
2. `DELETE /books/{id}` で書籍を削除でき、204が返る
3. バリデーションエラー時にJSON形式でエラー詳細が返る
4. CRUD全4操作がcurlで動作確認できる

---

## Day 4 完了時のファイル構成

```
book-manager/
├── src/main/java/com/training/bookmanager/
│   ├── controller/
│   │   ├── HealthController.java
│   │   ├── BookApiController.java        ← 更新
│   │   └── GlobalExceptionHandler.java   ← 新規
│   ├── model/
│   │   └── Book.java
│   ├── repository/
│   │   └── BookRepository.java
│   ├── service/
│   │   └── BookService.java              ← 更新
│   └── dto/
│       ├── BookRequest.java
│       ├── BookUpdateRequest.java        ← 新規
│       └── BookResponse.java
└── ...
```
