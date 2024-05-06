package com.wutiancheng.videoapp.ext

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions

fun NavController.switchTab(route: String,args: Bundle?,navOptions: NavOptions?){
    val list=backQueue.filter {
        it.destination.id==route.hashCode()
    }
    if(list.isEmpty()){
        navigateTo(route,args,navOptions)
    }else{
        navigateBack(route,false,false)
    }
}

fun NavController.navigateTo(route:String,args:Bundle?=null,navOptions: NavOptions?=null){
    navigate(route.hashCode(),args,navOptions)
}

fun NavController.navigateBack(route: String,inclusive:Boolean=false,saveState:Boolean=false){
    popBackStack(route.hashCode(),inclusive,saveState)
}