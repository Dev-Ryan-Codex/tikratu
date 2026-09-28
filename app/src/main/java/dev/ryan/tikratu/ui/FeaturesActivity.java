package dev.ryan.tikratu.ui;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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

        SharedPreferences prefs = AppPrefs.get(this);
        items = buildItems(prefs);

        recyclerFeatures = findViewById(R.id.recycler_features);
        settingsContainer = findViewById(R.id.settings_container);
        infoContainer = findViewById(R.id.info_container);

        RecyclerView recycler = (RecyclerView) recyclerFeatures;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeatureAdapter(items, prefs);
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
    }

    private void showTab(View toShow, int titleRes) {
        recyclerFeatures.setVisibility(toShow == recyclerFeatures ? View.VISIBLE : View.GONE);
        settingsContainer.setVisibility(toShow == settingsContainer ? View.VISIBLE : View.GONE);
        infoContainer.setVisibility(toShow == infoContainer ? View.VISIBLE : View.GONE);
        screenTitle.setText(titleRes);
    }

    private List<FeatureItem> buildItems(SharedPreferences prefs) {
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
