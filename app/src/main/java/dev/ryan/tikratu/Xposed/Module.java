package dev.ryan.tikratu.Xposed;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dev.ryan.tikratu.mods.ads.AdBlocker;
import dev.ryan.tikratu.utils.log.ModuleLog;

public class Module implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        ModuleLog.line("(TikRatu): cargando en " + lpparam.packageName + " (proceso " + lpparam.processName + ")");

        try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
            new AdBlocker().disableFeedAdFlag(bridge, lpparam.classLoader);
            // Próximos hooks (watermark, duet/stitch, region, etc.) se agregan acá,
            // uno por clase en dev.ryan.tikratu.mods.<categoria>, igual que AdBlocker.
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): error inicializando DexKit: " + t.getMessage());
        }
    }
}
