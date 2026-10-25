package dev.ryan.tikratu.mods.interaction;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes11.dex */
public class DisableLoginBlocker {
    private static final String[] GATES = {"enableForcedLogin", "shouldShowForcedLogin"};
    private static final String SERVICE_CLASS = "com.ss.android.ugc.aweme.services.MandatoryLoginService";

    public void block(ClassLoader classLoader) {
        try {
            Class<?> service = XposedHelpers.findClass(SERVICE_CLASS, classLoader);
            int hooked = 0;
            for (Method m : service.getDeclaredMethods()) {
                boolean isGate = false;
                for (String g : GATES) {
                    if (g.equals(m.getName())) {
                        isGate = true;
                    }
                }
                if (isGate && m.getReturnType() == Boolean.TYPE && m.getParameterCount() == 1) {
                    try {
                        XposedBridge.hookMethod(m, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.interaction.DisableLoginBlocker.1
                            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                                if (RuntimeSettings.enabled(Prefs.KEY_DISABLE_LOGIN, false)) {
                                    param.setResult(false);
                                }
                            }
                        });
                        hooked++;
                    } catch (Throwable t) {
                        ModuleLog.line("(TikRatu | DisableLoginBlocker): fallo hook " + m.getName() + " (" + t.getMessage() + ")");
                    }
                }
            }
            if (hooked > 0) {
                ModuleLog.line("(TikRatu | DisableLoginBlocker): " + hooked + " gate(s) de login forzado desactivados");
            } else {
                ModuleLog.line("(TikRatu | DisableLoginBlocker): no se encontraron gates de login forzado con la firma esperada");
            }
        } catch (Throwable t2) {
            ModuleLog.line("(TikRatu | DisableLoginBlocker): no se encontro com.ss.android.ugc.aweme.services.MandatoryLoginService (" + t2.getMessage() + ")");
        }
    }
}
