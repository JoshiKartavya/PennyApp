package com.kartavya.penny

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleSignInActivity : Activity() {
    companion object {
        private const val RC_SIGN_IN = 9001
        
        var onSuccessCallback: ((String, String, String?) -> Unit)? = null
        var onFailureCallback: ((String) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val serverClientId = "420970137197-cccvrkmjo7hednf6eghcuvmoqatpmitb.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(serverClientId)
            .build()
            
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        // Sign out first to always show the account chooser dialog!
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email
                val name = account?.displayName
                val idToken = account?.idToken
                if (email != null) {
                    onSuccessCallback?.invoke(email, name ?: "", idToken)
                } else {
                    onFailureCallback?.invoke("Google Account email was null")
                }
            } catch (e: Exception) {
                onFailureCallback?.invoke(e.message ?: "Authentication failed")
            }
            finish()
        }
    }
}
