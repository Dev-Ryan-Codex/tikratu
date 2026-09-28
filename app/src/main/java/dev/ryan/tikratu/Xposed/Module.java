package dev.ryan.tikratu.Xposed;

import java.io.File;

import org.luckypray.dexkit.DexKitBridge;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dev.ryan.tikratu.mods.ads.AdBlocker;
import dev.ryan.tikratu.mods.ads.AdSignalsBlocker;
import dev.ryan.tikratu.mods.feed.FeedFilterBlocker;
import dev.ryan.tikratu.mods.interaction.StopVideoLoopingBlocker;
import dev.ryan.tikratu.mods.interaction.ScreenCaptureBlocker;
import dev.ryan.tikratu.mods.interaction.DisableLoginBlocker;
import dev.ryan.tikratu.mods.tracking.AdsIdBlocker;
import dev.ryan.tikratu.mods.tracking.AdsMetadataBlocker;
import dev.ryan.tikratu.mods.tracking.LocationBlocker;
import dev.ryan.tikratu.mods.media.DownloadUnlockBlocker;
import dev.ryan.tikratu.mods.media.PhotoWatermarkBlocker;
import dev.ryan.tikratu.mods.media.UrlSanitizerBlocker;
import dev.ryan.tikratu.mods.media.WatermarkBlocker;
import dev.ryan.tikratu.mods.ui.FontStyleBlocker;
import dev.ryan.tikratu.utils.ModulePackage;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

public class Module implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";

    private static String moduleNativeLibDir;
    private static boolean dexkitNativeLoaded;

    /**
     * Corre una sola vez, muy temprano, antes de que Zygote forkee ningun
     * proceso de app. startupParam.modulePath es la ruta absoluta al APK de
     * ESTE MISMO modulo en disco (dev.ryan.tikratu) — es la unica forma
     * confiable de encontrar el .so de DexKit cuando este codigo termina
     * corriendo INYECTADO dentro del proceso de TikTok (donde el classloader
     * de Xposed no resuelve automaticamente la carpeta de libs nativas de
     * nuestro propio paquete). Mismo patron que usa InstaEclipse para lo mismo.
     */
    @Override
    public void initZygote(StartupParam startupParam) {
        String apkDir = new File(startupParam.modulePath).getParent();
        // No asumir que la carpeta se llama igual que Build.SUPPORTED_ABIS[0]
        // ("arm64-v8a"): verificado en dispositivo real (LineageOS, KernelSU Next)
        // que PackageManager extrae las libs nativas bajo un nombre ABREVIADO
        // (lib/arm64/, no lib/arm64-v8a/). Se lista el directorio real en vez
        // de adivinar el nombre.
        File libDir = new File(apkDir, "lib");
        File[] abiDirs = libDir.listFiles(File::isDirectory);
        if (abiDirs != null && abiDirs.length > 0) {
            moduleNativeLibDir = abiDirs[0].getAbsolutePath();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (ModulePackage.NAME.equals(lpparam.packageName)) {
            hookSelfStatusCheck(lpparam);
            return;
        }

        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        ModuleLog.line("(TikRatu): cargando en " + lpparam.packageName + " (proceso " + lpparam.processName + ")");

        // ModulePrefsReader necesita un Context real (AndroidAppHelper.
        // currentApplication()) para consultar PrefsProvider via Binder IPC.
        // Confirmado en dispositivo real: en handleLoadPackage ese Context
        // TODAVIA no existe (currentApplication() devuelve null aqui mismo).
        // El primer punto garantizado donde SI existe es Application.attach(),
        // que Android llama con el Context base antes de Application.onCreate() —
        // se difiere toda la logica de instalacion de hooks hasta ahi.
        XposedHelpers.findAndHookMethod(android.app.Application.class, "attach", android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        installHooks(lpparam, (android.content.Context) param.thisObject);
                    }
                });
    }

    private void installHooks(XC_LoadPackage.LoadPackageParam lpparam, android.content.Context context) {
        ModulePrefsReader prefs = new ModulePrefsReader(context);

        if (prefs.getBoolean(Prefs.KEY_ADS_ID_BLOCKER, Prefs.DEFAULT_ADS_ID_BLOCKER)) {
            new AdsIdBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_LOCATION_BLOCKER, Prefs.DEFAULT_LOCATION_BLOCKER)) {
            new LocationBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_ADS_METADATA_BLOCKER, Prefs.DEFAULT_ADS_METADATA_BLOCKER)) {
            new AdsMetadataBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_WATERMARK_BLOCKER, Prefs.DEFAULT_WATERMARK_BLOCKER)) {
            new WatermarkBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_PHOTO_WATERMARK_BLOCKER, Prefs.DEFAULT_PHOTO_WATERMARK_BLOCKER)) {
            new PhotoWatermarkBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_DOWNLOAD_UNLOCK_BLOCKER, Prefs.DEFAULT_DOWNLOAD_UNLOCK_BLOCKER)) {
            new DownloadUnlockBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_AD_SIGNALS_BLOCKER, Prefs.DEFAULT_AD_SIGNALS_BLOCKER)) {
            new AdSignalsBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_URL_SANITIZER_BLOCKER, Prefs.DEFAULT_URL_SANITIZER_BLOCKER)) {
            new UrlSanitizerBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_FONT_STYLE_BLOCKER, Prefs.DEFAULT_FONT_STYLE_BLOCKER)) {
            new FontStyleBlocker().block(lpparam.classLoader);
        }

        FeedFilterBlocker feedFilter = new FeedFilterBlocker(
                prefs.getBoolean(Prefs.KEY_HIDE_LIVE, Prefs.DEFAULT_HIDE_LIVE),
                prefs.getBoolean(Prefs.KEY_HIDE_STORY, Prefs.DEFAULT_HIDE_STORY),
                prefs.getBoolean(Prefs.KEY_HIDE_SHOP, Prefs.DEFAULT_HIDE_SHOP),
                prefs.getBoolean(Prefs.KEY_HIDE_IMAGE, Prefs.DEFAULT_HIDE_IMAGE),
                prefs.getBoolean(Prefs.KEY_HIDE_PROMOTED_MUSIC, Prefs.DEFAULT_HIDE_PROMOTED_MUSIC),
                prefs.getLong(Prefs.KEY_MAX_DURATION_SEC, Prefs.DEFAULT_MAX_DURATION_SEC),
                prefs.getLong(Prefs.KEY_MIN_VIEWS, Prefs.DEFAULT_MIN_VIEWS),
                prefs.getLong(Prefs.KEY_MIN_LIKES, Prefs.DEFAULT_MIN_LIKES),
                prefs.getString(Prefs.KEY_CAPTION_BLOCKLIST, Prefs.DEFAULT_CAPTION_BLOCKLIST));
        if (feedFilter.isActive()) {
            feedFilter.block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_STOP_VIDEO_LOOPING, Prefs.DEFAULT_STOP_VIDEO_LOOPING)) {
            new StopVideoLoopingBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_SCREEN_CAPTURE_BLOCKER, Prefs.DEFAULT_SCREEN_CAPTURE_BLOCKER)) {
            new ScreenCaptureBlocker().block(lpparam.classLoader);
        }
        if (prefs.getBoolean(Prefs.KEY_DISABLE_LOGIN, Prefs.DEFAULT_DISABLE_LOGIN)) {
            new DisableLoginBlocker().block(lpparam.classLoader);
        }

        if (prefs.getBoolean(Prefs.KEY_AD_BLOCKER, Prefs.DEFAULT_AD_BLOCKER)) {
            try {
                loadDexKitNativeLibrary();
                try (DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir)) {
                    new AdBlocker().disableFeedAdFlag(bridge, lpparam.classLoader);
                    // Proximos hooks (watermark, duet/stitch, etc.) se agregan aca,
                    // uno por clase en dev.ryan.tikratu.mods.<categoria>, igual que AdBlocker.
                }
            } catch (Throwable t) {
                ModuleLog.line("(TikRatu): error inicializando DexKit: " + t.getMessage());
            }
        }
    }

    private static synchronized void loadDexKitNativeLibrary() {
        if (dexkitNativeLoaded) return;
        if (moduleNativeLibDir == null) {
            throw new IllegalStateException("moduleNativeLibDir es null — initZygote no corrio (¿el modulo no esta en xposedscope de si mismo?)");
        }
        System.load(moduleNativeLibDir + "/libdexkit.so");
        dexkitNativeLoaded = true;
        ModuleLog.line("(TikRatu): libdexkit.so cargado desde " + moduleNativeLibDir);
    }

    /**
     * Truco de "¿esta activo?": hookeamos StatusChecker.isModuleActive() para que
     * devuelva true SOLO cuando este mismo codigo corre via LSPosed dentro del
     * proceso de la propia companion app (dev.ryan.tikratu esta en xposedscope,
     * ver arrays.xml). Si LSPosed no esta activo, el metodo original (que
     * siempre devuelve false) queda sin hookear y la companion app lo detecta.
     */
    private void hookSelfStatusCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "dev.ryan.tikratu.utils.StatusChecker", lpparam.classLoader,
                    "isModuleActive", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu): no se pudo instalar el self-check (" + t.getMessage() + ")");
        }
    }
}
