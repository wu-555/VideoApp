package com.wutiancheng.videoapp.page.user

import androidx.lifecycle.ViewModel
import com.wutiancheng.videoapp.http.ApiResult
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.HttpResult
import com.wutiancheng.videoapp.page.login.UserManager

class UserViewModel : ViewModel() {
    suspend fun loadData(): ApiResult<List<Feed>>? {
        kotlin.runCatching {
            ApiService.iApiInterface.queryProfileFeeds(UserManager.userId())
        }.onFailure {
            it.printStackTrace()
        }.onSuccess {
            return it
        }
        return null
    }

    suspend fun removeData(itemId: Long, userId: Long): ApiResult<HttpResult>? {
        kotlin.runCatching {
            ApiService.iApiInterface.deleteFeed(itemId, userId)
        }.onFailure {
            it.printStackTrace()
        }.onSuccess {
            return it
        }
        return null
    }
}