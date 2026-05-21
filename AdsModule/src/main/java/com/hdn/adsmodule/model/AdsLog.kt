package com.hdn.adsmodule.model

import com.google.android.gms.ads.AdError

class AdsLog(
    var adType: String,
    var adId: String,
    var action: String,
    var mess: String,
    var adError: AdError?
)