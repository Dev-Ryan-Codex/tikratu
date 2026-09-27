package dev.ryan.tikratu.mods.ads;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado contra TikTok oficial 46.4.3 (com.zhiliaoapp.musically), decompilando
 * classes30.dex con jadx: RankData.java hace `this.LLILLL = aweme.isAd();` importando
 * com.ss.android.ugc.aweme.feed.model.Aweme por su nombre real (no ofuscado). Esa clase
 * y ese getter son el punto único donde el feed decide si un Aweme es publicidad.
 */
public class AdBlocker {

    private static final String TARGET_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String TARGET_METHOD = "isAd";

    public void disableFeedAdFlag(DexKitBridge bridge, ClassLoader classLoader) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(false);
            }
        };

        // Camino directo: clase y método confirmados sin ofuscar en 46.4.3.
        try {
            XposedHelpers.findAndHookMethod(TARGET_CLASS, classLoader, TARGET_METHOD, hook);
            ModuleLog.line("(TikRatu | AdBlocker): hooked directo -> " + TARGET_CLASS + "." + TARGET_METHOD + "()");
            return;
        } catch (Throwable directEx) {
            ModuleLog.line("(TikRatu | AdBlocker): reflexion directa fallo (" + directEx.getMessage() + "), probando DexKit...");
        }

        // Fallback para versiones futuras donde ByteDance renombre la clase/método:
        // buscamos en runtime un boolean sin argumentos llamado "isAd" cuya clase
        // siga viviendo bajo un paquete "feed.model" (o similar).
        // NOTA: la firma exacta de MethodMatcher/FindMethod de dexkit 2.0.3 no se
        // verificó compilando (no hay entorno Android SDK en esta máquina) — revisar
        // contra la doc/README de https://github.com/LuckyPray/DexKit antes del primer build.
        try {
            List<MethodData> methods = bridge.findMethod(
                    FindMethod.create().matcher(
                            MethodMatcher.create()
                                    .name(TARGET_METHOD)
                                    .paramCount(0)
                    )
            );

            for (MethodData method : methods) {
                String returnType = String.valueOf(method.getReturnType());
                if (!returnType.contains("boolean")) continue;
                if (!method.getClassName().toLowerCase().contains("feed")) continue;

                try {
                    Method targetMethod = method.getMethodInstance(classLoader);
                    XposedBridge.hookMethod(targetMethod, hook);
                    ModuleLog.line("(TikRatu | AdBlocker): hooked via DexKit -> " +
                            method.getClassName() + "." + method.getName());
                    return;
                } catch (Throwable hookEx) {
                    ModuleLog.line("(TikRatu | AdBlocker): no se pudo hookear " + method.getName() + ": " + hookEx.getMessage());
                }
            }

            ModuleLog.line("(TikRatu | AdBlocker): DexKit no encontro ningun isAd() booleano en feed.model");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | AdBlocker): excepcion en busqueda DexKit: " + t.getMessage());
        }
    }
}
