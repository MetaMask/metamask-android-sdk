# MetaMask iOS SDK
The MetaMask iOS SDK enables developers to connect their native iOS apps to the Ethereum blockchain via the MetaMask Mobile wallet, effectively enabling the creation of iOS native decentralised applications (Dapps).

## Getting Started
You can import the MetaMask iOS SDK into your native iOS app to enable users to easily connect with their MetaMask Mobile wallet. Refer to the [MetaMask API Reference](https://c0f4f41c-2f55-4863-921b-sdk-docs.github.io/guide/rpc-api.html#table-of-contents) to see all the ethereum RPC methods available.

### 1. Install

#### Cocoapods
To add MetaMask iOS SDK as a cocoapods dependency to your project, add this entry in your Podfile: 
```
  pod 'metamask-ios-sdk'
```
And then run:
```
pod install
```
#### Swift Package Manager
##### Via Xcode Menu
To add MetaMask iOS SDK as an SPM package to your project, in Xcode select: `File -> Swift Packages -> Add Package Dependency`. And then enter this repository's url, i.e https://github.com/MetaMask/metamask-ios-sdk.

##### Via Package file
```swift
    dependencies: [
        .package(
            url: "https://github.com/MetaMask/metamask-ios-sdk",
            from: "0.2.0"
        )
    ]
```

#### Note
Please note that the SDK supports `ios-arm64` (iOS devices) and `ios-arm64-simulator` (M1 chip simulators). We currently do not support `ios-ax86_64-simulator` (Intel chip simulators).

### 2. Import the SDK
```
import metamask_ios_sdk
```

### 3. Connect your Dapp
```swift
@ObservedObject var ethereum = MetaMaskSDK.shared.ethereum

// We log three events: connection request, connected, disconnected, otherwise no tracking. 
// This helps us to monitor any SDK connection issues. 
//  

let dapp = Dapp(name: "Dub Dapp", url: "https://dubdapp.com")

// This is the same as calling "eth_requestAccounts"
ethereum.connect(dapp)
```

We log three SDK events: `connectionRequest`, `connected` and `disconnected`. Otherwise no tracking. This helps us to monitor any SDK connection issues. If you wish to disable this, you can do so by setting `MetaMaskSDK.shared.enableDebug = false` or `ethereum.enableDebug = false`.


### 4. You can now call any ethereum provider method
We use Combine to publish ethereum events, so you'll need an `AnyCancellable` storage.
```swift
@State private var cancellables: Set<AnyCancellable> = []
```
#### Example 1: Get Chain ID
```swift
@State var chainId: String?

let chainIdRequest = EthereumRequest(method: .ethChainId)

ethereum.request(chainIdRequest)?.sink(receiveCompletion: { completion in
    switch completion {
    case .failure(let error):
        print("\(error.localizedDescription)")
    default: break
    }
}, receiveValue: { result in
    self.chainId = result
})
.store(in: &cancellables)  
```


#### Example 2: Get account balance
```swift
@State var balance: String?

// Create parameters
let parameters: [String] = [
    ethereum.selectedAddress, // address to check for balance
    "latest" // "latest", "earliest" or "pending" (optional)
  ]
  
// Create request  
let getBalanceRequest = EthereumRequest(
    method: .ethGetBalance,
    params: parameters)

// Make request
ethereum.request(getBalanceRequest)?.sink(receiveCompletion: { completion in
    switch completion {
    case .failure(let error):
        print("\(error.localizedDescription)")
    default: break
    }
}, receiveValue: { result in
    self.balance = result
})
.store(in: &cancellables)  
```

#### Example 3: Send transaction
##### Using parameters dictionary
If your request parameters is a simple dictionary of string key-value pairs, you can use it directly. Note that the use of `Any` or even `AnyHashable` types is not supported as the type needs to be explicitly known.

```swift
// Create parameters
let parameters: [String: String] = [
    "to": "0x...", // receiver address
    "from": ethereum.selectedAddress, // sender address
    "value": "0x..." // amount
  ]
    
// Create request
let transactionRequest = EthereumRequest(
    method: .ethSendTransaction,
    params: [parameters] // eth_sendTransaction rpc call expects an array parameters object
    )

// Make a transaction request
ethereum.request(transactionRequest)?.sink(receiveCompletion: { completion in
    switch completion {
    case .failure(let error):
        print("\(error.localizedDescription)")
    default: break
    }
}, receiveValue: { result in
    print(result)
})
.store(in: &cancellables)  
```
##### Using a struct
 For a more complex parameters representation, you can define and use a struct that conforms to `CodableData` i.e implementing the requirement:
 ```
 func socketRepresentation() -> NetworkData
 ```
 so that the type can be represented as a socket packet.

```swift
struct Transaction: CodableData {
    let to: String
    let from: String
    let value: String
    let data: String?

    init(to: String, from: String, value: String, data: String? = nil) {
        self.to = to
        self.from = from
        self.value = value
        self.data = data
    }

    func socketRepresentation() -> NetworkData {
        [
            "to": to,
            "from": from,
            "value": value,
            "data": data
        ]
    }
}

let transaction = Transaction(
    to: "0x...", // receiver address
    from: ethereum.selectedAddress, // sender address
    value: "0x..." // amount
)

let transactionRequest = EthereumRequest(
    method: .ethSendTransaction,
    params: [transaction] // eth_sendTransaction rpc call expects an array parameters object
    )
```
Then make a request as shown in [Example 3](#example-3-send-transaction) above

## Examples
We have created an [Example](./Example/) dapp as a guide on how to connect to ethereum and make requests. There are three illustrated examples:

1) `ConnectView.swift` - Connect to the ethereum blockchain via the MetaMask SDK. The other examples are based on a successful connection as demonstrated in this example

2) `TransactionView.swift` - Send a transaction

3) `SignView.swift` - Sign a transaction

4) `SwitchChainView.swift` - Switch to a different network chain (you need to call the `addEthereumChain` rpc call first if it doesn't already exist in the MetaMask wallet). 

To run the example project, clone this repository, change directory to `metamask-ios-sdk/Example`, and then run `pod install` from the Example directory to install the SDK as a dependency on the project, and then open `metamask-ios-sdk.xcworkspace` and run the project. 

You will need to have MetaMask Mobile wallet installed on your target i.e physical device or simulator, so you can either have it installed from the [App Store](https://apps.apple.com/us/app/metamask-blockchain-wallet/id1438144202), or clone and compile MetaMask Mobile wallet from [source](https://github.com/MetaMask/metamask-mobile) and build to your target device. 

## Requirements
This SDK has an iOS minimum version requirement of 14.0. You need your app to have an iOS minimum deployment of no less than 14.0.
