package com.wutiancheng.videoapp.http

import com.google.gson.Gson
import com.google.gson.JsonElement
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Converter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class GsonResponseBodyConverter<T>(private val gson: Gson, private val type: Type) :
    Converter<ResponseBody, ApiResult<T>> {
    override fun convert(value: ResponseBody): ApiResult<T>? {
        value.use {
            // ParameterizedType是参数化类型，具体指带有类型参数的类型，如List<String>这种泛型
            // 这里进行类型判断，要求接口返回值类型是ApiResult的子类
            if (type !is ParameterizedType
                || !ApiResult::class.java.isAssignableFrom(type.rawType as Class<*>)
            ) {
                throw RuntimeException("The return type of the method must be ApiResut")
            }

            val apiResult: ApiResult<T> = ApiResult()

            // 使用jsonObject来解析json文件
            val jsonObject = JSONObject(it.string())

            // optInt()：当key对应的value为null时会返回一个默认值
            // getInt()：当key对应的value为null时会抛出一个异常
            apiResult.status = jsonObject.optInt("status")
            apiResult.errMsg = jsonObject.getString("message")

            val data1: JSONObject? = jsonObject.optJSONObject("data")
            data1?.let {
                val data2 = data1.getString("data")
                data2?.let {
                    // 将T类型参数化
                    val parameterizedType = type as ParameterizedType
                    val argumentType = parameterizedType.actualTypeArguments[0]
                    kotlin.runCatching {
                        apiResult.body = gson.fromJson(data2, argumentType)
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
            return apiResult
        }
    }
}