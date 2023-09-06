package io.metamask.ecies

import android.util.Log

public class Ecies {
    companion object {
        const val TAG = "ECIES"
        // Load the Rust library.
        init {
            try {
                System.loadLibrary("ecies")
                Log.d(TAG, "Ecies loaded successfully!")
            } catch (e: UnsatisfiedLinkError) {
                Log.d(TAG, "Ecies could not be loaded: ${e.message}")
            }
        }

        // Rust FFI function signatures.
        @JvmStatic external fun generateSecretKey(): String
        @JvmStatic external fun derivePublicKeyFrom(secret: String): String
        @JvmStatic external fun encryptMessage(public: String, message: String): String
        @JvmStatic external fun decryptMessage(secret: String, message: String): String
    }

    // Kotlin functions that wrap Rust FFI functions.
    public fun privateKey(): String {
        return generateSecretKey()
    }

    public fun publicKeyFrom(secretKey: String): String {
        return derivePublicKeyFrom(secretKey)
    }

    public fun encrypt(publicKey: String, message: String): String {
        return encryptMessage(publicKey, message)
    }

    public fun decrypt(secretKey: String, message: String): String {
        return decryptMessage(secretKey, message)
    }
}
