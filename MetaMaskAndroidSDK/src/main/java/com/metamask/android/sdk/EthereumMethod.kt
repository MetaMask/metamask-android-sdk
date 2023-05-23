package com.metamask.android.sdk

enum class EthereumMethod(val value: String) {
    ETHSIGN("eth_sign"),
    WEB3SHA("web3_sha3"),
    ETHCALL("eth_call"),
    ETHCHAINID("eth_chainId"),
    ETHGETCODE("eth_getCode"),
    ETHACCOUNTS("eth_accounts"),
    ETHGASPRICE("eth_gasPrice"),
    PERSONALSIGN("personal_sign"),
    ETHGETBALANCE("eth_getBalance"),
    WATCHASSET("wallet_watchAsset"),
    ETHBLOCKNUMBER("eth_blockNumber"),
    ETHESTIMATEGAS("eth_estimateGas"),
    ETHGETSTORAGEAT("eth_getStorageAt"),
    ETHSIGNTYPEDDATA("eth_signTypedData"),
    ETHGETBLOCKBYHASH("eth_getBlockByHash"),
    WEB3CLIENTVERSION("web3_clientVersion"),
    ETHREQUESTACCOUNTS("eth_requestAccounts"),
    ETHSIGNTRANSACTION("eth_signTransaction"),
    ETHSENDTRANSACTION("eth_sendTransaction"),
    ETHSIGNTYPEDDATAV3("eth_signTypedData_v3"),
    ETHSIGNTYPEDDATAV4("eth_signTypedData_v4"),
    ADDETHEREUMCHAIN("wallet_addEthereumChain"),
    METAMASKCHAINCHANGED("metamask_chainChanged"),
    ETHSENDRAWTRANSACTION("eth_sendRawTransaction"),
    SWITCHETHEREUMCHAIN("wallet_switchEthereumChain"),
    ETHGETTRANSACTIONCOUNT("eth_getTransactionCount"),
    METAMASKACCOUNTSCHANGED("metamask_accountsChanged"),
    ETHGETTRANSACTIONBYHASH("eth_getTransactionByHash"),
    ETHGETTRANSACTIONRECEIPT("eth_getTransactionReceipt"),
    GETMETAMASKPROVIDERSTATE("metamask_getProviderState"),
    ETHGETBLOCKTRANSACTIONCOUNTBYHASH("eth_getBlockTransactionCountByHash"),
    ETHGETBLOCKTRANSACTIONCOUNTBYNUMBER("eth_getBlockTransactionCountByNumber"),
    UNKNOWN("unknown");

    companion object {
        fun hasMethod(method: String): Boolean {
            return enumValues<EthereumMethod>()
                .toList()
                .map { it.value }
                .contains(method)
        }

        fun requiresAuthorisation(method: EthereumMethod): Boolean {
            val authorisationMethods: List<EthereumMethod> = listOf(
                ETHSIGN, WATCHASSET, PERSONALSIGN,
                ADDETHEREUMCHAIN, SWITCHETHEREUMCHAIN,
                ETHSENDTRANSACTION, ETHREQUESTACCOUNTS,
                ETHSIGNTYPEDDATA, ETHSIGNTYPEDDATAV3, ETHSIGNTYPEDDATAV4
            )
            return authorisationMethods.contains(method)
        }

        fun requiresAuthorisation(method: String): Boolean {
            val authorisationMethods: List<String> = listOf(
                ETHSIGN, WATCHASSET, PERSONALSIGN,
                ADDETHEREUMCHAIN, SWITCHETHEREUMCHAIN,
                ETHSENDTRANSACTION, ETHREQUESTACCOUNTS,
                ETHSIGNTYPEDDATA, ETHSIGNTYPEDDATAV3, ETHSIGNTYPEDDATAV4
            ).map { it.value }

            return authorisationMethods.contains(method)
        }

        fun isResultMethod(method: EthereumMethod): Boolean {
            val resultMethods: List<EthereumMethod> = listOf(
                ETHSIGN, ETHCHAINID, PERSONALSIGN,
                ADDETHEREUMCHAIN, SWITCHETHEREUMCHAIN,
                ETHSIGNTRANSACTION, ETHSENDTRANSACTION,
                WATCHASSET, ETHREQUESTACCOUNTS, GETMETAMASKPROVIDERSTATE,
                ETHSIGNTYPEDDATA, ETHSIGNTYPEDDATAV3, ETHSIGNTYPEDDATAV4,
            )
            return resultMethods.contains(method)
        }

        fun isResultMethod(method: String): Boolean {
            val resultMethods: List<String> = listOf(
                ETHSIGN, ETHCHAINID, PERSONALSIGN,
                ADDETHEREUMCHAIN, SWITCHETHEREUMCHAIN,
                ETHSIGNTRANSACTION, ETHSENDTRANSACTION,
                WATCHASSET, ETHREQUESTACCOUNTS, GETMETAMASKPROVIDERSTATE,
                ETHSIGNTYPEDDATA, ETHSIGNTYPEDDATAV3, ETHSIGNTYPEDDATAV4,
            ).map { it.value }
            return resultMethods.contains(method)
        }
    }
}