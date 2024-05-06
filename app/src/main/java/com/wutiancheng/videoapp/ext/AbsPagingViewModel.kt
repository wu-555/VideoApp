package com.wutiancheng.videoapp.ext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.wutiancheng.videoapp.http.ApiResult

abstract class AbsPagingViewModel<T : Any> : ViewModel() {
    val pageFlow = Pager(
        config = PagingConfig(
            pageSize = 10,
            initialLoadSize = 10,
            enablePlaceholders = false,
            prefetchDistance = 1
        ), pagingSourceFactory = {
            AbsPagingSource()
        }
    ).flow.cachedIn(viewModelScope)

    inner class AbsPagingSource : PagingSource<Long, T>() {
        override fun getRefreshKey(state: PagingState<Long, T>): Long? {
            return null
        }

        override suspend fun load(params: LoadParams<Long>): LoadResult<Long, T> {
            kotlin.runCatching {
                this@AbsPagingViewModel.doLoadPage(params)
            }.onSuccess {
                if (it.body?.isNotEmpty() == true) {
                    return LoadResult.Page(it.body!!, null, it.nextPageKey)
                }
            }.onFailure {
                it.printStackTrace()
            }
            return if (params.key == null) LoadResult.Page(
                arrayListOf(),
                null,
                0
            ) else LoadResult.Error(RuntimeException("no more data to fetch"))
        }

        override val keyReuseSupported: Boolean
            get() = true
    }

    abstract suspend fun doLoadPage(params: PagingSource.LoadParams<Long>): ApiResult<List<T>>
}