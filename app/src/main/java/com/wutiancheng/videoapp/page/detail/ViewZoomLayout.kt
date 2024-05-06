package com.wutiancheng.videoapp.page.detail

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.recyclerview.widget.RecyclerView
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.exoplayer.WrapperPlayerView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ViewZoomLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val viewDragHelper=ViewDragHelper.create(this,1.0f,Callback())
    private var playerView: WrapperPlayerView? = null
    private var listView: RecyclerView? = null
    private var minHeight: Int = 0
    private var maxHeight: Int = 0
    private var canDragZoom = false
    private var flingRunnable:Callback.FlingRunnable?=null
    private val overScroller=OverScroller(context)


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (playerView == null) {
            playerView = findViewById(R.id.player_view)
            listView = findViewById(R.id.list_view)
            val videoWidth = playerView!!.videoWidthPx
            val videoHeight = playerView!!.videoHeightPx
            minHeight = playerView!!.measuredHeight
            maxHeight = ((this.measuredWidth * 1.0f / videoWidth) * videoHeight).roundToInt()

            // 如果视频的高大于视频的宽，则不进行视频缩放
            canDragZoom = videoHeight>videoWidth
        }
    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 将触摸事件传递给ViewDragHelper
        if (!canDragZoom) return super.onInterceptTouchEvent(ev)
        // 如果滑动还没有到终点，则暂停动画
        if (overScroller.currY != overScroller.finalY) {
            overScroller.abortAnimation()
            return true
        }
        return viewDragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // 将触摸事件传递给ViewDragHelper
        if (!canDragZoom)return super.onTouchEvent(ev)
        viewDragHelper.processTouchEvent(ev)
        return true
    }


    inner class Callback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (!canDragZoom) return false
            //  当视频的bottom在minHeight和maxHeight之间时，视频可以拖动放大
            return playerView!!.bottom in minHeight..maxHeight
        }

        // 返回视图垂直方向的拖动的最大范围
        override fun getViewVerticalDragRange(child: View): Int {
            if (!canDragZoom) return 0
            return maxHeight - minHeight
        }

        /**
         * 修正view的垂直方向坐标（当view被拖到边界时应，坐标可能需要进行修正）
         * @param [child] 当前需要进行位置约束的子view对象
         * @param [top] 该子view相对于父viewGroup的顶部的偏移量
         * @param [dy] 子view在本次拖动中垂直方向上的位移量。>0表示向下拖动，<0表示向上拖动
         * @return [Int] 子view的垂直方向修正后的位移量
         */
        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            // dy==0 表示没有拖动
            if (dy == 0) return 0
            // 三种情况让子view不跟随拖动：
            // 1.向上拖动且子view的bottom已经达到最低高度，这时视频不能再缩小了
            // 2.向下拖动且子view的bottom已经达到最大高度，这时视频不能再放大了
            // 3.当评论列表可以向上滑时，视频不跟随拖动进行缩放
            if ((dy < 0 && playerView!!.bottom <= minHeight)
                || (dy > 0 && playerView!!.bottom >= maxHeight)
                || (dy > 0 && listView!!.canScrollVertically(-1))
            ) {
                return 0
            }
            var maxConsumed = 0
            if (dy > 0) {
                // 用户向下拖动时
                maxConsumed = if (playerView!!.bottom + dy >= maxHeight) {
                    maxHeight - playerView!!.bottom
                } else {
                    dy
                }
            } else {
                // 用户向上拖动时
                maxConsumed = if (playerView!!.bottom + dy <= minHeight) {
                    minHeight - playerView!!.bottom
                } else {
                    dy
                }
            }
            // 根据计算的滑动距离修改playerView的layoutParams
            // 这里实现layoutParams已经实现了缩放，只是没有惯性
            val layoutParams = playerView!!.layoutParams
            layoutParams.height = layoutParams.height + maxConsumed
            playerView!!.layoutParams = layoutParams
            return maxConsumed
        }

        /**
         * 用户释放拖动的子view时，对子view的最终位置和状态进行处理
         * @param [releasedChild] 当前释放的子view对象
         * @param [xvel] 释放时子view在水平方向上的速度
         * @param [yvel] 释放时子view在垂直方向上的速度。>0表示向下，<0表示向上
         */
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            if ((playerView!!.bottom in minHeight..maxHeight) && yvel != 0f) {
                playerView!!.removeCallbacks(flingRunnable)
                flingRunnable=FlingRunnable()
                flingRunnable?.fling(xvel.toInt(),yvel.toInt())
            }
        }

        inner class FlingRunnable : Runnable {
            // 实现惯性
            fun fling(xvel: Int,yvel: Int){
                /**
                 *startX:开始的X值，由于我们不需要在水平方向滑动 所以为0
                 *startY:开始滑动时Y的起始值，那就是flingview的bottom值
                 *xxel:水平方向上的速度，实际上为0的
                 *yxel:垂直方向上的速度。即松手时的速度
                 *minX:水平方向上 滚动回弹的越界最小值，给0即可
                 *maxX:水平方向上 滚动回弹越界的最大值，实际上给O也是一样的
                 *minY:垂直方向上 滚动回弹的越界最小值，给0即可
                 *maxy:垂直方向上，滚动回弹越界的最大值，实际上给0也一样
                 */
                overScroller.fling(
                    0,
                    playerView!!.bottom,
                    xvel,
                    yvel,
                    0,
                    Int.MAX_VALUE,
                    0,
                    Int.MAX_VALUE
                )
                run()
            }

            override fun run() {
                val layoutParams=playerView!!.layoutParams
                // overScroller.computeScrollOffset()检测滑动
                if(overScroller.computeScrollOffset()&&layoutParams.height in minHeight..maxHeight){
                    // 这里实现缩放时的惯性动画
                    val newHeight=max(min(overScroller.currY,maxHeight),minHeight)
                    if(newHeight!=layoutParams.height){
                        layoutParams.height=newHeight
                        playerView!!.layoutParams=layoutParams
                    }
                    // 刷新view
                    ViewCompat.postOnAnimation(playerView!!,this)
                }else{
                    playerView!!.removeCallbacks(this)
                }
            }
        }
    }



}