package dev.ryan.tikratu.Xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes9.dex */
public final class RuntimeSettings {
    private static final String AUTHORITY = "dev.ryan.tikratu.prefs";
    private static final long REFRESH_SECONDS = 2;
    private static Context appContext;
    private static volatile Map<String, String> cache = Collections.emptyMap();
    private static ScheduledExecutorService scheduler;

    private RuntimeSettings() {
    }

    public static synchronized void init(Context context) {
        if (appContext != null) {
            return;
        }
        appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        load();
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() { // from class: dev.ryan.tikratu.Xposed.RuntimeSettings$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.ThreadFactory
            public final Thread newThread(Runnable runnable) {
                return RuntimeSettings.lambda$init$0(runnable);
            }
        });
        scheduler.scheduleWithFixedDelay(new Runnable() { // from class: dev.ryan.tikratu.Xposed.RuntimeSettings$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RuntimeSettings.load();
            }
        }, REFRESH_SECONDS, REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    static /* synthetic */ Thread lambda$init$0(Runnable r) {
        Thread t = new Thread(r, "TikRatu-settings");
        t.setDaemon(true);
        return t;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void load() {
        try {
            Uri uri = Uri.parse("content://dev.ryan.tikratu.prefs/all");
            Cursor cursor = appContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    Map<String, String> fresh = new HashMap<>();
                    int keyIdx = cursor.getColumnIndexOrThrow("key");
                    int valueIdx = cursor.getColumnIndexOrThrow("value");
                    while (cursor.moveToNext()) {
                        fresh.put(cursor.getString(keyIdx), cursor.getString(valueIdx));
                    }
                    cache = fresh;
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
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
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | RuntimeSettings): fallo el refresh (" + t + ")");
        }
    }

    public static boolean enabled(String key, boolean defaultValue) {
        String v = cache.get(key);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }

    public static long getLong(String key, long defaultValue) {
        String v = cache.get(key);
        if (v == null) {
            return defaultValue;
        }
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
