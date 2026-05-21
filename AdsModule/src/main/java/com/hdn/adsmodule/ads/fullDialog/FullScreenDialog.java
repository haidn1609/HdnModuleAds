package com.hdn.adsmodule.ads.fullDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.hdn.adsmodule.R;
import com.hdn.adsmodule.ads.nativeAds.NativeAds;
import com.hdn.adsmodule.databinding.DialogFullScreenBinding;


public class FullScreenDialog extends DialogFragment {
    private DialogFullScreenBinding binding;
    private static final String KEY_LAYOUT_RES = "layout_res";
    private static final String KEY_NATIVE_KEY = "native_key";
    private int layoutRes = R.layout.template_native_full;
    private String nativeKey = "";
    public static FullScreenDialog newInstance(@LayoutRes int layoutRes,String nativeKey) {
        FullScreenDialog dialog = new FullScreenDialog();

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_LAYOUT_RES, layoutRes);
        bundle.putString(KEY_NATIVE_KEY, nativeKey);

        dialog.setArguments(bundle);

        return dialog;
    }
    public void showAllowingStateLoss(FragmentManager fm, String tag) {
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    public static FullScreenDialog display(FragmentManager fragmentManager) {
        FullScreenDialog exampleDialog = new FullScreenDialog();
        exampleDialog.show(fragmentManager, "TAG");
        return exampleDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog);
        if (getArguments() != null) {
            layoutRes = getArguments().getInt(
                    KEY_LAYOUT_RES,
                    R.layout.dialog_full_screen
            );
            nativeKey = getArguments().getString(
                    KEY_NATIVE_KEY,
                    ""
            );
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(width, height);
                dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                dialog.getWindow().setWindowAnimations(R.style.AppTheme_FullScreenDialog);
            }

        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFullScreenBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e("zzz", "onViewCreated: "+layoutRes );
        NativeAds.loadAndShow(requireActivity(),nativeKey,layoutRes,binding.flNative,false,1);
        binding.ivClose.setOnClickListener(v -> {
            requireDialog().dismiss();
            dismiss();
        });
    }
}
