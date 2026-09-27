package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado contra TikTok oficial 46.4.3 y 47.0.3 (com.zhiliaoapp.musically),
 * decompilando con jadx la clase com.ss.android.ugc.aweme.feed.model.Video
 * (classes25.dex en 46.4.3, classes3.dex en 47.0.3 — ni la clase ni sus
 * getters están ofuscados, mismo motivo que Aweme.isAd(): son campos Gson).
 *
 * El servidor de TikTok ya manda DOS URLs de descarga por video en la misma
 * respuesta:
 *   - download_addr             -> getDownloadAddr()             (con marca de agua)
 *   - download_no_watermark_addr -> getDownloadNoWatermarkAddr()  (sin marca de agua)
 *
 * Este hook no fabrica ninguna URL nueva: solo hace que getDownloadAddr()
 * devuelva el valor de getDownloadNoWatermarkAddr() cuando éste no es null.
 * Si el server no mandó variante sin marca de agua para ese video en particular,
 * se devuelve el resultado original sin tocar nada (no rompe la descarga).
 *
 * Cubre el flujo de descarga/guardado de VIDEO. No cubre fotos ni GIFs
 * (el mod original tenía toggles separados para eso) — pendiente si se
 * confirma un campo equivalente en el modelo de imagen.
 */
public class WatermarkBlocker {

    private static final String VIDEO_CLASS = "com.ss.android.ugc.aweme.feed.model.Video";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(VIDEO_CLASS, classLoader, "getDownloadAddr",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object noWatermarkAddr = XposedHelpers.callMethod(
                                    param.thisObject, "getDownloadNoWatermarkAddr");
                            if (noWatermarkAddr != null) {
                                param.setResult(noWatermarkAddr);
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | WatermarkBlocker): hooked " + VIDEO_CLASS + ".getDownloadAddr()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
