package com.wutiancheng.videoapp.model

class Category {
    var activateSize = 0
    var normalSize = 0
    var activeColor: String? = null
    var normalColor: String? = null
    var select = 0
    var tabGravity = 0
    var tabs: List<Tab>? = null

    class Tab {
        var title: String? = null
        var index = 0
        var tag: String? = null
    }
}