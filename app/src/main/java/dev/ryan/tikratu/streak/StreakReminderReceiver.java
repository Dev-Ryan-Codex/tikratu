package dev.ryan.tikratu.streak;

import android.R;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/* JADX INFO: loaded from: classes5.dex */
public class StreakReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "tikratu_streak_reminder";
    private static final int NOTIFICATION_ID = 2001;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
        StreakReminderScheduler.rearmIfEnabled(context);
    }

    private void showNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Renovación de racha", 3);
        manager.createNotificationChannel(channel);
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            return;
        }
        Intent openTikTok = context.getPackageManager().getLaunchIntentForPackage("com.zhiliaoapp.musically");
        PendingIntent contentIntent = null;
        if (openTikTok != null) {
            int flags = 67108864 | 134217728;
            contentIntent = PendingIntent.getActivity(context, 0, openTikTok, flags);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_dialog_info).setContentTitle("TikRatu — Racha").setContentText("Renová tu racha de TikTok hoy 🔥").setPriority(0).setAutoCancel(true);
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
        }
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
