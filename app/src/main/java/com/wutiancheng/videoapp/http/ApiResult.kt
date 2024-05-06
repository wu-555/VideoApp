package com.wutiancheng.videoapp.http

class ApiResult<T> {
    internal var nextPageKey: Long? = 0
    // 网络请求的状态
    internal var status=0
    // 网络请求当前是否存在异常
    val success:Boolean
        get() = status==200

    var errMsg:String=""
    // 每次请求的数据模型对象
    var body:T?=null
}