package com.hdn.adsmodule.ads

class AdsIdConfig {
    object Debug {
        const val BANNER = "ca-app-pub-3940256099942544/9214589741"
        const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
        const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    }

    object Release {
        var APP_OPEN = ""
        var BANNER = ""
        var INTERSTITIAL = ""
        var INTERSTITIAL_SPLASH = ""
        var REWARDED = ""
    }

    object Remote {
        var APP_OPEN = ""
        var BANNER = ""
        var INTERSTITIAL = ""
        var INTERSTITIAL_SPLASH = ""
        var REWARDED = ""
    }
}