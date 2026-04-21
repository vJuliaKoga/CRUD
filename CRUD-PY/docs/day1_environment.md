# Day 1: 環境構築・Hello World API（Python/FastAPI版）

## 学習目標

- Python仮想環境（venv）を作成し、依存パッケージを管理できる
- FastAPIの基本構造を理解し、最小限のAPIサーバーを起動できる
- curlまたはブラウザでAPIの動作確認ができる

## 所要時間の目安

| 作業                       | 時間 |
| -------------------------- | ---- |
| 仮想環境・プロジェクト作成 | 15分 |
| FastAPIの基本コード作成    | 25分 |
| 動作確認・理解             | 20分 |

---

## 1. プロジェクトの作成

ターミナルを開き、以下のコマンドを順に実行する。

```bash
mkdir book-manager
cd book-manager
```

Windows PowerShellの場合:

```powershell
New-Item -Path "book-manager" -ItemType Directory
Set-Location book-manager
```

## 2. 仮想環境の作成

Pythonの仮想環境（venv）は、プロジェクトごとに独立したパッケージ管理を行う仕組み。
グローバル環境を汚さないために必ず使用する。

```bash
python -m venv venv
```

Windows PowerShellの場合:

```powershell
python -m venv venv
```

Windows PowerShellで仮想環境を有効化する。

```powershell
.\venv\Scripts\Activate.ps1
```

有効化されると、ターミナルのプロンプトに `(venv)` が表示される。

### 補足: venvとは何か

`venv` はPython標準の仮想環境ツール。
仮想環境内にインストールしたパッケージは、その環境でのみ有効となる。
これにより、プロジェクトAではFastAPI 0.100を、プロジェクトBではFastAPI 0.110を、というように異なるバージョンを共存させることができる。

---

## 3. 依存パッケージのインストール

まず `requirements.txt` を作成する。
テキストエディタで以下の内容のファイルを作成すること。

**requirements.txt**

```
fastapi==0.115.0
uvicorn==0.30.0
```

作成したら、インストールを実行する。

```bash
pip install -r requirements.txt
```

Windows PowerShellの場合:

```powershell
python -m pip install -r requirements.txt
```

### 各パッケージの役割

- `fastapi`: PythonのWebフレームワーク。型ヒント（Type Hints）を活用し、自動でAPIドキュメントを生成してくれる点が特徴。
- `uvicorn`: ASGI（Asynchronous Server Gateway Interface）サーバー。FastAPIアプリケーションを実行するために必要なWebサーバー。

---

## 4. Hello World APIの作成

テキストエディタで `main.py` を新規作成し、以下のコードを**手で入力**する。

**main.py**

```python
from fastapi import FastAPI

app = FastAPI(title="書籍管理システム", version="1.0.0")


@app.get("/health")
def health_check():
    return {"status": "ok", "message": "Book Manager API is running"}
```

### コード解説

- `FastAPI()`: FastAPIアプリケーションのインスタンスを作成する。`title` と `version` はAPIドキュメントに表示される。
- `@app.get("/health")`: デコレータ（Decorator）と呼ばれる記法。`GET /health` へのリクエストが来たとき、直下の関数を呼び出すよう登録している。
- 関数の戻り値として辞書（dict）を返すと、FastAPIが自動的にJSON形式に変換してレスポンスを返す。

---

## 5. サーバーの起動

ターミナルで以下を実行する。

```bash
uvicorn main:app --reload --port 8000
```

Windows PowerShellの場合:

```powershell
uvicorn main:app --reload --port 8000
```

### コマンドの意味

- `main:app`: `main.py` ファイルの中の `app` という変数（FastAPIインスタンス）を指定
- `--reload`: ファイルを保存するたびにサーバーが自動的に再起動する（開発時のみ使用）
- `--port 8000`: ポート番号8000でサーバーを起動する

起動に成功すると、以下のようなログが表示される。

```
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
INFO:     Started reloader process [xxxxx] using StatReload
```

---

## 6. 動作確認

サーバーを起動したまま、別のターミナルウィンドウを開いて以下を実行する。

### curlで確認

```bash
curl http://localhost:8000/health
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/health"
```

期待されるレスポンス:

```json
{ "status": "ok", "message": "Book Manager API is running" }
```

### ブラウザで確認

ブラウザで以下のURLにアクセスする。

- ヘルスチェック: http://localhost:8000/health
- 自動生成APIドキュメント: http://localhost:8000/docs

FastAPIは `Swagger UI` というAPIドキュメントを自動で生成する。`/docs` にアクセスすると、登録されているエンドポイントの一覧と、ブラウザ上からAPIを試すことができるインタラクティブな画面が表示される。

---

## 7. ルートエンドポイントの追加

`main.py` に以下を追記する（`health_check` 関数の下に追加）。

```python
@app.get("/")
def root():
    return {"message": "Welcome to Book Manager"}
```

ファイルを保存すると `--reload` オプションによりサーバーが自動で再起動する。

curlで確認する。

```bash
curl http://localhost:8000/
```

Windows PowerShellの場合:

```powershell
Invoke-RestMethod -Uri "http://localhost:8000/"
```

---

## 8. 本日の確認ポイント

以下の項目を全て達成できていることを確認する。

1. `python --version` で Python 3.11以上が表示される
2. 仮想環境 `(venv)` が有効化されている
3. `uvicorn main:app --reload` でサーバーが起動する
4. `curl http://localhost:8000/health` で `{"status":"ok",...}` が返る
5. ブラウザで `http://localhost:8000/docs` にアクセスするとSwagger UIが表示される

---

## Day 1 完了時のファイル構成

```
book-manager/
├── venv/                # 仮想環境（Git管理対象外）
├── main.py              # アプリケーション本体
└── requirements.txt     # 依存パッケージ一覧
```

---

## 自習課題（余裕がある場合）

- FastAPI公式ドキュメント（https://fastapi.tiangolo.com/ja/）の「最初のステップ」を読む
- `@app.get("/hello/{name}")` のようにパスパラメータを受け取るエンドポイントを自分で追加してみる
