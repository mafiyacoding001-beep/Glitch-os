package com.example.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserEntity
import com.example.data.local.VideoDatabase
import com.example.data.model.GlitchAgentSettings
import com.example.data.repository.FirestoreRepository
import com.example.util.FirestoreService
import com.example.util.GoogleSignInHelper
import com.example.util.GoogleUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val firestoreRepo = FirestoreRepository()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                handleSuccessfulGoogleSignIn(account)
            } else {
                Toast.makeText(this, "Sign-in returned empty account", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("LoginActivity", "Google Sign-In ApiException: status ${e.statusCode}", e)
            val errorMsg = when (e.statusCode) {
                10 -> "Developer configuration error (SHA-1 / Client ID mismatch)"
                7 -> "Network error. Please check your connection."
                12501 -> "Sign-in cancelled by user"
                else -> "Google Sign-In error (Code ${e.statusCode}): ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            if (e.statusCode != 12501) {
                startCredentialManagerLogin()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Google Sign-In error", e)
            Toast.makeText(this, "Sign-in error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val forceLogin = intent.getBooleanExtra("FORCE_LOGIN", false)
        try {
            auth = FirebaseAuth.getInstance()
            if (!forceLogin && auth.currentUser != null && GoogleSignInHelper.hasValidYouTubePermissions(this)) {
                // Ensure existing user is synced to helper
                GoogleSignInHelper.checkExistingUser(this)
                openMainActivity()
                return
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Firebase Auth init error", e)
        }

        findViewById<Button>(R.id.btnGoogleLogin).setOnClickListener {
            startGoogleLogin()
        }
    }

    private fun startGoogleLogin() {
        try {
            val rawClientId = getString(R.string.default_web_client_id)
            val webClientId = if (rawClientId.isNotBlank() && rawClientId != "AI_STUDIO_CLIENT_ID") {
                rawClientId
            } else {
                GoogleSignInHelper.WEB_CLIENT_ID
            }

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .requestScopes(
                    Scope(GoogleSignInHelper.YOUTUBE_UPLOAD_SCOPE),
                    Scope(GoogleSignInHelper.YOUTUBE_SCOPE),
                    Scope(GoogleSignInHelper.YOUTUBE_FORCE_SSL_SCOPE),
                    Scope(GoogleSignInHelper.YOUTUBE_READONLY_SCOPE)
                )
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            // Sign out first to ensure account picker and permission consent screen is shown
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to launch Google Sign In, trying CredentialManager", e)
            startCredentialManagerLogin()
        }
    }

    private fun handleSuccessfulGoogleSignIn(account: GoogleSignInAccount) {
        val hasYtUpload = GoogleSignIn.hasPermissions(
            account,
            Scope(GoogleSignInHelper.YOUTUBE_UPLOAD_SCOPE),
            Scope(GoogleSignInHelper.YOUTUBE_SCOPE)
        )

        val googleUser = GoogleUser(
            displayName = account.displayName ?: "YouTube Creator",
            email = account.email,
            photoUrl = account.photoUrl?.toString(),
            idToken = account.idToken,
            hasYouTubePermission = hasYtUpload
        )

        GoogleSignInHelper.saveUserToPreferences(this, googleUser)

        lifecycleScope.launch {
            try {
                // 1. Firebase Auth with Google ID Token
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    val cred = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(cred).await()
                }

                // 2. Local Database saving
                val entity = UserEntity(
                    email = account.email ?: "",
                    name = account.displayName ?: "YouTube Creator",
                    avatarUrl = account.photoUrl?.toString(),
                    oauthToken = account.idToken,
                    lastLogin = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    VideoDatabase.getDatabase(this@LoginActivity).authDao().saveUser(entity)
                }

                // 3. Firestore Complete Data & Collections Sync
                withContext(Dispatchers.IO) {
                    try {
                        FirestoreService.saveCompleteUserAndDeviceData(this@LoginActivity, googleUser)
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "saveCompleteUserAndDeviceData failed", e)
                    }
                    try {
                        firestoreRepo.syncUser(entity)
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "firestoreRepo.syncUser failed", e)
                    }
                    try {
                        FirestoreService.saveAgentSettings(this@LoginActivity, GlitchAgentSettings())
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "saveAgentSettings failed", e)
                    }
                }

                Toast.makeText(
                    this@LoginActivity,
                    if (hasYtUpload) "✅ YouTube permissions granted! Welcome ${account.displayName ?: ""}"
                    else "Logged in as ${account.displayName ?: ""}",
                    Toast.LENGTH_SHORT
                ).show()

                openMainActivity()
            } catch (e: Exception) {
                Log.e("LoginActivity", "Post-login processing error", e)
                Toast.makeText(this@LoginActivity, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                openMainActivity()
            }
        }
    }

    private fun startCredentialManagerLogin() {
        val credentialManager = CredentialManager.create(this)
        val rawClientId = getString(R.string.default_web_client_id)
        val webClientId = if (rawClientId.isNotBlank() && rawClientId != "AI_STUDIO_CLIENT_ID") {
            rawClientId
        } else {
            GoogleSignInHelper.WEB_CLIENT_ID
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdToken.idToken)
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid Google Credential format", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "CredentialManager Sign-In Failed", e)
                Toast.makeText(this@LoginActivity, "Sign-in error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch {
            try {
                val authResult = auth.signInWithCredential(cred).await()
                val user = authResult.user

                if (user != null) {
                    val googleUser = GoogleUser(
                        displayName = user.displayName ?: "YouTube Creator",
                        email = user.email,
                        photoUrl = user.photoUrl?.toString(),
                        idToken = idToken,
                        hasYouTubePermission = true
                    )
                    GoogleSignInHelper.saveUserToPreferences(this@LoginActivity, googleUser)

                    val entity = UserEntity(
                        email = user.email ?: "",
                        name = user.displayName ?: "User",
                        avatarUrl = user.photoUrl?.toString(),
                        oauthToken = idToken,
                        lastLogin = System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        VideoDatabase.getDatabase(this@LoginActivity).authDao().saveUser(entity)
                        try {
                            FirestoreService.saveCompleteUserAndDeviceData(this@LoginActivity, googleUser)
                            firestoreRepo.syncUser(entity)
                            FirestoreService.saveAgentSettings(this@LoginActivity, GlitchAgentSettings())
                        } catch (e: Exception) {
                            Log.e("LoginActivity", "Firestore sync failed", e)
                        }
                    }
                    openMainActivity()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Firebase Auth Failed", e)
                Toast.makeText(this@LoginActivity, "Auth error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
