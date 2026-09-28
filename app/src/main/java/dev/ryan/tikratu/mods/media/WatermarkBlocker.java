package dev.ryan.tikratu.mods.media;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Verificado contra TikTok oficial 46.4.3 y 47.0.3 (com.zhiliaoapp.musically),
 * decompilando con jadx la clase com.ss.android.ugc.aweme.feed.model.Video
 * (classes25.dex en 46.4.3, classes3.dex en 47.0.3 — ni la clase ni sus
 * getters están ofuscados, mismo motivo que Aweme.isAd(): son campos Gson).
 *
 * El servidor de TikTok ya manda DOS URLs de descarga por video en la misma
 * respuesta:
 *   - download_addr             -> getDownloadAddr()             (con marca de agua)
 *   - download_no_watermark_addr -> getDownloadNoWatermarkAddr()  (sin marca de agua)
 *
 * Este hook no fabrica ninguna URL nueva: solo hace que getDownloadAddr()
 * devuelva el valor de getDownloadNoWatermarkAddr() cuando éste no es null.
 * Si el server no mandó variante sin marca de agua para ese video en particular,
 * se devuelve el resultado original sin tocar nada (no rompe la descarga).
 *
 * Cubre el flujo de descarga/guardado de VIDEO. No cubre fotos ni GIFs
 * (el mod original tenía toggles separados para eso) — pendiente si se
 * confirma un campo equivalente en el modelo de imagen.
 *
 * DIAGNÓSTICO CONFIRMADO EN DISPOSITIVO REAL (2026-09-27, TikTok 47.0.3):
 * el hook SÍ se dispara y SÍ reemplaza el resultado (se confirmó con logs
 * que download_no_watermark_addr no es null y apunta a una URL distinta,
 * del endpoint normal de reproducción interna
 * api16-normal-c-*.tiktokv.com/aweme/v1/play/ en vez del endpoint de
 * descarga con &watermark=1&logo_name=tiktok en la query). Pese a eso, el
 * archivo de video guardado en el dispositivo SIGUE teniendo la marca de
 * agua incrustada en los píxeles.
 *
 * Conclusión: no es un bug de este hook — es que, al menos para esta
 * cuenta/región/versión, el contenido que sirve ByteDance en
 * download_no_watermark_addr YA NO es realmente una copia limpia (puede
 * que lo haya sido en versiones viejas de TikTok, y que el campo se haya
 * dejado de usar para ese fin sin quitarlo del modelo). No hay URL
 * alternativa del propio servidor que dé un archivo sin marca — swap de
 * campo no alcanza.
 *
 * FIX INTENTADO #1 (2026-09-27): TikwmResolver — resuelve el video vía la
 * API pública de terceros tikwm.com usando Aweme.getShareUrl() (ver
 * TikwmResolver.java). Confirmado en dispositivo real (2 videos, 2 cuentas
 * distintas, una de ellas un creador personal sin repost) que TAMPOCO
 * produce un archivo sin marca hoy — se deja activo como intento
 * best-effort con fallback seguro, pero sin expectativa real.
 *
 * FIX INTENTADO #2 (2026-09-27): re-decompilando Video.java se encontraron
 * TRES campos de descarga más, nunca antes probados, con getters públicos
 * sin ofuscar (misma razón Gson que los demás):
 *   - new_download_addr        -> getNewDownloadAddr()
 *   - ui_alike_download_addr   -> getUIAlikeDownloadAddr()
 *   - caption_download_addr    -> getCaptionDownloadAddr()
 * (Se descubrieron comparando el modelo protobuf equivalente de Video,
 * clase X.C05OL en classes3.dex de TikTok 47.0.3, que además del par ya
 * conocido tiene download_suffix_logo_addr/has_download_suffix_logo_addr
 * y misc_download_addrs — estos dos últimos NO tienen equivalente Gson en
 * Video.java, no son alcanzables desde este hook.)
 * Se prueban en cascada, en este orden: newDownloadAddr (el nombre sugiere
 * que reemplazó al viejo mecanismo) -> uiAlikeAddr -> captionDownloadAddr
 * -> tikwm.com -> downloadNoWatermarkAddr (último fallback, sabido roto).
 * El hook loguea uri+urlList de cada candidato antes de elegir, para poder
 * diagnosticar en logcat cuál (si alguno) sirve contenido limpio.
 *
 * MECANISMO PRINCIPAL (importado 2026-09-28 de gnadgnaoh/SexAlloy vía
 * mentalblank/Tiktok-Revanced): ademas de todo lo anterior, se fuerza
 * ACLCommonShare.getTranscode() -> 1. Ese campo (default 3) es la señal
 * con la que TikTok decide si el archivo descargado lleva marca; es un
 * mecanismo DISTINTO al swap de URL de arriba, y el que usa el modulo
 * Xposed de referencia. Pendiente de confirmar el efecto real en el
 * archivo descargado (los swaps de URL ya se probaron sin exito).
 */
public class WatermarkBlocker {

    private static final String VIDEO_CLASS = "com.ss.android.ugc.aweme.feed.model.Video";
    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";

    // Video -> shareUrl del Aweme dueño de ese Video. Se llena en
    // Aweme.getVideo() (que se llama en muchos lugares del feed, no solo al
    // descargar) y se consulta reactivamente en getDownloadAddr() (que sí
    // confirmamos en dispositivo real que solo se llama al tocar Descargar).
    private static final Map<Object, String> shareUrlByVideo = Collections.synchronizedMap(new WeakHashMap<>());

    // getDownloadAddr() se confirmó en dispositivo real que corre en el hilo
    // PRINCIPAL de TikTok (log: "main=true"). La llamada de red a tikwm.com no
    // puede correr ahí directo (Android tira NetworkOnMainThreadException, y
    // aunque no la tirara, bloquear el hilo principal arriesga un ANR). Se
    // ejecuta en un hilo aparte con timeout total corto (bien por debajo del
    // umbral de detección de ANR de Android para eventos táctiles, ~5s): si
    // no llega a tiempo, se corta y se hace fallback sin haber bloqueado la
    // UI de forma perceptible.
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TikRatu-tikwm");
        t.setDaemon(true);
        return t;
    });
    private static final long RESOLVE_TIMEOUT_MS = 2500;

    private static final String ACL_COMMON_SHARE_CLASS = "com.ss.android.ugc.aweme.feed.model.ACLCommonShare";
    private static final int TRANSCODE_NO_WATERMARK = 1;

    public void block(ClassLoader classLoader) {
        // Mecanismo nuevo, importado del modulo Xposed gnadgnaoh/SexAlloy
        // (via mentalblank/Tiktok-Revanced): TikTok decide si el archivo
        // descargado lleva marca de agua segun ACLCommonShare.getTranscode()
        // (campo "transcode", default 3). Forzarlo a 1 (sin marca) es el punto
        // que usa ese modulo. Clase/campo/metodo confirmados sin ofuscar en
        // 47.0.3 (ACLCommonShare.java: "public int transcode = 3", getTranscode()).
        try {
            XposedHelpers.findAndHookMethod(ACL_COMMON_SHARE_CLASS, classLoader, "getTranscode",
                    XC_MethodReplacement.returnConstant(TRANSCODE_NO_WATERMARK));
            ModuleLog.line("(TikRatu | WatermarkBlocker): hooked " + ACL_COMMON_SHARE_CLASS + ".getTranscode() -> " + TRANSCODE_NO_WATERMARK);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): no se pudo hookear getTranscode (" + t.getMessage() + ")");
        }

        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "getVideo",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object video = param.getResult();
                            if (video == null) return;
                            try {
                                Object shareUrl = XposedHelpers.callMethod(param.thisObject, "getShareUrl");
                                if (shareUrl instanceof String && !((String) shareUrl).isEmpty()) {
                                    shareUrlByVideo.put(video, (String) shareUrl);
                                }
                            } catch (Throwable ignored) {
                                // Aweme sin shareUrl (raro) - no rompe nada, solo no habilita tikwm para este item.
                            }
                        }
                    });
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): no se pudo hookear " + AWEME_CLASS + ".getVideo() (" + t.getMessage() + ")");
        }

        try {
            XposedHelpers.findAndHookMethod(VIDEO_CLASS, classLoader, "getDownloadAddr",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object original = param.getResult();
                            Object video = param.thisObject;

                            Object newDownloadAddr = safeCall(video, "getNewDownloadAddr");
                            Object uiAlikeAddr = safeCall(video, "getUIAlikeDownloadAddr");
                            Object captionDownloadAddr = safeCall(video, "getCaptionDownloadAddr");
                            Object noWatermarkAddr = safeCall(video, "getDownloadNoWatermarkAddr");

                            ModuleLog.line("(TikRatu | WatermarkBlocker): candidatos - original=" + describe(original)
                                    + " | new=" + describe(newDownloadAddr)
                                    + " | uiAlike=" + describe(uiAlikeAddr)
                                    + " | caption=" + describe(captionDownloadAddr)
                                    + " | noWatermark=" + describe(noWatermarkAddr));

                            if (newDownloadAddr != null) {
                                ModuleLog.line("(TikRatu | WatermarkBlocker): probando newDownloadAddr");
                                param.setResult(newDownloadAddr);
                                return;
                            }
                            if (uiAlikeAddr != null) {
                                ModuleLog.line("(TikRatu | WatermarkBlocker): probando uiAlikeAddr (newDownloadAddr era null)");
                                param.setResult(uiAlikeAddr);
                                return;
                            }
                            if (captionDownloadAddr != null) {
                                ModuleLog.line("(TikRatu | WatermarkBlocker): probando captionDownloadAddr (new/uiAlike eran null)");
                                param.setResult(captionDownloadAddr);
                                return;
                            }

                            String shareUrl = shareUrlByVideo.get(video);
                            if (shareUrl != null && original != null) {
                                String cleanUrl = resolveWithTimeout(shareUrl);
                                if (cleanUrl != null) {
                                    try {
                                        XposedHelpers.callMethod(original, "setUrlList", Collections.singletonList(cleanUrl));
                                        ModuleLog.line("(TikRatu | WatermarkBlocker): URL resuelta via tikwm.com para "
                                                + shareUrl + " -> " + cleanUrl);
                                        return;
                                    } catch (Throwable t) {
                                        ModuleLog.line("(TikRatu | WatermarkBlocker): fallo aplicando URL de tikwm (" + t.getMessage() + ")");
                                    }
                                } else {
                                    ModuleLog.line("(TikRatu | WatermarkBlocker): tikwm.com no resolvio " + shareUrl + ", fallback a download_no_watermark_addr");
                                }
                            }

                            if (noWatermarkAddr != null) {
                                param.setResult(noWatermarkAddr);
                            }
                        }
                    });
            ModuleLog.line("(TikRatu | WatermarkBlocker): hooked " + VIDEO_CLASS + ".getDownloadAddr()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }

    private static Object safeCall(Object obj, String method) {
        try {
            return XposedHelpers.callMethod(obj, method);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String resolveWithTimeout(String shareUrl) {
        Future<TikwmResolver.Result> future = NETWORK_EXECUTOR.submit((Callable<TikwmResolver.Result>) () -> TikwmResolver.resolve(shareUrl));
        try {
            TikwmResolver.Result result = future.get(RESOLVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return result != null ? result.bestVideoUrl() : null;
        } catch (TimeoutException e) {
            future.cancel(true);
            ModuleLog.line("(TikRatu | WatermarkBlocker): tikwm.com no respondio en " + RESOLVE_TIMEOUT_MS + "ms, se corta");
            return null;
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): error esperando tikwm.com (" + t.getMessage() + ")");
            return null;
        }
    }

    private static String describe(Object urlModel) {
        if (urlModel == null) return "null";
        try {
            Object uri = XposedHelpers.callMethod(urlModel, "getUri");
            Object urlList = XposedHelpers.callMethod(urlModel, "getUrlList");
            return "uri=" + uri + " urls=" + urlList;
        } catch (Throwable t) {
            return "presente (no se pudo describir: " + t.getMessage() + ")";
        }
    }
}
