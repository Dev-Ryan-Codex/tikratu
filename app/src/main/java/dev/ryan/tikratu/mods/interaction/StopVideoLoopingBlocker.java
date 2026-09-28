package dev.ryan.tikratu.mods.interaction;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * "Dejar de reproducir en bucle" — el video se detiene al terminar en vez de
 * repetirse.
 *
 * Punto de hook tomado del parche "Stop video looping" de
 * icysymmetra/tiktok-patches-for-morphe (fork de ReVanced): el reproductor
 * de ByteDance es com.ss.ttvideoengine.TTVideoEngine, con el metodo publico
 * setLooping(boolean). Confirmado en TikTok 47.0.3 (strings del dex: la
 * clase y el metodo aparecen sin ofuscar, son de una libreria de ByteDance,
 * no del codigo minificado de la app). Se fuerza el argumento a false antes
 * de que corra el metodo original.
 */
public class StopVideoLoopingBlocker {

    private static final String VIDEO_ENGINE_CLASS = "com.ss.ttvideoengine.TTVideoEngine";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(VIDEO_ENGINE_CLASS, classLoader, "setLooping", boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.args[0] = false;
                        }
                    });
            ModuleLog.line("(TikRatu | StopVideoLoopingBlocker): hooked " + VIDEO_ENGINE_CLASS + ".setLooping(boolean)");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | StopVideoLoopingBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
