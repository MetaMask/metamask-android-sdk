package io.metamask.androidsdk

import android.util.Log
import okhttp3.*
import java.io.IOException

internal class HttpClient {
    private val client = OkHttpClient()

    fun newCall(baseUrl: String, parameters: Map<String, String>? = null) {

        val request: Request

        if (parameters != null) {
            val requestBodyBuilder = FormBody.Builder()

            for ((key, value) in parameters) {
                requestBodyBuilder.add(key, value)
            }

            val requestBody: RequestBody = requestBodyBuilder.build()
            request = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .build()
        } else {
            request = Request.Builder()
                .url(baseUrl)
                .build()
        }

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG,"HttpClient: error ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {

            }
        })
    }
}