package com.hdn.adsmodule.ads

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.hdn.adsmodule.ads.banner.BannerAds
import com.hdn.adsmodule.ads.inter.Callback
import com.hdn.adsmodule.ads.inter.InterAds
import com.hdn.adsmodule.ads.inter.InterCallback
import com.hdn.adsmodule.ads.inter.InterSplashAds
import com.hdn.adsmodule.ads.nativeAds.NativeAds
import com.hdn.adsmodule.ads.open.OpenAds
import com.hdn.adsmodule.ads.reward.RewardAds
import com.hdn.adsmodule.ads.reward.RewardAds.RewardCallback
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog
import com.hdn.adsmodule.model._enum.BannerType

object AdsManager {
    @JvmStatic
    var adsPair: ((AdValue) -> Unit)? = null

    @JvmStatic
    var adsLog: ((AdsLog) -> Unit)? = null

    // Bắn trạng thái loading (load-and-show) ra ngoài: true = hiện loading, false = ẩn. App tự show UI.
    @JvmStatic
    var adsLoading: ((Boolean) -> Unit)? = null

    fun onAdsPair(adValue: AdValue) {
        adsPair?.invoke(adValue)
    }

    fun onAdsLog(adValue: AdsLog) {
        adsLog?.invoke(adValue)
    }

    fun onAdsLoading(isLoading: Boolean) {
        adsLoading?.invoke(isLoading)
    }

    //config
    @JvmStatic
    fun setDebug(isDebug: Boolean) {
        AdsController.isDebug = isDebug
    }

    @JvmStatic
    fun setEnabled(adsEnable: Boolean) {
        AdsController.adsEnable = adsEnable
    }

    @JvmStatic
    fun setVip(isVip: Boolean) {
        AdsController.isVip = isVip
    }

    // Thời gian cooldown giữa 2 lần show inter (ms)
    @JvmStatic
    fun setInterAdsTime(timeMillis: Long) {
        InterAds.setInterAdsTime(timeMillis)
    }

    //init
    @JvmStatic
    fun setOpaIdRl(id: String) {
        AdsIdConfig.Release.APP_OPEN = id
    }

    @JvmStatic
    fun setOpaIdRm(id: String) {
        AdsIdConfig.Remote.APP_OPEN = id
    }

    @JvmStatic
    fun setBnaIdRl(id: String) {
        AdsIdConfig.Release.BANNER = id
    }

    @JvmStatic
    fun setBnaIdRm(id: String) {
        AdsIdConfig.Remote.BANNER = id
    }

    @JvmStatic
    fun setBncaIdRl(id: String) {
        AdsIdConfig.Release.BANNER_COLLAPSIBLE = id
    }

    @JvmStatic
    fun setBncaIdRm(id: String) {
        AdsIdConfig.Remote.BANNER_COLLAPSIBLE = id
    }

    @JvmStatic
    fun setItaIdRl(id: String) {
        AdsIdConfig.Release.INTERSTITIAL = id
    }

    @JvmStatic
    fun setItaIdRm(id: String) {
        AdsIdConfig.Remote.INTERSTITIAL = id
    }

    @JvmStatic
    fun setItsaIdRl(id: String) {
        AdsIdConfig.Release.INTERSTITIAL_SPLASH = id
    }

    @JvmStatic
    fun setItsaIdRm(id: String) {
        AdsIdConfig.Remote.INTERSTITIAL_SPLASH = id
    }

    @JvmStatic
    fun setRwaIdRl(id: String) {
        AdsIdConfig.Release.REWARDED = id
    }

    @JvmStatic
    fun setRwaIdRm(id: String) {
        AdsIdConfig.Remote.REWARDED = id
    }

    @JvmStatic
    fun addNative(key: String, rlId: String, rmId: String) {
        NativeAds.addNative(key, rlId, rmId)
    }

    //    open
    @JvmStatic
    fun showOpenAds(activity: Activity, callBack: Callback?) {
        OpenAds.showOpenAds(activity, callBack)
    }

    //    banner
    @JvmStatic
    fun showBannerAds(activity: Activity, type: BannerType, container: ViewGroup) {
        BannerAds.createBannerView(activity, type) {
            container.removeAllViews()
            container.addView(it)
        }
    }

    //    inter
    @JvmStatic
    fun showInterSplashAds(
        activity: AppCompatActivity,
        startCallback: Callback?,
        doneCallBack: Callback?
    ) {
        InterSplashAds.showAdsBreak(activity, false, 0, "", startCallback, doneCallBack)
    }

    @JvmStatic
    fun showInterSplashAds(
        activity: AppCompatActivity,
        dialogRes: Int,
        nativeKey: String,
        startCallback: Callback?,
        doneCallBack: Callback?
    ) {
        InterSplashAds.showAdsBreak(
            activity,
            true,
            dialogRes,
            nativeKey,
            startCallback,
            doneCallBack
        )
    }

    @JvmStatic
    @JvmOverloads
    fun showInterAds(
        activity: AppCompatActivity,
        useWithoutVip: Boolean = false,
        fakeLoadingTime: Long = 0L,
        callback: InterCallback?
    ) {
        InterAds.showAdsBreak(activity, useWithoutVip, fakeLoadingTime, callback)
    }

    // forceShow = load-and-show. Loading bắn ra ngoài qua AdLoading.onLoading.
    // fakeLoadingTime>0 (ms) để fake thời gian loading trước khi show.
    @JvmStatic
    @JvmOverloads
    fun forceShowInterAds(
        activity: AppCompatActivity,
        useWithoutVip: Boolean = false,
        autoCache: Boolean = true,
        fakeLoadingTime: Long = 0L,
        callback: InterCallback?
    ) {
        InterAds.forceShowAdsBreak(activity, useWithoutVip, autoCache, fakeLoadingTime, callback)
    }

    //    reward
    @JvmStatic
    @JvmOverloads
    fun showRewardAds(
        activity: Activity,
        useInterFallback: Boolean = false,
        useWithoutVip: Boolean = false,
        autoCache: Boolean = true,
        fakeLoadingTime: Long = 0L,
        callback: RewardCallback
    ) {
        if (useInterFallback) {
            RewardAds.showRewardWithFallbackInter(activity, useWithoutVip, autoCache, fakeLoadingTime, callback)
        } else {
            RewardAds.show(activity, callback, useWithoutVip, autoCache, fakeLoadingTime)
        }
    }

    //    native
    @JvmStatic
    fun preloadNative(
        activity: AppCompatActivity,
        key: String,
        cacheSize: Int = 1
    ) {
        NativeAds.preload(activity, key, cacheSize)
    }

    @JvmStatic
    fun showNative(
        activity: Activity,
        key: String,
        resId: Int,
        container: FrameLayout,
        loadIfMissing: Boolean = false,
        cacheSize: Int = 1
    ): Boolean {
        return NativeAds.show(activity, key, resId, container, loadIfMissing, cacheSize)
    }

    @JvmStatic
    fun loadAndShowNative(
        activity: Activity,
        key: String,
        resId: Int,
        container: FrameLayout,
        loadIfMissing: Boolean = false,
        cacheSize: Int = 1
    ): Boolean {
        return NativeAds.loadAndShow(activity, key, resId, container, loadIfMissing, cacheSize)
    }
}