package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.AdEntity

object ShareHelper {
    fun shareAd(context: Context, ad: AdEntity) {
        val shareText = "🔥【AI 核心智能推荐】${ad.advertiserName} —《${ad.title}》🔥\n" +
                "🌟 极简卖点：${ad.summary}\n" +
                "🏷️ 受控关联词：#${ad.tags.replace(",", " #")}\n" +
                "🔗 推荐直达详情页：https://tictokads.com/ad/${ad.id}"

        try {
            // 1. Copy info to system Clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Tictok Ad Link", shareText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "【链接与核心卖点已复制】", Toast.LENGTH_SHORT).show()

            // 2. Launch system standard share sheet dialog chooser
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val chooserIntent = Intent.createChooser(sendIntent, "选择分享平台")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "分享异常: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
