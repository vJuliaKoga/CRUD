# Day 1: 環境構築・Hello World API（Java/Spring Boot版）

## 学習目標

- Spring Initializrを使ってSpring Bootプロジェクトを作成できる
- Mavenによるビルドとプロジェクト構成を理解する
- 最小限のREST APIを実装し、動作確認ができる

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| プロジェクト作成・構成理解 | 20分 |
| Hello World API作成 | 20分 |
| 動作確認 | 20分 |

---

## 1. プロジェクトの作成

ブラウザで Spring Initializr（https://start.spring.io/）にアクセスし、以下の設定でプロジェクトを生成する。

| 項目 | 設定値 |
|------|--------|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.0.5（最新の安定版） |
| Group | com.training |
| Artifact | book-manager |
| Name | book-manager |
| Package name | com.training.bookmanager |
| Packaging | Jar |
| Java | 17 |

「ADD DEPENDENCIES」をクリックし、以下の依存関係を追加する。

- **Spring Web**: REST API開発に必要
- **Spring Data JPA**: ORMによるデータベース操作
- **H2 Database**: 組込み型データベース（SQLite相当）
- **Thymeleaf**: HTMLテンプレートエンジン
- **Validation**: 入力値検証

「GENERATE」ボタンをクリックしてZIPファイルをダウンロードし、任意の場所に展開する。

---

## 2. プロジェクト構成の理解

展開したディレクトリの構成を確認する。

```
book-manager/
├── pom.xml                                    # Maven設定ファイル
├── src/
│   ├── main/
│   │   ├── java/com/training/bookmanager/
│   │   │   └── BookManagerApplication.java    # エントリポイント
│   │   └── resources/
│   │       ├── application.properties         # 設定ファイル
│   │       ├── static/                        # 静的ファイル
│   │       └── templates/                     # HTMLテンプレート
│   └── test/
│       └── java/com/training/bookmanager/
│           └── BookManagerApplicationTests.java
└── mvnw / mvnw.cmd                            # Mavenラッパー
```

### 主要ファイルの役割

- `pom.xml`: Project Object Model。プロジェクトの依存関係、ビルド設定、メタデータを管理するXMLファイル。Pythonの `requirements.txt` に相当するが、より多機能。
- `BookManagerApplication.java`: アプリケーションのエントリポイント。`main` メソッドからSpring Bootが起動する。
- `application.properties`: データベース接続先やサーバーポートなど、アプリケーションの設定を記述するファイル。
- `mvnw` / `mvnw.cmd`: Mavenラッパー。Mavenがインストールされていなくても、このスクリプト経由でビルドできる。

---

## 3. 設定ファイルの編集

**src/main/resources/application.properties** を以下の内容にする。

```properties
# サーバー設定
server.port=8000

# H2 Database設定
spring.datasource.url=jdbc:h2:file:./data/books
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA設定
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2コンソール（開発用）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### 設定項目の解説

- `server.port=8000`: FastAPI版と同じポート番号で起動する。
- `spring.datasource.url=jdbc:h2:file:./data/books`: H2データベースをファイルベースで使用する。`./data/books.mv.db` としてデータが保存される。
- `spring.jpa.hibernate.ddl-auto=update`: アプリケーション起動時にエンティティ定義に合わせてテーブルを自動作成/更新する。
- `spring.jpa.show-sql=true`: 実行されるSQL文をコンソールに出力する（開発時のデバッグ用）。
- `spring.h2.console.enabled=true`: ブラウザからH2データベースの中身を確認できるコンソールを有効化する。

---

## 4. Hello World APIの作成

**src/main/java/com/training/bookmanager/** ディレクトリに `controller` パッケージを作成する。

```bash
mkdir -p src/main/java/com/training/bookmanager/controller
```

Windows PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path "src/main/java/com/training/bookmanager/controller"
```

**src/main/java/com/training/bookmanager/controller/HealthController.java** を新規作成する。

```java
package com.training.bookmanager.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of(
            "status", "ok",
            "message", "Book Manager API is running"
        );
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("message", "Welcome to Book Manager");
    }
}
```

### コード解説

- `@RestController`: このクラスがREST APIのコントローラであることをSpringに伝えるアノテーション。戻り値は自動的にJSONに変換される。
- `@GetMapping("/health")`: HTTP GETメソッドで `/health` パスへのリクエストを処理する。FastAPIの `@app.get("/health")` に相当。
- `Map.of(...)`: Java 9以降で使える不変Mapの生成メソッド。Springが自動的にJSONに変換する。

---

## 5. ビルドと起動

プロジェクトルートディレクトリで以下を実行する。

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

初回ビルドでは依存ライブラリのダウンロードに数分かかる場合がある。

起動に成功すると、以下のようなログが表示される。

```
Started BookManagerApplication in X.XXX seconds
```

---

## 6. 動作確認

### curlで確認

```bash
curl http://localhost:8000/health
```

Windows PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/health"
```

期待されるレスポンス:

```json
{"status":"ok","message":"Book Manager API is running"}
```

### ブラウザで確認

- ヘルスチェック: http://localhost:8000/health
- H2コンソール: http://localhost:8000/h2-console

H2コンソールにアクセスする際は、以下の設定で接続する。

| 項目 | 値 |
|------|-----|
| JDBC URL | jdbc:h2:file:./data/books |
| User Name | sa |
| Password | （空欄） |

---

## 7. 本日の確認ポイント

1. `java -version` で Java 17以上が表示される
2. `.\mvnw.cmd spring-boot:run` でアプリケーションが起動する
3. `Invoke-RestMethod -Uri "http://localhost:8000/health"` で `{"status":"ok",...}` が返る
4. H2コンソール（http://localhost:8000/h2-console）に接続できる

---

## Day 1 完了時のファイル構成

```
book-manager/
├── pom.xml
├── src/main/java/com/training/bookmanager/
│   ├── BookManagerApplication.java
│   └── controller/
│       └── HealthController.java          ← 新規
├── src/main/resources/
│   ├── application.properties             ← 更新
│   ├── static/
│   └── templates/
└── mvnw / mvnw.cmd
```

---

## 自習課題（余裕がある場合）

- Spring Boot公式ドキュメント（https://spring.io/projects/spring-boot）の「Getting Started」を読む
- `@GetMapping("/hello/{name}")` でパスパラメータを受け取るエンドポイントを追加してみる（`@PathVariable` を使う）
