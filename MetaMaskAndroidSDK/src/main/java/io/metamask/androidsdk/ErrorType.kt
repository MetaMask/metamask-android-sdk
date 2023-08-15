package io.metamask.androidsdk

enum class ErrorType(val code: Int) {
    // Ethereum Provider
    USERREJECTEDREQUEST(4001), // Ethereum Provider User Rejected Request
    UNAUTHORISEDREQUEST(4100), // Ethereum Provider User Rejected Request
    UNSUPPORTEDMETHOD(4200), // Ethereum Provider Unsupported Method
    DISCONNECTED(4900), // Ethereum Provider Not Connected
    CHAINDISCONNECTED(4901), // Ethereum Provider Chain Not Connected
    UNRECOGNIZEDCHAINID(4902), // Unrecognized chain ID. Try adding the chain using wallet_addEthereumChain first

    // Ethereum RPC
    INVALIDINPUT(-32000), // JSON RPC 2.0 Server error
    TRANSACTIONREJECTED(-32003), // Ethereum JSON RPC Transaction Rejected
    INVALIDREQUEST(-32600), // JSON RPC 2.0 Invalid Request
    INVALIDMETHODPARAMETERS(-32602), // JSON RPC 2.0 Invalid Parameters
    SERVERERROR(-32603), // Could be one of many outcomes
    PARSEERROR(-32700), // JSON RPC 2.0 Parse error
    UNKNOWNERROR(-1); // Check RequestError.code instead

    companion object {
        fun message(code: Int): String {

            return when(ErrorType.values().first { it.code == code }) {
                USERREJECTEDREQUEST -> "Ethereum Provider User Rejected Request"
                UNAUTHORISEDREQUEST -> "Ethereum Provider User Rejected Request"
                UNSUPPORTEDMETHOD -> "Ethereum Provider Unsupported Method"
                DISCONNECTED -> "Ethereum Provider Not Connected"
                CHAINDISCONNECTED -> "Ethereum Provider Chain Not Connected"
                UNRECOGNIZEDCHAINID -> "Unrecognized chain ID. Try adding the chain using addEthereumChain first"
                INVALIDINPUT -> "JSON RPC 2.0 Server error"
                TRANSACTIONREJECTED -> "Ethereum Transaction Rejected"
                INVALIDREQUEST -> "Invalid Request"
                SERVERERROR -> "Server error"
                PARSEERROR -> "Parse error"
                else -> "The request failed"
            }
        }
    }
}