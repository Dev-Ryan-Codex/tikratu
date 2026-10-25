package dev.ryan.tikratu.mods.media;

import androidx.core.app.NotificationCompat;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes7.dex */
final class TikwmResolver {
    private static final String API = "https://www.tikwm.com/api/";
    private static final Map<String, Result> CACHE = new LinkedHashMap<String, Result>(16, 0.75f, true) { // from class: dev.ryan.tikratu.mods.media.TikwmResolver.1
        @Override // java.util.LinkedHashMap
        protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
            return size() > 50;
        }
    };
    private static final int CACHE_MAX = 50;
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final String UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";

    static final class Result {
        final String hdVideoUrl;
        final List<String> images;
        final String playVideoUrl;

        Result(String hd, String play, List<String> images) {
            this.hdVideoUrl = hd;
            this.playVideoUrl = play;
            this.images = images;
        }

        String bestVideoUrl() {
            return this.hdVideoUrl != null ? this.hdVideoUrl : this.playVideoUrl;
        }
    }

    private TikwmResolver() {
    }

    static synchronized Result resolve(String shareUrl) {
        if (shareUrl != null) {
            if (!shareUrl.isEmpty()) {
                Result cached = CACHE.get(shareUrl);
                if (cached != null) {
                    return cached;
                }
                try {
                    String query = "url=" + URLEncoder.encode(shareUrl, "UTF-8") + "&hd=1";
                    URL url = new URL("https://www.tikwm.com/api/?" + query);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                    conn.setReadTimeout(READ_TIMEOUT_MS);
                    conn.setRequestProperty("User-Agent", UA);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        ModuleLog.line("(TikRatu | TikwmResolver): HTTP " + code);
                        return null;
                    }
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            body.append(line);
                        }
                    }
                    Result result = parse(body.toString());
                    if (result != null) {
                        CACHE.put(shareUrl, result);
                    }
                    return result;
                } catch (Throwable t) {
                    ModuleLog.line("(TikRatu | TikwmResolver): fallo la resolucion (" + t.getMessage() + ")");
                    return null;
                }
            }
        }
        return null;
    }

    private static Result parse(String body) {
        try {
            JSONObject json = new JSONObject(body);
            if (json.optInt("code", -1) != 0) {
                ModuleLog.line("(TikRatu | TikwmResolver): respuesta con error: " + json.optString(NotificationCompat.CATEGORY_MESSAGE, "?"));
                return null;
            }
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                return null;
            }
            String hd = data.optString("hdplay", "");
            String play = data.optString("play", "");
            List<String> images = new ArrayList<>();
            JSONArray imagesArr = data.optJSONArray("images");
            if (imagesArr != null) {
                for (int i = 0; i < imagesArr.length(); i++) {
                    String u = imagesArr.optString(i, "");
                    if (!u.isEmpty()) {
                        images.add(u);
                    }
                }
            }
            if (hd.isEmpty() && play.isEmpty() && images.isEmpty()) {
                return null;
            }
            return new Result(hd.isEmpty() ? null : hd, play.isEmpty() ? null : play, images);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | TikwmResolver): respuesta no parseable (" + t.getMessage() + ")");
            return null;
        }
    }
}
