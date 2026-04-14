import json
import os
import sqlite3
import time


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SEED_JSON = os.path.join(ROOT, "app", "src", "main", "assets", "tag_catalog_seed.json")
OUT_DB = os.path.join(ROOT, "app", "src", "main", "assets", "databases", "nhviewer.db")
IDENTITY_HASH = "87798771dbaf45da4f79fbb9a996eca2"


def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def load_seed() -> list[dict]:
    if not os.path.exists(SEED_JSON):
        return []
    with open(SEED_JSON, "r", encoding="utf-8-sig") as f:
        data = json.load(f)
    if isinstance(data, list):
        return data
    return []


def create_schema(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        PRAGMA user_version=2;

        CREATE TABLE IF NOT EXISTS favorites (
            galleryId INTEGER NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            coverUrl TEXT,
            pageCount INTEGER NOT NULL,
            savedAt INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS history (
            galleryId INTEGER NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            coverUrl TEXT,
            pageCount INTEGER NOT NULL,
            lastReadPage INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS reading_progress (
            galleryId INTEGER NOT NULL PRIMARY KEY,
            pageIndex INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS tags (
            id INTEGER NOT NULL PRIMARY KEY,
            type TEXT NOT NULL,
            name TEXT NOT NULL,
            slug TEXT NOT NULL,
            name_zh TEXT,
            count INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        );

        CREATE INDEX IF NOT EXISTS index_tags_type ON tags(type);
        CREATE INDEX IF NOT EXISTS index_tags_name ON tags(name);
        CREATE INDEX IF NOT EXISTS index_tags_slug ON tags(slug);

        CREATE TABLE IF NOT EXISTS room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        );
        """
    )
    conn.execute(
        "INSERT OR REPLACE INTO room_master_table(id, identity_hash) VALUES(42, ?)",
        (IDENTITY_HASH,),
    )


def insert_tags(conn: sqlite3.Connection, tags: list[dict]) -> None:
    now_ms = int(time.time() * 1000)
    rows = []
    for t in tags:
        rows.append(
            (
                int(t.get("id", 0)),
                str(t.get("type", "")),
                str(t.get("name", "")),
                str(t.get("slug", "")),
                t.get("name_zh", None),
                int(t.get("count", 0) or 0),
                int(t.get("updated_at", now_ms) or now_ms),
            )
        )
    conn.executemany(
        """
        INSERT OR REPLACE INTO tags(
            id, type, name, slug, name_zh, count, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        rows,
    )


def main() -> None:
    ensure_dir(os.path.dirname(OUT_DB))
    if os.path.exists(OUT_DB):
        os.remove(OUT_DB)

    tags = load_seed()
    conn = sqlite3.connect(OUT_DB)
    try:
        create_schema(conn)
        insert_tags(conn, tags)
        conn.commit()
    finally:
        conn.close()

    print(f"Built prebuilt db: {OUT_DB}")
    print(f"Tags inserted: {len(tags)}")


if __name__ == "__main__":
    main()
