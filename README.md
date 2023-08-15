# MetaMask Android SDK
The MetaMask Android SDK enables developers to connect their native Android apps to the Ethereum blockchain via the MetaMask Mobile wallet, effectively enabling the creation of Android native decentralised applications (Dapps).

## Getting Started
You can import the MetaMask Android SDK into your native Android app to enable users to easily connect with their MetaMask Mobile wallet. Refer to the [MetaMask API Reference](https://docs.metamask.io/wallet/reference/provider-api) to see all the ethereum RPC methods available.

### 1. Install

#### MavenCentral
To add MetaMask Android SDK from Maven as a dependency to your project, add this entry in your `app/build.gradle` file's dependencies block: 
```
dependencies {
  implementation 'io.metamask.androidsdk:metamasksdk:0.1.0'
}

```
And then sync your project with the gradle settings. Once the syncing has completed, you can now start using the library by first importing it.

### 2. Import the SDK
```
import io.metamask.androidsdk
```
We use Hilt for Dagger dependency injection, so you will need to add the corresponding dependencies in your `app/build.gradle`:

```
plugins {
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

dependencies {
    // dagger-hilt
    implementation 'com.google.dagger:hilt-android:2.43.2'
    kapt 'com.google.dagger:hilt-compiler:2.43.2'
    
    // viewmodel-related
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1'
}
```
Refer to the example app for more details on how we set up a Jetpack Compose project to work with the SDK.

### 3. Connect your Dapp
The Ethereum module requires the app context, so you will need to instantiate it from an Activity or a module that injects a context.
```kotlin
// MainActivity

// Obtain EthereumViewModel using viewModels()
val ethereum: EthereumViewModel by viewModels()

// We track three events: connection request, connected, disconnected, otherwise no tracking. 
// This helps us to monitor any SDK connection issues. 
//  

val dapp = Dapp(name: "Droid Dapp", url: "https://droiddapp.com")

// This is the same as calling "eth_requestAccounts"
ethereum.connect(dapp) { result ->
    if (result is RequestError) {
        Log.e(TAG, "Ethereum connection error: ${result.message}")
    } else {
        Logger.d(TAG, "Ethereum connection result: $result")
    }
}
```

We log three SDK events: `connectionRequest`, `connected` and `disconnected`. Otherwise no tracking. This helps us to monitor any SDK connection issues. If you wish to disable this, you can do so by setting `ethereum.enableDebug = false`.


### 4. You can now call any ethereum provider method

#### Example 1: Get Chain ID
```kotlin
var chainId: String? = null

val chainIdRequest = EthereumRequest(EthereumMethod.ETHCHAINID.value) // or EthereumRequest("eth_chainId")

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
#### Example 3: Sign message
```kotlin
val message = "{\"domain\":{\"chainId\":1,\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"

val from = ethereum.selectedAddress ?: ""
val params: List<String> = listOf(from, message)

val signRequest = EthereumRequest(
    EthereumMethod.ETHSIGNTYPEDDATAV4.value,
    params
)

ethereum.sendRequest(signRequest) { result ->
    if (result is RequestError) {
        Log.e(TAG, "Ethereum sign error: ${result.message}")
    } else {
        Log.d(TAG, "Ethereum sign result: $result")
    }
}
```

#### Example 4: Send transaction

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

#### Example 5: Switch chain
```kotlin

fun switchToChain(chainId: String, callback: (Any?) -> Unit) {

    if (ethereum.chainId == chainId) {
        // already on requested chain
        return
    }

    val switchChainParams: Map<String, String> = mapOf("chainId" to chainId)
    val switchChainRequest = EthereumRequest(
        method = EthereumMethod.SWITCHETHEREUMCHAIN.value,
        params = listOf(switchChainParams)
    )

    ethereum.sendRequest(switchChainRequest) { result ->
        if (result is RequestError) {
            if (result.code == ErrorType.UNRECOGNIZEDCHAINID.code || result.code == ErrorType.SERVERERROR.code) {
                val message = "$chainId has not been added to your MetaMask wallet. Add chain?"
                val buttonTitle = "OK"
                val action: () -> Unit = {
                    addEthereumChain(chainId, callback)
                }
                showSnackbarWithAction(message, buttonTitle, action)
            } else {
                showToast("Switch chain error: ${result.message}")
            }
        } else {
            showToast("Successfully switched to $chainId")
            callback(chainId)
        }
    }
}

fun addEthereumChain(chainId: String, callback: (Any?) -> Unit) {

    val addChainParams: Map<String, Any> = mapOf(
        "chainId" to chainId,
        "chainName" to Network.chainNameFor(chainId),
        "rpcUrls" to listOf("https://polygon-rpc.com")
    )
    val addChainRequest = EthereumRequest(
        method = EthereumMethod.ADDETHEREUMCHAIN.value,
        params = listOf(addChainParams)
    )

    ethereum.sendRequest(addChainRequest) { result ->
        if (result is RequestError) {
            showToast("Add chain error: ${result.message}")
        } else {
            if (chainId == ethereum.chainId) {
                showToast("Successfully switched to $chainId")
            } else {
                showToast("Successfully added $chainId")
            }
            callback(result)
        }
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