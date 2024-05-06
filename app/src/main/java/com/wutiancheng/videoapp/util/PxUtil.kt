package com.wutiancheng.videoapp.util

import android.content.Context
import android.util.DisplayMetrics

object PxUtil {
    private val metrics: DisplayMetrics = AppGlobals.getApplication().applicationContext.resources.displayMetrics

    fun dp2px(dpValue: Int) = (metrics.density * dpValue + 0.5f).toInt()

    fun getScreenWidth() = metrics.widthPixels

    fun getScreenHeight() = metrics.heightPixels
}