package dev.ryan.tikratu.mods.tracking;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Neutraliza el Advertising ID (GAID) que TikTok pide a Google Play Services.
 * Apunta a la clase pública com.google.android.gms.ads.identifier.AdvertisingIdClient$Info
 * (parte de la libreria cliente "play-services-ads-identifier" que las apps
 * embeben en su propio APK) — no es un endpoint interno de TikTok, así que no
 * requiere RE adicional contra su dex ofuscado.
 */
public class AdsIdBlocker {

    private static final String INFO_CLASS =
            "com.google.android.gms.ads.identifier.AdvertisingIdClient$Info";
    private static final String ZERO_UUID = "00000000-0000-0000-0000-000000000000";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(INFO_CLASS, classLoader, "getId", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(ZERO_UUID);
                }
            });

            XposedHelpers.findAndHookMethod(INFO_CLASS, classLoader, "isLimitAdTrackingEnabled", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });

            ModuleLog.line("(TikRatu | AdsIdBlocker): hooked AdvertisingIdClient.Info (getId/isLimitAdTrackingEnabled)");
        } catch (Throwable t) {
            // Normal si TikTok no linkea esta libreria en esta version/build, o si
            // Play Services no esta disponible en el dispositivo.
            ModuleLog.line("(TikRatu | AdsIdBlocker): clase no encontrada, nada que hookear (" + t.getMessage() + ")");
        }
    }
}
