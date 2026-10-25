package dev.ryan.tikratu.mods.interaction;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes11.dex */
public class PlaybackSpeedBlocker {
    private static final String[] SPEED_SETTERS = {"LIZ"};
    private static final String VIDEO_ENGINE_CLASS = "com.ss.ttvideoengine.TTVideoEngine";
    private static boolean loggedApply;

    public void block(ClassLoader classLoader) {
        XC_MethodHook applyHook = new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.interaction.PlaybackSpeedBlocker.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                PlaybackSpeedBlocker.this.applySpeed(param.thisObject);
            }
        };
        int hooked = 0;
        String[] strArr = {"play", "start"};
        for (int i = 0; i < 2; i++) {
            String m = strArr[i];
            try {
                XposedHelpers.findAndHookMethod(VIDEO_ENGINE_CLASS, classLoader, m, new Object[]{applyHook});
                hooked++;
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu | PlaybackSpeedBlocker): no se pudo hookear " + m + "() (" + t.getMessage() + ")");
            }
        }
        ModuleLog.line("(TikRatu | PlaybackSpeedBlocker): hooked TTVideoEngine (" + hooked + " métodos play/start)");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applySpeed(Object engine) {
        float speed = currentSpeed();
        if (speed <= 0.0f || speed == 1.0f) {
            return;
        }
        for (String setter : SPEED_SETTERS) {
            try {
                XposedHelpers.callMethod(engine, setter, new Object[]{Float.valueOf(speed), Float.valueOf(1.0f)});
                if (!loggedApply) {
                    loggedApply = true;
                    ModuleLog.line("(TikRatu | PlaybackSpeedBlocker): velocidad forzada a " + speed + "x vía " + setter + "(float,float)");
                    return;
                }
                return;
            } catch (Throwable th) {
            }
        }
        if (!loggedApply) {
            loggedApply = true;
            ModuleLog.line("(TikRatu | PlaybackSpeedBlocker): ningún setter de velocidad conocido funcionó en esta versión");
        }
    }

    private static float currentSpeed() {
        String v = RuntimeSettings.getString(Prefs.KEY_PLAYBACK_SPEED, Prefs.DEFAULT_PLAYBACK_SPEED);
        try {
            return Float.parseFloat(v.trim());
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }
}
