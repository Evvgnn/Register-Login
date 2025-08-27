package com.example.registerlogin

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.registerlogin.data.api.KtorApiClient
import com.example.registerlogin.utils.TokenManager
import kotlinx.coroutines.launch

class SuccessActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var ktorApiClient: KtorApiClient

    private lateinit var welcomeTextView: TextView
    private lateinit var getDataButton: Button
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        tokenManager = TokenManager(this)
        ktorApiClient = KtorApiClient.getInstance()

        initViews()
        setupUI()
        setupClickListeners()
    }

    private fun initViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView)
        getDataButton = findViewById(R.id.getDataButton)
        logoutButton = findViewById(R.id.logoutButton)
    }

    private fun setupUI() {
        val userName = tokenManager.getUserName() ?: "User"
        welcomeTextView.text = "Welcome back, $userName!"
    }

    private fun setupClickListeners() {
        getDataButton.setOnClickListener { getExamData() }
        logoutButton.setOnClickListener { logout() }
    }

    private fun getExamData() {
        val accessToken = tokenManager.getAccessToken()
        if (accessToken == null) {
            Toast.makeText(this, "Access token is missing", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        getDataButton.isEnabled = false
        getDataButton.text = "Starting..."

        lifecycleScope.launch {
            try {
                val result = ktorApiClient.getExamAndroid(accessToken)

                when (result) {
                    is KtorApiClient.ApiResult.Success -> {
                        val examData = result.data
                        Toast.makeText(this@SuccessActivity,
                            "Quiz received successfully!",
                            Toast.LENGTH_LONG).show()
                        Log.d("SuccessActivity", "Quiz data: $examData")
                    }
                    is KtorApiClient.ApiResult.Error -> {
                        Log.e("SuccessActivity", "API error: code=${result.code}, message='${result.message}'")

                        when (result.code) {
                            400 -> {
                                Toast.makeText(this@SuccessActivity, "Error 400: Bad Request", Toast.LENGTH_LONG).show()
                            }
                            401 -> {
                                Toast.makeText(
                                    this@SuccessActivity,
                                    "401 – Unauthorized. Refreshing token...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                refreshTokenAndRetry()
                            }
                            403 -> {
                                Toast.makeText(this@SuccessActivity, " Error 403: Forbidden", Toast.LENGTH_LONG).show()
                            }
                            404 -> {
                                Toast.makeText(this@SuccessActivity, "Error 404: Not Found", Toast.LENGTH_LONG).show()
                            }
                            503 -> {
                                Toast.makeText(this@SuccessActivity, "Error 503: Service Unavailable", Toast.LENGTH_LONG).show()
                            }
                            504 -> {
                                Toast.makeText(this@SuccessActivity, "Error 504: Gateway Timeout", Toast.LENGTH_LONG).show()
                            }
                            0 -> {
                                Toast.makeText(this@SuccessActivity, result.message.ifBlank { "Network error" }, Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@SuccessActivity, "Error ${result.code}: ${result.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SuccessActivity", "Exception in getExamData", e)
                Toast.makeText(this@SuccessActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                getDataButton.isEnabled = true
                getDataButton.text = "Start the quiz"
            }
        }
    }

    private fun refreshTokenAndRetry() {
        val refreshToken = tokenManager.getRefreshToken()
        val email = tokenManager.getUserEmail()

        if (refreshToken == null || email == null) {
            Toast.makeText(this, "Cannot refresh token", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        getDataButton.isEnabled = false
        getDataButton.text = "Refreshing token..."

        lifecycleScope.launch {
            try {
                val result = ktorApiClient.refreshToken(email, refreshToken)

                when (result) {
                    is KtorApiClient.ApiResult.Success -> {
                        tokenManager.saveTokens(
                            result.data.accessToken,
                            result.data.refreshToken,
                            result.data.email,
                            result.data.userName
                        )

                        Toast.makeText(this@SuccessActivity, "Token refreshed. Retrying request...", Toast.LENGTH_SHORT).show()

                        getExamData()
                    }
                    is KtorApiClient.ApiResult.Error -> {
                        tokenManager.clearTokens()
                        Toast.makeText(this@SuccessActivity, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                        logout()
                    }
                }
            } catch (e: Exception) {
                Log.e("SuccessActivity", "Error refreshing token", e)
                Toast.makeText(this@SuccessActivity, "Token refresh error: ${e.message}", Toast.LENGTH_LONG).show()
                logout()
            } finally {
                getDataButton.isEnabled = true
                getDataButton.text = "Start the quiz"
            }
        }
    }

    private fun logout() {
        tokenManager.clearTokens()
        finish()
    }
}