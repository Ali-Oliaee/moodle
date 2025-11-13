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
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders

import com.example.moodle.R
import com.example.moodle.utils.FontHelper
import com.example.moodle.ui.whiteboard.WhiteboardActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var inputUsername: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var viewModel: LoginViewModel
    // TODO: remove this button after testing
    private lateinit var btnSkip: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)

        initializeViews()
        applyFonts()
        setupClickListeners()
        observeViewModel()
        checkLoginStatus()
    }

    private fun initializeViews() {
        inputUsername = findViewById(R.id.inputUsername)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        btnSkip = findViewById(R.id.btnSkip)
    }

    private fun applyFonts() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        FontHelper.applyFontToViewGroup(this, rootView)
    }

    private fun checkLoginStatus() {
        if (viewModel.getIsLoggedIn()) {
            navigateToWhiteboard()
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString()
            val password = inputPassword.text.toString()
            viewModel.performLogin(username, password)
        }
        btnSkip.setOnClickListener {
            navigateToWhiteboard()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this, Observer { state ->
            state?.let { handleState(it) }
        })
    }

    private fun handleState(state: LoginViewState) {
        updateLoadingState(state.isLoading)
        updateErrorState(state.error)
        
        if (state.shouldNavigate) {
            state.successMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            navigateToWhiteboard()
            viewModel.onNavigationHandled()
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.text = getString(R.string.login_loading)
        } else {
            progressBar.visibility = View.GONE
            btnLogin.isEnabled = true
            btnLogin.text = getString(R.string.login)
        }
    }

    private fun updateErrorState(error: String?) {
        if (error != null) {
            errorText.text = error
            errorText.visibility = View.VISIBLE
        } else {
            errorText.visibility = View.GONE
            errorText.text = ""
        }
    }

    private fun navigateToWhiteboard() {
        val intent = Intent(this, WhiteboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cancelJob()
    }
}
