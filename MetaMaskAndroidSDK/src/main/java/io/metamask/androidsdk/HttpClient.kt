package io.metamask.androidsdk

import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

internal class HttpClient {
    private val client = OkHttpClient()

    fun newCall(baseUrl: String, parameters: Map<String, String>? = null) {
        val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()

        parameters?.forEach { (key, value) ->
            urlBuilder?.addQueryParameter(key, value)
        }

        val url = urlBuilder?.build()

        val request = url?.let {
            Request.Builder()
                .url(it)
                .build()
        }

        if (request != null) {
            client.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG,"HttpClient: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG,"HttpClient: ${response}")
                }
            })
        }
    }
}