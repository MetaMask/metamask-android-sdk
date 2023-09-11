package com.metamask.dapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.metamask.dapp.ui.theme.MetaMaskAndroidSDKClientTheme
import dagger.hilt.android.AndroidEntryPoint
import io.metamask.androidsdk.*
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val screensViewModel: ScreensViewModel by viewModels()
    private val ethereumViewModel: EthereumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MetaMaskAndroidSDKClientTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Setup(ethereumViewModel, screensViewModel)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MetaMaskAndroidSDKClientTheme {
        Greeting("Android")
    }
}