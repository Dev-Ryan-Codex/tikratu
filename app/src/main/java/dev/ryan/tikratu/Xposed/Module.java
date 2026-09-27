package dev.ryan.tikratu.Xposed;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dev.ryan.tikratu.mods.ads.AdBlocker;
import dev.ryan.tikratu.mods.tracking.AdsIdBlocker;
import dev.ryan.tikratu.mods.tracking.AdsMetadataBlocker;
import dev.ryan.tikratu.mods.tracking.LocationBlocker;
import dev.ryan.tikratu.utils.log.ModuleLog;

public class Module implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        ModuleLog.line("(TikRatu): cargando en " + lpparam.packageName + " (proceso " + lpparam.processName + ")");

        // Hooks que no necesitan DexKit (apuntan a clases publicas de Android /
        // Play Services, no a codigo interno ofuscado de TikTok).
        new AdsIdBlocker().block(lpparam.classLoader);
        new LocationBlocker().block(lpparam.classLoader);
        new AdsMetadataBlocker().block(lpparam.classLoader);

        // Hooks que dependen de encontrar codigo interno de TikTok en runtime.
        try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
            new AdBlocker().disableFeedAdFlag(bridge, lpparam.classLoader);
            // Proximos hooks (watermark, duet/stitch, etc.) se agregan aca,
            // uno por clase en dev.ryan.tikratu.mods.<categoria>, igual que AdBlocker.
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): error inicializando DexKit: " + t.getMessage());
        }
    }
}
