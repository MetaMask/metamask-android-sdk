package io.metamask.androidsdk

enum class Network(val chainId: String) {
    ETHEREUM("0x1"),
    LINEAR("0xe708"),
    POLYGON("0x89"),
    AVALANCHE("0xa86a"),
    FANTOM_OPERA("0xfa"),
    BNB_SMART_CHAIN("0x38"),
    GOERLI("0x5"),
    KOVAN("0x2a");

    companion object {
        fun name(network: Network?): String {
            return when(network) {
                ETHEREUM -> "Ethereum"
                LINEAR -> "Linear"
                POLYGON -> "Polygon"
                AVALANCHE -> "Avalanche"
                FANTOM_OPERA -> "Fantom Opera"
                BNB_SMART_CHAIN -> "BNB Smart Chain"
                GOERLI -> "Goerli Testnet"
                KOVAN -> "Kovan Testnet"
                null -> {
                    ""
                }
            }
        }

        fun fromChainId(chainId: String): Network? {
            for (network in values()) {
                if (network.chainId == chainId) {
                    return network
                }
            }
            return null
        }

        fun rpcUrls(network: Network?): List<String> {
            return when(network) {
                POLYGON -> listOf("https://polygon-rpc.com")
                FANTOM_OPERA -> listOf("https://rpc.ftm.tools/")
                AVALANCHE -> listOf("https://api.avax.network/ext/bc/C/rpc")
                BNB_SMART_CHAIN -> listOf("https://bsc-dataseed1.binance.org")
                else -> {
                    listOf()
                }
            }
        }

        fun chainNameFor(chainId: String): String {
            val network = enumValues<Network>()
                .toList()
                .firstOrNull { it.chainId == chainId }

            return name(network)
        }
    }
}