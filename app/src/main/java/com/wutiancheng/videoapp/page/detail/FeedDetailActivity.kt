package com.wutiancheng.videoapp.page.detail

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.TYPE_VIDEO

class FeedDetailActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feed: Feed? = intent.getParcelableExtra(KEY_FEED, Feed::class.java)
        if (feed == null) {
            finish()
            return
        }
        val viewHandler = if (feed.itemType == TYPE_VIDEO) {
            VideoViewHandler(this)
        } else {
            ImageViewHandler(this)
        }
        viewHandler.bindInitData(feed)
        setContentView(viewHandler.getRootView())
    }

    companion object {
        private const val KEY_FEED = "key_feed"
        const val KEY_CATEGORY = "key_category"

        fun startFeedDetailActivity(
            context: Activity,
            item: Feed,
            category: String,
            shareView: View?
        ) {
            val intent = Intent(context, FeedDetailActivity::class.java)
            intent.putExtra(KEY_FEED, item)
            intent.putExtra(KEY_CATEGORY, category)
            if(shareView!=null){
                val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    context, shareView, "share_View"
                )
                ActivityCompat.startActivity(context, intent, optionsCompat.toBundle())
            }else{
                context.startActivity(intent)
            }
        }
    }
}