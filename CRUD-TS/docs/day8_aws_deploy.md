# Day 8: AWS EC2デプロイ（TypeScript/Express版）

## 学習目標

- EC2インスタンスを作成し、SSH接続できる
- EC2上にNode.jsアプリケーションをデプロイし、外部からアクセスできる状態にする
- 本番環境での起動方法とセキュリティの基本を理解する

## 所要時間の目安

| 作業 | 時間 |
|------|------|
| EC2インスタンス作成 | 15分 |
| 環境構築・デプロイ | 30分 |
| 動作確認・後片付け | 15分 |

---

## 1. EC2インスタンスの作成

AWSマネジメントコンソール（https://console.aws.amazon.com/）にログインする。

### 1-1. インスタンス起動

1. EC2ダッシュボードを開く
2. 「インスタンスを起動」をクリック
3. 以下の設定で作成する

| 項目 | 設定値 |
|------|--------|
| 名前 | book-manager-training |
| AMI | Amazon Linux 2023 |
| インスタンスタイプ | t2.micro（無料枠対象） |
| キーペア | 新しいキーペアを作成（名前: `book-manager-key`、タイプ: RSA、形式: .pem） |
| ネットワーク設定 | SSH許可 + HTTP許可 |

4. 「インスタンスを起動」をクリック

キーペアファイル（`book-manager-key.pem`）は安全な場所に保存すること。

### 1-2. セキュリティグループの確認

インバウンドルールに以下が含まれていることを確認する。

| タイプ | ポート | ソース |
|--------|-------|--------|
| SSH | 22 | 自分のIPまたは 0.0.0.0/0 |
| HTTP | 80 | 0.0.0.0/0 |
| カスタムTCP | 8000 | 0.0.0.0/0 |

ポート8000のルールがない場合は追加する。

---

## 2. SSH接続

インスタンスが「実行中」になったら、パブリックIPアドレスを控える。

```powershell
ssh -i book-manager-key.pem ec2-user@<パブリックIP>
```

初回接続時に `Are you sure you want to continue connecting?` と聞かれたら `yes` と入力する。

---

## 3. EC2上の環境構築

SSH接続した状態で以下のコマンドを実行する。

### 3-1. システムパッケージの更新

```bash
sudo dnf update -y
```

### 3-2. Node.jsのインストール

```bash
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo dnf install -y nodejs
node --version
npm --version
```

### 3-3. PostgreSQLのセットアップ

```bash
sudo dnf install -y postgresql16-server postgresql16
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

データベースとユーザーを作成する。

```bash
sudo -u postgres psql
```

```sql
CREATE USER bookuser WITH PASSWORD 'bookpass123';
CREATE DATABASE bookmanager OWNER bookuser;
GRANT ALL PRIVILEGES ON DATABASE bookmanager TO bookuser;
\q
```

PostgreSQLの認証方式を変更する。

```bash
sudo vi /var/lib/pgsql/data/pg_hba.conf
```

以下の行を探す。

```
local   all             all                                     peer
host    all             all             127.0.0.1/32            ident
```

以下に変更する。

```
local   all             all                                     md5
host    all             all             127.0.0.1/32            md5
```

PostgreSQLを再起動する。

```bash
sudo systemctl restart postgresql
```

---

## 4. アプリケーションのデプロイ

### 4-1. ファイルの転送

ローカルマシンの別のPowerShellで、必要なファイルをEC2に転送する。

まず、転送対象を整理する。Node.jsプロジェクトでは `node_modules` はサーバー上で `npm install` するため転送不要。

```powershell
# ビルド済みファイル・設定ファイルを転送
scp -i book-manager-key.pem -r `
  dist/ `
  prisma/ `
  package.json `
  package-lock.json `
  ec2-user@<パブリックIP>:~/book-manager/
```

`scp` でディレクトリ転送がうまくいかない場合は、ZIPにしてから転送する。

```powershell
# ローカルでZIPを作成
Compress-Archive -Path dist, prisma, package.json, package-lock.json -DestinationPath book-manager.zip

# ZIPを転送
scp -i book-manager-key.pem book-manager.zip ec2-user@<パブリックIP>:~/
```

EC2側で解凍する。

```bash
cd ~
mkdir book-manager
cd book-manager
unzip ~/book-manager.zip
```

### 4-2. EC2上でのセットアップ

SSH接続したターミナルに戻る。

```bash
cd ~/book-manager

# 本番用パッケージのみインストール（devDependenciesは除外）
npm install --omit=dev
```

### 4-3. 環境変数の設定

```bash
export DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager"
export PORT=8000
```

永続化する場合は `~/.bashrc` に追記する。

```bash
cat << 'EOF' >> ~/.bashrc
export DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager"
export PORT=8000
EOF
source ~/.bashrc
```

### 4-4. マイグレーションの実行

```bash
npx prisma migrate deploy
```

---

## 5. アプリケーションの起動

### 5-1. 起動確認

```bash
node dist/server.js
```

以下のログが表示されれば成功。

```
Server is running on http://localhost:8000
```

### 5-2. ブラウザで確認

ローカルマシンのブラウザで以下にアクセスする。

```
http://<EC2のパブリックIP>:8000/
```

書籍管理画面が表示され、CRUD操作が全て動作することを確認する。

### 5-3. バックグラウンド起動（任意）

```bash
nohup node dist/server.js > app.log 2>&1 &
```

ログの確認:

```bash
tail -f app.log
```

停止する場合:

```bash
pkill -f "node dist/server.js"
```

---

## 6. 後片付け（重要）

研修終了後、不要な課金を防ぐためにEC2インスタンスを停止または終了すること。

1. AWSマネジメントコンソールでEC2ダッシュボードを開く
2. 作成したインスタンスを選択
3. 「インスタンスの状態」→「インスタンスを終了」

---

## 7. 本日の確認ポイント

1. EC2インスタンスが作成され、SSH接続できる
2. EC2上にNode.js 20とPostgreSQLがインストールされている
3. ブラウザから `http://<パブリックIP>:8000/` で書籍管理画面が表示される
4. 画面からCRUD操作が全て動作する
5. 研修終了後、EC2インスタンスを停止/終了している

---

## 研修完了

以上で8日間の研修カリキュラムは全て完了である。この研修を通じて以下のスキルを習得した。

- REST APIの設計と実装（CRUD操作）
- Prisma ORMによる型安全なデータベース操作
- HTMLフロントエンドとAPIの連携
- Jestによる自動テストの作成と実行
- データベースの切替（SQLite → PostgreSQL）
- 環境変数による構成管理
- TypeScriptのビルドとデプロイ
- AWSクラウド環境へのデプロイ

これらは全てのWebアプリケーション開発に共通する基礎スキルである。次のステップとして、認証機能の追加（Passport.js）、Docker化、CI/CDパイプラインの構築、Next.jsへの移行などに取り組むことを推奨する。
