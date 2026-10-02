package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Fuerza la descarga habilitada en TikTok oficial.
 *
 * Verificado en el dex de TikTok 47.1.4 (jadx) — la descarga está gateada por
 * DOS cosas, no una:
 *   1. com.ss.android.ugc.aweme.feed.model.Aweme
 *        @C02uS("prevent_download") boolean preventDownload; -> isPreventDownload()
 *      Flag per-post simple (el que ya forzábamos a false). Pero NO alcanza:
 *      muchos videos siguen sin poder descargarse por el punto 2.
 *   2. Aweme.getVideoControl() -> com.ss.android.ugc.aweme.feed.model.VideoControl
 *      (data class de campos PÚBLICOS, sin getters — verificado, 38 líneas):
 *        Boolean allowDownload;      // null/false => la app bloquea descarga
 *        int     preventDownloadType; // != 0 => tipo de restricción activa
 *      Es el gate fuerte (viene de la config de privacidad/descarga del autor).
 *
 * Fix: isPreventDownload()->false, y en cada getVideoControl() no-nulo se fija
 * allowDownload=TRUE y preventDownloadType=0. Los campos se escriben por
 * reflexión (XposedHelpers) porque son públicos del propio classloader de TikTok.
 *
 * Nota (honesta): si un post no trae download address del servidor (privado /
 * geobloqueado) esto no lo arregla — no hay URL que descargar. Cubre los casos
 * en que la descarga está deshabilitada por flag/config, no por falta de medio.
 */
public class DownloadUnlockBlocker {

    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String USER_CLASS = "com.ss.android.ugc.aweme.profile.model.User";
    private static boolean loggedVc;

    public void block(ClassLoader classLoader) {
        // 0) Gate a nivel AUTOR (clave para historias y para creadores que
        // desactivaron la descarga en su cuenta): verificado en User.java de
        // TikTok 47.1.4 -> @C02uS("prevent_download") boolean preventDownload;
        // isPreventDownload(). Un story es un Aweme cuyo autor puede tener este
        // flag; forzarlo a false destraba la descarga de su contenido.
        // (No se toca User.downloadSetting (int): su valor "permitido" no está
        // verificado en el dex disponible y no se inventa.)
        try {
            XposedHelpers.findAndHookMethod(USER_CLASS, classLoader, "isPreventDownload",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (enabled()) param.setResult(false);
                        }
                    });
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked " + USER_CLASS + ".isPreventDownload()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo hook User.isPreventDownload (" + t.getMessage() + ")");
        }

        // 1) Flag per-post prevent_download.
        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "isPreventDownload",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (enabled()) param.setResult(false);
                        }
                    });
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked " + AWEME_CLASS + ".isPreventDownload()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo hook isPreventDownload (" + t.getMessage() + ")");
        }

        // 2) Gate fuerte: VideoControl.allowDownload / preventDownloadType.
        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "getVideoControl",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!enabled()) return;
                            Object vc = param.getResult();
                            if (vc == null) return;
                            try {
                                XposedHelpers.setObjectField(vc, "allowDownload", Boolean.TRUE);
                                XposedHelpers.setIntField(vc, "preventDownloadType", 0);
                                if (!loggedVc) {
                                    loggedVc = true;
                                    ModuleLog.line("(TikRatu | DownloadUnlockBlocker): VideoControl.allowDownload=true, preventDownloadType=0");
                                }
                            } catch (Throwable t) {
                                ModuleLog.line("(TikRatu | DownloadUnlockBlocker): no se pudo parchear VideoControl (" + t.getMessage() + ")");
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked " + AWEME_CLASS + ".getVideoControl()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo hook getVideoControl (" + t.getMessage() + ")");
        }
    }

    private static boolean enabled() {
        return RuntimeSettings.enabled(Prefs.KEY_DOWNLOAD_UNLOCK_BLOCKER, Prefs.DEFAULT_DOWNLOAD_UNLOCK_BLOCKER);
    }
}
