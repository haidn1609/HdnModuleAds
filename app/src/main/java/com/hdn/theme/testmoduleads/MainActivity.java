package com.hdn.theme.testmoduleads;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.hdn.adsmodule.ads.AdsManager;
import com.hdn.adsmodule.ads.inter.InterAds;
import com.hdn.adsmodule.ads.inter.InterSplashAds;
import com.hdn.adsmodule.ads.open.OpenAds;
import com.hdn.adsmodule.ads.reward.RewardAds;
import com.hdn.adsmodule.model._enum.BannerType;
import com.hdn.theme.testmoduleads.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initData();
        initEvent();
    }

    private void initData() {
        AdsManager.setDebug(true);
        AdsManager.setVip(false);
        RemoteManager.initRemoteConfig(() -> {
            Toast.makeText(this, "config done", Toast.LENGTH_SHORT).show();

            InterAds.initInterAds(this, new ArrayList<>(), false, null, null);
            InterSplashAds.initInterAds(this, new ArrayList<>(), null);
            OpenAds.initOpenAds(this, new ArrayList<>(), null);
        });
        AdsManager.setAdsPair(adValue -> {
            return null;
        });
        AdsManager.setAdsLog(adValue -> {
            Log.e("Ads_log", adValue.toString());
            return null;
        });
    }

    private void initEvent() {
        binding.showBna.setOnClickListener(v -> AdsManager.showBannerAds(this, BannerType.NORMAL, binding.adFrame));
        binding.showBnca.setOnClickListener(v -> AdsManager.showBannerAds(this, BannerType.COLLAPSIBLE, binding.adFrame));

        binding.showOpa.setOnClickListener(view -> AdsManager.showOpenAds(this, null));

        binding.showItsa.setOnClickListener(view -> AdsManager.showInterSplashAds(this, 0, "native_dialog_full", () -> {
            Toast.makeText(this, "itsa start show", Toast.LENGTH_SHORT).show();
            return null;
        }, () -> {
            Toast.makeText(this, "itsa start show done", Toast.LENGTH_SHORT).show();
            return null;
        }, 1500L));
        binding.showIta.setOnClickListener(view -> AdsManager.showInterAds(this, false, 0L, success -> {
            Toast.makeText(this, "ita show done: " + success, Toast.LENGTH_SHORT).show();
            return null;
        }));
        binding.showItaFix.setOnClickListener(view -> AdsManager.forceShowInterAds(this, false, false, 0L, success -> {
            Toast.makeText(this, "ita force show done: " + success, Toast.LENGTH_SHORT).show();
            return null;
        }));

        binding.showRwa.setOnClickListener(view -> AdsManager.showRewardAds(this, true, true, false, 1500L, new RewardAds.RewardCallback() {
            @Override
            public void onAdShowed() {
                Toast.makeText(MainActivity.this, "start show ads", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdClosed() {
                Toast.makeText(MainActivity.this, "onAdClosed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRewardEarned() {
                //action earn
                Toast.makeText(MainActivity.this, "onRewardEarned", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdFailed() {
                Toast.makeText(MainActivity.this, "onAdFailed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPremium() {
                //action earn
                Toast.makeText(MainActivity.this, "onPremium", Toast.LENGTH_SHORT).show();
            }
        }));

        binding.showNta1.setOnClickListener(view -> AdsManager.loadAndShowNative(this, "native_home", R.layout.template_native_media, binding.adFrame, true, 1));
        binding.showNta2.setOnClickListener(view -> AdsManager.loadAndShowNative(this, "native_home", R.layout.template_native_no_media, binding.adFrame, true, 0));
        binding.showNtca.setOnClickListener(view -> AdsManager.loadAndShowNative(this, "native_collapse", R.layout.template_native_collapse, binding.adFrame, true, 1));

        // nta3/nta4 -> mở ActivityNativeFull (1 = hiện Open Store, 2 = hiện Close)
        binding.showNta3.setOnClickListener(view -> ActivityNativeFull.start(this, 1));
        binding.showNta4.setOnClickListener(view -> ActivityNativeFull.start(this, 2));
    }

}