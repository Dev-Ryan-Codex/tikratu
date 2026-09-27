package dev.ryan.tikratu.utils;

/**
 * Truco clásico de detección "¿Xposed/LSPosed me está cargando de verdad?":
 * isModuleActive() siempre devuelve false en un APK normal instalado sin LSPosed.
 * Module.java (el hook) se agrega a sí mismo como target en xposedscope (ver
 * arrays.xml) y, cuando LSPosed carga dev.ryan.tikratu, hookea este método
 * exacto para que devuelva true. Si la companion app lo llama y ve "true",
 * es porque el framework Xposed está activo y este módulo está habilitado.
 */
public final class StatusChecker {

    private StatusChecker() {
    }

    public static boolean isModuleActive() {
        return false;
    }
}
