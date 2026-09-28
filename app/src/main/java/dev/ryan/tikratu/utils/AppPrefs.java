package dev.ryan.tikratu.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * BUG REAL ENCONTRADO EN DISPOSITIVO (2026-09-28): en este dispositivo/OS
 * (Android 16 preview, targetSdk 36), Context.getSharedPreferences() NUNCA
 * llega a crear el archivo shared_prefs/<pkg>_preferences.xml en disco —
 * confirmado con un diagnostico que lista el contenido real de dataDir
 * justo despues de un commit() (sincronico): solo aparecen cache/,
 * code_cache/ y files/, nunca shared_prefs/. Esto rompe XSharedPreferences
 * de raiz (lee un archivo XML que jamas se crea), independientemente de
 * cualquier arreglo de permisos — no es un problema de permisos, es que
 * el backend de SharedPreferences en este SO ya no persiste como XML
 * plano de la forma clasica que XSharedPreferences espera.
 *
 * Fix real: no usar SharedPreferences en absoluto para nada que TikTok
 * necesite leer cross-proceso. Se implementa un archivo de propiedades
 * propio (java.util.Properties, formato texto plano de toda la vida)
 * bajo getFilesDir() — territorio que SI existe y es escribible/legible
 * de forma predecible — con permisos world-readable puestos a mano por
 * esta misma app (dueña real del archivo, unico proceso que puede
 * hacerle chmod con exito).
 */
public final class AppPrefs {
    private static final String FILE_NAME = "tikratu_prefs.properties";

    private AppPrefs() {
    }

    public static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        String raw = load(context).getProperty(key);
        return raw != null ? Boolean.parseBoolean(raw) : defaultValue;
    }

    public static int getInt(Context context, String key, int defaultValue) {
        String raw = load(context).getProperty(key);
        if (raw == null) return defaultValue;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(Context context, String key, long defaultValue) {
        String raw = load(context).getProperty(key);
        if (raw == null) return defaultValue;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getString(Context context, String key, String defaultValue) {
        String raw = load(context).getProperty(key);
        return raw != null ? raw : defaultValue;
    }

    public static void putLong(Context context, String key, long value) {
        Properties props = load(context);
        props.setProperty(key, String.valueOf(value));
        save(context, props);
    }

    public static void putString(Context context, String key, String value) {
        Properties props = load(context);
        props.setProperty(key, value);
        save(context, props);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        Properties props = load(context);
        props.setProperty(key, String.valueOf(value));
        save(context, props);
    }

    public static void putInt(Context context, String key, int value) {
        Properties props = load(context);
        props.setProperty(key, String.valueOf(value));
        save(context, props);
    }

    private static Properties load(Context context) {
        Properties props = new Properties();
        File f = file(context);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException ignored) {
                // Archivo corrupto/ilegible - se sigue con props vacio (defaults).
            }
        }
        return props;
    }

    private static void save(Context context, Properties props) {
        File f = file(context);
        try (FileOutputStream out = new FileOutputStream(f)) {
            props.store(out, null);
            out.flush();
            out.getFD().sync();
        } catch (IOException ignored) {
            return;
        }
        // Permisos world-readable puestos por esta misma app (dueña del
        // archivo) - un proceso ajeno (TikTok) no puede hacer chmod sobre
        // un archivo que no le pertenece, por eso esto no puede hacerse
        // del lado de Module.java.
        context.getFilesDir().setExecutable(true, false);
        f.setReadable(true, false);
    }
}
