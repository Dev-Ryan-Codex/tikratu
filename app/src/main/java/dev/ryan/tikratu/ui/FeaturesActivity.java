package dev.ryan.tikratu.ui;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.ryan.tikratu.R;
import dev.ryan.tikratu.streak.StreakReminderScheduler;
import dev.ryan.tikratu.utils.AppPrefs;
import dev.ryan.tikratu.utils.Prefs;

/**
 * Pantalla única con todos los switches, agrupados por categoría con headers
 * (mismo patrón que la pantalla "Features" de InstaEclipse: una lista con
 * secciones, no una pantalla separada por categoría).
 */
public class FeaturesActivity extends Activity {

    private FeatureAdapter adapter;
    private List<FeatureItem> items;
    private FeatureItem timeItem;
    private int timeItemPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SharedPreferences prefs = AppPrefs.get(this);
        items = buildItems(prefs);

        RecyclerView recycler = findViewById(R.id.recycler_features);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeatureAdapter(items, prefs);
        recycler.setAdapter(adapter);
    }

    private List<FeatureItem> buildItems(SharedPreferences prefs) {
        List<FeatureItem> list = new ArrayList<>();

        list.add(FeatureItem.header(getString(R.string.section_ads)));
        list.add(FeatureItem.toggle(R.drawable.ic_block,
                getString(R.string.feature_ad_blocker_title), getString(R.string.feature_ad_blocker_desc),
                Prefs.KEY_AD_BLOCKER, Prefs.DEFAULT_AD_BLOCKER));
        list.add(FeatureItem.toggle(R.drawable.ic_block,
                getString(R.string.feature_ads_id_title), getString(R.string.feature_ads_id_desc),
                Prefs.KEY_ADS_ID_BLOCKER, Prefs.DEFAULT_ADS_ID_BLOCKER));
        list.add(FeatureItem.toggle(R.drawable.ic_block,
                getString(R.string.feature_ads_metadata_title), getString(R.string.feature_ads_metadata_desc),
                Prefs.KEY_ADS_METADATA_BLOCKER, Prefs.DEFAULT_ADS_METADATA_BLOCKER));
        list.add(FeatureItem.toggle(R.drawable.ic_block,
                getString(R.string.feature_location_title), getString(R.string.feature_location_desc),
                Prefs.KEY_LOCATION_BLOCKER, Prefs.DEFAULT_LOCATION_BLOCKER));
        list.add(FeatureItem.toggle(R.drawable.ic_block,
                getString(R.string.feature_ad_signals_title), getString(R.string.feature_ad_signals_desc),
                Prefs.KEY_AD_SIGNALS_BLOCKER, Prefs.DEFAULT_AD_SIGNALS_BLOCKER));

        list.add(FeatureItem.header(getString(R.string.section_media)));
        list.add(FeatureItem.toggle(R.drawable.ic_download,
                getString(R.string.feature_watermark_title), getString(R.string.feature_watermark_desc),
                Prefs.KEY_WATERMARK_BLOCKER, Prefs.DEFAULT_WATERMARK_BLOCKER));
        list.add(FeatureItem.toggle(R.drawable.ic_download,
                getString(R.string.feature_photo_watermark_title), getString(R.string.feature_photo_watermark_desc),
                Prefs.KEY_PHOTO_WATERMARK_BLOCKER, Prefs.DEFAULT_PHOTO_WATERMARK_BLOCKER));
        list.add(FeatureItem.toggle(R.drawable.ic_download,
                getString(R.string.feature_download_unlock_title), getString(R.string.feature_download_unlock_desc),
                Prefs.KEY_DOWNLOAD_UNLOCK_BLOCKER, Prefs.DEFAULT_DOWNLOAD_UNLOCK_BLOCKER));

        list.add(FeatureItem.header(getString(R.string.section_streak)));
        list.add(FeatureItem.toggle(R.drawable.ic_notifications,
                getString(R.string.feature_streak_title), getString(R.string.feature_streak_desc),
                Prefs.KEY_STREAK_ENABLED, Prefs.DEFAULT_STREAK_ENABLED,
                checked -> StreakReminderScheduler.setEnabled(FeaturesActivity.this, checked)));

        int hour = StreakReminderScheduler.getHour(this);
        int minute = StreakReminderScheduler.getMinute(this);
        timeItem = FeatureItem.action(R.drawable.ic_schedule, getString(R.string.feature_streak_time_title),
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
