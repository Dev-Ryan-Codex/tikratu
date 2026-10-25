package dev.ryan.tikratu.mods.interaction;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes11.dex */
public class GoogleLoginFixBlocker {
    private static final String[] AUTH_CLASSES = {"com.bytedance.lobby.google.GoogleAuth", "com.bytedance.lobby.google.GoogleWebAuth"};

    public void block(ClassLoader classLoader) {
        for (String className : AUTH_CLASSES) {
            forceBooleanNoArgMethods(classLoader, className);
        }
    }

    private void forceBooleanNoArgMethods(ClassLoader classLoader, String className) {
        try {
            Class<?> cls = XposedHelpers.findClassIfExists(className, classLoader);
            if (cls == null) {
                ModuleLog.line("(TikRatu | GoogleLoginFixBlocker): no existe " + className + " en esta versión");
                return;
            }
            int hooked = 0;
            for (Method m : cls.getDeclaredMethods()) {
                int mod = m.getModifiers();
                if (m.getParameterCount() == 0 && m.getReturnType() == Boolean.TYPE && Modifier.isPublic(mod) && Modifier.isFinal(mod) && !Modifier.isStatic(mod)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.interaction.GoogleLoginFixBlocker.1
                        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                            if (RuntimeSettings.enabled(Prefs.KEY_GOOGLE_LOGIN_FIX, false)) {
                                param.setResult(false);
                            }
                        }
                    });
                    hooked++;
                }
            }
            ModuleLog.line("(TikRatu | GoogleLoginFixBlocker): " + className + " -> " + hooked + " métodos boolean forzados a false");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | GoogleLoginFixBlocker): fallo en " + className + " (" + t.getMessage() + ")");
        }
    }
}
