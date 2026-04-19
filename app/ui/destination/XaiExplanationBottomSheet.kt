package com.smarttour360.app.ui.destination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smarttour360.app.R

class XaiExplanationBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.sheet_xai_explanation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        view.findViewById<TextView>(R.id.text_sheet_title).text = args.getString(ARG_TITLE)
        view.findViewById<TextView>(R.id.text_risk_score).text =
            "Risk Score: ${args.getString(ARG_RISK_SCORE)}"
        view.findViewById<TextView>(R.id.text_explanation).text = args.getString(ARG_EXPLANATION)
        view.findViewById<TextView>(R.id.text_dominant_factor).text =
            "Dominant Factor: ${args.getString(ARG_DOMINANT_FACTOR)}"
        view.findViewById<TextView>(R.id.text_structural).text =
            "Structural (Crime) ${args.getInt(ARG_STRUCTURAL)}%"
        view.findViewById<TextView>(R.id.text_situational).text =
            "Situational (Wx) ${args.getInt(ARG_SITUATIONAL)}%"
        view.findViewById<TextView>(R.id.text_environmental).text =
            "Environmental (AQI) ${args.getInt(ARG_ENVIRONMENTAL)}%"
        view.findViewById<TextView>(R.id.text_blockchain_ref).text =
            "Blockchain Ref: ${args.getString(ARG_BLOCKCHAIN_REF)}"
        view.findViewById<ImageButton>(R.id.button_close).setOnClickListener { dismiss() }
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_RISK_SCORE = "risk_score"
        private const val ARG_EXPLANATION = "explanation"
        private const val ARG_DOMINANT_FACTOR = "dominant_factor"
        private const val ARG_STRUCTURAL = "structural"
        private const val ARG_SITUATIONAL = "situational"
        private const val ARG_ENVIRONMENTAL = "environmental"
        private const val ARG_BLOCKCHAIN_REF = "blockchain_ref"

        fun newInstance(
            title: String,
            riskScore: String,
            explanation: String,
            dominantFactor: String,
            structural: Int,
            situational: Int,
            environmental: Int,
            blockchainRef: String
        ): XaiExplanationBottomSheet {
            return XaiExplanationBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_RISK_SCORE, riskScore)
                    putString(ARG_EXPLANATION, explanation)
                    putString(ARG_DOMINANT_FACTOR, dominantFactor)
                    putInt(ARG_STRUCTURAL, structural)
                    putInt(ARG_SITUATIONAL, situational)
                    putInt(ARG_ENVIRONMENTAL, environmental)
                    putString(ARG_BLOCKCHAIN_REF, blockchainRef)
                }
            }
        }
    }
}
