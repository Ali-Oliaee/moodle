package com.example.moodle.ui.whiteboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.moodle.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        const val ACTION_START = "com.example.moodle.action.START_RECORDING"
        const val ACTION_STOP = "com.example.moodle.action.STOP_RECORDING"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA_INTENT = "data_intent"
        private const val CHANNEL_ID = "recording"
        private const val NOTIF_ID = 1001
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var width = 0
    private var height = 0
    private var densityDpi = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        if (mediaProjection != null) return // already running

        createNotificationChannel()
        
        // Start foreground with proper service type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification("Recording…"),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else startForeground(NOTIF_ID, buildNotification("Recording…"))

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA_INTENT)
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data!!)

        // Get display metrics
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        densityDpi = metrics.densityDpi

        val size = Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealSize(size)
        width = size.x
        height = size.y

        currentOutputFile = createOutputFile()
        
        // Check if we can write to the output file
        if (!currentOutputFile!!.parentFile.canWrite()) {
            Log.e("ScreenRecordService", "Cannot write to output directory: ${currentOutputFile!!.parentFile.absolutePath}")
            Toast.makeText(this, "Cannot write to Downloads folder. Check permissions.", Toast.LENGTH_LONG).show()
            stopForeground(true)
            stopSelf()
            return
        }
        
        // Prepare recorder with audio and video
        mediaRecorder = MediaRecorder().apply {
            // IMPORTANT: Set audio source first, then video source
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(currentOutputFile!!.absolutePath)
            
            // Configure audio encoding
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(256_000) // 256 kbps
            setAudioSamplingRate(48000) // 48 kHz
            
            // Configure video encoding
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(8_000_000) // ~8 Mbps
            setVideoFrameRate(30)
            setVideoSize(width, height)
            
            try {
                prepare()
                Log.i("ScreenRecordService", "MediaRecorder prepared successfully with audio and video")
            } catch (e: Exception) {
                Log.e("ScreenRecordService", "prepare() failed", e)
                Toast.makeText(this@ScreenRecordService, "Failed to prepare recorder: ${e.message}", Toast.LENGTH_LONG).show()
                stopForeground(true)
                stopSelf()
                return
            }
        }

        // Create virtual display that feeds the recorder's surface
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "MoodleScreen",
            width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface, null, null
        )

        try {
            mediaRecorder?.start()
            Log.i("ScreenRecordService", "Recording started: ${currentOutputFile!!.absolutePath}")
            // Broadcast recording started
            sendBroadcast(Intent("RECORDING_STARTED"))
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "start() failed", e)
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.w("ScreenRecordService", "stop() error (may be harmless): ${e.message}")
        }
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        // Notify media scanner and user about the saved file
        currentOutputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                // Scan the file so it appears in file managers
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(file.absolutePath),
                    arrayOf("video/mp4")
                ) { path, uri ->
                    Log.i("ScreenRecordService", "Media scanner completed for: $path, URI: $uri")
                }
                
                // Show toast notification
                Toast.makeText(
                    this,
                    "Recording saved to Downloads/MoodleScreenRecordings/\n${file.name}",
                    Toast.LENGTH_LONG
                ).show()
                
                Log.i("ScreenRecordService", "Recording saved: ${file.absolutePath}, Size: ${file.length()} bytes")
            } else {
                Toast.makeText(
                    this,
                    "Recording failed - file not created or empty",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("ScreenRecordService", "Recording file does not exist or is empty: ${file.absolutePath}")
            }
        }

        stopForeground(true)
        stopSelf()
        currentOutputFile = null
        
        // Broadcast recording stopped
        sendBroadcast(Intent("RECORDING_STOPPED"))
    }

    private fun createOutputFile(): File {
        // Use the public Downloads directory
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val moodleDir = File(downloadsDir, "MoodleScreenRecordings")
        
        // Create directory if it doesn't exist
        if (!moodleDir.exists()) {
            val created = moodleDir.mkdirs()
            Log.d("ScreenRecordService", "Created directory: $created, Path: ${moodleDir.absolutePath}")
        }
        
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val outputFile = File(moodleDir, "MoodleScreenRec_$ts.mp4")
        
        Log.i("ScreenRecordService", "Output file will be: ${outputFile.absolutePath}")
        Log.i("ScreenRecordService", "Directory exists: ${moodleDir.exists()}, Directory writable: ${moodleDir.canWrite()}")
        
        return outputFile
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Screen Recording",
                    NotificationManager.IMPORTANCE_LOW
                )
                mgr.createNotificationChannel(ch)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Moodle Screen Recorder")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
