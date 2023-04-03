package com.metamask.android.sdk

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.util.logging.Logger

class CommunicationClient : IDappInterface.Stub() {
    override fun sendMessage(message: String?): String {
        try {
            val json = JSONObject(message)
            return json.get("data")
        } catch (e: JSONException) {
            Log.e("SDK", e.localizedMessage)
        }
    }

}