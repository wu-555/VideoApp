package com.wutiancheng.videoapp.list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutAbsListFragmentBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.model.Feed
import kotlinx.coroutines.launch

open class AbsListFragment : Fragment() {
    private lateinit var feedAdapter: FeedAdapter

    // 使用属性委托
    private val binding: LayoutAbsListFragmentBinding by invokeViewBinding()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecycleView()
    }

    private fun setUpRecycleView() {
        val context = requireContext()

        // 设置每个item之间的间隔样式
        // 在Theme文件中重新指定android:listDivider属性值为创建的drawable资源
        binding.listView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        // 初始化页面主体的Adapter，也就是RecycleView的Adapter
        feedAdapter = FeedAdapter(childFragmentManager, getFeedType(), viewLifecycleOwner)

        // 设置页脚的Adapter
        val concatAdapter = feedAdapter.withLoadStateFooter(FooterLoadStateAdapter())
        binding.listView.adapter = concatAdapter
        // 设置布局属性
        binding.listView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        // 设置下拉时，进度圆圈的圆环颜色
        binding.refreshLayout.setColorSchemeColors(context.getColor(R.color.color_theme))
        // 设置刷新动作监听
        binding.refreshLayout.setOnRefreshListener {
            // 通知feedAdapter刷新数据
            feedAdapter.refresh()
        }

        lifecycleScope.launch {
            feedAdapter.onPagesUpdatedFlow.collect {
                val hasData = feedAdapter.itemCount > 0
                // 当有数据时，显示列表并隐藏加载视图
                binding.listView.setVisibility(hasData)
                binding.loadingStatus.setVisibility(!hasData)
                // 结束转圈的动画
                binding.refreshLayout.isRefreshing = false
                // 当没有数据时，就显示空白
                if (!hasData) {
                    binding.loadingStatus.showEmpty {
                        feedAdapter.retry()
                    }
                }
            }
        }

    }

    fun submitData(pagingData: PagingData<Feed>) {
        lifecycleScope.launch {
            feedAdapter.submitData(pagingData)
        }
    }

    fun getFeedType(): String {
        return arguments?.getString("feedType") ?: "all"
    }
}