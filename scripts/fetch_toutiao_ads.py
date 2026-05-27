#!/usr/bin/env python3
from __future__ import annotations

import concurrent.futures
import hashlib
import html
import json
import re
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import quote
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parent.parent
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets" / "offline_ads"
IMAGES_DIR = ASSET_ROOT / "images"
VIDEOS_DIR = ASSET_ROOT / "videos"
MANIFEST_PATH = ASSET_ROOT / "manifest.json"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    )
}

ZH_RE = re.compile(r"[\u4e00-\u9fff]")
JSON_BLOCK_RE = re.compile(
    r"<script[^>]*data-druid-card-data-id[^>]*type=[\"']application/json[\"'][^>]*>(.*?)</script>"
)


@dataclass(frozen=True)
class QueryConfig:
    query: str
    channel: str
    tags: tuple[str, str, str]
    wants_video: bool = False


QUERY_CONFIGS = [
    QueryConfig("口红推荐", "精选", ("彩妆", "显白", "通勤妆"), True),
    QueryConfig("香水推荐", "精选", ("香氛", "礼物", "高级感")),
    QueryConfig("防晒推荐", "精选", ("防晒", "夏季", "通勤"), True),
    QueryConfig("护手霜推荐", "精选", ("护手", "保湿", "随身护理")),
    QueryConfig("粉底液推荐", "精选", ("底妆", "服帖", "持妆")),
    QueryConfig("腮红推荐", "精选", ("彩妆", "氛围感", "元气")),
    QueryConfig("面霜推荐", "精选", ("护肤", "修护", "保湿")),
    QueryConfig("隔离霜推荐", "精选", ("底妆", "提亮", "日常防护")),
    QueryConfig("散粉推荐", "精选", ("底妆", "控油", "持妆")),
    QueryConfig("唇釉推荐", "精选", ("彩妆", "显色", "高级感")),
    QueryConfig("睫毛膏推荐", "精选", ("彩妆", "纤长", "放大双眼")),
    QueryConfig("耳机推荐", "电商", ("数码", "降噪", "通勤"), True),
    QueryConfig("手机推荐", "电商", ("数码", "性能", "旗舰"), True),
    QueryConfig("跑鞋推荐", "电商", ("运动", "缓震", "轻量"), True),
    QueryConfig("电脑椅推荐", "电商", ("家居", "办公", "舒适")),
    QueryConfig("空气炸锅推荐", "电商", ("厨房电器", "高效", "家居"), True),
    QueryConfig("家居好物", "电商", ("家居", "收纳", "氛围感"), True),
    QueryConfig("行李箱推荐", "电商", ("出行", "耐用", "大容量")),
    QueryConfig("吹风机推荐", "电商", ("个护电器", "速干", "居家")),
    QueryConfig("电动牙刷推荐", "电商", ("个护", "清洁", "智能护理"), True),
    QueryConfig("咖啡推荐", "本地", ("咖啡", "下午茶", "品质生活"), True),
    QueryConfig("火锅推荐", "本地", ("火锅", "聚会", "本地生活"), True),
    QueryConfig("奶茶推荐", "本地", ("饮品", "热门打卡", "下午茶")),
    QueryConfig("轻食推荐", "本地", ("轻食", "健康饮食", "本地生活")),
    QueryConfig("烘焙推荐", "本地", ("烘焙", "甜品", "周末好去处")),
    QueryConfig("本地探店", "本地", ("探店", "周末好去处", "城市生活")),
]

TARGET_AD_COUNT = 100
TARGET_PER_QUERY = 4
SELECTION_BUFFER = 120


def fetch_text(url: str) -> str:
    last_error: Exception | None = None
    for attempt in range(3):
        request = Request(url, headers=HEADERS)
        try:
            with urlopen(request, timeout=30) as response:
                return response.read().decode("utf-8", "ignore")
        except Exception as exc:
            last_error = exc
            time.sleep(1.2 * (attempt + 1))
    raise RuntimeError(f"Failed to fetch {url}: {last_error}")


def download_binary(url: str, destination: Path) -> None:
    last_error: Exception | None = None
    for attempt in range(3):
        request = Request(url, headers=HEADERS)
        try:
            with urlopen(request, timeout=60) as response:
                payload = response.read()
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(payload)
            return
        except Exception as exc:
            last_error = exc
            time.sleep(1.2 * (attempt + 1))
    raise RuntimeError(f"Failed to download {url}: {last_error}")


def clean_text(value: Any) -> str:
    if value is None:
        return ""
    text = html.unescape(str(value))
    text = text.replace("\u200b", " ").replace("\x02", " ")
    text = re.sub(r"<[^>]+>", "", text)
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


def chinese_count(text: str) -> int:
    return len(ZH_RE.findall(text))


def guess_image_extension(url: str) -> str:
    lowered = url.lower()
    for ext in (".jpg", ".jpeg", ".png", ".webp"):
        if ext in lowered:
            return ext
    return ".jpg"


def build_search_url(query: str, kind: str) -> str:
    return (
        "https://so.toutiao.com/search?"
        f"keyword={quote(query)}&pd={kind}&dvpf=pc&source=input"
    )


def extract_json_cards(html_text: str) -> list[dict[str, Any]]:
    cards: list[dict[str, Any]] = []
    for raw in JSON_BLOCK_RE.findall(html_text):
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            continue
        data = payload.get("data")
        if isinstance(data, dict):
            cards.append(data)
    return cards


def build_title(query: str, raw_title: str) -> str:
    title = clean_text(raw_title)
    title = re.sub(r"[_|｜-]?\s*(哔哩哔哩|bilibili)$", "", title, flags=re.IGNORECASE)
    title = re.sub(r"\s*_+\s*", " ", title)
    if query.replace("推荐", "") not in title:
        title = f"{query}｜{title}"
    return short_text(title, 38)


def normalize_info_candidate(config: QueryConfig, data: dict[str, Any]) -> dict[str, Any] | None:
    display = data.get("display") or {}
    display_info = display.get("info") or {}
    title = clean_text(
        data.get("title")
        or (display.get("title") or {}).get("text")
    )
    if chinese_count(title) < 4:
        return None

    image_url = clean_text(
        data.get("large_image_url")
        or data.get("image_url")
        or ((display_info.get("images") or [None])[0])
    )
    if not image_url:
        return None
    if image_url.startswith("http://"):
        image_url = "https://" + image_url[len("http://"):]

    source = clean_text(
        data.get("source")
        or display_info.get("site_name")
        or "今日头条"
    )
    summary = clean_text(
        data.get("abstract")
        or data.get("summary_content")
        or (display.get("summary") or {}).get("text")
    )
    share_url = clean_text(
        data.get("share_url")
        or data.get("url")
        or data.get("open_url")
        or f"https://toutiao.com/group/{clean_text(data.get('group_id'))}/"
    )
    if not share_url:
        return None

    ad_title = build_title(config.query, title)
    ad_description = short_text(
        f"头条热门{config.query}素材，{summary or title}",
        96,
    )
    ad_summary = short_text(
        f"AI 推荐理由：素材来自今日头条搜索，围绕 {'、'.join(config.tags)} 需求，"
        f"适合在{config.channel}频道做兴趣分发与转化承接。",
        100,
    )

    return {
        "query": config.query,
        "channel": config.channel,
        "tags": list(config.tags),
        "title": ad_title,
        "description": ad_description,
        "summary": ad_summary,
        "advertiser_name": short_text(source or "今日头条热榜", 18),
        "cover_url": image_url,
        "source_name": "今日头条搜索",
        "source_page_url": share_url,
    }


def fetch_info_bucket(config: QueryConfig) -> tuple[str, list[dict[str, Any]]]:
    html_text = fetch_text(build_search_url(config.query, "information"))
    bucket: list[dict[str, Any]] = []
    seen: set[str] = set()
    for data in extract_json_cards(html_text):
        candidate = normalize_info_candidate(config, data)
        if candidate is None:
            continue
        dedupe_key = candidate["source_page_url"]
        if dedupe_key in seen:
            continue
        seen.add(dedupe_key)
        bucket.append(candidate)
    return config.query, bucket


def select_candidates(buckets: dict[str, list[dict[str, Any]]], target_count: int) -> list[dict[str, Any]]:
    selected: list[dict[str, Any]] = []
    global_seen: set[str] = set()

    for round_index in range(TARGET_PER_QUERY):
        for config in QUERY_CONFIGS:
            bucket = buckets.get(config.query, [])
            if round_index >= len(bucket):
                continue
            item = bucket[round_index]
            dedupe_key = item["source_page_url"]
            if dedupe_key in global_seen:
                continue
            global_seen.add(dedupe_key)
            selected.append(item)

    if len(selected) < target_count:
        for config in QUERY_CONFIGS:
            bucket = buckets.get(config.query, [])
            for item in bucket[TARGET_PER_QUERY:]:
                dedupe_key = item["source_page_url"]
                if dedupe_key in global_seen:
                    continue
                global_seen.add(dedupe_key)
                selected.append(item)
                if len(selected) >= target_count:
                    return selected[:target_count]

    return selected[:target_count]


def download_image_task(task: tuple[str, str, Path]) -> tuple[str, bool, str]:
    ad_id, url, destination = task
    try:
        download_binary(url, destination)
        return ad_id, True, ""
    except Exception as exc:
        return ad_id, False, str(exc)


def normalize_video_result(config: QueryConfig, data: dict[str, Any]) -> dict[str, str] | None:
    display = data.get("display") or {}
    display_info = display.get("info") or {}
    title = clean_text(
        (display.get("title") or {}).get("text")
        or data.get("title")
    )
    url = clean_text(data.get("url") or display_info.get("url"))
    if not title or not url:
        return None
    if not url.startswith("http"):
        return None
    return {
        "query": config.query,
        "title": short_text(title, 48),
        "url": url,
    }


def fetch_video_candidates(config: QueryConfig) -> list[dict[str, str]]:
    html_text = fetch_text(build_search_url(config.query, "video"))
    results: list[dict[str, str]] = []
    seen: set[str] = set()
    for data in extract_json_cards(html_text):
        candidate = normalize_video_result(config, data)
        if candidate is None:
            continue
        if candidate["url"] in seen:
            continue
        seen.add(candidate["url"])
        results.append(candidate)
    return results


def download_video_with_ytdlp(url: str, output_stem: str) -> Path:
    template = VIDEOS_DIR / f"{output_stem}.%(ext)s"
    before = set(VIDEOS_DIR.glob(f"{output_stem}.*"))
    command = [
        "uvx",
        "yt-dlp",
        "--no-part",
        "-f",
        "best[height<=360]/bestvideo[height<=360]/bestvideo/best",
        "-o",
        str(template),
        url,
    ]
    result = subprocess.run(
        command,
        cwd=ROOT,
        capture_output=True,
        text=True,
        timeout=240,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip() or "yt-dlp failed")

    after = [path for path in VIDEOS_DIR.glob(f"{output_stem}.*") if path not in before]
    if not after:
        after = list(VIDEOS_DIR.glob(f"{output_stem}.*"))
    if not after:
        raise RuntimeError("yt-dlp completed without output file")

    output_path = max(after, key=lambda item: item.stat().st_mtime)
    if output_path.stat().st_size < 50 * 1024:
        raise RuntimeError(f"downloaded file is too small: {output_path.stat().st_size} bytes")
    return output_path


def prepare_videos() -> dict[str, Path]:
    prepared: dict[str, Path] = {}
    for config in QUERY_CONFIGS:
        if not config.wants_video:
            continue

        try:
            candidates = fetch_video_candidates(config)
        except Exception as exc:
            print(f"Video search failed for {config.query}: {exc}", file=sys.stderr)
            continue

        for index, candidate in enumerate(candidates[:4], start=1):
            stem = f"video_{slugify(config.query)}_{index}"
            try:
                path = download_video_with_ytdlp(candidate["url"], stem)
                prepared[config.query] = path
                print(f"Video ready for {config.query}: {path.name}")
                break
            except Exception as exc:
                print(f"Video download failed for {config.query} -> {candidate['url']}: {exc}", file=sys.stderr)
    return prepared


def slugify(value: str) -> str:
    value = re.sub(r"[^\w\u4e00-\u9fff]+", "_", value)
    value = value.strip("_")
    if not value:
        return "item"
    return value


def prune_unused_assets(used_cover_names: set[str], used_video_names: set[str]) -> None:
    for file in IMAGES_DIR.glob("ad_*.*"):
        if file.name not in used_cover_names:
            file.unlink(missing_ok=True)

    for file in VIDEOS_DIR.glob("*.*"):
        if file.name not in used_video_names:
            file.unlink(missing_ok=True)


def main() -> int:
    ASSET_ROOT.mkdir(parents=True, exist_ok=True)
    IMAGES_DIR.mkdir(parents=True, exist_ok=True)
    VIDEOS_DIR.mkdir(parents=True, exist_ok=True)

    buckets: dict[str, list[dict[str, Any]]] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
        future_map = {executor.submit(fetch_info_bucket, config): config.query for config in QUERY_CONFIGS}
        for future in concurrent.futures.as_completed(future_map):
            query = future_map[future]
            try:
                _, bucket = future.result()
                buckets[query] = bucket
                print(f"{query}: {len(bucket)} 条候选")
            except Exception as exc:
                print(f"{query}: 抓取失败 - {exc}", file=sys.stderr)
                buckets[query] = []

    candidates = select_candidates(buckets, target_count=SELECTION_BUFFER)
    if len(candidates) < TARGET_AD_COUNT:
        raise RuntimeError(f"只抓到 {len(candidates)} 条可用头条中文素材，未达到 {TARGET_AD_COUNT} 条。")

    video_assets = prepare_videos()

    ads: list[dict[str, Any]] = []
    image_tasks: list[tuple[str, str, Path]] = []
    assigned_video_queries: set[str] = set()

    for index, candidate in enumerate(candidates, start=1):
        ad_id = f"ad_{index:03d}"
        image_ext = guess_image_extension(candidate["cover_url"])
        cover_name = f"{ad_id}{image_ext}"
        cover_path = IMAGES_DIR / cover_name
        image_tasks.append((ad_id, candidate["cover_url"], cover_path))

        query = candidate["query"]
        video_path = video_assets.get(query)
        card_type = "big_image" if index % 2 == 1 else "small_image"
        video_asset_path = ""
        video_url = ""
        if video_path is not None and query not in assigned_video_queries:
            assigned_video_queries.add(query)
            card_type = "video"
            video_asset_path = f"offline_ads/videos/{video_path.name}"
            video_url = candidate["source_page_url"]

        stats_seed = candidate["title"] + candidate["source_page_url"]
        ads.append(
            {
                "id": ad_id,
                "channel": candidate["channel"],
                "cardType": card_type,
                "title": candidate["title"],
                "description": candidate["description"],
                "advertiserName": candidate["advertiser_name"],
                "coverUrl": candidate["cover_url"],
                "videoUrl": video_url,
                "summary": candidate["summary"],
                "tags": ",".join(candidate["tags"]),
                "likeCount": stable_int(stats_seed + "like", 1200, 6800),
                "favoriteCount": stable_int(stats_seed + "fav", 350, 3800),
                "shareCount": stable_int(stats_seed + "share", 90, 980),
                "impressions": stable_int(stats_seed + "imp", 9000, 78000),
                "clicks": stable_int(stats_seed + "clk", 800, 8600),
                "coverAssetPath": f"offline_ads/images/{cover_name}",
                "videoAssetPath": video_asset_path,
                "sourceName": candidate["source_name"],
                "sourcePageUrl": candidate["source_page_url"],
            }
        )

    download_results: dict[str, tuple[bool, str]] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        future_map = {executor.submit(download_image_task, task): task[0] for task in image_tasks}
        for future in concurrent.futures.as_completed(future_map):
            ad_id, ok, error = future.result()
            download_results[ad_id] = (ok, error)

    final_ads: list[dict[str, Any]] = []
    for ad in ads:
        ok, error = download_results.get(ad["id"], (False, "missing download result"))
        if ok:
            final_ads.append(ad)
        else:
            print(f"Image download failed for {ad['id']}: {error}", file=sys.stderr)

    if len(final_ads) < TARGET_AD_COUNT:
        raise RuntimeError(f"图片下载后仅保留 {len(final_ads)} 条素材，未达到 {TARGET_AD_COUNT} 条。")

    final_ads = final_ads[:TARGET_AD_COUNT]
    used_cover_names = {Path(ad["coverAssetPath"]).name for ad in final_ads}
    used_video_names = {
        Path(ad["videoAssetPath"]).name
        for ad in final_ads
        if ad.get("videoAssetPath")
    }
    prune_unused_assets(used_cover_names, used_video_names)

    manifest = {
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "recordCount": len(final_ads),
        "ads": final_ads,
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"已写入 {len(final_ads)} 条今日头条中文广告素材 -> {MANIFEST_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
