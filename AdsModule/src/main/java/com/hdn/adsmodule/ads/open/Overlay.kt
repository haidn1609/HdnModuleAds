package com.hdn.adsmodule.ads.open

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.hdn.adsmodule.R
import com.hdn.adsmodule.ads.AdsController


class Overlay : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ponytail: không dùng binding -> app tạo layout cùng tên để ghi đè mà không ClassCastException.
        // View tìm theo id và đều optional, layout của app thiếu id nào thì bỏ qua cái đó
        setContentView(R.layout.activity_overlay)
        instance = this
        val appIcon: Drawable? = applicationInfo
            .loadIcon(packageManager)
        findViewById<ImageView?>(R.id.imgAvatar)?.setImageDrawable(appIcon)

        window.decorView.post {
            // show fail / dismiss -> đóng overlay, không để kẹt màn
            OpenAds.showOpenAds(this) { finish() }
        }
        findViewById<View?>(R.id.btnContinue)?.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    companion object {
        var instance: Overlay? = null
        fun start(context: Context) {
            // Overlay đang hiện -> không mở đè thêm
            if (instance != null) return
            // VIP / tắt ads / chưa có ad / đang delay -> không mở overlay, tranh thủ load cho lần sau
            if (!AdsController.canShowAds() || OpenAds.flagQC != 1 || !OpenAds.isCanShowOpenAds()) {
                OpenAds.initOpenAds(context) {}
                return
            }
            val intent = Intent(context, Overlay::class.java)
            context.startActivity(intent)
        }

        fun finish() {
            instance?.finish()
        }
    }
}