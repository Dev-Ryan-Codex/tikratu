package dev.ryan.tikratu.mods.media;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * A diferencia de los demás hooks, este NO apunta a ninguna clase de TikTok
 * (que podría cambiar de nombre entre versiones): hookea
 * android.content.ClipboardManager.setPrimaryClip(ClipData), la API pública
 * de Android que TikTok usa para poner el link en el portapapeles cuando el
 * usuario toca "Copiar enlace". Mismo criterio de riesgo que
 * AdsMetadataBlocker (hookea una API de la plataforma, no código propio de
 * TikTok) — estable independientemente de cómo TikTok ofusque su código.
 *
 * Parámetros de tracking confirmados en classes_47 (grep sobre el dex, no
 * inventados): el propio código de TikTok usa este regex para detectar
 * params de tracking en URLs de terceros (classes12.dex):
 *   (^utm_|^sub[0-9]+$|^subid[0-9]?$|^ttclid$
 * A eso se suman `_r`, `enter_from_merge`, `enter_method`,
 * `share_from_user_id`, vistos en un query string real de share
 * (classes2.dex: "&_r=1&enter_from_merge=share&enter_method=share&share_from_user_id=").
 *
 * Solo toca el texto copiado si contiene una URL Y esa URL tiene al menos
 * uno de estos params — si no, no modifica nada (no interfiere con copiar
 * texto normal, comentarios, nombres de usuario, etc.).
 */
public class UrlSanitizerBlocker {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern TRACKING_PARAM = Pattern.compile(
            "^(utm_[a-zA-Z_]+|_r|ttclid|enter_from_merge|enter_method|share_from_user_id|sub\\d+|subid\\d*)$");

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(ClipboardManager.class, "setPrimaryClip",
                    ClipData.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!RuntimeSettings.enabled(Prefs.KEY_URL_SANITIZER_BLOCKER, Prefs.DEFAULT_URL_SANITIZER_BLOCKER)) return;
                            ClipData clip = (ClipData) param.args[0];
                            if (clip == null || clip.getItemCount() == 0) return;
                            CharSequence original = clip.getItemAt(0).getText();
                            if (original == null) return;
                            String sanitized = sanitize(original.toString());
                            if (!sanitized.equals(original.toString())) {
                                CharSequence label = clip.getDescription() != null
                                        ? clip.getDescription().getLabel() : "text";
                                param.args[0] = ClipData.newPlainText(label, sanitized);
                                ModuleLog.line("(TikRatu | UrlSanitizerBlocker): link sanitizado en el portapapeles");
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | UrlSanitizerBlocker): hooked ClipboardManager.setPrimaryClip()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | UrlSanitizerBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }

    private static String sanitize(String text) {
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        int last = 0;
        boolean changed = false;
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            String cleaned = stripTrackingParams(url);
            if (!cleaned.equals(url)) changed = true;
            result.append(text, last, urlMatcher.start()).append(cleaned);
            last = urlMatcher.end();
        }
        result.append(text.substring(last));
        return changed ? result.toString() : text;
    }

    private static String stripTrackingParams(String url) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) return url;
        String base = url.substring(0, queryStart);
        String query = url.substring(queryStart + 1);
        StringBuilder kept = new StringBuilder();
        for (String param : query.split("&")) {
            if (param.isEmpty()) continue;
            String key = param.contains("=") ? param.substring(0, param.indexOf('=')) : param;
            if (TRACKING_PARAM.matcher(key).matches()) continue;
            if (kept.length() > 0) kept.append('&');
            kept.append(param);
        }
        return kept.length() > 0 ? base + "?" + kept : base;
    }
}
