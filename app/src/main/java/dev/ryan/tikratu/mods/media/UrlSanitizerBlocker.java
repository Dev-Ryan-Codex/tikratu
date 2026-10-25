package dev.ryan.tikratu.mods.media;

import android.content.ClipData;
import android.content.ClipboardManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kotlin.text.Typography;

/* JADX INFO: loaded from: classes7.dex */
public class UrlSanitizerBlocker {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern TRACKING_PARAM = Pattern.compile("^(utm_[a-zA-Z_]+|_r|ttclid|enter_from_merge|enter_method|share_from_user_id|sub\\d+|subid\\d*)$");

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(ClipboardManager.class, "setPrimaryClip", new Object[]{ClipData.class, new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.UrlSanitizerBlocker.1
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    ClipData clip;
                    CharSequence original;
                    if (!RuntimeSettings.enabled(Prefs.KEY_URL_SANITIZER_BLOCKER, true) || (clip = (ClipData) param.args[0]) == null || clip.getItemCount() == 0 || (original = clip.getItemAt(0).getText()) == null) {
                        return;
                    }
                    String sanitized = UrlSanitizerBlocker.sanitize(original.toString());
                    if (!sanitized.equals(original.toString())) {
                        CharSequence label = clip.getDescription() != null ? clip.getDescription().getLabel() : "text";
                        param.args[0] = ClipData.newPlainText(label, sanitized);
                        ModuleLog.line("(TikRatu | UrlSanitizerBlocker): link sanitizado en el portapapeles");
                    }
                }
            }});
            ModuleLog.line("(TikRatu | UrlSanitizerBlocker): hooked ClipboardManager.setPrimaryClip()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | UrlSanitizerBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String sanitize(String text) {
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        int last = 0;
        boolean changed = false;
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            String cleaned = stripTrackingParams(url);
            if (!cleaned.equals(url)) {
                changed = true;
            }
            result.append((CharSequence) text, last, urlMatcher.start()).append(cleaned);
            last = urlMatcher.end();
        }
        result.append(text.substring(last));
        return changed ? result.toString() : text;
    }

    private static String stripTrackingParams(String url) {
        int queryStart = url.indexOf(63);
        if (queryStart < 0) {
            return url;
        }
        String base = url.substring(0, queryStart);
        String query = url.substring(queryStart + 1);
        StringBuilder kept = new StringBuilder();
        for (String param : query.split("&")) {
            if (!param.isEmpty()) {
                String key = param.contains("=") ? param.substring(0, param.indexOf(61)) : param;
                if (!TRACKING_PARAM.matcher(key).matches()) {
                    if (kept.length() > 0) {
                        kept.append(Typography.amp);
                    }
                    kept.append(param);
                }
            }
        }
        return kept.length() > 0 ? base + "?" + ((Object) kept) : base;
    }
}
