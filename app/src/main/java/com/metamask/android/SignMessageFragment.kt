package com.metamask.android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import io.metamask.androidsdk.*
import java.util.*

class SignMessageFragment : Fragment(), RootLayoutProvider {

//    private val ethereum: EthereumViewModel by viewModels(
//        ownerProducer = { requireParentFragment() }
//    )
    //private lateinit var ethereumViewModel: EthereumViewModel
    private val mainHandler = Handler(Looper.getMainLooper())
    private var signMessageText = "{\"domain\":{\"chainId\":1,\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"

    private val ethereumViewModel: EthereumViewModel by viewModels {
        EthereumViewModelFactory(requireContext())
    }

    override fun getRootLayout(): View {
        return view?.findViewById(R.id.nav_host_fragment_content_main)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val signInputLabel = view.findViewById<TextView>(R.id.signTextView)
        signInputLabel.text = signMessageText

        val signButton = view.findViewById<Button>(R.id.signActionButton)
        val signResultLabel = view.findViewById<TextView>(R.id.signTextResult)

        //ethereumViewModel = ViewModelProvider(this)[EthereumViewModel::class.java]
        Logger.log("Eth selected address is: ${ethereumViewModel.selectedAddress}")

        signButton.setOnClickListener {
            signMessage() { result ->
                Logger.log("Selected address: ${ethereumViewModel.selectedAddress}")
                signResultLabel.text = result.toString()
            }
        }
    }

    fun signMessage(callback: (Any?) -> Unit) {
        val params: List<String> = listOf(ethereumViewModel.selectedAddress, signMessageText)

        val signRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHSIGNTYPEDDATAV4.value,
            params
        )

        ethereumViewModel.sendRequest(signRequest) { result ->
            mainHandler.post {
                if (result is RequestError) {
                    //showToast(result.message)
                    Logger.log("Ethereum sign error: ${result.message}")
                } else {
                    Logger.log("Ethereum sign result: $result")
                    callback(result)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_message, container, false)
    }
}