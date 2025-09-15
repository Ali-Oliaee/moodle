package com.example.moodle.ui.recorder

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.moodle.R
import com.example.moodle.ScreenRecordService

class RecorderActivity : AppCompatActivity() {

    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_START
                putExtra(ScreenRecordService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecordService.EXTRA_DATA_INTENT, result.data)
            }
            startForegroundService(intent)
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorder)

        val startBtn = findViewById<Button>(R.id.startRecordBtn)
        val stopBtn = findViewById<Button>(R.id.stopRecordBtn)

        startBtn.setOnClickListener {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            captureLauncher.launch(captureIntent)
        }

        stopBtn.setOnClickListener {
            val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_STOP
            }
            startService(stopIntent)
            Toast.makeText(this, "Stopping…", Toast.LENGTH_SHORT).show()
        }
    }
}
