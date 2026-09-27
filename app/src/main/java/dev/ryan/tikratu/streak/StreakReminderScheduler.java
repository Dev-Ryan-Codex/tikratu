package dev.ryan.tikratu.streak;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * Recordatorio LOCAL de streak — a diferencia del "AutoStreak" del mod original
 * (que mandaba un mensaje real dentro de TikTok sin que el usuario tocara nada),
 * esto solo dispara una notificacion del sistema a una hora fija para que el
 * usuario mismo mande el mensaje. No hookea nada de TikTok, no automatiza
 * ninguna interaccion dentro de la app — es un despertador con Intent en vez
 * de sonido.
 */
public final class StreakReminderScheduler {

    private static final String PREFS = "tikratu_prefs";
    private static final String KEY_ENABLED = "streak_reminder_enabled";
    private static final String KEY_HOUR = "streak_reminder_hour";
    private static final String KEY_MINUTE = "streak_reminder_minute";
    private static final int REQUEST_CODE = 1001;

    private StreakReminderScheduler() {
    }

    public static void schedule(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_ENABLED, true)
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .apply();

        armNextAlarm(context, hour, minute);
    }

    public static void cancel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, false).apply();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(buildPendingIntent(context));
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static int getHour(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HOUR, 20);
    }

    public static int getMinute(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MINUTE, 0);
    }

    /** Llamado por StreakReminderReceiver despues de mostrar la notificacion, y por BootReceiver. */
    public static void rearmIfEnabled(Context context) {
        if (!isEnabled(context)) return;
        armNextAlarm(context, getHour(context), getMinute(context));
    }

    private static void armNextAlarm(Context context, int hour, int minute) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = buildPendingIntent(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
        }
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, StreakReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }
}
