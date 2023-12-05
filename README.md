# MetaMask Android SDK
![Maven Central](https://img.shields.io/maven-central/v/io.metamask.androidsdk/metamask-android-sdk)

Import MetaMask SDK into your native Android dapp to enable your users to easily connect with their
MetaMask Mobile wallet.

See the following for more information:

- [Example Android dapp](app)
- Documentation for [setting up the SDK in your Android dapp](https://docs.metamask.io/wallet/how-to/connect/set-up-sdk/mobile/android/)
- Documentation for the [Android SDK architecture](https://docs.metamask.io/wallet/concepts/sdk/android/)

You can also see the [JavaScript SDK repository](https://github.com/MetaMask/metamask-sdk) and the
[iOS SDK repository](https://github.com/MetaMask/metamask-ios-sdk).

## Prerequisites

- MetaMask Mobile version 7.6.0 or above installed on your target device (that is, a physical
  device or emulator).
  You can install MetaMask Mobile from [Google Play](https://play.google.com/store/apps/details?id=io.metamask),
  or clone and compile MetaMask Mobile from [source](https://github.com/MetaMask/metamask-mobile)
  and build to your target device.
- Android SDK version 23 or above.

## Get started

### 1. Install the SDK

To add the SDK from Maven Central as a dependency to your project, in your `app/build.gradle` file,
add the following entry to the `dependencies` block:

```gradle title="build.gradle"
dependencies {
    implementation 'io.metamask.androidsdk:metamask-android-sdk:0.2.1'
}
```

Then, sync your project with the Gradle settings.
Once the syncing completes, you can set up the rest of your project.

### 2. Import the SDK

Import the SDK by adding the following line to the top of your project file:

```kotlin
import io.metamask.androidsdk.Ethereum
```

### 3. Connect your dapp

You can connect your dapp to MetaMask in one of two ways:

1. [Use the `ethereum` provider object directly](#31-use-the-provider-object-directly).
   We recommend using this method in a pure model layer.
2. [Use a ViewModel](#32-use-a-viewmodel) that injects the `ethereum` provider object.
   We recommend using this method at the app level, because it provides a single instance that
   survives configuration changes and can be shared across all views.

> **Note:**
> By default, MetaMask logs three SDK events: `connection_request`, `connected`, and `disconnected`.
> This allows MetaMask to monitor any SDK connection issues.
> To disable this, set `ethereum.enableDebug = false`.

#### 3.1. Use the provider object directly

Use the `ethereum` provider object directly to connect your dapp to MetaMask by adding the following
code to your project file:

```kotlin
@AndroidEntryPoint
class SomeModel(private val repository: ApplicationRepository) {
    
    val dappMetadata = DappMetadata("Droid Dapp", "https://droiddapp.com")
    val ethereum = Ethereum(context, dappMetadata)

    // This is the same as calling eth_requestAccounts
    ethereum.connect() { result ->
        when (result) {
            is Result.Error -> {
                Logger.log("Ethereum connection error: ${result.error.message}")
                onError(result.error.message)
            }
            is Result.Success -> {
                Logger.log("Ethereum connection result: $result")
                onSuccess()
            }
        }
    }
}
```

#### 3.2. Use a ViewModel

To connect your dapp to MetaMask using a ViewModel, create a ViewModel that injects the
`ethereum` provider object, then add wrapper functions for each Ethereum method you wish to call.

You can use a dependency manager such as [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
to initialize the ViewModel and maintain its state across configuration changes.
If you use Hilt, your setup might look like the following:

```kotlin title="EthereumViewModel.kt"
@HiltViewModel
class EthereumViewModel @Inject constructor(
        private val ethereum: Ethereum
): ViewModel() {

  val ethereumState = MediatorLiveData<EthereumState>().apply {
    addSource(ethereum.ethereumState) { newEthereumState ->
      value = newEthereumState
    }
  }

  // Wrapper function to connect the dapp
  fun connect(callback: ((Any?) -> Unit)?) {
    ethereum.connectcallback)
  }
  
  // Wrapper function call all RPC methods
  fun sendRequest(request: EthereumRequest, callback: ((Any?) -> Unit)?) {
    ethereum.sendRequest(request, callback)
  }
}
```

To use the ViewModel, add the following code to your project file:

```kotlin
val ethereumViewModel: EthereumViewModel by viewModels()

// This is the same as calling eth_requestAccounts
ethereumViewModel.connect()
```

See the example dapp's
[`EthereumViewModel.kt`](app/src/main/java/com/metamask/dapp/EthereumViewModel.kt) file for more information.

### 4. Call methods

You can now call any [JSON-RPC API method](https://docs.metamask.io/wallet/reference/eth_subscribe/)
using `ethereum.sendRequest()`.

#### Example: Get account balance

The following example gets the user's account balance by calling
[`eth_getBalance`](https://docs.metamask.io/wallet/reference/eth_getbalance/).

```kotlin
var balance: String? = null

// Create parameters
val params: List<String> = listOf(
    ethereum.selectedAddress,
    "latest" // "latest", "earliest" or "pending" (optional)
    )

// Create request
val getBalanceRequest = EthereumRequest(
    method = EthereumMethod.ETHGETBALANCE.value,
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

#### Example: Sign message

The following example requests the user sign a message by calling
[`eth_signTypedData_v4`](https://docs.metamask.io/wallet/reference/eth_signtypeddata_v4/).

```kotlin
val message = "{\"domain\":{\"chainId\":\"${ethereum.chainId}\",\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"

val from = ethereum.selectedAddress
val params: List<String> = listOf(from, message)

val signRequest = EthereumRequest(
    method = EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
    params = params
)

ethereum.sendRequest(signRequest) { result ->
    when (result) {
        is Result.Error -> {
            Logger.log("Ethereum sign error: ${result.error.message}")
            onError(result.error.message)
        }
        is Result.Success.Item -> {
            Logger.log("Ethereum sign result: $result")
            onSuccess(result.value)
        }
        else -> {}
    }
}
```

#### Example: Request batching

The following example requests the user to personal sign a batch of messages each of
[`personal_sign`](https://docs.metamask.io/wallet/reference/personal_sign/).

```kotlin
val messages: List<String> = listOf("First message", "Second message", "Last message")
val requestBatch: MutableList<EthereumRequest> = mutableListOf()

for (message in messages) {
    val params: List<String> = listOf(address, message)
    val ethereumRequest = EthereumRequest(
        method = EthereumMethod.PERSONAL_SIGN.value,
        params = params
    )
    requestBatch.add(ethereumRequest)
}

ethereum.sendRequestBatch(requestBatch) { result ->
    when (result) {
        is Result.Error -> {
            Logger.log("Ethereum batch sign error: ${result.error.message}")
            onError(result.error.message)
        }
        is Result.Success.Items -> {
            Logger.log("Ethereum batch sign result: $result")
            onSuccess(result.value)
        }
        else -> {}
    }
}
```

#### Example: Send transaction

The following example sends a transaction by calling
[`eth_sendTransaction`](https://docs.metamask.io/wallet/reference/eth_sendtransaction/).

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

#### Example: Switch chain

The following example switches to a new Ethereum chain by calling
[`wallet_switchEthereumChain`](https://docs.metamask.io/wallet/reference/wallet_switchethereumchain/)
and [`wallet_addEthereumChain`](https://docs.metamask.io/wallet/reference/wallet_addethereumchain/).

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
        when (result) {
            is Result.Error -> {
                if (result.error.code == ErrorType.UNRECOGNIZED_CHAIN_ID.code || result.error.code == ErrorType.SERVER_ERROR.code) {
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
                    onError("Switch chain error: ${result.error.message}", null)
                }
            }
            is Result.Success -> {
                onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
            }
        }
    }
}

private fun addEthereumChain(
    chainId: String,
    onSuccess: (message: String) -> Unit,
    onError: (message: String) -> Unit
) {
    Logger.log("Adding chainId: $chainId")

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
        when (result) {
            is Result.Error -> {
                onError("Add chain error: ${result.error.message}")
            }
            is Result.Success -> {
                if (chainId == ethereum.chainId) {
                    onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
                } else {
                    onSuccess("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
                }
            }
        }
    }
}
```
