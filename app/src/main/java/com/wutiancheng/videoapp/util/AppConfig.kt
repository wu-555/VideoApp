package com.wutiancheng.videoapp.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.wutiancheng.videoapp.model.BottomBar
import com.wutiancheng.videoapp.model.Category
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

// 解析底部的BottomBar配置文件

private const val TAG = "AppConfig"

object AppConfig {
    private var sBottomBar: BottomBar? = null
    private var sCategory: Category? = null
    fun getBottomConfig(): BottomBar {
        if (sBottomBar == null) {
            val content = parseFile(AppGlobals.getApplication().applicationContext, "main_tabs_config.json")
            sBottomBar = Gson().fromJson(content, BottomBar::class.java)
        }
        Log.d(TAG, "$sBottomBar")
        return sBottomBar!!
    }

    fun getCategoryConfig(): Category {
        if (sCategory == null) {
            val content = parseFile(AppGlobals.getApplication().applicationContext, "category_tabs_config.json")
            sCategory = Gson().fromJson(content, Category::class.java)
        }
         return sCategory!!
    }

    private fun parseFile(context: Context, fileName: String): String {
        val assets = context.assets
        var `is`: InputStream? = null
        var br: BufferedReader? = null
        val builder = StringBuilder()
        try {
            `is` = assets.open(fileName)
            br = BufferedReader(InputStreamReader(`is`))
            var line: String? = null
            while (br.readLine().also { line = it } != null) {
                builder.append(line)
            }
        } catch (e: IOException) {
            Log.e(TAG, "main_tabs_config.json解析失败", e)
        } finally {
            try {
                `is`?.close()
                br?.close()
            } catch (e: Exception) {
                Log.e(TAG, "文件流关闭失败", e)
            }
        }
        return builder.toString()
    }
}