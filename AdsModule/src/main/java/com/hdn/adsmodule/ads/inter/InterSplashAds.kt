package com.hdn.adsmodule.ads.inter

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.hdn.adsmodule.R
import com.hdn.adsmodule.ads.AdUnitParser
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsIdConfig
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.ads.fullDialog.FullScreenDialog
import com.hdn.adsmodule.base.ui.LoadingDialog
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog
import kotlin.collections.ifEmpty
import kotlin.collections.isNotEmpty
import kotlin.run

object InterSplashAds {
    private val interIdDefault: List<String>
        get() = AdUnitParser.parse(
            if (AdsController.isDebug) {
                AdsIdConfig.Debug.INTERSTITIAL
            } else {
                AdsIdConfig.Remote.INTERSTITIAL_SPLASH
            },
            if (AdsController.isDebug) AdsIdConfig.Debug.INTERSTITIAL else AdsIdConfig.Release.INTERSTITIAL_SPLASH
        )

    var mInterstitialAd: InterstitialAd? = null
        private set

    private var isLoading = false
    private var loadTimeAd: Long = 0
    private var currentAdUnitIds: List<String> = emptyList()

    @JvmStatic
    fun initInterAds(
        ac: Context,
        adUnitIds: List<String> = interIdDefault,
        callback: Callback?
    ) {
        currentAdUnitIds = adUnitIds.ifEmpty { interIdDefault }

        if (!AdsController.canShowAds()) {
            clear()
            callback?.invoke()
            return
        }

        if (!isCanLoadAds) {
            callback?.invoke()
            return
        }

        mInterstitialAd = null
        isLoading = true
        loadInterstitialByIndex(ac, currentAdUnitIds, 0, callback)
    }

    private fun loadInterstitialByIndex(
        context: Context,
        ids: List<String>,
        index: Int,
        callback: Callback?
    ) {
        if (index >= ids.size) {
            clear()
            callback?.invoke()
            return
        }
        val adUnitId = ids[index]
        AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, adUnitId, AdsLog.Action.LOAD, AdsLog.Mess.START_LOAD, null))
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, adUnitId, AdsLog.Action.LOAD, AdsLog.Mess.LOAD_SUCCESS, null))
                    mInterstitialAd = interstitialAd
                    isLoading = false
                    loadTimeAd = System.currentTimeMillis()
                    mInterstitialAd?.setOnPaidEventListener { adValue ->
                        AdsManager.onAdsPair(
                            AdValue(
                                adValue,
                                mInterstitialAd?.responseInfo?.loadedAdapterResponseInfo
                            )
                        )
                    }
                    callback?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    AdsManager.onAdsLog(
                        AdsLog(
                            AdsLog.Type.INTER_SPLASH,
                            adUnitId,
                            AdsLog.Action.LOAD,
                            AdsLog.Mess.LOAD_FAILED,
                            loadAdError
                        )
                    )
                    loadInterstitialByIndex(context, ids, index + 1, callback)
                }
            }
        )
    }

    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    val isCanShowAds: Boolean
        get() = mInterstitialAd != null && !isAdsOverdue

    private val isCanLoadAds: Boolean
        get() = !isLoading && currentAdUnitIds.isNotEmpty() && (mInterstitialAd == null || isAdsOverdue)

    private val isAdsOverdue: Boolean
        get() = System.currentTimeMillis() - loadTimeAd > 4 * 60 * 60 * 1000L

    @JvmStatic
    fun Activity.canShowInter(): Boolean {
        if (isFinishing || isDestroyed) return false
        return (this as? LifecycleOwner)
            ?.lifecycle
            ?.currentState
            ?.isAtLeast(Lifecycle.State.RESUMED)
            ?: true
    }

    @JvmStatic
    @JvmOverloads
    fun showAdsBreak(
        activity: AppCompatActivity,
        showDialog: Boolean,
        dialogRes: Int,
        nativeKey: String,
        startCallback: Callback?,
        doneCallBack: Callback?,
        fakeLoadingTime: Long = 0L
    ) {
        if (!AdsController.canShowAds()) {
            doneCallBack?.invoke()
            return
        }

        AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW, AdsLog.Mess.CALL_SHOW, null))

        val enableDialog = showDialog && nativeKey.isNotEmpty()
        val layoutRes = if (enableDialog) {
            if (dialogRes != 0) dialogRes else R.layout.template_native_full
        } else {
            0
        }
        val key = if (enableDialog) nativeKey else ""

        // Fake loading: hiện loading -> chờ fake time (tranh thủ load) -> show nếu ad sẵn sàng
        if (fakeLoadingTime > 0) {
            LoadingDialog.show(activity)
            if (!isCanShowAds) initInterAds(activity, callback = null)
            Handler(Looper.getMainLooper()).postDelayed({
                LoadingDialog.dismiss()
                if (isCanShowAds) {
                    runCatching {
                        showAdsFull(activity, enableDialog, layoutRes, key, startCallback, doneCallBack)
                    }.onFailure {
                        AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW, AdsLog.Mess.ERR_CALL_SHOW, null))
                        doneCallBack?.invoke()
                    }
                } else {
                    AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW, AdsLog.Mess.CANT_SHOW_VIP, null))
                    doneCallBack?.invoke()
                }
            }, fakeLoadingTime)
            return
        }

        if (!isCanShowAds) {
            AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW, AdsLog.Mess.CANT_SHOW_VIP, null))
            doneCallBack?.invoke()
            return
        }

        try {
            showAdsFull(activity, enableDialog, layoutRes, key, startCallback, doneCallBack)
        } catch (_: Exception) {
            AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW, AdsLog.Mess.ERR_CALL_SHOW, null))
            doneCallBack?.invoke()
        }
    }

    private fun showAdsFull(
        context: AppCompatActivity,
        showDialog: Boolean,
        dialogRes: Int,
        nativeKey: String,
        startCallback: Callback?,
        doneCallBack: Callback?
    ) {
        val currentAd = mInterstitialAd ?: run {
            AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW_ADS_FULL, AdsLog.Mess.SHOW_FAILED_AD_NULL, null))
            doneCallBack?.invoke()
            return
        }
        val dialogNativeFull = FullScreenDialog.newInstance(dialogRes, nativeKey)
        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW_ADS_FULL, AdsLog.Mess.SHOW_FAILED, adError))
                mInterstitialAd = null
                doneCallBack?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW_ADS_FULL, AdsLog.Mess.SHOW_SUCCESS, null))
                startCallback?.invoke()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.ADS_CLICK, AdsLog.Action.SHOW_ADS_FULL, null))
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW_ADS_FULL, AdsLog.Mess.SHOW_DISMISS, null))
                mInterstitialAd = null
                if (dialogNativeFull.dialog != null && showDialog) {
                    dialogNativeFull.requireDialog().setOnDismissListener {
                        AdsManager.onAdsLog(
                            AdsLog(
                                AdsLog.Type.INTER_SPLASH,
                                "",
                                AdsLog.Action.SHOW_DIALOG_ADS,
                                "dialog_dismiss",
                                null
                            )
                        )
                        dialogNativeFull.dismiss()
                        doneCallBack?.invoke()
                    }
                } else {
                    doneCallBack?.invoke()
                }

            }
        }
        if (showDialog) {
            Handler(Looper.getMainLooper()).postDelayed({
                AdsManager.onAdsLog(AdsLog(AdsLog.Type.INTER_SPLASH, "", AdsLog.Action.SHOW_DIALOG_ADS, AdsLog.Mess.CALL_SHOW, null))
                dialogNativeFull.showAllowingStateLoss(
                    context.supportFragmentManager,
                    "FullScreenDialog"
                )
            }, 3000)
        }
        currentAd.show(context)
    }

    private fun clear() {
        mInterstitialAd = null
        isLoading = false
        loadTimeAd = 0
    }
}
