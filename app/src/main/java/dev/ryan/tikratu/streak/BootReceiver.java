package dev.ryan.tikratu.streak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arma el recordatorio de streak despues de un reinicio, si estaba activado. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            StreakReminderScheduler.rearmIfEnabled(context);
        }
    }
}
