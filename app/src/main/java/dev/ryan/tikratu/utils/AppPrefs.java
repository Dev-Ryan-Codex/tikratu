package dev.ryan.tikratu.utils;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/* JADX INFO: loaded from: classes10.dex */
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
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(Context context, String key, long defaultValue) {
        String raw = load(context).getProperty(key);
        if (raw == null) {
            return defaultValue;
        }
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
            try {
                FileInputStream in = new FileInputStream(f);
                try {
                    props.load(in);
                    in.close();
                } catch (Throwable th) {
                    try {
                        in.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException e) {
            }
        }
        return props;
    }

    private static void save(Context context, Properties props) {
        File f = file(context);
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                props.store(out, (String) null);
                out.flush();
                out.getFD().sync();
                out.close();
                context.getFilesDir().setExecutable(true, false);
                f.setReadable(true, false);
            } catch (Throwable th) {
                try {
                    out.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (IOException e) {
        }
    }
}
