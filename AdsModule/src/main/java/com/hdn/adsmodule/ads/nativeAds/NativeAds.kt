package com.hdn.adsmodule.ads.nativeAds

import android.app.Activity
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.hdn.adsmodule.ads.AdUnitParser
import com.hdn.adsmodule.ads.AdsController
import com.hdn.adsmodule.ads.AdsIdConfig
import kotlin.collections.forEach
import kotlin.collections.getOrPut

object NativeAds {

    private val managers = hashMapOf<String, NativeAdManager>()
    private val listModel = mutableListOf<NativeAdsModel>()

    enum class Action {
        PRELOAD,
        SHOW,
        LOAD_AND_SHOW
    }

    private fun getIds(nativeModel: NativeAdsModel): List<String> {

        val fallback = if (AdsController.isDebug) {
            AdsIdConfig.Debug.NATIVE
        } else {
            nativeModel.releaseId
        }

        val raw = if (AdsController.isDebug) {
            AdsIdConfig.Debug.NATIVE
        } else {
            nativeModel.remoteId
        }

        return AdUnitParser.parse(raw, fallback)
    }

    private fun getManager(nativeModel: NativeAdsModel): NativeAdManager {
        return managers.getOrPut(nativeModel.key) {
            NativeAdManager(nativeModel.key,getIds(nativeModel))
        }
    }

    private fun clear(container: FrameLayout? = null) {
        container?.removeAllViews()

        managers.values.forEach {
            it.destroy()
        }

        managers.clear()
    }

    /**
     * Hàm duy nhất cho preload/show/loadAndShow
     */
    @JvmStatic
    fun load(
        activity: Activity,
        nativeModel: NativeAdsModel,
        action: Action,
        container: FrameLayout? = null,
        @LayoutRes layoutRes: Int = 0,
        loadIfMissing: Boolean = false,
        cacheSize: Int = 1
    ): Boolean {

        if (!AdsController.canShowAds()) {
            clear(container)
            return false
        }

        val manager = getManager(nativeModel)

        return when (action) {

            Action.PRELOAD -> {
                manager.preload(activity, cacheSize)
                true
            }

            Action.SHOW -> {

                if (container == null || layoutRes == 0) {
                    return false
                }

                manager.show(
                    activity = activity,
                    container = container,
                    layoutResId = layoutRes,
                    loadIfMissing = loadIfMissing,
                    cacheSize = cacheSize
                )
            }

            Action.LOAD_AND_SHOW -> {

                if (container == null || layoutRes == 0) {
                    return false
                }

                manager.loadAndShow(
                    activity = activity,
                    container = container,
                    layoutResId = layoutRes,
                    cacheSize = cacheSize
                )
            }
        }
    }

    @JvmStatic
    fun addNative(key: String, rlId: String, rmId: String) {
        val nativeModel = getNative(key)
        if (nativeModel == null) {
            listModel.add(
                NativeAdsModel(key, rlId, rmId)
            )
        }
    }

    fun getNative(key: String): NativeAdsModel? {
        return listModel.find { it.key == key }
    }

    @JvmStatic
    fun destroy(nativeModel: NativeAdsModel) {
        managers[nativeModel.key]?.destroy()
        managers.remove(nativeModel.key)
    }

    @JvmStatic
    fun destroyAll() {
        managers.values.forEach {
            it.destroy()
        }
        managers.clear()
    }

    // =========================================================
    // ACTION
    // =========================================================
    @JvmStatic
    fun preload(
        activity: Activity,
        key: String,
        cacheSize: Int = 1
    ) {
        val nativeModel = getNative(key)
        if (nativeModel != null) {
            load(
                activity = activity,
                nativeModel = nativeModel,
                action = Action.PRELOAD,
                cacheSize = cacheSize
            )
        }
    }

    @JvmStatic
    fun show(
        activity: Activity,
        key: String,
        resId: Int,
        container: FrameLayout,
        loadIfMissing: Boolean = false,
        cacheSize: Int = 1
    ): Boolean {
        val nativeModel = getNative(key)
        return if (nativeModel != null) {
            load(
                activity = activity,
                nativeModel = nativeModel,
                action = Action.SHOW,
                container = container,
                layoutRes = resId,
                loadIfMissing = loadIfMissing,
                cacheSize = cacheSize
            )
        } else {
            false
        }
    }

    @JvmStatic
    fun loadAndShow(
        activity: Activity,
        key: String,
        resId: Int,
        container: FrameLayout,
        loadIfMissing: Boolean = false,
        cacheSize: Int = 1
    ): Boolean {
        val nativeModel = getNative(key)
        return if (nativeModel != null) {
            load(
                activity = activity,
                nativeModel = nativeModel,
                action = Action.LOAD_AND_SHOW,
                container = container,
                layoutRes = resId,
                loadIfMissing = loadIfMissing,
                cacheSize = cacheSize
            )
        } else {
            false
        }
    }
}