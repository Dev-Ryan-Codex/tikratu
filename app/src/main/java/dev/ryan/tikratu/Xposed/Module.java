package dev.ryan.tikratu.Xposed;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dev.ryan.tikratu.mods.ads.AdBlocker;
import dev.ryan.tikratu.mods.tracking.AdsIdBlocker;
import dev.ryan.tikratu.mods.tracking.AdsMetadataBlocker;
import dev.ryan.tikratu.mods.tracking.LocationBlocker;
import dev.ryan.tikratu.mods.media.PhotoWatermarkBlocker;
import dev.ryan.tikratu.mods.media.WatermarkBlocker;
import dev.ryan.tikratu.utils.ModulePackage;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

public class Module implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (ModulePackage.NAME.equals(lpparam.packageName)) {
            hookSelfStatusCheck(lpparam);
            return;
        }

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        ModuleLog.line("(TikRatu): cargando en " + lpparam.packageName + " (proceso " + lpparam.processName + ")");

        XSharedPreferences prefs = new XSharedPreferences(ModulePackage.NAME);
        prefs.makeWorldReadable();
        prefs.reload();

        if (prefs.getBoolean(Prefs.KEY_ADS_ID_BLOCKER, Prefs.DEFAULT_ADS_ID_BLOCKER)) {
            new AdsIdBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_LOCATION_BLOCKER, Prefs.DEFAULT_LOCATION_BLOCKER)) {
            new LocationBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_ADS_METADATA_BLOCKER, Prefs.DEFAULT_ADS_METADATA_BLOCKER)) {
            new AdsMetadataBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_WATERMARK_BLOCKER, Prefs.DEFAULT_WATERMARK_BLOCKER)) {
            new WatermarkBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_PHOTO_WATERMARK_BLOCKER, Prefs.DEFAULT_PHOTO_WATERMARK_BLOCKER)) {
            new PhotoWatermarkBlocker().block(lpparam.classLoader);
        }

        if (prefs.getBoolean(Prefs.KEY_AD_BLOCKER, Prefs.DEFAULT_AD_BLOCKER)) {
            try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                new AdBlocker().disableFeedAdFlag(bridge, lpparam.classLoader);
                // Proximos hooks (watermark, duet/stitch, etc.) se agregan aca,
                // uno por clase en dev.ryan.tikratu.mods.<categoria>, igual que AdBlocker.
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu): error inicializando DexKit: " + t.getMessage());
            }
        }
    }

    /**
     * Truco de "¿esta activo?": hookeamos StatusChecker.isModuleActive() para que
     * devuelva true SOLO cuando este mismo codigo corre via LSPosed dentro del
     * proceso de la propia companion app (dev.ryan.tikratu esta en xposedscope,
     * ver arrays.xml). Si LSPosed no esta activo, el metodo original (que
     * siempre devuelve false) queda sin hookear y la companion app lo detecta.
     */
    private void hookSelfStatusCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "dev.ryan.tikratu.utils.StatusChecker", lpparam.classLoader,
                    "isModuleActive", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): no se pudo instalar el self-check (" + t.getMessage() + ")");
        }
    }
}
