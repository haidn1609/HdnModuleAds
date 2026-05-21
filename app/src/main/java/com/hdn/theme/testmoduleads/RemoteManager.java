package com.hdn.theme.testmoduleads;

import android.annotation.SuppressLint;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.hdn.adsmodule.ads.AdsManager;

import java.util.Collections;
import java.util.List;

public class RemoteManager {
    public static Gson gson = new Gson();
    public static final String iap_enabled = "iap_enabled";
    public static final String ads_enabled = "enable_ads";
    public static final String intern_ads_time = "intern_ads_time";
    public static final String use_case_test = "using_case_test_value";
    public static final String config_native_show = "config_native_show";
    private static ConfigIdRemote configIdRemote = new ConfigIdRemote();

    public static void initRemoteConfig(Runnable complete) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> remoteConfig(mFirebaseRemoteConfig, complete));
    }

    @SuppressLint("WrongConstant")
    private static void remoteConfig(FirebaseRemoteConfig mFirebaseRemoteConfig, Runnable complete) {
        try {
            // [START get_config_values]
            boolean iap_enabled = mFirebaseRemoteConfig.getBoolean(RemoteManager.iap_enabled);
            boolean ads_enabled = mFirebaseRemoteConfig.getBoolean(RemoteManager.ads_enabled);
            String intern_ads_time = mFirebaseRemoteConfig.getString(RemoteManager.intern_ads_time);
            String useCaseTest = mFirebaseRemoteConfig.getString(RemoteManager.use_case_test);
            String jsonSetupId = mFirebaseRemoteConfig.getString(useCaseTest);
            String jsonSetupNativeShow = mFirebaseRemoteConfig.getString(config_native_show);
            //init data obj
            configIdRemote = parseConfigIdRemote(jsonSetupId);
            initRemoteId();
        } catch (Exception ignored) {
        } finally {
            if (complete != null) {
                complete.run();
            }
        }
    }

    private static void initRemoteId() {
        AdsManager.setOpaIdRl(encodeAdIds(configIdRemote.getAppOpen()));
        AdsManager.setOpaIdRm(encodeAdIds(configIdRemote.getAppOpen()));

        AdsManager.setBnaIdRl(encodeAdIds(configIdRemote.getBanner()));
        AdsManager.setBnaIdRm(encodeAdIds(configIdRemote.getBanner()));

        AdsManager.setItaIdRl(encodeAdIds(configIdRemote.getInterstitial()));
        AdsManager.setItaIdRm(encodeAdIds(configIdRemote.getInterstitial()));

        AdsManager.setItsaIdRl(encodeAdIds(configIdRemote.getInterSplash()));
        AdsManager.setItsaIdRm(encodeAdIds(configIdRemote.getInterSplash()));

        AdsManager.setRwaIdRl(encodeAdIds(configIdRemote.getRewarded()));
        AdsManager.setRwaIdRm(encodeAdIds(configIdRemote.getRewarded()));

        AdsManager.addNative("native_dialog_full",
                encodeAdIds(configIdRemote.getNativeDialogFull()),
                encodeAdIds(configIdRemote.getNativeDialogFull()));
        AdsManager.addNative("native_home",
                encodeAdIds(configIdRemote.getNative()),
                encodeAdIds(configIdRemote.getNative()));
        AdsManager.addNative("native_collapse",
                encodeAdIds(configIdRemote.getNativeCollapse()),
                encodeAdIds(configIdRemote.getNativeCollapse()));
    }

    private static ConfigIdRemote parseConfigIdRemote(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return new ConfigIdRemote();
            }
            return gson.fromJson(json, ConfigIdRemote.class);
        } catch (JsonSyntaxException e) {
            return new ConfigIdRemote();
        }
    }

    private static String encodeAdIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return gson.toJson(Collections.emptyList());
        }
        return gson.toJson(ids);
    }
}
