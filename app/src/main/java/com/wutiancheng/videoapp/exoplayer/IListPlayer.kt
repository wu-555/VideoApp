package com.wutiancheng.videoapp.exoplayer

interface IListPlayer {
    // 获取当前视频播放器的exoPlayer(textureView)是否已经被挂载到某个容器上
    val attachView:WrapperPlayerView?

    // 是否正在进行视频播放
    val isPlaying:Boolean

    // 页面不可见时暂停播放
    fun inActive()

    // 页面回复可见时，继续播放
    fun onActive()

    // 点击播放/暂停按钮
    fun togglePlay(attachView:WrapperPlayerView?,videoUrl:String)

    // 释放视频播放器资源
    fun stop(release:Boolean)
}