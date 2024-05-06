package com.wutiancheng.videoapp.page.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.wutiancheng.videoapp.ext.AbsPagingViewModel
import com.wutiancheng.videoapp.http.ApiResult
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.page.login.UserManager

class HomeViewModel : AbsPagingViewModel<Feed>() {

    private var feedType: String = "all"

    fun setFeedType(feedType: String) {
        this.feedType = feedType
    }

    override suspend fun doLoadPage(params: PagingSource.LoadParams<Long>): ApiResult<List<Feed>> {
        val apiResult=ApiService.iApiInterface.getFeeds(params.key?:0,feedType,10,UserManager.userId())
        apiResult.nextPageKey=apiResult.body?.lastOrNull()?.id
        return apiResult
    }
}