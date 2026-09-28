package dev.ryan.tikratu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import dev.ryan.tikratu.ui.FeaturesActivity;
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

        findViewById(R.id.open_tiktok_button).setOnClickListener(v -> openTikTok());
        findViewById(R.id.view_features_button).setOnClickListener(v ->
                startActivity(new Intent(this, FeaturesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusCard();
    }

    private void openTikTok() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(TIKTOK_PACKAGE);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, R.string.tiktok_not_installed, Toast.LENGTH_SHORT).show();
        }
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
            return getString(R.string.tiktok_not_installed);
        }
    }
}
