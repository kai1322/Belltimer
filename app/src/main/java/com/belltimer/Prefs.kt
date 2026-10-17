package com.belltimer

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("bell_timer", Context.MODE_PRIVATE)

    /** ช่วงเวลาที่ผู้ใช้เลือกในหน้าจอ (นาที) */
    var intervalMin: Int
        get() = sp.getInt("interval_min", 5).coerceIn(1, 240)
        set(v) { sp.edit().putInt("interval_min", v.coerceIn(1, 240)).commit() }

    /** จำนวนรอบ "ปิ๊ง ปิ๊ง" ต่อครั้ง 1–3 */
    var repeat: Int
        get() = sp.getInt("repeat", 1).coerceIn(1, 3)
        set(v) { sp.edit().putInt("repeat", v.coerceIn(1, 3)).commit() }

    /** ความดัง 5–100 */
    var volume: Int
        get() = sp.getInt("volume", 80).coerceIn(5, 100)
        set(v) { sp.edit().putInt("volume", v.coerceIn(5, 100)).commit() }

    var running: Boolean
        get() = sp.getBoolean("running", false)
        set(v) { sp.edit().putBoolean("running", v).commit() }

    /** เวลา (epoch ms) ตอนกดเริ่ม */
    var startMs: Long
        get() = sp.getLong("start_ms", 0L)
        set(v) { sp.edit().putLong("start_ms", v).commit() }

    /** ช่วงเวลาที่ใช้จริงตอนกดเริ่ม (นาที) */
    var runIntervalMin: Int
        get() = sp.getInt("run_interval_min", 5).coerceIn(1, 240)
        set(v) { sp.edit().putInt("run_interval_min", v.coerceIn(1, 240)).commit() }

    /** เวลาที่ตั้งปลุกครั้งถัดไป */
    var nextMs: Long
        get() = sp.getLong("next_ms", 0L)
        set(v) { sp.edit().putLong("next_ms", v).commit() }
}
