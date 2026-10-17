package com.belltimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Scheduler.ACTION_RING -> ring(ctx, intent)
            Scheduler.ACTION_STOP -> Scheduler.stop(ctx)
        }
    }

    private fun ring(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        if (!p.running) return

        // 1) ตั้งรอบถัดไปก่อนเสมอ โดยคำนวณจากเวลาเริ่ม (ไม่เลื่อนสะสม)
        val n = intent.getLongExtra("n", 0L)
        val intervalMs = p.runIntervalMin * 60_000L
        val next = maxOf(n + 1, Scheduler.indexAfter(p.startMs, intervalMs, System.currentTimeMillis()))
        Scheduler.schedule(ctx, next)

        // 2) อัปเดตแถบแจ้งเตือน
        Notifier.show(ctx)

        // 3) เล่นเสียง (สั้นกว่า 10 วินาที) ระหว่างถือ wake lock
        val pending = goAsync()
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "belltimer:ring")
        wl.acquire(15_000L)
        val repeat = p.repeat
        val volume = p.volume
        Thread {
            try {
                BellPlayer.play(repeat, volume)
            } finally {
                if (wl.isHeld) wl.release()
                pending.finish()
            }
        }.start()
    }
}
