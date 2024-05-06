package com.wutiancheng.videoapp.page.publish

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.LayoutTagBottomSheetDialogBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.http.ApiService
import com.wutiancheng.videoapp.model.TagList
import com.wutiancheng.videoapp.page.login.UserManager
import com.wutiancheng.videoapp.util.PxUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private val viewBinding: LayoutTagBottomSheetDialogBinding by invokeViewBinding()
    private val mTagList= mutableListOf<TagList>()
    private var listener:OnTagItemSelectedListener?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val parent=viewBinding.root.parent as ViewGroup
        val behavior=BottomSheetBehavior.from(parent)
        // 设置折叠高度为屏幕的1/3
        behavior.peekHeight=PxUtil.getScreenHeight()/3
        // 折叠状态设置为false
        behavior.isHideable=false

        val layoutParams=parent.layoutParams
        // 展开的最大高度为屏幕的2/3
        layoutParams.height=PxUtil.getScreenHeight()/3*2
        parent.layoutParams=layoutParams

        viewBinding.recyclerView.layoutManager=LinearLayoutManager(context)

        // 查询标签集合数据
        queryTagList()

        viewBinding.recyclerView.adapter=TagsAdapter()
    }

    private fun queryTagList() {
        lifecycleScope.launch {
            kotlin.runCatching {
                val response=ApiService.iApiInterface.getTagList(UserManager.userId())
                response
            }.onSuccess {
                withContext(Dispatchers.Main){
                    mTagList.addAll(it.body!!)
                    viewBinding.recyclerView.adapter?.notifyDataSetChanged()
                }
            }.onFailure {
                withContext(Dispatchers.Main){
                    Toast.makeText(context,"获取标签失败",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class TagsAdapter:RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val textView=TextView(parent.context)
            textView.apply {
                textSize=13f
                typeface= Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(parent.context,R.color.color_000))
                gravity=Gravity.CENTER_VERTICAL
                // -1对应match_parent
                layoutParams=RecyclerView.LayoutParams(-1,PxUtil.dp2px(45))
                return object :RecyclerView.ViewHolder(textView){}
            }
        }

        override fun getItemCount(): Int {
            return mTagList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val textView=holder.itemView as TextView
            val tagList=mTagList[position]
            textView.text=tagList.title
            holder.itemView.setOnClickListener{
                listener?.onTagItemSelected(tagList)
                dismiss()
            }
        }

    }

    fun setOnTagItemSelectedListener(listener: OnTagItemSelectedListener){
        this.listener=listener
    }

    interface OnTagItemSelectedListener{
        fun onTagItemSelected(tagList:TagList)
    }
}