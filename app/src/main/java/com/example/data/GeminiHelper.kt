package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchIntent(
    val channel: String?,
    val tags: List<String>,
    val explanation: String
)

object GeminiHelper {
    private const val TAG = "GeminiHelper"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Parses the natural language query into structural filters using Gemini 3.5 Flash.
     * Falls back to high-quality keyword parsing if the API fails or key is missing.
     */
    suspend fun parseSearchQuery(query: String): SearchIntent = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
            Log.w(TAG, "Gemini API Key is missing or default. Using high-quality local parser.")
            return@withContext parseMockQuery(query)
        }

        val systemPrompt = """
            You are a precision-focused AI search assistant for an Android ad feed app called "Tictok Advert".
            Your task is to parse the user's natural language query into structural search filters.
            
            PRECISION RULES:
            - If the user mentions a specific product type like "球鞋" (sneaker), "跑鞋" (running shoe), or "运动鞋", you MUST include "运动" in the tags.
            - If the user mentions "吃", "餐厅", "火锅", you MUST include "美味" in the tags.
            - Match "channel" strictly: "精选", "电商", "本地". Products for sale -> "电商", Local services/food -> "本地".
            
            Provide output ONLY in raw JSON format.
            The output JSON MUST follow this schema:
            {
               "channel": "精选" | "电商" | "本地" | null,
               "tags": [string, string, ...],
               "explanation": string
            }
            - "tags" should capture search tags. Choose matching words like "运动", "学生党", "性价比", "本地生活", "通勤", "家居", "美味", "生活服务", "数码推荐" etc.
            - "explanation" should be professional, written in exactly 1 brief Chinese sentence.
            
            Example:
            Input: "我想看适合打工人的平价球鞋"
            Output: {"channel":"电商","tags":["运动","性价比","通勤"],"explanation":"已为您精准定位到适合通勤、主打性价比的运动球鞋系列广告。"}
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "User query: $query") })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        try {
            val urlWithKey = "$API_URL?key=$apiKey"
            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(urlWithKey)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errString = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API request failed: code ${response.code}, body: $errString")
                    return@withContext parseMockQuery(query)
                }

                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Gemini Raw Response: $bodyStr")
                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val textOutput = firstPart?.optString("text") ?: ""

                val cleanText = textOutput.trim().removeSurrounding("```json", "```").trim()
                Log.d(TAG, "Gemini Clean Text: $cleanText")

                val parsedJson = JSONObject(cleanText)
                val channel = if (parsedJson.isNull("channel")) null else parsedJson.getString("channel")
                val jsonTags = parsedJson.optJSONArray("tags") ?: JSONArray()
                val tags = mutableListOf<String>()
                for (i in 0 until jsonTags.length()) {
                    tags.add(jsonTags.getString(i))
                }
                val explanation = parsedJson.optString("explanation", "已解析您的搜索意图，并为您搜索相关广告。")

                SearchIntent(channel, tags, explanation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini API: ${e.message}", e)
            parseMockQuery(query)
        }
    }

    /**
     * Highly responsive keyword rule-based mock query parsing as a flawless local fallback.
     */
    fun parseMockQuery(query: String): SearchIntent {
        val lowercaseQuery = query.lowercase()
        val tags = mutableListOf<String>()
        var channel: String? = null

        // Channel pattern matching
        if (lowercaseQuery.contains("电商") || lowercaseQuery.contains("买") || lowercaseQuery.contains("购物") || lowercaseQuery.contains("商品") || lowercaseQuery.contains("鞋") || lowercaseQuery.contains("衣服") || lowercaseQuery.contains("包邮")) {
            channel = "电商"
        } else if (lowercaseQuery.contains("本地") || lowercaseQuery.contains("周边") || lowercaseQuery.contains("店") || lowercaseQuery.contains("打卡") || lowercaseQuery.contains("美食") || lowercaseQuery.contains("吃") || lowercaseQuery.contains("玩") || lowercaseQuery.contains("门票") || lowercaseQuery.contains("景区") || lowercaseQuery.contains("生活服务")) {
            channel = "本地"
        } else if (lowercaseQuery.contains("推荐") || lowercaseQuery.contains("精选") || lowercaseQuery.contains("热门")) {
            channel = "精选"
        }

        // Tag pattern matching
        if (lowercaseQuery.contains("运动") || lowercaseQuery.contains("跑") || lowercaseQuery.contains("鞋") || lowercaseQuery.contains("健身")) {
            tags.add("运动")
        }
        if (lowercaseQuery.contains("学生") || lowercaseQuery.contains("校园") || lowercaseQuery.contains("党") || lowercaseQuery.contains("年轻") || lowercaseQuery.contains("平价")) {
            tags.add("学生党")
        }
        if (lowercaseQuery.contains("性价比") || lowercaseQuery.contains("省钱") || lowercaseQuery.contains("便宜") || lowercaseQuery.contains("打折") || lowercaseQuery.contains("平价") || lowercaseQuery.contains("折扣")) {
            tags.add("性价比")
        }
        if (lowercaseQuery.contains("本地") || lowercaseQuery.contains("同城") || lowercaseQuery.contains("周边") || lowercaseQuery.contains("日常")) {
            tags.add("本地生活")
        }
        if (lowercaseQuery.contains("通勤") || lowercaseQuery.contains("办公") || lowercaseQuery.contains("日常") || lowercaseQuery.contains("上班")) {
            tags.add("通勤")
        }
        if (lowercaseQuery.contains("家居") || lowercaseQuery.contains("家具") || lowercaseQuery.contains("沙发") || lowercaseQuery.contains("装修") || lowercaseQuery.contains("实用")) {
            tags.add("家居")
        }
        if (lowercaseQuery.contains("吃") || lowercaseQuery.contains("喝") || lowercaseQuery.contains("美食") || lowercaseQuery.contains("餐饮") || lowercaseQuery.contains("味道") || lowercaseQuery.contains("火锅")) {
            tags.add("美味")
        }
        if (lowercaseQuery.contains("科技") || lowercaseQuery.contains("数码") || lowercaseQuery.contains("手机") || lowercaseQuery.contains("推荐") || lowercaseQuery.contains("电子")) {
            tags.add("数码推荐")
        }

        // Generate nice explanatory text based on selected tags and channel
        val channelText = if (channel != null) "${channel}频道中" else "信息流中"
        val tagsText = if (tags.isNotEmpty()) "【${tags.joinToString("/")}】相关的" else "相匹配的优质"
        val explanation = if (tags.isNotEmpty() || channel != null) {
            "本地AI助手已检索到${channelText}与${tagsText}广告宣传内容，助您快捷理解卖点。"
        } else {
            "本地AI助手已理解您的“${query}”兴趣偏好，已为您检索、聚合了相关商业信息流。"
        }

        return SearchIntent(channel, tags, explanation)
    }
}
