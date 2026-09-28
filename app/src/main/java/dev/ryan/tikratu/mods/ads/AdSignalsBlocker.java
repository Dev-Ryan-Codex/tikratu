package dev.ryan.tikratu.mods.ads;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Complemento de AdBlocker: TikTok tiene MÁS de una señal de "esto es un
 * anuncio", verificado en com.ss.android.ugc.aweme.feed.model.AwemeExtKt
 * (classes5.dex en 47.0.3, presente igual en 46.4.3):
 *
 *   isAdTraffic(aweme)        -> aweme.isAd() || aweme.isSoftAd()
 *   isDescAreaReplaceAd(...)  -> depende de isAdTraffic(aweme)
 *   isTextMoreAd*(...)        -> depende de isAdTraffic(aweme)
 *   isPseudoAd(aweme)         -> independiente, mira getCommerceVideoAuthInfo()
 *   isSearchPreciseAd(aweme)  -> independiente, mira aweme.awemeRawAd
 *
 * Como isAdTraffic() y todo lo que depende de ella solo miran isAd()/isSoftAd(),
 * ya quedan cubiertas hookeando Aweme.isSoftAd() (Aweme.isAd() ya lo hookea
 * AdBlocker). isPseudoAd() e isSearchPreciseAd() son independientes y necesitan
 * su propio hook.
 */
public class AdSignalsBlocker {

    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String AWEME_EXT_CLASS = "com.ss.android.ugc.aweme.feed.model.AwemeExtKt";

    public void block(ClassLoader classLoader) {
        XC_MethodHook returnFalse = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        };

        hookQuiet(classLoader, AWEME_CLASS, "isSoftAd", returnFalse);

        try {
            Class<?> aweme = XposedHelpers.findClass(AWEME_CLASS, classLoader);
            hookQuietStatic(classLoader, AWEME_EXT_CLASS, "isPseudoAd", aweme, returnFalse);
            hookQuietStatic(classLoader, AWEME_EXT_CLASS, "isSearchPreciseAd", aweme, returnFalse);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdSignalsBlocker): no se encontro la clase Aweme (" + t.getMessage() + ")");
        }
    }

    private void hookQuiet(ClassLoader classLoader, String className, String methodName, XC_MethodHook hook) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, hook);
            ModuleLog.line("(TikRatu | AdSignalsBlocker): hooked " + className + "." + methodName + "()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdSignalsBlocker): fallo " + className + "." + methodName + " (" + t.getMessage() + ")");
        }
    }

    private void hookQuietStatic(ClassLoader classLoader, String className, String methodName, Class<?> paramType, XC_MethodHook hook) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, paramType, hook);
            ModuleLog.line("(TikRatu | AdSignalsBlocker): hooked " + className + "." + methodName + "(Aweme)");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdSignalsBlocker): fallo " + className + "." + methodName + " (" + t.getMessage() + ")");
        }
    }
}
