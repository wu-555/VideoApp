package com.wutiancheng.videoapp.cache

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wutiancheng.videoapp.model.Author
import com.wutiancheng.videoapp.util.AppGlobals

@Database(entities = [Author::class], version = 1)
abstract class CacheManager : RoomDatabase() {
    abstract val authorDao:AuthorDao

    companion object{
        private val database=Room.databaseBuilder(AppGlobals.getApplication().applicationContext,CacheManager::class.java,"videoapp_cache")
            .allowMainThreadQueries()//允许在主线程中进行查询操作
            .build()

        @JvmStatic
        fun get():CacheManager{
            return database
        }
    }
}