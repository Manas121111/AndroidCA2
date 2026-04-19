package com.smarttour360.app.ui.blockchain

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AcknowledgeRiskDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("High Risk Destination")
            .setMessage("This destination is currently flagged RED.\n\nRisk Score: 0.81\nPrimary risk: Crime / incident history at 78%.\n\nYour acknowledgement will be recorded on the blockchain for transparency.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("I Understand — Proceed", null)
            .create()
    }
}
