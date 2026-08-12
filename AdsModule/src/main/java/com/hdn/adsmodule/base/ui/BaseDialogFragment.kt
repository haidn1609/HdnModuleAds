package com.hdn.adsmodule.base.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

abstract class BaseDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BaseDialog(requireContext(), theme)
}
