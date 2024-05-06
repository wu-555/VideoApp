package com.wutiancheng.videoapp.page.login

import android.content.Intent
import com.wutiancheng.videoapp.cache.CacheManager
import com.wutiancheng.videoapp.model.Author
import com.wutiancheng.videoapp.util.AppGlobals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object UserManager {
    // 这个属性用于缓存当前登陆的用户信息，以免每次都要去数据库里查询
    private val userFlow:MutableStateFlow<Author> = MutableStateFlow(Author())
    suspend fun save(author: Author){
        CacheManager.get().authorDao.saveAuthor(author)
        userFlow.emit(author)
    }

    fun isLogin():Boolean{
        return userFlow.value.expiresTime>System.currentTimeMillis()
    }

    fun loginIfNeed(){
        if(isLogin()){
            return
        }else{
            val intent=Intent(AppGlobals.getApplication(),LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            AppGlobals.getApplication().startActivity(intent)
        }
    }

    suspend fun getUser():Flow<Author>{
        loadCache()
        return userFlow
    }

    suspend fun userId():Long{
        loadCache()
        return userFlow.value.userId
    }

    private suspend fun loadCache() {
        if(!isLogin()){
            val cache=CacheManager.get().authorDao.getUser()
            cache?.run{
                userFlow.emit(this)
            }
        }
    }
}