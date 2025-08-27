package com.example.registerlogin.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val USER_EMAIL = "user_email"
        private const val USER_NAME = "user_name"
    }

    fun saveTokens(accessToken: String, refreshToken: String, email: String, userName: String) {
        prefs.edit()
            .putString(ACCESS_TOKEN, accessToken)
            .putString(REFRESH_TOKEN, refreshToken)
            .putString(USER_EMAIL, email)
            .putString(USER_NAME, userName)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(REFRESH_TOKEN, null)
    fun getUserEmail(): String? = prefs.getString(USER_EMAIL, null)
    fun getUserName(): String? = prefs.getString(USER_NAME, null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null && getRefreshToken() != null
}