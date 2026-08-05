package com.hdn.adsmodule.ads.inter

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import com.hdn.adsmodule.R

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.hdn.adsmodule.ads.AdUnitParser
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsIdConfig
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog

import java.util.Date
import kotlin.collections.ifEmpty
import kotlin.let
import kotlin.run

typealias Callback = (() -> Unit)
// Callback show inter: true = show thành công, false = không show được
typealias InterCallback = ((Boolean) -> Unit)

object InterAds {
    private val interIdDefault: List<String>
        get() = AdUnitParser.parse(
            if (AdsController.isDebug) {
                AdsIdConfig.Debug.INTERSTITIAL
            } else {
                AdsIdConfig.Remote.INTERSTITIAL
            },
            if (AdsController.isDebug) AdsIdConfig.Debug.INTERSTITIAL else AdsIdConfig.Release.BANNER
        )

    private var currentAdUnitIds: List<String> = emptyList()
    private var mInterstitialAd: InterstitialAd? = null
    private var isLoading = false
    var isShowing: Boolean = false
    private var isCoolingDown: Boolean = false
    private var loadTimeAd: Long = 0
    private var loadingDialog: Dialog? = null
    var interAdsTime = 45000L
    @JvmStatic
    @JvmOverloads
    fun initInterAds(
        context: Context,
        adUnitIds: List<String> = interIdDefault,
        isForceReload: Boolean = false,
        onLoadSuccess: (() -> Unit)? = null,
        onLoadError: (() -> Unit)? = null,
        useWithoutVip: Boolean = false
    ) {
        currentAdUnitIds = adUnitIds.ifEmpty { interIdDefault }
        if (!AdsController.canShowAds(useWithoutVip)) {
            clear()
            onLoadSuccess?.invoke()
            return
        }

        if (!isCanLoadAds && !isForceReload) {
            onLoadError?.invoke()
            return
        }

        if (isLoading) {
            return
        }


        mInterstitialAd = null
        isLoading = true

        loadInterstitialByIndex(
            context = context.applicationContext,
            ids = currentAdUnitIds,
            index = 0,
            adRequest = AdRequest.Builder().build(),
            onLoadSuccess = onLoadSuccess,
            onLoadError = onLoadError
        )
    }

    private fun loadInterstitialByIndex(
        context: Context,
        ids: List<String>,
        index: Int,
        adRequest: AdRequest,
        onLoadSuccess: (() -> Unit)?,
        onLoadError: (() -> Unit)?
    ) {
        if (index >= ids.size) {
            mInterstitialAd = null
            isLoading = false
            onLoadError?.invoke()
            return
        }
        AdsManager.onAdsLog(AdsLog("ita", ids[index], "load", "start_load", null))
        InterstitialAd.load(
            context,
            ids[index],
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    AdsManager.onAdsLog(AdsLog("ita", ids[index], "load", "load_success", null))
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.setOnPaidEventListener { adValue ->
                        AdsManager.onAdsPair(
                            AdValue(
                                adValue,
                                mInterstitialAd?.responseInfo?.loadedAdapterResponseInfo
                            )
                        )
                    }
                    isLoading = false
                    loadTimeAd = Date().time
                    onLoadSuccess?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AdsManager.onAdsLog(AdsLog("ita", ids[index], "load", "load_failed", loadAdError))
                    loadInterstitialByIndex(
                        context = context,
                        ids = ids,
                        index = index + 1,
                        adRequest = adRequest,
                        onLoadSuccess = onLoadSuccess,
                        onLoadError = onLoadError
                    )
                }
            }
        )
    }

    private val isCanLoadAds: Boolean
        get() {
            if (isLoading || isShowing) return false
            if (currentAdUnitIds.isEmpty()) return false
            if (mInterstitialAd == null) return true
            return isAdsOverdue
        }

    val isCanShowAds: Boolean
        get() {
            if (isLoading || isShowing || isCoolingDown) return false
            if (mInterstitialAd == null) return false
            return !isAdsOverdue
        }

    val isCanForceShowAds: Boolean
        get() {
            if (isLoading || isShowing) return false
            if (mInterstitialAd == null) return false
            return !isAdsOverdue
        }

    private val isAdsOverdue: Boolean
        get() = Date().time - loadTimeAd > 3600000 * 4

    // forceShow = load-and-show: có ad thì show luôn, chưa có thì hiện loading dialog -> load -> show
    @JvmStatic
    @JvmOverloads
    fun forceShowAdsBreak(
        activity: Activity,
        useWithoutVip: Boolean = false,
        autoCache: Boolean = true,
        callback: InterCallback?
    ) {
        isCoolingDown = false
        AdsManager.onAdsLog(AdsLog("ita", "", "force_show", "call_show", null))
        if (!AdsController.canShowAds(useWithoutVip)) {
            callback?.invoke(false)
            return
        }
        if (activity !is AppCompatActivity) {
            AdsManager.onAdsLog(AdsLog("ita", "", "force_show", "err_not_activity", null))
            callback?.invoke(false)
            return
        }
        // Có ad sẵn -> show luôn, khỏi loading
        if (isCanForceShowAds) {
            showAdsFull(activity, useWithoutVip, autoCache, callback)
            return
        }
        // Đang load dở hoặc đang show -> không show chồng
        if (isLoading || isShowing) {
            AdsManager.onAdsLog(AdsLog("ita", "", "force_show", "err_busy", null))
            callback?.invoke(false)
            return
        }
        // Chưa có ad -> hiện loading dialog, load xong show luôn
        showLoading(activity)
        initInterAds(
            context = activity,
            isForceReload = true,
            onLoadSuccess = {
                dismissLoading()
                if (isCanForceShowAds) {
                    showAdsFull(activity, useWithoutVip, autoCache, callback)
                } else {
                    callback?.invoke(false)
                }
            },
            onLoadError = {
                dismissLoading()
                callback?.invoke(false)
            },
            useWithoutVip = useWithoutVip
        )
    }

    @JvmStatic
    fun showAdsBreak(activity: Activity?, useWithoutVip: Boolean, callback: InterCallback?) {
        AdsManager.onAdsLog(AdsLog("ita", "", "show", "call_show", null))
        if (isCanShowAds && activity is AppCompatActivity) {
            showAdsFull(activity, useWithoutVip, callback = callback)
        } else {
            AdsManager.onAdsLog(AdsLog("ita", "", "show", "err_call_show", null))
            activity?.let { initInterAds(context = it, useWithoutVip = useWithoutVip) }
            callback?.invoke(false)
        }
    }

    @JvmStatic
    fun showAdsBreak(activity: Activity?, callback: InterCallback?) =
        showAdsBreak(activity, false, callback)

    private fun showAdsFull(
        context: AppCompatActivity,
        useWithoutVip: Boolean,
        autoCache: Boolean = true,
        callback: InterCallback?
    ) {
        if (!AdsController.canShowAds(useWithoutVip)) {
            callback?.invoke(false)
            return
        }

        val currentAd = mInterstitialAd ?: run {
            AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_failed_ad_null", null))
            callback?.invoke(false)
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_failed", adError))
                mInterstitialAd = null
                isShowing = false
                if (autoCache) initInterAds(context = context, useWithoutVip = useWithoutVip)
                callback?.invoke(false)
            }

            override fun onAdShowedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_success", null))
                isShowing = true
            }

            override fun onAdClicked() {
                AdsManager.onAdsLog(AdsLog("ita", "", "adsClick", "show_ads_full", null))
            }

            override fun onAdDismissedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_dismiss", null))
                isShowing = false
                mInterstitialAd = null
                startDelay()
                if (autoCache) initInterAds(context = context, useWithoutVip = useWithoutVip)
                callback?.invoke(true)
            }
        }

        currentAd.show(context)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val resetCooldownRunnable = Runnable {
        isCoolingDown = false
    }

    fun startDelay() {
        isCoolingDown = true
        handler.removeCallbacks(resetCooldownRunnable)
        handler.postDelayed(resetCooldownRunnable, interAdsTime)
    }

    private fun showLoading(activity: Activity) {
        if (loadingDialog?.isShowing == true) return
        loadingDialog = Dialog(activity, R.style.AppTheme_FullScreenDialog).apply {
            setContentView(R.layout.dialog_loading_inter)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setCancelable(false)
            show()
        }
    }

    private fun dismissLoading() {
        try {
            loadingDialog?.dismiss()
        } catch (_: Exception) {
        }
        loadingDialog = null
    }

    private fun clear() {
        mInterstitialAd = null
        isLoading = false
        isShowing = false
        isCoolingDown = false
        loadTimeAd = 0
    }
}
