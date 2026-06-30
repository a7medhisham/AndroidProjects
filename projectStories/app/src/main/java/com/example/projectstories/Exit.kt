package com.example.projectstories

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ExitDialog:DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder=AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setTitle(getString(R.string.exit))
            .setMessage(getString(R.string.do_you_want_to_exit))
            .setPositiveButton(getString(R.string.ok), { _, _ -> activity?.finish() })
            .setNegativeButton(getString(R.string.no),{ dialog, _ ->dialog.dismiss()})
        return builder.create()
    }

}