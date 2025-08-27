package com.example.registerlogin.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ResetPasswordFragment : Fragment() {

    private lateinit var navigationInterface: NavigationInterface

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    private lateinit var resetButton: Button
    private lateinit var signUpTextView: TextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is NavigationInterface) {
            navigationInterface = context
        } else {
            throw RuntimeException("$context must implement NavigationInterface")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()

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

    private fun initViews(view: View) {
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout)

        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)

        resetButton = view.findViewById(R.id.resetButton)
        signUpTextView = view.findViewById(R.id.signUpTextView)
    }

    private fun setupClickListeners() {
        resetButton.setOnClickListener {
            val emailOk = validateEmail()
            val passOk = validatePassword()
            val confirmOk = validateConfirmPassword()
            if (!emailOk || !passOk || !confirmOk) {
                return@setOnClickListener
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(
                    requireContext(),
                    "No internet connection. Please check your network connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            performReset()
        }

        signUpTextView.setOnClickListener {
            navigationInterface.navigateToRegister()
        }
    }

    private fun performReset() {
        val email = emailEditText.text.toString().trim()

        Toast.makeText(
            requireContext(),
            "Password reset email sent to $email\nPlease check your inbox.",
            Toast.LENGTH_LONG
        ).show()

        navigationInterface.navigateToLogin(email)
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
        inputLayout.setErrorTextColor(ContextCompat.getColorStateList(requireContext(), R.color.error_color))
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
