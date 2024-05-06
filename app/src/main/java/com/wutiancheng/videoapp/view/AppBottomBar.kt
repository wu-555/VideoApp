package com.wutiancheng.videoapp.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarMenuView
import com.wutiancheng.videoapp.util.AppConfig
import com.wutiancheng.videoapp.R
import kotlin.math.roundToInt

@SuppressLint("RestrictedApi")
class AppBottomBar(context: Context, attrs: AttributeSet?=null) : BottomNavigationView(context, attrs) {
    private val sIcons= intArrayOf(
        R.drawable.icon_tab_main,
        R.drawable.icon_tab_category,
        R.drawable.icon_tab_publish,
        R.drawable.icon_tab_tags,
        R.drawable.icon_tab_user
    )

    init {
        val config = AppConfig.getBottomConfig()
        val states = arrayOfNulls<IntArray>(2)
        states[0] = IntArray(1) { android.R.attr.state_selected }
        states[1] = intArrayOf()

        val colors = intArrayOf(
            Color.parseColor(config.activeColor),
            Color.parseColor(config.inActivityColor)
        )

        // states表示按钮的状态，colors表示状态对应的颜色
        // states有几维状态组，colors就要有几维
        // 可以多个状态对应于一个颜色，把这多个状态放到一个数组里就行
        val colorStateList = ColorStateList(states, colors)

        // 设置tab文本的颜色状态
        itemTextColor = colorStateList

        // 设置tab icon的颜色状态
        itemIconTintList = colorStateList

        //设置文本的可见状态
        //LABEL_VISIBILITY_LABELED:设置按钮的文本为一直显示模式
        //LABEL_VISIBILITY_AUTO:当按钮个数小于三个时一直显示，或者当按钮个数大于3个且小于5个时，被选中的那个按钮文本才会显示
        //LABEL_VISIBILITY_SELECTED：只有被选中的那个按钮的文本才会显示
        //LABEL_VISIBILITY_UNLABELED:所有的按钮文本都不显示
        labelVisibilityMode = LABEL_VISIBILITY_LABELED

        val tabs=config.tabs

        // 在菜单中添加tab选项，并为每一个tab设置icon
        tabs.forEachIndexed { index, tab ->
            if(!tab.enable) return@forEachIndexed
            val menuItem=menu.add(0,tab.route.hashCode(),index,tab.title)
            menuItem.setIcon(sIcons[index])
        }

        // 为每一个icon设置大小
        tabs.forEachIndexed { index, tab ->
            // 根据屏幕像素设置大小
            val iconSize=dp2Px(tab.size)

            // 拿到NavigationBarMenuView
            val menuView=getChildAt(0) as NavigationBarMenuView

            // 防止中间的添加按钮icon向上移动后被截断
            // clipChildren必须给该viewGroup的所有父布局文件都设为clipChildren=false
            menuView.clipChildren=false
            menuView.clipToPadding=false
            // 拿到ItemView
            val itemView=menuView.getChildAt(index) as BottomNavigationItemView
            itemView.setIconSize(iconSize)
            itemView.clipChildren=false
            itemView.clipToPadding=false
            if(TextUtils.isEmpty(tab.title)){
                itemView.setIconTintList(ColorStateList.valueOf(Color.parseColor(config.activeColor)))
                post(Runnable {
                    itemView.scrollBy(0,dp2Px(20))
                })
            }
        }

        // 设置默认fragment
        if(config.selectTab>=0){
            val tab=tabs[config.selectTab]
            val itemId=tab.route.hashCode()
            post { Runnable{
                selectedItemId=itemId
            } }
        }
    }

    private fun dp2Px(size: Int): Int {
        val density=context.resources.displayMetrics.density
        return (density*size+0.5f).roundToInt()
    }
}