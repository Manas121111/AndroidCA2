package com.smarttour360.app.ui.chatbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smarttour360.app.R

class ItineraryBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.sheet_itinerary, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.text_itinerary_content).text =
            arguments?.getString(ARG_CONTENT).orEmpty()
    }

    companion object {
        private const val ARG_CONTENT = "content"

        fun newInstance(content: String): ItineraryBottomSheet {
            return ItineraryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONTENT, content)
                }
            }
        }
    }
}
