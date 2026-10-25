package dev.ryan.tikratu.mods.media;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.Xposed.RuntimeSettings;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;
import java.util.List;

/* JADX INFO: loaded from: classes7.dex */
public class PhotoWatermarkBlocker {
    private static final String IMAGE_INFO_CLASS = "com.ss.android.ugc.aweme.feed.model.PhotoModeImageInfo";

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(IMAGE_INFO_CLASS, classLoader, "getImageList", new Object[]{new XC_MethodHook() { // from class: dev.ryan.tikratu.mods.media.PhotoWatermarkBlocker.1
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    Object clean;
                    if (RuntimeSettings.enabled(Prefs.KEY_PHOTO_WATERMARK_BLOCKER, true)) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            for (Object item : (List) result) {
                                if (item != null && (clean = XposedHelpers.getObjectField(item, "displayImageNoWatermark")) != null) {
                                    XposedHelpers.setObjectField(item, "ownerWatermarkImage", clean);
                                    XposedHelpers.setObjectField(item, "userWatermarkImage", clean);
                                }
                            }
                        }
                    }
                }
            }});
            ModuleLog.line("(TikRatu | PhotoWatermarkBlocker): hooked com.ss.android.ugc.aweme.feed.model.PhotoModeImageInfo.getImageList()");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | PhotoWatermarkBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
