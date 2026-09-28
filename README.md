# TikRatu

Módulo Xposed/LSPosed para la app **oficial** de TikTok (`com.zhiliaoapp.musically`). No parchea, no re-firma ni redistribuye el APK — corre como hook en runtime sobre la instalación oficial del usuario, en un dispositivo con root + LSPosed.

Inspirado en [InstaEclipse](https://github.com/ReSo7200/InstaEclipse) (módulo standalone + [DexKit](https://github.com/LuckyPray/DexKit) para ubicar métodos en runtime sin depender de nombres ofuscados fijos) y en [WaEnhancer](https://github.com/Dev4Mod/WaEnhancer) (UI de la companion app: categorías con pantallas de preferencias, cada feature con su propio switch).

Ver [DISCLAIMER.md](DISCLAIMER.md) antes de usarlo.

## Por qué existe

Nace del análisis de un mod de terceros para TikTok (repack cerrado, re-firmado con certificado ajeno, con protección de código nativo tipo VM) hecho como trabajo de la materia **Protección de Software**. La idea de TikRatu es lograr una funcionalidad similar (por ahora: sacar el flag de "esto es un anuncio" del feed) de forma **abierta, auditable y sin tocar el binario de TikTok**, usando la técnica de hooking en runtime en vez de parchear+re-firmar un APK.

## Fix de fondo: los toggles no llegaban a TikTok (2026-09-28)

Probando el primer toggle con default `false` se descubrió que **ningún toggle llegaba de verdad al proceso de TikTok** — los de default `true` solo "funcionaban" por coincidencia con el fallback. Cadena de causas, todas confirmadas en el dispositivo:

1. `Context.getSharedPreferences()` nunca creaba `shared_prefs/*.xml` en este SO (listado real de `dataDir`: solo `cache/`, `code_cache/`, `files/`) → `XSharedPreferences` no tenía archivo que leer.
2. Un archivo propio (`files/tikratu_prefs.properties`, world-readable, verificado con `run-as`) tampoco sirve: TikTok recibe `ENOENT` — aislamiento de namespace de montaje por app, no permisos.
3. Solución: `PrefsProvider` (ContentProvider, Binder IPC). `AndroidAppHelper.currentApplication()` es `null` en `handleLoadPackage` e incluso dentro de `Application.attach()`, así que la instalación de hooks se difiere a `Application.attach()` y se usa `param.thisObject` como Context.

Confirmado en logcat: `prefs leidas via PrefsProvider`.

## Filtro de feed (2026-09-28)

Punto de hook tomado del parche "Feed filter" de [icysymmetra/tiktok-patches-for-morphe](https://github.com/icysymmetra/tiktok-patches-for-morphe) (fork de ReVanced, vía [mentalblank/Tiktok-Revanced](https://github.com/mentalblank/Tiktok-Revanced)), verificado en 47.0.3 decompilando `classes4.dex`: `FeedApiService.fetchFeedList(...)` → `FeedItemList` (`public List<Aweme> items`, `getItems()`). `FeedFilterBlocker` saca items de esa lista antes del RecyclerView (no fuerza discriminadores de tipo globales). Criterios: directos, historias, Tienda, fotos/presentación, duración máxima (ms: ASUMIDO), mínimo de vistas, mínimo de likes, lista de palabras en la descripción.

Confirmado en dispositivo con "Quitar directos" activo: `FeedFilterBlocker: 2 de 9 items filtrados`.

## Catálogo completo del plugin de referencia (capturas 2026-09-27)

Del "TikTok Plugin" de las capturas (`C:\Audit\equipos\tiktok apk\img tiktok plugin`), catalogado íntegro y clasificado:

**Ya implementadas en TikRatu** (nombre real de la captura -> feature nuestra):
- Eliminar anuncios en para ti -> `AdBlocker`
- Quitar anuncios encubiertos (parcial, ver nota) -> `AdSignalsBlocker`
- Purificar los enlaces -> `UrlSanitizerBlocker`
- Renovación automática de la racha -> `StreakReminderScheduler` (versión acotada: solo recordatorio local, no auto-envío — ver "Por qué NO se replicaron otros hallazgos")
- Descargar video/foto sin marca -> `WatermarkBlocker`/`PhotoWatermarkBlocker` (rotos hoy, ver sección dedicada)

**Investigado a fondo, sin resultado (2026-09-28)** — filtro a nivel de *adaptador de feed*: se usó DexKit en runtime (no solo grep sobre código decompilado) para buscar, en el .dex completo del APK instalado, quién invoca cada predicado de tipo (`isTikTokStory`: 0 callers: `isPhotoMode`: 230 callers, `isLiveNoDeduplicateClient`: 1, `isCard`: 4) y por separado quién invoca `Aweme.getAwemeType()` (467 callers). En ambos casos, revisando los callers con firma de lista/colección (candidatos más probables a ser un "filtro que arma la lista del feed"), **ningún resultado se distingue de forma clara** — son todos renderizado de componentes individuales del feed (`FeedAvatarLiveAssem`, `VideoDiggVM`, `VideoMusicBaseVM`, `VideoShareViewModel`, etc.) o lógica de mensajería/comentarios/búsqueda no relacionada, todo en paquetes completamente ofuscados (`X.XXXX`) sin ninguna señal adicional para diferenciar el candidato correcto entre cientos. Se agotaron las vías estáticas (grep) y dinámicas (DexKit) razonables sin converger — encontrar este punto real requeriría instrumentación más profunda (breakpoints con stack trace en tiempo real vía debugger, no solo búsqueda por firma) que excede el alcance de esta sesión. Quedan pendientes:
- Quitar directos (ocultar LIVE en Para ti)
- Eliminar presentación (ocultar posts photo-mode/slideshow del feed)
- Ocultar los videos de la Tienda de TikTok (Shop)
- Remove Recommendations
- Quitar historias (Stories)
- Ocultar publicaciones largas (por duración, `Video.getDuration()` ya confirmado existente)
- Filtrar por Vistas y Me gusta
- Lista de bloqueo de descripción (caption blocklist)

**Pendientes, requieren su propia investigación (no el filtro de feed)**:
- Región / Forzar modo de región
- Remove Pendant Banners / Remove Tako AI (elementos de UI específicos, no flags de datos)
- Dejar de reproducir en bucle (hook del lifecycle del reproductor, no del modelo)
- Always Show Publish Date (formateo de fecha, no encontrado un formatter estable — ver intento previo)
- Estilo de fuente (Typeface factory)
- Quitar anuncios de pestaña Siguiente/Search/Explorar (podrían depender de las mismas señales `isAd`/`isSoftAd` ya cubiertas, o tener flags propios por pestaña — no verificado)

**Descartada**: Hide CAPTCHA popups (ver "Features descartadas por falta de hook seguro").

**N/A — específico de la propia app companion re-firmada del plugin, sin equivalente en nuestra arquitectura de hook puro**: Actualizar al privado (modelo freemium), Download Via MAX, Ruta de descargas de vídeos/imágenes (elegimos no interceptar el guardado de archivos), Modo nocturno, Idioma, TikTok Web, Corregir enlaces del navegador, Inicio de sesión de emergencia, Cargar configuración desde TikTok, Restablecer/Copia de seguridad/Restaurar, Aplicaciones predeterminadas, Botón flotante, Telegram Channel.

## Estado actual

Columna "Estado real" = confirmado con logs de un dispositivo real (Android 16 / LineageOS + KernelSU Next + LSPosed), no solo análisis estático. Ver §"Pruebas en dispositivo real" para el detalle de cada uno.

| Feature | Estado real (dispositivo) |
|---|---|
| Quitar el flag "es anuncio" del feed (`AdBlocker`) | 🟢 **Arreglado y confirmado en dispositivo real (2026-09-27)** — DexKit carga y `Aweme.isAd()` se hookea directo (ver detalle abajo) |
| Bloquear Advertising ID / GAID (`AdsIdBlocker`) | 🟡 No-op en este dispositivo (LineageOS sin Play Services con esa librería) — sin verificar en un device con Play Services real |
| Ocultar el App ID de AdMob (`AdsMetadataBlocker`) | 🔴 **Roto** — el hook falla al instalarse (`Bundle#getString` no hookeable en este build/Android 16) |
| Anular `LocationManager.getLastKnownLocation()` (`LocationBlocker`) | 🟢 Se instala correctamente (no se verificó el efecto en runtime, solo la instalación del hook) |
| Descargar video sin marca de agua (`WatermarkBlocker`) | 🔴 **Sigue roto — tikwm.com tampoco lo soluciona, confirmado con 2 videos/cuentas distintas** (2026-09-27). Se deja el código activo (no rompe nada, es best-effort) pero sin expectativa de que funcione hoy (ver detalle abajo) |
| Limpiar links copiados (`UrlSanitizerBlocker`) | ⚪ Código completo, no probado todavía en dispositivo |
| Descargar foto (slideshow) sin marca de agua (`PhotoWatermarkBlocker`) | ⚪ Se instala correctamente — no probado todavía con un post de foto real |
| Marca de agua en GIFs | No implementado — el mecanismo es distinto (dibujado por el cliente, no una URL alternativa del servidor) |
| Recordatorio local de streak (no auto-envío) | ⚪ No probado en este dispositivo todavía |
| Forzar descarga habilitada (`DownloadUnlockBlocker`) | 🟢 Se instala correctamente en dispositivo real (no se verificó el efecto en un post con `preventDownload=true`, solo la instalación del hook) |
| Anuncios encubiertos: `isSoftAd`/`isPseudoAd`/`isSearchPreciseAd` (`AdSignalsBlocker`) | 🟢 Se instala correctamente en dispositivo real (no se verificó el efecto visual, solo la instalación de los 3 hooks) |
| Ocultar CAPTCHA (`hideCaptcha` del plugin original) | ❌ **Descartado** — ver "Features descartadas por falta de hook seguro" |
| Ocultar tipos de contenido del feed (stories/shop/recomendaciones/live) | Pendiente — requiere ubicar el filtro a nivel de adaptador de feed, no el getter de tipo (ver nota de seguridad más abajo) |
| Resto de features del mod original (duet/stitch, filtros de feed, region, UI...) | No implementadas todavía — se agregan de a una, siguiendo el mismo patrón (`mods/<categoria>/<Feature>Hook.java`) |

## Pruebas en dispositivo real (2026-09-27)

Primera vez que el módulo se probó en un teléfono real (Android 16, LineageOS, KernelSU Next + LSPosed, sin Google Play Services), conectado por ADB para poder leer logcat en vivo, tomar screenshots, y automatizar taps con `uiautomator`/`input tap` durante la sesión.

**El síntoma reportado ("falla al descargar videos/historias") NO era un crash.** Se armó la hipótesis inicial de que el reinicio de TikTok a su `SplashActivity` justo después de tocar "Descargar" era un crash causado por `WatermarkBlocker`/`PhotoWatermarkBlocker`. Se descartó con evidencia:

- Logcat completo capturado durante el evento: **cero** `FATAL EXCEPTION`, cero eventos de "Process ... died".
- El archivo de video **se guardó igual** en `/sdcard/DCIM/Camera/` (se confirmó bajándolo por `adb pull` y extrayendo un frame con `ffmpeg`).
- Conclusión: el "reinicio a Splash" es la propia `Activity` de TikTok reconstruyéndose al volver del selector de compartir (comportamiento normal de Android bajo presión de memoria/recreación de Activity), no una falla de este módulo.

**El problema real, confirmado con logging agregado a `WatermarkBlocker`**: el hook se instala, se dispara, y reemplaza `getDownloadAddr()` por `getDownloadNoWatermarkAddr()` correctamente (se ve en el log que esta última no es `null` y apunta a una URL distinta — el endpoint normal de reproducción interna `api16-normal-c-*.tiktokv.com/aweme/v1/play/`, sin `watermark=1&logo_name=tiktok` en la query, a diferencia del original). **Pese a eso, el archivo final descargado sigue con la marca de agua incrustada en los píxeles.** Esto no se puede arreglar cambiando el hook: es TikTok (del lado del servidor) sirviendo contenido marcado por ambas URLs, al menos para esta cuenta/región/versión. El campo `download_no_watermark_addr` puede haber dejado de significar lo que su nombre sugiere.

De paso, esta sesión de pruebas destapó **dos hooks que directamente no funcionan** en este dispositivo, no relacionados con el reporte original:

- **`AdBlocker` nunca se activa** (ver "Fix de DexKit" abajo — ya resuelto en una sesión posterior).
- **`AdsMetadataBlocker` falla al instalarse**: `XposedHelpers.findAndHookMethod` sobre `Bundle#getString(String)` tira error en este Android 16 — puede que ART haya empezado a tratar ese método de forma especial (demasiado caliente/inlineable) y LSPosed no pueda hookearlo ahí.

### Fix de DexKit (root cause completo, dos capas)

`DexKitBridge.create()` tiraba `UnsatisfiedLinkError: No implementation found for ... nativeInitDexKit`. Se descartó primero un problema de empaquetado/símbolos (`radare2` confirmó que `libdexkit.so` sí estaba en las 4 ABIs del APK con el símbolo exportado correcto) — la causa real es que código Xposed inyectado en el proceso de TikTok no resuelve automáticamente la carpeta de libs nativas del propio módulo. Se implementó `IXposedHookZygoteInit.initZygote()` (captura `StartupParam.modulePath`) + `System.load()` explícito, siguiendo el mismo patrón que usa InstaEclipse. Verificar esto en dispositivo real destapó **dos bugs adicionales, uno arriba del otro**:

1. **Las libs nativas no se extraían a disco**: `nativeloader` mostraba `library_path=.../base.apk!/lib/arm64-v8a` (formato `!/` = mapeada dentro del `.apk`, sin extraer), así que cualquier ruta de archivo real fallaba con `dlopen failed: ... not found`. **Fix**: `packaging { jniLibs { useLegacyPackaging = true } } }` en `app/build.gradle` (AGP 8.7.0) — sin esto, AGP empaqueta las libs comprimidas/alineadas a página para mmap directo desde el ZIP, que es el comportamiento por defecto desde hace varias versiones de AGP.
2. **El nombre de la carpeta ABI no es el que devuelve `Build.SUPPORTED_ABIS[0]`**: incluso con `useLegacyPackaging` activado, `System.load(...+"/lib/arm64-v8a/libdexkit.so")` seguía fallando. Verificado con `adb shell find` sobre el directorio de instalación real: `PackageManager` extrajo la lib bajo `lib/arm64/`, **no** `lib/arm64-v8a/` (nombre de carpeta abreviado en este dispositivo — LineageOS + KernelSU Next). **Fix**: en vez de construir el nombre de carpeta a partir de `Build.SUPPORTED_ABIS[0]`, se lista el contenido real de `<apkDir>/lib/` con `File.listFiles()` y se usa la única carpeta que aparece — no depende de ninguna tabla de mapeo ABI→nombre-de-carpeta que pueda variar por vendor/versión de Android.

Confirmado con logcat en frío (2026-09-27, misma sesión): `(TikRatu): libdexkit.so cargado desde .../lib/arm64` seguido de `(TikRatu | AdBlocker): hooked directo -> Aweme.isAd()`.

**Herramientas instaladas en esta máquina durante la sesión** (quedan disponibles para la próxima): `adb` (Android Platform Tools, vía winget) y `ffmpeg` (para extraer frames de video y verificar visualmente el resultado de los downloads).

### ¿Por qué el mod original (Rezvorck) sí lograba sacar la marca de agua?

El mod analizado en `C:\Audit\equipos\tiktok apk\README.md` y su plugin usan **exactamente el mismo truco** que `WatermarkBlocker` (server manda dos URLs, se usa la que dice "no watermark"). No es una técnica distinta — es la misma. La diferencia más probable es **de tiempo**: ese mod/plugin apunta a TikTok **46.4.3**; esta sesión de pruebas se hizo contra la versión real instalada del dispositivo, **47.0.3**. ByteDance pelea activamente contra este tipo de mods y cambia el comportamiento del backend con frecuencia (por versión de app, o por rollout de servidor/A-B testing sin ni siquiera tocar el cliente). Es probable que el campo `download_no_watermark_addr` haya dejado de servir contenido limpio en algún punto entre esas dos versiones — algo que rompería la implementación de ellos tanto como la nuestra, ya que ambas dependen del mismo campo del servidor. Un mod comercial activo probablemente se actualiza seguido para encontrar el próximo endpoint/parámetro que sí funcione; acá estamos viendo una foto de un momento específico.

Se intentó verificar bajando ambas URLs directo desde la PC (fuera del teléfono) para comparar los archivos byte a byte — ambas fallaron (404/400): estas URLs firmadas están atadas a la sesión/dispositivo que las pidió (IP, user-agent, y probablemente el `signaturev3` valida más que la URL en sí), no se pueden reproducir desde un cliente externo. Confirmar esta hipótesis con certeza requeriría correr el mod real en un dispositivo y diffear tráfico de red contra el nuestro — no se hizo en esta sesión.

**Reconfirmado en una segunda sesión de pruebas (2026-09-27, misma fecha, después del batch de fixes de DexKit)**, con un video completamente distinto (contenido de @ferxxo444, no el video BTS de la primera prueba): logcat muestra el mismo patrón (`getDownloadAddr()` original con `&watermark=1&logo_name=tiktok_m` en la query, `getDownloadNoWatermarkAddr()` sin ese parámetro, ambos con el mismo `video_id`) y el hook reemplaza el resultado correctamente. El archivo se bajó (`adb pull`) y se extrajo un frame con `ffmpeg` (`docs/evidencia/watermark_persiste_2026-09-27.png`): **el logo "TikTok @ferxxo444" sigue incrustado en los píxeles**. Con dos videos distintos mostrando el mismo resultado, la hipótesis de servidor queda mucho más sólida — no es un caso aislado de un video puntual. **No hay más nada que cambiar del lado del hook usando solo campos de TikTok**: mientras ambos campos del servidor devuelvan el mismo contenido marcado, ninguna combinación de swap de campos en el cliente puede producir un archivo limpio.

### Fix real: resolución vía tikwm.com (tercero)

El profesor acercó un proyecto de referencia (`C:\Audit\equipos\app tiktok\TikTokDL`, un downloader standalone hecho como parte del curso) que resuelve videos de TikTok pegando un link, vía la API pública de **tikwm.com**. La diferencia clave: tikwm hace su propia extracción server-to-server contra la API de TikTok, con sus propias credenciales — no depende de qué le sirve el backend de TikTok a la sesión/cuenta de un usuario en particular, que es justo lo que está roto arriba.

Integración a `WatermarkBlocker` (no se creó una app aparte — se integró directo al hook existente, mismo punto ya confirmado que dispara sólo al tocar "Descargar", no durante el scroll del feed):

1. Se hookea también `Aweme.getVideo()` (getter público sin ofuscar) para, cada vez que se llama, guardar en un `WeakHashMap<Video, String>` la asociación `Video -> Aweme.getShareUrl()` (campo público sin ofuscar, confirmado en `Aweme.java`).
2. En `Video.getDownloadAddr()` (el hook de siempre), si hay un `shareUrl` asociado a ese `Video`, se llama a `TikwmResolver.resolve(shareUrl)` (nueva clase, `HttpURLConnection` + `org.json`, timeout 6s/8s) y, si devuelve una URL, se muta el `UrlModel` original con `setUrlList(...)` (setter público de `com.ss.android.ugc.aweme.base.model.UrlModel`) en vez de fabricar un objeto nuevo.
3. Si tikwm falla, no responde, o tira excepción — cualquier error — se hace **fallback silencioso** al swap de `getDownloadNoWatermarkAddr()` de siempre (no arregla el problema, pero tampoco cambia el comportamiento respecto a antes de este fix).

**Tradeoff explícito (decisión de diseño, no un descuido)**: este único hook manda el `shareUrl` del video que el usuario decide descargar a un servidor gratuito de terceros sin SLA — puede fallar, cambiar de formato o discontinuarse en cualquier momento, y ese tercero ve qué video se está descargando. Es la única vía real encontrada para que el archivo final quede efectivamente sin marca; el resto de los hooks de TikRatu no dependen de ningún servicio externo.

**Confirmado en dispositivo real (2026-09-27)**: `getDownloadAddr()` SÍ corre en el hilo principal de TikTok (log: `main=true`). La primera versión de este fix llamaba a tikwm.com directo en ese hilo — resultado: `TikwmResolver: fallo la resolucion (null)`, consistente con `NetworkOnMainThreadException` (Android prohíbe I/O de red en el hilo principal; esa excepción típicamente no lleva mensaje). La función nunca podía funcionar así. **Fix de threading**: la llamada a `TikwmResolver.resolve()` ahora corre en un hilo dedicado (`Executors.newSingleThreadExecutor`), y `getDownloadAddr()` espera el resultado con `Future.get(2500, TimeUnit.MILLISECONDS)` — timeout corto, bien por debajo del umbral típico de detección de ANR (~5s) para eventos táctiles.

**Con el fix de threading, tikwm.com SÍ resuelve una URL exitosamente** (log: `URL resuelta via tikwm.com para <shareUrl> -> https://v16-notes.tiktokcdn-us.com/...`, dominio CDN distinto al de TikTok, `hdplay` del JSON de tikwm) — pero **el archivo final sigue con la marca de agua incrustada**, verificado dos veces:

1. Video de `@kinset.pe` (cuenta de curación/compilación, "ARMY BUS") — descartado inicialmente como posible falso positivo (hipótesis: el video ya venía marcado de una fuente anterior antes de subirse a TikTok, ya que ese tipo de cuenta resube contenido).
2. Video de `@thor_linda` (cuenta personal, dueño grabando a su propia mascota — **no** es una cuenta de repost/compilación, descarta esa hipótesis): `docs/evidencia/tikwm_sigue_con_marca_2026-09-27.png` — el frame extraído muestra "TikTok @thor_linda" incrustado en la esquina inferior izquierda, igual que con el swap de campo propio.

**Conclusión**: ni el swap de campo de TikTok ni la resolución externa vía tikwm.com producen hoy un archivo sin marca, para esta cuenta/región/fecha. No se puede afirmar con certeza *por qué* falla tikwm.com específicamente (podría ser que su endpoint `hdplay` ya no sirva contenido limpio, medidas anti-scraping recientes de TikTok, o un problema puntual de su servicio) — no hay forma de depurar el lado servidor de un tercero. Se deja el código de `TikwmResolver` activo (no rompe nada, es un intento best-effort con fallback seguro) pero la UI (`strings.xml`) y esta tabla reflejan honestamente que no funciona hoy, en vez de prometer un resultado no verificado.

### Fix intentado #3: diff contra el mod original + tres campos de descarga nunca antes probados

A pedido explícito del profesor, se comparó el código real de descarga entre el TikTok 46.4.3 oficial (sin modificar) y `46.4.3_universal_fix.apk` (el mod), extrayendo todos los `classes*.dex` de ambos APKs y buscando por string exacto (`download_no_watermark_addr`, `getDownloadAddr`, `getDownloadNoWatermarkAddr`) en cada archivo por separado. **Resultado: el mod usa exactamente el mismo `Video.java` sin modificar** (mismos getters, mismo dex `classes25.dex` en ambos) — no hay ningún parche de código adicional que replicar. Confirma lo que ya se sospechaba: el mod funcionaba porque en su época (46.4.3) el campo `download_no_watermark_addr` sí devolvía contenido limpio, no porque tuviera una técnica distinta a la nuestra.

De paso, comparando el modelo protobuf equivalente de `Video` (clase ofuscada `X.C05OL`/`X.C5206024e` según el build, en `classes3.dex` de TikTok 47.0.3 oficial y en `classes.dex` del mod) se encontró un catálogo de campos de descarga bastante más amplio que el par ya conocido:

```
download_addr                      -> getDownloadAddr()            (con marca, ya conocido)
new_download_addr                  -> getNewDownloadAddr()          NUEVO, nunca probado
download_suffix_logo_addr          -> (sin getter Gson, solo protobuf, no alcanzable desde este hook)
has_download_suffix_logo_addr      -> (idem, flag booleano acompañante)
ui_alike_download_addr             -> getUIAlikeDownloadAddr()      NUEVO, nunca probado
caption_download_addr              -> getCaptionDownloadAddr()     NUEVO, nunca probado
misc_download_addrs                -> (sin getter Gson, solo protobuf, tipo String — posible JSON anidado)
download_no_watermark_addr         -> getDownloadNoWatermarkAddr() (confirmado roto)
play_addr_3d_fallback              -> (para contenido 3D, no aplica al caso general)
```

De los tres nuevos campos alcanzables desde Gson (`newDownloadAddr`, `uiAlikeAddr`, `captionDownloadAddr`), se implementó una cascada en `WatermarkBlocker` que los prueba en ese orden antes de tikwm.com y el swap viejo, con logging de diagnóstico de las 4 URLs candidatas en cada llamada. **Probado en dispositivo real con 2 videos de cuentas distintas (2026-09-27): los tres campos nuevos llegan `null` en ambos casos** — el servidor no los está poblando para esta cuenta/sesión/momento (podría ser un campo activo solo bajo otro experimento A/B, solo para ciertas cuentas/regiones, o solo alcanzable realmente por la vía protobuf y no por la vía JSON que usa nuestro hook). No se descarta que en otra cuenta/región/versión sí vengan poblados — el código queda activo por si acaso, sin costo (si son null, cae al siguiente candidato).

**Conclusión final de esta investigación extendida**: se agotaron todas las vías de swap-de-campo-del-servidor conocidas (2 campos originales + 3 nuevos descubiertos) y la resolución vía servicio de terceros (tikwm.com) — ninguna produce hoy un archivo sin marca para esta cuenta/dispositivo. El hook queda con la cascada completa activa (no hace daño, prioriza el mejor candidato disponible en cada caso) pero sin prometer un resultado que no se puede confirmar.

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
- **`DownloadUnlockBlocker`** — mismo `Aweme` de §"Cómo funciona el hook de ads", campo/getter público `preventDownload`/`isPreventDownload()` (verificado sin ofuscar en 46.4.3 y 47.0.3, mismo motivo Gson que `isAd()`). Es un flag de un solo propósito (gatekeeper de descarga por-post) — a diferencia de `isImage()`/`isTikTokStory`, no participa en decisiones de renderizado, así que forzarlo a `false` globalmente es seguro en el mismo sentido que `isAd()`.
- **`AdSignalsBlocker`** — TikTok tiene más de una señal de "esto es un anuncio", verificado en `com.ss.android.ugc.aweme.feed.model.AwemeExtKt` (`classes5.dex` en 47.0.3, mismo contenido en 46.4.3): `isAdTraffic(aweme)` = `aweme.isAd() || aweme.isSoftAd()`, y de ahí dependen `isDescAreaReplaceAd`/`isTextMoreAd*`. Como `isAd()` ya lo cubre `AdBlocker`, sólo faltaba `isSoftAd()`. Además hookea dos flags independientes de `isAdTraffic()`: `isPseudoAd(aweme)` (mira `getCommerceVideoAuthInfo()`) e `isSearchPreciseAd(aweme)` (mira `aweme.awemeRawAd`). Las tres son gates booleanos de un solo propósito, mismo criterio de seguridad que `isAd()`/`isPreventDownload()`.
- **`UrlSanitizerBlocker`** — a diferencia de todos los demás, no apunta a ninguna clase de TikTok: hookea `android.content.ClipboardManager.setPrimaryClip(ClipData)` (API pública de Android, mismo criterio de riesgo que `AdsMetadataBlocker` — estable sin importar cómo TikTok ofusque su código interno). Si el texto copiado contiene una URL con parámetros de tracking conocidos (`utm_*`, `ttclid`, `_r`, `enter_from_merge`, `enter_method`, `share_from_user_id`, `sub\d+` — confirmados en `classes12.dex`/`classes2.dex`, no inventados), los quita antes de que lleguen al portapapeles real. No toca nada si el texto copiado no es una URL con esos parámetros (comentarios, nombres de usuario, etc. quedan intactos).

## Features descartadas por falta de hook seguro

- **`hideCaptcha`** (del plugin original: *"Prevents client-side CAPTCHA... does not bypass server-side security checks"*): se buscó el string `popCaptchaDialog`/`requestAndPopCaptchaDialog` en `classes19.dex` de 47.0.3 — aparece dentro de una clase totalmente ofuscada por R8 (`C32970dhO`, campos `LIZ`/`LIZIZ`/`LJ`, log-tag hardcodeado `"BroadcastMessagePresenter"`), y está acotada a CAPTCHA de **TikTok LIVE** (`CaptchaLivePauseTimeChannel`, `roomId`), no al CAPTCHA general de la app. El camino más general pasa por `com.ss.android.ugc.aweme.sec.captcha.SecCaptcha` (`classes7.dex`, nombre de clase real y sin ofuscar), pero es un wrapper delgado sobre el SDK de terceros **BDTuring** (`com.bytedance.bdturing.VerifyTaskHandler`) — casi todos sus campos/métodos internos están ofuscados (`C0DXZ`, `C28160DXy`...) salvo `dismissVerifyDialog()`, que pertenece al SDK de verificación compartido (también usado en login/compras), no es un flag de un solo propósito. Se descarta: no hay, hoy, un nombre de método estable y de propósito único para este hook — forzarlo requeriría hookear identificadores ofuscados (rompen en cualquier build nuevo) o un método del SDK de verificación compartido con toda la app (riesgo de romper flujos legítimos), violando el mismo criterio de seguridad aplicado a los discriminadores de tipo.

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

## App companion: UI (rediseño 2026-09-27)

El profesor acercó capturas reales de un "TikTok Plugin" de referencia (`C:\Audit\equipos\tiktok apk\img tiktok plugin`, 9 screenshots) con un diseño y catálogo de funciones más amplio que InstaEclipse. Se rediseñó la UI de TikRatu calcando ese estilo:

- **Paleta**: negro puro (`#000000`) en vez de la rampa de superficies M3 que se usaba antes, con acento durazno/salmón (`#f0b8a0`) para switches ON y botones destacados, en vez del cian/rosa de TikTok.
- **Lista de features**: filas planas sin `MaterialCardView` ni iconos (antes: card con ícono a la izquierda) — título + descripción a la izquierda, switch a la derecha, con separación por espaciado en vez de bordes.
- **Navegación**: `BottomNavigationView` de 3 tabs (Inicio/Ajustes/Información) en vez de una pantalla única con botón atrás — mismo patrón que el plugin de referencia. "Ajustes" e "Información" tienen contenido real de TikRatu (link al repo, disclaimer, descripción del proyecto), no una copia 1:1 de las opciones del plugin (esas —modo nocturno, idioma, backup/restore de config, Telegram channel— son específicas de su propia app companion re-firmada, sin equivalente en nuestra arquitectura).
- Verificado en dispositivo real (dump de pantalla vía ADB) tras un bug real encontrado y corregido: `BottomNavigationView` no garantiza que el primer ítem del menú quede seleccionado visualmente sin un `setSelectedItemId()` explícito — sin eso, abría en la tab "Información" por defecto.

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
