def test_health_check(client):
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_create_book_via_api(client):
    res = client.post(
        "/books/",
        json={
            "title": "トム・ソーヤーの冒険",
            "author": "マーク・トウェイン",
        },
    )
    assert res.status_code == 201
    data = res.json()
    assert data["title"] == "トム・ソーヤーの冒険"
    assert data["id"] is not None


def test_list_books_via_api(client):
    client.post(
        "/books/",
        json={"title": "トム・ソーヤーの冒険", "author": "マーク・トウェイン"},
    )
    client.post(
        "/books/",
        json={"title": "ハックルベリー・フィンの冒険", "author": "マーク・トウェイン"},
    )

    res = client.get("/books/")
    assert res.status_code == 200
    assert len(res.json()) == 2


def test_get_book_via_api(client):
    create_res = client.post(
        "/books/",
        json={
            "title": "トム・ソーヤーの冒険",
            "author": "マーク・トウェイン",
        },
    )
    book_id = create_res.json()["id"]

    res = client.get(f"/books/{book_id}")
    assert res.status_code == 200
    assert res.json()["title"] == "トム・ソーヤーの冒険"


def test_get_book_not_found_via_api(client):
    res = client.get("/books/9999")
    assert res.status_code == 404


def test_update_book_via_api(client):
    create_res = client.post(
        "/books/",
        json={
            "title": "トム・ソーヤーの冒険",
            "author": "マーク・トウェイン",
        },
    )
    book_id = create_res.json()["id"]

    res = client.put(
        f"/books/{book_id}",
        json={"title": "トム・ソーヤーの冒険 改訂版"},
    )
    assert res.status_code == 200
    assert res.json()["title"] == "トム・ソーヤーの冒険 改訂版"
    assert res.json()["author"] == "マーク・トウェイン"


def test_delete_book_via_api(client):
    create_res = client.post(
        "/books/",
        json={
            "title": "ハックルベリー・フィンの冒険",
            "author": "マーク・トウェイン",
        },
    )
    book_id = create_res.json()["id"]

    res = client.delete(f"/books/{book_id}")
    assert res.status_code == 204

    res = client.get(f"/books/{book_id}")
    assert res.status_code == 404


def test_create_book_validation_error(client):
    res = client.post("/books/", json={"title": "", "author": "マーク・トウェイン"})
    assert res.status_code == 422


def test_create_book_missing_field(client):
    res = client.post("/books/", json={"title": "トム・ソーヤーの冒険"})
    assert res.status_code == 422
