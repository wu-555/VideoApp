package com.wutiancheng.videoapp.page.category

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.wutiancheng.videoapp.databinding.LayoutFragmentCategoryBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.ext.setTextVisibility
import com.wutiancheng.videoapp.navigation.BaseFragment
import com.wutiancheng.videoapp.page.home.HomeFragment
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import com.wutiancheng.videoapp.util.AppConfig

@NavDestination(NavDestination.NavType.Fragment, "category_fragment")
class CategoryFragment : Fragment() {
    private val viewBinding: LayoutFragmentCategoryBinding by invokeViewBinding()
    private val categoryConfig = AppConfig.getCategoryConfig()
    private lateinit var mediator: TabLayoutMediator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = viewBinding.tabLayout
        val viewPager = viewBinding.viewPager

        // 关闭预加载，但下面这行代码没有效果
        //viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        // 由于ViewPager2底层的预加载是通过RecycleView的layoutManager实现的，因此可以通过获得第一个子view(底层的recyclerView)进行关闭
        (viewPager.getChildAt(0) as RecyclerView).layoutManager?.isItemPrefetchEnabled = false


        // ViewPager2的底层实际是RecycleView，因此要设置adapter
        viewPager.adapter = object : FragmentStateAdapter(childFragmentManager, this.lifecycle) {
            override fun getItemCount(): Int {
                return categoryConfig.tabs!!.size
            }

            override fun createFragment(position: Int): Fragment {
                val tag = categoryConfig.tabs!![position].tag
                return HomeFragment.newInstance(tag)
            }
        }

        // tab的对齐方式
        tabLayout.tabGravity = categoryConfig.tabGravity

        // tabLayout和viewPager进行绑定，这样滑动的时候能联动起来

        mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.customView = makeTabView(position)
        }
        mediator.attach()

        // 设置滑动时的文本状态的样式改变
        viewPager.registerOnPageChangeCallback(pageChangeCallback)

        // 设置默认选中的页面，因为数据和视图是异步创建获取的，因此这里也采用异步的方式设置
        viewPager.post {
            viewPager.currentItem = categoryConfig.select
        }
    }

    private val pageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            // 获取tab的数量
            val childCount = viewBinding.tabLayout.childCount
            // 遍历每一个tab
            for (i in 0 until childCount) {
                val tab = viewBinding.tabLayout.getTabAt(i)
                val customView = tab!!.customView as TextView
                if (tab.position == position) {
                    // 进入这个条件说明当前的tab是被选中的tab
                    // 被选中时字体放大，样式设为粗体
                    customView.textSize = categoryConfig.activateSize.toFloat()
                    customView.typeface = Typeface.DEFAULT_BOLD
                } else {
                    customView.textSize = categoryConfig.normalSize.toFloat()
                    customView.typeface = Typeface.DEFAULT
                }
            }
        }
    }

    private fun makeTabView(position: Int): View {
        val tabView = TextView(context)

        // tab状态
        val states = arrayOfNulls<IntArray>(2)
        states[0] = intArrayOf(android.R.attr.state_selected)
        states[1] = intArrayOf()

        // tab状态对应的颜色
        val colors = intArrayOf(
            Color.parseColor(categoryConfig.activeColor),
            Color.parseColor(categoryConfig.normalColor)
        )

        // 将状态与颜色进行关联
        val stateList = ColorStateList(states, colors)

        // 设置视图文本
        tabView.setTextColor(stateList)
        tabView.gravity = Gravity.CENTER
        tabView.text=categoryConfig.tabs!![position].title
        tabView.textSize = categoryConfig.normalSize.toFloat()
        return tabView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediator.detach()
        viewBinding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }
}