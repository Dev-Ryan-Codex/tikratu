package dev.ryan.tikratu.mods.ads;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado contra TikTok oficial (com.zhiliaoapp.musically): la clase
 * com.ss.android.ugc.aweme.feed.model.Aweme y su método isAd() NO están
 * ofuscados (son campos Gson), en 46.4.3 y 47.0.3. Ese getter es el punto
 * donde el feed decide si un post es publicidad.
 *
 * El hook se instala SIEMPRE; consulta RuntimeSettings en cada disparo, así
 * activar/desactivar "Quitar anuncios" aplica sin reiniciar TikTok. El hook
 * directo basta en las versiones objetivo — el fallback DexKit anterior se
 * quitó del arranque (aceleraba el inicio y ya no se necesita).
 */
public class AdBlocker {

    private static final String TARGET_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(TARGET_CLASS, classLoader, "isAd", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_AD_BLOCKER, Prefs.DEFAULT_AD_BLOCKER)) {
                        param.setResult(false);
                    }
                }
            });
            ModuleLog.line("(TikRatu | AdBlocker): hooked " + TARGET_CLASS + ".isAd()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
