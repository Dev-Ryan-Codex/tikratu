package dev.ryan.tikratu.mods.ads;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes12.dex */
public class AdSignalsBlocker {
    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String AWEME_EXT_CLASS = "com.ss.android.ugc.aweme.feed.model.AwemeExtKt";

    public void block(ClassLoader classLoader) {
        XC_MethodHook returnFalse = new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.ads.AdSignalsBlocker.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                if (RuntimeSettings.enabled(Prefs.KEY_AD_SIGNALS_BLOCKER, true)) {
                    param.setResult(false);
                }
            }
        };
        hookQuiet(classLoader, AWEME_CLASS, "isSoftAd", returnFalse);
        try {
            Class<?> aweme = XposedHelpers.findClass(AWEME_CLASS, classLoader);
            hookQuietStatic(classLoader, AWEME_EXT_CLASS, "isPseudoAd", aweme, returnFalse);
            hookQuietStatic(classLoader, AWEME_EXT_CLASS, "isSearchPreciseAd", aweme, returnFalse);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdSignalsBlocker): no se encontro la clase Aweme (" + t.getMessage() + ")");
        }
    }

    private void hookQuiet(ClassLoader classLoader, String className, String methodName, XC_MethodHook hook) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, new Object[]{hook});
            ModuleLog.line("(TikRatu | AdSignalsBlocker): hooked " + className + "." + methodName + "()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdSignalsBlocker): fallo " + className + "." + methodName + " (" + t.getMessage() + ")");
        }
    }

    private void hookQuietStatic(ClassLoader classLoader, String className, String methodName, Class<?> paramType, XC_MethodHook hook) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, new Object[]{paramType, hook});
            ModuleLog.line("(TikRatu | AdSignalsBlocker): hooked " + className + "." + methodName + "(Aweme)");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdSignalsBlocker): fallo " + className + "." + methodName + " (" + t.getMessage() + ")");
        }
    }
}
