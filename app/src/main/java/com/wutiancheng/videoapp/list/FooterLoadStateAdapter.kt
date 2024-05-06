package com.wutiancheng.videoapp.list

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutAbsListLoadingFooterBinding

// FooterLoadStateAdapter用于管理页面页脚的view
class FooterLoadStateAdapter : LoadStateAdapter<FooterLoadStateAdapter.LoadStateViewHolder>() {

    inner class LoadStateViewHolder(val binding: LayoutAbsListLoadingFooterBinding) :
        RecyclerView.ViewHolder(binding.root)


    // loadState：列表的加载状态
    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        val loading = holder.binding.loading
        val loadingText = holder.binding.text
        when (loadState) {
            is LoadState.Loading -> {
                loadingText.setText(R.string.abs_list_loading_footer_loading)
                loading.show()
                return
            }

            is LoadState.Error -> {
                Log.d("OnBindViewHolder","已经没有更多了")
                loadingText.setText(R.string.abs_list_loading_footer_error)
            }

            else -> {}
        }
        loading.hide()

        // 下UI的下一帧绘制时执行，这样视图的隐藏更平滑
        loading.postOnAnimation {
            loading.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = LayoutAbsListLoadingFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LoadStateViewHolder(binding)
    }
}