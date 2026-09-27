package dev.ryan.tikratu;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import dev.ryan.tikratu.streak.StreakReminderScheduler;

public class MainActivity extends Activity {

    private Switch streakSwitch;
    private Button pickTimeButton;
    private TextView timeLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        streakSwitch = findViewById(R.id.streak_switch);
        pickTimeButton = findViewById(R.id.pick_time_button);
        timeLabel = findViewById(R.id.time_label);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        boolean enabled = StreakReminderScheduler.isEnabled(this);
        streakSwitch.setChecked(enabled);
        updateTimeLabel();

        streakSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if (isChecked) {
                    StreakReminderScheduler.schedule(MainActivity.this,
                            StreakReminderScheduler.getHour(MainActivity.this),
                            StreakReminderScheduler.getMinute(MainActivity.this));
                } else {
                    StreakReminderScheduler.cancel(MainActivity.this);
                }
            }
        });

        pickTimeButton.setOnClickListener(v -> {
            int hour = StreakReminderScheduler.getHour(MainActivity.this);
            int minute = StreakReminderScheduler.getMinute(MainActivity.this);
            new TimePickerDialog(MainActivity.this, (view, h, m) -> {
                StreakReminderScheduler.schedule(MainActivity.this, h, m);
                streakSwitch.setChecked(true);
                updateTimeLabel();
            }, hour, minute, true).show();
        });
    }

    private void updateTimeLabel() {
        int hour = StreakReminderScheduler.getHour(this);
        int minute = StreakReminderScheduler.getMinute(this);
        timeLabel.setText(String.format("Hora del recordatorio: %02d:%02d", hour, minute));
    }
}
