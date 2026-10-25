package dev.ryan.tikratu.mods.feed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public class FeedFilterBlocker {
    private static final int AWEME_TYPE_LIVE = 101;
    private static final String FEED_API_SERVICE = "com.ss.android.ugc.aweme.feed.FeedApiService";
    private static final String FEED_ITEM_LIST = "com.ss.android.ugc.aweme.feed.model.FeedItemList";
    private static final String SHOP_MARKER = "placeholder_product_id";

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean anyActive() {
        return RuntimeSettings.enabled(Prefs.KEY_HIDE_LIVE, false) || RuntimeSettings.enabled(Prefs.KEY_HIDE_STORY, false) || RuntimeSettings.enabled(Prefs.KEY_HIDE_SHOP, false) || RuntimeSettings.enabled(Prefs.KEY_HIDE_IMAGE, false) || RuntimeSettings.enabled(Prefs.KEY_HIDE_PROMOTED_MUSIC, false) || RuntimeSettings.getLong(Prefs.KEY_MAX_DURATION_SEC, 0L) > 0 || RuntimeSettings.getLong(Prefs.KEY_MIN_VIEWS, 0L) > 0 || RuntimeSettings.getLong(Prefs.KEY_MIN_LIKES, 0L) > 0 || !RuntimeSettings.getString(Prefs.KEY_CAPTION_BLOCKLIST, "").trim().isEmpty() || forceRegionActive();
    }

    private static boolean forceRegionActive() {
        return RuntimeSettings.enabled(Prefs.KEY_FORCE_REGION, false) && !RuntimeSettings.getString(Prefs.KEY_REGION_CODE, "").trim().isEmpty();
    }

    public void block(ClassLoader classLoader) {
        XC_MethodHook filterResult = new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.feed.FeedFilterBlocker.1
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                FeedFilterBlocker.this.filterFeedItemList(param.getResult());
            }
        };
        try {
            Class<?> api = XposedHelpers.findClass(FEED_API_SERVICE, classLoader);
            int n = XposedBridge.hookAllMethods(api, "fetchFeedList", filterResult).size();
            ModuleLog.line("(TikRatu | FeedFilterBlocker): hooked com.ss.android.ugc.aweme.feed.FeedApiService.fetchFeedList (" + n + " metodos)");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): fallo hook fetchFeedList (" + t.getMessage() + ")");
        }
        try {
            XposedHelpers.findAndHookMethod(FEED_ITEM_LIST, classLoader, "getItems", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.feed.FeedFilterBlocker.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    List<Object> filtered;
                    if (FeedFilterBlocker.anyActive()) {
                        FeedFilterBlocker.this.filterFeedItemList(param.thisObject);
                        Object result = param.getResult();
                        if (!(result instanceof List) || (filtered = FeedFilterBlocker.this.filterList((List) result, false)) == null) {
                            return;
                        }
                        param.setResult(filtered);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | FeedFilterBlocker): hooked com.ss.android.ugc.aweme.feed.model.FeedItemList.getItems()");
        } catch (Throwable t2) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): fallo hook getItems (" + t2.getMessage() + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void filterFeedItemList(Object feedItemList) {
        List<Object> filtered;
        if (feedItemList == null || !anyActive()) {
            return;
        }
        try {
            Object raw = XposedHelpers.getObjectField(feedItemList, "items");
            if ((raw instanceof List) && (filtered = filterList((List) raw, true)) != null) {
                XposedHelpers.setObjectField(feedItemList, "items", filtered);
            }
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): error filtrando (" + t.getMessage() + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<Object> filterList(List<?> items, boolean log) {
        List<Object> kept = new ArrayList<>(items.size());
        int removed = 0;
        for (Object item : items) {
            if (item != null && shouldHide(item)) {
                removed++;
            } else {
                kept.add(item);
            }
        }
        if (removed == 0) {
            return null;
        }
        if (log) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): " + removed + " de " + items.size() + " items filtrados");
        }
        return kept;
    }

    private boolean shouldHide(Object aweme) {
        Object stats;
        if (RuntimeSettings.enabled(Prefs.KEY_HIDE_LIVE, false) && isLive(aweme)) {
            return true;
        }
        if (RuntimeSettings.enabled(Prefs.KEY_HIDE_STORY, false) && bool(aweme, "getIsTikTokStory")) {
            return true;
        }
        if (RuntimeSettings.enabled(Prefs.KEY_HIDE_SHOP, false) && isShop(aweme)) {
            return true;
        }
        if (RuntimeSettings.enabled(Prefs.KEY_HIDE_IMAGE, false) && isImage(aweme)) {
            return true;
        }
        if (RuntimeSettings.enabled(Prefs.KEY_HIDE_PROMOTED_MUSIC, false) && bool(aweme, "isWithPromotionalMusic")) {
            return true;
        }
        long maxDurationSec = RuntimeSettings.getLong(Prefs.KEY_MAX_DURATION_SEC, 0L);
        if (maxDurationSec > 0 && isLongerThan(aweme, maxDurationSec)) {
            return true;
        }
        long minViews = RuntimeSettings.getLong(Prefs.KEY_MIN_VIEWS, 0L);
        long minLikes = RuntimeSettings.getLong(Prefs.KEY_MIN_LIKES, 0L);
        if ((minViews > 0 || minLikes > 0) && (stats = call(aweme, "getStatistics")) != null) {
            if (minViews > 0 && num(stats, "getPlayCount") < minViews) {
                return true;
            }
            if (minLikes > 0 && num(stats, "getDiggCount") < minLikes) {
                return true;
            }
        }
        if (forceRegionActive()) {
            String want = RuntimeSettings.getString(Prefs.KEY_REGION_CODE, "").trim();
            Object region = call(aweme, "getRegion");
            if (region instanceof String) {
                String have = ((String) region).trim();
                if (!have.isEmpty() && !have.equalsIgnoreCase(want)) {
                    return true;
                }
            }
        }
        String blocklist = RuntimeSettings.getString(Prefs.KEY_CAPTION_BLOCKLIST, "");
        if (!blocklist.trim().isEmpty()) {
            Object desc = call(aweme, "getDesc");
            if (desc instanceof String) {
                String lower = ((String) desc).toLowerCase(Locale.ROOT);
                for (String w : blocklist.split(",")) {
                    String word = w.trim().toLowerCase(Locale.ROOT);
                    if (!word.isEmpty() && lower.contains(word)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private static boolean isLive(Object aweme) {
        if (num(aweme, "getLiveId") > 0 || bool(aweme, "isLiveReplay")) {
            return true;
        }
        Object liveType = call(aweme, "getLiveType");
        return ((liveType instanceof String) && !((String) liveType).isEmpty()) || num(aweme, "getAwemeType") == 101;
    }

    private static boolean isShop(Object aweme) {
        Object shareUrl = call(aweme, "getShareUrl");
        return (shareUrl instanceof String) && ((String) shareUrl).contains(SHOP_MARKER);
    }

    private static boolean isImage(Object aweme) {
        Object images = call(aweme, "getImageInfos");
        return ((!(images instanceof List) || ((List) images).isEmpty()) && call(aweme, "getPhotoModeImageInfo") == null && call(aweme, "getPhotoModeTextInfo") == null) ? false : true;
    }

    private static boolean isLongerThan(Object aweme, long maxSec) {
        Object video = call(aweme, "getVideo");
        if (video == null) {
            return false;
        }
        long durationMs = num(video, "getDuration");
        return durationMs > 1000 * maxSec;
    }

    private static Object call(Object obj, String method) {
        try {
            return XposedHelpers.callMethod(obj, method, new Object[0]);
        } catch (Throwable th) {
            return null;
        }
    }

    private static boolean bool(Object obj, String method) {
        Object v = call(obj, method);
        return (v instanceof Boolean) && ((Boolean) v).booleanValue();
    }

    private static long num(Object obj, String method) {
        Object v = call(obj, method);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return 0L;
    }
}
