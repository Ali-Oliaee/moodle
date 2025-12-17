# Explain application recording process
_Exported on 12/11/2025 at 09:45:11 GMT+3:30 from Cursor (2.1.46)_

---

**User**

explain simple how the recording process happends in this application?

---

**Cursor**

I'll read the relevant files to explain the recording process.

## How Recording Works in This App

Here's a simple breakdown of the screen recording process:

### **1. User Triggers Recording**
When the user taps the record button, `RecordingController.toggleRecording()` is called.

### **2. Permission Checks**
Before recording can start, the app checks for required permissions (in order):
- **Notification permission** (Android 13+) — needed for foreground service
- **Microphone permission** — to record audio
- **Storage permission** — to save the video file

### **3. Request Screen Capture Access**
Once permissions are granted, Android shows a system dialog asking the user to allow screen capture:
```
mediaProjectionManager.createScreenCaptureIntent()
```

### **4. Start the Recording Service**
If the user approves, `ScreenRecordService` is started as a **foreground service** (with a notification showing "Recording…").

### **5. Recording Setup**
Inside `ScreenRecordService.startRecording()`:
1. **Get MediaProjection** — the system's screen capture API
2. **Get screen dimensions** — width, height, and density
3. **Create output file** — saves to `Downloads/MoodleScreenRecordings/MoodleScreenRec_[timestamp].mp4`
4. **Configure MediaRecorder** with:
   - Audio: Microphone → AAC codec @ 256kbps
   - Video: Screen surface → H264 codec @ 8Mbps, 30fps
5. **Create VirtualDisplay** — mirrors the screen to the recorder's surface
6. **Start recording** — `mediaRecorder.start()`

### **6. Stop Recording**
When the user taps the button again:
1. `ACTION_STOP` is sent to the service
2. MediaRecorder stops and releases resources
3. MediaProjection and VirtualDisplay are released
4. The file is scanned so it appears in file managers
5. A toast shows where the file was saved

### **Simple Flow Diagram**
```
User taps Record → Check Permissions → System Permission Dialog
       ↓
   User Approves → Start ScreenRecordService (foreground)
       ↓
   Setup: MediaProjection + MediaRecorder + VirtualDisplay
       ↓
   Recording in progress... (notification visible)
       ↓
   User taps Stop → Stop service → Save file to Downloads
```

The two classes work together: **RecordingController** handles the UI and permissions, while **ScreenRecordService** does the actual heavy lifting of capturing and encoding the screen.

```plaintext
mediaProjectionManager.createScreenCaptureIntent()
```

```plaintext
User taps Record → Check Permissions → System Permission Dialog
       ↓
   User Approves → Start ScreenRecordService (foreground)
       ↓
   Setup: MediaProjection + MediaRecorder + VirtualDisplay
       ↓
   Recording in progress... (notification visible)
       ↓
   User taps Stop → Stop service → Save file to Downloads
```

