package dev.ryan.tikratu.mods.tracking;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes4.dex */
public class AdsMetadataBlocker {
    private static final String ADMOB_APP_ID_KEY = "com.google.android.gms.ads.APPLICATION_ID";
    private static final String PACKAGE_MANAGER_IMPL = "android.app.ApplicationPackageManager";

    public void block(ClassLoader classLoader) {
        XC_MethodHook stripMetadata = new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.tracking.AdsMetadataBlocker.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                Bundle metaData;
                if (RuntimeSettings.enabled(Prefs.KEY_ADS_METADATA_BLOCKER, true)) {
                    Object result = param.getResult();
                    if ((result instanceof ApplicationInfo) && (metaData = ((ApplicationInfo) result).metaData) != null && metaData.containsKey(AdsMetadataBlocker.ADMOB_APP_ID_KEY)) {
                        metaData.remove(AdsMetadataBlocker.ADMOB_APP_ID_KEY);
                    }
                }
            }
        };
        int hooked = 0;
        try {
            for (Method m : XposedHelpers.findClass(PACKAGE_MANAGER_IMPL, classLoader).getDeclaredMethods()) {
                if (m.getName().equals("getApplicationInfo")) {
                    XposedBridge.hookMethod(m, stripMetadata);
                    hooked++;
                }
            }
            if (hooked > 0) {
                ModuleLog.line("(TikRatu | AdsMetadataBlocker): hooked getApplicationInfo (" + hooked + " metodos) para ocultar el AdMob App ID");
            } else {
                ModuleLog.line("(TikRatu | AdsMetadataBlocker): no se encontro getApplicationInfo en android.app.ApplicationPackageManager");
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdsMetadataBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
