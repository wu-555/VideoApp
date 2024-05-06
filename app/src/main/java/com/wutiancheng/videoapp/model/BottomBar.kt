package com.wutiancheng.videoapp.model

import com.google.gson.annotations.SerializedName

class BottomBar {
    // tab被选中时的颜色
    @SerializedName("activityColor")
    var activeColor: String = ""

    @SerializedName("inActiveColor")
    // tab未被选中时的颜色
    var inActivityColor: String = ""

    // 存储所有的tab
    var tabs: List<Tab> = mutableListOf()

    // 默认展示的tab
    var selectTab: Int = 0

    class Tab {
        // 图标的大小
        var size: Int = 0

        // 是否显示这个tab
        var enable: Boolean = false

        // tab对应的index
        var index: Int = 0

        // tab对应的route
        var route: String = ""

        // tab的标题
        var title: String = ""

        // 是否需要登陆
        var needLogin: Boolean = false
    }

}