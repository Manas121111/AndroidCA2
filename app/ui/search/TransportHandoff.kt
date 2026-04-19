package com.smarttour360.app.ui.search

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

fun Fragment.showExternalBookingHandoff(
    title: String,
    summary: String,
    url: String
) {
    AlertDialog.Builder(requireContext())
        .setTitle(title)
        .setMessage(
            "The selected booking details will be copied to the clipboard, then the external booking website will open.\n\n$summary"
        )
        .setPositiveButton("Open") { _, _ ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Booking details", summary))
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        .setNegativeButton("Cancel", null)
        .show()
}
