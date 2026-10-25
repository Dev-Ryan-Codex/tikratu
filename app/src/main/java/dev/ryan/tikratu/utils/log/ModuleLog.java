package dev.ryan.tikratu.utils.log;

import de.robv.android.xposed.XposedBridge;

/* JADX INFO: loaded from: classes3.dex */
public final class ModuleLog {
    private ModuleLog() {
    }

    public static void line(String message) {
        XposedBridge.log(message);
    }
}
