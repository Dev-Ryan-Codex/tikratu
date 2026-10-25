package dev.ryan.tikratu.mods.ui;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Oculta el boton de Tako (el asistente de IA de TikTok) del feed.
 *
 * TakoAssem es el componente del icono en la columna derecha del feed
 * (W3() = "right_container_chat_gpt"). Verificado en 47.4.3: vive en
 * split_df_a_dex, extiende BaseCellSlotComponent y sobreescribe
 * onViewCreated(View), cuyo nombre es del framework y no se ofusca. Los
 * demas metodos (fq, gq, nq...) si estan ofuscados y cambian por version,
 * asi que no se tocan: en cambio se marca el contenedor y se fuerza GONE
 * en View.setVisibility, porque fq(boolean) lo vuelve a mostrar al cambiar
 * de video.
 */
public class TakoAiBlocker {
    private static final String TAKO_ASSEM = "com.ss.android.ugc.aweme.feed.assem.tikbot.TakoAssem";
    // Clave de tag: debe ser un id "de aplicacion" (byte alto >= 2).
    private static final int TAG_KEY = 0x7e0a0001;

    private static boolean loggedHide;

    public void block(ClassLoader classLoader) {
        Class<?> tako = XposedHelpers.findClassIfExists(TAKO_ASSEM, classLoader);
        if (tako == null) {
            ModuleLog.line("(TikRatu | TakoAiBlocker): TakoAssem no existe en esta version de TikTok");
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(tako, "onViewCreated", new Object[]{View.class, new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    try {
                        View container = (View) XposedHelpers.callMethod(param.thisObject, "getContainerView", new Object[0]);
                        if (container == null) return;
                        container.setTag(TAG_KEY, Boolean.TRUE);
                        if (RuntimeSettings.enabled(Prefs.KEY_HIDE_TAKO_AI, false)) {
                            container.setVisibility(View.GONE);
                            if (!loggedHide) {
                                loggedHide = true;
                                ModuleLog.line("(TikRatu | TakoAiBlocker): boton de Tako oculto");
                            }
                        }
                    } catch (Throwable t) {
                        ModuleLog.line("(TikRatu | TakoAiBlocker): fallo al marcar el contenedor (" + t.getMessage() + ")");
                    }
                }
            }});
            XposedHelpers.findAndHookMethod(View.class, "setVisibility", new Object[]{Integer.TYPE, new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (((View) param.thisObject).getTag(TAG_KEY) != null
                            && RuntimeSettings.enabled(Prefs.KEY_HIDE_TAKO_AI, false)) {
                        param.args[0] = Integer.valueOf(View.GONE);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | TakoAiBlocker): hooked TakoAssem.onViewCreated + View.setVisibility");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | TakoAiBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
