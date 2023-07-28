package io.metamask.androidsdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
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

class ExampleDapp(context: Context) {
    val ethereum = Ethereum.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun connect(callback: (Any?) -> Unit) {
        val dapp = Dapp("DroidDapp", "https://www.droiddapp.io")

        ethereum.connect(dapp) { result ->
            if (result is RequestError) {
                Logger.log("Ethereum connection error: ${result.message}")
            } else {
                Logger.log("Ethereum connection result: $result")
            }
            mainHandler.post {
                callback(result)
            }
        }
    }

    fun signMessage(callback: (Any?) -> Unit) {
        val message = "{\"domain\":{\"chainId\":1,\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Linda!\",\"from\":{\"name\":\"Aliko\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Linda\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"
        val from = ethereum.selectedAddress ?: ""
        val params: List<String> = listOf(from, message)
        val signRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHSIGNTYPEDDATAV4.value,
            params
        )

        ethereum.sendRequest(signRequest) { result ->
            if (result is RequestError) {
                Logger.log("Ethereum sign error: ${result.message}")
                callback(result.message)
            } else {
                Logger.log("Ethereum sign result: $result")
                callback(result)
            }
        }
    }

    fun sendTransaction(callback: (Any?) -> Unit) {
        val from = ethereum.selectedAddress ?: ""
        val to = "0x0000000000000000000000000000000000000000"
        val amount = "0x01"
        val params: Map<String, Any> = mapOf(
            "from" to from,
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
                callback(result.message)
            } else {
                Logger.log("Ethereum transaction result: $result")
                callback(result)
            }
        }
    }

    fun switchChain(source: Network, destination: Network) {

    }
}