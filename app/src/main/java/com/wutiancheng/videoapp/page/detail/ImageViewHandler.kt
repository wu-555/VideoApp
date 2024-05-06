package com.wutiancheng.videoapp.page.detail

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.wutiancheng.videoapp.databinding.LayoutFeedDetailTypeImageBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedTypeImageBinding
import com.wutiancheng.videoapp.ext.bindFeedImage
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.util.PxUtil

class ImageViewHandler(context: FragmentActivity) : ViewHandler(context) {
    private val viewBinding: LayoutFeedDetailTypeImageBinding by invokeViewBinding()

    init {
        listView=viewBinding.listView
        bottomInteractionBinding=viewBinding.bottomInteraction
    }

    override fun getRootView() = viewBinding.root

    override fun bindInitData(feed: Feed) {
        super.bindInitData(feed)
        viewBinding.actionClose.setOnClickListener {
            context.finish()
        }
        viewBinding.feedImage.bindFeedImage(getViewLifecycleOwner(), feed, PxUtil.dp2px(250))
        bindAuthorInfo(viewBinding.feedAuthor, viewBinding.feedText, viewBinding.feedLabel)
    }

    override fun getHeaderView(): View? {
        return null
    }
}