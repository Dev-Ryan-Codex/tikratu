package dev.ryan.tikratu.mods.ui;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.Module;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Reemplaza la tipografía propia de TikTok por la que elija el usuario.
 *
 * Verificado en dispositivo (TikTok 47.1.4, logcat del propio módulo): la app
 * NO carga su fuente con Typeface.createFromAsset(AssetManager, String) — como
 * se asumía antes, un hook que por eso nunca se disparaba — sino con la API
 * moderna android.graphics.Typeface$Builder, construida con
 *   new Typeface.Builder(assetManager, "font/TikTokSans-VF.otf")
 * (literal capturado del constructor en runtime, decenas de veces al abrir).
 *
 * Estrategia: se marca en el propio objeto Builder (additional instance field,
 * sin depender de nombres de campos internos del framework) cada Builder creado
 * con la ruta de TikTok Sans; luego se intercepta Builder.build() y, si el
 * Builder estaba marcado y el usuario tiene el reemplazo activo, se devuelve la
 * fuente elegida en lugar de la original.
 *
 * Selector de fuente (Prefs.KEY_FONT_CHOICE):
 *   - "system"    → sans-serif del sistema
 *   - "serif"     → serif del sistema
 *   - "monospace" → monoespaciada del sistema
 *   - "ios"       → Inter (assets/fonts/ios.ttf de ESTE módulo). Inter es la
 *                   fuente abierta (SIL OFL) más usada como sustituto de la
 *                   San Francisco de iOS; la SF real es propietaria de Apple y
 *                   no puede redistribuirse dentro del APK.
 *
 * La fuente "ios" se carga desde el APK del propio módulo (no el de TikTok)
 * mediante un AssetManager creado por reflexión que apunta a Module.modulePath,
 * el patrón estándar de Xposed para servir assets propios dentro de un proceso
 * ajeno. Se cachea tras la primera carga exitosa.
 */
public class FontStyleBlocker {

    private static final String MARK = "tikratu_is_tiktoksans";
    private static final java.util.Map<String, Typeface> FONT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean loggedSwap;

    public void block(ClassLoader classLoader) {
        Class<?> builder;
        try {
            builder = XposedHelpers.findClass("android.graphics.Typeface$Builder", classLoader);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FontStyleBlocker): no existe Typeface.Builder (" + t.getMessage() + ")");
            return;
        }

        // 1) Marcar los Builder construidos con la ruta de TikTok Sans.
        try {
            XposedBridge.hookAllConstructors(builder, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args.length >= 2
                            && param.args[0] instanceof AssetManager
                            && param.args[1] instanceof String
                            && ((String) param.args[1]).contains("TikTokSans")) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, MARK, Boolean.TRUE);
                    }
                }
            });
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FontStyleBlocker): fallo hook constructor (" + t.getMessage() + ")");
        }

        // 2) Interceptar build() y reemplazar el resultado si estaba marcado.
        try {
            XposedHelpers.findAndHookMethod(builder, "build", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!RuntimeSettings.enabled(Prefs.KEY_FONT_STYLE_BLOCKER, Prefs.DEFAULT_FONT_STYLE_BLOCKER)) return;
                    Object marked = XposedHelpers.getAdditionalInstanceField(param.thisObject, MARK);
                    if (marked != Boolean.TRUE) return;
                    Typeface replacement = resolveTypeface();
                    if (replacement != null) {
                        param.setResult(replacement);
                        if (!loggedSwap) {
                            loggedSwap = true;
                            ModuleLog.line("(TikRatu | FontStyleBlocker): TikTok Sans reemplazada por '"
                                    + RuntimeSettings.getString(Prefs.KEY_FONT_CHOICE, Prefs.DEFAULT_FONT_CHOICE) + "'");
                        }
                    }
                }
            });
            ModuleLog.line("(TikRatu | FontStyleBlocker): hooked Typeface.Builder.build()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FontStyleBlocker): fallo hook build() (" + t.getMessage() + ")");
        }
    }

    private Typeface resolveTypeface() {
        String choice = RuntimeSettings.getString(Prefs.KEY_FONT_CHOICE, Prefs.DEFAULT_FONT_CHOICE);
        switch (choice) {
            case Prefs.FONT_SERIF:
                return Typeface.create(Typeface.SERIF, Typeface.NORMAL);
            case Prefs.FONT_MONOSPACE:
                return Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
            case Prefs.FONT_IOS:
                // Inter es variable font; se fija el eje de peso en 600 (SemiBold).
                return orSystem(loadModuleFont("fonts/ios.ttf", 600));
            case Prefs.FONT_HYPEROS:
                // MiSans-Semibold ya es un archivo de peso SemiBold fijo (no variable),
                // se usa tal cual sin forzar eje de peso.
                return orSystem(loadModuleFont("fonts/hyperos.ttf", 0));
            case Prefs.FONT_SYSTEM:
            default:
                return Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        }
    }

    /** Si la carga de la fuente embebida falla, cae al sans-serif del sistema (predecible). */
    private static Typeface orSystem(Typeface tf) {
        return tf != null ? tf : Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    }

    /**
     * Carga una fuente embebida en el APK del propio módulo (assets/fonts/...)
     * desde dentro del proceso de TikTok, vía un AssetManager por reflexión que
     * apunta a Module.modulePath (patrón estándar de Xposed). Cachea por ruta.
     * weight > 0 fuerza ese eje de peso (para variable fonts); weight 0 = tal cual.
     */
    private Typeface loadModuleFont(String assetPath, int weight) {
        Typeface cached = FONT_CACHE.get(assetPath);
        if (cached != null) return cached;
        synchronized (FontStyleBlocker.class) {
            cached = FONT_CACHE.get(assetPath);
            if (cached != null) return cached;
            try {
                String apk = Module.modulePath;
                if (apk == null) {
                    ModuleLog.line("(TikRatu | FontStyleBlocker): Module.modulePath es null; no se puede cargar " + assetPath);
                    return null;
                }
                AssetManager am = AssetManager.class.newInstance();
                XposedHelpers.callMethod(am, "addAssetPath", apk);
                Typeface tf = Typeface.createFromAsset(am, assetPath);
                if (weight > 0) tf = Typeface.create(tf, weight, false);
                FONT_CACHE.put(assetPath, tf);
                ModuleLog.line("(TikRatu | FontStyleBlocker): fuente '" + assetPath + "'"
                        + (weight > 0 ? " (peso " + weight + ")" : "") + " cargada desde " + apk);
                return tf;
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu | FontStyleBlocker): fallo cargar " + assetPath + " (" + t.getMessage() + ")");
                return null;
            }
        }
    }
}
