package com.example.registerlogin.fragments

import android.content.Context
import android.content.Intent
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
import com.example.registerlogin.SuccessActivity
import com.example.registerlogin.data.api.KtorApiClient
import com.example.registerlogin.data.models.LoginRequest
import com.example.registerlogin.utils.TokenManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.regex.Pattern

class LoginFragment : Fragment() {

    private lateinit var navigationInterface: NavigationInterface

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    private lateinit var loginButton: Button
    private lateinit var signUpTextView: TextView
    private lateinit var forgotPasswordTextView: TextView

    private lateinit var tokenManager: TokenManager
    private val apiClient = KtorApiClient.getInstance()

    companion object {
        private const val ARG_EMAIL = "email"

        fun newInstance(email: String? = null): LoginFragment {
            val fragment = LoginFragment()
            val args = Bundle()
            email?.let { args.putString(ARG_EMAIL, it) }
            fragment.arguments = args
            return fragment
        }
    }

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
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()

        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmailField()
        }

        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePasswordField()
        }

        arguments?.getString(ARG_EMAIL)?.let { email ->
            emailEditText.setText(email)
        }
    }

    private fun initViews(view: View) {
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)

        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)

        loginButton = view.findViewById(R.id.loginButton)
        signUpTextView = view.findViewById(R.id.signUpTextView)
        forgotPasswordTextView = view.findViewById(R.id.forgotPasswordTextView)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val isEmailValid = validateEmailField()
            val isPasswordValid = validatePasswordField()

            if (isEmailValid && isPasswordValid) {
                performLogin()
            }
        }

        signUpTextView.setOnClickListener {
            navigationInterface.navigateToRegister()
        }

        forgotPasswordTextView.setOnClickListener {
            navigationInterface.navigateToResetPassword()
        }
    }

    private fun performLogin() {
        if (!isNetworkAvailable()) {
            Toast.makeText(
                requireContext(),
                "No internet connection. Please check your network connection and try again.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        tryApiLogin(email, password)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun tryApiLogin(email: String, password: String) {
        lifecycleScope.launch {
            loginButton.isEnabled = false
            loginButton.text = "Signing in..."

            try {
                Log.d("LoginFragment", "Starting API login attempt...")
                val request = LoginRequest(email = email, password = password)
                val result = apiClient.login(request)
                Log.d("LoginFragment", "API call completed, result type: ${result::class.simpleName}")

                when (result) {
                    is KtorApiClient.ApiResult.Success -> {
                        Log.d("LoginFragment", "Login successful")
                        val auth = result.data
                        tokenManager.saveTokens(
                            accessToken = auth.accessToken,
                            refreshToken = auth.refreshToken,
                            email = auth.email,
                            userName = auth.userName
                        )

                        emailInputLayout.error = null
                        passwordInputLayout.error = null

                        val intent = Intent(requireContext(), SuccessActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }

                    is KtorApiClient.ApiResult.Error -> {
                        Log.d("LoginFragment", "API returned error: code=${result.code}, message='${result.message}'")
                        handleApiError(result.code, result.message)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "Exception caught: ${e::class.simpleName}: ${e.message}", e)
                handleException(e)
            } finally {
                loginButton.isEnabled = true
                loginButton.text = "Login"
            }
        }
    }

    private fun handleApiError(code: Int, message: String) {
        when (code) {
            400 -> {
                Toast.makeText(requireContext(), "Error 400: Bad Request", Toast.LENGTH_LONG).show()
            }
            401 -> {
                emailInputLayout.error = null
                passwordInputLayout.error = "Invalid email or password"
                Toast.makeText(requireContext(), "Error 401: Unauthorized", Toast.LENGTH_LONG).show()
            }
            403 -> {
                Toast.makeText(requireContext(), " Error 403: Forbidden", Toast.LENGTH_LONG).show()
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
                val msg = if (message.isNotBlank()) message else "An error occurred"
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
                Toast.makeText(requireContext(), "Unexpected error: ${exception.message ?: "unknown"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateEmailField(): Boolean {
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

    private fun validatePasswordField(): Boolean {
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

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return Pattern.compile(emailPattern).matcher(email).matches()
    }

    private fun setError(inputLayout: TextInputLayout, message: String) {
        inputLayout.error = message
        inputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.error_color))
    }
}