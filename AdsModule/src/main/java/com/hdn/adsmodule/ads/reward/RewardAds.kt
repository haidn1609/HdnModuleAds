package com.hdn.adsmodule.ads.reward

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
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

    private var loadingDialog: Dialog? = null
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
        callback: RewardCallback
    ) {
        show(activity, callback, useInterFallback = true, useWithoutVip = useWithoutVip)
    }

    fun show(activity: Activity, callback: RewardCallback, useWithoutVip: Boolean) {
        show(activity, callback, useInterFallback = false, useWithoutVip = useWithoutVip)
    }

    private fun show(
        activity: Activity,
        callback: RewardCallback,
        useInterFallback: Boolean,
        useWithoutVip: Boolean = false
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
                AdsManager.onAdsLog(AdsLog("rwa", "", "show", "err_showing", null))
                callback.onAdFailed()
            }
            return
        }

        currentAdUnitIds = rewardIdDefault
        val ad = rewardedAd
        if (ad != null) {
            showInternal(activity, ad, callback, useInterFallback, useWithoutVip)
        } else {
            loadAndShow(activity, callback, useInterFallback, useWithoutVip)
        }
    }

    private fun loadAndShow(
        activity: Activity,
        callback: RewardCallback,
        useWithoutVip: Boolean = false,
        useInterFallback: Boolean
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
        showLoading(activity)

        loadRewardedAd(
            activity = activity,
            ids = currentAdUnitIds,
            index = 0,
            onLoaded = { ad ->
                dismissLoading()
                showInternal(activity, ad, callback, useInterFallback, useWithoutVip)
            },
            onFailed = {
                dismissLoading()
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
        AdsManager.onAdsLog(AdsLog("rwa", ids[index], "load", "start_load", null))
        RewardedAd.load(
            activity,
            ids[index],
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    AdsManager.onAdsLog(AdsLog("rwa", ids[index], "load", "load_success", null))
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
                    AdsManager.onAdsLog(AdsLog("rwa", ids[index], "load", "load_failed", error))
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
        useWithoutVip: Boolean = false
    ) {
        hasEarnedReward = false
        if (!AdsController.adsEnable || (AdsController.isVip && !useWithoutVip)) {
            showAdUnavailableToast(activity)
            callback.onAdFailed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdsManager.onAdsLog(AdsLog("rwa", "", "show", "start_show", null))
                isShowing = true
                callback.onAdShowed()
            }

            override fun onAdClicked() {
                AdsManager.onAdsLog(AdsLog("rwa", "", "adsClick", "show", null))
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdsManager.onAdsLog(AdsLog("rwa", "", "show", "show_failed", adError))
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
                AdsManager.onAdsLog(AdsLog("rwa", "", "show", "show_dismiss", null))
                InterAds.startDelay()
                rewardedAd = null
                isShowing = false

                if (hasEarnedReward) {
                    callback.onRewardEarned()
                }

                callback.onAdClosed()
                preload(activity)
            }
        }
        AdsManager.onAdsLog(AdsLog("rwa", "", "show", "call_show", null))
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
        AdsManager.onAdsLog(AdsLog("rwa", "", "show_ita_fall_back", "call_show", null))
        InterAds.forceShowAdsBreak(activity, useWithoutVip) {
            if (it) {
                callback.onRewardEarned()
            } else {
                callback.onAdFailed()
            }
            callback.onAdClosed()
        }
    }

    private fun showLoading(activity: Activity) {
        if (loadingDialog?.isShowing == true) return

        loadingDialog = Dialog(activity, R.style.AppTheme_FullScreenDialog).apply {
            setContentView(R.layout.dialog_loading_reward)
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

    interface RewardCallback {
        fun onAdShowed()
        fun onAdClosed()
        fun onRewardEarned()
        fun onAdFailed()
        fun onPremium()
    }
}
