package com.wutiancheng.videoapp.pluginruntime

data class NavData(
    val route: String,
    val className: String,
    val asStarter: Boolean,
    val type: NavDestination.NavType
)
