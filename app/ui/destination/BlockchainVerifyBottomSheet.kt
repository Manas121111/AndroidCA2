package com.smarttour360.app.ui.destination

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smarttour360.app.R

class BlockchainVerifyBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.sheet_blockchain_verify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        view.findViewById<TextView>(R.id.text_chain_flag).text =
            "On-chain flag: ${args.getString(ARG_FLAG)}"
        view.findViewById<TextView>(R.id.text_chain_hash).text =
            "Transaction: ${args.getString(ARG_HASH)}"
        view.findViewById<TextView>(R.id.text_chain_timestamp).text =
            "Timestamped: ${args.getString(ARG_TIMESTAMP)}"
        view.findViewById<ImageButton>(R.id.button_close).setOnClickListener { dismiss() }
    }

    companion object {
        private const val ARG_FLAG = "flag"
        private const val ARG_HASH = "hash"
        private const val ARG_TIMESTAMP = "timestamp"

        fun newInstance(
            flag: String,
            blockchainRef: String,
            timestamp: String
        ): BlockchainVerifyBottomSheet {
            return BlockchainVerifyBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FLAG, flag)
                    putString(ARG_HASH, blockchainRef)
                    putString(ARG_TIMESTAMP, timestamp)
                }
            }
        }
    }
}
