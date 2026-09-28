package dev.ryan.tikratu.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import dev.ryan.tikratu.R;
import dev.ryan.tikratu.streak.StreakReminderScheduler;
import dev.ryan.tikratu.utils.AppPrefs;
import dev.ryan.tikratu.utils.Prefs;

/**
 * Pantalla única con todos los switches, agrupados por categoría con headers
 * discretos (diseño calcado del plugin de referencia: lista plana sin cards
 * ni iconos, ver C:\Audit\equipos\tiktok apk\img tiktok plugin).
 */
public class FeaturesActivity extends Activity {

    private static final String REPO_URL = "https://github.com/Dev-Ryan-Codex/tikratu";

    private FeatureAdapter adapter;
    private List<FeatureItem> items;
    private FeatureItem timeItem;
    private int timeItemPosition;

    private View recyclerFeatures;
    private View settingsContainer;
    private View infoContainer;
    private TextView screenTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        screenTitle = findViewById(R.id.tv_screen_title);

        items = buildItems();

        recyclerFeatures = findViewById(R.id.recycler_features);
        settingsContainer = findViewById(R.id.settings_container);
        infoContainer = findViewById(R.id.info_container);

        RecyclerView recycler = (RecyclerView) recyclerFeatures;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeatureAdapter(items, this);
        recycler.setAdapter(adapter);

        findViewById(R.id.action_repo).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showTab(recyclerFeatures, R.string.features);
            } else if (id == R.id.nav_settings) {
                showTab(settingsContainer, R.string.nav_settings);
            } else if (id == R.id.nav_info) {
                showTab(infoContainer, R.string.nav_info);
            }
            return true;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void showTab(View toShow, int titleRes) {
        recyclerFeatures.setVisibility(toShow == recyclerFeatures ? View.VISIBLE : View.GONE);
        settingsContainer.setVisibility(toShow == settingsContainer ? View.VISIBLE : View.GONE);
        infoContainer.setVisibility(toShow == infoContainer ? View.VISIBLE : View.GONE);
        screenTitle.setText(titleRes);
    }

    private List<FeatureItem> buildItems() {
        List<FeatureItem> list = new ArrayList<>();

        list.add(FeatureItem.header(getString(R.string.section_ads)));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_ad_blocker_title), getString(R.string.feature_ad_blocker_desc),
                Prefs.KEY_AD_BLOCKER, Prefs.DEFAULT_AD_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_ads_id_title), getString(R.string.feature_ads_id_desc),
                Prefs.KEY_ADS_ID_BLOCKER, Prefs.DEFAULT_ADS_ID_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_ads_metadata_title), getString(R.string.feature_ads_metadata_desc),
                Prefs.KEY_ADS_METADATA_BLOCKER, Prefs.DEFAULT_ADS_METADATA_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_location_title), getString(R.string.feature_location_desc),
                Prefs.KEY_LOCATION_BLOCKER, Prefs.DEFAULT_LOCATION_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_ad_signals_title), getString(R.string.feature_ad_signals_desc),
                Prefs.KEY_AD_SIGNALS_BLOCKER, Prefs.DEFAULT_AD_SIGNALS_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_url_sanitizer_title), getString(R.string.feature_url_sanitizer_desc),
                Prefs.KEY_URL_SANITIZER_BLOCKER, Prefs.DEFAULT_URL_SANITIZER_BLOCKER));

        list.add(FeatureItem.header(getString(R.string.section_feed)));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_hide_live_title), getString(R.string.feature_hide_live_desc),
                Prefs.KEY_HIDE_LIVE, Prefs.DEFAULT_HIDE_LIVE));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_hide_story_title), getString(R.string.feature_hide_story_desc),
                Prefs.KEY_HIDE_STORY, Prefs.DEFAULT_HIDE_STORY));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_hide_shop_title), getString(R.string.feature_hide_shop_desc),
                Prefs.KEY_HIDE_SHOP, Prefs.DEFAULT_HIDE_SHOP));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_hide_image_title), getString(R.string.feature_hide_image_desc),
                Prefs.KEY_HIDE_IMAGE, Prefs.DEFAULT_HIDE_IMAGE));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_hide_music_title), getString(R.string.feature_hide_music_desc),
                Prefs.KEY_HIDE_PROMOTED_MUSIC, Prefs.DEFAULT_HIDE_PROMOTED_MUSIC));
        list.add(numberAction(R.string.feature_max_duration_title, Prefs.KEY_MAX_DURATION_SEC, Prefs.DEFAULT_MAX_DURATION_SEC));
        list.add(numberAction(R.string.feature_min_views_title, Prefs.KEY_MIN_VIEWS, Prefs.DEFAULT_MIN_VIEWS));
        list.add(numberAction(R.string.feature_min_likes_title, Prefs.KEY_MIN_LIKES, Prefs.DEFAULT_MIN_LIKES));
        list.add(textAction(R.string.feature_blocklist_title, Prefs.KEY_CAPTION_BLOCKLIST, Prefs.DEFAULT_CAPTION_BLOCKLIST));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_stop_loop_title), getString(R.string.feature_stop_loop_desc),
                Prefs.KEY_STOP_VIDEO_LOOPING, Prefs.DEFAULT_STOP_VIDEO_LOOPING));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_screen_capture_title), getString(R.string.feature_screen_capture_desc),
                Prefs.KEY_SCREEN_CAPTURE_BLOCKER, Prefs.DEFAULT_SCREEN_CAPTURE_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_disable_login_title), getString(R.string.feature_disable_login_desc),
                Prefs.KEY_DISABLE_LOGIN, Prefs.DEFAULT_DISABLE_LOGIN));

        list.add(FeatureItem.header(getString(R.string.section_ui)));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_font_style_title), getString(R.string.feature_font_style_desc),
                Prefs.KEY_FONT_STYLE_BLOCKER, Prefs.DEFAULT_FONT_STYLE_BLOCKER));

        list.add(FeatureItem.header(getString(R.string.section_media)));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_watermark_title), getString(R.string.feature_watermark_desc),
                Prefs.KEY_WATERMARK_BLOCKER, Prefs.DEFAULT_WATERMARK_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_photo_watermark_title), getString(R.string.feature_photo_watermark_desc),
                Prefs.KEY_PHOTO_WATERMARK_BLOCKER, Prefs.DEFAULT_PHOTO_WATERMARK_BLOCKER));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_download_unlock_title), getString(R.string.feature_download_unlock_desc),
                Prefs.KEY_DOWNLOAD_UNLOCK_BLOCKER, Prefs.DEFAULT_DOWNLOAD_UNLOCK_BLOCKER));

        list.add(FeatureItem.header(getString(R.string.section_streak)));
        list.add(FeatureItem.toggle(
                getString(R.string.feature_streak_title), getString(R.string.feature_streak_desc),
                Prefs.KEY_STREAK_ENABLED, Prefs.DEFAULT_STREAK_ENABLED,
                checked -> StreakReminderScheduler.setEnabled(FeaturesActivity.this, checked)));

        int hour = StreakReminderScheduler.getHour(this);
        int minute = StreakReminderScheduler.getMinute(this);
        timeItem = FeatureItem.action(getString(R.string.feature_streak_time_title),
                String.format("%02d:%02d", hour, minute), this::showTimePicker);
        list.add(timeItem);
        timeItemPosition = list.size() - 1;

        return list;
    }

    private String numberLabel(long value) {
        return value > 0 ? String.valueOf(value) : getString(R.string.value_disabled);
    }

    private String textLabel(String value) {
        return value == null || value.trim().isEmpty() ? getString(R.string.value_disabled) : value;
    }

    private FeatureItem numberAction(int titleRes, String key, long defaultValue) {
        FeatureItem[] holder = new FeatureItem[1];
        holder[0] = FeatureItem.action(getString(titleRes),
                numberLabel(AppPrefs.getLong(this, key, defaultValue)),
                () -> showInputDialog(titleRes, String.valueOf(AppPrefs.getLong(this, key, defaultValue)),
                        InputType.TYPE_CLASS_NUMBER, input -> {
                            long parsed;
                            try {
                                parsed = input.isEmpty() ? 0 : Long.parseLong(input);
                            } catch (NumberFormatException e) {
                                parsed = 0;
                            }
                            AppPrefs.putLong(this, key, Math.max(0, parsed));
                            holder[0].value = numberLabel(Math.max(0, parsed));
                            adapter.notifyItemChanged(items.indexOf(holder[0]));
                        }));
        return holder[0];
    }

    private FeatureItem textAction(int titleRes, String key, String defaultValue) {
        FeatureItem[] holder = new FeatureItem[1];
        holder[0] = FeatureItem.action(getString(titleRes),
                textLabel(AppPrefs.getString(this, key, defaultValue)),
                () -> showInputDialog(titleRes, AppPrefs.getString(this, key, defaultValue),
                        InputType.TYPE_CLASS_TEXT, input -> {
                            AppPrefs.putString(this, key, input.trim());
                            holder[0].value = textLabel(input);
                            adapter.notifyItemChanged(items.indexOf(holder[0]));
                        }));
        return holder[0];
    }

    private interface OnInput {
        void onInput(String value);
    }

    private void showInputDialog(int titleRes, String current, int inputType, OnInput onInput) {
        EditText input = new EditText(this);
        input.setInputType(inputType);
        if (inputType == InputType.TYPE_CLASS_TEXT) input.setHint(R.string.feature_blocklist_hint);
        input.setText("0".equals(current) ? "" : current);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(R.string.feed_filter_note)
                .setView(container)
                .setPositiveButton(R.string.dialog_ok, (d, w) -> onInput.onInput(input.getText().toString()))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showTimePicker() {
        int hour = StreakReminderScheduler.getHour(this);
        int minute = StreakReminderScheduler.getMinute(this);
        new TimePickerDialog(this, (view, h, m) -> {
            StreakReminderScheduler.setTime(this, h, m);
            timeItem.value = String.format("%02d:%02d", h, m);
            adapter.notifyItemChanged(timeItemPosition);
        }, hour, minute, true).show();
    }
}
