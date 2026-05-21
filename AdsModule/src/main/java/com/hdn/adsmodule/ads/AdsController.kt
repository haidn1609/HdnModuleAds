package com.hdn.adsmodule.ads

object AdsController {
    var isDebug = true
    var adsEnable = true
    var isVip = false

    fun canShowAds(): Boolean {
        return adsEnable && !isVip
    }
}
