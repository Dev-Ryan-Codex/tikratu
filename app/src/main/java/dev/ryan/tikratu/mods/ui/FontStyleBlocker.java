package dev.ryan.tikratu.mods.ui;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado en el dex de TikTok 47.0.3 (strings): la fuente propia de la app
 * se carga desde un asset embebido en el propio APK,
 * "font/TikTokSans-VF.otf" (variable font "TikTok Sans"), confirmado en
 * varios classes*.dex (ej. classes2.dex, classes7.dex: literal
 * "assets://font/TikTokSans-VF.otf").
 *
 * A diferencia de un mod que parchea el APK (que puede reemplazar el archivo
 * .otf físicamente), un hook de runtime no puede sustituir el asset — pero sí
 * puede interceptar el punto donde Android CARGA ese asset como Typeface:
 * android.graphics.Typeface.createFromAsset(AssetManager, String) — API
 * pública de Android, no una clase de TikTok (mismo criterio de riesgo que
 * UrlSanitizerBlocker/AdsMetadataBlocker: estable sin importar cómo TikTok
 * ofusque su código interno). Si el path pedido es el de TikTok Sans, se
 * devuelve la tipografía sans-serif del sistema en su lugar.
 */
public class FontStyleBlocker {

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(Typeface.class, "createFromAsset",
                    AssetManager.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!RuntimeSettings.enabled(Prefs.KEY_FONT_STYLE_BLOCKER, Prefs.DEFAULT_FONT_STYLE_BLOCKER)) return;
                            Object path = param.args[1];
                            if (path instanceof String && ((String) path).contains("TikTokSans")) {
                                param.setResult(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | FontStyleBlocker): hooked Typeface.createFromAsset()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FontStyleBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
