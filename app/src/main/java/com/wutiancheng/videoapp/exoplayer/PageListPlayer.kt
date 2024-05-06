package com.wutiancheng.videoapp.exoplayer

import android.annotation.SuppressLint
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.util.AppGlobals
import java.io.File

// 一个页面对应一个唯一的PageListPlayer，根据页面的名字获取它
// 这个类实现exoplayer
class PageListPlayer : IListPlayer, Player.Listener, StyledPlayerControlView.VisibilityListener {
    private val exoPlayer: ExoPlayer
    private val exoPlayerView: StyledPlayerView
    private val exoPlayerControllerView: StyledPlayerControlView

    private var playing: Boolean = false

    private var playingUrl: String? = null

    override var attachView: WrapperPlayerView? = null

    override val isPlaying: Boolean
        get() = playing

    init {
        exoPlayer = ExoPlayer.Builder(AppGlobals.getApplication().applicationContext).build()
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        exoPlayerView = LayoutInflater.from(AppGlobals.getApplication().applicationContext)
            .inflate(R.layout.layout_exo_player_view, null) as StyledPlayerView

        // 这个设置用于在视频帖子详情页面，视频的宽高比例会随着WrapperPlayerView的layoutParams里的宽高变化而变化
        exoPlayerView.resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT

        exoPlayerControllerView =
            LayoutInflater.from(AppGlobals.getApplication().applicationContext)
                .inflate(
                    R.layout.layout_exo_player_controller_view,
                    null
                ) as StyledPlayerControlView

        // 只有设置了player属性，才能把exoPlayer绑定上去，从而显示画面，更新进度条
        exoPlayerView.player = exoPlayer
        exoPlayerControllerView.player = exoPlayer
    }

    // 处理视频暂停
    override fun inActive() {
        if (TextUtils.isEmpty(playingUrl) || attachView == null) {
            return
        }
        exoPlayer.playWhenReady = false
        exoPlayer.removeListener(this)
        exoPlayerControllerView.removeVisibilityListener(this)
        attachView?.inActive()
    }

    // 处理视频播放
    override fun onActive() {
        if (TextUtils.isEmpty(playingUrl) || attachView == null) {
            return
        }
        // 缓存加载成功后自动播放
        exoPlayer.playWhenReady = true

        exoPlayer.addListener(this)

        exoPlayerControllerView.addVisibilityListener(this)

        // 显示视频播放控制器
        exoPlayerControllerView.show()

        // 一个新视频要播放时，要把exoPlayerView和exoPlayerControllerView添加到wrapperPlayerView上去
        // 也就是在这里，对视频帖子动态添加exoPlayerView和exoPlayerControllerView
        attachView?.onActive(exoPlayerView, exoPlayerControllerView)

        if (exoPlayer.playbackState == Player.STATE_READY) {
            onPlayerStateChanged(true, Player.STATE_READY)
        } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun togglePlay(attachView: WrapperPlayerView?, videoUrl: String) {
        attachView?.setOnTouchListener(null)
        // 当触碰视频view时，显示exoPlayerControllerView
        attachView?.setOnTouchListener { _, _ ->
            exoPlayerControllerView.show()
            true
        }

        if (TextUtils.equals(videoUrl, playingUrl)) {
            // 如果待播放的url和正在播放的url相同，说明点击的是正在播放的视频的暂停/播放按钮
            if (playing) {
                inActive()
            } else {
                // 当进入帖子详情页时，由于是共用一个播放器，因此要切换绑定一下详情页的wrapperPlayerView
                this.attachView=attachView
                onActive()
            }
        } else {
            // 说明点了一个新的视频的播放按钮
            // 先暂停当前播放的视频
            inActive()
            // 记录新视频所属item的wrapperView
            this.attachView = attachView
            // 记录新视频的url
            playingUrl = videoUrl
            // 设置数据源
            exoPlayer.setMediaSource(createMediaSource(videoUrl))
            // 加载
            exoPlayer.prepare()
            // 播放视频
            onActive()
        }
    }

    override fun stop(release: Boolean) {
        playing = false
        playingUrl = null
        exoPlayer.playWhenReady = false
        exoPlayerControllerView.hideImmediately()
        attachView?.removeView(exoPlayerView)
        attachView?.removeView(exoPlayerControllerView)
        attachView = null
        if (release) {
            // 代表页面被销毁了
            exoPlayer.release()
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        playing = playbackState == Player.STATE_READY && playWhenReady
        attachView?.onPlayerStateChanged(playing, playbackState)
    }

    //  exoPlayerControllerView的显示
    override fun onVisibilityChange(visibility: Int) {
        attachView?.onVisibilityChange(visibility, exoPlayer.playbackState == Player.STATE_ENDED)
    }

    // 进度条拖动回调函数
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        exoPlayer.playWhenReady = true
    }

    companion object {
        // 本地缓存效果
        private val cache = SimpleCache(
            AppGlobals.getApplication().cacheDir,
            LeastRecentlyUsedCacheEvictor(1024 * 1024 * 200),
            StandaloneDatabaseProvider(AppGlobals.getApplication().applicationContext)
        )

        // flag的意思是加载缓存的时候如果没有加载到则等待
        private val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheWriteDataSinkFactory(
                CacheDataSink.Factory().setCache(cache).setFragmentSize(Long.MAX_VALUE)
            )
            .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
        private val progressMediaSourceFactory = ProgressiveMediaSource.Factory(
            cacheDataSourceFactory
        )


        private val sPageListPlayer = hashMapOf<String, IListPlayer>()

        // 根据pageName拿到这个page对应的pageListPlayer
        fun get(pageName: String): IListPlayer {
            if (!sPageListPlayer.containsKey(pageName)) {
                val pageListPlayer = PageListPlayer()
                sPageListPlayer[pageName] = pageListPlayer
            }
            return sPageListPlayer[pageName]!!
        }

        // 根据page的销毁情况进行资源释放
        // release为true代表页面被销毁，播放器也要销毁
        // release为false代表页面视图被销毁，则播放器保留
        fun stop(pageName: String, release: Boolean = true) {
            if (release) {
                sPageListPlayer.remove(pageName)?.stop(true)
            } else {
                sPageListPlayer.remove(pageName)?.stop(false)
            }
        }

        fun createMediaSource(videoUrl: String): MediaSource {
            val file = File(videoUrl)
            // 如果是本地文件，则重新创建一个ProgressiveMediaSource
            // 当dataSourceFactory指定为FileDataSource.factory，才能正常的从本地文件播放视频
            if (file.exists()) {
                val dataSpec = DataSpec(Uri.fromFile(file))
                val fileDataSource = FileDataSource()
                fileDataSource.open(dataSpec)
                val uri = fileDataSource.uri
                val factory = ProgressiveMediaSource.Factory(FileDataSource.Factory())
                return factory.createMediaSource(MediaItem.fromUri(uri!!))
            } else {
                return progressMediaSourceFactory.createMediaSource(
                    MediaItem.fromUri(
                        Uri.parse(
                            videoUrl
                        )
                    )
                )
            }
        }
    }
}