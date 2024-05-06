package com.wutiancheng.videoapp.page.publish

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.exoplayer2.util.MimeTypes
import com.wutiancheng.videoapp.databinding.ActivityLayoutPublishBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.ext.setImageUrl
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.TagList
import com.wutiancheng.videoapp.page.login.UserManager
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@NavDestination(NavDestination.NavType.Activity, "publish_activity")
class PublishActivity : AppCompatActivity() {
    private val viewBinding: ActivityLayoutPublishBinding by invokeViewBinding()
    private var selectedTagList: TagList? = null
    private var width: Int = 0
    private var height: Int = 0
    private var filePath: String? = null
    private var mimeType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.actionAddTag.setOnClickListener {
            showBottomSheetDialog()
        }

        viewBinding.actionAddFile.setOnClickListener {
            CaptureActivity.startActivityForResult(this)
        }

        viewBinding.actionPublish.setOnClickListener {
            publish()
        }

        viewBinding.actionClose.setOnClickListener {
            finish()
        }
    }

    private fun publish() {
        // 当用户没有输入内容时直接返回
        if (TextUtils.isEmpty(viewBinding.inputView.text)) return

        viewBinding.actionPublish.setVisibility(false)
        viewBinding.actionPublishProgress.setVisibility(true)
        viewBinding.actionPublishProgress.show()

        lifecycleScope.launch {
            if (!TextUtils.isEmpty(filePath)) {
                UploadFileManager.upload(
                    this@PublishActivity, filePath!!, mimeType!!
                ) { coverFileUploadUrl, originalFileUploadUrl ->
                    if (TextUtils.isEmpty(originalFileUploadUrl) ||
                        (MimeTypes.isVideo(mimeType) && TextUtils.isEmpty(coverFileUploadUrl))){
                        recoverUIState()
                        return@upload
                    }
                    publishFeed(coverFileUploadUrl,originalFileUploadUrl)
                }
            } else {
                Log.d("PublishActivity","发布文本帖子")
                publishFeed()
            }
        }
    }


    private fun recoverUIState() {
        viewBinding.actionPublish.setVisibility(true)
        viewBinding.actionPublishProgress.setVisibility(false)
        viewBinding.actionPublishProgress.hide()
    }

    private fun publishFeed(
        coverFileUploadUrl: String? = null, originFileUploadUrl: String? = null
    ) {
        // 上传帖子
        lifecycleScope.launch {
            kotlin.runCatching {
                val apiResult = ApiService.iApiInterface.publishFeed(
                    coverFileUploadUrl,
                    originFileUploadUrl,
                    width,
                    height,
                    selectedTagList?.tagId ?: 0L,
                    selectedTagList?.title ?: "",
                    viewBinding.inputView.text.toString(),
                    UserManager.userId()
                )
                withContext(Dispatchers.Main) {
                    if (apiResult.success) {
                        Toast.makeText(this@PublishActivity, "帖子发布成功", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    } else {
                        Toast.makeText(this@PublishActivity, "帖子发布失败", Toast.LENGTH_SHORT)
                            .show()
                        recoverUIState()
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }

        }
    }

    private fun showBottomSheetDialog() {
        // 用户点击添加标签按钮后，弹出标签选择dialog
        val fragment = TagBottomSheetDialogFragment()
        fragment.setOnTagItemSelectedListener(object :
            TagBottomSheetDialogFragment.OnTagItemSelectedListener {
            override fun onTagItemSelected(tagList: TagList) {
                this@PublishActivity.selectedTagList = tagList
                viewBinding.actionAddTag.text = tagList.title
            }
        })
        fragment.show(supportFragmentManager, "tag_dialog")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CaptureActivity.REQ_CAPTURE && resultCode == RESULT_OK) {
            Log.d("PublishActivity", "success")
            width = data?.getIntExtra(CaptureActivity.RESULT_FILE_WIDTH, 0) ?: 0
            height = data?.getIntExtra(CaptureActivity.RESULT_FILE_HEIGHT, 0) ?: 0
            filePath = data?.getStringExtra(CaptureActivity.RESULT_FILE_PATH)
            mimeType = data?.getStringExtra(CaptureActivity.RESULT_FILE_TYPE)
            // 拍摄完成后启动缩略图
            showFileThumbnail()
        }
    }

    private fun showFileThumbnail() {
        if (TextUtils.isEmpty(filePath)) return
        viewBinding.actionAddFile.setVisibility(false)
        viewBinding.fileContainer.setVisibility(true)
        viewBinding.cover.setImageUrl(filePath)
        val isVideo = MimeTypes.isVideo(mimeType)
        viewBinding.videoIcon.setVisibility(MimeTypes.isVideo(mimeType))
        //  图片或视频被点击后启动预览activity
        viewBinding.cover.setOnClickListener {
            PreviewActivity.startActivityForResult(this, filePath!!, isVideo, null)
        }

        //  删除视频或图片按钮的点击事件绑定
        viewBinding.actionDeleteFile.setOnClickListener {
            viewBinding.actionAddFile.setVisibility(true)
            viewBinding.fileContainer.setVisibility(false)
            viewBinding.cover.setImageDrawable(null)
            filePath = null
            mimeType = null
            width = 0
            height = 0
        }
    }
}