package com.wutiancheng.videoapp.ext

import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.Ugc
import com.wutiancheng.videoapp.page.login.UserManager
import kotlinx.coroutines.flow.collectLatest


/**
 * @param [like] 用于判断用户点击的按钮是点赞还是不喜欢
 */
suspend fun Feed.toggleFeedLike(like:Boolean,callback:(Ugc)->Unit){
    UserManager.loginIfNeed()
    UserManager.getUser().collectLatest {
        if (it.userId <= 0) return@collectLatest
        val response = if (like) {
            ApiService.iApiInterface.toggleFeedLike(itemId, it.userId)
        } else {
            ApiService.iApiInterface.toggleDissFeed(itemId, it.userId)
        }
        response.body?.run {
            val ugc = Ugc()
            ugc.hasLiked = this.getAsJsonPrimitive("hasLiked").asBoolean
            ugc.hasdiss = this.getAsJsonPrimitive("hasdiss").asBoolean
            ugc.likeCount = this.getAsJsonPrimitive("likeCount").asInt
            callback(ugc)
        }
    }
}

suspend fun Feed.toggleFeedFavorite(callback:(Ugc)->Unit){
    UserManager.loginIfNeed()
    UserManager.getUser().collectLatest {
        if (it.userId <= 0) return@collectLatest
        val response = ApiService.iApiInterface.toggleFeedFavorite(it.userId,itemId)
        response.body?.run {
            val ugc = Ugc()
            ugc.hasFavorite=this.getAsJsonPrimitive("hasFavorite").asBoolean
            callback(ugc)
        }
    }
}