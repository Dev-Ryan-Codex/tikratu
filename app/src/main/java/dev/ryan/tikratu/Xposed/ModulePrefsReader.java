package dev.ryan.tikratu.Xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Reemplaza XSharedPreferences (descartado) Y la primera version de este
 * archivo (leer el archivo de propiedades directo — TAMBIEN descartado).
 * Ver PrefsProvider.java para el detalle completo: leer directamente un
 * archivo de otra app falla con ENOENT desde el proceso de TikTok, por
 * aislamiento de namespace de montaje de Android moderno — ni XML clasico
 * (XSharedPreferences) ni un archivo propio (intento anterior) pueden
 * cruzar ese limite. Un ContentProvider (Binder IPC real) si puede.
 */
final class ModulePrefsReader {

    private static final String AUTHORITY = "dev.ryan.tikratu.prefs";
    private final Map<String, String> values = new HashMap<>();

    // Recibe el Context explicito: AndroidAppHelper.currentApplication() lee
    // ActivityThread.mInitialApplication, que Android setea DESPUES de que
    // Application.attach() retorna — confirmado en dispositivo real que es
    // null incluso dentro del hook de attach(). La propia instancia de
    // Application (param.thisObject en ese hook) ya tiene su base Context.
    ModulePrefsReader(Context context) {
        try {
            if (context == null) {
                ModuleLog.line("(TikRatu): Context null — se usan los valores por defecto de cada feature");
                return;
            }
            Uri uri = Uri.parse("content://" + AUTHORITY + "/all");
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor == null) {
                    ModuleLog.line("(TikRatu): PrefsProvider devolvio cursor null — se usan los valores por defecto de cada feature");
                    return;
                }
                int keyIdx = cursor.getColumnIndexOrThrow("key");
                int valueIdx = cursor.getColumnIndexOrThrow("value");
                while (cursor.moveToNext()) {
                    values.put(cursor.getString(keyIdx), cursor.getString(valueIdx));
                }
                ModuleLog.line("(TikRatu): prefs leidas via PrefsProvider (" + values.size() + " keys)");
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): fallo consultando PrefsProvider (" + t + ") — se usan los valores por defecto de cada feature");
        }
    }

    boolean getBoolean(String key, boolean defaultValue) {
        String raw = values.get(key);
        return raw != null ? Boolean.parseBoolean(raw) : defaultValue;
    }
}
