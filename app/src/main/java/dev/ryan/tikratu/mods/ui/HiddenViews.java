package dev.ryan.tikratu.mods.ui;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Vistas de TikTok que un toggle debe mantener ocultas. TikTok las vuelve a
 * mostrar (cambio de video, animaciones, etc.), asi que no basta con ponerles
 * GONE una vez: se marcan con un tag que guarda la clave del toggle y un unico
 * hook a View.setVisibility fuerza GONE mientras el toggle siga activo.
 */
public final class HiddenViews {
    // Clave de tag: debe ser un id de aplicacion (byte alto >= 2).
    private static final int TAG_KEY = 0x7e0a0001;
    private static boolean guardInstalled;

    private HiddenViews() {
    }

    /** Marca la vista con la clave del toggle y la oculta si esta activo. */
    public static void hide(View view, String prefKey) {
        if (view == null) return;
        installGuard();
        view.setTag(TAG_KEY, prefKey);
        if (RuntimeSettings.enabled(prefKey, false)) {
            view.setVisibility(View.GONE);
        }
    }

    private static synchronized void installGuard() {
        if (guardInstalled) return;
        guardInstalled = true;
        try {
            XposedHelpers.findAndHookMethod(View.class, "setVisibility", new Object[]{Integer.TYPE, new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    Object key = ((View) param.thisObject).getTag(TAG_KEY);
                    if (key != null && RuntimeSettings.enabled((String) key, false)) {
                        param.args[0] = Integer.valueOf(View.GONE);
                    }
                }
            }});
        } catch (Throwable t) {
            guardInstalled = false;
            ModuleLog.line("(TikRatu | HiddenViews): fallo el hook de View.setVisibility (" + t.getMessage() + ")");
        }
    }
}
