package dev.ryan.tikratu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dev.ryan.tikratu.ui.SettingsActivity;
import dev.ryan.tikratu.utils.StatusChecker;

public class MainActivity extends AppCompatActivity {

    private static final String TIKTOK_PACKAGE = "com.zhiliaoapp.musically";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        updateStatusCard();

        findViewById(R.id.category_ads).setOnClickListener(v -> openCategory(SettingsActivity.CATEGORY_ADS));
        findViewById(R.id.category_streak).setOnClickListener(v -> openCategory(SettingsActivity.CATEGORY_STREAK));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusCard();
    }

    private void openCategory(String category) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_CATEGORY, category);
        startActivity(intent);
    }

    private void updateStatusCard() {
        TextView moduleStatus = findViewById(R.id.module_status);
        TextView tiktokVersion = findViewById(R.id.tiktok_version);

        boolean active = StatusChecker.isModuleActive();
        moduleStatus.setText(active
                ? "Módulo activo (LSPosed lo está cargando)"
                : "Módulo inactivo — activalo en LSPosed Manager y reiniciá esta app");

        tiktokVersion.setText(getTikTokVersionLabel());
    }

    private String getTikTokVersionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(TIKTOK_PACKAGE, 0);
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode()
                    : info.versionCode;
            return "TikTok instalado: " + info.versionName + " (" + versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "TikTok no está instalado en este dispositivo";
        }
    }
}
