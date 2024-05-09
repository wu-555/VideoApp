package com.wutiancheng.videoapp.util

import android.nfc.Tag
import android.util.Log
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.common.OSSLog
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.google.common.primitives.Bytes

object OssUploader {
    private const val TAG = "OssUploader"
    private var oss: OSSClient
    private const val ALIYUN_BUCKET_URL =
        "*****url*****"
    private const val BUCKET_NAME = "*****name*****"
    private const val END_POINT = "http://oss-cn-chengdu.aliyuncs.com"
    private const val ACCESS_ID="*****id*****"
    private const val ACCESS_KEY="*****key*****"

    init {
        //val credentialProvider = OSSAuthCredentialsProvider(AUTH_SERVER_URL)
        val conf = ClientConfiguration()
        // 连接超时设为15s
        conf.connectionTimeout = 15 * 1000
        // socket超时设为15s
        conf.socketTimeout = 15 * 1000
        // 最大同时请求数设为5
        conf.maxConcurrentRequest = 5
        // 最大错误重连次数设为2
        conf.maxErrorRetry = 2

        // 关闭日志
        OSSLog.disableLog()
        oss = OSSClient(
            AppGlobals.getApplication().applicationContext,
            END_POINT,
            OSSPlainTextAKSKCredentialProvider(ACCESS_ID, ACCESS_KEY),
            conf
        )
    }

    fun upload(bytes: ByteArray?): String {
        val objectKey = System.currentTimeMillis().toString()
        val request = PutObjectRequest(BUCKET_NAME, objectKey, bytes)
        return upload(request)
    }

    fun upload(filePath: String): String {
        val objectKey = filePath.substring(
            filePath.lastIndexOf("/") + 1
        )
        val request = PutObjectRequest(BUCKET_NAME, objectKey, filePath)
        return upload(request)
    }

    private fun upload(putRequest: PutObjectRequest): String {
        putRequest.progressCallback =
            OSSProgressCallback<PutObjectRequest> { _, currentSize, totalSize ->
                Log.d(TAG, "upload currentSize: $currentSize totalSize: $totalSize")
            }
        val result = oss.putObject(putRequest)
        return if (result.statusCode == 200) {
            ALIYUN_BUCKET_URL + putRequest.objectKey
        } else {
            Log.d(TAG, result.serverCallbackReturnBody)
            ""
        }
    }
}