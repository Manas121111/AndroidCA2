package com.smarttour360.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smarttour360.app.R
import com.smarttour360.app.ui.AppNavigator
import com.smarttour360.app.ui.state.AppStateStore

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = activity as? AppNavigator
        val identityInput = view.findViewById<EditText>(R.id.input_login_identity)

        fun continueToOnboarding() {
            val identity = identityInput.text?.toString().orEmpty().trim()
            AppStateStore.startUserSession(requireContext(), identity)
            navigator?.openOnboarding()
        }

        view.findViewById<MaterialButton>(R.id.button_login).setOnClickListener {
            continueToOnboarding()
        }
        view.findViewById<MaterialButton>(R.id.button_google).setOnClickListener {
            continueToOnboarding()
        }
        view.findViewById<MaterialButton>(R.id.button_signup).setOnClickListener {
            continueToOnboarding()
        }
    }
}
