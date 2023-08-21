package io.metamask.androidsdk

enum class EthereumMethod(val value: String) {
    ETH_SIGN("eth_sign"),
    WEB3_SHA("web3_sha3"),
    ETH_CALL("eth_call"),
    ETH_CHAIN_ID("eth_chainId"),
    ETH_GET_CODE("eth_getCode"),
    ETH_ACCOUNTS("eth_accounts"),
    ETH_GAS_PRICE("eth_gasPrice"),
    PERSONAL_SIGN("personal_sign"),
    ETH_GET_BALANCE("eth_getBalance"),
    WATCH_ASSET("wallet_watchAsset"),
    ETH_BLOCK_NUMBER("eth_blockNumber"),
    ETH_ESTIMATE_GAS("eth_estimateGas"),
    ETH_GET_STORAGE_AT("eth_getStorageAt"),
    ETH_SIGN_TYPED_DATA("eth_signTypedData"),
    ETH_GET_BLOCK_BY_HASH("eth_getBlockByHash"),
    WEB3_CLIENT_VERSION("web3_clientVersion"),
    ETH_REQUEST_ACCOUNTS("eth_requestAccounts"),
    ETH_SIGN_TRANSACTION("eth_signTransaction"),
    ETH_SEND_TRANSACTION("eth_sendTransaction"),
    ETH_SIGN_TYPED_DATA_V3("eth_signTypedData_v3"),
    ETH_SIGN_TYPED_DATA_V4("eth_signTypedData_v4"),
    ADD_ETHEREUM_CHAIN("wallet_addEthereumChain"),
    METAMASK_CHAIN_CHANGED("metamask_chainChanged"),
    ETH_SEND_RAW_TRANSACTION("eth_sendRawTransaction"),
    SWITCH_ETHEREUM_CHAIN("wallet_switchEthereumChain"),
    ETH_GET_TRANSACTION_COUNT("eth_getTransactionCount"),
    METAMASK_ACCOUNTS_CHANGED("metamask_accountsChanged"),
    ETH_GET_TRANSACTION_BY_HASH("eth_getTransactionByHash"),
    ETH_GET_TRANSACTION_RECEIPT("eth_getTransactionReceipt"),
    GET_METAMASK_PROVIDER_STATE("metamask_getProviderState"),
    ETH_GET_BLOCK_TRANSACTION_COUNT_BY_HASH("eth_getBlockTransactionCountByHash"),
    ETH_GET_BLOCK_TRANSACTION_COUNT_BY_NUMBER("eth_getBlockTransactionCountByNumber"),
    UNKNOWN("unknown");

    companion object {
        fun hasMethod(method: String): Boolean {
            return enumValues<EthereumMethod>()
                .toList()
                .map { it.value }
                .contains(method)
        }

        fun requiresAuthorisation(method: String): Boolean {
            val authorisationMethods: List<String> = listOf(
                ETH_SIGN, WATCH_ASSET, PERSONAL_SIGN,
                ADD_ETHEREUM_CHAIN, SWITCH_ETHEREUM_CHAIN,
                ETH_SEND_TRANSACTION, ETH_REQUEST_ACCOUNTS,
                ETH_SIGN_TYPED_DATA, ETH_SIGN_TYPED_DATA_V3, ETH_SIGN_TYPED_DATA_V4
            ).map { it.value }

            return authorisationMethods.contains(method)
        }

        fun isResultMethod(method: String): Boolean {
            val resultMethods: List<String> = listOf(
                ETH_SIGN, ETH_CHAIN_ID, PERSONAL_SIGN,
                ADD_ETHEREUM_CHAIN, SWITCH_ETHEREUM_CHAIN,
                ETH_SIGN_TRANSACTION, ETH_SEND_TRANSACTION,
                WATCH_ASSET, ETH_REQUEST_ACCOUNTS, GET_METAMASK_PROVIDER_STATE,
                ETH_SIGN_TYPED_DATA, ETH_SIGN_TYPED_DATA_V3, ETH_SIGN_TYPED_DATA_V4,
            ).map { it.value }
            return resultMethods.contains(method)
        }
    }
}