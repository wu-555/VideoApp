package com.wutiancheng.videoapp.page.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.tencent.connect.UserInfo
import com.tencent.connect.common.Constants
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.ActivityLayoutLoginBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.http.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private val viewBinding: ActivityLayoutLoginBinding by invokeViewBinding()
    private lateinit var tencent: Tencent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.actionClose.setOnClickListener { finish() }
        viewBinding.actionLogin.setOnClickListener { login() }

        // Tencent类是SDK的主要实现类，开发者可通过Tencent类访问腾讯开放的OpenAPI。
        // 其中APP_ID是分配给第三方应用的appid，类型为String。
        // Context0传入应用程序的全局context，可通过activity的getApplicationContext方法获取
        tencent = Tencent.createInstance("102104178", applicationContext)
    }


    private fun login() {
        // login(跳转到sdk的activity的activity，获得权限，登陆结果回调接口的实现类对象)
        tencent.login(this, "all", loginListener)
    }


    /*
    * SDK内部采用弱引用的方式持有接口实例，因此在调用 Tencent.login() 时传入的 IUiListener 对象，
    * 应该定义为当前类的成员变量，避免用户拉起QQ的授权登录页面后，在登录页面停留较长时间后才进行授权时，
    * SDK内部的弱引用被系统回收而导致回调没有被执行的问题。
    * */
    private val loginListener = object : LoginListener() {
        override fun onComplete(p0: Any?) {
            super.onComplete(p0)
            val response=p0 as JSONObject
            val openId=response.getString("openid")
            val accessToken=response.getString("access_token")
            // 过期时间
            val expiresIn=response.getLong("expires_in")
            tencent.openId=openId
            tencent.setAccessToken(accessToken,expiresIn.toString())

            getUserInfo()
        }
    }

    private fun getUserInfo() {
        val userInfo=UserInfo(this,tencent.qqToken)
        userInfo.getUserInfo(object :LoginListener(){
            override fun onComplete(p0: Any?) {
                super.onComplete(p0)
                val response=p0 as JSONObject
                val nickname=response.optString("nickname")
                val avatar=response.optString("figureurl_2")
                save(nickname,avatar)
            }
        })
    }

    private fun save(nickname: String, avatar: String) {
        lifecycleScope.launch{
            val apiResult=ApiService.iApiInterface.saveUser(nickname,avatar,tencent.openId,tencent.expiresIn)
            if(apiResult.success&&apiResult.body!=null){
                UserManager.save(apiResult.body!!)
                finish()
            }else{
                withContext(Dispatchers.Main){
                    Toast.makeText(this@LoginActivity,"登陆失败",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private open inner class LoginListener() : IUiListener {
        override fun onComplete(p0: Any?) {
            Toast.makeText(this@LoginActivity, "登陆完成", Toast.LENGTH_SHORT).show()
        }

        override fun onError(p0: UiError?) {
            Toast.makeText(
                this@LoginActivity,
                "登陆失败：reason${p0?.errorMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCancel() {
            Toast.makeText(
                this@LoginActivity,
                "登陆取消",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onWarning(p0: Int) {
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_LOGIN) {
            // 当登陆成功后，会回调IUiListener中的onComplete方法
            // 当登陆失败时，会回调IUiListener中的OnError或OnCancel方法
            Tencent.onActivityResultData(requestCode, resultCode, data, loginListener)

        }
    }
}