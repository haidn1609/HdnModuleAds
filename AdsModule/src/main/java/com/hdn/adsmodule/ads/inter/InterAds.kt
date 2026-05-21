package com.hdn.adsmodule.ads.inter

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

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
    var interAdsTime = 45000L
    @JvmStatic
    fun initInterAds(
        context: Context,
        adUnitIds: List<String> = interIdDefault,
        isForceReload: Boolean = false,
        onLoadSuccess: (() -> Unit)? = null,
        onLoadError: (() -> Unit)? = null
    ) {
        currentAdUnitIds = adUnitIds.ifEmpty { interIdDefault }
        if (!AdsController.canShowAds()) {
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

    @JvmStatic
    fun forceShowAdsBreak(activity: Activity, callback: Callback?) {
        isCoolingDown = false
        AdsManager.onAdsLog(AdsLog("ita", "", "force_show", "call_show", null))
        if (!isCanForceShowAds) {
            AdsManager.onAdsLog(AdsLog("ita", "", "force_show", "err_cant_show", null))
            initInterAds(activity)
            callback?.invoke()
            return
        }

        if (activity is AppCompatActivity) {
            showAdsFull(activity, callback)
        } else {
            AdsManager.onAdsLog(AdsLog("ita", "", "force_show", "err_not_activity", null))
            callback?.invoke()
        }
    }
    @JvmStatic
    fun showAdsBreak(activity: Activity?, callback: Callback?) {
        AdsManager.onAdsLog(AdsLog("ita", "", "show", "call_show", null))
        if (isCanShowAds && activity is AppCompatActivity) {
            showAdsFull(activity, callback)
        } else {
            AdsManager.onAdsLog(AdsLog("ita", "", "show", "err_call_show", null))
            activity?.let { initInterAds(it) }
            callback?.invoke()
        }
    }

    private fun showAdsFull(context: AppCompatActivity, callback: Callback?) {
        if (!AdsController.canShowAds()) {
            callback?.invoke()
            return
        }

        val currentAd = mInterstitialAd ?: run {
            AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_failed_ad_null", null))
            callback?.invoke()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_failed", adError))
                mInterstitialAd = null
                isShowing = false
                initInterAds(context)
                callback?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_success", null))
                isShowing = true
            }

            override fun onAdDismissedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog("ita", "", "show_ads_full", "show_dismiss", null))
                isShowing = false
                mInterstitialAd = null
                startDelay()
                initInterAds(context)
                callback?.invoke()
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

    private fun clear() {
        mInterstitialAd = null
        isLoading = false
        isShowing = false
        isCoolingDown = false
        loadTimeAd = 0
    }
}
