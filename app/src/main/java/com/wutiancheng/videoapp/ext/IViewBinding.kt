package com.wutiancheng.videoapp.ext

import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.wutiancheng.videoapp.model.Feed

interface IViewBinding {
    fun getLayoutInflater():LayoutInflater

    fun getViewLifecycleOwner():LifecycleOwner
}