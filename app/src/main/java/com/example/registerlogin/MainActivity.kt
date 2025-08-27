package com.example.registerlogin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.registerlogin.fragments.LoginFragment
import com.example.registerlogin.fragments.RegisterFragment
import com.example.registerlogin.fragments.ResetPasswordFragment
import com.example.registerlogin.utils.TokenManager

class MainActivity : AppCompatActivity(), NavigationInterface {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tokenManager = TokenManager(this)

        if (tokenManager.isLoggedIn()) {
            val intent = Intent(this, SuccessActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .commit()
        }
    }

    override fun navigateToLogin(email: String?) {
        val fragment = LoginFragment.newInstance(email)
        replaceFragment(fragment)
    }

    override fun navigateToRegister() {
        replaceFragment(RegisterFragment())
    }

    override fun navigateToResetPassword() {
        replaceFragment(ResetPasswordFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

interface NavigationInterface {
    fun navigateToLogin(email: String? = null)
    fun navigateToRegister()
    fun navigateToResetPassword()
}
