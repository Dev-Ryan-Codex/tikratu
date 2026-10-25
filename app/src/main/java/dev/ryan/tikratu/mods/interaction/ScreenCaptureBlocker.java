package dev.ryan.tikratu.mods.interaction;

import android.app.Activity;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes11.dex */
public class ScreenCaptureBlocker {
    public void block(ClassLoader classLoader) {
        int hooked = 0;
        for (Method method : Activity.class.getDeclaredMethods()) {
            String name = method.getName();
            if (name.equals("registerScreenCaptureCallback") || name.equals("unregisterScreenCaptureCallback")) {
                try {
                    XposedBridge.hookMethod(method, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.interaction.ScreenCaptureBlocker.1
                        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                            if (RuntimeSettings.enabled(Prefs.KEY_SCREEN_CAPTURE_BLOCKER, false)) {
                                param.setResult((Object) null);
                            }
                        }
                    });
                    hooked++;
                } catch (Throwable t) {
                    ModuleLog.line("(TikRatu | ScreenCaptureBlocker): fallo hook " + name + " (" + t.getMessage() + ")");
                }
            }
        }
        if (hooked > 0) {
            ModuleLog.line("(TikRatu | ScreenCaptureBlocker): neutralizado el callback de captura (" + hooked + " metodos)");
        } else {
            ModuleLog.line("(TikRatu | ScreenCaptureBlocker): Activity no tiene la API de callback de captura en este Android");
        }
    }
}
