package com.wutiancheng.videoapp.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.wutiancheng.videoapp.databinding.LayoutLoadingStatusViewBinding

class LoadingStatusView @JvmOverloads constructor(context: Context, attr: AttributeSet?=null, defaultStyleAttr: Int = 0) :
    LinearLayout(context, attr, defaultStyleAttr) {
    private val binding = LayoutLoadingStatusViewBinding.inflate(LayoutInflater.from(context),this,true)

    init {
        binding.loading.show()
    }

    @SuppressLint("ResourceType")
    fun showEmpty(@DrawableRes iconRes: Int=0, text: String="", retryText:String="重试", retry: OnClickListener?=null) {
        binding.loading.hide()
        binding.emptyLayout.visibility = View.VISIBLE
        if (iconRes > 0) {
            binding.emptyIcon.setImageResource(iconRes)
        }
        if (text.isNotEmpty()) {
            binding.emptyText.text = text
            binding.emptyText.visibility = View.VISIBLE
        }
        retry?.run {
            binding.emptyAction.visibility = View.VISIBLE
            binding.emptyAction.text=retryText
            binding.emptyAction.setOnClickListener(this)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility != View.VISIBLE) {
            binding.loading.hide()
        }
    }
}