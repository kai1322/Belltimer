package com.belltimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** ตั้งปลุกต่ออัตโนมัติหลังรีสตาร์ท / อัปเดตแอป / เปลี่ยนเวลาเครื่อง */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        if (!p.running) return
        val n = Scheduler.indexAfter(
            p.startMs, p.runIntervalMin * 60_000L, System.currentTimeMillis()
        )
        Scheduler.schedule(ctx, n)
        Notifier.show(ctx)
    }
}
