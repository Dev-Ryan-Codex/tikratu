package dev.ryan.tikratu.mods.interaction;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.lang.reflect.Method;

/**
 * Evita que TikTok muestre el dialogo de CAPTCHA.
 *
 * Verificado en 47.4.3 (classes3.dex): com.ss.android.ugc.aweme.sec.SecApiImpl
 * implementa ISecApi y sus metodos popCaptcha(Activity, int, cb) y
 * popCaptchaV2(...) (2 sobrecargas) son los unicos que abren el dialogo; ambos
 * tienen nombre estable porque vienen de la interfaz. El callback es un tipo
 * ofuscado, asi que no se invoca: simplemente no se abre el dialogo.
 *
 * Riesgo: el servidor sigue exigiendo la verificacion, asi que la accion que la
 * pidio (login, publicar, comentar) puede fallar o quedarse esperando. Por eso
 * el toggle viene apagado.
 */
public class CaptchaBlocker {
    private static final String SEC_API = "com.ss.android.ugc.aweme.sec.SecApiImpl";

    private static boolean loggedSkip;

    public void block(ClassLoader classLoader) {
        Class<?> sec = XposedHelpers.findClassIfExists(SEC_API, classLoader);
        if (sec == null) {
            ModuleLog.line("(TikRatu | CaptchaBlocker): SecApiImpl no existe en esta version de TikTok");
            return;
        }
        XC_MethodHook hook = new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                if (!RuntimeSettings.enabled(Prefs.KEY_HIDE_CAPTCHA, false)) return;
                param.setResult(null);
                if (!loggedSkip) {
                    loggedSkip = true;
                    ModuleLog.line("(TikRatu | CaptchaBlocker): dialogo de CAPTCHA omitido (" + param.method.getName() + ")");
                }
            }
        };
        int count = 0;
        for (Method m : sec.getDeclaredMethods()) {
            String name = m.getName();
            if (name.equals("popCaptcha") || name.equals("popCaptchaV2")) {
                try {
                    XposedBridge.hookMethod(m, hook);
                    count++;
                } catch (Throwable t) {
                    ModuleLog.line("(TikRatu | CaptchaBlocker): fallo hook " + name + " (" + t.getMessage() + ")");
                }
            }
        }
        ModuleLog.line("(TikRatu | CaptchaBlocker): hooked " + count + " metodos de SecApiImpl");
    }
}
