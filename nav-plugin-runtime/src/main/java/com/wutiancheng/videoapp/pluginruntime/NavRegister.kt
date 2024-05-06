// this file is generated by auto,please do not modify
package com.wutiancheng.videoapp.pluginruntime

import com.wutiancheng.videoapp.pluginruntime.NavDestination.NavType.Activity
import com.wutiancheng.videoapp.pluginruntime.NavDestination.NavType.Dialog
import com.wutiancheng.videoapp.pluginruntime.NavDestination.NavType.Fragment
import com.wutiancheng.videoapp.pluginruntime.NavDestination.NavType.None
import kotlin.collections.ArrayList
import kotlin.collections.List

object NavRegister {
    private val navList: ArrayList<NavData> = ArrayList<NavData>()


    init {
        navList.add(NavData("home_fragment","com.wutiancheng.videoapp.page.home.HomeFragment",true,Fragment))
                navList.add(NavData("category_fragment","com.wutiancheng.videoapp.page.category.CategoryFragment",false,Fragment))
                navList.add(NavData("user_fragment","com.wutiancheng.videoapp.page.user.UserFragment",false,Fragment))
                navList.add(NavData("publish_activity","com.wutiancheng.videoapp.page.publish.PublishActivity",false,Activity))
                navList.add(NavData("tags_fragment","com.wutiancheng.videoapp.page.tag.TagsFragment",false,Fragment))

    }

    fun get(): List<NavData> {
        val list=ArrayList<NavData>()
                list.addAll(navList)
                return list
    }
}