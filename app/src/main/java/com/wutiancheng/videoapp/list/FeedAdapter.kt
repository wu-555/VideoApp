package com.wutiancheng.videoapp.list

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutFeedAuthorBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedInteractionBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedLabelBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedTextBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedTopCommentBinding
import com.wutiancheng.videoapp.exoplayer.PagePlayerDetector
import com.wutiancheng.videoapp.exoplayer.WrapperPlayerView
import com.wutiancheng.videoapp.ext.bindFeedImage
import com.wutiancheng.videoapp.ext.bindTopComment
import com.wutiancheng.videoapp.ext.load
import com.wutiancheng.videoapp.ext.setImageResource
import com.wutiancheng.videoapp.ext.setImageUrl
import com.wutiancheng.videoapp.ext.setMaterialButton
import com.wutiancheng.videoapp.ext.setTextColor
import com.wutiancheng.videoapp.ext.setTextVisibility
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.ext.toggleFeedLike
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.Author
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.TYPE_IMAGE_TEXT
import com.wutiancheng.videoapp.model.TYPE_TEXT
import com.wutiancheng.videoapp.model.TYPE_VIDEO
import com.wutiancheng.videoapp.model.TopComment
import com.wutiancheng.videoapp.model.Ugc
import com.wutiancheng.videoapp.page.detail.CommentDialog
import com.wutiancheng.videoapp.page.detail.FeedDetailActivity
import com.wutiancheng.videoapp.page.login.UserManager
import com.wutiancheng.videoapp.util.AppGlobals
import com.wutiancheng.videoapp.util.PxUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

// FeedAdapter用于管理页面主体的view
class FeedAdapter(private val fragmentManager: FragmentManager,private val pageName: String, private val lifecycleOwner: LifecycleOwner) :
    PagingDataAdapter<Feed, FeedAdapter.FeedViewHolder>(object : DiffUtil.ItemCallback<Feed>() {
        override fun areItemsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Feed, newItem: Feed): Boolean {
            return oldItem == newItem
        }
    }) {

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        PagePlayerDetector.IPlayDetector {
        // 使用bind就不会创建新的view
        private val feedAuthorBinding =
            LayoutFeedAuthorBinding.bind(itemView.findViewById(R.id.feed_author))
        private val feedTextBinding =
            LayoutFeedTextBinding.bind(itemView.findViewById(R.id.feed_text))
        private val feedLabelsBinding =
            LayoutFeedLabelBinding.bind(itemView.findViewById(R.id.feed_label))
        internal val feedImage: ImageView? = itemView.findViewById(R.id.feed_image)
        private val feedTopCommentBinding =
            LayoutFeedTopCommentBinding.bind(itemView.findViewById(R.id.feed_comment))
        private val feedInteractionBinding =
            LayoutFeedInteractionBinding.bind(itemView.findViewById(R.id.feed_interaction))
        internal val wrapperPlayerView: WrapperPlayerView? = itemView.findViewById(R.id.feed_video)

        fun bindFeedAuthor(author: Author?) {
            author?.let {
                feedAuthorBinding.authorAvatar.setImageUrl(it.avatar, true)
                feedAuthorBinding.authorName.text = it.name
            }
        }

        fun bindFeedText(feedsText: String?) {
            feedTextBinding.feedText.setTextVisibility(feedsText)
        }

        fun bindLabel(activityText: String?) {
            if(activityText==null||activityText=="0"){
                feedLabelsBinding.root.setVisibility(false)
            }else{
                feedLabelsBinding.root.setTextVisibility(activityText)
            }
        }

        fun bindFeedImage(feed: Feed, maxHeight: Int) {
            feedImage?.bindFeedImage(this@FeedAdapter.lifecycleOwner, feed, maxHeight)
        }

        fun bindTopComment(topComment: TopComment?) {
            feedTopCommentBinding.bindTopComment(lifecycleOwner, topComment) {
                // bindingAdapterPosition在RecycleView添加Header Adapter时依然能正确获得item的位置
                notifyItemChanged(bindingAdapterPosition, it)
            }
        }

        fun bindInteraction(ugc: Ugc?, itemId: Long,callback:(Context)->Unit) {
            ugc?.run {
                val context = itemView.context
                if (likeCount > 0) {
                    feedInteractionBinding.interactionLike.text = likeCount.toString()
                }

                feedInteractionBinding.interactionLike.setMaterialButton(
                    likeCount.toString(), hasLiked,
                    R.drawable.icon_cell_liked,
                    R.drawable.icon_cell_like
                )
                feedInteractionBinding.interactionDiss.setMaterialButton(
                    null, hasdiss,
                    R.drawable.icon_cell_dissed,
                    R.drawable.icon_cell_diss
                )


                if (commentCount > 0) {
                    feedInteractionBinding.interactionComment.text = commentCount.toString()
                }
                if (shareCount > 0) {
                    feedInteractionBinding.interactionShare.text = shareCount.toString()
                }
            }

            feedInteractionBinding.interactionLike.setOnClickListener {
                toggleFeedLike(itemId, true)
            }

            feedInteractionBinding.interactionDiss.setOnClickListener {
                toggleFeedLike(itemId, false)
            }

            feedInteractionBinding.interactionShare.setOnClickListener {
                Toast.makeText(it.context,"服务暂未开发",Toast.LENGTH_SHORT).show()
            }

            feedInteractionBinding.interactionComment.setOnClickListener {
                callback(it.context)
            }
        }

        private fun toggleFeedLike(itemId: Long, like: Boolean) {
            lifecycleOwner.lifecycleScope.launch {
                val feed=snapshot().items[bindingAdapterPosition]
                feed.toggleFeedLike(like){
                    feed.getUgcOrDefault().hasLiked=it.hasLiked
                    feed.getUgcOrDefault().hasdiss=it.hasdiss
                    feed.getUgcOrDefault().likeCount=it.likeCount
                    notifyItemChanged(bindingAdapterPosition,feed.getUgcOrDefault())
                }
            }
        }

        // 绑定视频数据
        fun bindFeedVideo(width: Int, height: Int, maxHeight: Int, cover: String?, url: String?) {
            url?.run {
                wrapperPlayerView?.run {
                    setVisibility(true)
                    bindData(width, height, cover, url, maxHeight)
                    setListener(object : WrapperPlayerView.Listener {
                        override fun onTogglePlay(attachView: WrapperPlayerView) {
                            pagePlayerDetector.togglePlay(wrapperPlayerView, url)
                        }
                    })
                }
            }
        }

        override fun getAttachView(): WrapperPlayerView {
            return wrapperPlayerView!!
        }

        override fun getVideoUrl(): String {
            return getItem(layoutPosition)?.url ?: ""
        }

        fun isVideo(): Boolean {
            return getItem(layoutPosition)?.itemType == TYPE_VIDEO
        }
    }

    private lateinit var pagePlayerDetector: PagePlayerDetector

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return 0
        return item.itemType
    }

    override fun onBindViewHolder(
        holder: FeedViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        // 如果payloads为空，则全量式更新数据；如果payloads不为空，则增量式更新数据
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else if (payloads[0] is Ugc) {
            holder.bindInteraction(payloads[0] as Ugc, getItem(position)!!.itemId){
                FeedDetailActivity.startFeedDetailActivity(
                    it as Activity,
                    getItem(position)!!,
                    pageName,
                    holder.feedImage?:holder.wrapperPlayerView!!
                )
            }
        } else if (payloads[0] is TopComment) {
            holder.bindTopComment(payloads[0] as TopComment)
        }

    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bindFeedAuthor(item.author)
        holder.bindFeedText(item.feedsText)
        holder.bindLabel(item.activityText)
        if (item.itemType == TYPE_IMAGE_TEXT) {
            // 传入图片的宽度、高度、最大高度、url
            holder.bindFeedImage(item, PxUtil.dp2px(300))
        } else {
            holder.bindFeedVideo(item.width, item.height, PxUtil.dp2px(300), item.cover, item.url)
        }

        holder.bindTopComment(item.topComment)
        holder.bindInteraction(item.ugc, item.itemId){
            FeedDetailActivity.startFeedDetailActivity(
                it as Activity,
                getItem(position)!!,
                pageName,
                holder.feedImage?:holder.wrapperPlayerView!!
            )
        }
        holder.itemView.setOnClickListener {
            FeedDetailActivity.startFeedDetailActivity(
                it.context as Activity,
                item,
                pageName,
                holder.feedImage?:holder.wrapperPlayerView!!
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        if (viewType != TYPE_TEXT && viewType != TYPE_IMAGE_TEXT && viewType != TYPE_VIDEO) {
            val view = View(parent.context)
            view.visibility = View.GONE
            return FeedViewHolder(view)
        }
        val layoutResId = if (viewType == TYPE_IMAGE_TEXT || viewType == TYPE_TEXT) {
            R.layout.layout_feed_type_image
        } else {
            R.layout.layout_feed_type_video
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return FeedViewHolder(view)
    }

    // 当一个新的item显示到屏幕上时，把这个item的viewHolder(也是IPlayerDetector)添加到页面对应的PagePlayerDetector里进行管理
    override fun onViewAttachedToWindow(holder: FeedViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder.isVideo()) {
            pagePlayerDetector.addDetector(holder)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (!::pagePlayerDetector.isInitialized) {
            pagePlayerDetector = PagePlayerDetector(pageName, lifecycleOwner, recyclerView)
        }
    }

    // 当item从屏幕上消失时，将item的viewHolder从PagePlayerDetector中移除
    override fun onViewDetachedFromWindow(holder: FeedViewHolder) {
        super.onViewDetachedFromWindow(holder)
        pagePlayerDetector.removeDetector(holder)
    }
}