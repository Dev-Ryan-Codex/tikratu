# TikRatu

Módulo Xposed/LSPosed para la app **oficial** de TikTok (`com.zhiliaoapp.musically`). No parchea, no re-firma ni redistribuye el APK — corre como hook en runtime sobre la instalación oficial del usuario, en un dispositivo con root + LSPosed.

Inspirado en [InstaEclipse](https://github.com/ReSo7200/InstaEclipse) (módulo standalone + [DexKit](https://github.com/LuckyPray/DexKit) para ubicar métodos en runtime sin depender de nombres ofuscados fijos) y en [WaEnhancer](https://github.com/Dev4Mod/WaEnhancer) (UI de la companion app: categorías con pantallas de preferencias, cada feature con su propio switch).

Ver [DISCLAIMER.md](DISCLAIMER.md) antes de usarlo.

## Por qué existe

Nace del análisis de un mod de terceros para TikTok (repack cerrado, re-firmado con certificado ajeno, con protección de código nativo tipo VM) hecho como trabajo de la materia **Protección de Software**. La idea de TikRatu es lograr una funcionalidad similar (por ahora: sacar el flag de "esto es un anuncio" del feed) de forma **abierta, auditable y sin tocar el binario de TikTok**, usando la técnica de hooking en runtime en vez de parchear+re-firmar un APK.

## Estado actual

| Feature | Estado |
|---|---|
| Quitar el flag "es anuncio" del feed (`AdBlocker`) | Implementado, hook verificado contra el código real de TikTok **46.4.3 y 47.0.3** (ver abajo) |
| Bloquear Advertising ID / GAID (`AdsIdBlocker`) | Implementado — apunta a `AdvertisingIdClient.Info` de Play Services, no a código interno de TikTok |
| Ocultar el App ID de AdMob (`AdsMetadataBlocker`) | Implementado — intercepta `Bundle.getString("com.google.android.gms.ads.APPLICATION_ID")`, verificado que esa key existe en el manifest de 46.4.3 y 47.0.3 |
| Anular `LocationManager.getLastKnownLocation()` (`LocationBlocker`) | Implementado — no cubre `FusedLocationProviderClient` (API mas moderna de Play Services), ver comentario en el código |
| Descargar video sin marca de agua (`WatermarkBlocker`) | Implementado — verificado contra 46.4.3 y 47.0.3, ver detalle abajo. No cubre fotos ni GIFs todavía |
| Recordatorio local de streak (no auto-envío) | Implementado, en la app companion (`streak/`) — notificación programada, no automatiza nada dentro de TikTok |
| Resto de features del mod original (watermark, duet/stitch, CAPTCHA, region...) | No implementadas todavía — se agregan de a una, siguiendo el mismo patrón (`mods/<categoria>/<Feature>Hook.java`) |

### Por qué NO se replicaron otros hallazgos del mod analizado

El mod original (`C:\Audit\equipos\tiktok apk\README.md`) parcheaba y re-firmaba el APK. TikRatu nunca toca el binario de TikTok, así que varios de sus hallazgos no tienen equivalente acá:

- **Bot de streak silencioso** (auto-envío de mensajes sin interacción): se implementó acotado a un recordatorio, no a automatizar el envío — eso ya es simular actividad falsa a escala.
- **Kill-switch remoto sin permiso**: era una vulnerabilidad/backdoor del mod (cualquier app podía matar TikTok), no una feature a clonar.
- **Borrar el WebView de ByteDance, limpiar dex muertos, cambiar el ícono**: son ediciones estáticas del APK — un hook en runtime no puede borrar archivos ni cambiar el ícono de una app instalada.
- **Ofuscación anti-reversing / posible bypass de firma**: el mod la necesitaba porque re-firmaba TikTok con otro certificado. TikRatu nunca re-firma nada, así que no hay firma que bypassear.

## Cómo funciona el hook de ads

Se decompiló `classes30.dex` del TikTok oficial 46.4.3 (`com.zhiliaoapp.musically`, build apkmirror) con jadx. La clase `com.ss.android.ugc.aweme.commercialize.intelligence.feed.model.RankData` (paquete literalmente "commercialize") hace:

```java
this.LLILLL = aweme.isAd();
```

`aweme` es del tipo `com.ss.android.ugc.aweme.feed.model.Aweme` — el modelo central de un post/video en el feed. **Ni la clase `Aweme` ni el método `isAd()` están ofuscados** (se confirmó buscando el descriptor `Lcom/ss/android/ugc/aweme/feed/model/Aweme;` en los 50 `classes*.dex` del APK original: aparece consistentemente igual en decenas de archivos). Esto es inusual — la mayoría del código de TikTok sí está minificado por R8 (ver los nombres tipo `LLILLL`, `01RT` en el mismo archivo) — probablemente porque `Aweme` se serializa con Gson y esas reglas de "keep" preservan sus miembros.

`AdBlocker.disableFeedAdFlag()` (`app/src/main/java/dev/ryan/tikratu/mods/ads/AdBlocker.java`):

1. **Camino directo**: `XposedHelpers.findAndHookMethod("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader, "isAd", hook)` — funciona mientras ByteDance no renombre esa clase/método.
2. **Fallback con DexKit**: si el camino directo falla (versión futura donde sí lo ofusquen), busca en runtime un método booleano sin argumentos llamado `isAd` dentro de una clase cuyo paquete contenga `feed`, y lo hookea por reflexión. Mismo patrón que usa InstaEclipse para Instagram.

El hook fuerza `param.setResult(false)` — todo el código que consulta `aweme.isAd()` para decidir si mostrar UI/tracking de publicidad pasa a ver siempre "no es un anuncio".

**Re-verificado en TikTok 47.0.3** (build apkmirror más nueva que 46.4.3): mismo patrón, misma clase, mismo método. En 47.0.3 `RankData` vive en `classes5.dex` (no en `classes30.dex` como en 46.4.3 — los números de dex se reordenan entre builds, por eso el hook busca por nombre de clase/método y no por número de archivo) y la línea equivalente es `this.LLJJIII = aweme.isAd();`.

## Otros hooks implementados (verificados igual, contra 46.4.3 y 47.0.3)

- **`AdsIdBlocker`** — hookea `com.google.android.gms.ads.identifier.AdvertisingIdClient$Info` (clase pública de la librería cliente de Play Services, no de TikTok): `getId()` devuelve un UUID cero, `isLimitAdTrackingEnabled()` devuelve `true`.
- **`AdsMetadataBlocker`** — TikTok declara `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" .../>` en su manifest (confirmado en ambas versiones vía `strings64` sobre el `AndroidManifest.xml` crudo). Se hookea `Bundle.getString(String)` a nivel de proceso e intercepta solo esa key -> `null`, así el SDK de Google Mobile Ads no puede inicializarse.
- **`LocationBlocker`** — hookea `LocationManager.getLastKnownLocation(String)` -> `null` (valor de retorno válido según la API, no una excepción). No cubre `FusedLocationProviderClient` todavía (ver comentario en el código: su `getLastLocation()` devuelve un `Task<Location>` ya armado, no se puede anular con un simple `setResult(null)` sin arriesgar un NPE en quien lo consume).

## Hook de watermark en descargas

Se decompiló `com.ss.android.ugc.aweme.feed.model.Video` (clase pública, sin ofuscar, mismo motivo que `Aweme` — Gson) en `classes25.dex` (46.4.3) y `classes3.dex` (47.0.3). El servidor de TikTok manda **dos** URLs de descarga por video en la misma respuesta:

```java
@02s3("download_addr")
public UrlModel downloadAddr;             // getDownloadAddr() -> con marca de agua

@02s3("download_no_watermark_addr")
public UrlModel downloadNoWatermarkAddr;  // getDownloadNoWatermarkAddr() -> sin marca de agua
```

`WatermarkBlocker` hookea `Video.getDownloadAddr()` y, si `getDownloadNoWatermarkAddr()` del mismo objeto no es `null`, reemplaza el resultado por ese valor. No fabrica ninguna URL — solo prioriza el campo que el propio servidor ya manda para este fin. Si un video en particular no trae variante sin marca de agua, el hook no toca nada (se devuelve el resultado original). Verificado idéntico (mismos nombres de clase/campo/getter) en 46.4.3 y 47.0.3.

No cubre fotos ni GIFs (el mod original tenía toggles separados para "Remove Pictures Watermark" / "Remove GIF Watermark") — habría que confirmar si existe un campo `download_no_watermark_addr` equivalente en el modelo de imagen antes de replicarlo ahí.

**Nota de verificación**: no hay entorno Android SDK / emulador / dispositivo en esta máquina, así que el proyecto **no se compiló ni se probó en runtime** todavía. La API exacta de `MethodMatcher`/`FindMethod` de `dexkit:2.0.3` usada en el fallback de `AdBlocker` se escribió según la documentación pública de DexKit, pero hay que confirmarla al abrir el proyecto en Android Studio (primer build) antes de asumir que compila tal cual. Los otros 3 hooks (`AdsIdBlocker`, `AdsMetadataBlocker`, `LocationBlocker`) usan solo la API estándar de Xposed (`XposedHelpers.findAndHookMethod`), sin DexKit.

## Recordatorio de streak (sin auto-envío)

El mod original mandaba un mensaje real dentro de TikTok, sin que el usuario tocara nada (via hooks nativos ofuscados en `libtigrik.so`). TikRatu implementa una versión acotada e independiente en la app companion (`streak/StreakReminderScheduler.java`, `StreakReminderReceiver.java`, `BootReceiver.java`): programa una notificación local a una hora elegida por el usuario, que sobrevive reinicios. No hookea nada de TikTok ni automatiza ninguna interacción — el usuario sigue siendo quien manda el mensaje.

## App companion: categorías + switches

La companion app (lo que ves al abrir el ícono de TikRatu, corriendo en su propio proceso, no dentro de TikTok) tiene:

- **Card de estado**: si LSPosed está cargando el módulo de verdad (ver truco de detección abajo) + **versión de TikTok instalada** (`versionName` + `versionCode` leídos con `PackageManager.getPackageInfo`, requiere declarar `<queries>` en el manifest por las reglas de visibilidad de paquetes de Android 11+).
- **Categoría "Anuncios y tracking"**: switches independientes para `AdBlocker`, `AdsIdBlocker`, `AdsMetadataBlocker`, `LocationBlocker` (`res/xml/prefs_ads.xml`).
- **Categoría "Streak"**: switch de activar/desactivar + selector de hora (`res/xml/prefs_streak.xml`).

Cada switch es un `SwitchPreferenceCompat` estándar de `androidx.preference` — se persiste solo en el archivo de SharedPreferences por defecto de la app. `Module.java` lee ese mismo archivo con `XSharedPreferences` al cargar en el proceso de TikTok, y solo instala el hook si el switch está prendido. **Importante**: como la lectura de prefs pasa una sola vez, al principio de `handleLoadPackage`, tocar un switch requiere **forzar el cierre de TikTok y volver a abrirlo** para que tome efecto (no hay refresco en caliente).

### Truco de "¿el módulo está activo?"

`dev.ryan.tikratu.utils.StatusChecker.isModuleActive()` siempre devuelve `false` en el código fuente. `dev.ryan.tikratu` se agrega a sí mismo como target en `xposedscope` (`arrays.xml`), y cuando LSPosed carga la companion app, `Module.java` hookea ese método puntual para que devuelva `true`. Si la companion app lo llama y ve `true`, es porque LSPosed realmente está activo — es la técnica estándar que usan la mayoría de los módulos Xposed/LSPosed para este chequeo (incluido WaEnhancer).

## Build

1. Abrir la carpeta en Android Studio (usa Gradle 8.10.2 / AGP 8.7.0, iguales a InstaEclipse).
2. `./gradlew assembleDebug` (o el botón Run de Android Studio).
3. Instalar el APK en el celular con LSPosed.
4. Activar el módulo en LSPosed Manager, marcar `com.zhiliaoapp.musically` **y `dev.ryan.tikratu`** en su alcance (el segundo es necesario para el truco de "¿está activo?" de arriba).
5. Abrir TikRatu, confirmar que dice "Módulo activo" y que muestra la versión de TikTok instalada.
6. Configurar los switches que quieras en cada categoría.
7. Forzar el cierre de TikTok y volver a abrirlo para que los hooks tomen los valores actuales.
8. Revisar logs con `adb logcat | grep TikRatu` o desde el visor de logs de LSPosed.

## Estructura

```
app/src/main/java/dev/ryan/tikratu/
├── MainActivity.java                       # status card + versión de TikTok + categorías
├── ui/SettingsActivity.java                 # host de las pantallas de preferencias
├── ui/AdsPreferenceFragment.java            # switches de ads/tracking
├── ui/MediaPreferenceFragment.java          # switch de watermark
├── ui/StreakPreferenceFragment.java         # switch + hora del streak
├── Xposed/Module.java                      # entry point IXposedHookLoadPackage
├── mods/ads/AdBlocker.java                 # isAd() -> false (DexKit + directo)
├── mods/tracking/AdsIdBlocker.java         # Advertising ID -> cero
├── mods/tracking/AdsMetadataBlocker.java   # oculta el AdMob App ID
├── mods/tracking/LocationBlocker.java      # LocationManager -> null
├── mods/media/WatermarkBlocker.java        # getDownloadAddr() -> getDownloadNoWatermarkAddr()
├── streak/StreakReminderScheduler.java     # AlarmManager + SharedPreferences
├── streak/StreakReminderReceiver.java      # dispara la notificación
├── streak/BootReceiver.java                # re-arma el recordatorio tras reiniciar
├── utils/Prefs.java                        # keys de preferencias compartidas UI <-> hooks
├── utils/StatusChecker.java                 # truco de detección "¿está activo?"
├── utils/ModulePackage.java                 # nombre de paquete propio (para XSharedPreferences)
└── utils/log/ModuleLog.java                # wrapper de XposedBridge.log
```

Cada feature nueva va en `mods/<categoria>/<Nombre>Hook.java`, se agrega su switch en el `res/xml/prefs_<categoria>.xml` correspondiente (misma key en `utils/Prefs.java`), y se registra el gating en `Module.java`, igual que las que ya existen.

## Licencia

MIT — ver [LICENSE](LICENSE).
