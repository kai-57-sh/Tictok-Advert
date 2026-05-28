package com.example.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object OfflineAdBundleLoader {
    private const val manifestAssetPath = "offline_ads/manifest.json"

    data class OfflineAdBundle(
        val version: String,
        val ads: List<AdEntity>
    )

    fun getBundleVersion(context: Context): String {
        val manifestPayload = readManifestPayload(context)
        return resolveBundleVersion(manifestPayload.text, manifestPayload.json)
    }

    fun loadBundle(context: Context): OfflineAdBundle {
        val manifestPayload = readManifestPayload(context)
        val adsArray = manifestPayload.json.getJSONArray("ads")
        val version = resolveBundleVersion(manifestPayload.text, manifestPayload.json)

        val ads = buildList(adsArray.length()) {
            for (index in 0 until adsArray.length()) {
                val item = adsArray.getJSONObject(index)
                val coverAssetPath = item.optString("coverAssetPath").takeIf { it.isNotBlank() }
                val videoAssetPath = item.optString("videoAssetPath").takeIf { it.isNotBlank() }

                add(
                    AdEntity(
                        id = item.getString("id"),
                        channel = item.getString("channel"),
                        cardType = item.getString("cardType"),
                        title = item.getString("title"),
                        description = item.getString("description"),
                        advertiserName = item.getString("advertiserName"),
                        coverUrl = item.optString("coverUrl"),
                        videoUrl = item.optString("videoUrl"),
                        summary = item.getString("summary"),
                        tags = item.getString("tags"),
                        likeCount = item.optInt("likeCount"),
                        favoriteCount = item.optInt("favoriteCount"),
                        shareCount = item.optInt("shareCount"),
                        impressions = item.optInt("impressions"),
                        clicks = item.optInt("clicks"),
                        localCoverPath = coverAssetPath?.let { copyAssetToFilesDir(context, it) },
                        localVideoPath = videoAssetPath?.let { copyAssetToFilesDir(context, it) }
                    )
                )
            }
        }

        return OfflineAdBundle(
            version = version,
            ads = ads
        )
    }

    private data class ManifestPayload(
        val text: String,
        val json: JSONObject
    )

    private fun readManifestPayload(context: Context): ManifestPayload {
        val manifestText = context.assets.open(manifestAssetPath).bufferedReader().use { it.readText() }
        return ManifestPayload(
            text = manifestText,
            json = JSONObject(manifestText)
        )
    }

    private fun resolveBundleVersion(manifestText: String, manifest: JSONObject): String {
        return manifest.optString("generatedAt").ifBlank { manifestText.hashCode().toString() }
    }

    private fun copyAssetToFilesDir(context: Context, assetPath: String): String {
        require(assetPath.startsWith("offline_ads/")) { "Unexpected asset path: $assetPath" }

        val outputFile = File(context.filesDir, assetPath)
        outputFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        return outputFile.absolutePath
    }
}
