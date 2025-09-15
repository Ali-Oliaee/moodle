// package com.example.moodle

// import android.app.Notification
// import android.app.NotificationChannel
// import android.app.NotificationManager
// import android.app.Service
// import android.content.Context
// import android.content.Intent
// import android.content.ContentValues
// import android.net.Uri
// import android.graphics.Point
// import android.media.MediaRecorder
// import android.media.projection.MediaProjection
// import android.media.projection.MediaProjectionManager
// import android.os.Build
// import android.os.Environment
// import android.os.IBinder
// import android.util.DisplayMetrics
// import android.util.Log
// import android.view.WindowManager
// import androidx.core.app.NotificationCompat
// import android.hardware.display.DisplayManager
// import android.hardware.display.VirtualDisplay
// import android.provider.MediaStore
// import java.io.File
// import android.os.ParcelFileDescriptor
// import java.text.SimpleDateFormat
// import java.util.Locale

// class ScreenRecordService : Service() {

//     companion object {
//         const val ACTION_START = "com.example.moodle.action.START_RECORDING"
//         const val ACTION_STOP  = "com.example.moodle.action.STOP_RECORDING"
//         const val EXTRA_RESULT_CODE = "result_code"
//         const val EXTRA_DATA_INTENT = "data_intent"
//         private const val CHANNEL_ID = "recording"
//         private const val NOTIF_ID = 1001
//     }

//     private var mediaProjection: MediaProjection? = null
//     private var virtualDisplay: VirtualDisplay? = null
//     private var mediaRecorder: MediaRecorder? = null
//     private var pfd: ParcelFileDescriptor? = null
//     private var width = 0
//     private var height = 0
//     private var densityDpi = 0

//     override fun onBind(intent: Intent?): IBinder? = null

//     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//         when (intent?.action) {
//             ACTION_START -> startRecording(intent)
//             ACTION_STOP  -> stopRecording()
//         }
//         return START_NOT_STICKY
//     }

//     private fun createOutputFileUri(): Uri? {
//         val resolver = contentResolver
//         val values = ContentValues().apply {
//             put(MediaStore.Downloads.DISPLAY_NAME,
//                 "ScreenRec_${System.currentTimeMillis()}.mp4")
//             put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
//             // Relative path inside Downloads
//             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                 put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
//             }
//         }
    
//         return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
//     }

//     private fun startRecording(intent: Intent) {
//         if (mediaProjection != null) return // already running

//         createNotificationChannel()
//         startForeground(NOTIF_ID, buildNotification("Recording…"))

//         val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
//         val data = intent.getParcelableExtra<Intent>(EXTRA_DATA_INTENT)
//         val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//         mediaProjection = mpm.getMediaProjection(resultCode, data!!)

//         // Get display metrics
//         val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//         val metrics = DisplayMetrics()
//         @Suppress("DEPRECATION")
//         wm.defaultDisplay.getMetrics(metrics)
//         densityDpi = metrics.densityDpi

//         val size = Point()
//         @Suppress("DEPRECATION")
//         wm.defaultDisplay.getRealSize(size)
//         width = size.x
//         height = size.y

//         val uri = createOutputFileUri()
//         if (uri == null) {
//             Log.e("ScreenRecordService", "Failed to create file in Downloads")
//             stopSelf()
//             return
//         }

//         // Prepare recorder
//         pfd = contentResolver.openFileDescriptor(uri, "w") ?: return
//         mediaRecorder = MediaRecorder().apply {
//             setVideoSource(MediaRecorder.VideoSource.SURFACE)
//             setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//             setOutputFile(pfd!!.fileDescriptor)
//             setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//             setVideoEncodingBitRate(8_000_000) // ~8 Mbps
//             setVideoFrameRate(30)
//             setVideoSize(width, height)
//             try {
//                 prepare()
//             } catch (e: Exception) {
//                 Log.e("ScreenRecordService", "prepare() failed", e)
//                 stopForeground(true)
//                 stopSelf()
//                 return
//             }
//         }

//         // Create virtual display that feeds the recorder's surface
//         virtualDisplay = mediaProjection?.createVirtualDisplay(
//             "MoodleScreen",
//             width, height, densityDpi,
//             DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//             mediaRecorder!!.surface, null, null
//         )

//         try {
//             mediaRecorder?.start()
//         } catch (e: Exception) {
//             Log.e("ScreenRecordService", "start() failed", e)
//             stopRecording()
//         }
//     }

//     private fun stopRecording() {
//         try {
//             mediaRecorder?.apply {
//                 stop()
//                 reset()
//                 release()
//             }
//         } catch (e: Exception) {
//             Log.w("ScreenRecordService", "stop() error (may be harmless): ${e.message}")
//         }
//         mediaRecorder = null

//         virtualDisplay?.release()
//         virtualDisplay = null

//         mediaProjection?.stop()
//         mediaProjection = null

//         stopForeground(true)
//         stopSelf()
//         pfd?.close()
//         pfd = null
//     }

//     private fun outputFile(): File {
//         val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
//         if (!dir.exists()) dir.mkdirs()
//         val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
//         return File(dir, "ScreenRec_$ts.mp4")
//     }

//     private fun createNotificationChannel() {
//         if (Build.VERSION.SDK_INT >= 26) {
//             val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//             if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
//                 val ch = NotificationChannel(
//                     CHANNEL_ID,
//                     "Screen Recording",
//                     NotificationManager.IMPORTANCE_LOW
//                 )
//                 mgr.createNotificationChannel(ch)
//             }
//         }
//     }

//     private fun buildNotification(text: String): Notification {
//         return NotificationCompat.Builder(this, CHANNEL_ID)
//             .setSmallIcon(R.mipmap.ic_launcher) // use your icon
//             .setContentTitle("Moodle")
//             .setContentText(text)
//             .setOngoing(true)
//             .build()
//     }

//     override fun onDestroy() {
//         stopRecording()
//         super.onDestroy()
//     }
// }
