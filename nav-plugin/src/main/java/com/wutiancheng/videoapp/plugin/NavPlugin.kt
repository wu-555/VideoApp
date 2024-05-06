package com.wutiancheng.videoapp.plugin

import com.android.build.gradle.BaseExtension
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin

class NavPlugin: Plugin<Project>{
    override fun apply(p0: Project) {
        print("NavPlugin apply...")

        // 判断插件是否应用于app module
        // app module会包含"application"插件，因此可以通过判断是否有这个插件来判断是否属于app module
        val appPlugin=p0.plugins.findPlugin(ApplicationPlugin::class.java)
        assert(appPlugin==null){
            GradleException("NavPlugin can only be applied to app module")
        }

        // 注册自定义插件
        val extensions=p0.extensions.findByType(BaseExtension::class.java)
        extensions?.registerTransform(NavTransform(p0))
    }
}