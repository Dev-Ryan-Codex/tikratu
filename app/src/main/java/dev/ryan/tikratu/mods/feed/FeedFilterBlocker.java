package dev.ryan.tikratu.mods.feed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Filtro de feed: saca items de la lista que TikTok recibe del servidor,
 * ANTES de que llegue al RecyclerView — no fuerza ningun discriminador de
 * tipo global (isPhotoMode, getIsTikTokStory, etc.), asi que no cambia como
 * se renderiza el contenido que si se muestra.
 *
 * Punto de hook tomado del parche "Feed filter" de
 * github.com/icysymmetra/tiktok-patches-for-morphe (fork de ReVanced),
 * verificado en TikTok 47.0.3 (nombres sin ofuscar):
 *   - com.ss.android.ugc.aweme.feed.FeedApiService.fetchFeedList(1 param)
 *     -> devuelve com.ss.android.ugc.aweme.feed.model.FeedItemList
 *   - FeedItemList.items (List de Aweme) / FeedItemList.getItems()
 * Se filtra en ambos: la respuesta de red (fetchFeedList) y la lectura
 * (getItems), porque TikTok tambien arma el feed desde cache sin volver a
 * pasar por fetchFeedList.
 *
 * Criterios (misma logica que los filtros de icysymmetra, getters
 * verificados en Aweme.java/Video.java/AwemeStatistics.java de 47.0.3):
 *   - LIVE: getLiveId() > 0 || isLiveReplay() || getLiveType() no vacio
 *           || getAwemeType() == 101
 *   - Historias: getIsTikTokStory()
 *   - Tienda: getShareUrl() contiene "placeholder_product_id"
 *   - Fotos/presentacion: getImageInfos() no vacio
 *           || getPhotoModeImageInfo() != null || getPhotoModeTextInfo() != null
 *   - Largos: getVideo().getDuration() > maxDurationSec — getDuration()
 *           devuelve el campo JSON "duration" (videoLength); se ASUME en
 *           milisegundos (convencion de la API de TikTok), no verificado
 *           todavia con un video de duracion conocida
 *   - Vistas/likes: getStatistics().getPlayCount()/getDiggCount() < minimo
 *   - Descripcion: getDesc() contiene alguna palabra de la lista
 */
public class FeedFilterBlocker {

    private static final String FEED_API_SERVICE = "com.ss.android.ugc.aweme.feed.FeedApiService";
    private static final String FEED_ITEM_LIST = "com.ss.android.ugc.aweme.feed.model.FeedItemList";
    private static final int AWEME_TYPE_LIVE = 101;
    private static final String SHOP_MARKER = "placeholder_product_id";

    private final boolean hideLive;
    private final boolean hideStory;
    private final boolean hideShop;
    private final boolean hideImage;
    private final boolean hidePromotedMusic;
    private final long maxDurationSec;
    private final long minViews;
    private final long minLikes;
    private final List<String> blockWords = new ArrayList<>();

    public FeedFilterBlocker(boolean hideLive, boolean hideStory, boolean hideShop, boolean hideImage,
                             boolean hidePromotedMusic, long maxDurationSec, long minViews, long minLikes,
                             String captionBlocklist) {
        this.hideLive = hideLive;
        this.hideStory = hideStory;
        this.hideShop = hideShop;
        this.hideImage = hideImage;
        this.hidePromotedMusic = hidePromotedMusic;
        this.maxDurationSec = maxDurationSec;
        this.minViews = minViews;
        this.minLikes = minLikes;
        if (captionBlocklist != null) {
            for (String word : captionBlocklist.split(",")) {
                String w = word.trim().toLowerCase(Locale.ROOT);
                if (!w.isEmpty()) blockWords.add(w);
            }
        }
    }

    public boolean isActive() {
        return hideLive || hideStory || hideShop || hideImage || hidePromotedMusic
                || maxDurationSec > 0 || minViews > 0 || minLikes > 0 || !blockWords.isEmpty();
    }

    public void block(ClassLoader classLoader) {
        XC_MethodHook filterResult = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                filterFeedItemList(param.getResult());
            }
        };

        try {
            Class<?> api = XposedHelpers.findClass(FEED_API_SERVICE, classLoader);
            int n = XposedBridge.hookAllMethods(api, "fetchFeedList", filterResult).size();
            ModuleLog.line("(TikRatu | FeedFilterBlocker): hooked " + FEED_API_SERVICE + ".fetchFeedList (" + n + " metodos)");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): fallo hook fetchFeedList (" + t.getMessage() + ")");
        }

        try {
            // getItems() a veces devuelve una COPIA de this.items (verificado en
            // FeedItemList.java de 47.0.3) — se filtra el valor devuelto, no se
            // reemplaza por el campo, para no cambiar esa semantica.
            XposedHelpers.findAndHookMethod(FEED_ITEM_LIST, classLoader, "getItems", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    // Confirmado en dispositivo real: getItems() se llama decenas de
                    // veces por lote. Se limpia el campo de origen una vez (las
                    // llamadas siguientes ya no encuentran nada que sacar) y se
                    // filtra tambien el valor devuelto por si ya era una copia.
                    filterFeedItemList(param.thisObject);
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<Object> filtered = filterList((List<?>) result, false);
                        if (filtered != null) param.setResult(filtered);
                    }
                }
            });
            ModuleLog.line("(TikRatu | FeedFilterBlocker): hooked " + FEED_ITEM_LIST + ".getItems()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): fallo hook getItems (" + t.getMessage() + ")");
        }
    }

    private void filterFeedItemList(Object feedItemList) {
        if (feedItemList == null) return;
        try {
            Object raw = XposedHelpers.getObjectField(feedItemList, "items");
            if (!(raw instanceof List)) return;
            List<Object> filtered = filterList((List<?>) raw, true);
            if (filtered != null) XposedHelpers.setObjectField(feedItemList, "items", filtered);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): error filtrando (" + t.getMessage() + ")");
        }
    }

    /** Devuelve una lista nueva sin los items a ocultar, o null si no hay nada que sacar. */
    private List<Object> filterList(List<?> items, boolean log) {
        List<Object> kept = new ArrayList<>(items.size());
        int removed = 0;
        for (Object item : items) {
            if (item != null && shouldHide(item)) {
                removed++;
            } else {
                kept.add(item);
            }
        }
        if (removed == 0) return null;
        if (log) {
            ModuleLog.line("(TikRatu | FeedFilterBlocker): " + removed + " de " + items.size() + " items filtrados");
        }
        return kept;
    }

    private boolean shouldHide(Object aweme) {
        if (hideLive && isLive(aweme)) return true;
        if (hideStory && bool(aweme, "getIsTikTokStory")) return true;
        if (hideShop && isShop(aweme)) return true;
        if (hideImage && isImage(aweme)) return true;
        if (hidePromotedMusic && bool(aweme, "isWithPromotionalMusic")) return true;
        if (maxDurationSec > 0 && isLongerThan(aweme, maxDurationSec)) return true;
        if (minViews > 0 || minLikes > 0) {
            Object stats = call(aweme, "getStatistics");
            if (stats != null) {
                if (minViews > 0 && num(stats, "getPlayCount") < minViews) return true;
                if (minLikes > 0 && num(stats, "getDiggCount") < minLikes) return true;
            }
        }
        if (!blockWords.isEmpty()) {
            Object desc = call(aweme, "getDesc");
            if (desc instanceof String) {
                String lower = ((String) desc).toLowerCase(Locale.ROOT);
                for (String w : blockWords) {
                    if (lower.contains(w)) return true;
                }
            }
        }
        return false;
    }

    private static boolean isLive(Object aweme) {
        if (num(aweme, "getLiveId") > 0) return true;
        if (bool(aweme, "isLiveReplay")) return true;
        Object liveType = call(aweme, "getLiveType");
        if (liveType instanceof String && !((String) liveType).isEmpty()) return true;
        return num(aweme, "getAwemeType") == AWEME_TYPE_LIVE;
    }

    private static boolean isShop(Object aweme) {
        Object shareUrl = call(aweme, "getShareUrl");
        return shareUrl instanceof String && ((String) shareUrl).contains(SHOP_MARKER);
    }

    private static boolean isImage(Object aweme) {
        Object images = call(aweme, "getImageInfos");
        if (images instanceof List && !((List<?>) images).isEmpty()) return true;
        return call(aweme, "getPhotoModeImageInfo") != null || call(aweme, "getPhotoModeTextInfo") != null;
    }

    private static boolean isLongerThan(Object aweme, long maxSec) {
        Object video = call(aweme, "getVideo");
        if (video == null) return false;
        long durationMs = num(video, "getDuration");
        return durationMs > maxSec * 1000L;
    }

    private static Object call(Object obj, String method) {
        try {
            return XposedHelpers.callMethod(obj, method);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean bool(Object obj, String method) {
        Object v = call(obj, method);
        return v instanceof Boolean && (Boolean) v;
    }

    private static long num(Object obj, String method) {
        Object v = call(obj, method);
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }
}
