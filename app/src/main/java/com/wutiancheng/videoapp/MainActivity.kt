package com.wutiancheng.videoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.wutiancheng.videoapp.databinding.ActivityMainBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.view.AppBottomBar
import com.wutiancheng.videoapp.navigation.NavGraphBuilder
import com.wutiancheng.videoapp.ext.switchTab
import com.wutiancheng.videoapp.page.login.UserManager
import com.wutiancheng.videoapp.util.AppConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewBinding :ActivityMainBinding by invokeViewBinding()
    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).findNavController()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        NavGraphBuilder.build(navController, this)
        val appBottomBar = findViewById<AppBottomBar>(R.id.app_bottom_bar)
        appBottomBar.setOnItemSelectedListener {
            val tab = AppConfig.getBottomConfig().tabs[it.order]?: return@setOnItemSelectedListener false
            if (tab.needLogin) {
                lifecycleScope.launch {
                    UserManager.loginIfNeed()
                    UserManager.getUser().collectLatest {author->
                        if (author.userId > 0) {
                            navController.switchTab(tab.route, null, null)
                        }
                    }
                }
                false
            }else{
                navController.switchTab(tab.route, null, null)
                !TextUtils.isEmpty(it.title)
            }

        }
    }
}