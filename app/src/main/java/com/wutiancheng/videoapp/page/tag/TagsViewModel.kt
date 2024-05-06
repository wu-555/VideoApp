package com.wutiancheng.videoapp.page.tag

import androidx.lifecycle.ViewModel
import androidx.paging.PagingSource
import com.wutiancheng.videoapp.ext.AbsPagingViewModel
import com.wutiancheng.videoapp.http.ApiResult
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.page.login.UserManager

class TagsViewModel : ViewModel() {
    private var nextPageKey: Long=0L

    suspend fun loadData(refresh:Boolean=true):ApiResult<List<Feed>>{
        if(refresh){
            nextPageKey=0L
        }
        val apiResult=ApiService.iApiInterface.getFeeds(
            feedId = nextPageKey,
            feedType = "all",
            userId = UserManager.userId(),
            pageCount = 10
        )
        nextPageKey=apiResult.body?.lastOrNull()?.id?:0L
        return apiResult
    }
}