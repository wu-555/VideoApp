package com.wutiancheng.videoapp.cache

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wutiancheng.videoapp.model.Author

// Dao:data access object
@Dao
interface AuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAuthor(author: Author):Long

    @Query("select * from author limit 1")
    suspend fun getUser():Author?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(author: Author):Int

    @Delete
    suspend fun delete(author: Author):Int
}