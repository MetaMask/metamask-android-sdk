# MetaMask Android SDK
The MetaMask Android SDK enables developers to connect their native Android apps to the Ethereum blockchain via the MetaMask Mobile wallet, effectively enabling the creation of Android native decentralised applications (Dapps).

## Getting Started
You can import the MetaMask Android SDK into your native Android app to enable users to easily connect with their MetaMask Mobile wallet. Refer to the [MetaMask API Reference](https://docs.metamask.io/wallet/reference/provider-api) for more information.

### 1. Install

#### MavenCentral
To add MetaMask Android SDK from Maven as a dependency to your project, add this entry in your `app/build.gradle` file's dependencies block: 
```
dependencies {
  implementation 'io.metamask.androidsdk:metamask-android-sdk:0.1.2'
}

```
And then sync your project with the gradle settings. Once the syncing has completed, you can now start using the library by first importing it. 

<b>Please note that this SDK requires MetaMask Mobile version 7.6.0 or higher</b>.

### 2. Setup your app
#### 2.1 Gradle settings
We use Hilt for Dagger dependency injection, so you will need to add the corresponding dependencies.

In the project's root `build.gradle`, 
```
buildscript {
    // other setup here

    ext {
        hilt_version = '2.43.2'
    }

    dependencies {
        classpath "com.google.dagger:hilt-android-gradle-plugin:$hilt_version"
    }
}
plugins {
    // other setup here
    id 'com.google.dagger.hilt.android' version "$hilt_version" apply false
}
```

And then in your `app/build.gradle`:

```
plugins {
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

dependencies {
    // dagger-hilt
    implementation "com.google.dagger:hilt-android:$hilt_version"
    kapt "com.google.dagger:hilt-compiler:$hilt_version"
    
    // viewmodel-related
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1'
}
```
#### 2.2 ViewModel Module Dependencies Injection
Since we use Hilt dependency injection, you will also need to create a module defining ethereum viewmodel injection. This is a single instance that will be shared across various view models and will survive configuration changes.

```
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.ApplicationRepository
import io.metamask.androidsdk.EthereumViewModel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EthereumViewModelModule {

    @Provides
    @Singleton
    fun provideEthereumViewModel(repository: ApplicationRepository): EthereumViewModel {
        return EthereumViewModel(repository)
    }
}
```

#### 2.3 Setup Application Class
If you don't have an application class, you need to create one.
```
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DappApplication : Application() {}
```
Then update `android:name` in the `AndroidManifest.xml` to this application class.

```
<manifest>
    <application
        android:name=".DappApplication"
        ...
    </application>
</manifest>

```
#### 2.4 Add `@AndroidEntryPoint` to your Activity and Fragment
As a final step, if you need to inject your dependencies in an activity, you need to add `@AndroidEntryPoint` in your activity class. However, if you need to inject your dependencies in a fragment, then you need to add `@AndroidEntryPoint` in both the fragment and the activity that hosts the fragment.

```
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
   // ...
}
```

```
@AndroidEntryPoint
class LoginFragment : Fragment() {
   // ...
}
```

Refer to the example app for more details on how we set up a Jetpack Compose project to work with the SDK.

### 3. Import the SDK
Now you can import the SDK and start using it.
```
import io.metamask.androidsdk.EthereumViewModel
// other imports as necessary
```

### 4. Connect your Dapp
The Ethereum module requires the app context, so you will need to instantiate it from an Activity or a module that injects a context.
```kotlin
// MainActivity

// Obtain EthereumViewModel using viewModels()
val ethereumViewModel: EthereumViewModel by viewModels()

// We track three events: connection request, connected, disconnected, otherwise no tracking. 
// This helps us to monitor any SDK connection issues. 
//  

val dapp = Dapp(name: "Droid Dapp", url: "https://droiddapp.com")

// This is the same as calling "eth_requestAccounts"
ethereumViewModel.connect(dapp) { result ->
    if (result is RequestError) {
        Log.e(TAG, "Ethereum connection error: ${result.message}")
    } else {
        Logger.d(TAG, "Ethereum connection result: $result")
    }
}
```

We log three SDK events: `connectionRequest`, `connected` and `disconnected`. Otherwise no tracking. This helps us to monitor any SDK connection issues. If you wish to disable this, you can do so by setting `ethereumViewModel.enableDebug = false`.


### 5. You can now call any ethereum provider method

#### Example 1: Get Chain ID
```kotlin
var chainId: String? = null

val chainIdRequest = EthereumRequest(EthereumMethod.ETH_CHAIN_ID.value) // or EthereumRequest("eth_chainId")

ethereumViewModel.sendRequest(chainIdRequest) { result ->
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
    ethereumViewModel.selectedAddress, 
    "latest" // "latest", "earliest" or "pending" (optional)
    )

  
// Create request  
let getBalanceRequest = EthereumRequest(
    EthereumMethod.ETHGETBALANCE.value,
    params)

// Make request
ethereumViewModel.sendRequest(getBalanceRequest) { result ->
    if (result is RequestError) {
        // handle error
    } else {
        balance = result
    }
}
```
#### Example 3: Sign message
```kotlin
val message = "{\"domain\":{\"chainId\":\"${ethereumViewModel.chainId}\",\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"

val from = ethereumViewModel.selectedAddress
val params: List<String> = listOf(from, message)

val signRequest = EthereumRequest(
    EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
    params
)

ethereumViewModel.sendRequest(signRequest) { result ->
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
val from = ethereumViewModel.
val to = "0x0000000000000000000000000000000000000000"
val amount = "0x01"
val params: Map<String, Any> = mapOf(
    "from" to from,
    "to" to to,
    "amount" to amount
)

// Create request
val transactionRequest = EthereumRequest(
    EthereumMethod.ETH_SEND_TRANSACTION.value,
    listOf(params)
)

// Make a transaction request
ethereumViewModel.sendRequest(transactionRequest) { result ->
    if (result is RequestError) {
        // handle error
    } else {
        Log.d(TAG, "Ethereum transaction result: $result")
    }
} 
```

#### Example 5: Switch chain
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

    ethereumViewModel.sendRequest(switchChainRequest) { result ->
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

    ethereumViewModel.sendRequest(addChainRequest) { result ->
        if (result is RequestError) {
            onError("Add chain error: ${result.message}")
        } else {
            if (chainId == ethereumViewModel.chainId) {
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

## Requirements
### MetaMask Mobile
This SDK requires MetaMask Mobile version 7.6.0 or higher.

### Environment
You will need to have MetaMask Mobile wallet installed on your target device i.e physical device or emulator, so you can either have it installed from the [Google Play](https://play.google.com/store/apps/details?id=io.metamask), or clone and compile MetaMask Mobile wallet from [source](https://github.com/MetaMask/metamask-mobile) and build to your target device. 

### Hardware
This SDK has an Minimum Android SDK (minSdk) version requirement of 23.

## Resources
Refer to the [MetaMask API Reference](https://docs.metamask.io/wallet/reference/provider-api) for more information.