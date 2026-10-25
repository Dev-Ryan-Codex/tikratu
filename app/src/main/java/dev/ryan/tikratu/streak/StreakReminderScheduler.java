package dev.ryan.tikratu.streak;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import dev.ryan.tikratu.utils.AppPrefs;
import dev.ryan.tikratu.utils.Prefs;
import java.util.Calendar;

public final class StreakReminderScheduler {
    private static final int REQUEST_CODE = 1001;

    private StreakReminderScheduler() {
    }

    public static void setEnabled(Context context, boolean enabled) {
        AppPrefs.putBoolean(context, Prefs.KEY_STREAK_ENABLED, enabled);
        if (enabled) {
            armNextAlarm(context, getHour(context), getMinute(context));
        } else {
            cancelAlarm(context);
        }
    }

    public static void setTime(Context context, int hour, int minute) {
        AppPrefs.putInt(context, Prefs.KEY_STREAK_HOUR, hour);
        AppPrefs.putInt(context, Prefs.KEY_STREAK_MINUTE, minute);
        if (isEnabled(context)) {
            armNextAlarm(context, hour, minute);
        }
    }

    public static boolean isEnabled(Context context) {
        return AppPrefs.getBoolean(context, Prefs.KEY_STREAK_ENABLED, false);
    }

    public static int getHour(Context context) {
        return AppPrefs.getInt(context, Prefs.KEY_STREAK_HOUR, 20);
    }

    public static int getMinute(Context context) {
        return AppPrefs.getInt(context, Prefs.KEY_STREAK_MINUTE, 0);
    }

    public static void rearmIfEnabled(Context context) {
        if (isEnabled(context)) {
            armNextAlarm(context, getHour(context), getMinute(context));
        }
    }

    private static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        alarmManager.cancel(buildPendingIntent(context));
    }

    private static void armNextAlarm(Context context, int hour, int minute) {
        Calendar next = Calendar.getInstance();
        next.set(11, hour);
        next.set(12, minute);
        next.set(13, 0);
        if (next.before(Calendar.getInstance())) {
            next.add(6, 1);
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        PendingIntent pendingIntent = buildPendingIntent(context);
        long at = next.getTimeInMillis();
        try {
            // Android 12+: SCHEDULE_EXACT_ALARM viene denegado por defecto; sin el
            // permiso se usa una alarma inexacta (puede retrasarse unos minutos).
            if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(0, at, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(0, at, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(0, at, pendingIntent);
        }
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, (Class<?>) StreakReminderReceiver.class);
        int flags = 67108864 | 134217728;
        return PendingIntent.getBroadcast(context, 1001, intent, flags);
    }
}
