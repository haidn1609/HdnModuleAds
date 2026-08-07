package com.hdn.adsmodule.model

import com.google.android.gms.ads.AdError

class AdsLog(
    var adType: String,
    var adId: String,
    var action: String,
    var mess: String,
    var adError: AdError?
) {
    // Loại ad (adType). Native dùng key động nên không có hằng ở đây.
    object Type {
        const val INTER = "ita"
        const val INTER_SPLASH = "itsa"
        const val REWARD = "rwa"
        const val BANNER = "bna"
        const val BANNER_COLLAPSIBLE = "bnca"
        const val OPEN = "opa"
        const val GDPR = "gdpr"
    }

    // Nhóm hành động (action)
    object Action {
        const val LOAD = "load"
        const val CALL_LOAD = "call_load"
        const val SHOW = "show"
        const val SHOW_ADS_FULL = "show_ads_full"
        const val SHOW_DIALOG_ADS = "show_dialog_ads"
        const val SHOW_ITA_FALLBACK = "show_ita_fall_back"
        const val SHOW_GDPR = "show_gdpr"
        const val FORCE_SHOW = "force_show"
        const val ADS_CLICK = "adsClick"
    }

    // Chi tiết trạng thái (mess)
    object Mess {
        const val START_LOAD = "start_load"
        const val LOAD_SUCCESS = "load_success"
        const val LOAD_FAILED = "load_failed"
        const val START_FAILED = "start_failed"
        const val CALL_SHOW = "call_show"
        const val START_SHOW = "start_show"
        const val SHOW_SUCCESS = "show_success"
        const val SHOW_FAILED = "show_failed"
        const val SHOW_FAILED_AD_NULL = "show_failed_ad_null"
        const val SHOW_DISMISS = "show_dismiss"
        const val SHOW_DONE = "show_done"
        const val SHOW_DONE2 = "show_done2"
        const val CANT_SHOW_VIP = "cant_show_vip"
        const val ERR_SHOWING = "err_showing"
        const val ERR_CALL_SHOW = "err_call_show"
        const val ERR_NOT_ACTIVITY = "err_not_activity"
        const val ERR_BUSY = "err_busy"
        const val ERR_CANT_SHOW = "err_cant_show"
        const val ERR_DELAY = "err_delay"
        const val ERR_ACTIVITY_UNAVAILABLE = "err_activity_unavailable"
    }
}
