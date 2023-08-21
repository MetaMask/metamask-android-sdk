package io.metamask.androidsdk

enum class ErrorType(val code: Int) {
    // Ethereum Provider
    USER_REJECTED_REQUEST(4001), // Ethereum Provider User Rejected Request
    UNAUTHORISED_REQUEST(4100), // Ethereum Provider User Rejected Request
    UNSUPPORTED_METHOD(4200), // Ethereum Provider Unsupported Method
    DISCONNECTED(4900), // Ethereum Provider Not Connected
    CHAIN_DISCONNECTED(4901), // Ethereum Provider Chain Not Connected
    UNRECOGNIZED_CHAIN_ID(4902), // Unrecognized chain ID. Try adding the chain using wallet_ADD_ETHEREUM_CHAIN first

    // Ethereum RPC
    INVALID_INPUT(-32000), // JSON RPC 2.0 Server error
    TRANSACTION_REJECTED(-32003), // Ethereum JSON RPC Transaction Rejected
    INVALID_REQUEST(-32600), // JSON RPC 2.0 Invalid Request
    INVALID_METHOD_PARAMETERS(-32602), // JSON RPC 2.0 Invalid Parameters
    SERVER_ERROR(-32603), // Could be one of many outcomes
    PARSE_ERROR(-32700), // JSON RPC 2.0 Parse error
    UNKNOWN_ERROR(-1); // Check RequestError.code instead

    companion object {
        fun message(code: Int): String {

            return when(values().firstOrNull { it.code == code }) {
                USER_REJECTED_REQUEST -> "User rejected request"
                UNAUTHORISED_REQUEST -> "Request not authorised"
                UNSUPPORTED_METHOD -> "Ethereum provider unsupported method"
                DISCONNECTED -> "Ethereum provider not connected"
                CHAIN_DISCONNECTED -> "Ethereum provider chain not connected"
                UNRECOGNIZED_CHAIN_ID -> "Unrecognized chain ID. Try adding the chain using ADD_ETHEREUM_CHAIN first"
                INVALID_INPUT -> "JSON RPC 2.0 Server error"
                TRANSACTION_REJECTED -> "Ethereum transaction rejected"
                INVALID_METHOD_PARAMETERS -> "JSON RPC 2.0 invalid parameters"
                INVALID_REQUEST -> "Invalid request"
                SERVER_ERROR -> "Server error"
                PARSE_ERROR -> "Parse error"
                else -> "The request failed"
            }
        }
    }
}