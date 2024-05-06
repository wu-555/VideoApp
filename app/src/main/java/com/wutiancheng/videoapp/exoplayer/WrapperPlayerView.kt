package com.wutiancheng.videoapp.exoplayer

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutListWrapperPlayerViewBinding
import com.wutiancheng.videoapp.ext.setBlurImageUrl
import com.wutiancheng.videoapp.ext.setImageUrl
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.util.PxUtil

// 动态挂载显示视屏画面的playerView和视频播放控制器
class WrapperPlayerView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleResource: Int = 0
) : FrameLayout(context, attr, defStyleAttr, defStyleResource) {
    private var callback: Listener?=null
    private val viewBinding =
        LayoutListWrapperPlayerViewBinding.inflate(LayoutInflater.from(context), this)

    internal var videoWidthPx:Int=0
    internal var videoHeightPx:Int=0

    init {
        viewBinding.playBtn.setOnClickListener {
            callback?.onTogglePlay(this)
        }
    }

    /**
     * TODO 在WrapperPlayerView中设置高斯模糊背景，视频播放/暂停按钮的点击响应事件，playerView的宽和高
     * @param [widthPx] 视频宽度像素
     * @param [heightPx] 视频高度像素
     * @param [coverUrl] 视频封面url
     * @param [videoUrl] 视频url
     * @param [maxHeight] 视频最大高度
     */
    fun bindData(
        widthPx: Int,
        heightPx: Int,
        coverUrl: String?,
        videoUrl: String?,
        maxHeight: Int
    ) {
        // 根据视频的widPx，heightPx 计算出cover blur wrapperView的宽高
        viewBinding.cover.setImageUrl(coverUrl)

        if (widthPx < heightPx) {
            coverUrl?.run {
                viewBinding.blurBackground.setBlurImageUrl(this, 10)
                viewBinding.blurBackground.setVisibility(true)
            }
        } else {
            viewBinding.blurBackground.setVisibility(false)
        }

        videoWidthPx=widthPx
        videoHeightPx=heightPx

        setSize(widthPx, heightPx, PxUtil.getScreenWidth(), maxHeight)
    }


    private fun setSize(widthPx: Int, heightPx: Int, maxWidth: Int, maxHeight: Int) {
        // 计算视频 原始宽度>原始高度 或 原始高度>原始宽度 时的cover、wrapperView的等比缩放
        val coverWidth: Int
        val coverHeight: Int
        if (widthPx >= heightPx) {
            coverWidth = maxWidth
            Log.d("WrapperPlayerView","${widthPx * 1.0f / maxWidth}")
            coverHeight = (heightPx / (widthPx * 1.0f / maxWidth)).toInt()
        } else {
            coverHeight = maxHeight
            coverWidth = (widthPx / (heightPx * 1.0f / maxHeight)).toInt()
        }

        // 设置wrapperView的宽高
        val wrapperViewParams = layoutParams
        wrapperViewParams.width = maxWidth
        wrapperViewParams.height = coverHeight
        layoutParams = wrapperViewParams

        // 设置blurBackgroundView的宽高
        val blurParams = viewBinding.blurBackground.layoutParams
        blurParams.width = maxWidth
        blurParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        viewBinding.blurBackground.layoutParams = blurParams

        // 设置coverView封面图的宽高
        val coverParams = viewBinding.cover.layoutParams as LayoutParams
        coverParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        coverParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        coverParams.gravity = Gravity.CENTER
        viewBinding.cover.scaleType = ImageView.ScaleType.FIT_CENTER
        viewBinding.cover.layoutParams = coverParams
    }

    fun onActive(
        playerView: StyledPlayerView,
        playerControllerView: StyledPlayerControlView
    ) {
        val parent = playerView.parent
        if (parent != this) {
            if (parent != null) {
                // 进入到这里说明playerView被挂载到某个item上面去了
                (parent as ViewGroup).removeView(playerView)
            }
            // 把playerView添加到点击的item上面来
            val coverParams = viewBinding.cover.layoutParams
            // index表示添加在布局中的位置
            this.addView(playerView, 1, coverParams)
        }
        val controllerParent = playerControllerView.parent
        if (controllerParent != this) {
            if (controllerParent != null) {
                (controllerParent as ViewGroup).removeView(playerControllerView)
            }
            val ctrlParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            ctrlParams.gravity=Gravity.BOTTOM
            this.addView(playerControllerView,ctrlParams)
        }
    }

    fun inActive() {
        // 视频暂停播放时显示封面，播放按钮
        viewBinding.cover.setVisibility(true)
        viewBinding.playBtn.setVisibility(true)
        viewBinding.playBtn.setImageResource(R.drawable.icon_video_play)
    }

    fun onPlayerStateChanged(playing: Boolean, playbackState: Int) {
        if(playing){
            // 如果正在播放
            viewBinding.cover.setVisibility(false)
            viewBinding.bufferView.setVisibility(false)
            viewBinding.playBtn.setVisibility(true)
            viewBinding.playBtn.setImageResource(R.drawable.icon_video_pause)
        }else if(playbackState==Player.STATE_ENDED){
            // 如果播放完成了
            viewBinding.cover.setVisibility(true)
            viewBinding.playBtn.setVisibility(true)
            viewBinding.playBtn.setImageResource(R.drawable.icon_video_play)
        }else if(playbackState==Player.STATE_BUFFERING){
            // 如果视频在缓冲加载中
            viewBinding.bufferView.setVisibility(true)
        }
    }

    fun onVisibilityChange(visibility: Int, playEnd: Boolean) {
        // 如果视频播放完了，播放按钮一直显示
        // 否则状态跟随视频播放控制器的显示
        viewBinding.playBtn.setVisibility(if(playEnd) true else visibility== View.VISIBLE)
    }

    fun setListener(callback:Listener){
        this.callback=callback
    }

    interface Listener{
        fun onTogglePlay(attachView:WrapperPlayerView)
    }
}