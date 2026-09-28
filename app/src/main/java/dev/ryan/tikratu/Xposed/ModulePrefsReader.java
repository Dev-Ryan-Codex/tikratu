package dev.ryan.tikratu.Xposed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import dev.ryan.tikratu.utils.log.ModuleLog;

/**
 * Reemplaza XSharedPreferences (descartado — ver AppPrefs.java para el
 * detalle completo del bug real encontrado en dispositivo: en este OS,
 * Context.getSharedPreferences() nunca crea el archivo XML clasico que
 * XSharedPreferences necesita leer). Lee directamente el archivo de
 * propiedades propio que escribe AppPrefs, con el mismo formato
 * java.util.Properties.
 */
final class ModulePrefsReader {

    private final Properties props = new Properties();

    ModulePrefsReader(String modulePackageName) {
        File f = new File("/data/user/0/" + modulePackageName + "/files/tikratu_prefs.properties");
        try (FileInputStream in = new FileInputStream(f)) {
            props.load(in);
            ModuleLog.line("(TikRatu): prefs cargadas desde " + f.getAbsolutePath());
        } catch (IOException e) {
            ModuleLog.line("(TikRatu): no se pudo leer " + f.getAbsolutePath() + " (" + e.getMessage()
                    + ") — se usan los valores por defecto de cada feature");
        }
    }

    boolean getBoolean(String key, boolean defaultValue) {
        String raw = props.getProperty(key);
        return raw != null ? Boolean.parseBoolean(raw) : defaultValue;
    }
}
