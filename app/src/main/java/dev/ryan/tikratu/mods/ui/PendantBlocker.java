package dev.ryan.tikratu.mods.ui;

import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.lang.reflect.Field;

/**
 * Oculta el "pendant": el banner/widget flotante de actividades y recompensas
 * (touchpoint) que TikTok superpone al feed.
 *
 * Verificado en 47.4.3 (classes4.dex): com.bytedance.touchpoint.ui.pendant.SpecActWidget
 * implementa la interfaz del pendant, asi que bind(ViewGroup) y showNormalPendant()
 * tienen nombre estable. bind() crea la vista y la agrega a la ViewGroup recibida;
 * el campo que la guarda esta ofuscado, asi que se localiza como el campo View
 * cuyo padre es esa ViewGroup.
 */
public class PendantBlocker {
    private static final String SPEC_ACT_WIDGET = "com.bytedance.touchpoint.ui.pendant.SpecActWidget";

    private static boolean loggedHide;

    public void block(ClassLoader classLoader) {
        Class<?> widget = XposedHelpers.findClassIfExists(SPEC_ACT_WIDGET, classLoader);
        if (widget == null) {
            ModuleLog.line("(TikRatu | PendantBlocker): SpecActWidget no existe en esta version de TikTok");
            return;
        }
        XC_MethodHook rehide = new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                ViewGroup parent = param.args.length == 1 && param.args[0] instanceof ViewGroup
                        ? (ViewGroup) param.args[0] : null;
                hidePendantViews(param.thisObject, parent);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(widget, "bind", new Object[]{ViewGroup.class, rehide});
            XposedHelpers.findAndHookMethod(widget, "showNormalPendant", new Object[]{rehide});
            ModuleLog.line("(TikRatu | PendantBlocker): hooked SpecActWidget.bind/showNormalPendant");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | PendantBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }

    private static void hidePendantViews(Object widget, ViewGroup parent) {
        try {
            for (Field f : widget.getClass().getDeclaredFields()) {
                if (!View.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object v = f.get(widget);
                if (!(v instanceof View)) continue;
                View view = (View) v;
                // El campo rootLayout tambien es una View: solo la vista agregada al padre.
                if (parent != null ? view.getParent() != parent : view.getParent() == null) continue;
                HiddenViews.hide(view, Prefs.KEY_HIDE_PENDANT);
                if (!loggedHide) {
                    loggedHide = true;
                    ModuleLog.line("(TikRatu | PendantBlocker): pendant marcado (" + f.getName() + ")");
                }
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | PendantBlocker): fallo al marcar la vista (" + t.getMessage() + ")");
        }
    }
}
