package dev.ryan.tikratu.mods.interaction;

import android.app.Activity;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * "Deshabilitar detección de captura de pantalla" — evita que TikTok
 * reaccione a screenshots / grabaciones de pantalla.
 *
 * Importado del módulo Xposed gnadgnaoh/SexAlloy (vía
 * mentalblank/Tiktok-Revanced). El punto principal es la API pública de
 * Android android.app.Activity.registerScreenCaptureCallback(...) /
 * unregisterScreenCaptureCallback(...) (API 34+, presente en el dispositivo
 * de destino) — al neutralizarla, TikTok nunca recibe el aviso de captura.
 * Es API de plataforma, no código de TikTok: estable sin importar la
 * ofuscación (mismo criterio que UrlSanitizerBlocker/AdsMetadataBlocker).
 *
 * (SexAlloy además neutraliza un ClearModePanelComponent interno vía
 * búsqueda de strings con DexKit; esa parte secundaria no se porta acá para
 * mantener el hook simple y de bajo riesgo — la API de plataforma es el
 * camino principal.)
 */
public class ScreenCaptureBlocker {

    public void block(ClassLoader classLoader) {
        int hooked = 0;
        for (Method method : Activity.class.getDeclaredMethods()) {
            String name = method.getName();
            if (name.equals("registerScreenCaptureCallback") || name.equals("unregisterScreenCaptureCallback")) {
                try {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(null));
                    hooked++;
                } catch (Throwable t) {
                    ModuleLog.line("(TikRatu | ScreenCaptureBlocker): fallo hook " + name + " (" + t.getMessage() + ")");
                }
            }
        }
        if (hooked > 0) {
            ModuleLog.line("(TikRatu | ScreenCaptureBlocker): neutralizado el callback de captura (" + hooked + " metodos)");
        } else {
            ModuleLog.line("(TikRatu | ScreenCaptureBlocker): Activity no tiene la API de callback de captura en este Android");
        }
    }
}
