package com.hdn.adsmodule.ads.reward

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.hdn.adsmodule.R
import com.hdn.adsmodule.ads.AdUnitParser
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsIdConfig
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.ads.inter.InterAds
import com.hdn.adsmodule.base.ui.LoadingDialog
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog

object RewardAds {
    private val rewardIdDefault: List<String>
        get() = AdUnitParser.parse(
            if (AdsController.isDebug) {
                AdsIdConfig.Debug.REWARDED
            } else {
                AdsIdConfig.Remote.REWARDED
            },
            if (AdsController.isDebug) AdsIdConfig.Debug.REWARDED else AdsIdConfig.Release.REWARDED
        )

    private var currentAdUnitIds: List<String> = emptyList()
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    var isShowing = false
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var hasEarnedReward = false
    private fun showAdUnavailableToast(activity: Activity) {
        Toast.makeText(activity, activity.getString(R.string.ad_unavailable), Toast.LENGTH_SHORT)
            .show()
    }

    fun preload(context: Activity) {
        currentAdUnitIds = rewardIdDefault
        if (rewardedAd != null || isLoading) return

        isLoading = true
        loadRewardedAd(
            activity = context,
            ids = currentAdUnitIds,
            index = 0,
            onLoaded = {},
            onFailed = {
                rewardedAd = null
                isLoading = false
            }
        )
    }

    fun showRewardWithFallbackInter(
        activity: Activity,
        useWithoutVip: Boolean = false,
        autoCache: Boolean = true,
        fakeLoadingTime: Long = 0L,
        callback: RewardCallback
    ) {
        show(activity, callback, useInterFallback = true, useWithoutVip = useWithoutVip, autoCache = autoCache, fakeLoadingTime = fakeLoadingTime)
    }

    fun show(activity: Activity, callback: RewardCallback, useWithoutVip: Boolean, autoCache: Boolean = true, fakeLoadingTime: Long = 0L) {
        show(activity, callback, useInterFallback = false, useWithoutVip = useWithoutVip, autoCache = autoCache, fakeLoadingTime = fakeLoadingTime)
    }

    private fun show(
        activity: Activity,
        callback: RewardCallback,
        useInterFallback: Boolean,
        useWithoutVip: Boolean = false,
        autoCache: Boolean = true,
        fakeLoadingTime: Long = 0L
    ) {
        if (!AdsController.adsEnable || (AdsController.isVip && !useWithoutVip)) {
            callback.onPremium()
            return
        }
        if (isShowing) {
            showAdUnavailableToast(activity)
            if (useInterFallback) {
                showInterFallback(activity, useWithoutVip, callback)
            } else {
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.SHOW, AdsLog.Mess.ERR_SHOWING, null))
                callback.onAdFailed()
            }
            return
        }

        currentAdUnitIds = rewardIdDefault

        // Fake loading: bắn loading -> chờ fake time (tranh thủ load) -> show nếu ad sẵn sàng
        if (fakeLoadingTime > 0) {
            LoadingDialog.show(activity)
            if (rewardedAd == null) preload(activity)
            handler.postDelayed({
                LoadingDialog.dismiss()
                val ad = rewardedAd
                if (ad != null) {
                    showInternal(activity, ad, callback, useInterFallback, useWithoutVip, autoCache)
                } else {
                    showAdUnavailableToast(activity)
                    if (useInterFallback) {
                        showInterFallback(activity, useWithoutVip, callback)
                    } else {
                        callback.onAdFailed()
                    }
                }
            }, fakeLoadingTime)
            return
        }

        val ad = rewardedAd
        if (ad != null) {
            showInternal(activity, ad, callback, useInterFallback, useWithoutVip, autoCache)
        } else {
            loadAndShow(activity, callback, useInterFallback, useWithoutVip, autoCache)
        }
    }

    private fun loadAndShow(
        activity: Activity,
        callback: RewardCallback,
        useWithoutVip: Boolean = false,
        useInterFallback: Boolean,
        autoCache: Boolean = true
    ) {
        if (!AdsController.adsEnable || (AdsController.isVip && !useWithoutVip)) {
            callback.onPremium()
            return
        }
        if (isLoading) {
            Toast.makeText(activity, activity.getString(R.string.ad_is_loading), Toast.LENGTH_SHORT)
                .show()
            return
        }

        isLoading = true
        LoadingDialog.show(activity)

        loadRewardedAd(
            activity = activity,
            ids = currentAdUnitIds,
            index = 0,
            onLoaded = { ad ->
                LoadingDialog.dismiss()
                showInternal(activity, ad, callback, useInterFallback, useWithoutVip, autoCache)
            },
            onFailed = {
                LoadingDialog.dismiss()
                rewardedAd = null
                isLoading = false
                showAdUnavailableToast(activity)
                if (useInterFallback) {
                    showInterFallback(activity, useWithoutVip, callback)
                } else {
                    callback.onAdFailed()
                }
            }
        )
    }

    private fun loadRewardedAd(
        activity: Activity,
        ids: List<String>,
        index: Int,
        onLoaded: (RewardedAd) -> Unit,
        onFailed: () -> Unit
    ) {
        if (index >= ids.size) {
            rewardedAd = null
            isLoading = false
            onFailed()
            return
        }
        AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, ids[index], AdsLog.Action.LOAD, AdsLog.Mess.START_LOAD, null))
        RewardedAd.load(
            activity,
            ids[index],
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, ids[index], AdsLog.Action.LOAD, AdsLog.Mess.LOAD_SUCCESS, null))
                    rewardedAd = ad
                    isLoading = false
                    ad.setOnPaidEventListener {
                        AdsManager.onAdsPair(
                            AdValue(
                                it,
                                ad.responseInfo.loadedAdapterResponseInfo
                            )
                        )
                    }
                    onLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, ids[index], AdsLog.Action.LOAD, AdsLog.Mess.LOAD_FAILED, error))
                    loadRewardedAd(activity, ids, index + 1, onLoaded, onFailed)
                }
            }
        )
    }

    private fun showInternal(
        activity: Activity,
        ad: RewardedAd,
        callback: RewardCallback,
        useInterFallback: Boolean,
        useWithoutVip: Boolean = false,
        autoCache: Boolean = true
    ) {
        hasEarnedReward = false
        if (!AdsController.adsEnable || (AdsController.isVip && !useWithoutVip)) {
            showAdUnavailableToast(activity)
            callback.onAdFailed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.SHOW, AdsLog.Mess.START_SHOW, null))
                isShowing = true
                callback.onAdShowed()
            }

            override fun onAdClicked() {
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.ADS_CLICK, AdsLog.Action.SHOW, null))
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.SHOW, AdsLog.Mess.SHOW_FAILED, adError))
                rewardedAd = null
                isShowing = false
                showAdUnavailableToast(activity)
                if (useInterFallback) {
                    showInterFallback(activity, useWithoutVip, callback)
                } else {
                    callback.onAdFailed()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.SHOW, AdsLog.Mess.SHOW_DISMISS, null))
                InterAds.startDelay()
                rewardedAd = null
                isShowing = false

                if (hasEarnedReward) {
                    callback.onRewardEarned()
                }

                callback.onAdClosed()
                if (autoCache) preload(activity)
            }
        }
        AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.SHOW, AdsLog.Mess.CALL_SHOW, null))
        ad.show(activity) {
            hasEarnedReward = true
        }
    }

    private fun showInterFallback(
        activity: Activity,
        useWithoutVip: Boolean,
        callback: RewardCallback
    ) {
        callback.onAdShowed()
        AdsManager.onAdsLog(AdsLog(AdsLog.Type.REWARD, "", AdsLog.Action.SHOW_ITA_FALLBACK, AdsLog.Mess.CALL_SHOW, null))
        InterAds.forceShowAdsBreak(activity, useWithoutVip) {
            if (it) {
                callback.onRewardEarned()
            } else {
                callback.onAdFailed()
            }
            callback.onAdClosed()
        }
    }

    interface RewardCallback {
        fun onAdShowed()
        fun onAdClosed()
        fun onRewardEarned()
        fun onAdFailed()
        fun onPremium()
    }
}
