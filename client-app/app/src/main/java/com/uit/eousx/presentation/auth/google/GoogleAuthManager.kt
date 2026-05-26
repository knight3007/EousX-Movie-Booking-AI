package com.uit.eousx.presentation.auth.google

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class GoogleAuthManager(
    private val context: Context,
    private val firebaseAuthProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) {
    fun getSignInIntent(): Intent {
        return googleSignInClient().signInIntent
    }

    suspend fun getFreshSignInIntent(): Intent {
        firebaseAuthProvider().signOut()
        googleSignInClient().signOut().await()
        return getSignInIntent()
    }

    private fun googleSignInClient(): GoogleSignInClient {
        val webClientId = context.defaultWebClientId()
            ?: error(
                "Missing Firebase web client id. app/google-services.json must include an oauth_client " +
                    "entry with client_type 3. Re-download it from Firebase after enabling Google Sign-In."
            )

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, options)
    }

    suspend fun handleSignInResult(data: Intent?): GoogleAuthResult {
        if (data == null) {
            return GoogleAuthResult.Error("Google sign-in was cancelled.")
        }

        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val googleAccountIdToken = account.idToken
                ?: return GoogleAuthResult.Error("Google account did not return an ID token.")

            val credential = GoogleAuthProvider.getCredential(googleAccountIdToken, null)
            val authResult = firebaseAuthProvider().signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return GoogleAuthResult.Error("Firebase sign-in did not return a user.")
            val firebaseIdToken = firebaseUser.getIdToken(true).await().token
                ?: return GoogleAuthResult.Error("Firebase did not return an ID token.")

            GoogleAuthResult.Success(firebaseIdToken)
        } catch (exception: ApiException) {
            GoogleAuthResult.Error(exception.toReadableMessage())
        } catch (throwable: Throwable) {
            GoogleAuthResult.Error(throwable.message ?: "Google sign-in failed.")
        }
    }
}

private fun Context.defaultWebClientId(): String? {
    val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (resourceId == 0) null else getString(resourceId).takeIf { it.isNotBlank() }
}

private fun ApiException.toReadableMessage(): String {
    return when (statusCode) {
        CommonStatusCodes.CANCELED, GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
            "Google sign-in was cancelled."
        }
        CommonStatusCodes.NETWORK_ERROR -> "Network error while signing in with Google."
        else -> statusMessage ?: "Google sign-in failed. Code: $statusCode"
    }
}

private object GoogleSignInStatusCodes {
    const val SIGN_IN_CANCELLED = 12501
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Google task failed."))
            }
        }
    }
}
