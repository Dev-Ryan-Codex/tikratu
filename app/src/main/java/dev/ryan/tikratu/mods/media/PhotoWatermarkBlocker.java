package dev.ryan.tikratu.mods.media;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado contra TikTok oficial 46.4.3 y 47.0.3 (com.zhiliaoapp.musically).
 *
 * com.ss.android.ugc.aweme.feed.model.PhotoModeImageUrlModel (posts de foto/
 * slideshow) tiene tres campos análogos a los de Video, pero SIN getters
 * (Gson los inyecta por reflexión directa sobre el campo público):
 *
 *   @02s3("display_image")          UrlModel displayImageNoWatermark  (sin marca)
 *   @02s3("owner_watermark_image")  UrlModel ownerWatermarkImage      (con marca del autor)
 *   @02s3("user_watermark_image")   UrlModel userWatermarkImage       (con marca del que descarga)
 *
 * Se ubicó el código real que guarda/comparte la foto (clase auto-generada
 * en el paquete "X", tipo "0oOF" — nombre que cambia en cada build de R8, NO
 * sirve como target de hook estable). Esa lógica arma la lista de imágenes
 * llamando a PhotoModeImageInfo.getImageList() y ahí, según un flag interno,
 * lee displayImageNoWatermark (sin marca) o userWatermarkImage/ownerWatermarkImage
 * (con marca).
 *
 * En vez de perseguir esa clase inestable, se hookea
 * com.ss.android.ugc.aweme.feed.model.PhotoModeImageInfo.getImageList() —
 * getter público, sin ofuscar (mismo motivo que Aweme/Video: Gson), confirmado
 * idéntico en 46.4.3 y 47.0.3. Cada vez que se pide la lista de fotos del post
 * (sea cual sea el código que la llamó — guardar, compartir, lo que sea, y sin
 * importar si el objeto se pobló por JSON o por protobuf) se recorre e iguala
 * ownerWatermarkImage/userWatermarkImage a displayImageNoWatermark en cada
 * item. Esto reemplaza el hook anterior sobre el conversor protobuf
 * (ConvertHelper), que solo cubría un camino de parseo — este cubre los dos.
 */
public class PhotoWatermarkBlocker {

    private static final String IMAGE_INFO_CLASS = "com.ss.android.ugc.aweme.feed.model.PhotoModeImageInfo";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(IMAGE_INFO_CLASS, classLoader, "getImageList",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (!(result instanceof List)) return;
                            for (Object item : (List<?>) result) {
                                if (item == null) continue;
                                Object clean = XposedHelpers.getObjectField(item, "displayImageNoWatermark");
                                if (clean == null) continue;
                                XposedHelpers.setObjectField(item, "ownerWatermarkImage", clean);
                                XposedHelpers.setObjectField(item, "userWatermarkImage", clean);
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | PhotoWatermarkBlocker): hooked " + IMAGE_INFO_CLASS + ".getImageList()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | PhotoWatermarkBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
