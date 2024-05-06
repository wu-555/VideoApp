package com.wutiancheng.videoapp.ext

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.util.AppGlobals
import com.wutiancheng.videoapp.util.PxUtil
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import org.w3c.dom.Text
import java.util.concurrent.locks.Condition

fun View.setVisibility(visibility: Boolean) {
    this.visibility = if (visibility) View.VISIBLE else View.GONE
}

fun TextView.setTextVisibility(content: String?, whenNullGone: Boolean = true) {
    if (TextUtils.isEmpty(content) && whenNullGone) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    text = content
}

fun ImageView.setImageUrl(imageUrl: String?, isCircle: Boolean = false, radius: Int = 0) {
    if (TextUtils.isEmpty(imageUrl)) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    val builder = Glide.with(this).load(imageUrl)
    if (isCircle) {
        builder.transform(CircleCrop())
    } else if (radius > 0) {
        builder.transform(RoundedCornersTransformation(PxUtil.dp2px(radius), 0))
    }
    val layoutParams = this.layoutParams
    if (layoutParams != null && layoutParams.width > 0 && layoutParams.height > 0) {
        builder.override(layoutParams.width, layoutParams.height)
    }
    builder.into(this)
}

fun ImageView.setImageResource(
    condition: Boolean,
    @DrawableRes trueRes: Int,
    @DrawableRes falseRes: Int
) {
    setImageResource(if (condition) trueRes else falseRes)
}

fun ImageView.load(imageUrl: String?, callback: (Bitmap) -> Unit) {
    Glide.with(this).asBitmap().load(imageUrl).into(object : BitmapImageViewTarget(this) {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            super.onResourceReady(resource, transition)
            callback(resource)
        }
    })
}

fun ImageView.setBlurImageUrl(blurUrl: String, radius: Int) {
    Glide.with(this).load(blurUrl).override(radius).transform(BlurTransformation()).dontAnimate()
        .into(object : DrawableImageViewTarget(this) {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                super.onResourceReady(resource, transition)
                background = resource
            }
        })
}

fun MaterialButton.setMaterialButton(
    content: String?,
    condition: Boolean,
    @DrawableRes trueRes: Int,
    @DrawableRes falseRes: Int
) {
    if(!TextUtils.isEmpty(content)){
        text=content
    }
    setIconResource(if(condition) trueRes else falseRes)
    val colorStatus=ColorStateList.valueOf(context.getColor(if(condition) R.color.color_theme else R.color.color_3d3))
    iconTint=colorStatus
    setTextColor(colorStatus)
}

fun TextView.setTextColor(condition: Boolean, @ColorRes trueRes: Int, @ColorRes falseRes: Int) {
    setTextColor(AppGlobals.getApplication().getColor(if (condition) trueRes else falseRes))
}

