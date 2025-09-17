package com.example.moodle.ui.login

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.WhiteboardActivity
import com.example.moodle.data.repository.AuthRepository
import com.example.moodle.data.datastore.UserPreferences
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository(UserPreferences(this))
        initializeViews()
        setupClickListeners()
        checkLoginStatus()
    }

    private fun initializeViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }

    private fun checkLoginStatus() {
        if (authRepository.isLoggedIn()) {
            navigateToWhiteboard()
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate inputs
        if (username.isBlank()) {
            showError("Username cannot be empty")
            return
        }
        
        if (password.isBlank()) {
            showError("Password cannot be empty")
            return
        }

        // Show loading and perform login
        showLoading()
        hideError()
        
        scope.launch {
            try {
                val result = authRepository.login(username, password)
                
                result.fold(
                    onSuccess = { loginResponse ->
                        hideLoading()
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToWhiteboard()
                    },
                    onFailure = { exception ->
                        hideLoading()
                        showError(exception.message ?: "Login failed")
                    }
                )
            } catch (e: Exception) {
                hideLoading()
                showError("Unexpected error: ${e.message}")
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
    }

    private fun showError(error: String) {
        tvError.text = error
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
        tvError.text = ""
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
