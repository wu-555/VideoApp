package com.wutiancheng.videoapp.page.detail

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutFeedAuthorBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedDetailBottomInteractionBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedLabelBinding
import com.wutiancheng.videoapp.databinding.LayoutFeedTextBinding
import com.wutiancheng.videoapp.ext.IViewBinding
import com.wutiancheng.videoapp.ext.invokeViewModel
import com.wutiancheng.videoapp.ext.setImageUrl
import com.wutiancheng.videoapp.ext.setMaterialButton
import com.wutiancheng.videoapp.ext.setTextVisibility
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.ext.toggleFeedFavorite
import com.wutiancheng.videoapp.ext.toggleFeedLike
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.list.FooterLoadStateAdapter
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.TopComment
import com.wutiancheng.videoapp.page.login.UserManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class ViewHandler(val context: FragmentActivity) : IViewBinding,ViewModelStoreOwner {
    private lateinit var feedCommentListAdapter: FeedCommentListAdapter
    private lateinit var feed: Feed
    override val viewModelStore: ViewModelStore
        get() = context.viewModelStore

    private val feedCommentViewModel: FeedCommentViewModel by invokeViewModel()

    protected lateinit var listView:RecyclerView

    open lateinit var bottomInteractionBinding: LayoutFeedDetailBottomInteractionBinding

    override fun getLayoutInflater() = context.layoutInflater

    override fun getViewLifecycleOwner() = context

    abstract fun getRootView(): View

    open fun bindInitData(feed: Feed) {
        if(!::feed.isInitialized){
            this.feed=feed
        }
        bindFeedComments()
        bindBottomInteraction()
        bindLifecycle()
    }

    private fun bindLifecycle(){
        context.lifecycle.addObserver(object :LifecycleEventObserver{
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when(event){
                    Lifecycle.Event.ON_RESUME->onResume()
                    Lifecycle.Event.ON_PAUSE->onPause()
                    Lifecycle.Event.ON_STOP->onStop()
                    Lifecycle.Event.ON_DESTROY->onDestory()
                    else->return
                }
            }
        })
    }

    open fun onDestory() {

    }

    open fun onStop() {

    }

    open fun onPause() {

    }

    open fun onResume() {

    }

    private fun bindFeedComments(){
        feedCommentViewModel.setItemId(feed.itemId)
        listView.layoutManager=LinearLayoutManager(context)
        listView.itemAnimator=null

        if(!::feedCommentListAdapter.isInitialized){
            feedCommentListAdapter=FeedCommentListAdapter(getViewLifecycleOwner(),context)
        }
        val loadStateAdapter=LoadStateAdapter()
        val concatAdapter=feedCommentListAdapter.withLoadStateHeaderAndFooter(
            loadStateAdapter,
            FooterLoadStateAdapter()
        )

        getHeaderView()?.let {
            concatAdapter.addAdapter(0,object :androidx.paging.LoadStateAdapter<RecyclerView.ViewHolder>(){
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, loadState: LoadState) {

                }

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    loadState: LoadState
                ): RecyclerView.ViewHolder {
                    return object :RecyclerView.ViewHolder(it){}
                }

                override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
                    return true
                }
            })
        }

        listView.adapter=concatAdapter
        context.lifecycleScope.launch{
            feedCommentListAdapter.addLoadStateListener {
                if(feedCommentListAdapter.itemCount>0){
                    concatAdapter.removeAdapter(loadStateAdapter)
                }
            }
            feedCommentViewModel.pageFlow.collect{
                feedCommentListAdapter.submitPagingData(it)
            }
        }
    }

    abstract fun getHeaderView():View?

    fun bindBottomInteraction(){
        val commentDialog=CommentDialog.newInstance(feed.itemId)
        commentDialog.setCommentAddListener(object :CommentDialog.ICommentLister{
            override fun onAddComment(comment: TopComment) {
                // 将新添加的评论添加到列表头，且列表滚动到头位置
                feedCommentListAdapter.insertHeadItem(comment)
                listView.post {
                    listView.scrollToPosition(0)
                }
            }
        })
        bottomInteractionBinding.inputView.setOnClickListener {
            context.lifecycleScope.launchWhenStarted {
                UserManager.loginIfNeed()
                UserManager.getUser().collectLatest {
                    if(it.userId<=0) return@collectLatest
                    commentDialog.show(context.supportFragmentManager,"comment_dialog")
                }
            }
        }

        bottomInteractionBinding.interactionLike.setMaterialButton(
            "",
            feed.getUgcOrDefault().hasLiked,
            R.drawable.icon_cell_liked,
            R.drawable.icon_cell_like
        )

        bottomInteractionBinding.interactionFavorite.setMaterialButton(
            "",
            feed.getUgcOrDefault().hasFavorite,
            R.drawable.icon_collected,
            R.drawable.icon_collect
        )

        bottomInteractionBinding.interactionLike.setOnClickListener {
            context.lifecycleScope.launch{
                feed.toggleFeedLike(true){
                    feed.getUgcOrDefault().hasLiked=it.hasLiked
                    bindBottomInteraction()
                }
            }
        }

        bottomInteractionBinding.interactionFavorite.setOnClickListener {
            context.lifecycleScope.launch{
                feed.toggleFeedFavorite {
                    feed.getUgcOrDefault().hasFavorite=it.hasFavorite
                    bindBottomInteraction()
                }
            }
        }
    }

    fun bindAuthorInfo(
        feedAuthorBinding: LayoutFeedAuthorBinding,
        feedTextBinding: LayoutFeedTextBinding,
        feedLabelsBinding: LayoutFeedLabelBinding
    ) {
        feedAuthorBinding.authorAvatar.setImageUrl(feed.author?.avatar,true)
        feedAuthorBinding.authorName.setTextVisibility(feed.author?.name)
        feedTextBinding.feedText.setTextVisibility(feed.feedsText)
        if(feed.activityText!=null&&feed.activityText!="0"){
            feedLabelsBinding.root.setTextVisibility(feed.activityText)
        }else{
            feedLabelsBinding.root.setVisibility(false)
        }
    }

//    override fun getViewModelStore(): ViewModelStore {
//        return context.viewModelStore
//    }

}