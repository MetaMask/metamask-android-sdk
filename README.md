# MetaMask Android SDK
The MetaMask Android SDK enables developers to connect their native Android apps to the Ethereum blockchain via the MetaMask Mobile wallet, effectively enabling the creation of Android native decentralised applications (Dapps).

## Getting Started
You can import the MetaMask Android SDK into your native Android app to enable users to easily connect with their MetaMask Mobile wallet. Refer to the [MetaMask API Reference](https://c0f4f41c-2f55-4863-921b-sdk-docs.github.io/guide/rpc-api.html#table-of-contents) to see all the ethereum RPC methods available.

### 1. Install

#### MavenCentral
To add MetaMask Android SDK from Maven as a dependency to your project, add this entry in your `app/build.gradle` file's dependencies block: 
```
dependencies {
  implementation 'io.metamask.androidsdk:sdk:0.1.0'
}

```
And then sync your project with the gradle settings. Once the syncing has completed, you can now start using the library by first importing it.

### 2. Import the SDK
```
import io.metamask.androidsdk
```

### 3. Connect your Dapp
The Ethereum module requires the app context, so you will need to instantiate it from an Activity or a module that injects a context.
```kotlin
// MainActivity

val ethereum: Ethereum by lazy {
    Ethereum.getInstance(this)
}

// We track three events: connection request, connected, disconnected, otherwise no tracking. 
// This helps us to monitor any SDK connection issues. 
//  

let dapp = Dapp(name: "Droid Dapp", url: "https://droiddapp.com")

// This is the same as calling "eth_requestAccounts"
ethereum.connect(dapp) { result ->
    if (result is RequestError) {
        Log.e(TAG, "Ethereum connection error: ${result.message}")
    } else {
        Logger.d(TAG, "Ethereum connection result: $result")
    }
}
```

We log three SDK events: `connectionRequest`, `connected` and `disconnected`. Otherwise no tracking. This helps us to monitor any SDK connection issues. If you wish to disable this, you can do so by setting `MetaMaskSDK.shared.enableDebug = false` or `ethereum.enableDebug = false`.


### 4. You can now call any ethereum provider method

#### Example 1: Get Chain ID
```kotlin
var chainId: String? = null

let chainIdRequest = EthereumRequest(EthereumMethod.ETHCHAINID.value) // or EthereumRequest("eth_chainId")

ethereum.sendRequest(chainIdRequest) { result ->
    if (result is RequestError) {
        // handle error
    } else {
        chainId = result
    }
}
```


#### Example 2: Get account balance
```kotlin
var balance: String? = null

// Create parameters
val params: List<String> = listOf(
    ethereum.selectedAddress, 
    "latest" // "latest", "earliest" or "pending" (optional)
    )

  
// Create request  
let getBalanceRequest = EthereumRequest(
    EthereumMethod.ETHGETBALANCE.value,
    params)

// Make request
ethereum.sendRequest(getBalanceRequest) { result ->
    if (result is RequestError) {
        // handle error
    } else {
        balance = result
    }
}
```

#### Example 3: Send transaction
##### Using parameters dictionary
If your request parameters is a simple dictionary of string key-value pairs, you can use it directly. Note that the use of `Any` or even `AnyHashable` types is not supported as the type needs to be explicitly known.

```kotlin
// Create parameters
val from = ethereum.selectedAddress ?: ""
val to = "0x0000000000000000000000000000000000000000"
val amount = "0x01"
val params: Map<String, Any> = mapOf(
    "from" to from,
    "to" to to,
    "amount" to amount
)

// Create request
val transactionRequest = EthereumRequest(
    EthereumMethod.ETHSENDTRANSACTION.value,
    listOf(params)
)

// Make a transaction request
ethereum.sendRequest(transactionRequest) { result ->
    if (result is RequestError) {
        // handle error
    } else {
        Log.d(TAG, "Ethereum transaction result: $result")
    }
} 
```

## Examples
We have created an `ExampleDapp` class with a few ethereum requests to act as a guide on how to connect to ethereum and make requests. 

## Requirements
### Environment
You will need to have MetaMask Mobile wallet installed on your target device i.e physical device or emulator, so you can either have it installed from the [Google Play](https://play.google.com/store/apps/details?id=io.metamask), or clone and compile MetaMask Mobile wallet from [source](https://github.com/MetaMask/metamask-mobile) and build to your target device. 

### Hardware
This SDK has an Minimum Android SDK (minSdk) version requirement of 23.