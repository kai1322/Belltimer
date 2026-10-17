package com.belltimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Scheduler {
    const val ACTION_RING = "com.belltimer.action.RING"
    const val ACTION_STOP = "com.belltimer.action.STOP"

    /** ดัชนีรอบถัดไปที่ยังไม่ถึงเวลา โดยนับจาก start เสมอ: start + n * interval > now */
    fun indexAfter(startMs: Long, intervalMs: Long, nowMs: Long): Long {
        val n = Math.floorDiv(nowMs - startMs, intervalMs) + 1
        return if (n < 1) 1 else n
    }

    private fun ringIntent(ctx: Context, n: Long): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(ACTION_RING)
            .putExtra("n", n)
        return PendingIntent.getBroadcast(
            ctx, 1, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** ตั้งปลุกรอบที่ n ที่เวลา start + n * interval (exact alarm) */
    fun schedule(ctx: Context, n: Long) {
        val p = Prefs(ctx)
        val at = p.startMs + n * p.runIntervalMin * 60_000L
        p.nextMs = at
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = ringIntent(ctx, n)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun stop(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(ringIntent(ctx, 0))
        Prefs(ctx).running = false
        BellPlayer.stop()
        Notifier.cancel(ctx)
    }
}
