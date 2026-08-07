package com.hdn.theme.testmoduleads;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.hdn.adsmodule.ads.AdsManager;
import com.hdn.theme.testmoduleads.databinding.DialogNativeFullBinding;

public class ActivityNativeFull extends AppCompatActivity {
    private static final String EXTRA_CLOSE_TYPE = "close_type";

    public interface OnCloseListener {
        void onClose();
    }

    // ponytail: callback tĩnh, 1 instance tại 1 thời điểm
    private static OnCloseListener closeListener;

    public static void start(Context context, int closeType) {
        start(context, closeType, null);
    }

    public static void start(Context context, int closeType, OnCloseListener onClose) {
        closeListener = onClose;
        Intent intent = new Intent(context, ActivityNativeFull.class);
        intent.putExtra(EXTRA_CLOSE_TYPE, closeType);
        context.startActivity(intent);
    }

    private DialogNativeFullBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.dialog_native_full);
        int closeType = getIntent().getIntExtra(EXTRA_CLOSE_TYPE, 0);

        AdsManager.loadAndShowNative(this, "native_dialog_full",
                R.layout.template_native_full, binding.adFrame, true, 1);

        // closeType = 1 -> hiện btnOpenStore, = 2 -> hiện btnClose
        if (closeType == 1) {
            binding.btnOpenStore.setVisibility(View.VISIBLE);
        } else if (closeType == 2) {
            binding.btnClose.setVisibility(View.VISIBLE);
        }

        // btnClose -> đóng activity
        binding.btnClose.setOnClickListener(v -> finish());

        // btnOpenStore -> trigger click CTA của native ad, rồi ẩn nút này và hiện btnClose
        binding.btnOpenStore.setOnClickListener(v -> {
            View cta = binding.adFrame.findViewById(R.id.cta);
            if (cta != null) {
                cta.performClick();
            }
            binding.btnOpenStore.setVisibility(View.GONE);
            binding.btnClose.setVisibility(View.VISIBLE);
        });
    }

    // Nuốt back: chỉ đóng bằng nút (ponytail: cố tình không gọi super)
    @Override
    public void onBackPressed() {
    }

    // Gọi callback sau khi close (chỉ khi thực sự finish, không phải recreate)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing() && closeListener != null) {
            closeListener.onClose();
            closeListener = null;
        }
    }
}
