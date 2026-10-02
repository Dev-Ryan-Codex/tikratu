package dev.ryan.tikratu.mods.region;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Fuerza la región que TikTok reporta para cargar contenido.
 *
 * Cadena de región verificada en dex (jadx):
 *   com.ss.android.ugc.aweme.app.services.RegionService implements IRegionService {
 *       public String getRegion() { return X.<core>.LIZIZ(); }
 *   }
 *   - 47.0.3: el core es X.03bD   (getRegion() -> 03bD.LIZIZ())
 *   - 47.1.4: el core es X.03aj   (getRegion() -> 03aj.LIZIZ())
 *   => el nombre ofuscado del core CAMBIA entre versiones. Por eso se intenta
 *      una lista de candidatos y se hookea el que exista en runtime.
 *
 * Verificado EN DISPOSITIVO (47.1.4, logcat): RegionService.getRegion() NO se
 * llama en el arranque ni al navegar el feed — los consumidores llaman al core
 * ofuscado (X.03aj.LIZIZ()) directamente. Por eso hookeamos el CORE, no sólo el
 * wrapper limpio. LIZIZ() es `static String` (sin args), devuelve el código de
 * región (ISO alpha-2 en minúscula). Los params de API derivados son
 * region / store_region / sys_region / carrier_region.
 *
 * Lista de 206 países en res/values/regions.xml (extraída del plugin de
 * terceros). Código vacío o toggle apagado = región del dispositivo.
 *
 * FRÁGIL: al actualizar TikTok, si el core cambia de nombre hay que añadir el
 * nuevo a CORE_GETTER_CANDIDATES (se detecta con: RegionService.getRegion() ->
 * X.<nuevo>.LIZIZ() en el dex de la versión nueva).
 */
public class RegionBlocker {

    private static final String REGION_SERVICE = "com.ss.android.ugc.aweme.app.services.RegionService";
    // Core getter ofuscado por versión. Orden: más nuevo primero.
    private static final String[] CORE_CLASSES = {"X.03aj", "X.03bD"};
    private static final String CORE_METHOD = "LIZIZ";

    private static boolean loggedWrapper;
    private static boolean loggedCore;

    public void block(ClassLoader classLoader) {
        // 1) Wrapper limpio (por si algún consumidor sí lo usa).
        try {
            XposedHelpers.findAndHookMethod(REGION_SERVICE, classLoader, "getRegion", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String code = forcedCode();
                    if (code == null) return;
                    if (!loggedWrapper) {
                        loggedWrapper = true;
                        ModuleLog.line("(TikRatu | RegionBlocker): RegionService.getRegion() '" + param.getResult() + "' -> '" + code + "'");
                    }
                    param.setResult(code);
                }
            });
            ModuleLog.line("(TikRatu | RegionBlocker): hooked RegionService.getRegion()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | RegionBlocker): fallo hook wrapper (" + t.getMessage() + ")");
        }

        // 2) Core getter ofuscado (el que realmente se llama). Se hookea el
        //    primer candidato que exista en esta versión.
        boolean hooked = false;
        for (String core : CORE_CLASSES) {
            try {
                Class<?> cls = XposedHelpers.findClassIfExists(core, classLoader);
                if (cls == null) continue;
                XposedHelpers.findAndHookMethod(cls, CORE_METHOD, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String code = forcedCode();
                        if (code == null) return;
                        if (!loggedCore) {
                            loggedCore = true;
                            ModuleLog.line("(TikRatu | RegionBlocker): " + core + "." + CORE_METHOD + "() '" + param.getResult() + "' -> '" + code + "'");
                        }
                        param.setResult(code);
                    }
                });
                ModuleLog.line("(TikRatu | RegionBlocker): hooked core " + core + "." + CORE_METHOD + "()");
                hooked = true;
                break;
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu | RegionBlocker): fallo hook core " + core + " (" + t.getMessage() + ")");
            }
        }
        if (!hooked) {
            ModuleLog.line("(TikRatu | RegionBlocker): ningún core getter conocido existe en esta versión de TikTok");
        }
    }

    /** Código ISO forzado (minúscula) o null si no hay que forzar. */
    private static String forcedCode() {
        if (!RuntimeSettings.enabled(Prefs.KEY_FORCE_REGION, Prefs.DEFAULT_FORCE_REGION)) return null;
        String code = RuntimeSettings.getString(Prefs.KEY_REGION_CODE, Prefs.DEFAULT_REGION_CODE);
        if (code == null || code.trim().isEmpty()) return null;
        return code.trim().toLowerCase();
    }
}
