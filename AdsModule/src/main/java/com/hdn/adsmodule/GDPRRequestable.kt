package com.hdn.adsmodule

import android.app.Activity
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.hdn.adsmodule.ads.AdsManager
import com.hdn.adsmodule.model.AdsLog

/**
 * Created by Huann on 1/13/2026 9:40 AM
 */


class GDPRRequestable(private val context: Activity) {
    var consentInformation: ConsentInformation? = null

    fun interface RequestGDPRCompleted {
        fun onRequestGDPRCompleted(formError: FormError?)
    }

    private var onRequestGDPRCompleted: RequestGDPRCompleted? = null

    fun setOnRequestGDPRCompleted(onRequestGDPRCompleted: RequestGDPRCompleted) {
        this.onRequestGDPRCompleted = onRequestGDPRCompleted
    }

    fun requestGDPR() {
        val consentDebugSettingsBuilder = ConsentDebugSettings.Builder(context)
        if (BuildConfig.DEBUG) {
            consentDebugSettingsBuilder
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(YOUR_TEST_DEVICE_ID)
        }

        val consentDebugSettings = consentDebugSettingsBuilder.build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(consentDebugSettings)
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation!!.requestConsentInfoUpdate(
            context,
            params,
            {
                if (consentInformation!!.isConsentFormAvailable) {
                    loadForm()
                } else {
                    onRequestGDPRCompleted!!.onRequestGDPRCompleted(null)
                }
            },
            { formError -> onRequestGDPRCompleted!!.onRequestGDPRCompleted(formError) }
        )
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(context,
            { consentForm ->
                Companion.consentForm = consentForm
                if (consentInformation!!.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    AdsManager.onAdsLog(AdsLog("gdpr","" , "show_gdpr", "", null))
                    Companion.consentForm!!.show(context) {
                        if (consentInformation!!.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                            onRequestGDPRCompleted!!.onRequestGDPRCompleted(null)
                            AdsManager.onAdsLog(AdsLog("gdpr","" , "show_gdpr", "show_done", null))
                        }
                        //  loadForm();
                    }
                } else if (consentInformation!!.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                    onRequestGDPRCompleted!!.onRequestGDPRCompleted(null)
                    AdsManager.onAdsLog(AdsLog("gdpr","" , "show_gdpr", "show_done2", null))
                }
            },
            { formError -> onRequestGDPRCompleted!!.onRequestGDPRCompleted(formError) })
    }

    companion object {
        var consentForm: ConsentForm? = null
        var gdprRequestable: GDPRRequestable? = null
        var YOUR_TEST_DEVICE_ID: String = "25FDFE1909F2CA306D72CED218984437"

        fun getGdprRequestable(activity: Activity): GDPRRequestable {
            if (gdprRequestable == null) {
                return GDPRRequestable(activity).also { gdprRequestable = it }
            } else return gdprRequestable!!
        }
    }
}

