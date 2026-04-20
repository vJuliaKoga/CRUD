# Day 7: PostgreSQL切替・本番準備（Java/Spring Boot版）

## 学習目標

- H2 DatabaseからPostgreSQLへの切替手順を理解する
- Spring Bootのプロファイル機能による環境別設定を実践できる
- 本番デプロイに向けた構成変更を行える

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| PostgreSQLセットアップ | 20分 |
| 設定ファイルの切替 | 15分 |
| 動作確認・テスト | 15分 |
| 本番ビルド | 10分 |

---

## 1. PostgreSQLのセットアップ

### インストール（未インストールの場合）

Windows: PostgreSQLの公式サイト（https://www.postgresql.org/download/windows/）からインストーラをダウンロードしてインストールする。

### データベースとユーザーの作成

```powershell
psql -U postgres
```

```sql
CREATE USER bookuser WITH PASSWORD 'bookpass123';
CREATE DATABASE bookmanager OWNER bookuser;
GRANT ALL PRIVILEGES ON DATABASE bookmanager TO bookuser;
\q
```

接続確認:

```powershell
psql -U bookuser -d bookmanager -h localhost
```

接続できたら `\q` で抜ける。

---

## 2. 依存関係の追加

**pom.xml** の `<dependencies>` セクションに PostgreSQL ドライバを追加する。

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

H2 Databaseの依存関係は削除せず、テスト用として残す。スコープを変更する。

既存の H2 依存関係:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

これを以下に変更する:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

`scope` を `runtime` から `test` に変更することで、H2はテスト実行時のみ使用される。

---

## 3. プロファイルによる環境別設定

Spring Bootの「プロファイル」機能を使い、開発環境と本番環境で設定を切り替える。

### 本番用設定ファイルの作成

**src/main/resources/application.properties** を以下のように更新する。

```properties
# 共通設定
server.port=8000
spring.jpa.show-sql=false

# デフォルトのプロファイル
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

**src/main/resources/application-dev.properties** を新規作成する（開発用）。

```properties
# 開発環境: H2 Database
spring.datasource.url=jdbc:h2:file:./data/books
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**src/main/resources/application-prod.properties** を新規作成する（本番用）。

```properties
# 本番環境: PostgreSQL
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/bookmanager}
spring.datasource.username=${DATABASE_USER:bookuser}
spring.datasource.password=${DATABASE_PASSWORD:bookpass123}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=false
```

### 設定の解説

- `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`: 環境変数 `SPRING_PROFILES_ACTIVE` が設定されていなければ `dev` プロファイルを使う。
- `${DATABASE_URL:jdbc:postgresql://...}`: 環境変数 `DATABASE_URL` が設定されていればその値を、なければデフォルト値を使う。本番環境では環境変数でパスワード等を管理する。
- `application-dev.properties`: プロファイル名 `dev` のときに読み込まれる。
- `application-prod.properties`: プロファイル名 `prod` のときに読み込まれる。

---

## 4. PostgreSQLでの動作確認

prodプロファイルで起動する。

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

または環境変数で指定する:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
.\mvnw.cmd spring-boot:run
```

起動ログに `PostgreSQLDialect` が表示されることを確認する。

ブラウザで http://localhost:8000/ にアクセスし、書籍の登録・表示・更新・削除が全て動作することを確認する。

### PostgreSQLにデータが入っていることの確認

```powershell
psql -U bookuser -d bookmanager -h localhost
```

```sql
SELECT * FROM books;
\q
```

---

## 5. テストの実行

テストは引き続きH2（インメモリ）で実行される。`src/test/resources/application.properties` で設定済みのため変更不要。

```powershell
.\mvnw.cmd test
```

全てのテストが成功することを確認する。

---

## 6. 本番用JARファイルのビルド

EC2にデプロイするためのJARファイルを作成する。

```powershell
.\mvnw.cmd clean package "-DskipTests"
```

`target/` ディレクトリに `book-manager-0.0.1-SNAPSHOT.jar` が生成される。

JARファイルの動作確認:

```bash
java -jar target/book-manager-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

アプリケーションが正常に起動することを確認する。`Ctrl+C` で停止する。

---

## 7. 本日の確認ポイント

1. PostgreSQLにデータベースとユーザーが作成されている
2. `prod` プロファイルで起動するとPostgreSQLに接続される
3. `dev` プロファイルで起動するとH2 Databaseに接続される
4. テストが全て成功する
5. `mvnw clean package` でJARファイルがビルドできる

---

## Day 7 完了時のファイル構成

```
book-manager/
├── pom.xml                                ← 更新
├── src/main/resources/
│   ├── application.properties             ← 更新
│   ├── application-dev.properties         ← 新規
│   ├── application-prod.properties        ← 新規
│   ├── templates/
│   │   └── index.html
│   └── static/
│       └── style.css
├── target/
│   └── book-manager-0.0.1-SNAPSHOT.jar    ← ビルド生成物
└── ...
```
