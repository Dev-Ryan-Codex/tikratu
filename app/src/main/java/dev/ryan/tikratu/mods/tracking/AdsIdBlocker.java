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
        // Hooks separados: getId() e isLimitAdTrackingEnabled() se hookean por
        // separado porque en TikTok 47.0.3 la clase EXISTE y getId() también,
        // pero R8 renombró/eliminó isLimitAdTrackingEnabled() — juntos en un
        // solo try, el fallo del segundo tiraba abajo el hook del primero
        // (confirmado en dispositivo: el log solo mostraba el segundo método).
        boolean any = false;
        try {
            XposedHelpers.findAndHookMethod(INFO_CLASS, classLoader, "getId", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(ZERO_UUID);
                }
            });
            ModuleLog.line("(TikRatu | AdsIdBlocker): hooked AdvertisingIdClient.Info.getId() -> UUID cero");
            any = true;
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdsIdBlocker): getId() no hookeable (" + t.getMessage() + ")");
        }

        try {
            XposedHelpers.findAndHookMethod(INFO_CLASS, classLoader, "isLimitAdTrackingEnabled", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });
            ModuleLog.line("(TikRatu | AdsIdBlocker): hooked isLimitAdTrackingEnabled() -> true");
            any = true;
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdsIdBlocker): isLimitAdTrackingEnabled() no existe en este build (normal)");
        }

        if (!any) {
            ModuleLog.line("(TikRatu | AdsIdBlocker): ningun metodo de AdvertisingIdClient.Info se pudo hookear");
        }
    }
}
