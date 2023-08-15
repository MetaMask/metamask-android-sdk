package com.metamask.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.metamask.android.databinding.FragmentFirstBinding
import io.metamask.androidsdk.*
import io.metamask.androidsdk.RootLayoutProvider

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), RootLayoutProvider {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private var navigateToDappActions = true

    private val ethereumViewModel: EthereumViewModel by viewModels {
        EthereumViewModelFactory(requireContext())
    }

    override fun getRootLayout(): View {
        return view?.findViewById(R.id.nav_host_fragment_content_main)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        Logger.log("onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.log("onViewCreated")

        val connectButton = view.findViewById<Button>(R.id.connectButton)
        val connectResultLabel = view.findViewById<TextView>(R.id.connectText)

        val sessionButton = view.findViewById<Button>(R.id.clearSessionButton)
        val sessionLabel = view.findViewById<TextView>(R.id.sessionText)

        //ethereumViewModel = ViewModelProvider(this)[EthereumViewModel::class.java]
        "SessionId: ${ethereumViewModel.getSessionId()}".also { sessionLabel.text = it }

        sessionButton.setOnClickListener {
            ethereumViewModel.clearSession()
        }

        connectButton.setOnClickListener {
            navigateToDappActions = true
            ethereumViewModel.connect(
                Dapp("DroidDapp", "https://www.droiddapp.io"
                )) {}
        }

        ethereumViewModel.activeAddress.observe(viewLifecycleOwner) { account ->
            connectResultLabel.text = account
            Logger.log("FirstFragment Eth selected address is: ${ethereumViewModel.selectedAddress}")
            if (account.isNotEmpty() && navigateToDappActions) {
                navigateToDappActionsFragment()
                navigateToDappActions = false
            }
        }

        ethereumViewModel.sessionId.observe(viewLifecycleOwner) { sessionId ->
            "SessionId: $sessionId".also { sessionLabel.text = it }
        }
    }

    private fun navigateToDappActionsFragment() {
        Logger.log("Navigating to Dapp Actions")
        val navController = view?.findNavController()
        navController?.navigate(R.id.action_FirstFragment_to_DappActionsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}