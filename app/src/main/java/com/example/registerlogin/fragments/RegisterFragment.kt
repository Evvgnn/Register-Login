package com.example.registerlogin.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.registerlogin.NavigationInterface
import com.example.registerlogin.R
import com.example.registerlogin.data.api.KtorApiClient
import com.example.registerlogin.data.models.SignUpRequest
import com.example.registerlogin.utils.TokenManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.regex.Pattern

class RegisterFragment : Fragment() {

    private lateinit var navigationInterface: NavigationInterface

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout

    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    private lateinit var signUpButton: Button
    private lateinit var signInTextView: TextView

    private lateinit var tokenManager: TokenManager
    private val apiClient = KtorApiClient.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is NavigationInterface) {
            navigationInterface = context
        } else {
            throw RuntimeException("$context must implement NavigationInterface")
        }
        tokenManager = TokenManager(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupValidationOnFocusLost()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        nameInputLayout = view.findViewById(R.id.nameInputLayout)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout)

        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)

        signUpButton = view.findViewById(R.id.signUpButton)
        signInTextView = view.findViewById(R.id.signInTextView)
    }

    private fun setupValidationOnFocusLost() {
        nameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateName()
        }
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail()
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePassword()
        }
        confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateConfirmPassword()
        }
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            val isNameValid = validateName()
            val isEmailValid = validateEmail()
            val isPasswordValid = validatePassword()
            val isConfirmPasswordValid = validateConfirmPassword()

            if (isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid) {
                performSignUp()
            }
        }

        signInTextView.setOnClickListener {
            navigationInterface.navigateToLogin()
        }
    }

    private fun performSignUp() {
        if (!isNetworkAvailable()) {
            Toast.makeText(
                requireContext(),
                "No internet connection. Registration requires internet access.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            signUpButton.isEnabled = false
            signUpButton.text = "Signing up..."

            try {
                Log.d("RegisterFragment", "Starting API registration attempt...")
                val request = SignUpRequest(
                    email = emailEditText.text.toString().trim(),
                    password = passwordEditText.text.toString(),
                    userName = nameEditText.text.toString().trim()
                )

                val result = apiClient.signUp(request)
                Log.d("RegisterFragment", "API call completed, result type: ${result::class.simpleName}")

                when (result) {
                    is KtorApiClient.ApiResult.Success -> {
                        Log.d("RegisterFragment", "Registration successful")
                        val auth = result.data
                        tokenManager.saveTokens(
                            accessToken = auth.accessToken,
                            refreshToken = auth.refreshToken,
                            email = auth.email,
                            userName = auth.userName
                        )

                        clearAllErrors()

                        navigationInterface.navigateToLogin(auth.email)
                    }
                    is KtorApiClient.ApiResult.Error -> {
                        Log.d("RegisterFragment", "API returned error: code=${result.code}, message='${result.message}'")
                        handleApiError(result.code, result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterFragment", "Exception caught: ${e::class.simpleName}: ${e.message}", e)
                handleException(e)
            } finally {
                signUpButton.isEnabled = true
                signUpButton.text = "Sign Up"
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun handleApiError(code: Int, message: String) {
        when (code) {
            400 -> {
                if (message.contains("already exists", ignoreCase = true)) {
                    Toast.makeText(requireContext(), "Error 400: Bad Request", Toast.LENGTH_LONG).show()
                    setError(emailInputLayout, "Email already registered")
                } else {
                    Toast.makeText(requireContext(), "Error 400: Bad Request", Toast.LENGTH_LONG).show()
                    setError(emailInputLayout, "Please check your input data")
                }
            }
            401 -> {
                Toast.makeText(requireContext(), "Error 401: Unauthorized", Toast.LENGTH_LONG).show()
            }
            403 -> {
                Toast.makeText(requireContext(), "Error 403: Forbidden", Toast.LENGTH_LONG).show()
            }
            404 -> {
                Toast.makeText(requireContext(), "Error 404: Not Found", Toast.LENGTH_LONG).show()
            }
            503 -> {
                Toast.makeText(requireContext(), "Error 503: Service Unavailable", Toast.LENGTH_LONG).show()
            }
            504 -> {
                Toast.makeText(requireContext(), "Error 504: Gateway Timeout", Toast.LENGTH_LONG).show()
            }
            0 -> {
                Toast.makeText(requireContext(), message.ifBlank { "Network error" }, Toast.LENGTH_LONG).show()
            }
            else -> {
                val msg = if (message.isNotBlank()) message else "Registration failed. Please try again."
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleException(exception: Exception) {
        when (exception) {
            is UnknownHostException -> {
                Toast.makeText(requireContext(),
                    "No internet connection. Please check your network connection.",
                    Toast.LENGTH_LONG).show()
            }
            is SocketTimeoutException -> {
                Toast.makeText(requireContext(),
                    "Connection timeout. Please check your internet connection.",
                    Toast.LENGTH_LONG).show()
            }
            is IOException -> {
                Toast.makeText(requireContext(),
                    "Network error. Please check your internet connection.",
                    Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(requireContext(),
                    "Network error. Please check your connection and try again.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearAllErrors() {
        nameInputLayout.error = null
        emailInputLayout.error = null
        passwordInputLayout.error = null
        confirmPasswordInputLayout.error = null
    }

    private fun validateName(): Boolean {
        val name = nameEditText.text.toString().trim()
        return when {
            name.isEmpty() -> {
                setError(nameInputLayout, "Name is required")
                false
            }
            name.length < 2 -> {
                setError(nameInputLayout, "Name must be at least 2 characters")
                false
            }
            else -> {
                nameInputLayout.error = null
                true
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = emailEditText.text.toString().trim()
        return when {
            email.isEmpty() -> {
                setError(emailInputLayout, "Email is required")
                false
            }
            !isValidEmail(email) -> {
                setError(emailInputLayout, "Invalid email format")
                false
            }
            else -> {
                emailInputLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = passwordEditText.text.toString()
        return when {
            password.isEmpty() -> {
                setError(passwordInputLayout, "Password is required")
                false
            }
            password.length < 6 -> {
                setError(passwordInputLayout, "Password must be at least 6 characters")
                false
            }
            else -> {
                passwordInputLayout.error = null
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        return when {
            confirmPassword.isEmpty() -> {
                setError(confirmPasswordInputLayout, "Please confirm your password")
                false
            }
            confirmPassword != password -> {
                setError(confirmPasswordInputLayout, "Passwords do not match")
                false
            }
            else -> {
                confirmPasswordInputLayout.error = null
                true
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return Pattern.compile(emailPattern).matcher(email).matches()
    }

    private fun setError(inputLayout: TextInputLayout, message: String) {
        inputLayout.error = message
        inputLayout.setErrorTextColor(
            ContextCompat.getColorStateList(requireContext(), R.color.error_color)
        )
    }
}