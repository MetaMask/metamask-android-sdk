package io.metamask.androidsdk

import org.json.JSONObject

open class ReadOnlyRPCProvider(private val infuraAPIKey: String?, readonlyRPCMap: Map<String, String>?, private val logger: Logger = DefaultLogger) {
    val rpcUrls: Map<String, String> = when {
        readonlyRPCMap != null && infuraAPIKey != null -> {
            // Merge infuraReadonlyRPCMap with readonlyRPCMap, overriding infura's keys if they are present in readonlyRPCMap
            val mergedMap = infuraReadonlyRPCMap(infuraAPIKey).toMutableMap()
            mergedMap.putAll(readonlyRPCMap)
            mergedMap
        }
        readonlyRPCMap != null -> readonlyRPCMap
        infuraAPIKey != null -> infuraReadonlyRPCMap(infuraAPIKey)
        else -> emptyMap()
    }

    fun supportsChain(chainId: String): Boolean {
        return !rpcUrls[chainId].isNullOrEmpty()
    }

    fun infuraReadonlyRPCMap(infuraAPIKey: String) : Map<String, String> {
        return mapOf(
            // ###### Ethereum ######
            // Mainnet
            //"0x1" to "https://mainnet.infura.io/v3/${infuraAPIKey}",
    
            // Sepolia 11155111
            "0x2a" to "https://sepolia.infura.io/v3/${infuraAPIKey}",
    
            // ###### Linear ######
            // Mainnet
            "0xe708" to "https://linea-mainnet.infura.io/v3/${infuraAPIKey}",
            // Goerli Testnet
            "0xe704" to "https://linea-goerli.infura.io/v3/${infuraAPIKey}",
    
            // ###### Polygon ######
            // Mainnet
            "0x89" to "https://polygon-mainnet.infura.io/v3/${infuraAPIKey}",
            // Mumbai
            "0x13881" to "https://polygon-mumbai.infura.io/v3/${infuraAPIKey}",
            // ###### Optimism ######
            // Mainnet
            "0x45" to "https://optimism-mainnet.infura.io/v3/${infuraAPIKey}",
            // Goerli
            "0x1a4" to "https://optimism-goerli.infura.io/v3/${infuraAPIKey}",
            // ###### Arbitrum ######
            // Mainnet
            "0xa4b1" to "https://arbitrum-mainnet.infura.io/v3/${infuraAPIKey}",
            // Goerli
            "0x66eed" to "https://arbitrum-goerli.infura.io/v3/${infuraAPIKey}",
            // ###### Palm ######
            // Mainnet
            "0x2a15c308d" to "https://palm-mainnet.infura.io/v3/${infuraAPIKey}",
            // Testnet
            "0x2a15c3083" to "https://palm-testnet.infura.io/v3/${infuraAPIKey}",
            // ###### Avalanche C-Chain ######
            // Mainnet
            "0xa86a" to "https://avalanche-mainnet.infura.io/v3/${infuraAPIKey}",
            // Fuji
            "0xa869" to "https://avalanche-fuji.infura.io/v3/${infuraAPIKey}",
            // ###### NEAR ######
            // // Mainnet
            // "0x4e454152" to "https://near-mainnet.infura.io/v3/${infuraAPIKey}",
            // // Testnet
            // "0x4e454153" to "https://near-testnet.infura.io/v3/${infuraAPIKey}",
            // ###### Aurora ######
            // Mainnet
            "0x4e454152" to "https://aurora-mainnet.infura.io/v3/${infuraAPIKey}",
            // Testnet
            "0x4e454153" to "https://aurora-testnet.infura.io/v3/${infuraAPIKey}",
            // ###### StarkNet ######
            // Mainnet
            "0x534e5f4d41494e" to "https://starknet-mainnet.infura.io/v3/${infuraAPIKey}",
            // Goerli
            "0x534e5f474f45524c49" to "https://starknet-goerli.infura.io/v3/${infuraAPIKey}",
            // Goerli 2
            "0x534e5f474f45524c4932" to "https://starknet-goerli2.infura.io/v3/${infuraAPIKey}",
            // ###### Celo ######
            // Mainnet
            "0xa4ec" to "https://celo-mainnet.infura.io/v3/${infuraAPIKey}",
            // Alfajores Testnet
            "0xaef3" to "https://celo-alfajores.infura.io/v3/${infuraAPIKey}",
        )
    }

    open fun makeRequest(request: RpcRequest, chainId: String, dappMetadata: DappMetadata, callback: ((Result) -> Unit)?) {
        val httpClient = HttpClient()

        val devicePlatformInfo = DeviceInfo.platformDescription
        val headers = mapOf(
            "Metamask-Sdk-Info" to "Sdk/Android SdkVersion/${SDKInfo.VERSION} Platform/$devicePlatformInfo dApp/${dappMetadata.url} dAppTitle/${dappMetadata.name}"
        )
        httpClient.addHeaders(headers)

        val params: MutableMap<String, Any> = mutableMapOf()
        params["method"] = request.method
        params["jsonrpc"] = "2.0"
        params["id"] = request.id
        params["params"] = request.params ?: listOf<String>()

        val endpoint = rpcUrls[chainId]
        if (endpoint == null) {
            callback?.invoke(Result.Error(RequestError(-1, "There is no defined network for chainId $chainId, please provide it via readonlyRPCMap")))
            return
        }

        httpClient.newCall(endpoint, parameters = params) { response, ioException ->
            if (response != null) {
                logger.log("InfuraProvider:: response $response")
                try {
                    val result = JSONObject(response).optString("result") ?: ""
                    callback?.invoke(Result.Success.Item(result))
                } catch (e: Exception) {
                    logger.error("InfuraProvider:: error: ${e.message}")
                    callback?.invoke(Result.Error(RequestError(-1, response)))
                }
            } else if (ioException != null) {
                callback?.invoke(Result.Success.Item(ioException.message ?: ""))
            }
        }
    }
}