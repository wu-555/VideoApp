package com.wutiancheng.videoapp.page.detail

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wutiancheng.videoapp.util.PxUtil
import com.wutiancheng.videoapp.view.LoadingStatusView

class LoadStateAdapter(val height:Int=PxUtil.getScreenHeight()/3): LoadStateAdapter<RecyclerView.ViewHolder>() {
    private var recyclerView: RecyclerView? = null
    private var loadingStatusView: LoadingStatusView? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, loadState: LoadState) {
        loadingStatusView!!.showEmpty(text="快来抢占第一把🪑吧")
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): RecyclerView.ViewHolder {
        loadingStatusView = LoadingStatusView(parent.context)
        loadingStatusView!!.layoutParams=ViewGroup.LayoutParams(-1,height)
        return object : RecyclerView.ViewHolder(loadingStatusView!!) {}
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        val concatAdapter = recyclerView?.adapter as? ConcatAdapter ?: return true
        var itemCount = 0
        for (adapter in concatAdapter.adapters) {
            // 查看管理数的adapter是否有数据
            if (adapter !is LoadStateAdapter) {
                itemCount = adapter.itemCount
            }
        }
        return itemCount <= 0
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
}