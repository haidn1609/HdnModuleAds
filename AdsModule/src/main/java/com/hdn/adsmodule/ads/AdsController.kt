package com.hdn.adsmodule.ads

object AdsController {
    var isDebug = true
    var adsEnable = true
    var isVip = false

    fun canShowAds(): Boolean {
        return adsEnable && !isVip
    }

    // useWithoutVip = true: vẫn cho show dù đang VIP (giống RewardAds)
    fun canShowAds(useWithoutVip: Boolean): Boolean {
        return adsEnable && (!isVip || useWithoutVip)
    }
}
