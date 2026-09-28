package dev.ryan.tikratu.Xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Estado de las preferencias LEÍDO EN VIVO dentro del proceso de TikTok.
 *
 * Antes cada hook se instalaba (o no) una sola vez al arrancar, según el
 * valor del toggle en ese momento — así, prender/apagar una opción no tenía
 * efecto hasta reiniciar TikTok. Ahora TODOS los hooks se instalan siempre y
 * consultan este cache en cada disparo; desactivar un toggle hace que el hook,
 * aunque instalado, no haga nada a partir del próximo disparo.
 *
 * El cache se llena una vez de forma síncrona al arrancar (para que el primer
 * frame ya tenga el estado correcto) y luego se refresca en segundo plano cada
 * pocos segundos leyendo el mismo PrefsProvider (Binder IPC) que usa
 * ModulePrefsReader. Los hooks solo leen del HashMap en memoria (instantáneo),
 * nunca hacen IPC en su hilo.
 */
public final class RuntimeSettings {

    private static final String AUTHORITY = "dev.ryan.tikratu.prefs";
    private static final long REFRESH_SECONDS = 2;

    private static volatile Map<String, String> cache = Collections.emptyMap();
    private static Context appContext;
    private static ScheduledExecutorService scheduler;

    private RuntimeSettings() {
    }

    public static synchronized void init(Context context) {
        if (appContext != null) return;
        appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        load();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TikRatu-settings");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(RuntimeSettings::load, REFRESH_SECONDS, REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    private static void load() {
        try {
            Uri uri = Uri.parse("content://" + AUTHORITY + "/all");
            try (Cursor cursor = appContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor == null) return;
                Map<String, String> fresh = new HashMap<>();
                int keyIdx = cursor.getColumnIndexOrThrow("key");
                int valueIdx = cursor.getColumnIndexOrThrow("value");
                while (cursor.moveToNext()) {
                    fresh.put(cursor.getString(keyIdx), cursor.getString(valueIdx));
                }
                cache = fresh;
            }
        } catch (Throwable t) {
            // Se conserva el cache anterior; no se toca.
            ModuleLog.line("(TikRatu | RuntimeSettings): fallo el refresh (" + t + ")");
        }
    }

    public static boolean enabled(String key, boolean defaultValue) {
        String v = cache.get(key);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String v = cache.get(key);
        if (v == null) return defaultValue;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getString(String key, String defaultValue) {
        String v = cache.get(key);
        return v != null ? v : defaultValue;
    }
}
