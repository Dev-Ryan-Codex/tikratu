package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado contra TikTok oficial 46.4.3 y 47.0.3, decompilando con jadx
 * com.ss.android.ugc.aweme.feed.model.PhotoModeImageUrlModel (posts de foto /
 * slideshow). A diferencia de Video, esta clase NO tiene getters para sus
 * campos de watermark (Gson los inyecta directo por reflexión sobre campos
 * públicos), así que no hay un método simple tipo getDownloadAddr() para
 * hookear. Sus campos relevantes:
 *
 *   @02s3("display_image")          UrlModel displayImageNoWatermark  (sin marca)
 *   @02s3("owner_watermark_image")  UrlModel ownerWatermarkImage      (con marca del autor)
 *   @02s3("user_watermark_image")   UrlModel userWatermarkImage       (con marca del usuario que descarga)
 *
 * Como no se puede hookear una LECTURA de campo público con Xposed, se
 * hookea en cambio el método que arma el objeto a partir de protobuf:
 *   com.ss.android.ugc.tiktok.ConvertHelper
 *     .com$ss$ugc$tiktok$proto$ImagePostInfoV2$$com$ss$android$ugc$aweme$feed$model$PhotoModeImageUrlModel(...)
 * (nombre confirmado idéntico en 46.4.3 y 47.0.3 — lo genera un codegen de
 * proto-a-modelo de ByteDance, el primer parámetro es un tipo proto con
 * nombre ofuscado que cambia entre builds, por eso se usa hookAllMethods
 * por NOMBRE en vez de matchear tipos de parámetro exactos).
 *
 * Después de que el método arma el objeto, se sobreescriben
 * ownerWatermarkImage/userWatermarkImage con el valor de
 * displayImageNoWatermark (si existe), directamente sobre los campos del
 * objeto ya construido — no hace falta tocar la construcción en sí.
 *
 * LIMITACIÓN CONOCIDA: esto cubre el camino protobuf. Si el post de foto se
 * parsea por el camino JSON/Gson normal (que también existe, dado que los
 * campos tienen anotaciones @02s3/@SerializedName), Gson asigna los campos
 * por reflexión directa sin pasar por ningún método hookeable — Xposed no
 * puede interceptar una lectura de campo público. Cubrir ese camino
 * requeriría encontrar y hookear el método real de "guardar/compartir foto"
 * que LEE ownerWatermarkImage/userWatermarkImage, que no se identificó
 * todavía (pendiente).
 */
public class PhotoWatermarkBlocker {

    private static final String CONVERTER_CLASS = "com.ss.android.ugc.tiktok.ConvertHelper";
    private static final String CONVERTER_METHOD =
            "com$ss$ugc$tiktok$proto$ImagePostInfoV2$$com$ss$android$ugc$aweme$feed$model$PhotoModeImageUrlModel";

    public void block(ClassLoader classLoader) {
        try {
            Class<?> converterClass = XposedHelpers.findClass(CONVERTER_CLASS, classLoader);
            XposedBridge.hookAllMethods(converterClass, CONVERTER_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    if (result == null) return;
                    Object clean = XposedHelpers.getObjectField(result, "displayImageNoWatermark");
                    if (clean == null) return;
                    XposedHelpers.setObjectField(result, "ownerWatermarkImage", clean);
                    XposedHelpers.setObjectField(result, "userWatermarkImage", clean);
                }
            });
            ModuleLog.line("(TikRatu | PhotoWatermarkBlocker): hooked " + CONVERTER_CLASS + "." + CONVERTER_METHOD);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | PhotoWatermarkBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
