package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* JADX INFO: loaded from: classes7.dex */
public class WatermarkBlocker {
    private static final int ACL_CODE_ALLOWED = 0;
    private static final String ACL_COMMON_SHARE_CLASS = "com.ss.android.ugc.aweme.feed.model.ACLCommonShare";
    private static final int ACL_SHOW_TYPE_ALLOWED = 2;
    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final long RESOLVE_TIMEOUT_MS = 2500;
    private static final int TRANSCODE_NO_WATERMARK = 1;
    private static final String VIDEO_CLASS = "com.ss.android.ugc.aweme.feed.model.Video";
    private static final Map<Object, String> shareUrlByVideo = Collections.synchronizedMap(new WeakHashMap());
    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: dev.ryan.tikratu.mods.media.WatermarkBlocker$$ExternalSyntheticLambda1
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return WatermarkBlocker.lambda$static$0(runnable);
        }
    });

    static /* synthetic */ Thread lambda$static$0(Runnable r) {
        Thread t = new Thread(r, "TikRatu-tikwm");
        t.setDaemon(true);
        return t;
    }

    public void block(ClassLoader classLoader) {
        hookAclConst(classLoader, "getCode", 0);
        hookAclConst(classLoader, "getShowType", 2);
        try {
            XposedHelpers.findAndHookMethod(ACL_COMMON_SHARE_CLASS, classLoader, "getTranscode", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.WatermarkBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_WATERMARK_BLOCKER, true)) {
                        param.setResult(1);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | WatermarkBlocker): hooked com.ss.android.ugc.aweme.feed.model.ACLCommonShare.getTranscode() -> 1");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): no se pudo hookear getTranscode (" + t.getMessage() + ")");
        }
        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "getVideo", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.WatermarkBlocker.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    Object video = param.getResult();
                    if (video == null) {
                        return;
                    }
                    try {
                        Object shareUrl = XposedHelpers.callMethod(param.thisObject, "getShareUrl", new Object[0]);
                        if ((shareUrl instanceof String) && !((String) shareUrl).isEmpty()) {
                            WatermarkBlocker.shareUrlByVideo.put(video, (String) shareUrl);
                        }
                    } catch (Throwable th) {
                    }
                }
            }});
        } catch (Throwable t2) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): no se pudo hookear com.ss.android.ugc.aweme.feed.model.Aweme.getVideo() (" + t2.getMessage() + ")");
        }
        try {
            XposedHelpers.findAndHookMethod(VIDEO_CLASS, classLoader, "getDownloadAddr", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.WatermarkBlocker.3
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_WATERMARK_BLOCKER, true)) {
                        Object original = param.getResult();
                        Object video = param.thisObject;
                        Object newDownloadAddr = WatermarkBlocker.safeCall(video, "getNewDownloadAddr");
                        Object uiAlikeAddr = WatermarkBlocker.safeCall(video, "getUIAlikeDownloadAddr");
                        Object captionDownloadAddr = WatermarkBlocker.safeCall(video, "getCaptionDownloadAddr");
                        Object noWatermarkAddr = WatermarkBlocker.safeCall(video, "getDownloadNoWatermarkAddr");
                        ModuleLog.line("(TikRatu | WatermarkBlocker): candidatos - original=" + WatermarkBlocker.describe(original) + " | new=" + WatermarkBlocker.describe(newDownloadAddr) + " | uiAlike=" + WatermarkBlocker.describe(uiAlikeAddr) + " | caption=" + WatermarkBlocker.describe(captionDownloadAddr) + " | noWatermark=" + WatermarkBlocker.describe(noWatermarkAddr));
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
                        String shareUrl = (String) WatermarkBlocker.shareUrlByVideo.get(video);
                        if (shareUrl != null && original != null) {
                            String cleanUrl = WatermarkBlocker.resolveWithTimeout(shareUrl);
                            if (cleanUrl != null) {
                                try {
                                    XposedHelpers.callMethod(original, "setUrlList", new Object[]{Collections.singletonList(cleanUrl)});
                                    ModuleLog.line("(TikRatu | WatermarkBlocker): URL resuelta via tikwm.com para " + shareUrl + " -> " + cleanUrl);
                                    return;
                                } catch (Throwable t3) {
                                    ModuleLog.line("(TikRatu | WatermarkBlocker): fallo aplicando URL de tikwm (" + t3.getMessage() + ")");
                                }
                            } else {
                                ModuleLog.line("(TikRatu | WatermarkBlocker): tikwm.com no resolvio " + shareUrl + ", fallback a download_no_watermark_addr");
                            }
                        }
                        if (noWatermarkAddr != null) {
                            param.setResult(noWatermarkAddr);
                        }
                    }
                }
            }});
            ModuleLog.line("(TikRatu | WatermarkBlocker): hooked com.ss.android.ugc.aweme.feed.model.Video.getDownloadAddr()");
        } catch (Throwable t3) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): fallo el hook (" + t3.getMessage() + ")");
        }
    }

    private void hookAclConst(ClassLoader classLoader, String method, final int value) {
        try {
            XposedHelpers.findAndHookMethod(ACL_COMMON_SHARE_CLASS, classLoader, method, new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.WatermarkBlocker.4
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (RuntimeSettings.enabled(Prefs.KEY_WATERMARK_BLOCKER, true)) {
                        param.setResult(Integer.valueOf(value));
                    }
                }
            }});
            ModuleLog.line("(TikRatu | WatermarkBlocker): hooked com.ss.android.ugc.aweme.feed.model.ACLCommonShare." + method + "() -> " + value);
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): no se pudo hookear " + method + " (" + t.getMessage() + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Object safeCall(Object obj, String method) {
        try {
            return XposedHelpers.callMethod(obj, method, new Object[0]);
        } catch (Throwable th) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String resolveWithTimeout(final String shareUrl) {
        Future<TikwmResolver.Result> future = NETWORK_EXECUTOR.submit(new Callable() { // from class: dev.ryan.tikratu.mods.media.WatermarkBlocker$$ExternalSyntheticLambda0
            @Override // java.util.concurrent.Callable
            public final Object call() {
                return TikwmResolver.resolve(shareUrl);
            }
        });
        try {
            TikwmResolver.Result result = future.get(RESOLVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (result != null) {
                return result.bestVideoUrl();
            }
            return null;
        } catch (TimeoutException e) {
            future.cancel(true);
            ModuleLog.line("(TikRatu | WatermarkBlocker): tikwm.com no respondio en 2500ms, se corta");
            return null;
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | WatermarkBlocker): error esperando tikwm.com (" + t.getMessage() + ")");
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String describe(Object urlModel) {
        if (urlModel == null) {
            return "null";
        }
        try {
            Object uri = XposedHelpers.callMethod(urlModel, "getUri", new Object[0]);
            Object urlList = XposedHelpers.callMethod(urlModel, "getUrlList", new Object[0]);
            return "uri=" + uri + " urls=" + urlList;
        } catch (Throwable t) {
            return "presente (no se pudo describir: " + t.getMessage() + ")";
        }
    }
}
