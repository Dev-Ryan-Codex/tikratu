# TikRatu

Módulo Xposed/LSPosed para la app **oficial** de TikTok (`com.zhiliaoapp.musically`). No parchea, no re-firma ni redistribuye el APK — corre como hook en runtime sobre la instalación oficial del usuario, en un dispositivo con root + LSPosed.

Inspirado en [InstaEclipse](https://github.com/ReSo7200/InstaEclipse) (módulo standalone + [DexKit](https://github.com/LuckyPray/DexKit) para ubicar métodos en runtime sin depender de nombres ofuscados fijos) y en [WaEnhancer](https://github.com/Dev4Mod/WaEnhancer) (UI de la companion app: categorías con pantallas de preferencias, cada feature con su propio switch).

Ver [DISCLAIMER.md](DISCLAIMER.md) antes de usarlo.

## Por qué existe

Nace del análisis de un mod de terceros para TikTok (repack cerrado, re-firmado con certificado ajeno, con protección de código nativo tipo VM) hecho como trabajo de la materia **Protección de Software**. La idea de TikRatu es lograr una funcionalidad similar (por ahora: sacar el flag de "esto es un anuncio" del feed) de forma **abierta, auditable y sin tocar el binario de TikTok**, usando la técnica de hooking en runtime en vez de parchear+re-firmar un APK.

## Estado actual

Columna "Estado real" = confirmado con logs de un dispositivo real (Android 16 / LineageOS + KernelSU Next + LSPosed), no solo análisis estático. Ver §"Pruebas en dispositivo real" para el detalle de cada uno.

| Feature | Estado real (dispositivo) |
|---|---|
| Quitar el flag "es anuncio" del feed (`AdBlocker`) | 🔴 **Roto** — DexKit nunca inicializa (`UnsatisfiedLinkError` en su librería nativa), el hook ni se instala |
| Bloquear Advertising ID / GAID (`AdsIdBlocker`) | 🟡 No-op en este dispositivo (LineageOS sin Play Services con esa librería) — sin verificar en un device con Play Services real |
| Ocultar el App ID de AdMob (`AdsMetadataBlocker`) | 🔴 **Roto** — el hook falla al instalarse (`Bundle#getString` no hookeable en este build/Android 16) |
| Anular `LocationManager.getLastKnownLocation()` (`LocationBlocker`) | 🟢 Se instala correctamente (no se verificó el efecto en runtime, solo la instalación del hook) |
| Descargar video sin marca de agua (`WatermarkBlocker`) | 🔴 **No logra el efecto** — el hook dispara y reemplaza la URL correctamente, pero el archivo descargado sigue con la marca de agua (ver detalle abajo, es un problema del lado del servidor de TikTok, no del hook) |
| Descargar foto (slideshow) sin marca de agua (`PhotoWatermarkBlocker`) | ⚪ Se instala correctamente — no probado todavía con un post de foto real |
| Marca de agua en GIFs | No implementado — el mecanismo es distinto (dibujado por el cliente, no una URL alternativa del servidor) |
| Recordatorio local de streak (no auto-envío) | ⚪ No probado en este dispositivo todavía |
| Resto de features del mod original (duet/stitch, CAPTCHA, region...) | No implementadas todavía — se agregan de a una, siguiendo el mismo patrón (`mods/<categoria>/<Feature>Hook.java`) |

## Pruebas en dispositivo real (2026-09-27)

Primera vez que el módulo se probó en un teléfono real (Android 16, LineageOS, KernelSU Next + LSPosed, sin Google Play Services), conectado por ADB para poder leer logcat en vivo, tomar screenshots, y automatizar taps con `uiautomator`/`input tap` durante la sesión.

**El síntoma reportado ("falla al descargar videos/historias") NO era un crash.** Se armó la hipótesis inicial de que el reinicio de TikTok a su `SplashActivity` justo después de tocar "Descargar" era un crash causado por `WatermarkBlocker`/`PhotoWatermarkBlocker`. Se descartó con evidencia:

- Logcat completo capturado durante el evento: **cero** `FATAL EXCEPTION`, cero eventos de "Process ... died".
- El archivo de video **se guardó igual** en `/sdcard/DCIM/Camera/` (se confirmó bajándolo por `adb pull` y extrayendo un frame con `ffmpeg`).
- Conclusión: el "reinicio a Splash" es la propia `Activity` de TikTok reconstruyéndose al volver del selector de compartir (comportamiento normal de Android bajo presión de memoria/recreación de Activity), no una falla de este módulo.

**El problema real, confirmado con logging agregado a `WatermarkBlocker`**: el hook se instala, se dispara, y reemplaza `getDownloadAddr()` por `getDownloadNoWatermarkAddr()` correctamente (se ve en el log que esta última no es `null` y apunta a una URL distinta — el endpoint normal de reproducción interna `api16-normal-c-*.tiktokv.com/aweme/v1/play/`, sin `watermark=1&logo_name=tiktok` en la query, a diferencia del original). **Pese a eso, el archivo final descargado sigue con la marca de agua incrustada en los píxeles.** Esto no se puede arreglar cambiando el hook: es TikTok (del lado del servidor) sirviendo contenido marcado por ambas URLs, al menos para esta cuenta/región/versión. El campo `download_no_watermark_addr` puede haber dejado de significar lo que su nombre sugiere.

De paso, esta sesión de pruebas destapó **dos hooks que directamente no funcionan** en este dispositivo, no relacionados con el reporte original:

- **`AdBlocker` nunca se activa**: `DexKitBridge.create()` tira `UnsatisfiedLinkError` (`No implementation found for ... nativeInitDexKit`) — la librería nativa de DexKit no está cargando en el APK de debug compilado por CI. Pendiente de investigar el empaquetado (probablemente falta configurar `ndk.abiFilters`/`jniLibs` en `app/build.gradle`, o el AAR de dexkit no se está mergeando bien).
- **`AdsMetadataBlocker` falla al instalarse**: `XposedHelpers.findAndHookMethod` sobre `Bundle#getString(String)` tira error en este Android 16 — puede que ART haya empezado a tratar ese método de forma especial (demasiado caliente/inlineable) y LSPosed no pueda hookearlo ahí.

**Herramientas instaladas en esta máquina durante la sesión** (quedan disponibles para la próxima): `adb` (Android Platform Tools, vía winget) y `ffmpeg` (para extraer frames de video y verificar visualmente el resultado de los downloads).

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

## Hook de watermark en fotos (slideshow) — y por qué no cubre GIFs

Se decompiló `com.ss.android.ugc.aweme.feed.model.PhotoModeImageUrlModel` (`classes7.dex` en 47.0.3, `classes2.dex` en 46.4.3) — el modelo de los posts de foto/slideshow. Tiene tres campos análogos a los de `Video`, pero **sin getters**:

```java
@02s3("display_image")          UrlModel displayImageNoWatermark;  // sin marca
@02s3("owner_watermark_image")  UrlModel ownerWatermarkImage;      // con marca del autor
@02s3("user_watermark_image")   UrlModel userWatermarkImage;       // con marca del usuario que descarga
```

Gson asigna estos campos por reflexión directa sobre el campo público (no hay `getDisplayImageNoWatermark()` que hookear), así que no alcanza con hookear un getter simple como en `Video`.

**Se ubicó el punto real de guardado/compartir** (decompilando `classes24.dex`): una clase auto-generada por R8 en el paquete `X` (algo como `X.0oOF`, el nombre cambia en cada build — no sirve como target estable) arma la lista de fotos llamando a `PhotoModeImageInfo.getImageList()` y ahí, según un flag interno (`C0oOH.LIZ`, también inestable), decide leer `displayImageNoWatermark` o `userWatermarkImage`/`ownerWatermarkImage`.

En vez de perseguir esas clases inestables, `PhotoWatermarkBlocker` hookea **`PhotoModeImageInfo.getImageList()`** — un getter público, sin ofuscar, confirmado idéntico en 46.4.3 y 47.0.3 (mismo motivo de estabilidad que `Aweme`/`Video`: campos serializados con Gson). Cada vez que se pide la lista de fotos del post —sea cual sea el código que la llamó, y sin importar si el objeto se pobló por JSON o por protobuf— se recorre la lista y se iguala `ownerWatermarkImage`/`userWatermarkImage` a `displayImageNoWatermark` en cada item. Esto cubre **ambos** caminos de parseo con un solo hook, a diferencia del primer intento (que solo tocaba el conversor protobuf).

**GIFs**: no tiene un campo `..._no_watermark_addr` equivalente — encontramos evidencia (`drawWatermarkToCover`, `getImageWatermarkPath`) de que la marca de agua en GIFs se **dibuja del lado del cliente** (probablemente al renderizar los frames del GIF a partir de un clip de video), no es una URL alternativa que el servidor ya manda. Hookear eso sin verificar bien el punto exacto arriesga corromper visualmente la exportación — se dejó afuera en vez de adivinar.

**Nota de verificación**: no hay entorno Android SDK / emulador / dispositivo en esta máquina, así que el proyecto **no se compiló ni se probó en runtime** todavía. La API exacta de `MethodMatcher`/`FindMethod` de `dexkit:2.0.3` usada en el fallback de `AdBlocker` se escribió según la documentación pública de DexKit, pero hay que confirmarla al abrir el proyecto en Android Studio (primer build) antes de asumir que compila tal cual. Los otros 3 hooks (`AdsIdBlocker`, `AdsMetadataBlocker`, `LocationBlocker`) usan solo la API estándar de Xposed (`XposedHelpers.findAndHookMethod`), sin DexKit.

## Recordatorio de streak (sin auto-envío)

El mod original mandaba un mensaje real dentro de TikTok, sin que el usuario tocara nada (via hooks nativos ofuscados en `libtigrik.so`). TikRatu implementa una versión acotada e independiente en la app companion (`streak/StreakReminderScheduler.java`, `StreakReminderReceiver.java`, `BootReceiver.java`): programa una notificación local a una hora elegida por el usuario, que sobrevive reinicios. No hookea nada de TikTok ni automatiza ninguna interacción — el usuario sigue siendo quien manda el mensaje.

## App companion: UI

Rediseñada calcando el estilo real de **InstaEclipse 0.7.0** (se decodificó el APK con apktool para copiar su sistema de color M3 dark-only, el estilo de card plano `cardElevation=0dp` + `strokeWidth=0dp`, y la fila de feature icono+título+`MaterialSwitch` de `item_feature.xml`/`item_feature_header.xml`) — no es un preset de `androidx.preference` genérico. InstaEclipse es Apache-2.0, así que el patrón de diseño (no su ícono ni su nombre) se reutiliza legítimamente, con paleta propia:

- **Tema dark-only** (`themes.xml`/`colors.xml`): misma rampa neutra de superficies que InstaEclipse (`#17171b` → `#303038`), pero con **primary cian** (`#25F4EE`) y **secondary rojo/rosa** (`#ff8ea3`) de TikTok en vez de su violeta, más un **tertiary dorado** para la sección streak.
- **Ícono propio** (`ic_launcher_foreground.xml`): un escudo (protección) en la misma técnica de desfase cromático cian/rojo que usa el logo de TikTok, pero con forma de escudo (no la nota musical) — ícono adaptativo, sin PNGs.
- **`MainActivity`**: card de estado (`colorPrimaryContainer`, esquinas 20dp, sin elevación) con si LSPosed está cargando el módulo de verdad (ver truco de detección abajo) + **versión de TikTok instalada** (`versionName`/`versionCode` vía `PackageManager.getPackageInfo`, requiere `<queries>` en el manifest por las reglas de visibilidad de paquetes de Android 11+), botón "Abrir TikTok" y botón "Ver funciones", más una card "Cómo usarlo".
- **`FeaturesActivity`**: una sola pantalla con **todos** los switches agrupados por header de sección (Anuncios y tracking / Media / Streak) — mismo patrón que la pantalla "Features" real de InstaEclipse (una lista con secciones, no una pantalla separada por categoría). Implementado a mano con `RecyclerView` + `FeatureAdapter` (`ui/FeatureItem.java`, `ui/FeatureAdapter.java`) en vez de `PreferenceFragmentCompat`, para poder calcar el look exacto de `item_feature.xml` (card plana, ícono 32dp, `MaterialSwitch`).

Cada switch persiste en el archivo de SharedPreferences por defecto de la app (`utils/AppPrefs.java`, mismo nombre de archivo que usaba antes `PreferenceManager.getDefaultSharedPreferences` — se sacó esa dependencia, ya no hace falta). `Module.java` lee ese mismo archivo con `XSharedPreferences` al cargar en el proceso de TikTok, y solo instala el hook si el switch está prendido. **Importante**: como la lectura de prefs pasa una sola vez, al principio de `handleLoadPackage`, tocar un switch requiere **forzar el cierre de TikTok y volver a abrirlo** para que tome efecto (no hay refresco en caliente).

### Truco de "¿el módulo está activo?"

`dev.ryan.tikratu.utils.StatusChecker.isModuleActive()` siempre devuelve `false` en el código fuente. `dev.ryan.tikratu` se agrega a sí mismo como target en `xposedscope` (`arrays.xml`), y cuando LSPosed carga la companion app, `Module.java` hookea ese método puntual para que devuelva `true`. Si la companion app lo llama y ve `true`, es porque LSPosed realmente está activo — es la técnica estándar que usan la mayoría de los módulos Xposed/LSPosed para este chequeo (incluido WaEnhancer).

## Build

1. Abrir la carpeta en Android Studio (usa Gradle 8.10.2 / AGP 8.7.0, iguales a InstaEclipse).
2. `./gradlew assembleDebug` (o el botón Run de Android Studio).
3. Instalar el APK en el celular con LSPosed.
4. Activar el módulo en LSPosed Manager, marcar `com.zhiliaoapp.musically` **y `dev.ryan.tikratu`** en su alcance (el segundo es necesario para el truco de "¿está activo?" de arriba).
5. Abrir TikRatu, confirmar que dice "Módulo activo" y que muestra la versión de TikTok instalada.
6. Tocar "Ver funciones" y configurar los switches que quieras.
7. Forzar el cierre de TikTok y volver a abrirlo para que los hooks tomen los valores actuales.
8. Revisar logs con `adb logcat | grep TikRatu` o desde el visor de logs de LSPosed.

## Estructura

```
app/src/main/java/dev/ryan/tikratu/
├── MainActivity.java                       # status card + versión de TikTok + "Abrir TikTok"/"Ver funciones"
├── ui/FeaturesActivity.java                # única pantalla con todos los switches (secciones con headers)
├── ui/FeatureItem.java                     # modelo (header / switch / action) para la lista
├── ui/FeatureAdapter.java                  # RecyclerView.Adapter, calca item_feature.xml de InstaEclipse
├── Xposed/Module.java                      # entry point IXposedHookLoadPackage
├── mods/ads/AdBlocker.java                 # isAd() -> false (DexKit + directo)
├── mods/tracking/AdsIdBlocker.java         # Advertising ID -> cero
├── mods/tracking/AdsMetadataBlocker.java   # oculta el AdMob App ID
├── mods/tracking/LocationBlocker.java      # LocationManager -> null
├── mods/media/WatermarkBlocker.java        # getDownloadAddr() -> getDownloadNoWatermarkAddr()
├── mods/media/PhotoWatermarkBlocker.java    # PhotoModeImageUrlModel: camino protobuf y JSON
├── streak/StreakReminderScheduler.java     # AlarmManager + SharedPreferences
├── streak/StreakReminderReceiver.java      # dispara la notificación
├── streak/BootReceiver.java                # re-arma el recordatorio tras reiniciar
├── utils/Prefs.java                        # keys de preferencias compartidas UI <-> hooks
├── utils/AppPrefs.java                      # SharedPreferences por defecto (sin depender de androidx.preference)
├── utils/StatusChecker.java                 # truco de detección "¿está activo?"
├── utils/ModulePackage.java                 # nombre de paquete propio (para XSharedPreferences)
└── utils/log/ModuleLog.java                # wrapper de XposedBridge.log
```

Cada feature nueva va en `mods/<categoria>/<Nombre>Hook.java`, se agrega un `FeatureItem.toggle(...)` en `FeaturesActivity.buildItems()` bajo el header que corresponda (misma key en `utils/Prefs.java`), y se registra el gating en `Module.java`, igual que las que ya existen.

## Licencia

MIT — ver [LICENSE](LICENSE).
