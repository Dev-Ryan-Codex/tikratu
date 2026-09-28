package dev.ryan.tikratu.mods.feed;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.MatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.util.List;

import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Sonda de diagnóstico, NO una feature — busca en el APK completo (vía DexKit,
 * no en un árbol decompilado parcial) quién llama realmente a los predicados
 * de tipo de AwemeExtKt (isTikTokStory, isPhotoMode, isImage,
 * isLiveNoDeduplicateClient). El objetivo es encontrar el punto real donde el
 * feed decide qué mostrar/ocultar por tipo de contenido — la pieza de
 * infraestructura que desbloquearía "Quitar historias/directos/Tienda/
 * presentación/recomendaciones" del catálogo del plugin de referencia.
 *
 * grep estático sobre el árbol ya decompilado (all_dex_jadx) no encontró un
 * caller de tipo "filtro de lista" para estos predicados — solo usos de
 * renderizado de UI. Esta sonda usa el motor real de DexKit contra el .dex
 * completo del APK instalado, que es más confiable que un árbol decompilado
 * parcial.
 */
public class FeedFilterProbe {

    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";

    public void probe(DexKitBridge bridge) {
        try {
            MethodMatcher target = MethodMatcher.create()
                    .declaredClass(AWEME_CLASS)
                    .name("getAwemeType");

            List<MethodData> callers = bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().invokeMethods(
                            MethodsMatcher.create().add(target).matchType(MatchType.Contains)
                    )
            ));

            ModuleLog.line("(TikRatu | FeedFilterProbe): getAwemeType -> " + callers.size() + " callers");
            for (MethodData caller : callers) {
                // Solo nos interesan los que además reciben/devuelven una lista o
                // colección (forma típica de un filtro que arma/depura el feed).
                String sign = caller.getMethodSign();
                if (sign != null && (sign.contains("Ljava/util/List") || sign.contains("Ljava/util/Collection")
                        || sign.contains("Ljava/util/Iterator"))) {
                    ModuleLog.line("(TikRatu | FeedFilterProbe):   [LIST] " + caller.getClassName() + "." + caller.getName()
                            + " " + sign);
                }
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FeedFilterProbe): fallo buscando callers de getAwemeType (" + t + ")");
        }
    }
}
