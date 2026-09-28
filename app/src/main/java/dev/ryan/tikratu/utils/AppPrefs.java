package dev.ryan.tikratu.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Mismo nombre de archivo que usaba androidx.preference.PreferenceManager
 * .getDefaultSharedPreferences() ("<packageName>_preferences") — se mantiene
 * a mano para no depender de esa librería solo por este helper, y porque
 * XSharedPreferences(packageName) del lado Xposed asume exactamente ese
 * mismo nombre de archivo por defecto.
 */
public final class AppPrefs {
    private AppPrefs() {
    }

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
}
