package com.example.registerlogin.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import com.example.registerlogin.NavigationInterface
import com.example.registerlogin.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()

        nameEditText.addTextChangedListener {
            nameInputLayout.error = null
        }
        emailEditText.addTextChangedListener {
            emailInputLayout.error = null
        }
        passwordEditText.addTextChangedListener {
            passwordInputLayout.error = null
        }
        confirmPasswordEditText.addTextChangedListener {
            confirmPasswordInputLayout.error = null
        }
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

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            if (validateData()) {
                val email = emailEditText.text.toString()
                navigationInterface.navigateToLogin(email)
            }
        }

        signInTextView.setOnClickListener {
            navigationInterface.navigateToLogin()
        }
    }

    private fun validateData(): Boolean {
        clearErrors()
        var isValid = true

        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        if (name.isEmpty()) {
            setError(nameInputLayout, "Name is required")
            isValid = false
        } else if (name.length < 2) {
            setError(nameInputLayout, "Name must be at least 2 characters")
            isValid = false
        }

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

        if (confirmPassword.isEmpty()) {
            setError(confirmPasswordInputLayout, "Please confirm your password")
            isValid = false
        } else if (password != confirmPassword) {
            setError(confirmPasswordInputLayout, "Passwords do not match")
            isValid = false
        }

        return isValid
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
        nameInputLayout.error = null
        emailInputLayout.error = null
        passwordInputLayout.error = null
        confirmPasswordInputLayout.error = null
    }
}