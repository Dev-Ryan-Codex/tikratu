package dev.ryan.tikratu.utils;

/**
 * Nombres de key/valores por defecto compartidos entre la UI (FeatureAdapter,
 * que persiste via AppPrefs — ver ese archivo para el porque NO se usa
 * SharedPreferences de Android) y el lado Xposed (Xposed/ModulePrefsReader,
 * que lee el mismo archivo directamente dentro del proceso de TikTok).
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

    public static final String KEY_URL_SANITIZER_BLOCKER = "pref_url_sanitizer_blocker_enabled";
    public static final boolean DEFAULT_URL_SANITIZER_BLOCKER = true;

    public static final String KEY_FONT_STYLE_BLOCKER = "pref_font_style_blocker_enabled";
    public static final boolean DEFAULT_FONT_STYLE_BLOCKER = false;

    // Filtro de feed (FeedFilterBlocker). Todo apagado por defecto: cambian
    // que contenido se ve, no son fixes de privacidad/ads.
    public static final String KEY_HIDE_LIVE = "pref_feed_hide_live";
    public static final boolean DEFAULT_HIDE_LIVE = false;

    public static final String KEY_HIDE_STORY = "pref_feed_hide_story";
    public static final boolean DEFAULT_HIDE_STORY = false;

    public static final String KEY_HIDE_SHOP = "pref_feed_hide_shop";
    public static final boolean DEFAULT_HIDE_SHOP = false;

    public static final String KEY_HIDE_IMAGE = "pref_feed_hide_image";
    public static final boolean DEFAULT_HIDE_IMAGE = false;

    public static final String KEY_HIDE_PROMOTED_MUSIC = "pref_feed_hide_promoted_music";
    public static final boolean DEFAULT_HIDE_PROMOTED_MUSIC = false;

    /** Segundos; 0 = desactivado. */
    public static final String KEY_MAX_DURATION_SEC = "pref_feed_max_duration_sec";
    public static final long DEFAULT_MAX_DURATION_SEC = 0;

    /** 0 = desactivado. */
    public static final String KEY_MIN_VIEWS = "pref_feed_min_views";
    public static final long DEFAULT_MIN_VIEWS = 0;

    /** 0 = desactivado. */
    public static final String KEY_MIN_LIKES = "pref_feed_min_likes";
    public static final long DEFAULT_MIN_LIKES = 0;

    /** Palabras separadas por coma; vacio = desactivado. */
    public static final String KEY_CAPTION_BLOCKLIST = "pref_feed_caption_blocklist";
    public static final String DEFAULT_CAPTION_BLOCKLIST = "";

    public static final String KEY_STOP_VIDEO_LOOPING = "pref_stop_video_looping";
    public static final boolean DEFAULT_STOP_VIDEO_LOOPING = false;

    public static final String KEY_SCREEN_CAPTURE_BLOCKER = "pref_screen_capture_blocker";
    public static final boolean DEFAULT_SCREEN_CAPTURE_BLOCKER = false;

    public static final String KEY_DISABLE_LOGIN = "pref_disable_login";
    public static final boolean DEFAULT_DISABLE_LOGIN = false;

    public static final String KEY_STREAK_ENABLED = "pref_streak_enabled";
    public static final boolean DEFAULT_STREAK_ENABLED = false;

    public static final String KEY_STREAK_HOUR = "pref_streak_hour";
    public static final int DEFAULT_STREAK_HOUR = 20;

    public static final String KEY_STREAK_MINUTE = "pref_streak_minute";
    public static final int DEFAULT_STREAK_MINUTE = 0;
}
