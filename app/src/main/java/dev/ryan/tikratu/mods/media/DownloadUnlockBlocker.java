package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado en TikTok oficial 46.4.3 y 47.0.3 (com.zhiliaoapp.musically):
 * com.ss.android.ugc.aweme.feed.model.Aweme tiene un campo/getter público
 * `preventDownload` / `isPreventDownload()` — un flag que el servidor puede
 * mandar por-post para que la app bloquee la descarga de ese contenido
 * puntual (independiente de la marca de agua). A diferencia de isImage()/
 * isPhotoMode()/getIsTikTokStory(), este SÍ es un flag de un solo propósito
 * (gatekeeper de descarga, no un discriminador estructural de tipo usado
 * para decidir cómo renderizar/reproducir contenido), así que forzarlo a
 * false es seguro en el mismo sentido que Aweme.isAd().
 */
public class DownloadUnlockBlocker {

    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "isPreventDownload",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(false);
                        }
                    });
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked " + AWEME_CLASS + ".isPreventDownload()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
