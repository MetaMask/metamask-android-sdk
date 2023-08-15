package io.metamask.androidsdk

enum class Network(val chainId: String) {
    GOERLI("0x5"),
    KOVAN("0x2a"),
    ETHEREUM("0x1"),
    POLYGON("0x89"),
    LINEAR("0xe708"),
    UNKNOWN("unknown");

    companion object {
        fun name(network: Network?): String {
            return when(network) {
                GOERLI -> "Goerli Testnet"
                KOVAN -> "Kovan Testnet"
                POLYGON -> "Polygon"
                ETHEREUM -> "Ethereum"
                LINEAR -> "Linear"
                else -> {
                    "Unknown chain"
                }
            }
        }

        fun fromChainId(chainId: String): Network {
            for (network in values()) {
                if (network.chainId == chainId) {
                    return network
                }
            }
            return UNKNOWN
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