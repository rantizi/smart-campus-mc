package com.unpad.padukampus.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavDeepLinkBuilder
import com.unpad.padukampus.R
import com.unpad.padukampus.core.constants.AppConstants
import com.unpad.padukampus.core.util.DateTimeUtils
import com.unpad.padukampus.data.local.entity.TugasEntity
import com.unpad.padukampus.ui.main.MainActivity
import com.unpad.padukampus.ui.tasks.TaskFormFragmentArgs

class NotificationHelper(private val context: Context) {
    private val delivered = context.getSharedPreferences("reminder_delivery", Context.MODE_PRIVATE)

    fun createChannel() {
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    AppConstants.REMINDER_CHANNEL_ID,
                    "Pengingat tugas",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
    }

    fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED)

    fun showTaskReminder(task: TugasEntity) {
        if (!canPost() || delivered.getLong(task.id, -1) == task.updatedAt) return
        val pendingIntent =
            NavDeepLinkBuilder(context)
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.taskFormFragment)
                .setArguments(TaskFormFragmentArgs(taskId = task.id).toBundle())
                .createPendingIntent()
        val notification =
            NotificationCompat.Builder(context, AppConstants.REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pengingat tugas")
                .setContentText(task.judul)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${task.judul}\n${DateTimeUtils.formatDeadline(task.deadline)}")
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        try {
            NotificationManagerCompat.from(context).notify(task.id, 1, notification)
            delivered.edit { putLong(task.id, task.updatedAt) }
        } catch (_: SecurityException) {
            /* Permission can be revoked between the check and notify. */
        }
    }

    fun cancelTask(id: String) {
        NotificationManagerCompat.from(context).cancel(id, 1)
    }

    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
        delivered.edit { clear() }
    }
}
