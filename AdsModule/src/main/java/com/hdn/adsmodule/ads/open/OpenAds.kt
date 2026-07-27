package com.hdn.adsmodule.ads.open

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.hdn.adsmodule.ads.AdUnitParser
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsIdConfig
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.ads.inter.Callback
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog
import java.util.Date

object OpenAds {
    private val openIdDefault: List<String>
        get() = AdUnitParser.parse(
            if (AdsController.isDebug) {
                AdsIdConfig.Debug.APP_OPEN
            } else {
                AdsIdConfig.Remote.APP_OPEN
            },
            if (AdsController.isDebug) AdsIdConfig.Debug.APP_OPEN else AdsIdConfig.Release.APP_OPEN
        )

    private var appOpenAd: AppOpenAd? = null
    private var isOpenShowingAd = false
    val disableClasses: ArrayList<Class<*>> = arrayListOf()
    private var loadTimeOpenAd: Long = 0
    var flagQC: Int = 1
    private var currentAdUnitIds: List<String> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable { flagQC = 1 }

    @JvmStatic
    fun initOpenAds(
        context: Context,
        adUnitIds: List<String> = openIdDefault,
        callback: Callback?
    ) {
        currentAdUnitIds = adUnitIds.ifEmpty { openIdDefault }

        if (!AdsController.canShowAds()) {
            clear()
            callback?.invoke()
            return
        }

        if (!isCanLoadAds) {
            callback?.invoke()
            return
        }

        appOpenAd = null
        loadOpenAdByIndex(context, currentAdUnitIds, 0, callback)
    }

    private fun loadOpenAdByIndex(
        context: Context,
        ids: List<String>,
        index: Int,
        callback: Callback?
    ) {
        if (index >= ids.size) {
            appOpenAd = null
            callback?.invoke()
            return
        }
        AdsManager.onAdsLog(AdsLog("opa", ids[index], "load", "start_load", null))
        AppOpenAd.load(
            context,
            ids[index],
            getAdRequest(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    AdsManager.onAdsLog(AdsLog("opa", ids[index], "load", "load_success", null))
                    appOpenAd = ad
                    appOpenAd?.setOnPaidEventListener { adValue ->
                        AdsManager.onAdsPair(
                            AdValue(
                                adValue,
                                appOpenAd?.responseInfo?.loadedAdapterResponseInfo
                            )
                        )
                    }
                    loadTimeOpenAd = Date().time
                    callback?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AdsManager.onAdsLog(AdsLog("opa", ids[index], "load", "start_failed", loadAdError))
                    loadOpenAdByIndex(context, ids, index + 1, callback)
                }
            }
        )
    }

    private fun isOpenAdsCanUse(): Boolean {
        val dateDifference = Date().time - loadTimeOpenAd
        return dateDifference < 3600000 * 4
    }

    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    fun isCanShowOpenAds(): Boolean {
        return appOpenAd != null && !isOpenShowingAd && isOpenAdsCanUse()
    }

    private val isCanLoadAds: Boolean
        get() {
            if (!AdsController.canShowAds()) return false
            if (isOpenShowingAd) return false
            if (currentAdUnitIds.isEmpty()) return false
            return appOpenAd == null || !isOpenAdsCanUse()
        }

    fun showOpenAds(context: Activity, callback: Callback?) {
        if (!AdsController.canShowAds()) {
            callback?.invoke()
            return
        }
        if (appOpenAd == null) {
            initOpenAds(context) {}
            return
        }
        AdsManager.onAdsLog(AdsLog("opa" , "", "show", "call_show",null))
        if (flagQC == 1) {
            if (isCanShowOpenAds()) {
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        AdsManager.onAdsLog(AdsLog("opa" , "", "show", "show_failed",adError))
                        appOpenAd = null
                        callback?.invoke()
                    }

                    override fun onAdShowedFullScreenContent() {
                        AdsManager.onAdsLog(AdsLog("opa" , "", "show", "show_success",null))
                        isOpenShowingAd = true
                    }

                    override fun onAdClicked() {
                        AdsManager.onAdsLog(AdsLog("opa" , "", "adsClick", "show",null))
                    }

                    override fun onAdDismissedFullScreenContent() {
                        AdsManager.onAdsLog(AdsLog("opa" , "", "show", "show_dismiss",null))
                        isOpenShowingAd = false
                        appOpenAd = null
                        initOpenAds(context) {}
                        startDelay()
                        callback?.invoke()
                        Overlay.finish()
                    }
                }
                AdsManager.onAdsLog(AdsLog("opa" , "", "show", "start_show",null))
                appOpenAd?.show(context)
            } else {
                AdsManager.onAdsLog(AdsLog("opa" , "", "show", "err_cant_show",null))
                callback?.invoke()
            }
        } else {
            AdsManager.onAdsLog(AdsLog("opa" , "", "show", "err_delay",null))
            callback?.invoke()
        }
    }

    fun startDelay() {
        flagQC = 0
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, if (AdsController.isDebug) 0 else 60000)
    }

    @JvmStatic
    fun disableAdsOpenForActivity(activityClass: Class<*>) {
        if (!disableClasses.contains(activityClass)) {
            disableClasses.add(activityClass)
        }
    }

    fun enableAdsOpenForActivity(activityClass: Class<*>) {
        disableClasses.remove(activityClass)
    }

    private fun clear() {
        appOpenAd = null
        isOpenShowingAd = false
        loadTimeOpenAd = 0
    }
}
