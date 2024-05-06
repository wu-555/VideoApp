package com.wutiancheng.videoapp.page.tag

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.pullrefresh.PullRefreshDefaults
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import coil.compose.rememberAsyncImagePainter
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.ext.invokeViewModel
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.TYPE_IMAGE_TEXT
import com.wutiancheng.videoapp.model.TYPE_TEXT
import com.wutiancheng.videoapp.model.TYPE_VIDEO
import com.wutiancheng.videoapp.page.detail.FeedDetailActivity
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import com.wutiancheng.videoapp.view.LoadingStatusView
import kotlinx.coroutines.launch
import kotlin.math.abs

@NavDestination(NavDestination.NavType.Fragment, "tags_fragment")
class TagsFragment : Fragment() {
    private val tagsViewModel: TagsViewModel by invokeViewModel()
    private val loadingResId = listOf(
        R.drawable.loading_big_1,
        R.drawable.loading_big_4,
        R.drawable.loading_big_7,
        R.drawable.loading_big_10,
        R.drawable.loading_big_13,
        R.drawable.loading_big_16,
        R.drawable.loading_big_19
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).also {
            it.setContent {
                WaterFull()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
    @Composable
    fun WaterFull() {
        var stateItems = remember {
            mutableStateListOf<Feed>()
        }

        var loadMoreState by remember {
            mutableStateOf(true)
        }

        // 初始加载数据
        LaunchedEffect(key1 = true, block = {
            val result = tagsViewModel.loadData()
            result.body?.run {
                stateItems.clear()
                stateItems.addAll(this)
            }
        })

        val coroutineScope = rememberCoroutineScope()

        // 记录刷新状态
        var refreshState = RememberPullRefreshState {
            coroutineScope.launch {
                val result = tagsViewModel.loadData()
                result.body?.run {
                    stateItems.clear()
                    stateItems.addAll(this)
                }
            }
        }
        // 根据页面数据显示空布局
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(onPull = {
                    refreshState.onPull(it)
                }, onRelease = {
                    refreshState.onRelease()
                })
        ) {
            AndroidView(factory = {
                LoadingStatusView(it)
            }, modifier = Modifier.fillMaxSize(), update = {
                it.setVisibility(stateItems.isEmpty())
            })

            // 瀑布流列表
            LazyVerticalStaggeredGrid(modifier = Modifier
                .background(
                    color = Color(
                        LocalContext.current.getColor(
                            R.color.color_gray
                        )
                    )
                )
                .padding(4.dp)
                .graphicsLayer {
                    translationY = refreshState.pullDistancePx
                }, columns = StaggeredGridCells.Fixed(2), content = {
                itemsIndexed(stateItems) { index, item ->
                    StaggeredItem(item = item)
                    if (index >= stateItems.size - 1) {
                        // 预加载，底部还有一个item时就进行加载
                        LaunchedEffect(key1 = stateItems.size, block = {
                            val result = tagsViewModel.loadData(false)
                            if (result.body?.isEmpty() == true) {
                                loadMoreState = false
                            } else {
                                loadMoreState = true
                                stateItems.addAll(result.body!!)
                            }
                        })
                    }
                }
                if (stateItems.isNotEmpty()) {
                    // StaggeredGridItemSpan.FullLine：让item占据整行
                    // 这个footer是在stateItems有数据时一直存在的，不像之前的列表是动态显示出来。
                    // 当获得数据时，列表拉到最后会显示“正在加载”
                    // 当无法再获得数据时，列表拉到最后会显示“已经没有更多”
                    item(span = StaggeredGridItemSpan.FullLine) {
                        loadMoreItem(loadMoreState)
                    }
                }
            }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalItemSpacing = 4.dp
            )
            PullRefreshIndicator(refreshState)
        }
    }

    @Composable
    private fun loadMoreItem(loadMoreState: Boolean) {
        if (loadMoreState) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "正在加载中...")
            }
        } else {
            Text(
                text = "——已经没有更多了——",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            )
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun RememberPullRefreshState(onRefresh: () -> Unit): PullRefreshState {
        var thresholdDistancePx = 0f
        var refreshOffsetPx = 0f

        with(LocalDensity.current) {
            thresholdDistancePx = PullRefreshDefaults.RefreshThreshold.toPx()
            refreshOffsetPx = PullRefreshDefaults.RefreshingOffset.toPx()
        }

        // 把需要记录的数据都放到这一个类里面进行统一管理,并用remember记录
        val state by remember {
            mutableStateOf(PullRefreshState(thresholdDistancePx, refreshOffsetPx, onRefresh))
        }

        return state
    }

    @Composable
    fun PullRefreshIndicator(refreshState: PullRefreshState) {
        val loadingAnimate = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = loadingResId.size.toFloat() - 1,
            animationSpec = infiniteRepeatable(
                animation = tween(500), repeatMode = RepeatMode.Reverse
            ),
            label = ""
        )
        val id =
            if (refreshState.refreshing) loadingAnimate.value else abs(refreshState.pullDistancePx) % loadingResId.size
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = loadingResId[id.toInt()]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp, 16.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = if (refreshState.pullDistancePx > 20) {
                            refreshState.pullDistancePx / 2
                        } else {
                            -60f
                        }
                    })
        }

    }

    class PullRefreshState(
        private val thresholdDistancePx: Float,
        private val refreshOffsetPx: Float,
        private val onRefresh: suspend () -> Unit
    ) {
        // 下拉滑动距离
        var pullDistancePx by mutableStateOf(0f)

        // 刷新状态
        var refreshing by mutableStateOf(false)

        fun onPull(delta: Float): Float {
            // 只有当下拉时，也就是delta<=0时，不执行下拉动作
            if (delta <= 0f) return 0f
            if (pullDistancePx + delta > thresholdDistancePx) {
                val consumePx = pullDistancePx + delta - thresholdDistancePx
                pullDistancePx = thresholdDistancePx
                return consumePx
            } else {
                pullDistancePx += delta
                return delta
            }
        }

        suspend fun onRelease() {
            if (pullDistancePx > refreshOffsetPx) {
                var initialValue = pullDistancePx
                animate(initialValue, refreshOffsetPx) { value, _ ->
                    pullDistancePx = value
                }
                refreshing = true
                onRefresh()
                initialValue = refreshOffsetPx
                animate(initialValue, 0f) { value, _ ->
                    pullDistancePx = value
                }
                refreshing = false
            } else {
                val initialValue = pullDistancePx
                animate(initialValue, 0f) { value, _ ->
                    pullDistancePx = value
                }
            }
        }

    }

    @Composable
    fun StaggeredItem(item: Feed) {
        Column(modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .wrapContentSize()
            .background(Color.White)
            .clickable {
                FeedDetailActivity.startFeedDetailActivity(
                    requireActivity(), item, when (item.itemType) {
                        TYPE_VIDEO -> "video"
                        TYPE_IMAGE_TEXT -> "pics"
                        TYPE_TEXT -> "text"
                        else -> "all"
                    }, null
                )
            }) {
            Box(modifier = Modifier.padding(bottom = 5.dp)) {
                // 帖子封面，如果帖子是视频，则右上角会有播放图标
                item.cover?.run {
                    Image(
                        painter = rememberAsyncImagePainter(model = this),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    if (item.itemType == TYPE_VIDEO) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                        )
                    }
                    // 封面底部的点赞和评论图标，以及对应的数量
                    item.ugc?.run {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                tint = Color.White,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(end = 2.dp)
                            )
                            Text(
                                text = item.ugc!!.likeCount.toString(),
                                fontSize = TextUnit(12f, TextUnitType.Sp),
                                color = Color.White,
                                modifier = Modifier.padding(end = 2.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Icon(
                                imageVector = Icons.Outlined.Comment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(horizontal = 2.dp)
                            )
                            Text(
                                text = item.ugc!!.commentCount.toString(),
                                fontSize = TextUnit(12f, TextUnitType.Sp),
                                color = Color.White,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                    }
                }
            }
            item.feedsText?.run {
                Text(
                    text = this,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    modifier = Modifier.padding(5.dp)
                )
            }

            item.author?.run {
                Row(modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(model = item.author.avatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )

                    Text(
                        text = item.author.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Gray,
                        modifier = Modifier.offset(x = 8.dp)
                    )
                }
            }
        }
    }
}