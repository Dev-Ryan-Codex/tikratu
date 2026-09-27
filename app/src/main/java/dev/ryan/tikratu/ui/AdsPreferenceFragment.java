package dev.ryan.tikratu.ui;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import dev.ryan.tikratu.R;

/**
 * Cada SwitchPreferenceCompat se persiste solo (androidx.preference lo hace
 * automático) en el archivo de SharedPreferences por defecto de la app, que
 * es el mismo archivo que Module.java lee con XSharedPreferences dentro del
 * proceso de TikTok. No hace falta código extra acá.
 */
public class AdsPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_ads, rootKey);
    }
}
