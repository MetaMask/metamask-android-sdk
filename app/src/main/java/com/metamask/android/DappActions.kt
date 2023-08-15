package com.metamask.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import io.metamask.androidsdk.EthereumViewModel
import io.metamask.androidsdk.EthereumViewModelFactory
import io.metamask.androidsdk.Logger

/**
 * A simple [Fragment] subclass.
 * Use the [DappActions.newInstance] factory method to
 * create an instance of this fragment.
 */
class DappActions : Fragment() {

    private val ethereumViewModel: EthereumViewModel by viewModels(
        factoryProducer = { EthereumViewModelFactory(requireContext()) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signButton = view.findViewById<Button>(R.id.signButton)
        val sendButton = view.findViewById<Button>(R.id.sendButton)
        val switchChainButton = view.findViewById<Button>(R.id.switchChainButton)

        signButton.setOnClickListener {
            navigateToSignFragment()
        }

        Logger.log("DappActions Eth selected address is: ${ethereumViewModel.selectedAddress}")

        sendButton.setOnClickListener {
            navigateToSendFragment()
        }

        switchChainButton.setOnClickListener {
            navigateToSwitchChainFragment()
        }
    }

    private fun navigateToSignFragment() {
        view?.findNavController()?.navigate(R.id.action_DappActions_to_SignFragment)
    }

    private fun navigateToSendFragment() {
        view?.findNavController()?.navigate(R.id.action_DappActions_to_SendTransactionFragment)
    }

    private fun navigateToSwitchChainFragment() {
        view?.findNavController()?.navigate(R.id.action_DappActions_to_SwitchChainFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dapp_actions, container, false)
    }
}