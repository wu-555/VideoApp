package com.wutiancheng.videoapp.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import java.io.File

private var sApplication: Application? = null

object AppGlobals {
    fun getApplication(): Application {
        if (sApplication == null) {
            kotlin.runCatching {
                sApplication=  Class.forName("android.app.ActivityThread").getMethod("currentApplication")
                    .invoke(null, *emptyArray()) as Application
            }.onFailure {
                it.printStackTrace()
            }
        }
        return sApplication!!
    }
}