package com.wutiancheng.videoapp.page.publish

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.ActivityLayoutPreviewBinding
import com.wutiancheng.videoapp.exoplayer.PageListPlayer
import com.wutiancheng.videoapp.exoplayer.WrapperPlayerView
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.ext.setImageUrl
import com.wutiancheng.videoapp.ext.setVisibility

class PreviewActivity : AppCompatActivity() {
    private val viewBinding: ActivityLayoutPreviewBinding by invokeViewBinding()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        requestPermission()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PermissionChecker.PERMISSION_GRANTED)
        ) {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQ_PREVIEW)
        } else {
            onGrantPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PREVIEW) {
            if (grantResults.isNotEmpty() && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                onGrantPermission()
            } else {
                // 判断用户是否永久关闭权限
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                if (showRationale) {
                    showNoAccess()
                } else {
                    goToSetting()
                }
            }
        }
    }

    private fun goToSetting() {
        // 打开设置界面
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            this.addCategory(Intent.CATEGORY_DEFAULT)
            this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also {
            startActivity(it)
        }
    }

    private fun showNoAccess() {
        // 再次询问用户是否开启权限
        AlertDialog.Builder(this).setTitle(R.string.preview_permission_message).setPositiveButton(
            R.string.capture_permission_no
        ) { _, _ ->
            finish()
        }.setNegativeButton(R.string.capture_permission_ok) { _, _ ->
            requestPermission()
        }.create().show()
    }

    private fun onGrantPermission() {
        val previewUrl: String = intent.getStringExtra(KEY_PREVIEW_URL) ?: return finish()
        val isVideo: Boolean = intent.getBooleanExtra(KEY_PREVIEW_VIDEO, false)
        val btnText: String? = intent.getStringExtra(KEY_PREVIEW_BTN_TEXT)
        // 设置完成按钮的点击事件
        if (TextUtils.isEmpty(btnText)) {
            viewBinding.actionOk.setVisibility(false)
        } else {
            viewBinding.actionOk.setVisibility(true)
            viewBinding.actionOk.text = btnText
            viewBinding.actionOk.setOnClickListener {
                setResult(Activity.RESULT_OK, Intent())
                finish()
            }
        }
        // 设置关闭按钮的点击事件
        viewBinding.actionClose.setOnClickListener {
            finish()
        }
        if (isVideo) {
            previewVideo(previewUrl)
        } else {
            previewImage(previewUrl)
        }
    }

    private fun previewImage(previewUrl: String) {
        viewBinding.photoView.setVisibility(true)
        viewBinding.photoView.setImageUrl(previewUrl)
    }

    private fun previewVideo(previewUrl: String) {
        val player = PageListPlayer.get(PAGE_NAME)
        viewBinding.playerView.setVisibility(true)
        viewBinding.playerView.setListener(object : WrapperPlayerView.Listener {
            override fun onTogglePlay(attachView: WrapperPlayerView) {
                player.togglePlay(attachView, previewUrl)
            }
        })
        player.togglePlay(viewBinding.playerView, previewUrl)
    }

    override fun onPause() {
        super.onPause()
        PageListPlayer.get(PAGE_NAME).inActive()
    }

    override fun onResume() {
        super.onResume()
        PageListPlayer.get(PAGE_NAME).onActive()
    }

    override fun onDestroy() {
        super.onDestroy()
        PageListPlayer.stop(PAGE_NAME)
    }

    companion object {
        private const val PAGE_NAME = "Preview"
        private const val KEY_PREVIEW_URL = "preview_url"
        private const val KEY_PREVIEW_VIDEO = "preview_video"
        private const val KEY_PREVIEW_BTN_TEXT = "preview_btn_text"
        const val REQ_PREVIEW = 1000

        fun startActivityForResult(
            activity: Activity,
            previewUrl: String,
            isVideo: Boolean,
            btnText: String?
        ) {
            val intent=Intent(activity,PreviewActivity::class.java)
            intent.putExtra(KEY_PREVIEW_URL,previewUrl)
            intent.putExtra(KEY_PREVIEW_VIDEO,isVideo)
            intent.putExtra(KEY_PREVIEW_BTN_TEXT,btnText)
            activity.startActivityForResult(intent, REQ_PREVIEW)
            // 设置activity切换动画，这里的0表示没有动画
            activity.overridePendingTransition(0,0)
        }
    }
}