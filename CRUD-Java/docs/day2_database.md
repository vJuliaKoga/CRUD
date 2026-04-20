# Day 2: データベース設計・モデル作成（Java/Spring Boot版）

## 学習目標

- JPAエンティティを使ってデータベーステーブルをJavaクラスとして定義できる
- Spring Data JPAのリポジトリインターフェースを理解する
- DTO（Data Transfer Object）によるリクエスト/レスポンスの型定義を理解する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| エンティティクラス作成 | 20分 |
| リポジトリ・DTO作成 | 25分 |
| 動作確認 | 15分 |

---

## 1. パッケージ構成の作成

以下のディレクトリを作成する。

```bash
mkdir -p src/main/java/com/training/bookmanager/model
mkdir -p src/main/java/com/training/bookmanager/repository
mkdir -p src/main/java/com/training/bookmanager/dto
mkdir -p src/main/java/com/training/bookmanager/service
```

Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path "src/main/java/com/training/bookmanager/model"
New-Item -ItemType Directory -Force -Path "src/main/java/com/training/bookmanager/repository"
New-Item -ItemType Directory -Force -Path "src/main/java/com/training/bookmanager/dto"
New-Item -ItemType Directory -Force -Path "src/main/java/com/training/bookmanager/service"
```

---

## 2. エンティティクラスの作成

JPA（Java Persistence API）のエンティティは、データベーステーブルに対応するJavaクラスである。クラスのフィールドがテーブルのカラムに対応する。

**src/main/java/com/training/bookmanager/model/Book.java** を新規作成する。

```java
package com.training.bookmanager.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 100)
    private String publisher;

    @Column(name = "published_date", length = 10)
    private String publishedDate;

    @Column(length = 13)
    private String isbn;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getter / Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### コード解説

- `@Entity`: このクラスがJPAエンティティ（＝データベーステーブル）であることを示す。
- `@Table(name = "books")`: テーブル名を明示的に指定する。省略するとクラス名がそのままテーブル名になる。
- `@Id`: 主キーを示す。
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: データベースの自動採番機能（AUTO INCREMENT）を使う。
- `@Column(nullable = false, length = 200)`: カラムの制約。`nullable = false` はNOT NULL制約、`length` は最大文字数。
- `@PrePersist`: エンティティが新規保存される直前に呼ばれるコールバック。`created_at` と `updated_at` を自動設定する。
- `@PreUpdate`: エンティティが更新される直前に呼ばれるコールバック。
- Getter/Setter: Javaではフィールドを `private` にし、アクセサメソッドを通じて値を読み書きするのが慣例である（カプセル化）。

---

## 3. リポジトリインターフェースの作成

Spring Data JPAの「リポジトリ」は、データベース操作のためのインターフェースである。インターフェースを定義するだけで、SpringがCRUD操作の実装を自動的に提供する。

**src/main/java/com/training/bookmanager/repository/BookRepository.java** を新規作成する。

```java
package com.training.bookmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.training.bookmanager.model.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
}
```

### コード解説

- `JpaRepository<Book, Long>`: 第1引数がエンティティの型、第2引数が主キーの型。これを継承するだけで以下のメソッドが自動的に使えるようになる。
  - `findAll()`: 全件取得
  - `findById(Long id)`: ID指定で取得
  - `save(Book book)`: 保存（新規作成・更新兼用）
  - `deleteById(Long id)`: ID指定で削除
  - `existsById(Long id)`: 存在確認
- `@Repository`: このインターフェースがデータアクセス層であることをSpringに伝える。

Pythonの `crud.py` では各操作を関数として手動で実装したが、Spring Data JPAではインターフェースの定義だけで基本的なCRUD操作が使える。これがSpring Data JPAの大きな特徴である。

---

## 4. DTOの作成

DTO（Data Transfer Object）は、APIのリクエストとレスポンスの形式を定義するクラスである。Pythonの `schemas.py`（Pydanticモデル）に相当する。

**src/main/java/com/training/bookmanager/dto/BookRequest.java** を新規作成する。

```java
package com.training.bookmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BookRequest {

    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;

    @NotBlank(message = "著者は必須です")
    @Size(max = 100, message = "著者は100文字以内で入力してください")
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

**src/main/java/com/training/bookmanager/dto/BookResponse.java** を新規作成する。

```java
package com.training.bookmanager.dto;

import java.time.LocalDateTime;

import com.training.bookmanager.model.Book;

public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String publishedDate;
    private String isbn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookResponse fromEntity(Book book) {
        BookResponse res = new BookResponse();
        res.id = book.getId();
        res.title = book.getTitle();
        res.author = book.getAuthor();
        res.publisher = book.getPublisher();
        res.publishedDate = book.getPublishedDate();
        res.isbn = book.getIsbn();
        res.createdAt = book.getCreatedAt();
        res.updatedAt = book.getUpdatedAt();
        return res;
    }

    // --- Getter ---

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getPublishedDate() { return publishedDate; }
    public String getIsbn() { return isbn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### コード解説

- `@NotBlank`: 空文字・null・空白のみの値を拒否する。Pydanticの `min_length=1` に相当。
- `@Size(max = 200)`: 最大文字数の制約。
- `@Pattern(regexp = ...)`: 正規表現によるパターン検証。`^$|` を先頭に付けることで空文字も許容している（任意項目のため）。
- `BookResponse.fromEntity(Book book)`: エンティティからレスポンスDTOに変換する静的ファクトリメソッド。Pydanticの `from_attributes=True` に相当する手動変換。

---

## 5. 動作確認

アプリケーションを起動する。

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

H2コンソール（http://localhost:8000/h2-console）に接続し、`books` テーブルが自動作成されていることを確認する。

```sql
SELECT * FROM BOOKS;
```

まだデータは0件だが、テーブルの存在とカラム構成を確認できる。

---

## 6. 本日の確認ポイント

1. `Book.java` エンティティが作成され、`books` テーブルが自動生成される
2. `BookRepository` インターフェースが作成されている
3. `BookRequest` / `BookResponse` DTOが作成されている
4. エンティティとDTOの役割の違いを説明できる
5. H2コンソールで `books` テーブルの構造を確認できる

---

## Day 2 完了時のファイル構成

```
book-manager/
├── pom.xml
├── src/main/java/com/training/bookmanager/
│   ├── BookManagerApplication.java
│   ├── controller/
│   │   └── HealthController.java
│   ├── model/
│   │   └── Book.java                  ← 新規
│   ├── repository/
│   │   └── BookRepository.java        ← 新規
│   └── dto/
│       ├── BookRequest.java           ← 新規
│       └── BookResponse.java          ← 新規
├── src/main/resources/
│   └── application.properties
└── mvnw / mvnw.cmd
```
