package com.wutiancheng.videoapp.exoplayer

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import java.util.regex.Pattern

/**
 * @author wwwu
 * @date 2024/03/31
 * @constructor 创建[PagePlayerDetector]
 * @param [pageName] 绑定列表的名字
 * @param [lifecycleOwner] 用于感知生命周期，控制视频播放
 * @param [recyclerView] 检查列表滑动后，item是否可以播放
 * TODO("一个页面绑定一个唯一的PagePlayerDetector")
 */
class PagePlayerDetector(
    private val pageName: String,
    private val lifecycleOwner: LifecycleOwner,
    private val recyclerView: RecyclerView?
) {
    // 每个recyclerView的item的viewHolder都是IPlayDetector
    private val mDetectListeners: MutableList<IPlayDetector> = arrayListOf()

    // 拿到PagePlayerDetector绑定的页面的pageListPlayer
    private val pageListPlayer = PageListPlayer.get(pageName)

    // 创建滚动监听器
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            // 当列表停止滑动时检测自动播放
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                autoPlay()
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dx == 0 && dy == 0) {
                // 说明列表初始数据加载成功，当调用RecycleView.notifyItemRangeInsert之后，item并不会立马添加到列表之中
                // 因为notifyItemRangeInsert是异步任务，所以使用post提交自动播放检测
                // 当itemView被真正添加到recycleView之后，会触发onScrolled
                postAutoPlay()
            } else {
                // 滑动中需要检测正在播放的item是否已经滑出屏幕，如果滑出则停止它
                if (pageListPlayer.isPlaying && !isTargetInBounds(pageListPlayer.attachView)) {
                    pageListPlayer.inActive()
                }
            }
        }
    }

    init {

        recyclerView?.addOnScrollListener(scrollListener)
        // 根据PagePlayerDetector所属的页面的生命周期状态执行对应的操作
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> pageListPlayer.inActive()
                    // 如果是Lifecycle.Event.ON_RESUME->pageListPlayer
                    // 则会在从详情页切换回帖子页面时，attachView还是详情页的wrapperPlayerView，因此要调用autoPlay()解决
                    Lifecycle.Event.ON_RESUME -> autoPlay()
                    Lifecycle.Event.ON_DESTROY -> {
                        mDetectListeners.clear()
                        recyclerView?.removeOnScrollListener(scrollListener)
                        recyclerView?.removeCallbacks(delayAutoPlayRunnable)
                        pageListPlayer.stop(false)
                    }

                    else -> {}
                }
            }
        })
    }

    fun addDetector(playerDetector: IPlayDetector) {
        mDetectListeners.add(playerDetector)
    }

    fun removeDetector(playerDetector: IPlayDetector) {
        mDetectListeners.remove(playerDetector)
    }


    private val delayAutoPlayRunnable = Runnable { autoPlay() }
    private fun postAutoPlay() {
        recyclerView?.post(delayAutoPlayRunnable)
    }


    // 记录recycleView的bottom和top的高度
    private var rvLocation: Pair<Int, Int>? = null
    private fun ensureRecycleViewLocation() {
        if (recyclerView == null) {
            return
        }
        val point = IntArray(2)
        recyclerView.getLocationOnScreen(point)
        rvLocation = Pair(point[1], point[1] + recyclerView.height)
    }

    // 检测ItemView的视频播放容器的viewGroup是否有至于1/2的高度在屏幕可视区域内
    private fun isTargetInBounds(attachView: ViewGroup?): Boolean {
        if (attachView == null) {
            return false
        }

        if (!attachView.isShown || !attachView.isAttachedToWindow) {
            return false
        }

        // 计算item的中心点
        val location = IntArray(2)
        attachView.getLocationOnScreen(location)
        val center = location[1] + attachView.height / 2

        // 计算recycleView的top和bottom高度
        ensureRecycleViewLocation()

        return rvLocation?.run {
            center in first..second
        } ?: false
    }

    private fun autoPlay() {
        // 如果列表为空或还没在列表中显示，则不自动播放
        if (mDetectListeners.size <= 0 || recyclerView?.childCount!! <= 0) {
            return
        }

        // 是否有正在播放的item，并且还在屏幕内
        if (pageListPlayer.isPlaying && isTargetInBounds(pageListPlayer.attachView)) {
            return
        }

        // 遍历现在屏幕中的item，将第一个符合条件的item开启自动播放
        var attachedViewListener: IPlayDetector? = null
        for (listener in mDetectListeners) {
            val inBounds = isTargetInBounds(listener.getAttachView())
            if (inBounds) {
                attachedViewListener = listener
                break
            }
        }
        attachedViewListener?.run {
            togglePlay(this.getAttachView(), this.getVideoUrl())
        }
    }

    fun togglePlay(attachView: WrapperPlayerView, videoUrl: String) {
        pageListPlayer.togglePlay(attachView, videoUrl)
    }

    interface IPlayDetector {
        /**
         * @return [WrapperPlayerView] 要检查item是否划出/划入屏幕的1/2，需要WrapperPlayerView
         */
        fun getAttachView(): WrapperPlayerView

        /**
         * @return [String] 如果划入屏幕，则需要视频的url进行播放
         */
        fun getVideoUrl(): String
    }
}