package dev.ryan.tikratu.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import dev.ryan.tikratu.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY = "category";
    public static final String CATEGORY_ADS = "ads";
    public static final String CATEGORY_MEDIA = "media";
    public static final String CATEGORY_STREAK = "streak";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        String category = getIntent().getStringExtra(EXTRA_CATEGORY);
        Fragment fragment;
        String title;

        if (CATEGORY_STREAK.equals(category)) {
            fragment = new StreakPreferenceFragment();
            title = "Streak";
        } else if (CATEGORY_MEDIA.equals(category)) {
            fragment = new MediaPreferenceFragment();
            title = "Media";
        } else {
            fragment = new AdsPreferenceFragment();
            title = "Anuncios y tracking";
        }

        setTitle(title);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
