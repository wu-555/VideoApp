package com.wutiancheng.videoapp.pluginruntime

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class NavDestination(val type: NavType, val route:String, val asStarter:Boolean=false) {
    public enum class NavType{
        Fragment,
        Activity,
        Dialog,
        None
    }
}