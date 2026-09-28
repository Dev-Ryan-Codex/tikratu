package dev.ryan.tikratu.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * Mismo nombre de archivo que usaba androidx.preference.PreferenceManager
 * .getDefaultSharedPreferences() ("<packageName>_preferences") — se mantiene
 * a mano para no depender de esa librería solo por este helper, y porque
 * XSharedPreferences(packageName) del lado Xposed asume exactamente ese
 * mismo nombre de archivo por defecto.
 *
 * BUG REAL ENCONTRADO EN DISPOSITIVO (2026-09-28): XSharedPreferences.
 * makeWorldReadable() se llama desde Module.java, pero corre DENTRO del
 * proceso de TikTok (UID ajeno al nuestro) — en Linux un proceso no puede
 * hacer chmod sobre un archivo que no le pertenece, así que ese intento
 * fallaba en silencio. Confirmado en dispositivo real: todos los toggles
 * con default=true "parecían" funcionar solo porque XSharedPreferences,
 * al no poder leer el archivo, siempre devolvía el valor default — el
 * primer toggle con default=false (FontStyleBlocker) expuso que la
 * lectura cross-proceso nunca funcionó. Fix: quien SÍ puede hacer chmod
 * sobre este archivo es nuestra propia app (dueña del UID) — se llama a
 * fixPermissions() después de cada escritura real (commit(), no apply(),
 * para garantizar que el archivo ya esté en disco antes del chmod).
 */
public final class AppPrefs {
    private AppPrefs() {
    }

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    /** Llamar despues de cada escritura (via commit(), no apply()) para que TikTok pueda leerla. */
    public static void fixPermissions(Context context) {
        try {
            File dataDir = new File(context.getApplicationInfo().dataDir);
            File sharedPrefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(sharedPrefsDir, context.getPackageName() + "_preferences.xml");

            boolean dataDirOk = dataDir.setExecutable(true, false);
            boolean sharedDirExecOk = sharedPrefsDir.setExecutable(true, false);
            boolean sharedDirReadOk = sharedPrefsDir.setReadable(true, false);
            boolean fileReadOk = prefsFile.setReadable(true, false);

            android.util.Log.i("TikRatu-diag", "fixPermissions: dataDir=" + dataDir.getAbsolutePath()
                    + " exists=" + dataDir.exists()
                    + " | sharedPrefsDir exists=" + sharedPrefsDir.exists()
                    + " | prefsFile=" + prefsFile.getAbsolutePath() + " exists=" + prefsFile.exists()
                    + " canRead=" + prefsFile.canRead()
                    + " | setExecutable(dataDir)=" + dataDirOk
                    + " setExecutable(sharedDir)=" + sharedDirExecOk
                    + " setReadable(sharedDir)=" + sharedDirReadOk
                    + " setReadable(file)=" + fileReadOk);
        } catch (Throwable t) {
            android.util.Log.i("TikRatu-diag", "fixPermissions fallo: " + t);
        }
    }
}
