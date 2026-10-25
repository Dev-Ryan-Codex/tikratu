package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

/* JADX INFO: loaded from: classes7.dex */
public class DownloadUnlockBlocker {
    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String USER_CLASS = "com.ss.android.ugc.aweme.profile.model.User";
    private static boolean loggedVc;

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(USER_CLASS, classLoader, "isPreventDownload", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.DownloadUnlockBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (DownloadUnlockBlocker.enabled()) {
                        param.setResult(false);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked com.ss.android.ugc.aweme.profile.model.User.isPreventDownload()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo hook User.isPreventDownload (" + t.getMessage() + ")");
        }
        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "isPreventDownload", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.DownloadUnlockBlocker.2
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    if (DownloadUnlockBlocker.enabled()) {
                        param.setResult(false);
                    }
                }
            }});
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked com.ss.android.ugc.aweme.feed.model.Aweme.isPreventDownload()");
        } catch (Throwable t2) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo hook isPreventDownload (" + t2.getMessage() + ")");
        }
        try {
            XposedHelpers.findAndHookMethod(AWEME_CLASS, classLoader, "getVideoControl", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.DownloadUnlockBlocker.3
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    Object vc;
                    if (DownloadUnlockBlocker.enabled() && (vc = param.getResult()) != null) {
                        try {
                            XposedHelpers.setObjectField(vc, "allowDownload", Boolean.TRUE);
                            XposedHelpers.setIntField(vc, "preventDownloadType", 0);
                            if (!DownloadUnlockBlocker.loggedVc) {
                                DownloadUnlockBlocker.loggedVc = true;
                                ModuleLog.line("(TikRatu | DownloadUnlockBlocker): VideoControl.allowDownload=true, preventDownloadType=0");
                            }
                        } catch (Throwable t3) {
                            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): no se pudo parchear VideoControl (" + t3.getMessage() + ")");
                        }
                    }
                }
            }});
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): hooked com.ss.android.ugc.aweme.feed.model.Aweme.getVideoControl()");
        } catch (Throwable t3) {
            ModuleLog.line("(TikRatu | DownloadUnlockBlocker): fallo hook getVideoControl (" + t3.getMessage() + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean enabled() {
        return RuntimeSettings.enabled(Prefs.KEY_DOWNLOAD_UNLOCK_BLOCKER, true);
    }
}
