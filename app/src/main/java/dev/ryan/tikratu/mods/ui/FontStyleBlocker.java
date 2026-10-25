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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: classes9.dex */
public class FontStyleBlocker {
    private static final Map<String, Typeface> FONT_CACHE = new ConcurrentHashMap();
    private static final String MARK = "tikratu_is_tiktoksans";
    private static volatile boolean loggedSwap;

    public void block(ClassLoader classLoader) {
        try {
            Class<?> builder = XposedHelpers.findClass("android.graphics.Typeface$Builder", classLoader);
            try {
                XposedBridge.hookAllConstructors(builder, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.ui.FontStyleBlocker.1
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                        if (param.args.length >= 2 && (param.args[0] instanceof AssetManager) && (param.args[1] instanceof String) && ((String) param.args[1]).contains("TikTokSans")) {
                            XposedHelpers.setAdditionalInstanceField(param.thisObject, FontStyleBlocker.MARK, Boolean.TRUE);
                        }
                    }
                });
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu | FontStyleBlocker): fallo hook constructor (" + t.getMessage() + ")");
            }
            try {
                XposedHelpers.findAndHookMethod(builder, "build", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.ui.FontStyleBlocker.2
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                        Typeface replacement;
                        if (RuntimeSettings.enabled(Prefs.KEY_FONT_STYLE_BLOCKER, false)) {
                            Object marked = XposedHelpers.getAdditionalInstanceField(param.thisObject, FontStyleBlocker.MARK);
                            if (marked == Boolean.TRUE && (replacement = FontStyleBlocker.this.resolveTypeface()) != null) {
                                param.setResult(replacement);
                                if (!FontStyleBlocker.loggedSwap) {
                                    FontStyleBlocker.loggedSwap = true;
                                    ModuleLog.line("(TikRatu | FontStyleBlocker): TikTok Sans reemplazada por '" + RuntimeSettings.getString(Prefs.KEY_FONT_CHOICE, "system") + "'");
                                }
                            }
                        }
                    }
                }});
                ModuleLog.line("(TikRatu | FontStyleBlocker): hooked Typeface.Builder.build()");
            } catch (Throwable t2) {
                ModuleLog.line("(TikRatu | FontStyleBlocker): fallo hook build() (" + t2.getMessage() + ")");
            }
        } catch (Throwable t3) {
            ModuleLog.line("(TikRatu | FontStyleBlocker): no existe Typeface.Builder (" + t3.getMessage() + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:20:0x0041  */
    public Typeface resolveTypeface() {
        String choice = RuntimeSettings.getString(Prefs.KEY_FONT_CHOICE, "system");
        switch (choice) {
            case "serif":
                return Typeface.create(Typeface.SERIF, 0);
            case "monospace":
                return Typeface.create(Typeface.MONOSPACE, 0);
            case "ios":
                return orSystem(loadModuleFont("fonts/ios.ttf", 600));
            case "hyperos":
                return orSystem(loadModuleFont("fonts/hyperos.ttf", 0));
            case "system":
            default:
                return Typeface.create(Typeface.SANS_SERIF, 0);
        }
    }

    private static Typeface orSystem(Typeface tf) {
        return tf != null ? tf : Typeface.create(Typeface.SANS_SERIF, 0);
    }

    private Typeface loadModuleFont(String assetPath, int weight) {
        Typeface cached = FONT_CACHE.get(assetPath);
        if (cached != null) {
            return cached;
        }
        synchronized (FontStyleBlocker.class) {
            Typeface cached2 = FONT_CACHE.get(assetPath);
            if (cached2 != null) {
                return cached2;
            }
            try {
                String apk = Module.modulePath;
                if (apk == null) {
                    ModuleLog.line("(TikRatu | FontStyleBlocker): Module.modulePath es null; no se puede cargar " + assetPath);
                    return null;
                }
                AssetManager am = (AssetManager) AssetManager.class.newInstance();
                XposedHelpers.callMethod(am, "addAssetPath", new Object[]{apk});
                Typeface tf = Typeface.createFromAsset(am, assetPath);
                if (weight > 0) {
                    tf = Typeface.create(tf, weight, false);
                }
                FONT_CACHE.put(assetPath, tf);
                ModuleLog.line("(TikRatu | FontStyleBlocker): fuente '" + assetPath + "'" + (weight > 0 ? " (peso " + weight + ")" : "") + " cargada desde " + apk);
                return tf;
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu | FontStyleBlocker): fallo cargar " + assetPath + " (" + t.getMessage() + ")");
                return null;
            }
        }
    }
}
