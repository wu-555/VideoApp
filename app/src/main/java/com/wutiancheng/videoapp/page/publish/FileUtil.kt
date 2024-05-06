package com.wutiancheng.videoapp.page.publish

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.wutiancheng.videoapp.util.AppGlobals
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

internal object FileUtil {
    fun generateVideoCoverFile(filePath: String): String? {
        val retrieve = MediaMetadataRetriever()
        retrieve.setDataSource(filePath)
        // 拿到视频的第一帧，返回的类型是Bitmap
        val bitmap = retrieve.frameAtTime
        if (bitmap != null) {
            // 将图片压缩到200MB内
            val bytes = compressBitmap(bitmap, 200)
            val file=File(AppGlobals.getApplication().cacheDir,System.currentTimeMillis().toString()+".jpeg")
            try {
                file.createNewFile()
                val fos=FileOutputStream(file)
                fos.write(bytes)
                fos.close()
            }catch (e:Exception){
                e.printStackTrace()
                return null
            }
            return file.absolutePath
        }
        return null
    }

    private fun compressBitmap(bitmap: Bitmap, limit: Int): ByteArray? {
        if (limit > 0) {
            val baos = ByteArrayOutputStream()
            baos.use {
                // 压缩的质量
                var options = 100
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos)
                // 如果压缩后的大小依然超过了limit，则减小质量，重新压缩
                // 如果最后依然大于200MB，则返回空
                while (baos.toByteArray().size > limit * 1024 && options > 0) {
                    baos.reset()
                    options -= 5
                    bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos)
                }
                if(options<=0){
                    return null
                }
                return baos.toByteArray()
            }
        }
        return null
    }
}