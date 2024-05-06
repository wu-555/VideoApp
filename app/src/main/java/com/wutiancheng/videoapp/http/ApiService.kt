package com.wutiancheng.videoapp.http

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create

object ApiService {
    // HttpLoggingInterceptor能够把request body 和 response body打印出来
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    // 这里的ip用本地的ip地址，不能用localhost
    private val retrofit=Retrofit.Builder()
        .baseUrl("http://8.136.122.222/jetpack/")
        //.baseUrl("http://10.20.246.229:8082/jetpack/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val iApiInterface=retrofit.create<IApiInterface>()
}