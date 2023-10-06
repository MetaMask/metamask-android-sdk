package com.metamask.dapp

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.Logger
import javax.inject.Inject

@HiltViewModel
class ScreenViewModel @Inject constructor(): ViewModel() {
    private val _currentScreen = mutableStateOf(DappScreen.CONNECT)
    val currentScreen: State<DappScreen> = _currentScreen

    fun setScreen(screen: DappScreen) {
        _currentScreen.value = screen
        Logger.log("Navigating to $screen")
    }
}