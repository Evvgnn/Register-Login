package com.example.registerlogin.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.registerlogin.NavigationInterface
import com.example.registerlogin.R
import com.example.registerlogin.SuccessActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

    private val validEmail = "test@example.com"
    private val validPassword = "123456"

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

        emailEditText.addTextChangedListener {
            emailInputLayout.error = null
        }

        passwordEditText.addTextChangedListener {
            passwordInputLayout.error = null
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
            if (validateData()) {
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

    private fun validateData(): Boolean {
        clearErrors()
        var isValid = true

        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty()) {
            setError(emailInputLayout, "Email is required")
            isValid = false
        } else if (!isValidEmail(email)) {
            setError(emailInputLayout, "Invalid email format")
            isValid = false
        }

        if (password.isEmpty()) {
            setError(passwordInputLayout, "Password is required")
            isValid = false
        } else if (password.length < 6) {
            setError(passwordInputLayout, "Password must be at least 6 characters")
            isValid = false
        }

        return isValid
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email == validEmail && password == validPassword) {
            val intent = Intent(requireContext(), SuccessActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        } else {
            Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_LONG).show()
            setError(emailInputLayout, "")
            setError(passwordInputLayout, "Invalid credentials")
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

    private fun clearErrors() {
        emailInputLayout.error = null
        passwordInputLayout.error = null
    }
}