package com.example.moodle.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.support.v7.app.AppCompatActivity   // <-- legacy (NOT androidx)
// import androidx.activity.viewModels
// import androidx.appcompat.app.AppCompatActivity
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.ViewModelProvider
import com.example.moodle.R
// import com.example.moodle.data.api.ApiClient
// import com.example.moodle.data.datastore.UserPreferences
// import com.example.moodle.data.repository.AuthRepository
// import com.example.moodle.ui.login.LoginViewModel.UiState
// import com.example.moodle.ui.recorder.RecorderActivity

class LoginActivity : AppCompatActivity() {

    // // Simple factory so we can pass repo without DI
    // private val viewModel: LoginViewModel by viewModels {
    //     object : ViewModelProvider.Factory {
    //         @Suppress("UNCHECKED_CAST")
    //         override fun <T : ViewModel> create(modelClass: Class<T>): T {
    //             val prefs = UserPreferences(applicationContext)
    //             val repo = AuthRepository(ApiClient.instance, prefs)
    //             return LoginViewModel(repo) as T
    //         }
    //     }
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // val userEt = findViewById<EditText>(R.id.usernameInput)
        // val passEt = findViewById<EditText>(R.id.passwordInput)
        // val loginBtn = findViewById<Button>(R.id.loginButton)

        // loginBtn.setOnClickListener {
        //     val u = userEt.text.toString().trim()
        //     val p = passEt.text.toString()
        //     if (u.isEmpty() || p.isEmpty()) {
        //         Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
        //         return@setOnClickListener
        //     }
        //     viewModel.login(u, p)
        // }

        // viewModel.state.observe(this) { state ->
        //     when (state) {
        //         is UiState.Loading -> {
        //             loginBtn.isEnabled = false
        //             loginBtn.text = "Signing in…"
        //         }
        //         is UiState.Success -> {
        //             Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        //             startActivity(Intent(this, RecorderActivity::class.java))
        //             finish()
        //         }
        //         is UiState.Error -> {
        //             loginBtn.isEnabled = true
        //             loginBtn.text = "Login"
        //             Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
        //         }
        //         else -> Unit
        //     }
        // }
    }
}
