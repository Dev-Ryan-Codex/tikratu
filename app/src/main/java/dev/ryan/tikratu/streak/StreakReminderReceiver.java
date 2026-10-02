package dev.ryan.tikratu.streak;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class StreakReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "tikratu_streak_reminder";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
        StreakReminderScheduler.rearmIfEnabled(context);
    }

    private void showNotification(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Renovación de racha", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // Sin permiso de notificaciones, no forzamos nada.
        }

        Intent openTikTok = context.getPackageManager().getLaunchIntentForPackage("com.zhiliaoapp.musically");
        PendingIntent contentIntent = null;
        if (openTikTok != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            contentIntent = PendingIntent.getActivity(context, 0, openTikTok, flags);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("TikRatu — Racha")
                .setContentText("Renová tu racha de TikTok hoy 🔥")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
