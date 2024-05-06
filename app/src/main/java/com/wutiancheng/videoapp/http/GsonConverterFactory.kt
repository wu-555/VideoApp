package com.wutiancheng.videoapp.http

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


class GsonConverterFactory private constructor() : Converter.Factory() {
    companion object {
        private val gson: Gson by lazy {
            Gson()
        }

        fun create(): GsonConverterFactory {
            return GsonConverterFactory()
        }
    }


    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        // request不需要转换，因此就直接照搬GsonConverterFactory的代码
        val adapter = gson.getAdapter(TypeToken.get(type))
        return GsonRequestBodyConverter(gson, adapter)
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return GsonResponseBodyConverter<Any>(gson, type)
    }
}