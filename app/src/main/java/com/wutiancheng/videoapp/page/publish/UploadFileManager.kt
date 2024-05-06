package com.wutiancheng.videoapp.page.publish

import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.exoplayer2.util.MimeTypes
import com.wutiancheng.videoapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

object UploadFileManager {
    suspend fun upload(
        context: Context,
        originalFilePath: String,
        mimeType: String,
        callback: (String?, String?) -> Unit
    ) {
        val workRequests = mutableListOf<OneTimeWorkRequest>()
        if (MimeTypes.isVideo(mimeType)) {
            // 提取视频封面图
            val coverFilePath = FileUtil.generateVideoCoverFile(originalFilePath)
            if (TextUtils.isEmpty(coverFilePath)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "生成视频封面图出错！",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@withContext
                }
                callback(null, null)
                return
            }
            val uploadCoverFileWorkRequest = getOneTimeWorkRequest(coverFilePath!!)
            workRequests.add(uploadCoverFileWorkRequest)
        }
        val uploadOriginFileWorkRequest = getOneTimeWorkRequest(originalFilePath)
        workRequests.add(uploadOriginFileWorkRequest)

        // 添加文件上传任务到workManager队列
        enqueue(context, workRequests, callback)
    }

    private suspend fun enqueue(
        context: Context,
        workRequests: MutableList<OneTimeWorkRequest>,
        callback: (String?, String?) -> Unit
    ) {
        var coverFileUploadUrl:String?=null
        var originalFileUploadUrl:String?=null

        val workContinuation = WorkManager.getInstance(context).beginWith(workRequests)
        workContinuation.enqueue()
        workContinuation.workInfosLiveData.asFlow().collectLatest {
            var failCount = 0
            var completeCount = 0
            for (workInfo in it) {
                val state = workInfo.state
                val outputData = workInfo.outputData
                val uuid = workInfo.id

                if (state == WorkInfo.State.FAILED) {
                    val coverFileUploadFail =
                        workRequests.size == 2 && uuid == workRequests.first().id
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            if (coverFileUploadFail) R.string.file_upload_cover_fail else R.string.file_upload_original_fail,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    failCount++
                } else if (state == WorkInfo.State.SUCCEEDED) {
                    val coverFileUploadSuccess=workRequests.size==2&&uuid==workRequests.first().id
                    val uploadUrl=outputData.getString("fileUrl")
                    if(coverFileUploadSuccess){
                        coverFileUploadUrl=uploadUrl
                    }else{
                        originalFileUploadUrl=uploadUrl
                    }
                    completeCount++
                }
                if(completeCount+failCount>=workRequests.size){
                    callback(coverFileUploadUrl,originalFileUploadUrl)
                }
            }

        }
    }


    private fun getOneTimeWorkRequest(uploadFilePath: String): OneTimeWorkRequest {
        val workData = Data.Builder()
            .putString("file", uploadFilePath)
            .build()

        return OneTimeWorkRequest.Builder(UploadFileWork::class.java)
            .setInputData(workData)
            .build()
    }
}