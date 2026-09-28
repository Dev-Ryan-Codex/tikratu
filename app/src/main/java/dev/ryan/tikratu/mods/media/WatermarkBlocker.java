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
 *
 * DIAGNÓSTICO CONFIRMADO EN DISPOSITIVO REAL (2026-09-27, TikTok 47.0.3):
 * el hook SÍ se dispara y SÍ reemplaza el resultado (se confirmó con logs
 * que download_no_watermark_addr no es null y apunta a una URL distinta,
 * del endpoint normal de reproducción interna
 * api16-normal-c-*.tiktokv.com/aweme/v1/play/ en vez del endpoint de
 * descarga con &watermark=1&logo_name=tiktok en la query). Pese a eso, el
 * archivo de video guardado en el dispositivo SIGUE teniendo la marca de
 * agua incrustada en los píxeles.
 *
 * Conclusión: no es un bug de este hook — es que, al menos para esta
 * cuenta/región/versión, el contenido que sirve ByteDance en
 * download_no_watermark_addr YA NO es realmente una copia limpia (puede
 * que lo haya sido en versiones viejas de TikTok, y que el campo se haya
 * dejado de usar para ese fin sin quitarlo del modelo). No hay URL
 * alternativa del propio servidor que dé un archivo sin marca — swap de
 * campo no alcanza. Se deja el hook activo (no hace daño, y podría servir
 * en cuentas/regiones donde el campo sí sea válido) pero no remover el
 * toggle sería falsa expectativa: está documentado como no confiable.
 */
public class WatermarkBlocker {

    private static final String VIDEO_CLASS = "com.ss.android.ugc.aweme.feed.model.Video";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(VIDEO_CLASS, classLoader, "getDownloadAddr",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object original = param.getResult();
                            Object noWatermarkAddr;
                            try {
                                noWatermarkAddr = XposedHelpers.callMethod(param.thisObject, "getDownloadNoWatermarkAddr");
                            } catch (Throwable t) {
                                ModuleLog.line("(TikRatu | WatermarkBlocker): getDownloadNoWatermarkAddr() fallo: " + t);
                                return;
                            }
                            ModuleLog.line("(TikRatu | WatermarkBlocker): getDownloadAddr() llamado. original="
                                    + describe(original) + " | noWatermark=" + describe(noWatermarkAddr));
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

    private static String describe(Object urlModel) {
        if (urlModel == null) return "null";
        try {
            Object uri = XposedHelpers.callMethod(urlModel, "getUri");
            Object urlList = XposedHelpers.callMethod(urlModel, "getUrlList");
            return "uri=" + uri + " urls=" + urlList;
        } catch (Throwable t) {
            return "presente (no se pudo describir: " + t.getMessage() + ")";
        }
    }
}
