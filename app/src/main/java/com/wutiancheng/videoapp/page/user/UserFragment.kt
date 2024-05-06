package com.wutiancheng.videoapp.page.user

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import coil.compose.rememberAsyncImagePainter
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.ext.invokeViewModel
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.model.Author
import com.wutiancheng.videoapp.model.Feed
import com.wutiancheng.videoapp.model.TYPE_IMAGE_TEXT
import com.wutiancheng.videoapp.model.TYPE_TEXT
import com.wutiancheng.videoapp.model.TYPE_VIDEO
import com.wutiancheng.videoapp.page.detail.FeedDetailActivity
import com.wutiancheng.videoapp.page.login.UserManager
import com.wutiancheng.videoapp.page.publish.PublishActivity
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import com.wutiancheng.videoapp.view.LoadingStatusView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@NavDestination(NavDestination.NavType.Fragment, "user_fragment")
class UserFragment : Fragment() {
    private val userViewModel: UserViewModel by invokeViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).also {
            it.setContent {
                UserScreen()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
    @Preview
    @Composable
    fun UserScreen() {
        val refreshState by remember {
            mutableStateOf(PullRefreshState())
        }

        val context = LocalContext.current

        val am = context.resources.assets

        val pagerState = rememberPagerState(0)

        val coroutine = rememberCoroutineScope()


        val pullRefreshState = rememberPullRefreshState(refreshing = refreshState.refreshingState.value, onRefresh = {
            coroutine.launch {
                refreshState.refreshingState.value = true
                val result = userViewModel.loadData()
                result?.body?.run {
                    refreshState.userFeedsState.clear()
                    refreshState.userFeedsState.addAll(this)
                }
                refreshState.refreshingState.value = false
            }
        })

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.color_gray))
                .pullRefresh(pullRefreshState)
        ) {
            // 登陆部分
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f),
                contentAlignment = Alignment.Center
            ) {
                // 未登录时
                AnimatedVisibility(visible = !refreshState.loginState.value) {
                    LoginUserCard()
                    LaunchedEffect(key1 = refreshState.loginState, block = {
                        UserManager.getUser().collectLatest {
                            if (it.userId <= 0) {
                                return@collectLatest
                            }
                            refreshState.loginState.value = true
                        }
                    })
                }
                // 登陆后
                AnimatedVisibility(visible = refreshState.loginState.value) {
                    Image(
                        bitmap = BitmapFactory.decodeStream(am.open("head_image_background.jpeg"))
                            .asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.5f
                    )
                    UserCard(author = refreshState.authorState.value)
                }
            }

            // 用户“作品”和“喜欢”列表
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color.White)
                    .align(
                        Alignment.BottomCenter
                    )
            ) {
                TabRow(selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(3.dp)
                        .align(Alignment.TopCenter),
                    backgroundColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            color = colorResource(id = R.color.color_theme),
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        )
                    })
                {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutine.launch {
                                pagerState.scrollToPage(0)
                            }
                        },
                        selectedContentColor = colorResource(
                            id = R.color.color_theme
                        ),
                        unselectedContentColor = Color.Gray
                    ) {
                        Text(
                            text = "拍摄作品",
                            textAlign = TextAlign.Center,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            fontWeight = if (pagerState.currentPage == 0) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutine.launch {
                                pagerState.scrollToPage(1)
                            }
                        },
                        selectedContentColor = colorResource(
                            id = R.color.color_theme
                        ),
                        unselectedContentColor = Color.Gray
                    ) {
                        Text(
                            text = "文字动态",
                            textAlign = TextAlign.Center,
                            fontSize = TextUnit(14f, TextUnitType.Sp),
                            fontWeight = if (pagerState.currentPage == 1) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                }
                HorizontalPager(
                    pageCount = 2,
                    pageSize = PageSize.Fill,
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(top = 50.dp),
                    beyondBoundsPageCount = 0
                ) {
                    // 根据页面数据显示空布局
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AndroidView(factory = {
                            LoadingStatusView(it)
                        }, modifier = Modifier.fillMaxSize(), update = {
                            it.setVisibility(refreshState.userFeedsState.isEmpty())
                            coroutine.launch {
                                val result = userViewModel.loadData()
                                if (result?.body == null) {
                                    it.showEmpty(
                                        text = "您目前还没有发布任何帖子哟～",
                                        retryText = "去发布"
                                    ) {
                                        val intent = Intent(context, PublishActivity::class.java)
                                        startActivity(intent)
                                    }
                                }
                            }
                        })
                    }
                    AnimatedVisibility(visible = !refreshState.userFeedsState.isEmpty()) {
                        if (it == 0) {
                            UserMediaList(userFeeds = refreshState.userFeedsState, refreshState.authorState.value) {
                                refreshState.refreshingState.value = true
                                val response = userViewModel.loadData()
                                response?.body?.run {
                                    refreshState.userFeedsState.clear()
                                    refreshState.userFeedsState.addAll(this)
                                }
                                refreshState.refreshingState.value = false
                            }
                        } else {
                            UserTextList(userFeeds = refreshState.userFeedsState, refreshState.authorState.value) {
                                refreshState.refreshingState.value = true
                                val response = userViewModel.loadData()
                                response?.body?.run {
                                    refreshState.userFeedsState.clear()
                                    refreshState.userFeedsState.addAll(this)
                                }
                                refreshState.refreshingState.value = false
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = refreshState.refreshingState.value,
                    state = pullRefreshState,
                    contentColor = colorResource(
                        id = R.color.color_theme
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        LaunchedEffect(key1 = refreshState.loginState, block =
        {
            val result = userViewModel.loadData()
            result?.body?.run {
                refreshState.userFeedsState.clear()
                refreshState.userFeedsState.addAll(this)
            }
        })

        LaunchedEffect(key1 = refreshState.loginState, block = {
            UserManager.getUser().collectLatest {
                refreshState.authorState.value = it
            }
        })
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UserMediaList(userFeeds: List<Feed>, author: Author, refreshCallback: suspend () -> Unit) {
        val mediaFeeds = userFeeds.filter {
            it.itemType != TYPE_TEXT && it.author?.userId == author.userId
        }
        mediaFeeds.forEach {
            it.feedsText = it.feedsText?.trimEnd()
        }
        LazyVerticalStaggeredGrid(modifier = Modifier
            .background(
                color = colorResource(id = R.color.color_gray)
            )
            .padding(4.dp), columns = StaggeredGridCells.Fixed(2), content = {
            itemsIndexed(mediaFeeds) { index, item ->
                StaggeredMediaItem(item = item, refreshCallback)
                if (index >= mediaFeeds.size - 1) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = "——已经没有更多了——",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                        )
                    }

                }
            }
        })
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UserTextList(userFeeds: List<Feed>, author: Author, refreshCallback: suspend () -> Unit) {
        val textFeeds = userFeeds.filter {
            it.itemType == TYPE_TEXT && it.author?.userId == author.userId
        }
        LazyColumn(
            modifier = Modifier
                .background(
                    color = Color(
                        LocalContext.current.getColor(
                            R.color.color_gray
                        )
                    )
                )
                .fillMaxSize(), content = {
                itemsIndexed(textFeeds) { index, item ->
                    ColumnTextItem(item = item, refreshCallback)
                    if (index >= textFeeds.size - 1) {
                        this@LazyColumn.item {
                            Text(
                                text = "——已经没有更多了——",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp)
                            )
                        }
                    }
                }
            }, verticalArrangement = Arrangement.spacedBy(4.dp)
        )
    }

    @Composable
    fun UserCard(author: Author) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = author.avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(80.dp))
                    .border(width = 2.dp, color = Color.White, shape = CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = author.name,
                fontSize = TextUnit(20f, TextUnitType.Sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 5.dp),
                fontWeight = FontWeight.Bold
            )
        }


    }

    @Composable
    fun LoginUserCard() {
        val coroutine = rememberCoroutineScope()
        val context = LocalContext.current

        Column(Modifier.padding(vertical = 20.dp, horizontal = 20.dp)) {
            Text(
                "欢迎来到部落格子", textAlign = TextAlign.Center,
                fontSize = TextUnit(30f, TextUnitType.Sp),
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                "马上登陆 一起happy", textAlign = TextAlign.Center,
                fontSize = TextUnit(15f, TextUnitType.Sp),
                color = Color.Gray,
            )
            Button(
                onClick = {
                    coroutine.launch {
                        UserManager.loginIfNeed()
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.color_theme)),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(text = "QQ登陆", textAlign = TextAlign.Center, color = Color.White)
            }
        }

    }

    @Composable
    fun StaggeredMediaItem(item: Feed, deleteCallback: suspend () -> Unit) {
        val coroutine = rememberCoroutineScope()
        Column(
            modifier = Modifier
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
                }
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 5.dp, start = 5.dp, end = 5.dp)
                    .clip(
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
            ) {
                // 帖子封面，如果帖子是视频，则右上角会有播放图标
                item.cover.run {
                    Image(
                        painter = rememberAsyncImagePainter(model = this),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .requiredHeight(200.dp)
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
                    IconButton(
                        onClick = {
                            coroutine.launch {
                                val result =
                                    userViewModel.removeData(item.itemId, item.author!!.userId)
                                result?.body?.run {
                                    if (this.result) {
                                        deleteCallback()
                                    } else {
                                        Toast.makeText(context, "帖子删除失败！", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                        }, modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

            }
            Row(modifier = Modifier.fillMaxWidth()) {
                item.feedsText?.run {
                    Text(
                        text = this,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black,
                        modifier = Modifier.padding(5.dp)
                    )
                }
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

    @Composable
    fun ColumnTextItem(item: Feed, deleteCallback: suspend () -> Unit) {
        val coroutine = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = colorResource(id = R.color.color_gray))
                .clickable {
                    FeedDetailActivity.startFeedDetailActivity(
                        requireActivity(), item, when (item.itemType) {
                            TYPE_VIDEO -> "video"
                            TYPE_IMAGE_TEXT -> "pics"
                            TYPE_TEXT -> "text"
                            else -> "all"
                        }, null
                    )
                }
        ) {
            // 头像和名称
            item.author?.run {
                Box(modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = item.author.avatar),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(40.dp))
                        )
                        Text(
                            text = item.author.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.Gray,
                            modifier = Modifier.offset(x = 8.dp),
                            fontSize = TextUnit(16f, TextUnitType.Sp)
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutine.launch {
                                val result =
                                    userViewModel.removeData(item.itemId, item.author!!.userId)
                                result?.body?.run {
                                    if (this.result) {
                                        deleteCallback()
                                        Toast.makeText(context, "帖子删除成功！", Toast.LENGTH_SHORT)
                                            .show()
                                    } else {
                                        Toast.makeText(context, "帖子删除失败！", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                        }, modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(30.dp)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                    }
                }
            }
            item.feedsText?.run {
                Text(
                    text = this,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 3.dp, bottom = 5.dp, start = 8.dp, end = 5.dp)
                )
            }
        }
    }

    class PullRefreshState() {
        val loginState = mutableStateOf(UserManager.isLogin())

        var authorState = mutableStateOf(Author())

        val userFeedsState = mutableStateListOf<Feed>()

        var refreshingState = mutableStateOf(false)
    }
}