package com.hdn.adsmodule.ads.open

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.hdn.adsmodule.R
import com.hdn.adsmodule.databinding.ActivityOverlayBinding


class Overlay : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overlay)
        instance = this
        val appIcon: Drawable? = applicationInfo
            .loadIcon(packageManager)
        binding.imgAvatar.setImageDrawable(appIcon)

        binding.main.post {
            OpenAds.showOpenAds(this) {
            }
        }
        binding.btnContinue.setOnClickListener {
            finish()
        }
    }

    companion object {
        var instance: Overlay? = null
        fun start(context: Context) {
            val intent = Intent(context, Overlay::class.java)
            context.startActivity(intent)
        }

        fun finish() {
            instance?.finish()
        }
    }
}