package com.hdn.adsmodule.base.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.hdn.adsmodule.R

// Loading dialog dùng chung cho luồng load-and-show của các loại ads.
// ponytail: 1 instance tĩnh tại 1 thời điểm (các ad không show chồng nhau).
class LoadingDialog : BaseDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_loading_ad, container, false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    companion object {
        private const val TAG = "ads_loading"
        private var current: LoadingDialog? = null

        // activity phải là AppCompatActivity (cần supportFragmentManager). Không phải -> bỏ qua.
        fun show(activity: Activity) {
            val fm = (activity as? AppCompatActivity)?.supportFragmentManager ?: return
            if (current?.isAdded == true) return
            current = LoadingDialog().also { runCatching { it.show(fm, TAG) } }
        }

        fun dismiss() {
            runCatching { current?.dismissAllowingStateLoss() }
            current = null
        }
    }
}
