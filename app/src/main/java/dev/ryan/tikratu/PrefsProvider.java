package dev.ryan.tikratu;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import dev.ryan.tikratu.utils.AppPrefs;

/**
 * BUG REAL ENCONTRADO EN DISPOSITIVO (2026-09-28): tanto
 * SharedPreferences/XSharedPreferences como leer directamente el archivo
 * de propiedades propio (AppPrefs) desde OTRO proceso fallan con ENOENT —
 * confirmado en logcat: el proceso de TikTok no puede ver
 * /data/user/0/dev.ryan.tikratu/files/... en absoluto, aunque el archivo
 * SI existe con permisos world-readable (confirmado via run-as). Esto es
 * aislamiento de namespace de montaje por-app de Android moderno
 * (sandboxing de almacenamiento), no un problema de permisos DAC — ningun
 * chmod lo arregla porque el archivo de otra app literalmente no esta
 * montado/visible en el namespace de un proceso ajeno.
 *
 * Fix real: un ContentProvider usa Binder IPC (no acceso directo a
 * archivos), que SI esta diseñado para cruzar este aislamiento de forma
 * segura — es el mecanismo soportado por Android para este caso exacto.
 * Devuelve todas las keys/values del archivo de AppPrefs en una sola
 * consulta (columnas: key, value), Module.java arma un Map en runtime.
 */
public class PrefsProvider extends ContentProvider {

    public static final String AUTHORITY = "dev.ryan.tikratu.prefs";
    public static final Uri URI_ALL = Uri.parse("content://" + AUTHORITY + "/all");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(AppPrefs.file(getContext()))) {
            props.load(in);
        } catch (IOException ignored) {
            // Archivo todavia no existe (fresh install, ningun toggle tocado) - cursor vacio, todo usa defaults.
        }
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            cursor.addRow(new Object[]{entry.getKey().toString(), entry.getValue().toString()});
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.dev.ryan.tikratu.prefs";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Solo lectura");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Solo lectura");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Solo lectura");
    }
}
