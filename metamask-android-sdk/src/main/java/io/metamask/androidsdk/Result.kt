package io.metamask.androidsdk

// sealed class Result {
//     sealed class Success: Result() {
//         data class Item(val value: String): Success()
//         data class Items(val value: List<String>): Success()
//         data class ItemMap(val value: Map<String, Any?>): Success()
//     }

//     data class Error(val error: RequestError): Result()
// }

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Error(val error: RequestError) : Result<Nothing>()
}
