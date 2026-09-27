# TikRatu

Módulo Xposed/LSPosed para la app **oficial** de TikTok (`com.zhiliaoapp.musically`). No parchea, no re-firma ni redistribuye el APK — corre como hook en runtime sobre la instalación oficial del usuario, en un dispositivo con root + LSPosed.

Inspirado en [InstaEclipse](https://github.com/ReSo7200/InstaEclipse) (mismo patrón: módulo standalone + [DexKit](https://github.com/LuckyPray/DexKit) para ubicar métodos en runtime sin depender de nombres ofuscados fijos).

Ver [DISCLAIMER.md](DISCLAIMER.md) antes de usarlo.

## Por qué existe

Nace del análisis de un mod de terceros para TikTok (repack cerrado, re-firmado con certificado ajeno, con protección de código nativo tipo VM) hecho como trabajo de la materia **Protección de Software**. La idea de TikRatu es lograr una funcionalidad similar (por ahora: sacar el flag de "esto es un anuncio" del feed) de forma **abierta, auditable y sin tocar el binario de TikTok**, usando la técnica de hooking en runtime en vez de parchear+re-firmar un APK.

## Estado actual

| Feature | Estado |
|---|---|
| Quitar el flag "es anuncio" del feed (`AdBlocker`) | Implementado, hook verificado contra el código real de TikTok 46.4.3 (ver abajo) |
| Resto de features del mod original (watermark, duet/stitch, CAPTCHA, streak, region...) | No implementadas todavía — se agregan de a una, siguiendo el mismo patrón (`mods/<categoria>/<Feature>Hook.java`) |

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

**Nota de verificación**: no hay entorno Android SDK / emulador / dispositivo en esta máquina, así que el proyecto **no se compiló ni se probó en runtime** todavía. La API exacta de `MethodMatcher`/`FindMethod` de `dexkit:2.0.3` usada en el fallback se escribió según la documentación pública de DexKit, pero hay que confirmarla al abrir el proyecto en Android Studio (primer build) antes de asumir que compila tal cual.

## Build

1. Abrir la carpeta en Android Studio (usa Gradle 8.10.2 / AGP 8.7.0, iguales a InstaEclipse).
2. `./gradlew assembleDebug` (o el botón Run de Android Studio).
3. Instalar el APK en el celular con LSPosed.
4. Activar el módulo en LSPosed Manager, marcar `com.zhiliaoapp.musically` en su alcance.
5. Forzar el cierre de TikTok y volver a abrirlo.
6. Revisar logs con `adb logcat | grep TikRatu` o desde el visor de logs de LSPosed.

## Estructura

```
app/src/main/java/dev/ryan/tikratu/
├── MainActivity.java          # pantalla de estado (companion app mínima)
├── Xposed/Module.java         # entry point IXposedHookLoadPackage
├── mods/ads/AdBlocker.java    # primer hook (isAd() -> false)
└── utils/log/ModuleLog.java  # wrapper de XposedBridge.log
```

Cada feature nueva va en `mods/<categoria>/<Nombre>Hook.java` y se registra en `Module.java`, igual que `AdBlocker`.

## Licencia

MIT — ver [LICENSE](LICENSE).
