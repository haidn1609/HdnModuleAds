package com.hdn.adsmodule.ads.banner

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener

import androidx.core.view.isNotEmpty
import com.hdn.adsmodule.R
import com.hdn.adsmodule.ads.AdUnitParser
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsIdConfig
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog
import com.hdn.adsmodule.model._enum.BannerType

object BannerAds {
    private fun getBannerIdDefault(bannerType: BannerType): List<String> =
        when (bannerType) {
            BannerType.COLLAPSIBLE -> {
                AdUnitParser.parse(
                    if (AdsController.isDebug)
                        AdsIdConfig.Debug.BANNER
                    else
                        AdsIdConfig.Remote.BANNER_COLLAPSIBLE,

                    if (AdsController.isDebug)
                        AdsIdConfig.Debug.BANNER
                    else
                        AdsIdConfig.Release.BANNER_COLLAPSIBLE
                )
            }

            BannerType.NORMAL -> {
                AdUnitParser.parse(
                    if (AdsController.isDebug)
                        AdsIdConfig.Debug.BANNER
                    else
                        AdsIdConfig.Remote.BANNER,

                    if (AdsController.isDebug)
                        AdsIdConfig.Debug.BANNER
                    else
                        AdsIdConfig.Release.BANNER
                )
            }
        }

    private var globalBannerView: AdView? = null
    private var isBannerLoaded = false

    private fun getAdRequest(type: BannerType): AdRequest = when (type) {
        BannerType.NORMAL -> AdRequest.Builder().build()
        BannerType.COLLAPSIBLE -> AdRequest.Builder()
            .addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                Bundle().apply { putString("collapsible", "bottom") }
            )
            .build()
    }

    fun getAdSize(activity: Activity): AdSize {
        val outMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(outMetrics)
        val adWidth = (outMetrics.widthPixels / outMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    @SuppressLint("InflateParams")
    @JvmStatic
    fun createBannerView(
        activity: Activity,
        type: BannerType,
        adUnitIds: List<String> = getBannerIdDefault(type),
        onFinished: ((View) -> Unit)? = null,
    ): View {
        if (!AdsController.canShowAds()) {
            globalBannerView?.destroy()
            globalBannerView = null
            isBannerLoaded = false

            val emptyView = View(activity)
            emptyView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            )
            onFinished?.invoke(emptyView)
            return emptyView
        }
        AdsManager.onAdsLog(
            AdsLog(
                if (type == BannerType.NORMAL) "bna" else "bnca",
                "",
                "call_show",
                "",
                null
            )
        )
        val viewRoot =
            LayoutInflater.from(activity).inflate(R.layout.banner_ads, null, false)//loading layout
        val adViewContainer: LinearLayout = viewRoot.findViewById(R.id.adView_container)
        val loadingText: View = viewRoot.findViewById(R.id.tv_loading)
        val loadingOverlay: View = viewRoot.findViewById(R.id.view_d)

        val bannerType = type

        val adSize = getAdSize(activity)
        val idsToUse = adUnitIds.ifEmpty { getBannerIdDefault(bannerType) }
        loadingText.minimumHeight = adSize.getHeightInPixels(activity)

        if (bannerType == BannerType.NORMAL) {
            val cachedBanner = globalBannerView
            if (cachedBanner != null) {
                (cachedBanner.parent as? LinearLayout)?.removeView(cachedBanner)
                adViewContainer.removeAllViews()
                adViewContainer.addView(cachedBanner)
                if (isBannerLoaded || adViewContainer.isNotEmpty()) {
                    hideLoading(loadingText, loadingOverlay)
                    AdsManager.onAdsLog(
                        AdsLog("bna", "", "show", "show_success", null)
                    )
                } else {
                    showLoading(loadingText, loadingOverlay)
                    AdsManager.onAdsLog(
                        AdsLog("bna", "", "show", "show_failed", null)
                    )
                }
            } else {
                AdsManager.onAdsLog(AdsLog("bna", "", "call_load", "", null))
                showLoading(loadingText, loadingOverlay)
                loadBannerByIndex(
                    activity = activity,
                    ids = idsToUse,
                    index = 0,
                    bannerType = bannerType,
                    adSize = adSize,
                    loadingText = loadingText,
                    loadingOverlay = loadingOverlay,
                    adViewContainer = adViewContainer
                )
            }
        } else {
            AdsManager.onAdsLog(AdsLog("bnca", "", "call_load", "", null))
            showLoading(loadingText, loadingOverlay)
            loadBannerByIndex(
                activity = activity,
                ids = idsToUse,
                index = 0,
                bannerType = bannerType,
                adSize = adSize,
                loadingText = loadingText,
                loadingOverlay = loadingOverlay,
                adViewContainer = adViewContainer
            )
        }

        onFinished?.invoke(viewRoot)
        return viewRoot
    }

    private fun loadBannerByIndex(
        activity: Activity,
        ids: List<String>,
        index: Int,
        bannerType: BannerType,
        adSize: AdSize,
        loadingText: View,
        loadingOverlay: View,
        adViewContainer: LinearLayout
    ) {
        if (index >= ids.size) {
            if (bannerType == BannerType.NORMAL) {
                isBannerLoaded = false
                globalBannerView = null
            }
            hideLoading(loadingText, loadingOverlay)
            return
        }

        val bannerView = AdView(activity).apply {
            adUnitId = ids[index]
            setAdSize(adSize)
            AdsManager.onAdsLog(
                AdsLog(
                    if (bannerType == BannerType.NORMAL) "bna" else "bnca",
                    adUnitId,
                    "load",
                    "start_load",
                    null
                )
            )
            onPaidEventListener = OnPaidEventListener { adValue ->
                AdsManager.onAdsPair(
                    AdValue(
                        adValue,
                        responseInfo?.loadedAdapterResponseInfo
                    )
                )
            }

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    if (bannerType == BannerType.NORMAL) {
                        isBannerLoaded = true
                        globalBannerView = this@apply
                    }
                    AdsManager.onAdsLog(
                        AdsLog(
                            if (bannerType == BannerType.NORMAL) "bna" else "bnca",
                            adUnitId,
                            "load",
                            "load_success",
                            null
                        )
                    )
                    AdsManager.onAdsLog(
                        AdsLog(
                            if (bannerType == BannerType.NORMAL) "bna" else "bnca",
                            "",
                            "show",
                            "show_success",
                            null
                        )
                    )
                    hideLoading(loadingText, loadingOverlay)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (bannerType == BannerType.NORMAL) {
                        isBannerLoaded = false
                        globalBannerView = null
                    }
                    AdsManager.onAdsLog(
                        AdsLog(
                            if (bannerType == BannerType.NORMAL) "bna" else "bnca",
                            adUnitId,
                            "load",
                            "load_failed",
                            error
                        )
                    )
                    adViewContainer.removeView(this@apply)
                    loadBannerByIndex(
                        activity = activity,
                        ids = ids,
                        index = index + 1,
                        bannerType = bannerType,
                        adSize = adSize,
                        loadingText = loadingText,
                        loadingOverlay = loadingOverlay,
                        adViewContainer = adViewContainer
                    )
                }
            }
        }

        if (bannerType == BannerType.NORMAL && index == 0) {
            globalBannerView = bannerView
        }
        adViewContainer.addView(bannerView)

        bannerView.loadAd(getAdRequest(bannerType))
    }

    private fun showLoading(loadingText: View, loadingOverlay: View) {
        loadingText.visibility = View.VISIBLE
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading(loadingText: View, loadingOverlay: View) {
        loadingText.visibility = View.GONE
        loadingOverlay.visibility = View.GONE
    }
}
