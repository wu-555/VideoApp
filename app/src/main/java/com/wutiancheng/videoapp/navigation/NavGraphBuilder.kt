package com.wutiancheng.videoapp.navigation

import android.content.ComponentName
import android.content.Context
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.FragmentNavigator
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import com.wutiancheng.videoapp.pluginruntime.NavRegister
import java.lang.IllegalStateException

object NavGraphBuilder {
    fun build(navController: NavController, context: Context) {
        val navProvider = navController.navigatorProvider
        val navGraphNavigator = navProvider.getNavigator<NavGraphNavigator>("navigation")
        val navGraph = NavGraph(navGraphNavigator)
        val iterator= NavRegister.get().iterator()

        while(iterator.hasNext()){
            val node=iterator.next()
            when(node.type){
                NavDestination.NavType.Fragment->{
                    val navigator=navProvider.getNavigator<FragmentNavigator>(node.type.name.toLowerCase())
                    val destination=navigator.createDestination()
                    destination.id=node.route.hashCode()
                    destination.setClassName(node.className)
                    navGraph.addDestination(destination)
                }
                NavDestination.NavType.Activity->{
                    val navigator=navProvider.getNavigator<ActivityNavigator>(node.type.name.toLowerCase())
                    val destination=navigator.createDestination()
                    destination.id=node.route.hashCode()
                    destination.setComponentName(ComponentName(context.packageName,node.className))
                    navGraph.addDestination(destination)
                }
                NavDestination.NavType.Dialog->{
                    val navigator=navProvider.getNavigator<DialogFragmentNavigator>(node.type.name.toLowerCase())
                    val destination=navigator.createDestination()
                    destination.id=node.route.hashCode()
                    destination.setClassName(node.className)
                    navGraph.addDestination(destination)
                }
                else -> throw IllegalStateException("cannot build NavGraph,because unknown destination type")
            }
            if(node.asStarter){
                navGraph.setStartDestination(node.route.hashCode())
            }
        }
        navController.setGraph(navGraph,null)
    }
}