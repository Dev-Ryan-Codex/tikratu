package dev.ryan.tikratu.mods.interaction;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes11.dex */
public class StopVideoLoopingBlocker {
    private static final String VIDEO_ENGINE_CLASS = "com.ss.ttvideoengine.TTVideoEngine";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(VIDEO_ENGINE_CLASS, classLoader, "setLooping", new Object[]{Boolean.TYPE, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.interaction.StopVideoLoopingBlocker.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_STOP_VIDEO_LOOPING, false)) {
                        param.args[0] = false;
                    }
                }
            }});
            ModuleLog.line("(TikRatu | StopVideoLoopingBlocker): hooked com.ss.ttvideoengine.TTVideoEngine.setLooping(boolean)");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | StopVideoLoopingBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
