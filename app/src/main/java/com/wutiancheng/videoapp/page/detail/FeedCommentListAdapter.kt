package com.wutiancheng.videoapp.page.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.paging.insertHeaderItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.wutiancheng.videoapp.databinding.LayoutFeedTopCommentBinding
import com.wutiancheng.videoapp.ext.bindTopComment
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.model.TopComment
import com.wutiancheng.videoapp.page.publish.PreviewActivity

class FeedCommentListAdapter(val lifecycleOwner: LifecycleOwner,val context:Context) :
    PagingDataAdapter<TopComment, FeedCommentListAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<TopComment>() {
        override fun areItemsTheSame(oldItem: TopComment, newItem: TopComment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TopComment, newItem: TopComment): Boolean {
            return oldItem == newItem
        }
    }) {

    private var pagingData:PagingData<TopComment> = PagingData.empty()

    inner class ViewHolder(val binding: LayoutFeedTopCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindTopComment(item:TopComment) {
            binding.godComment.setVisibility(false)
            binding.bindTopComment(lifecycleOwner,item){
                notifyItemChanged(bindingAdapterPosition)
            }
            binding.commentPreview.setOnClickListener {
                PreviewActivity.startActivityForResult(context as Activity,if(!TextUtils.isEmpty(item.videoUrl)){
                    item.videoUrl!!
                }else{
                    item.imageUrl!!
                },!TextUtils.isEmpty(item.videoUrl),"")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item:TopComment=getItem(position)!!
        holder.bindTopComment(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding=LayoutFeedTopCommentBinding.inflate(LayoutInflater.from(context),parent,false)
        return ViewHolder(binding)
    }

    fun insertHeadItem(comment: TopComment){
        val newPagingData=pagingData.insertHeaderItem(item=comment)
        submitPagingData(newPagingData)
    }

    fun submitPagingData(newPagingData:PagingData<TopComment>){
        this.pagingData=newPagingData
        submitData(lifecycleOwner.lifecycle,newPagingData)
    }
}