package com.hdn.adsmodule.ads

import android.app.Activity
import android.app.NativeActivity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.hdn.adsmodule.ads.banner.BannerAds
import com.hdn.adsmodule.ads.inter.Callback
import com.hdn.adsmodule.ads.inter.InterAds
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
    fun onAdsPair(adValue: AdValue) {
        adsPair?.invoke(adValue)
    }

    fun onAdsLog(adValue: AdsLog) {
        adsLog?.invoke(adValue)
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
    fun showInterAds(activity: AppCompatActivity, callback: Callback?) {
        InterAds.showAdsBreak(activity, callback)
    }

    @JvmStatic
    fun forceShowInterAds(activity: AppCompatActivity, callback: Callback?) {
        InterAds.forceShowAdsBreak(activity, callback)
    }

    //    reward
    @JvmStatic
    fun showRewardAds(activity: Activity, callback: RewardCallback) {
        RewardAds.showRewardWithFallbackInter(activity, callback)
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