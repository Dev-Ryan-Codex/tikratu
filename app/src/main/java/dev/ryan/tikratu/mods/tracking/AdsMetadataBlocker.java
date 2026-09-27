package dev.ryan.tikratu.mods.tracking;

import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * El TikTok oficial declara en su AndroidManifest.xml:
 *   <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID"
 *              android:value="ca-app-pub-6536861780333891~4547192519"/>
 * (verificado leyendo el manifest decodificado con apktool en 46.4.3 y 47.0.3).
 * El SDK de Google Mobile Ads lee ese valor llamando
 * ApplicationInfo.metaData.getString("com.google.android.gms.ads.APPLICATION_ID")
 * al iniciar. Si esa key no esta (o es null), el SDK de ads no puede
 * inicializarse. Hookeamos Bundle.getString(String) a nivel de proceso e
 * interceptamos solo esa key puntual -> devolvemos null.
 *
 * Nota de rendimiento: Bundle.getString(String) se llama muy seguido en
 * cualquier app Android (no solo para metaData), asi que este hook corre en
 * cada invocacion del proceso de TikTok. El chequeo es un simple equals()
 * contra un String constante, deberia ser insignificante, pero no se
 * bench-marqueo en un dispositivo real todavia.
 */
public class AdsMetadataBlocker {

    private static final String ADMOB_APP_ID_KEY = "com.google.android.gms.ads.APPLICATION_ID";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(Bundle.class, "getString", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (ADMOB_APP_ID_KEY.equals(param.args[0])) {
                                param.setResult(null);
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | AdsMetadataBlocker): hooked Bundle.getString para ocultar el AdMob App ID");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdsMetadataBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
