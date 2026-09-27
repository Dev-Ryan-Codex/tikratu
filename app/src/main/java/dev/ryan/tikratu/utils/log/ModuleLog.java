package dev.ryan.tikratu.utils.log;

import de.robv.android.xposed.XposedBridge;

public final class ModuleLog {

    private ModuleLog() {
    }

    public static void line(String message) {
        XposedBridge.log(message);
    }
}
