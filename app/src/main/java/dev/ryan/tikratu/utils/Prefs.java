package dev.ryan.tikratu.utils;

/**
 * Nombres de key/valores por defecto compartidos entre la UI (SwitchPreferenceCompat
 * en res/xml/prefs_*.xml, que persisten en el archivo de preferencias por defecto de
 * la app) y el lado Xposed (que los lee con XSharedPreferences dentro del proceso de
 * TikTok). Los nombres de key ACA deben coincidir letra por letra con los
 * android:key="..." de los XML de preferencias — no hay binding automático.
 */
public final class Prefs {

    private Prefs() {
    }

    public static final String KEY_AD_BLOCKER = "pref_ad_blocker_enabled";
    public static final boolean DEFAULT_AD_BLOCKER = true;

    public static final String KEY_ADS_ID_BLOCKER = "pref_ads_id_blocker_enabled";
    public static final boolean DEFAULT_ADS_ID_BLOCKER = true;

    public static final String KEY_ADS_METADATA_BLOCKER = "pref_ads_metadata_blocker_enabled";
    public static final boolean DEFAULT_ADS_METADATA_BLOCKER = true;

    public static final String KEY_LOCATION_BLOCKER = "pref_location_blocker_enabled";
    public static final boolean DEFAULT_LOCATION_BLOCKER = true;

    public static final String KEY_WATERMARK_BLOCKER = "pref_watermark_blocker_enabled";
    public static final boolean DEFAULT_WATERMARK_BLOCKER = true;

    public static final String KEY_PHOTO_WATERMARK_BLOCKER = "pref_photo_watermark_blocker_enabled";
    public static final boolean DEFAULT_PHOTO_WATERMARK_BLOCKER = true;

    public static final String KEY_DOWNLOAD_UNLOCK_BLOCKER = "pref_download_unlock_blocker_enabled";
    public static final boolean DEFAULT_DOWNLOAD_UNLOCK_BLOCKER = true;

    public static final String KEY_AD_SIGNALS_BLOCKER = "pref_ad_signals_blocker_enabled";
    public static final boolean DEFAULT_AD_SIGNALS_BLOCKER = true;

    public static final String KEY_STREAK_ENABLED = "pref_streak_enabled";
    public static final boolean DEFAULT_STREAK_ENABLED = false;

    public static final String KEY_STREAK_HOUR = "pref_streak_hour";
    public static final int DEFAULT_STREAK_HOUR = 20;

    public static final String KEY_STREAK_MINUTE = "pref_streak_minute";
    public static final int DEFAULT_STREAK_MINUTE = 0;
}
