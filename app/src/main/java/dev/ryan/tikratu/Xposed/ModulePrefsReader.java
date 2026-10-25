package dev.ryan.tikratu.Xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes9.dex */
final class ModulePrefsReader {
    private static final String AUTHORITY = "dev.ryan.tikratu.prefs";
    private final Map<String, String> values = new HashMap();

    ModulePrefsReader(Context context) {
        try {
            if (context == null) {
                ModuleLog.line("(TikRatu): Context null — se usan los valores por defecto de cada feature");
                return;
            }
            Uri uri = Uri.parse("content://dev.ryan.tikratu.prefs/all");
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor == null) {
                    ModuleLog.line("(TikRatu): PrefsProvider devolvio cursor null — se usan los valores por defecto de cada feature");
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                int keyIdx = cursor.getColumnIndexOrThrow("key");
                int valueIdx = cursor.getColumnIndexOrThrow("value");
                while (cursor.moveToNext()) {
                    this.values.put(cursor.getString(keyIdx), cursor.getString(valueIdx));
                }
                ModuleLog.line("(TikRatu): prefs leidas via PrefsProvider (" + this.values.size() + " keys)");
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): fallo consultando PrefsProvider (" + t + ") — se usan los valores por defecto de cada feature");
        }
    }

    boolean getBoolean(String key, boolean defaultValue) {
        String raw = this.values.get(key);
        return raw != null ? Boolean.parseBoolean(raw) : defaultValue;
    }

    long getLong(String key, long defaultValue) {
        String raw = this.values.get(key);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    String getString(String key, String defaultValue) {
        String raw = this.values.get(key);
        return raw != null ? raw : defaultValue;
    }
}
