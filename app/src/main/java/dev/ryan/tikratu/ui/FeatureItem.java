package dev.ryan.tikratu.ui;

public class FeatureItem {

    public enum Type { HEADER, SWITCH, ACTION }

    public interface OnToggle {
        void onToggle(boolean checked);
    }

    public interface OnClick {
        void onClick();
    }

    public final Type type;
    public final String title;
    public final String description;
    public final int iconRes;
    public final String prefKey;
    public final boolean defaultValue;
    public final OnToggle onToggle;
    public String value;
    public final OnClick onClick;

    private FeatureItem(Type type, String title, String description, int iconRes,
                         String prefKey, boolean defaultValue, OnToggle onToggle,
                         String value, OnClick onClick) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.prefKey = prefKey;
        this.defaultValue = defaultValue;
        this.onToggle = onToggle;
        this.value = value;
        this.onClick = onClick;
    }

    public static FeatureItem header(String title) {
        return new FeatureItem(Type.HEADER, title, null, 0, null, false, null, null, null);
    }

    public static FeatureItem toggle(int iconRes, String title, String description,
                                      String prefKey, boolean defaultValue) {
        return toggle(iconRes, title, description, prefKey, defaultValue, null);
    }

    public static FeatureItem toggle(int iconRes, String title, String description,
                                      String prefKey, boolean defaultValue, OnToggle onToggle) {
        return new FeatureItem(Type.SWITCH, title, description, iconRes, prefKey, defaultValue, onToggle, null, null);
    }

    public static FeatureItem action(int iconRes, String title, String value, OnClick onClick) {
        return new FeatureItem(Type.ACTION, title, null, iconRes, null, false, null, value, onClick);
    }
}
