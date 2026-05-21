package com.hdn.theme.testmoduleads;

import android.app.Application;

import com.google.android.gms.ads.AdActivity;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hdn.adsmodule.ads.open.OpenAds;
import com.hdn.adsmodule.ads.open.OpenAdsHelper;

public class App extends Application {
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        MobileAds.initialize(
                this,
                initializationStatus -> {
                });

        new OpenAdsHelper().setup(this);
        OpenAds.disableAdsOpenForActivity(AdActivity.class);

        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }

    public static App getInstance() {
        return instance;
    }
}
