package com.wutiancheng.videoapp.ext

import android.text.TextUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutFeedTopCommentBinding
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.TopComment
import com.wutiancheng.videoapp.page.login.UserManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun LayoutFeedTopCommentBinding.bindTopComment(lifecycleOwner:LifecycleOwner,topComment: TopComment?,callback:(TopComment)->Unit){
    root.setVisibility(topComment != null)
    mediaLayout.setVisibility(!TextUtils.isEmpty(topComment?.imageUrl))
    topComment?.run {
        commentAuthor.setTextVisibility(author?.name)
        commentAvatar.setImageUrl(author?.avatar, true)
        this@bindTopComment.commentText.setTextVisibility(commentText)
        commentLikeCount.setTextVisibility(this.getUgcOrDefault().likeCount.toString())
        commentPreviewVideoPlay.setVisibility(!TextUtils.isEmpty(videoUrl))
        commentPreview.setImageUrl(imageUrl)
        commentLikeStatus.setImageResource(
            this.getUgcOrDefault().hasLiked,
            R.drawable.icon_cell_liked,
            R.drawable.icon_cell_like
        )
        commentLikeCount.setTextColor(
            this.getUgcOrDefault().hasLiked,
            R.color.color_theme,
            R.color.color_3d3
        )
        commentLikeStatus.setOnClickListener {
            lifecycleOwner.lifecycleScope.launch{
                UserManager.loginIfNeed()
                UserManager.getUser().collectLatest {
                    if(it.userId<=0) return@collectLatest
                    val response= ApiService.iApiInterface.toggleCommentLike(commentId,itemId,it.userId)
                    response.body?.run {
                        val ugc=topComment.getUgcOrDefault()
                        ugc.hasLiked=this.getAsJsonPrimitive("hasLiked").asBoolean
                        ugc.likeCount=this.getAsJsonPrimitive("likeCount").asInt
                        topComment.commentUgc=ugc
                        callback(topComment)
                    }
                }
            }
        }
    }
}