package com.wutiancheng.videoapp.ext

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewbinding.ViewBinding
import java.lang.IllegalStateException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T : ViewBinding> invokeViewBinding() =
    InflateBindingProperty(T::class.java)

inline fun <reified T : ViewModel> invokeViewModel() =
    ViewModelProperty(T::class.java)

class ViewModelProperty<T : ViewModel>(private val clz: Class<T>) :
    ReadOnlyProperty<Any, T> {
    private var viewModel: T? = null
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if(thisRef !is ViewModelStoreOwner){
            throw IllegalStateException("invokeViewModel can only be used in ViewModelStoreOwner instance")
        }
        if (viewModel == null) {
            viewModel = ViewModelProvider(thisRef,ViewModelProvider.NewInstanceFactory())[clz]
        }
        return viewModel!!
    }

}

class InflateBindingProperty<T : ViewBinding>(private val clz: Class<T>) :
    ReadOnlyProperty<Any, T> {
    private var binding: T? = null
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        var layoutInflater: LayoutInflater?=null
        var viewLifecycleOwner: LifecycleOwner?=null
        when (thisRef) {
            is AppCompatActivity -> {
                layoutInflater = thisRef.layoutInflater
                viewLifecycleOwner = thisRef
            }

            is Fragment -> {
                layoutInflater=thisRef.layoutInflater
                if(thisRef.view!=null){
                    viewLifecycleOwner=thisRef.viewLifecycleOwner
                }
            }

            is IViewBinding->{
                layoutInflater=thisRef.getLayoutInflater()
                viewLifecycleOwner=thisRef.getViewLifecycleOwner()
            }

            else -> {
                throw IllegalStateException("invokeViewBinding can only be used in AppCompatActivity , Fragment or IViewBinding")
            }
        }
        if (binding == null) {
            try {
                binding = clz.getMethod("inflate", LayoutInflater::class.java)
                    .invoke(null, layoutInflater) as T
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                throw e
            }
        }
        viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                binding = null
            }
        })
        return binding!!
    }
}

