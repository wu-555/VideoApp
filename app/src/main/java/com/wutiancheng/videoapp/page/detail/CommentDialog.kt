package com.wutiancheng.videoapp.page.detail

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.exoplayer2.util.MimeTypes
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutFeedDetailCommentDialogBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.ext.invokeViewModel
import com.wutiancheng.videoapp.ext.setImageUrl
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.TopComment
import com.wutiancheng.videoapp.page.login.UserManager
import com.wutiancheng.videoapp.page.publish.CaptureActivity
import com.wutiancheng.videoapp.page.publish.PreviewActivity
import com.wutiancheng.videoapp.page.publish.UploadFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentDialog : AppCompatDialogFragment() {
    private var itemId: Long = 0
    private var mListener: ICommentLister? = null
    private val viewBinding: LayoutFeedDetailCommentDialogBinding by invokeViewBinding()
    private val viewModel: FeedCommentViewModel by invokeViewModel()
    private var height: Int = 0
    private var width: Int = 0
    private var filePath: String? = null
    private var mimeType: String? = null
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("PublishActivity", "success")
                width = result.data?.getIntExtra(CaptureActivity.RESULT_FILE_WIDTH, 0) ?: 0
                height = result.data?.getIntExtra(CaptureActivity.RESULT_FILE_HEIGHT, 0) ?: 0
                filePath = result.data?.getStringExtra(CaptureActivity.RESULT_FILE_PATH)
                mimeType = result.data?.getStringExtra(CaptureActivity.RESULT_FILE_TYPE)
                // 拍摄完成后启动缩略图
                if (!TextUtils.isEmpty(filePath)) {
                    viewBinding.commentExtLayout.setVisibility(true)
                    viewBinding.commentCover.setImageUrl(filePath)
                    viewBinding.commentIconVideo.setVisibility(MimeTypes.isVideo(mimeType))
                    viewBinding.commentVideo.isEnabled = false
                    viewBinding.commentVideo.imageAlpha = 80
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // 拿到dialog的window属性
        val window = dialog.window ?: return dialog
        // 设置window的背景和decorView的内间距
        // Color.TRANSPARENT为透明
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.setPadding(0, 0, 0, 0)
        // 设置window的软键盘输入模式
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        // 设置window的布局属性
        val attributes = window.attributes
        attributes.gravity = Gravity.BOTTOM
        attributes.horizontalMargin = 0f
        attributes.width = WindowManager.LayoutParams.MATCH_PARENT
        attributes.height = WindowManager.LayoutParams.WRAP_CONTENT
        window.attributes = attributes
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return viewBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.itemId = arguments?.getLong(KEY_ITEM_ID) ?: 0
        if (itemId <= 0) {
            dismiss()
            return
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setItemId(itemId)

        viewBinding.commentVideo.setOnClickListener {
            val intent = Intent(context, CaptureActivity::class.java)
            activityResultLauncher.launch(intent)
        }

        viewBinding.commentSend.setOnClickListener {
            publish()
        }

        viewBinding.commentDelete.setOnClickListener {
            filePath = null
            mimeType = null
            width = 0
            height = 0
            viewBinding.commentCover.setImageDrawable(null)
            viewBinding.commentExtLayout.setVisibility(false)
            viewBinding.commentVideo.isEnabled = true
            viewBinding.commentVideo.imageAlpha = 255
        }

        // 当点击评论后拉起软键盘
        view.post {
            // 让EditText获取焦点
            viewBinding.inputView.isFocusable = true
            viewBinding.inputView.isFocusableInTouchMode = true
            viewBinding.inputView.requestFocus()

            val manager =
                this.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showSoftInput(viewBinding.inputView, 0)
        }
    }

    private fun publish() {
        if (TextUtils.isEmpty(viewBinding.inputView.editableText.toString())) return
        setPublishUI(true)
        lifecycleScope.launch {
            if (!TextUtils.isEmpty(filePath)) {
                UploadFileManager.upload(
                    requireContext(), filePath!!, mimeType!!
                ) { coverFileUploadUrl, originalFileUploadUrl ->
                    if (TextUtils.isEmpty(originalFileUploadUrl) ||
                        (MimeTypes.isVideo(mimeType) && TextUtils.isEmpty(coverFileUploadUrl))
                    ) {
                        setPublishUI(false)
                        return@upload
                    }
                    lifecycleScope.launch {
                        publishComment(coverFileUploadUrl, originalFileUploadUrl)
                    }
                }
            } else {
                publishComment()
            }
        }
    }

    private suspend fun publishComment(
        coverFileUploadUrl: String? = null,
        originalFileUploadUrl: String? = null
    ) {
        val topComment = viewModel.publishComment(
            viewBinding.inputView.editableText.toString(),
            originalFileUploadUrl,
            coverFileUploadUrl,
            width,
            height
        )
        if (topComment == null) {
            withContext(Dispatchers.Main) {
                setPublishUI(false)
                Toast.makeText(requireContext(), R.string.comment_publish_fail, Toast.LENGTH_SHORT)
                    .show()
            }
            return
        }
        mListener?.onAddComment(topComment)
        dismiss()
    }

    override fun dismiss() {
        super.dismiss()
        filePath = null
        width = 0
        height = 0
        mimeType = null
    }

    private fun setPublishUI(publishing: Boolean) {
        viewBinding.commentSend.setVisibility(!publishing)
        viewBinding.actionPublish.setVisibility(publishing)
        if (publishing) {
            viewBinding.actionPublish.show()
        } else {
            viewBinding.actionPublish.hide()
        }
    }

    interface ICommentLister {
        fun onAddComment(comment: TopComment)
    }

    fun setCommentAddListener(listener: ICommentLister) {
        mListener = listener
    }

    companion object {
        private const val KEY_ITEM_ID = "key_item_id"

        fun newInstance(itemId: Long): CommentDialog {
            val args = Bundle().apply {
                putLong(KEY_ITEM_ID, itemId)
            }
            val fragment = CommentDialog()
            fragment.arguments = args
            return fragment
        }
    }
}