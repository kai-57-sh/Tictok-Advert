#!/usr/bin/env python3
from __future__ import annotations

import concurrent.futures
import hashlib
import json
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "offline_ads"
IMAGES_DIR = ASSET_ROOT / "images"
VIDEOS_DIR = ASSET_ROOT / "videos"
MANIFEST_PATH = ASSET_ROOT / "manifest.json"

HEADERS = {
    "User-Agent": "TictokAdvertOfflineSeeder/1.0",
}

FIELDS = ",".join(
    [
        "code",
        "product_name",
        "brands",
        "categories",
        "categories_tags",
        "image_front_small_url",
        "image_small_url",
        "image_url",
        "ingredients_text",
        "quantity",
        "countries",
    ]
)

SOURCE_CONFIGS = [
    {
        "key": "food",
        "label": "Open Food Facts",
        "base_url": "https://world.openfoodfacts.org/cgi/search.pl",
        "product_url_prefix": "https://world.openfoodfacts.org/product/",
        "params": {
            "action": "process",
            "json": 1,
            "page_size": 120,
            "page": 1,
            "sort_by": "unique_scans_n",
            "fields": FIELDS,
        },
    },
    {
        "key": "beauty",
        "label": "Open Beauty Facts",
        "base_url": "https://world.openbeautyfacts.org/cgi/search.pl",
        "product_url_prefix": "https://world.openbeautyfacts.org/product/",
        "params": {
            "action": "process",
            "json": 1,
            "page_size": 120,
            "page": 1,
            "sort_by": "unique_scans_n",
            "fields": FIELDS,
        },
    },
    {
        "key": "products",
        "label": "Open Products Facts",
        "base_url": "https://world.openproductsfacts.org/cgi/search.pl",
        "product_url_prefix": "https://world.openproductsfacts.org/product/",
        "params": {
            "action": "process",
            "json": 1,
            "page_size": 120,
            "page": 1,
            "sort_by": "unique_scans_n",
            "fields": FIELDS,
        },
    },
]

VIDEO_LIBRARY = [
    {
        "id": "mov_bbb",
        "url": "https://www.w3schools.com/html/mov_bbb.mp4",
    },
    {
        "id": "movie",
        "url": "https://www.w3schools.com/html/movie.mp4",
    },
    {
        "id": "sample_5s",
        "url": "https://samplelib.com/lib/preview/mp4/sample-5s.mp4",
    },
    {
        "id": "sample_10s",
        "url": "https://samplelib.com/lib/preview/mp4/sample-10s.mp4",
    },
]

DEFAULT_TAGS = {
    "food": ["美味", "囤货", "日常回购"],
    "beauty": ["护肤", "个护", "高颜值"],
    "products": ["品质生活", "家居好物", "精选推荐"],
}

TAG_RULES = [
    (("coffee", "espresso", "latte", "cappuccino"), "咖啡"),
    (("tea", "matcha", "green tea", "black tea"), "茶饮"),
    (("chocolate", "cocoa"), "巧克力"),
    (("cookie", "biscuit", "cracker", "wafer"), "零食"),
    (("chips", "crisps", "snack"), "零食"),
    (("cereal", "granola", "muesli", "breakfast"), "早餐"),
    (("noodle", "ramen", "pasta"), "速食"),
    (("water", "juice", "soda", "drink", "beverage"), "饮品"),
    (("organic", "bio"), "有机"),
    (("sugar free", "sans sucres", "no sugar", "zero"), "低糖"),
    (("protein", "vitamin", "supplement"), "营养补给"),
    (("shampoo", "conditioner", "hair"), "洗护"),
    (("cream", "serum", "lotion", "mask", "cleanser", "skin"), "护肤"),
    (("lipstick", "makeup", "cosmetic", "mascara"), "彩妆"),
    (("soap", "body wash", "bath", "deodorant"), "个护"),
    (("perfume", "fragrance", "parfum"), "香氛"),
    (("baby", "kids"), "家庭常备"),
    (("home", "kitchen", "house"), "家居好物"),
]


def fetch_json(url: str) -> dict[str, Any]:
    last_error: Exception | None = None
    for attempt in range(3):
        request = Request(url, headers=HEADERS)
        try:
            with urlopen(request, timeout=45) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as exc:
            last_error = exc
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"Failed to fetch JSON from {url}: {last_error}")


def download_binary(url: str, destination: Path) -> None:
    last_error: Exception | None = None
    for attempt in range(3):
        request = Request(url, headers=HEADERS)
        try:
            with urlopen(request, timeout=90) as response:
                payload = response.read()
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(payload)
            return
        except Exception as exc:
            last_error = exc
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"Failed to download {url}: {last_error}")


def clean_text(value: Any) -> str:
    if value is None:
        return ""
    text = str(value)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def short_text(value: str, limit: int) -> str:
    value = clean_text(value)
    if len(value) <= limit:
        return value
    return value[: limit - 1].rstrip() + "…"


def stable_int(seed: str, low: int, high: int) -> int:
    digest = hashlib.sha1(seed.encode("utf-8")).hexdigest()
    value = int(digest[:8], 16)
    return low + value % (high - low + 1)


def infer_tags(source_key: str, product_name: str, brands: str, categories: str) -> list[str]:
    haystack = " ".join([product_name, brands, categories]).lower()
    tags: list[str] = []
    for keywords, tag in TAG_RULES:
        if any(keyword in haystack for keyword in keywords) and tag not in tags:
            tags.append(tag)
    for fallback in DEFAULT_TAGS[source_key]:
        if fallback not in tags:
            tags.append(fallback)
    return tags[:4]


def infer_channel(source_key: str, tags: list[str], position: int) -> str:
    if source_key == "food":
        return "本地" if position % 4 != 0 else "电商"
    if source_key == "beauty":
        return "精选" if position % 3 != 0 else "电商"
    return "电商" if position % 5 != 0 else "精选"


def build_description(source_label: str, item: dict[str, Any], tags: list[str]) -> str:
    details = []
    quantity = clean_text(item.get("quantity"))
    countries = clean_text(item.get("countries"))
    ingredients = short_text(clean_text(item.get("ingredients_text")), 54)
    categories = short_text(clean_text(item.get("categories")), 40)

    if quantity:
        details.append(f"规格 {quantity}")
    if countries:
        details.append(f"来源 {countries}")
    if ingredients:
        details.append(f"配方信息 {ingredients}")
    elif categories:
        details.append(f"分类 {categories}")

    detail_text = "；".join(details[:3])
    return f"{source_label} 实时抓取素材，主打 {'、'.join(tags[:3])}。{detail_text}".strip()


def build_summary(source_label: str, brand: str, tags: list[str]) -> str:
    return (
        f"AI 推荐理由：这条素材突出 {'、'.join(tags[:3])}，品牌为 {brand}，"
        f"来源于 {source_label} 实时抓取并已完成本地离线缓存。"
    )


def normalize_product(config: dict[str, Any], item: dict[str, Any]) -> dict[str, Any] | None:
    code = clean_text(item.get("code"))
    product_name = clean_text(item.get("product_name"))
    if not code or not product_name:
        return None

    image_url = (
        clean_text(item.get("image_front_small_url"))
        or clean_text(item.get("image_small_url"))
        or clean_text(item.get("image_url"))
    )
    if not image_url:
        return None

    brands = clean_text(item.get("brands"))
    brand = brands.split(",")[0].strip() if brands else config["label"]
    categories = clean_text(item.get("categories"))
    categories_tags = item.get("categories_tags") or []
    category_blob = " ".join([categories, " ".join(categories_tags)])
    tags = infer_tags(config["key"], product_name, brand, category_blob)
    title = product_name if brand.lower() in product_name.lower() else f"{brand} {product_name}"

    return {
        "source_key": config["key"],
        "source_label": config["label"],
        "source_url": f"{config['product_url_prefix']}{code}",
        "code": code,
        "brand": brand,
        "title": short_text(title, 68),
        "description": short_text(build_description(config["label"], item, tags), 140),
        "summary": short_text(build_summary(config["label"], brand, tags), 120),
        "tags": tags,
        "image_url": image_url,
    }


def fetch_source_records(config: dict[str, Any]) -> list[dict[str, Any]]:
    url = f"{config['base_url']}?{urlencode(config['params'])}"
    payload = fetch_json(url)
    products = payload.get("products") or []
    normalized: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()
    for item in products:
        candidate = normalize_product(config, item)
        if candidate is None:
            continue
        dedupe_key = (candidate["brand"].lower(), candidate["title"].lower())
        if dedupe_key in seen:
            continue
        seen.add(dedupe_key)
        normalized.append(candidate)
    return normalized


def prepare_video_assets() -> list[dict[str, str]]:
    prepared: list[dict[str, str]] = []
    for item in VIDEO_LIBRARY:
        destination = VIDEOS_DIR / f"{item['id']}.mp4"
        try:
            if not destination.exists() or destination.stat().st_size == 0:
                print(f"Downloading video {item['url']}")
                download_binary(item["url"], destination)
            prepared.append(
                {
                    "remote_url": item["url"],
                    "asset_path": f"offline_ads/videos/{destination.name}",
                }
            )
        except Exception as exc:
            print(f"Video download failed for {item['url']}: {exc}", file=sys.stderr)
    return prepared


def download_image_task(task: tuple[str, str, Path]) -> tuple[str, bool, str]:
    ad_id, url, destination = task
    try:
        if not destination.exists() or destination.stat().st_size == 0:
            download_binary(url, destination)
        return ad_id, True, ""
    except Exception as exc:
        return ad_id, False, str(exc)


def assemble_manifest_records(candidates: list[dict[str, Any]], limit: int = 100) -> list[dict[str, Any]]:
    video_assets = prepare_video_assets()
    ads: list[dict[str, Any]] = []
    image_tasks: list[tuple[str, str, Path]] = []
    target_buffer = max(limit + 30, int(limit * 1.3))
    video_assignment_index = 0

    position = 0
    for candidate in candidates:
        if len(ads) >= target_buffer:
            break

        position += 1
        ad_id = f"ad_{len(ads) + 1:03d}"
        channel = infer_channel(candidate["source_key"], candidate["tags"], position)
        cover_name = f"{ad_id}.jpg"
        cover_path = IMAGES_DIR / cover_name
        card_type = "big_image" if len(ads) % 2 == 0 else "small_image"
        video_asset_path = ""
        video_url = ""
        if video_assets and len(ads) % 10 == 0:
            selected_video = video_assets[video_assignment_index % len(video_assets)]
            video_assignment_index += 1
            card_type = "video"
            video_asset_path = selected_video["asset_path"]
            video_url = selected_video["remote_url"]

        ads.append(
            {
                "id": ad_id,
                "channel": channel,
                "cardType": card_type,
                "title": candidate["title"],
                "description": candidate["description"],
                "advertiserName": candidate["brand"],
                "coverUrl": candidate["image_url"],
                "videoUrl": video_url,
                "summary": candidate["summary"],
                "tags": ",".join(candidate["tags"]),
                "likeCount": stable_int(candidate["code"] + "like", 900, 5800),
                "favoriteCount": stable_int(candidate["code"] + "fav", 260, 3200),
                "shareCount": stable_int(candidate["code"] + "share", 60, 900),
                "impressions": stable_int(candidate["code"] + "imp", 6000, 65000),
                "clicks": stable_int(candidate["code"] + "clk", 600, 7200),
                "coverAssetPath": f"offline_ads/images/{cover_name}",
                "videoAssetPath": video_asset_path,
                "sourceName": candidate["source_label"],
                "sourcePageUrl": candidate["source_url"],
            }
        )
        image_tasks.append((ad_id, candidate["image_url"], cover_path))

    download_results: dict[str, tuple[bool, str]] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        future_map = {executor.submit(download_image_task, task): task[0] for task in image_tasks}
        for future in concurrent.futures.as_completed(future_map):
            ad_id, ok, error = future.result()
            download_results[ad_id] = (ok, error)

    successful_ads: list[dict[str, Any]] = []
    for ad in ads:
        ok, error = download_results.get(ad["id"], (False, "missing download result"))
        if ok:
            successful_ads.append(ad)
        else:
            print(f"Skipping {ad['id']} due to image download failure: {error}", file=sys.stderr)

    if len(successful_ads) < limit:
        raise RuntimeError(f"Only prepared {len(successful_ads)} ads; expected {limit}.")

    return successful_ads[:limit]


def collect_candidates() -> list[dict[str, Any]]:
    buckets: list[list[dict[str, Any]]] = []
    for config in SOURCE_CONFIGS:
        try:
            records = fetch_source_records(config)
            print(f"{config['label']}: fetched {len(records)} records")
            if records:
                buckets.append(records)
        except Exception as exc:
            print(f"Source fetch failed for {config['label']}: {exc}", file=sys.stderr)

    combined: list[dict[str, Any]] = []
    index = 0
    while buckets:
        next_buckets: list[list[dict[str, Any]]] = []
        for bucket in buckets:
            if index < len(bucket):
                combined.append(bucket[index])
            if index + 1 < len(bucket):
                next_buckets.append(bucket)
        buckets = next_buckets
        index += 1

    if len(combined) < 100:
        raise RuntimeError(f"Only collected {len(combined)} candidate records from source APIs.")

    return combined


def prune_unused_assets(manifest_ads: list[dict[str, Any]]) -> None:
    used_image_names = {
        Path(ad["coverAssetPath"]).name
        for ad in manifest_ads
        if ad.get("coverAssetPath")
    }
    used_video_names = {
        Path(ad["videoAssetPath"]).name
        for ad in manifest_ads
        if ad.get("videoAssetPath")
    }

    for file in IMAGES_DIR.glob("ad_*.jpg"):
        if file.name not in used_image_names:
            file.unlink(missing_ok=True)

    for file in VIDEOS_DIR.glob("*.mp4"):
        if file.name not in used_video_names:
            file.unlink(missing_ok=True)


def main() -> int:
    ASSET_ROOT.mkdir(parents=True, exist_ok=True)
    IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    VIDEOS_DIR.mkdir(parents=True, exist_ok=True)

    candidates = collect_candidates()
    manifest_ads = assemble_manifest_records(candidates, limit=100)
    prune_unused_assets(manifest_ads)
    manifest = {
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "recordCount": len(manifest_ads),
        "ads": manifest_ads,
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(manifest_ads)} ads to {MANIFEST_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
