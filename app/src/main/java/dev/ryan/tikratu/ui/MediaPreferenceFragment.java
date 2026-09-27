package dev.ryan.tikratu.ui;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import dev.ryan.tikratu.R;

public class MediaPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_media, rootKey);
    }
}
