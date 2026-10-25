package dev.ryan.tikratu.mods.tracking;

import android.location.LocationManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes4.dex */
public class LocationBlocker {
    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(LocationManager.class, "getLastKnownLocation", new Object[]{String.class, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.tracking.LocationBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_LOCATION_BLOCKER, true)) {
                        param.setResult((Object) null);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | LocationBlocker): hooked LocationManager.getLastKnownLocation");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | LocationBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
