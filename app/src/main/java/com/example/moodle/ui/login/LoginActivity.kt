package com.example.moodle.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.support.v7.app.AppCompatActivity
import com.example.moodle.R
import com.example.moodle.ui.recorder.RecorderActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val screenRecorderBtn = findViewById<Button>(R.id.screenRecorderBtn)
        
        screenRecorderBtn.setOnClickListener {
            startActivity(Intent(this, RecorderActivity::class.java))
        }
    }
}
