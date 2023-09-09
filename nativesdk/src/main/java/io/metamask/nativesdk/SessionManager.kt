package io.metamask.nativesdk

class SessionManager {
    var sessionId = ""

    companion object {
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            if (instance == null) {
                instance = SessionManager()
            }
            return instance as SessionManager
        }
    }
}