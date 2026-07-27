package com.hdn.adsmodule.ads.nativeAds

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.hdn.adsmodule.R
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.model.AdValue
import com.hdn.adsmodule.model.AdsLog
import java.lang.ref.WeakReference
import java.util.ArrayDeque
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.let
import kotlin.ranges.coerceAtLeast
import kotlin.text.isNullOrEmpty

class NativeAdManager(
    private val key: String, private val adUnitIds: List<String>
) {

    private val TAG = "NativeAdManager"

    private val cachedAds = ArrayDeque<NativeAd>()
    private val activeAds = linkedSetOf<NativeAd>()
    private var desiredCacheSize = 1
    var isLoading: Boolean = false
        private set

    private enum class LoadMode {
        CACHE, NO_CACHE
    }

    private var pendingShow: PendingShow? = null
    private var currentIndex = 0

    private data class PendingShow(
        val activityRef: WeakReference<Activity>,
        val containerRef: WeakReference<FrameLayout>,
        val layoutResId: Int
    )

    fun preload(
        activity: Activity, cacheSize: Int = 1
    ) {

        if (!AdsController.canShowAds()) {
            destroy()
            return
        }

        desiredCacheSize = cacheSize.coerceAtLeast(0)

        if (desiredCacheSize == 0) {
            return
        }

        if (adUnitIds.isEmpty() || isLoading || cachedAds.size >= desiredCacheSize) {
            return
        }

        isLoading = true
        currentIndex = 0

        loadNextAd(activity, LoadMode.CACHE)
    }

    private fun loadNextAd(
        activity: Activity, mode: LoadMode
    ) {

        if (currentIndex >= adUnitIds.size) {
            isLoading = false
            tryShowPending()
            return
        }

        val adUnitId = adUnitIds[currentIndex]

        val adLoader = AdLoader.Builder(activity, adUnitId).forNativeAd { ad ->

                activeAds.add(ad)

                when (mode) {

                    LoadMode.CACHE -> {
                        cachedAds.addLast(ad)

                        isLoading = false
                        tryShowPending()

                        if (desiredCacheSize > 0 && cachedAds.size < desiredCacheSize) {
                            preload(activity, desiredCacheSize)
                        }
                    }

                    LoadMode.NO_CACHE -> {

                        isLoading = false

                        val pending = pendingShow

                        val container = pending?.containerRef?.get()

                        if (pending != null && container != null) {

                            val adView = activity.layoutInflater.inflate(
                                pending.layoutResId, container, false
                            ) as NativeAdView

                            populateNativeAdView(
                                activity, adView, ad, container
                            )

                            pendingShow = null
                        } else {
                            releaseAd(ad)
                        }
                    }
                }

                ad.setOnPaidEventListener { adValue ->
                    AdsManager.onAdsPair(
                        AdValue(
                            adValue, ad.responseInfo?.loadedAdapterResponseInfo
                        )
                    )
                }

                AdsManager.onAdsLog(
                    AdsLog(
                        key, adUnitId, "load", "load_success", null
                    )
                )
            }.withAdListener(object : AdListener() {

                override fun onAdClicked() {
                    AdsManager.onAdsLog(
                        AdsLog(
                            key, adUnitId, "adsClick", "show", null
                        )
                    )
                }

                override fun onAdFailedToLoad(
                    loadAdError: LoadAdError
                ) {

                    AdsManager.onAdsLog(
                        AdsLog(
                            key, adUnitId, "load", "load_failed", loadAdError
                        )
                    )

                    currentIndex++

                    loadNextAd(
                        activity, mode
                    )
                }
            }).withNativeAdOptions(
                NativeAdOptions.Builder().setVideoOptions(
                        VideoOptions.Builder().setStartMuted(true).build()
                    ).build()
            ).build()

        adLoader.loadAd(
            AdRequest.Builder().build()
        )
    }

    private fun tryShowPending() {

        val pending = pendingShow ?: return

        val activity = pending.activityRef.get()
        val container = pending.containerRef.get()

        AdsManager.onAdsLog(
            AdsLog(
                key, "", "try_show_pending", "call_show_pending", null
            )
        )

        if (activity == null || container == null) {

            AdsManager.onAdsLog(
                AdsLog(
                    key, "", "try_show_pending", "err_view_unavailable", null
                )
            )

            pendingShow = null
            return
        }

        val ad = pollCachedAd()

        if (ad == null) {

            AdsManager.onAdsLog(
                AdsLog(
                    key, "", "try_show_pending", "err_ad_null", null
                )
            )

            return
        }

        val adView = activity.layoutInflater.inflate(
            pending.layoutResId, container, false
        ) as NativeAdView

        AdsManager.onAdsLog(
            AdsLog(
                key, "", "try_show_pending", "call_show", null
            )
        )

        populateNativeAdView(
            activity, adView, ad, container
        )

        pendingShow = null

        // FIX cacheSize = 0
        if (desiredCacheSize > 0 && cachedAds.size < desiredCacheSize && !isLoading) {
            preload(activity, desiredCacheSize)
        }
    }

    fun show(
        activity: Activity,
        container: FrameLayout,
        layoutResId: Int,
        loadIfMissing: Boolean = false,
        cacheSize: Int = 1
    ): Boolean {

        if (!AdsController.canShowAds()) {
            container.removeAllViews()
            destroy()
            return false
        }

        desiredCacheSize = cacheSize.coerceAtLeast(0)

        pollCachedAd()?.let { ad ->

            val adView = activity.layoutInflater.inflate(
                layoutResId, container, false
            ) as NativeAdView

            populateNativeAdView(
                activity, adView, ad, container
            )

            // FIX cacheSize = 0
            if (desiredCacheSize > 0 && cachedAds.size < desiredCacheSize && !isLoading) {
                preload(activity, desiredCacheSize)
            }

            return true
        }

        pendingShow = PendingShow(
            activityRef = WeakReference(activity),
            containerRef = WeakReference(container),
            layoutResId = layoutResId
        )

        if (loadIfMissing && !isLoading) {
            preload(activity, desiredCacheSize)
        }

        return false
    }

    fun loadAndShow(
        activity: Activity, container: FrameLayout, layoutResId: Int, cacheSize: Int = 1
    ): Boolean {

        if (!AdsController.canShowAds()) {
            container.removeAllViews()
            destroy()
            return false
        }

        desiredCacheSize = cacheSize.coerceAtLeast(0)

        pollCachedAd()?.let { ad ->

            val adView = activity.layoutInflater.inflate(
                layoutResId, container, false
            ) as NativeAdView

            populateNativeAdView(
                activity, adView, ad, container
            )

            if (desiredCacheSize > 0 && cachedAds.size < desiredCacheSize && !isLoading) {
                preload(activity, desiredCacheSize)
            }

            return true
        }

        pendingShow = PendingShow(
            WeakReference(activity), WeakReference(container), layoutResId
        )

        if (!isLoading) {

            if (desiredCacheSize == 0) {
                loadSingleAdForShow(activity)
            } else {
                preload(activity, desiredCacheSize)
            }
        }

        return false
    }

    private fun loadSingleAdForShow(activity: Activity) {

        if (adUnitIds.isEmpty() || isLoading) return

        isLoading = true
        currentIndex = 0

        loadNextAd(activity, LoadMode.NO_CACHE)
    }

    private fun populateNativeAdView(
        activity: Activity,
        adView: NativeAdView,
        ad: NativeAd,
        container: FrameLayout,
    ) {

        if (activity.isFinishing || activity.isDestroyed) {

            AdsManager.onAdsLog(
                AdsLog(
                    key, "", "show", "err_activity_unavailable", null
                )
            )

            releaseAd(ad)
            return
        }

        AdsManager.onAdsLog(
            AdsLog(
                key, "", "show", "start_show", null
            )
        )

        container.removeAllViews()

        if (adView.parent != null) {
            (adView.parent as? ViewGroup)?.removeView(adView)
        }

        container.addView(adView)

        with(adView) {

            findViewById<MediaView>(R.id.media_view)?.let { mv ->

                mediaView = mv

                mv.mediaContent = ad.mediaContent

                mv.visibility = if (ad.mediaContent != null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }

            bindText(R.id.primary, ad.headline) {
                headlineView = it
            }

            bindText(R.id.body, ad.body) {
                bodyView = it
            }

            bindText(R.id.cta, ad.callToAction) {
                callToActionView = it
            }

            bindText(R.id.secondary, ad.advertiser) {
                advertiserView = it
            }

            findViewById<ImageView>(R.id.icon)?.let { iv ->

                iconView = iv

                val icon = ad.icon?.drawable

                if (icon != null) {
                    iv.setImageDrawable(icon)
                    iv.visibility = View.VISIBLE
                } else {
                    iv.visibility = View.GONE
                }
            }

            findViewById<RatingBar>(R.id.rating_bar)?.let { rb ->

                starRatingView = rb

                val rating = ad.starRating

                if (rating != null) {
                    rb.rating = rating.toFloat()
                    rb.visibility = View.VISIBLE
                } else {
                    rb.visibility = View.GONE
                }
            }

            findViewById<View>(R.id.btnClose)?.let { btnClose ->

                btnClose.setOnClickListener {

                    AdsManager.onAdsLog(
                        AdsLog(
                            key, "", "ad_click", "close_click", null
                        )
                    )

                    container.removeAllViews()
                    destroy()
                }
            }

            setNativeAd(ad)

            AdsManager.onAdsLog(
                AdsLog(
                    key, "", "show", "show_done", null
                )
            )
        }
    }

    fun destroy() {
        activeAds.forEach {
            it.destroy()
        }

        activeAds.clear()
        cachedAds.clear()

        isLoading = false
        pendingShow = null
        currentIndex = 0
    }

    private fun pollCachedAd(): NativeAd? {

        while (cachedAds.isNotEmpty()) {

            val ad = cachedAds.removeFirst()

            if (activeAds.contains(ad)) {
                return ad
            }
        }

        return null
    }

    private fun releaseAd(ad: NativeAd) {
        cachedAds.remove(ad)
        activeAds.remove(ad)
        ad.destroy()
    }

    private inline fun NativeAdView.bindText(
        resId: Int, text: String?, registerView: (View) -> Unit
    ) {

        val view = findViewById<View>(resId) ?: return

        if (!text.isNullOrEmpty()) {

            if (view is TextView) {
                view.text = text
            }

            if (view is Button) {
                view.text = text
            }

            view.visibility = View.VISIBLE

            registerView(view)

        } else {
            view.visibility = View.GONE
        }
    }
}