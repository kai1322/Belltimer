package com.belltimer

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private lateinit var intervalText: TextView
    private lateinit var intervalSeek: SeekBar
    private lateinit var volumeText: TextView
    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private val intervalViews = mutableListOf<View>()

    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        Notifier.ensureChannel(this)
        setContentView(buildUi())
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun seekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
            if (fromUser) onChange(p)
        }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun buildUi(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }

        fun label(t: String) = TextView(this).apply {
            text = t
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(24), 0, dp(8))
        }

        col.addView(TextView(this).apply {
            text = "Bell-timer"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = "เตือนด้วยเสียงระฆังซ้ำ ไม่มีโฆษณา"
            textSize = 14f
        })

        // ---- ช่วงเวลา ----
        col.addView(label("ช่วงเวลา (1 นาที – 4 ชั่วโมง)"))
        intervalText = TextView(this).apply {
            textSize = 30f
            gravity = Gravity.CENTER
        }
        col.addView(intervalText, LinearLayout.LayoutParams(MATCH, WRAP))

        val minus = Button(this).apply {
            text = "−"
            setOnClickListener { setInterval(prefs.intervalMin - 1) }
        }
        val plus = Button(this).apply {
            text = "+"
            setOnClickListener { setInterval(prefs.intervalMin + 1) }
        }
        intervalSeek = SeekBar(this).apply {
            max = 239
            setOnSeekBarChangeListener(seekListener { setInterval(it + 1) })
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(minus)
        row.addView(intervalSeek, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(plus)
        col.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))

        val presetRow = LinearLayout(this)
        listOf(1, 5, 10, 15, 30, 60, 120, 240).forEach { m ->
            val b = Button(this).apply {
                text = Fmt.short(m)
                setOnClickListener { setInterval(m) }
            }
            intervalViews.add(b)
            presetRow.addView(b)
        }
        col.addView(HorizontalScrollView(this).apply { addView(presetRow) })
        intervalViews.addAll(listOf(minus, plus, intervalSeek))

        // ---- จำนวนครั้ง ----
        col.addView(label("จำนวนรอบเสียงต่อครั้ง (ปิ๊ง ปิ๊ง × N)"))
        val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val radios = (1..3).map { n ->
            RadioButton(this).apply {
                id = View.generateViewId()
                text = "$n ครั้ง"
                tag = n
                setPadding(dp(4), 0, dp(20), 0)
            }
        }
        radios.forEach { group.addView(it) }
        group.check(radios[prefs.repeat - 1].id)
        group.setOnCheckedChangeListener { g, id ->
            (g.findViewById<View>(id)?.tag as? Int)?.let { prefs.repeat = it }
        }
        col.addView(group)

        // ---- ความดัง ----
        volumeText = label("")
        col.addView(volumeText)
        val volSeek = SeekBar(this).apply {
            max = 95
            progress = prefs.volume - 5
            setOnSeekBarChangeListener(seekListener {
                prefs.volume = it + 5
                updateVolumeText()
            })
        }
        col.addView(volSeek, LinearLayout.LayoutParams(MATCH, WRAP))
        updateVolumeText()
        col.addView(Button(this).apply {
            text = "ลองฟังเสียง"
            setOnClickListener {
                val r = prefs.repeat
                val v = prefs.volume
                Thread { BellPlayer.play(r, v) }.start()
            }
        })

        // ---- เริ่ม/หยุด ----
        startBtn = Button(this).apply {
            textSize = 22f
            setOnClickListener { if (prefs.running) stopTimer() else startTimer() }
        }
        col.addView(startBtn, LinearLayout.LayoutParams(MATCH, dp(68)).apply { topMargin = dp(28) })
        statusText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        col.addView(statusText, LinearLayout.LayoutParams(MATCH, WRAP))

        // ---- ให้ทำงานต่อเนื่อง ----
        col.addView(label("ให้ทำงานต่อเนื่อง"))
        col.addView(Button(this).apply {
            text = "ยกเว้นการจำกัดแบตเตอรี่"
            setOnClickListener { requestBatteryExemption() }
        })
        col.addView(Button(this).apply {
            text = "Xiaomi/Redmi: เริ่มอัตโนมัติ"
            setOnClickListener { openMiuiAutostart() }
        })
        col.addView(Button(this).apply {
            text = "Xiaomi/Redmi: ประหยัดแบตเตอรี่ → ไม่มีข้อจำกัด"
            setOnClickListener { openMiuiBattery() }
        })
        col.addView(TextView(this).apply {
            text = "Xiaomi/Redmi (HyperOS/MIUI): เปิด \"เริ่มอัตโนมัติ\", ตั้งการประหยัดแบตเตอรี่ของแอปเป็น " +
                "\"ไม่มีข้อจำกัด\", และล็อกแอปในหน้าแอปล่าสุด (กดค้างที่การ์ดแอป → ไอคอนล็อก) " +
                "เพื่อไม่ให้ระบบปิดแอปเอง"
            textSize = 13f
            setPadding(0, dp(8), 0, 0)
        })

        setInterval(prefs.intervalMin)
        return ScrollView(this).apply { addView(col) }
    }

    private fun setInterval(m: Int) {
        val v = m.coerceIn(1, 240)
        prefs.intervalMin = v
        intervalSeek.progress = v - 1
        intervalText.text = Fmt.long(v)
    }

    private fun updateVolumeText() {
        volumeText.text = "ความดัง ${prefs.volume}%"
    }

    private fun startTimer() {
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            Toast.makeText(this, "ต้องอนุญาต \"ปลุกและการเตือน\" ก่อน", Toast.LENGTH_LONG).show()
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) { }
            return
        }
        prefs.startMs = System.currentTimeMillis()
        prefs.runIntervalMin = prefs.intervalMin
        prefs.running = true
        Scheduler.schedule(this, 1)
        Notifier.show(this)
        refresh()
    }

    private fun stopTimer() {
        Scheduler.stop(this)
        refresh()
    }

    private fun refresh() {
        val running = prefs.running
        startBtn.text = if (running) "หยุด" else "เริ่ม"
        intervalViews.forEach { it.isEnabled = !running }
        statusText.text = if (running) {
            val left = ((prefs.nextMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            "เริ่มเมื่อ ${timeFmt.format(Date(prefs.startMs))} · ทุก ${Fmt.long(prefs.runIntervalMin)}\n" +
                "ครั้งถัดไป ${timeFmt.format(Date(prefs.nextMs))} (อีก ${Fmt.countdown(left)})"
        } else {
            "ยังไม่ได้เริ่ม"
        }
    }

    // ---------- แบตเตอรี่ / Xiaomi ----------

    private fun requestBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "ยกเว้นแล้ว", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                openAppDetails()
            }
        }
    }

    private fun openMiuiAutostart() {
        try {
            startActivity(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            )
        } catch (e: Exception) {
            openAppDetails()
        }
    }

    private fun openMiuiBattery() {
        try {
            startActivity(
                Intent().setComponent(
                    ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                )
                    .putExtra("package_name", packageName)
                    .putExtra("package_label", getString(R.string.app_name))
            )
        } catch (e: Exception) {
            openAppDetails()
        }
    }

    private fun openAppDetails() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) { }
    }
}
