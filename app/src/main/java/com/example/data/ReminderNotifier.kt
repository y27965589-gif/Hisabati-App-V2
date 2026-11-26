package com.example.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FinanceUtils.currentYM
import com.example.data.FinanceUtils.daysUntil
import com.example.data.FinanceUtils.debtRemaining
import com.example.data.FinanceUtils.fmt
import com.example.data.FinanceUtils.localDateISO
import com.example.model.AppPage
import com.example.model.SandooqiDB
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max

data class DueReminderAlert(
    val key: String,
    val title: String,
    val message: String,
    val targetPage: AppPage,
    val priority: Int, // lower = more urgent
)

object ReminderNotifier {

    const val CHANNEL_ID = "sandooqi_due_reminders"
    const val EXTRA_TARGET_PAGE = "target_page"
    private const val PREFS_NAME = "sandooqi_notif_prefs"
    private const val KEY_LAST_DAY = "last_notified_day"
    private const val KEY_SENT_KEYS = "sent_keys_today"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تذكيرات صندوقي والمواعيد",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "تنبيهات تلقائية لمواعيد تسليم الطلبيات، الديون، الكراء، والفواتير"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun collectDueAlerts(db: SandooqiDB, leadDays: Int = db.notificationLeadDays): List<DueReminderAlert> {
        val lead = leadDays.coerceIn(0, 7)
        val alerts = mutableListOf<DueReminderAlert>()
        val ym = currentYM()
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // 1. Tailoring orders (delivery due dates & fittings)
        for (o in db.orders) {
            if (o.stage == "مسلّم") continue
            val dDue = daysUntil(o.dueDate)
            if (dDue != null && dDue <= lead) {
                val desc = when {
                    dDue < 0 -> "طلبية متأخرة بـ ${-dDue} يوم (${o.model})"
                    dDue == 0 -> "موعد التسليم اليوم (${o.model})"
                    dDue == 1 -> "موعد التسليم غداً (${o.model})"
                    else -> "متبقي $dDue أيام للتسليم (${o.model})"
                }
                alerts.add(
                    DueReminderAlert(
                        key = "order_${o.id}_${o.dueDate}",
                        title = "خياطة: ${o.client}",
                        message = desc,
                        targetPage = AppPage.TAILORING,
                        priority = if (dDue <= 0) 0 else 1,
                    )
                )
            } else {
                val dFit = daysUntil(o.fittingDate)
                if (dFit != null && dFit in 0..minOf(1, lead)) {
                    alerts.add(
                        DueReminderAlert(
                            key = "fitting_${o.id}_${o.fittingDate}",
                            title = "موعد قياس (بروفة): ${o.client}",
                            message = "${if (dFit == 0) "اليوم" else "غداً"} — ${o.model}",
                            targetPage = AppPage.TAILORING,
                            priority = 2,
                        )
                    )
                }
            }
        }

        // 2. Home debts & receivables
        for (d in db.homeDebts) {
            val rem = debtRemaining(d)
            if (rem <= 0.0) continue
            val days = daysUntil(d.due) ?: continue
            if (days <= lead) {
                val isReceivable = d.direction == "in"
                val timing = when {
                    days < 0 -> "متأخر بـ ${-days} يوم"
                    days == 0 -> "مستحق اليوم"
                    days == 1 -> "مستحق غداً"
                    else -> "خلال $days أيام"
                }
                alerts.add(
                    DueReminderAlert(
                        key = "debt_${d.id}_${d.due}",
                        title = if (isReceivable) "تحصيل مستحق من ${d.creditor}" else "دين مستحق لـ ${d.creditor}",
                        message = "المتبقي ${fmt(rem)} · $timing",
                        targetPage = AppPage.BILLS,
                        priority = if (days <= 0) 0 else 1,
                    )
                )
            }
        }

        // 3. Rent obligations/receivables
        for (r in db.rentDebts) {
            if (r.settled) continue
            val days = daysUntil(r.due) ?: continue
            if (days <= lead) {
                val timing = when {
                    days < 0 -> "متأخر بـ ${-days} يوم"
                    days == 0 -> "مستحق اليوم"
                    else -> "خلال $days أيام"
                }
                alerts.add(
                    DueReminderAlert(
                        key = "rent_${r.id}_${r.due}",
                        title = "${r.unit}: ${r.party}",
                        message = "${fmt(r.amount)} · $timing",
                        targetPage = AppPage.BILLS,
                        priority = if (days <= 0) 0 else 2,
                    )
                )
            }
        }

        // 4. Monthly bills / obligations
        for (ob in db.obligations) {
            if (ym in ob.paidMonths) continue
            val diff = ob.dueDay - todayDay
            if (diff <= lead) {
                val timing = when {
                    diff < 0 -> "تجاوز يوم الاستحقاق (${ob.dueDay})"
                    diff == 0 -> "مستحقة اليوم (${ob.dueDay} في الشهر)"
                    else -> "تستحق خلال $diff أيام"
                }
                alerts.add(
                    DueReminderAlert(
                        key = "bill_${ob.id}_$ym",
                        title = "فاتورة غير مدفوعة: ${ob.name}",
                        message = "${fmt(ob.amount)} · $timing",
                        targetPage = AppPage.BILLS,
                        priority = if (diff <= 0) 1 else 2,
                    )
                )
            }
        }

        // 5. Tailoring debts
        for (sd in db.debts) {
            val rem = max(0.0, sd.amount - sd.paid)
            if (rem <= 0.0) continue
            val days = daysUntil(sd.due) ?: continue
            if (days <= lead) {
                alerts.add(
                    DueReminderAlert(
                        key = "sewdebt_${sd.id}_${sd.due}",
                        title = "دين ورشة: ${sd.name}",
                        message = "المتبقي ${fmt(rem)}",
                        targetPage = AppPage.TAILORING,
                        priority = 2,
                    )
                )
            }
        }

        // 6. Expiring warranties within 14 days
        for (w in db.warranties) {
            val days = daysUntil(w.warrantyEndDateIso) ?: continue
            if (days in 0..maxOf(7, lead)) {
                alerts.add(
                    DueReminderAlert(
                        key = "warranty_${w.id}_${w.warrantyEndDateIso}",
                        title = "انتهاء ضمان: ${w.deviceName}",
                        message = if (days == 0) "ينتهي اليوم" else "متبقي $days يوم على انتهاء الضمان",
                        targetPage = AppPage.TOOLS,
                        priority = 3,
                    )
                )
            }
        }

        return alerts.sortedBy { it.priority }
    }

    /**
     * Checks due alerts and posts system notifications.
     * Returns the count of notifications posted (0 if none or permission missing).
     */
    fun checkAndNotify(context: Context, db: SandooqiDB, force: Boolean = false): Int {
        if (!force && !db.notificationsEnabled) return 0
        if (!hasNotificationPermission(context)) return 0

        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        val alerts = collectDueAlerts(db)

        if (alerts.isEmpty()) {
            if (force) {
                postSingleNotification(
                    context = context,
                    nm = nm,
                    notificationId = 9001,
                    title = "إشعارات صندوقي مفعّلة",
                    message = "نظام التذكير يعمل بنجاح — لا توجد مواعيد أو ديون قريبة حالياً.",
                    targetPage = AppPage.HOME,
                )
                return 1
            }
            return 0
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = localDateISO()
        val lastDay = prefs.getString(KEY_LAST_DAY, "")
        val alreadySentToday = if (lastDay == today) {
            prefs.getStringSet(KEY_SENT_KEYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        } else {
            mutableSetOf()
        }

        val toSend = if (force) alerts else alerts.filter { it.key !in alreadySentToday }
        if (toSend.isEmpty()) return 0

        var posted = 0
        if (toSend.size <= 3) {
            toSend.forEachIndexed { idx, alert ->
                postSingleNotification(
                    context = context,
                    nm = nm,
                    notificationId = 9100 + idx,
                    title = alert.title,
                    message = alert.message,
                    targetPage = alert.targetPage,
                )
                posted++
            }
        } else {
            val top2 = toSend.take(2)
            top2.forEachIndexed { idx, alert ->
                postSingleNotification(
                    context = context,
                    nm = nm,
                    notificationId = 9100 + idx,
                    title = alert.title,
                    message = alert.message,
                    targetPage = alert.targetPage,
                )
                posted++
            }
            val restCount = toSend.size - 2
            val previewNames = toSend.drop(2).take(3).joinToString("، ") { it.title }
            postSingleNotification(
                context = context,
                nm = nm,
                notificationId = 9199,
                title = "لديك ${toSend.size} تذكيرات مالية ومواعيد",
                message = "بالإضافة لـ $restCount أخرى: $previewNames",
                targetPage = AppPage.HOME,
            )
            posted++
        }

        if (!force) {
            alreadySentToday.addAll(toSend.map { it.key })
            prefs.edit()
                .putString(KEY_LAST_DAY, today)
                .putStringSet(KEY_SENT_KEYS, alreadySentToday)
                .apply()
        }

        return posted
    }

    private fun postSingleNotification(
        context: Context,
        nm: NotificationManagerCompat,
        notificationId: Int,
        title: String,
        message: String,
        targetPage: AppPage,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_PAGE, targetPage.name)
        }
        val pi = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.sandooqi_icon_src)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)

        runCatching {
            nm.notify(notificationId, builder.build())
        }
    }
}

object ReminderScheduler {

    private const val REQUEST_CODE = 7701

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun scheduleDailyCheck(context: Context) {
        ReminderNotifier.ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            buildPendingIntent(context),
        )
    }

    fun cancelDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(buildPendingIntent(context))
    }

    fun syncSchedule(context: Context, enabled: Boolean) {
        if (enabled) scheduleDailyCheck(context) else cancelDailyCheck(context)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = SandooqiDatabase.getInstance(context).sandooqiDao().getStateOnce()
                if (entity != null) {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val parsed = moshi.adapter(SandooqiDB::class.java).fromJson(entity.json)
                    val db = FinanceUtils.normalizeSandooqiDB(parsed)
                    if (db.notificationsEnabled) {
                        ReminderNotifier.checkAndNotify(context, db, force = false)
                    }
                }
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BootReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = SandooqiDatabase.getInstance(context).sandooqiDao().getStateOnce()
                val enabled = if (entity != null) {
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val parsed = moshi.adapter(SandooqiDB::class.java).fromJson(entity.json)
                    FinanceUtils.normalizeSandooqiDB(parsed).notificationsEnabled
                } else true
                ReminderScheduler.syncSchedule(context, enabled)
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
