# Day 8: AWS EC2デプロイ（Python/FastAPI版）

## 学習目標

- EC2インスタンスを作成し、SSH接続できる
- EC2上にアプリケーションをデプロイし、外部からアクセスできる状態にする
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

1. 画面上部の検索バーで「EC2」と入力し、EC2ダッシュボードを開く
2. 「インスタンスを起動」ボタンをクリック
3. 以下の設定で作成する

| 項目 | 設定値 |
|------|--------|
| 名前 | book-manager-training |
| AMI | Amazon Linux 2023（デフォルト） |
| インスタンスタイプ | t2.micro（無料枠対象） |
| キーペア | 新しいキーペアを作成（名前: `book-manager-key`、タイプ: RSA、形式: .pem） |
| ネットワーク設定 | 「からのSSHトラフィックを許可」にチェック、「インターネットからのHTTPトラフィックを許可」にチェック |

4. 「インスタンスを起動」をクリック

キーペアファイル（`book-manager-key.pem`）はダウンロードされたら安全な場所に保存すること。このファイルを紛失するとSSH接続ができなくなる。

### 1-2. セキュリティグループの確認

作成したインスタンスのセキュリティグループに、以下のインバウンドルールが設定されていることを確認する。

| タイプ | ポート | ソース |
|--------|-------|--------|
| SSH | 22 | 自分のIPまたは 0.0.0.0/0 |
| HTTP | 80 | 0.0.0.0/0 |
| カスタムTCP | 8000 | 0.0.0.0/0 |

ポート8000のルールがない場合は、セキュリティグループのインバウンドルールを編集して追加する。

---

## 2. SSH接続

インスタンスが「実行中」になったら、パブリックIPアドレスを控える（例: `54.123.45.67`）。

```bash
# キーペアのパーミッション変更（Linux/macOS）
chmod 400 book-manager-key.pem

# SSH接続
ssh -i book-manager-key.pem ec2-user@<パブリックIP>
```

Windows (PowerShell)の場合:

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

### 3-2. Pythonと関連ツールのインストール

```bash
sudo dnf install -y python3.11 python3.11-pip python3.11-devel
sudo dnf install -y postgresql16-server postgresql16 gcc
```

### 3-3. PostgreSQLのセットアップ

```bash
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

PostgreSQLの認証方式をパスワード認証に変更する。

```bash
sudo vi /var/lib/pgsql/data/pg_hba.conf
```

ファイル末尾付近の以下の行を探す。

```
local   all             all                                     peer
host    all             all             127.0.0.1/32            ident
```

これを以下に変更する。

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

### 4-1. ソースコードの転送

ローカルマシンの別のターミナルで、プロジェクトディレクトリに移動し、ソースコードをEC2に転送する。

```bash
# venvや不要ファイルを除外して転送
rsync -avz --exclude='venv' --exclude='__pycache__' --exclude='*.db' --exclude='.env' \
  -e "ssh -i book-manager-key.pem" \
  ./ ec2-user@<パブリックIP>:~/book-manager/
```

`rsync` が使えない場合は `scp` で転送する。

```bash
scp -i book-manager-key.pem -r \
  main.py database.py models.py schemas.py crud.py routers/ templates/ static/ requirements.txt \
  ec2-user@<パブリックIP>:~/book-manager/
```

### 4-2. EC2上でのセットアップ

SSH接続したターミナルに戻る。

```bash
cd ~/book-manager

# 仮想環境の作成
python3.11 -m venv venv
source venv/bin/activate

# 依存パッケージのインストール
pip install -r requirements.txt
```

### 4-3. 環境変数の設定

EC2上では `.env` ファイルではなく、直接環境変数を設定する。

```bash
export DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager"
```

永続化する場合は `~/.bashrc` に追記する。

```bash
echo 'export DATABASE_URL="postgresql://bookuser:bookpass123@localhost:5432/bookmanager"' >> ~/.bashrc
```

---

## 5. アプリケーションの起動

### 5-1. 起動確認

```bash
cd ~/book-manager
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000
```

- `--host 0.0.0.0`: 全てのネットワークインターフェースでリクエストを受け付ける。`127.0.0.1`（デフォルト）だと外部からアクセスできない。

### 5-2. ブラウザで確認

ローカルマシンのブラウザで以下にアクセスする。

```
http://<EC2のパブリックIP>:8000/
```

書籍管理画面が表示され、登録・表示・編集・削除が全て動作することを確認する。

### 5-3. バックグラウンド起動（任意）

ターミナルを閉じてもアプリケーションを動かし続けたい場合は、`nohup` を使う。

```bash
nohup uvicorn main:app --host 0.0.0.0 --port 8000 > app.log 2>&1 &
```

ログの確認:

```bash
tail -f app.log
```

停止する場合:

```bash
pkill -f uvicorn
```

---

## 6. 後片付け（重要）

研修終了後、不要な課金を防ぐためにEC2インスタンスを停止または終了すること。

1. AWSマネジメントコンソールでEC2ダッシュボードを開く
2. 作成したインスタンスを選択
3. 「インスタンスの状態」→「インスタンスを終了」

停止は一時的な停止（再開可能）、終了は完全な削除である。研修が完全に終了した場合は「終了」を選ぶ。

---

## 7. 本日の確認ポイント

1. EC2インスタンスが作成され、SSH接続できる
2. EC2上でPostgreSQLが動作している
3. ブラウザから `http://<パブリックIP>:8000/` で書籍管理画面が表示される
4. 画面からCRUD操作が全て動作する
5. 研修終了後、EC2インスタンスを停止/終了している

---

## 研修完了

以上で8日間の研修カリキュラムは全て完了である。この研修を通じて以下のスキルを習得した。

- REST APIの設計と実装（CRUD操作）
- ORMによるデータベース操作
- HTMLフロントエンドとAPIの連携
- 自動テストの作成と実行
- データベースの切替（SQLite → PostgreSQL）
- 環境変数による構成管理
- AWSクラウド環境へのデプロイ

これらは全てのWebアプリケーション開発に共通する基礎スキルである。次のステップとして、認証機能の追加、Docker化、CI/CDパイプラインの構築などに取り組むことを推奨する。
