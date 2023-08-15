package io.metamask.androidsdk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EthereumViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EthereumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EthereumViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
