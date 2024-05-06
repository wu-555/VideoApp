package com.wutiancheng.videoapp.ext

import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.palette.graphics.Palette
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.util.PxUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun ImageView.bindFeedImage(lifecycleOwner:LifecycleOwner,feed: Feed,maxHeight: Int){
    if (TextUtils.isEmpty(feed.cover)) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE

    load(feed.cover!!) {
        // 如果服务器传回来的数据不带有width或height数据，就从url加载
        if (feed.width <= 0 || feed.height <= 0) {
            Log.d("FeedAdapter", "${feed.cover}")
            setFeedImageSize(it.width, it.height, maxHeight)
        }
        // 如果图片填充不满，则设置背景颜色
        lifecycleOwner.lifecycle.coroutineScope.launch(Dispatchers.IO) {
            // feedItem.backgroundColor如果为0，则说明没有生成过背景颜色
            if (feed.backgroundColor == 0) {
                val defaultColor = context.getColor(R.color.black)
                val color =
                    Palette.Builder(it).generate().getMutedColor(defaultColor)
                feed.backgroundColor = color
            }
            // 在这个view的线程上下文设置背景色
            withContext(lifecycleOwner.lifecycle.coroutineScope.coroutineContext) {
                background = ColorDrawable(feed.backgroundColor)
            }
        }

    }

    // 当width>0且height>0时，说明服务器返回的数据中包含了图片的宽度和高度
    if (feed.width > 0 && feed.height > 0) {
        setFeedImageSize(feed.width, feed.height, maxHeight)
    }
}

fun ImageView.setFeedImageSize(width: Int, height: Int, maxHeight: Int) {
    // 图片布局的宽度设置为屏幕的宽度，注意这里不是设置的图片的宽度
    val finalWidth: Int = PxUtil.getScreenWidth()
    // 当图片的宽度小于高度时，让图片居中，空余的部分用图片的主题色填充
    // 当图片的宽度大于高度时，让图片布局的高度自适应
    val finalHeight: Int = if (width > height) {
        // 将高度按照宽度缩放的比例进行缩放
        (height / (width * 1.0f / finalWidth)).toInt()
    } else {
        maxHeight
    }
    // 设置图片布局的参数
    val params = layoutParams as LinearLayout.LayoutParams
    params.width = finalWidth
    params.height = finalHeight
    params.gravity = Gravity.CENTER
    scaleType = ImageView.ScaleType.FIT_CENTER
    layoutParams = params
}