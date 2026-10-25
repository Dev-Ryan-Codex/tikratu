package dev.ryan.tikratu.mods.ads;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes12.dex */
public class AdBlocker {
    private static final String TARGET_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(TARGET_CLASS, classLoader, "isAd", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.ads.AdBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_AD_BLOCKER, true)) {
                        param.setResult(false);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | AdBlocker): hooked com.ss.android.ugc.aweme.feed.model.Aweme.isAd()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
