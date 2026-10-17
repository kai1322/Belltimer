package com.belltimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Notifier {
    private const val CHANNEL = "bell_timer_status"
    private const val ID = 1

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL, "สถานะตัวจับเวลา", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        nm.createNotificationChannel(ch)
    }

    fun show(ctx: Context) {
        ensureChannel(ctx)
        val p = Prefs(ctx)
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val text = "ครั้งถัดไป ${fmt.format(Date(p.nextMs))} · ทุก ${Fmt.long(p.runIntervalMin)}"
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags)
        val stop = PendingIntent.getBroadcast(
            ctx, 2,
            Intent(ctx, AlarmReceiver::class.java).setAction(Scheduler.ACTION_STOP),
            flags
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Bell-timer ทำงานอยู่")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(0, "หยุด", stop)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(ID, n)
        } catch (e: SecurityException) {
            // ยังไม่ได้อนุญาตการแจ้งเตือน — ตัวปลุกยังทำงานปกติ
        }
    }

    fun cancel(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(ID)
    }
}
