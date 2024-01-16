package io.metamask.androidsdk

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

internal class HttpClient {
    private val client = OkHttpClient()

    fun newCall(baseUrl: String, parameters: Map<String, Any>? = null, callback: ((String?, IOException?) -> Unit)? = null) {
        val json = JSONObject(parameters).toString()

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(baseUrl)
            .headers(Headers.headersOf("Accept", "application/json", "Content-Type", "application/json"))
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG,"HttpClient: error ${e.message}")
                if (callback != null) {
                    callback(null, e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    // Handle the response asynchronously
                    if (callback != null) {
                        callback(it.body?.string(), null)
                    }
                }
            }
        })
    }
}