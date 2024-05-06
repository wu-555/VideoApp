package com.wutiancheng.videoapp.page.detail

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStore
import com.wutiancheng.videoapp.databinding.LayoutFeedDetailTypeVideoBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedDetailTypeVideoHeaderBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedTypeVideoBinding
import com.wutiancheng.videoapp.exoplayer.IListPlayer
import com.wutiancheng.videoapp.exoplayer.PageListPlayer
import com.wutiancheng.videoapp.exoplayer.WrapperPlayerView
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.util.PxUtil

class VideoViewHandler(context: FragmentActivity) : ViewHandler(context) {
    private val viewBinding: LayoutFeedDetailTypeVideoBinding by invokeViewBinding()
    private val headerViewBinding: LayoutFeedDetailTypeVideoHeaderBinding by invokeViewBinding()
    private lateinit var player: IListPlayer

    init {
        listView = viewBinding.listView
        bottomInteractionBinding = viewBinding.bottomInteraction
        viewBinding.actionClose.setOnClickListener {
            context.finish()
        }
    }

    override fun getRootView() = viewBinding.root

    override fun bindInitData(feed: Feed) {
        super.bindInitData(feed)

        val category = context.intent.getStringExtra(FeedDetailActivity.KEY_CATEGORY)
        player = PageListPlayer.get(category!!)

        viewBinding.playerView.bindData(
            feed.width,
            feed.height,
            feed.cover,
            feed.url!!,
            PxUtil.dp2px(250)
        )

        // 用于用户对视频的播放控制
        viewBinding.playerView.setListener(object : WrapperPlayerView.Listener {
            override fun onTogglePlay(attachView: WrapperPlayerView) {
                player.togglePlay(viewBinding.playerView, feed.url)
            }
        })
        // 先让视频自动播放
        player.togglePlay(viewBinding.playerView, feed.url)
    }

    override fun onResume() {
        super.onResume()
        player.onActive()
    }

    override fun getHeaderView(): View? {
        bindAuthorInfo(
            headerViewBinding.authorInfo,
            headerViewBinding.feedText,
            headerViewBinding.feedLabel
        )
        headerViewBinding.root.layoutParams =
            ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        return headerViewBinding.root
    }

    override fun onPause() {
        super.onPause()
        player.inActive()
    }
}