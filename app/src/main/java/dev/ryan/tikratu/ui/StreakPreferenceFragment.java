package dev.ryan.tikratu.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import dev.ryan.tikratu.R;
import dev.ryan.tikratu.streak.StreakReminderScheduler;
import dev.ryan.tikratu.utils.Prefs;

public class StreakPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_streak, rootKey);

        SwitchPreferenceCompat enabledPref = findPreference(Prefs.KEY_STREAK_ENABLED);
        Preference timePref = findPreference("pref_streak_time_picker");

        updateTimeSummary(timePref);

        if (enabledPref != null) {
            enabledPref.setOnPreferenceChangeListener((preference, newValue) -> {
                StreakReminderScheduler.setEnabled(requireContext(), (Boolean) newValue);
                return true;
            });
        }

        if (timePref != null) {
            timePref.setOnPreferenceClickListener(preference -> {
                int hour = StreakReminderScheduler.getHour(requireContext());
                int minute = StreakReminderScheduler.getMinute(requireContext());
                new TimePickerDialog(requireContext(), (view, h, m) -> {
                    StreakReminderScheduler.setTime(requireContext(), h, m);
                    updateTimeSummary(timePref);
                }, hour, minute, true).show();
                return true;
            });
        }
    }

    private void updateTimeSummary(Preference timePref) {
        if (timePref == null) return;
        int hour = StreakReminderScheduler.getHour(requireContext());
        int minute = StreakReminderScheduler.getMinute(requireContext());
        timePref.setSummary(String.format("%02d:%02d", hour, minute));
    }
}
