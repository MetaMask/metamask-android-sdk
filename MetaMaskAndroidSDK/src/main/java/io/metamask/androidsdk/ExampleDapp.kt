package io.metamask.androidsdk

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.util.*

enum class Network(val chainId: String) {
    GOERLI("0x5"),
    KOVAN("0x2a"),
    ETHEREUM("0x1"),
    POLYGON("0x89"),
    UNKNOWN("unknown");

    companion object {
        fun name(network: Network): String {
            return when(network) {
                GOERLI -> "Goerli Testnet"
                KOVAN -> "Kovan Testnet"
                POLYGON -> "Polygon"
                ETHEREUM -> "Ethereum"
                else -> {
                    "unknown"
                }
            }
        }

        fun rpcUrls(network: Network): List<String> {
            return when(network) {
                POLYGON -> listOf("https://polygon-rpc.com")
                else -> {
                    listOf()
                }
            }
        }

        fun chainNameFor(chainId: String): String {
            val network = enumValues<Network>()
                .toList()
                .first { it.chainId == chainId }

            return name(network)
        }
    }
}

interface RootLayoutProvider {
    fun getRootLayout(): View
}

class ExampleDapp(private val ethereum: EthereumViewModel, private val rootLayoutProvider: RootLayoutProvider): AppCompatActivity() {

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSnackbarWithAction(message: String, buttonTitle: String, action: () -> Unit) {
        val snackbar = Snackbar.make(
            rootLayoutProvider.getRootLayout(),
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(buttonTitle) {
            action()
        }
        snackbar.show()
    }

    fun connect(callback: (Any?) -> Unit) {
        val dapp = Dapp("DroidDapp", "https://www.droiddapp.io")

        ethereum.connect(dapp) { result ->
            if (result is RequestError) {
                showToast(result.message)
                Logger.log("Ethereum connection error: ${result.message}")
            } else {
                Logger.log("Ethereum connection result: $result")
                callback(result)
            }
        }
    }

    fun signMessage(callback: (Any?) -> Unit) {
        val message = "{\"domain\":{\"chainId\":1,\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Linda!\",\"from\":{\"name\":\"Aliko\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Linda\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"
        val params: List<String> = listOf(ethereum.selectedAddress, message)

        val signRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHSIGNTYPEDDATAV4.value,
            params
        )

        ethereum.sendRequest(signRequest) { result ->
            if (result is RequestError) {
                showToast(result.message)
                Logger.log("Ethereum sign error: ${result.message}")
            } else {
                Logger.log("Ethereum sign result: $result")
                callback(result)
            }
        }
    }

    fun sendTransaction(callback: (Any?) -> Unit) {
        val to = "0x0000000000000000000000000000000000000000"
        val amount = "0x01"
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to ethereum.selectedAddress,
            "to" to to,
            "amount" to amount
        )

        val transactionRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHSENDTRANSACTION.value,
            listOf(params)
        )

        ethereum.sendRequest(transactionRequest) { result ->
            if (result is RequestError) {
                Logger.log("Ethereum transaction error: ${result.message}")
                showToast(result.message)
            } else {
                Logger.log("Ethereum transaction result: $result")
                callback(result)
            }
        }
    }

    fun switchChain(callback: (Any?) -> Unit) {
        val chainId = if (ethereum.chainId == Network.ETHEREUM.chainId) {
            Network.POLYGON.chainId
        } else {
            Network.ETHEREUM.chainId
        }
        val switchChainParams: Map<String, String> = mapOf("chainId" to chainId)
        val switchChainRequest = EthereumRequest(
            method = EthereumMethod.SWITCHETHEREUMCHAIN.value,
            params = listOf(switchChainParams)
        )

        ethereum.sendRequest(switchChainRequest) { result ->
            if (result is RequestError) {
                if (result.code == ErrorType.UNRECOGNIZEDCHAINID.code || result.code == ErrorType.SERVERERROR.code) {
                    val message = "${Network.chainNameFor(chainId)} $chainId has not been added to your MetaMask wallet.Add chain?"
                    val buttonTitle = "OK"
                    val action: () -> Unit = {
                        addEthereumChain(chainId, callback)
                    }
                    showSnackbarWithAction(message, buttonTitle, action)
                } else {
                    showToast("Switch chain error: ${result.message}")
                }
            } else {
                showToast("Successfully switched to $chainId")
                callback(chainId)
            }
        }
    }

    fun addEthereumChain(chainId: String, callback: (Any?) -> Unit) {
        val addChainParams: Map<String, Any> = mapOf(
            "chainId" to chainId,
            "chainName" to Network.chainNameFor(chainId),
            "rpcUrls" to Network.rpcUrls(Network.valueOf(chainId))
        )
        val addChainRequest = EthereumRequest(
            method = EthereumMethod.ADDETHEREUMCHAIN.value,
            params = listOf(addChainParams)
        )

        ethereum.sendRequest(addChainRequest) { result ->
            if (result is RequestError) {
                showToast("Add chain error: ${result.message}")
            } else {
                if (chainId == ethereum.chainId) {
                    showToast("Successfully switched to ${Network.chainNameFor(chainId)} $chainId")
                } else {
                    showToast("Successfully added ${Network.chainNameFor(chainId)} $chainId")
                }
                callback(result)
            }
        }
    }
}