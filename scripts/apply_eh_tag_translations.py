import argparse
import base64
import json
import struct
import urllib.request
from collections import defaultdict
from pathlib import Path


DEFAULT_TRANSLATION_URL = (
    "https://raw.githubusercontent.com/xiaojieonly/EhTagTranslation/main/"
    "tag-translations/tag-translations-zh-rCN.json"
)


TYPE_PREFIX = {
    "artist": "a:",
    "character": "c:",
    "group": "g:",
    "language": "l:",
    "parody": "p:",
}


def download_bytes(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=30) as resp:
        return resp.read()


def parse_eh_translation_bytes(raw: bytes) -> dict[str, str]:
    if len(raw) < 4:
        return {}

    size = struct.unpack(">i", raw[:4])[0]
    if size > 0 and size <= len(raw) - 4:
        payload = raw[4 : 4 + size]
    else:
        payload = raw

    text = payload.decode("utf-8", errors="replace")
    result: dict[str, str] = {}
    for line in text.split("\n"):
        if not line or "\r" not in line:
            continue
        key, encoded = line.split("\r", 1)
        key = key.strip().lower()
        if not key:
            continue
        try:
            zh = base64.b64decode(encoded).decode("utf-8", errors="replace").strip()
        except Exception:
            continue
        if zh:
            result[key] = zh
    return result


def normalize_tag_name(value: str) -> str:
    return value.strip().lower()


def normalize_tag_slug(value: str) -> str:
    return value.strip().lower().replace("-", " ")


def apply_translations(seed: list[dict], translation_map: dict[str, str]) -> dict:
    suffix_to_zh: dict[str, set[str]] = defaultdict(set)
    for key, zh in translation_map.items():
        suffix = key.split(":", 1)[1] if ":" in key else key
        suffix_to_zh[suffix].add(zh)
    unique_suffix = {
        suffix: next(iter(values))
        for suffix, values in suffix_to_zh.items()
        if len(values) == 1
    }

    stats = {
        "total": 0,
        "matched_direct": 0,
        "matched_unique_suffix": 0,
        "unmatched": 0,
        "per_type": defaultdict(lambda: {"direct": 0, "unique_suffix": 0, "unmatched": 0}),
    }

    for row in seed:
        stats["total"] += 1
        tag_type = str(row.get("type", "")).strip().lower()
        name = normalize_tag_name(str(row.get("name", "")))
        slug = normalize_tag_slug(str(row.get("slug", "")))

        mapped: str | None = None
        prefix = TYPE_PREFIX.get(tag_type)
        direct_candidates: list[str] = []

        if prefix is not None:
            direct_candidates.append(prefix + name)
            direct_candidates.append(prefix + slug)
        else:
            direct_candidates.append(name)
            direct_candidates.append(slug)

        for candidate in direct_candidates:
            if candidate in translation_map:
                mapped = translation_map[candidate]
                stats["matched_direct"] += 1
                stats["per_type"][tag_type]["direct"] += 1
                break

        if mapped is None:
            for candidate in (name, slug):
                if candidate in unique_suffix:
                    mapped = unique_suffix[candidate]
                    stats["matched_unique_suffix"] += 1
                    stats["per_type"][tag_type]["unique_suffix"] += 1
                    break

        if mapped is None:
            stats["unmatched"] += 1
            stats["per_type"][tag_type]["unmatched"] += 1
            continue

        row["name_zh"] = mapped

    return stats


def main() -> None:
    parser = argparse.ArgumentParser(description="Apply EH tag translations to local tag seed.")
    parser.add_argument(
        "--seed",
        default="app/src/main/assets/tag_catalog_seed.json",
        help="Path to tag seed json",
    )
    parser.add_argument(
        "--url",
        default=DEFAULT_TRANSLATION_URL,
        help="EH translation data URL",
    )
    parser.add_argument(
        "--output",
        default="",
        help="Output path (default overwrite seed)",
    )
    parser.add_argument(
        "--plain-output",
        default="",
        help="Optional plain translation json output path (utf-8, not base64)",
    )
    args = parser.parse_args()

    seed_path = Path(args.seed)
    out_path = Path(args.output) if args.output else seed_path

    if not seed_path.exists():
        raise FileNotFoundError(f"Seed file not found: {seed_path}")

    with seed_path.open("r", encoding="utf-8-sig") as f:
        seed = json.load(f)
    if not isinstance(seed, list):
        raise ValueError(f"Seed json must be list: {seed_path}")

    raw = download_bytes(args.url)
    translation_map = parse_eh_translation_bytes(raw)
    if not translation_map:
        raise RuntimeError("Failed to parse EH translation map from source")

    stats = apply_translations(seed, translation_map)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(seed, f, ensure_ascii=False, indent=4)
        f.write("\n")

    if args.plain_output:
        plain_path = Path(args.plain_output)
        plain_path.parent.mkdir(parents=True, exist_ok=True)
        plain_rows = [
            {"key": key, "zh": zh}
            for key, zh in sorted(translation_map.items(), key=lambda kv: kv[0])
        ]
        with plain_path.open("w", encoding="utf-8") as f:
            json.dump(plain_rows, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"plain translation file: {plain_path}")

    print(f"seed file: {seed_path}")
    print(f"output file: {out_path}")
    print(f"translation source entries: {len(translation_map)}")
    print(
        "mapped:"
        f" direct={stats['matched_direct']},"
        f" unique_suffix={stats['matched_unique_suffix']},"
        f" unmatched={stats['unmatched']},"
        f" total={stats['total']}"
    )
    for tag_type in sorted(stats["per_type"].keys()):
        s = stats["per_type"][tag_type]
        print(
            f"  {tag_type}: direct={s['direct']}, "
            f"unique_suffix={s['unique_suffix']}, unmatched={s['unmatched']}"
        )


if __name__ == "__main__":
    main()
