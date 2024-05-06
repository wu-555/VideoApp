package com.wutiancheng.videoapp.page.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.paging.map
import com.wutiancheng.videoapp.ext.invokeViewModel
import com.wutiancheng.videoapp.list.AbsListFragment
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import kotlinx.coroutines.launch

@NavDestination(NavDestination.NavType.Fragment, asStarter = true, route = "home_fragment")
class HomeFragment : AbsListFragment(){
    private val homeViewModel:HomeViewModel by invokeViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch{
            homeViewModel.setFeedType(getFeedType())
            // flow的collect方法是个挂起方法，需要协程作用域，同时，这种设计也可以避免在UI线程中更新数据
            // 调用collect后，数据也就正式开始加载了
            homeViewModel.pageFlow.collect{
                submitData(it)
            }
        }
    }

    companion object {
        fun newInstance(feedType: String?): Fragment {
            val args=Bundle().apply {
                putString("feedType",feedType)
            }
            val fragment=HomeFragment()
            fragment.arguments=args
            return fragment
        }
    }
}