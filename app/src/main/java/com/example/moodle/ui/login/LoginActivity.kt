package com.example.moodle.ui.login

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.moodle.BuildConfig
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.WhiteboardActivity
import com.example.moodle.data.repository.AuthRepository
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.utils.FontHelper
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var inputUsername: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository(UserPreferences(this))
        initializeViews()
        applyFonts()
        setupClickListeners()
        checkLoginStatus()
    }

    private fun initializeViews() {
        inputUsername = findViewById(R.id.inputUsername)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
    }

    private fun applyFonts() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        FontHelper.applyFontToViewGroup(this, rootView)
    }

    private fun checkLoginStatus() {
        if (authRepository.isLoggedIn()) navigateToWhiteboard()
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = inputUsername.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (username.isBlank()) {
            showError(getString(R.string.login_empty_username))
            return
        }
        if (password.isBlank()) {
            showError(getString(R.string.login_empty_password))
            return
        }

        showLoading()
        hideError()
        
        scope.launch {
            try {
                val result = authRepository.login(username, password)
                
                result.fold(
                    onSuccess = { loginResponse ->
                        hideLoading()
                        Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                        navigateToWhiteboard()
                    },
                    onFailure = { exception ->
                        hideLoading()
                        showError(exception.message ?: getString(R.string.login_error))
                    }
                )
            } catch (e: Exception) {
                hideLoading()
                showError(getString(R.string.login_error) + ": ${e.message}")
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        btnLogin.text = getString(R.string.login_loading)
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
        btnLogin.text = getString(R.string.login)
    }

    private fun showError(error: String) {
        errorText.text = error
        errorText.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText.visibility = View.GONE
        errorText.text = ""
    }

    private fun navigateToWhiteboard() {
        val intent = Intent(this, WhiteboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
