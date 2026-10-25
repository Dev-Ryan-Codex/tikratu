package dev.ryan.tikratu.mods.tracking;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes4.dex */
public class AdsIdBlocker {
    private static final String INFO_CLASS = "com.google.android.gms.ads.identifier.AdvertisingIdClient$Info";
    private static final String ZERO_UUID = "00000000-0000-0000-0000-000000000000";

    public void block(ClassLoader classLoader) {
        boolean any = false;
        try {
            XposedHelpers.findAndHookMethod(INFO_CLASS, classLoader, "getId", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.tracking.AdsIdBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_ADS_ID_BLOCKER, true)) {
                        param.setResult(AdsIdBlocker.ZERO_UUID);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | AdsIdBlocker): hooked AdvertisingIdClient.Info.getId() -> UUID cero");
            any = true;
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdsIdBlocker): getId() no hookeable (" + t.getMessage() + ")");
        }
        try {
            XposedHelpers.findAndHookMethod(INFO_CLASS, classLoader, "isLimitAdTrackingEnabled", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.tracking.AdsIdBlocker.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_ADS_ID_BLOCKER, true)) {
                        param.setResult(true);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | AdsIdBlocker): hooked isLimitAdTrackingEnabled() -> true");
            any = true;
        } catch (Throwable th) {
            ModuleLog.line("(TikRatu | AdsIdBlocker): isLimitAdTrackingEnabled() no existe en este build (normal)");
        }
        if (!any) {
            ModuleLog.line("(TikRatu | AdsIdBlocker): ningun metodo de AdvertisingIdClient.Info se pudo hookear");
        }
    }
}
