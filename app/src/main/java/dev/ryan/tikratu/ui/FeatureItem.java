package dev.ryan.tikratu.ui;

public class FeatureItem {

    public enum Type { HEADER, SWITCH, ACTION, STATUS }

    public interface OnToggle {
        void onToggle(boolean checked);
    }

    public interface OnClick {
        void onClick();
    }

    public final Type type;
    public final String title;
    public final String description;
    public final String prefKey;
    public final boolean defaultValue;
    public final OnToggle onToggle;
    public String value;
    public final OnClick onClick;
    // Solo para STATUS: textos dinámicos (estado del módulo + versión de TikTok).
    public String statusModule;
    public String statusVersion;

    private FeatureItem(Type type, String title, String description,
                         String prefKey, boolean defaultValue, OnToggle onToggle,
                         String value, OnClick onClick) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.prefKey = prefKey;
        this.defaultValue = defaultValue;
        this.onToggle = onToggle;
        this.value = value;
        this.onClick = onClick;
    }

    public static FeatureItem header(String title) {
        return new FeatureItem(Type.HEADER, title, null, null, false, null, null, null);
    }

    public static FeatureItem toggle(String title, String description,
                                      String prefKey, boolean defaultValue) {
        return toggle(title, description, prefKey, defaultValue, null);
    }

    public static FeatureItem toggle(String title, String description,
                                      String prefKey, boolean defaultValue, OnToggle onToggle) {
        return new FeatureItem(Type.SWITCH, title, description, prefKey, defaultValue, onToggle, null, null);
    }

    public static FeatureItem action(String title, String value, OnClick onClick) {
        return new FeatureItem(Type.ACTION, title, null, null, false, null, value, onClick);
    }

    public static FeatureItem action(String title, String description, String value, OnClick onClick) {
        return new FeatureItem(Type.ACTION, title, description, null, false, null, value, onClick);
    }

    /** Tarjeta de estado del módulo + botón "Abrir TikTok", como primer item del tab Inicio. */
    public static FeatureItem status(String statusModule, String statusVersion, OnClick onOpenTikTok) {
        FeatureItem item = new FeatureItem(Type.STATUS, null, null, null, false, null, null, onOpenTikTok);
        item.statusModule = statusModule;
        item.statusVersion = statusVersion;
        return item;
    }
}
