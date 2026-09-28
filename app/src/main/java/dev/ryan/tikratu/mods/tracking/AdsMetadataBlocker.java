package dev.ryan.tikratu.mods.tracking;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * El TikTok oficial declara en su AndroidManifest.xml:
 *   <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" .../>
 * (verificado en el manifest decodificado de 46.4.3 y 47.0.3). El SDK de
 * Google Mobile Ads lo lee de ApplicationInfo.metaData al iniciar; sin esa
 * key, el SDK no puede inicializarse.
 *
 * PRIMER INTENTO (descartado): hookear Bundle.getString(String) — confirmado
 * en dispositivo real que NO es hookeable en Android moderno (ART trata ese
 * método de forma especial), el hook fallaba al instalarse.
 *
 * ENFOQUE ACTUAL: interceptar dónde se OBTIENE el Bundle metaData, no dónde
 * se lee — se hookea PackageManager.getApplicationInfo(...) (impl real
 * android.app.ApplicationPackageManager, hookeable) y en el ApplicationInfo
 * devuelto se quita la key de AdMob de su metaData. Solo toca el metaData de
 * la propia app (TikTok), y solo si la key está presente — no altera nada más.
 *
 * NOTA: en dispositivos sin Google Play Services (p. ej. el LineageOS de
 * prueba) el SDK de ads ni siquiera está, así que este hook no tiene efecto
 * observable ahí — pero ya no FALLA al instalarse, y en un dispositivo con
 * Play Services sí impide la inicialización. No se pudo verificar el efecto
 * real por falta de un device con Play Services.
 */
public class AdsMetadataBlocker {

    private static final String ADMOB_APP_ID_KEY = "com.google.android.gms.ads.APPLICATION_ID";
    private static final String PACKAGE_MANAGER_IMPL = "android.app.ApplicationPackageManager";

    public void block(ClassLoader classLoader) {
        XC_MethodHook stripMetadata = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!RuntimeSettings.enabled(Prefs.KEY_ADS_METADATA_BLOCKER, Prefs.DEFAULT_ADS_METADATA_BLOCKER)) return;
                Object result = param.getResult();
                if (!(result instanceof ApplicationInfo)) return;
                Bundle metaData = ((ApplicationInfo) result).metaData;
                if (metaData != null && metaData.containsKey(ADMOB_APP_ID_KEY)) {
                    metaData.remove(ADMOB_APP_ID_KEY);
                }
            }
        };

        int hooked = 0;
        try {
            for (java.lang.reflect.Method m :
                    XposedHelpers.findClass(PACKAGE_MANAGER_IMPL, classLoader).getDeclaredMethods()) {
                if (m.getName().equals("getApplicationInfo")) {
                    XposedBridge.hookMethod(m, stripMetadata);
                    hooked++;
                }
            }
            if (hooked > 0) {
                ModuleLog.line("(TikRatu | AdsMetadataBlocker): hooked getApplicationInfo (" + hooked + " metodos) para ocultar el AdMob App ID");
            } else {
                ModuleLog.line("(TikRatu | AdsMetadataBlocker): no se encontro getApplicationInfo en " + PACKAGE_MANAGER_IMPL);
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdsMetadataBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
