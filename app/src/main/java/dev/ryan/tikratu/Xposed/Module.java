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
import dev.ryan.tikratu.mods.region.RegionBlocker;
import dev.ryan.tikratu.mods.ui.FontStyleBlocker;
import dev.ryan.tikratu.utils.ModulePackage;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.log.ModuleLog;

public class Module implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TARGET_PACKAGE = "com.zhiliaoapp.musically";

    private static String moduleNativeLibDir;
    private static boolean dexkitNativeLoaded;

    /**
     * Ruta absoluta al APK de este módulo en disco. La usa FontStyleBlocker
     * para cargar la fuente embebida (assets/fonts/ios.ttf) desde dentro del
     * proceso de TikTok vía un AssetManager que apunta a nuestro propio APK.
     */
    public static volatile String modulePath;

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
        modulePath = startupParam.modulePath;
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
        // Estado leido EN VIVO: los hooks se instalan SIEMPRE (una vez) y cada
        // uno consulta RuntimeSettings en cada disparo. Asi prender/apagar un
        // toggle aplica sin reiniciar TikTok (con ~2s de retraso, lo que tarda
        // el refresh del cache). Antes cada hook se instalaba condicionalmente
        // y el cambio no tomaba efecto hasta reiniciar.
        RuntimeSettings.init(context);
        ClassLoader cl = lpparam.classLoader;

        new AdBlocker().block(cl);
        new AdsIdBlocker().block(cl);
        new AdsMetadataBlocker().block(cl);
        new LocationBlocker().block(cl);
        new WatermarkBlocker().block(cl);
        new PhotoWatermarkBlocker().block(cl);
        new DownloadUnlockBlocker().block(cl);
        new AdSignalsBlocker().block(cl);
        new UrlSanitizerBlocker().block(cl);
        new FontStyleBlocker().block(cl);
        new FeedFilterBlocker().block(cl);
        new StopVideoLoopingBlocker().block(cl);
        new ScreenCaptureBlocker().block(cl);
        new DisableLoginBlocker().block(cl);
        new RegionBlocker().block(cl);
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
