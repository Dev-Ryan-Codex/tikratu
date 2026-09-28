package dev.ryan.tikratu.streak;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import dev.ryan.tikratu.utils.AppPrefs;
import dev.ryan.tikratu.utils.Prefs;

/**
 * Recordatorio LOCAL de streak — a diferencia del "AutoStreak" del mod original
 * (que mandaba un mensaje real dentro de TikTok sin que el usuario tocara nada),
 * esto solo dispara una notificacion del sistema a una hora fija para que el
 * usuario mismo mande el mensaje. No hookea nada de TikTok, no automatiza
 * ninguna interaccion dentro de la app.
 *
 * Usa el archivo de SharedPreferences por defecto de la app (el mismo que
 * escriben los SwitchPreferenceCompat de la UI), asi el switch en
 * prefs_streak.xml y este codigo quedan sincronizados sin logica extra.
 */
public final class StreakReminderScheduler {

    private static final int REQUEST_CODE = 1001;

    private StreakReminderScheduler() {
    }

    /** Prende o apaga el recordatorio (llamado desde el switch de la UI). */
    public static void setEnabled(Context context, boolean enabled) {
        AppPrefs.putBoolean(context, Prefs.KEY_STREAK_ENABLED, enabled);
        if (enabled) {
            armNextAlarm(context, getHour(context), getMinute(context));
        } else {
            cancelAlarm(context);
        }
    }

    /** Cambia la hora guardada. Si el recordatorio ya estaba activo, lo reprograma. */
    public static void setTime(Context context, int hour, int minute) {
        AppPrefs.putInt(context, Prefs.KEY_STREAK_HOUR, hour);
        AppPrefs.putInt(context, Prefs.KEY_STREAK_MINUTE, minute);
        if (isEnabled(context)) {
            armNextAlarm(context, hour, minute);
        }
    }

    public static boolean isEnabled(Context context) {
        return AppPrefs.getBoolean(context, Prefs.KEY_STREAK_ENABLED, Prefs.DEFAULT_STREAK_ENABLED);
    }

    public static int getHour(Context context) {
        return AppPrefs.getInt(context, Prefs.KEY_STREAK_HOUR, Prefs.DEFAULT_STREAK_HOUR);
    }

    public static int getMinute(Context context) {
        return AppPrefs.getInt(context, Prefs.KEY_STREAK_MINUTE, Prefs.DEFAULT_STREAK_MINUTE);
    }

    /** Llamado por StreakReminderReceiver despues de mostrar la notificacion, y por BootReceiver. */
    public static void rearmIfEnabled(Context context) {
        if (!isEnabled(context)) return;
        armNextAlarm(context, getHour(context), getMinute(context));
    }

    private static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(buildPendingIntent(context));
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
