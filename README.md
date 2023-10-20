# MetaMask Android SDK
![Maven Central](https://img.shields.io/maven-central/v/io.metamask.androidsdk/metamask-android-sdk)

The MetaMask Android SDK enables developers to connect their native Android apps to the Ethereum blockchain via the MetaMask Mobile wallet, effectively enabling the creation of Android native decentralised applications (Dapps).

## Getting Started
You can import the MetaMask Android SDK into your native Android app to enable users to easily connect with their MetaMask Mobile wallet. Refer to the [MetaMask API Reference](https://docs.metamask.io/wallet/reference/provider-api) for more information.

### 1. Add MetaMask Android SDK dependency

#### MavenCentral
![Maven Central](https://img.shields.io/maven-central/v/io.metamask.androidsdk/metamask-android-sdk)  

To add MetaMask Android SDK from Maven as a dependency to your project, add this entry in your `app/build.gradle` file's dependencies block:
```groovy
dependencies {
    implementation "io.metamask.androidsdk:metamask-android-sdk:0.2.1"
}
```
And then sync your project with the gradle settings. Once the syncing has completed, you can now start using the library by first importing it.

<b>Please note that this SDK requires MetaMask Mobile version 7.6.0 or higher</b>.

### 2. Import the SDK
Now you can import the SDK and start using it.
```kotlin
import io.metamask.androidsdk.Ethereum
```

### 3. Connect your Dapp
You can:<br>
a) Use `Ethereum` directly or <br>
b) Create a ViewModel that injects `Ethereum` and then use that ViewModel. <br><br>
Option `(a)` is recommended when interacting with the SDK within a pure model layer.
Option `(b)` is convenient at app level as it provides a single instance that will be shared across all views and will survive configuration changes.

#### 3.1 Using Ethereum directly
```kotlin
class SomeModel(private val context: Context) {
    val ethereum = Ethereum(context)
    
    val dapp = Dapp("Droid Dapp", "https://droiddapp.com")
    
    ethereum.connect(dapp) { result ->
        if (result is RequestError) {
            Log.e(TAG, "Ethereum connection error: ${result.message}")
        } else {
            Log.d(TAG, "Ethereum connection result: $result")
        }
    }
}
```

#### 3.2 Using a view model
Dependency injection managers like Hilt provide a great convenience by initialising the view model and maintaining its state, ensuring that state is retained between configuration changes.

All you have to do is to create a ViewModel that injects Ethereum and then add wrapper methods for the ethereum methods you wish to use. See EthereumViewModel in the example dapp in [src](./app/src) for a comprehensive usage example. If using Hilt, your setup may look like this:

```kotlin
@HiltViewModel
class EthereumViewModel @Inject constructor(
    private val ethereum: Ethereum
): ViewModel() {

    // See the example app on how to have your composables only work with state using ethereumState instead of having a viewmodel dependency 
    val ethereumState = MediatorLiveData<EthereumState>().apply {
        addSource(ethereum.ethereumState) { newEthereumState ->
            value = newEthereumState
        }
    }

    // wrap the ethereum connect method
    fun connect(dapp: Dapp, callback: ((Any?) -> Unit)?) {
        ethereum.connect(dapp, callback)
    }

    // wrap the ethereum sendRequest method for making all RPC requests
    fun sendRequest(request: EthereumRequest, callback: ((Any?) -> Unit)?) {
        ethereum.sendRequest(request, callback)
    }
}
```

Usage:

```kotlin
val ethereumViewModel: EthereumViewModel by viewModels()

val dapp = Dapp("Droid Dapp", "https://droiddapp.com")

ethereumViewModel.connect(dapp) { result ->
    if (result is RequestError) {
        Log.e(TAG, "Ethereum connection error: ${result.message}")
    } else {
        Log.d(TAG, "Ethereum connection result: $result")
    }
}
```

### 4. Make RPC calls

#### Example 1: Get account balance
```kotlin
var balance: String? = null

// Create parameters
val params: List<String> = listOf(
    ethereum.selectedAddress,
    "latest" // "latest", "earliest" or "pending" (optional)
)

// Create request
val getBalanceRequest = EthereumRequest(
    method = EthereumMethod.ETH_GET_BALANCE.value,
    params = params
)

// Make request
ethereum.sendRequest(getBalanceRequest) { result ->
    if (result is RequestError) {
        // handle error
    } else {
        balance = result
    }
}
```
#### Example 2: Sign message
```kotlin
val message = "{\"domain\":{\"chainId\":\"${ethereum.chainId}\",\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"

val from = ethereum.selectedAddress
val params: List<String> = listOf(from, message)

val signRequest = EthereumRequest(
    method = EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
    params = params
)

ethereum.sendRequest(signRequest) { result ->
    if (result is RequestError) {
        Log.e(TAG, "Ethereum sign error: ${result.message}")
    } else {
        Log.d(TAG, "Ethereum sign result: $result")
    }
}
```

#### Example 3: Send transaction

```kotlin
// Create parameters
val from = ethereum.selectedAddress
val to = "0x0000000000000000000000000000000000000000"
val amount = "0x01"
val params: Map<String, Any> = mapOf(
    "from" to from,
    "to" to to,
    "amount" to amount
)

// Create request
val transactionRequest = EthereumRequest(
    method = EthereumMethod.ETH_SEND_TRANSACTION.value,
    params = listOf(params)
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

#### Example 4: Switch chain
```kotlin

fun switchChain(
    chainId: String,
    onSuccess: (message: String) -> Unit,
    onError: (message: String, action: (() -> Unit)?) -> Unit
) {
    val switchChainParams: Map<String, String> = mapOf("chainId" to chainId)
    val switchChainRequest = EthereumRequest(
        method = EthereumMethod.SWITCH_ETHEREUM_CHAIN.value,
        params = listOf(switchChainParams)
    )

    ethereum.sendRequest(switchChainRequest) { result ->
        if (result is RequestError) {
            if (result.code == ErrorType.UNRECOGNIZED_CHAIN_ID.code || result.code == ErrorType.SERVER_ERROR.code) {
                val message = "${Network.chainNameFor(chainId)} ($chainId) has not been added to your MetaMask wallet. Add chain?"

                val action: () -> Unit = {
                    addEthereumChain(
                        chainId,
                        onSuccess = { result ->
                            onSuccess(result)
                        },
                        onError = { error ->
                            onError(error, null)
                        }
                    )
                }
                onError(message, action)
            } else {
                onError("Switch chain error: ${result.message}", null)
            }
        } else {
            onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
        }
    }
}

private fun addEthereumChain(
    chainId: String,
    onSuccess: (message: String) -> Unit,
    onError: (message: String) -> Unit
) {
    Log.d(TAG, "Adding chainId: $chainId")

    val addChainParams: Map<String, Any> = mapOf(
        "chainId" to chainId,
        "chainName" to Network.chainNameFor(chainId),
        "rpcUrls" to Network.rpcUrls(Network.fromChainId(chainId))
    )
    val addChainRequest = EthereumRequest(
        method = EthereumMethod.ADD_ETHEREUM_CHAIN.value,
        params = listOf(addChainParams)
    )

    ethereum.sendRequest(addChainRequest) { result ->
        if (result is RequestError) {
            onError("Add chain error: ${result.message}")
        } else {
            if (chainId == ethereum.chainId) {
                onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
            } else {
                onSuccess("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
            }
        }
    }
}
```

## Examples
See the [app](./app/) directory for an example dapp integrating the SDK, to act as a guide on how to connect to ethereum and make requests.

## Logging

We only log three SDK events: `connection_request`, `connected` and `disconnected`. This helps us to debug and monitor the SDK connection quality. If you wish to disable this, you can do so by setting `ethereum.enableDebug = false`.

## Requirements
### MetaMask Mobile
This SDK requires MetaMask Mobile version 7.6.0 or higher.

### Environment
You will need to have MetaMask Mobile wallet installed on your target device i.e physical device or emulator, so you can either have it installed from the [Google Play](https://play.google.com/store/apps/details?id=io.metamask), or clone and compile MetaMask Mobile wallet from [source](https://github.com/MetaMask/metamask-mobile) and build to your target device.

### Hardware
This SDK has an Minimum Android SDK (minSdk) version requirement of 23.

## Resources
Refer to the [MetaMask API Reference](https://docs.metamask.io/wallet/reference/provider-api) for more information.