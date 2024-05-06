package com.wutiancheng.videoapp.page.detail

import androidx.paging.PagingSource
import com.wutiancheng.videoapp.ext.AbsPagingViewModel
import com.wutiancheng.videoapp.http.ApiResult
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.TopComment
import com.wutiancheng.videoapp.page.login.UserManager
import retrofit2.http.Field

class FeedCommentViewModel : AbsPagingViewModel<TopComment>() {
    private var itemId: Long = 0

    fun setItemId(itemId: Long) {
        this.itemId = itemId
    }

    override suspend fun doLoadPage(params: PagingSource.LoadParams<Long>): ApiResult<List<TopComment>> {
        val apiResult = ApiService.iApiInterface.getFeedCommentList(
            UserManager.userId(),
            itemId,
            params.key ?: 0
        )
        apiResult.nextPageKey = apiResult.body?.lastOrNull()?.commentId
        return apiResult
    }

    suspend fun publishComment(
        commentText: String,
        videoUrl: String? = null,
        imageUrl: String? = null,
        width: Int = 0,
        height: Int = 0
    ): TopComment? {
        return kotlin.runCatching {
            ApiService.iApiInterface.addComment(
                UserManager.userId(), itemId, commentText, videoUrl, imageUrl, width, height
            )
        }.onFailure{
            it.printStackTrace()
        }.getOrNull()?.body
    }
}