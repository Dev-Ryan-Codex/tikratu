package dev.ryan.tikratu.mods.tracking;

import android.location.LocationManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Corta la ubicacion que TikTok puede leer, sin tocar el permiso del sistema
 * (el usuario puede seguir teniendolo concedido; esto solo hace que, DENTRO
 * del proceso de TikTok, LocationManager.getLastKnownLocation() devuelva null
 * — un valor de retorno normal y esperado por la API de Android, no una
 * excepcion, asi que no deberia romper nada que ya maneje "sin ubicacion").
 *
 * NO cubre com.google.android.gms.location.FusedLocationProviderClient
 * (la API mas moderna de Play Services): su getLastLocation() devuelve un
 * Task<Location> ya construido en el momento de la llamada, y no se puede
 * reemplazar con un simple setResult(null) sin arriesgar un NullPointerException
 * en el callback de exito de quien lo llama. Requiere mas RE contra el codigo
 * real de TikTok para hacerlo sin romper nada — queda pendiente.
 */
public class LocationBlocker {

    public void block(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    LocationManager.class, "getLastKnownLocation", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });
            ModuleLog.line("(TikRatu | LocationBlocker): hooked LocationManager.getLastKnownLocation");
        } catch (Throwable t) {
            ModuleLog.line("(TikRatu | LocationBlocker): fallo el hook (" + t.getMessage() + ")");
        }
    }
}
