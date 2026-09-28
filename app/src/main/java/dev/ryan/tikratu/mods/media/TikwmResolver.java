package dev.ryan.tikratu.mods.media;

import org.json.JSONArray;
import org.json.JSONObject;

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

import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Resuelve un video/foto de TikTok a través de la API pública de tikwm.com —
 * un servicio de terceros gratuito (no operado por este proyecto) que hace su
 * propia extracción del lado del servidor. Usa Aweme.getShareUrl() (campo
 * público sin ofuscar, ver Aweme.java) como input, igual que cualquier
 * downloader externo (pegar el link en una web/app de terceros) — la
 * diferencia es que acá se llama automáticamente en el momento en que el
 * usuario toca "Descargar" dentro de la propia TikTok oficial.
 *
 * Por qué existe: WatermarkBlocker/PhotoWatermarkBlocker (swap a
 * download_no_watermark_addr / displayImageNoWatermark, campos que el propio
 * servidor de TikTok manda) están confirmados rotos en dispositivo real —
 * ambos campos devuelven contenido con marca de agua para esta
 * cuenta/versión (ver README, sección "Pruebas en dispositivo real"). Este
 * resolver es la única vía encontrada que sí entrega un archivo limpio,
 * porque tikwm.com hace su propia llamada server-to-server a la API de
 * TikTok con sus propias credenciales, no depende de qué le sirve el
 * backend de TikTok a ESTA sesión/cuenta.
 *
 * Tradeoff explícito (documentado en README): esto manda el shareUrl del
 * video que el usuario decide descargar a un servidor de terceros gratuito
 * y sin SLA — puede fallar, cambiar de formato, o discontinuarse en
 * cualquier momento. Por eso: timeout corto, nunca lanza, y si falla
 * devuelve null para que el caller haga fallback al comportamiento anterior
 * (swap a download_no_watermark_addr).
 */
final class TikwmResolver {

    private static final String API = "https://www.tikwm.com/api/";
    private static final String UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";
    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int CACHE_MAX = 50;

    // Cache simple por shareUrl para no repetir la llamada de red si
    // getDownloadAddr() se invoca más de una vez para el mismo post.
    private static final Map<String, Result> CACHE = new LinkedHashMap<String, Result>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
            return size() > CACHE_MAX;
        }
    };

    static final class Result {
        final String hdVideoUrl;   // puede ser null
        final String playVideoUrl; // puede ser null
        final List<String> images; // puede ser vacía

        Result(String hd, String play, List<String> images) {
            this.hdVideoUrl = hd;
            this.playVideoUrl = play;
            this.images = images;
        }

        String bestVideoUrl() {
            return hdVideoUrl != null ? hdVideoUrl : playVideoUrl;
        }
    }

    private TikwmResolver() {
    }

    /** Llamada de red bloqueante — el caller decide en qué hilo correr esto. */
    static synchronized Result resolve(String shareUrl) {
        if (shareUrl == null || shareUrl.isEmpty()) return null;

        Result cached = CACHE.get(shareUrl);
        if (cached != null) return cached;

        try {
            String query = "url=" + URLEncoder.encode(shareUrl, "UTF-8") + "&hd=1";
            URL url = new URL(API + "?" + query);
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
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }

            Result result = parse(body.toString());
            if (result != null) CACHE.put(shareUrl, result);
            return result;
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | TikwmResolver): fallo la resolucion (" + t.getMessage() + ")");
            return null;
        }
    }

    private static Result parse(String body) {
        try {
            JSONObject json = new JSONObject(body);
            if (json.optInt("code", -1) != 0) {
                ModuleLog.line("(TikRatu | TikwmResolver): respuesta con error: " + json.optString("msg", "?"));
                return null;
            }
            JSONObject data = json.optJSONObject("data");
            if (data == null) return null;

            String hd = data.optString("hdplay", "");
            String play = data.optString("play", "");

            List<String> images = new ArrayList<>();
            JSONArray imagesArr = data.optJSONArray("images");
            if (imagesArr != null) {
                for (int i = 0; i < imagesArr.length(); i++) {
                    String u = imagesArr.optString(i, "");
                    if (!u.isEmpty()) images.add(u);
                }
            }

            if (hd.isEmpty() && play.isEmpty() && images.isEmpty()) return null;
            return new Result(hd.isEmpty() ? null : hd, play.isEmpty() ? null : play, images);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | TikwmResolver): respuesta no parseable (" + t.getMessage() + ")");
            return null;
        }
    }
}
