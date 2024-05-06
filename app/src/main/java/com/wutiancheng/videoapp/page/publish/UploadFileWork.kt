package com.wutiancheng.videoapp.page.publish

import android.content.Context
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wutiancheng.videoapp.util.OssUploader

class UploadFileWork(context:Context,workerParams:WorkerParameters): Worker(context,workerParams) {
    override fun doWork(): Result {
        val filePath=inputData.getString("file")
        return if(TextUtils.isEmpty(filePath)){
            Result.failure()
        }else{
            val fileUrl=OssUploader.upload(filePath!!)
            val data=Data.Builder()
                .putString("fileUrl",fileUrl)
                .build()
            Result.success(data)
        }
    }
}