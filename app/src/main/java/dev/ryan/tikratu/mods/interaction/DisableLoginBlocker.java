package dev.ryan.tikratu.mods.interaction;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * "Quitar login obligatorio" — evita la pantalla de login forzado, deja el
 * feed usable sin iniciar sesión.
 *
 * Importado del módulo Xposed gnadgnaoh/SexAlloy (vía
 * mentalblank/Tiktok-Revanced). TikTok decide mostrar el login forzado con
 * com.ss.android.ugc.aweme.services.MandatoryLoginService, métodos
 * enableForcedLogin() / shouldShowForcedLogin() (ambos booleanos). Se fuerzan
 * a false. Clase y métodos confirmados sin ofuscar en 47.0.3 (classes2.dex).
 */
public class DisableLoginBlocker {

    private static final String SERVICE_CLASS = "com.ss.android.ugc.aweme.services.MandatoryLoginService";
    private static final String[] GATES = {"enableForcedLogin", "shouldShowForcedLogin"};

    public void block(ClassLoader classLoader) {
        Class<?> service;
        try {
            service = XposedHelpers.findClass(SERVICE_CLASS, classLoader);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | DisableLoginBlocker): no se encontro " + SERVICE_CLASS + " (" + t.getMessage() + ")");
            return;
        }

        int hooked = 0;
        for (Method m : service.getDeclaredMethods()) {
            boolean isGate = false;
            for (String g : GATES) if (g.equals(m.getName())) isGate = true;
            if (!isGate) continue;
            if (m.getReturnType() != boolean.class || m.getParameterCount() != 1) continue;
            try {
                XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(false));
                hooked++;
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu | DisableLoginBlocker): fallo hook " + m.getName() + " (" + t.getMessage() + ")");
            }
        }
        if (hooked > 0) {
            ModuleLog.line("(TikRatu | DisableLoginBlocker): " + hooked + " gate(s) de login forzado desactivados");
        } else {
            ModuleLog.line("(TikRatu | DisableLoginBlocker): no se encontraron gates de login forzado con la firma esperada");
        }
    }
}
