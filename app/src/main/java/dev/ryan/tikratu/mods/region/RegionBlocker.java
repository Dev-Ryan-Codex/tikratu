package dev.ryan.tikratu.mods.region;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes8.dex */
public class RegionBlocker {
    private static final String[] CORE_CLASSES = {"X.03am", "X.03aj", "X.03bD"};
    private static final String CORE_METHOD = "LIZIZ";
    private static final String REGION_SERVICE = "com.ss.android.ugc.aweme.app.services.RegionService";
    private static boolean loggedCore;
    private static boolean loggedWrapper;

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(REGION_SERVICE, classLoader, "getRegion", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.region.RegionBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    String code = RegionBlocker.forcedCode();
                    if (code == null) {
                        return;
                    }
                    if (!RegionBlocker.loggedWrapper) {
                        RegionBlocker.loggedWrapper = true;
                        ModuleLog.line("(TikRatu | RegionBlocker): RegionService.getRegion() '" + param.getResult() + "' -> '" + code + "'");
                    }
                    param.setResult(code);
                }
            }});
            ModuleLog.line("(TikRatu | RegionBlocker): hooked RegionService.getRegion()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | RegionBlocker): fallo hook wrapper (" + t.getMessage() + ")");
        }
        boolean hooked = false;
        for (final String core : CORE_CLASSES) {
            try {
                Class<?> cls = XposedHelpers.findClassIfExists(core, classLoader);
                if (cls != null) {
                    XposedHelpers.findAndHookMethod(cls, CORE_METHOD, new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.region.RegionBlocker.2
                        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                            String code = RegionBlocker.forcedCode();
                            if (code == null) {
                                return;
                            }
                            if (!RegionBlocker.loggedCore) {
                                RegionBlocker.loggedCore = true;
                                ModuleLog.line("(TikRatu | RegionBlocker): " + core + "." + RegionBlocker.CORE_METHOD + "() '" + param.getResult() + "' -> '" + code + "'");
                            }
                            param.setResult(code);
                        }
                    }});
                    ModuleLog.line("(TikRatu | RegionBlocker): hooked core " + core + "." + CORE_METHOD + "()");
                    hooked = true;
                    break;
                }
            } catch (Throwable t2) {
                ModuleLog.line("(TikRatu | RegionBlocker): fallo hook core " + core + " (" + t2.getMessage() + ")");
            }
        }
        if (!hooked) {
            ModuleLog.line("(TikRatu | RegionBlocker): ningún core getter conocido existe en esta versión de TikTok");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String forcedCode() {
        String code;
        if (!RuntimeSettings.enabled(Prefs.KEY_FORCE_REGION, false) || (code = RuntimeSettings.getString(Prefs.KEY_REGION_CODE, "")) == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim().toLowerCase();
    }
}
