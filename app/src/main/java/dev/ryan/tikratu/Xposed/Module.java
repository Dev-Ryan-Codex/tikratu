package dev.ryan.tikratu.Xposed;

import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dev.ryan.tikratu.mods.ads.AdBlocker;
import dev.ryan.tikratu.mods.ads.AdSignalsBlocker;
import dev.ryan.tikratu.mods.feed.FeedFilterBlocker;
import dev.ryan.tikratu.mods.interaction.DisableLoginBlocker;
import dev.ryan.tikratu.mods.interaction.GoogleLoginFixBlocker;
import dev.ryan.tikratu.mods.interaction.PlaybackSpeedBlocker;
import dev.ryan.tikratu.mods.interaction.ScreenCaptureBlocker;
import dev.ryan.tikratu.mods.interaction.StopVideoLoopingBlocker;
import dev.ryan.tikratu.mods.ui.TakoAiBlocker;
import dev.ryan.tikratu.mods.media.DownloadUnlockBlocker;
import dev.ryan.tikratu.mods.media.PhotoWatermarkBlocker;
import dev.ryan.tikratu.mods.media.UrlSanitizerBlocker;
import dev.ryan.tikratu.mods.media.WatermarkBlocker;
import dev.ryan.tikratu.mods.region.RegionBlocker;
import dev.ryan.tikratu.mods.region.TelephonyRegionBlocker;
import dev.ryan.tikratu.mods.tracking.AdsIdBlocker;
import dev.ryan.tikratu.mods.tracking.AdsMetadataBlocker;
import dev.ryan.tikratu.mods.tracking.LocationBlocker;
import dev.ryan.tikratu.mods.ui.FontStyleBlocker;
import dev.ryan.tikratu.utils.ModulePackage;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes9.dex */
public class Module implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";
    public static volatile String modulePath;

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (ModulePackage.NAME.equals(lpparam.packageName)) {
            hookSelfStatusCheck(lpparam);
        } else {
            if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
                return;
            }
            ModuleLog.line("(TikRatu): cargando en " + lpparam.packageName + " (proceso " + lpparam.processName + ")");
            XposedHelpers.findAndHookMethod(Application.class, "attach", new Object[]{Context.class, new XC_MethodHook() { // from class: dev.ryan.tikratu.Xposed.Module.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    Module.this.installHooks(lpparam, (Context) param.thisObject);
                }
            }});
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void installHooks(XC_LoadPackage.LoadPackageParam lpparam, Context context) {
        RuntimeSettings.init(context);
        ClassLoader cl = lpparam.classLoader;
        new AdBlocker().block(cl);
        new AdsIdBlocker().block(cl);
        new AdsMetadataBlocker().block(cl);
        new LocationBlocker().block(cl);
        new WatermarkBlocker().block(cl);
        new PhotoWatermarkBlocker().block(cl);
        new DownloadUnlockBlocker().block(cl);
        new AdSignalsBlocker().block(cl);
        new UrlSanitizerBlocker().block(cl);
        new FontStyleBlocker().block(cl);
        new TakoAiBlocker().block(cl);
        new FeedFilterBlocker().block(cl);
        new StopVideoLoopingBlocker().block(cl);
        new PlaybackSpeedBlocker().block(cl);
        new ScreenCaptureBlocker().block(cl);
        new DisableLoginBlocker().block(cl);
        new RegionBlocker().block(cl);
        new TelephonyRegionBlocker().block(cl);
        new GoogleLoginFixBlocker().block(cl);
    }

    private void hookSelfStatusCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("dev.ryan.tikratu.utils.StatusChecker", lpparam.classLoader, "isModuleActive", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.Xposed.Module.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    param.setResult(true);
                }
            }});
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): no se pudo instalar el self-check (" + t.getMessage() + ")");
        }
    }
}
