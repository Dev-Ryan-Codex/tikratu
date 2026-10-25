package dev.ryan.tikratu;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import dev.ryan.tikratu.streak.StreakReminderScheduler;
import dev.ryan.tikratu.ui.FeatureAdapter;
import dev.ryan.tikratu.ui.FeatureItem;
import dev.ryan.tikratu.utils.AppPrefs;
import dev.ryan.tikratu.utils.Prefs;
import dev.ryan.tikratu.utils.StatusChecker;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String REPO_URL = "https://github.com/Dev-Ryan-Codex/tikratu";
    private static final String[] SPEED_VALUES = {"0.5", "0.75", Prefs.DEFAULT_PLAYBACK_SPEED, "1.25", "1.5", "2.0", "3.0"};
    private static final String TIKTOK_PACKAGE = "com.zhiliaoapp.musically";
    private FeatureAdapter adapter;
    private FeatureItem fontItem;
    private View infoContainer;
    private List<FeatureItem> items;
    private View recyclerFeatures;
    private FeatureItem regionItem;
    private TextView screenTitle;
    private View settingsContainer;
    private FeatureItem speedItem;
    private FeatureItem statusItem;
    private FeatureItem timeItem;
    private int timeItemPosition;

        interface OnInput {
        void onInput(String str);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
        }
        this.screenTitle = (TextView) findViewById(R.id.tv_screen_title);
        this.recyclerFeatures = findViewById(R.id.recycler_features);
        this.settingsContainer = findViewById(R.id.settings_container);
        this.infoContainer = findViewById(R.id.info_container);
        this.items = buildItems();
        RecyclerView recycler = (RecyclerView) this.recyclerFeatures;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        this.adapter = new FeatureAdapter(this.items, this);
        recycler.setAdapter(this.adapter);
        findViewById(R.id.action_repo).setOnClickListener((view) -> onCreateImpl0(view));
        BottomNavigationView bottomNav = (BottomNavigationView) findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener((menuItem) -> onCreateImpl1(menuItem));
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

        public void onCreateImpl0(View v) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(REPO_URL)));
    }

        public boolean onCreateImpl1(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            showTab(this.recyclerFeatures, R.string.app_name);
            return true;
        }
        if (id == R.id.nav_settings) {
            showTab(this.settingsContainer, R.string.nav_settings);
            return true;
        }
        if (id == R.id.nav_info) {
            showTab(this.infoContainer, R.string.nav_info);
            return true;
        }
        return true;
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        String str;
        if (this.statusItem == null || this.adapter == null) {
            return;
        }
        FeatureItem featureItem = this.statusItem;
        if (StatusChecker.isModuleActive()) {
            str = "Módulo activo (LSPosed lo está cargando)";
        } else {
            str = "Módulo inactivo — activalo en LSPosed Manager";
        }
        featureItem.statusModule = str;
        this.statusItem.statusVersion = getTikTokVersionLabel();
        this.adapter.notifyItemChanged(0);
    }

    private void showTab(View toShow, int titleRes) {
        this.recyclerFeatures.setVisibility(toShow == this.recyclerFeatures ? 0 : 8);
        this.settingsContainer.setVisibility(toShow == this.settingsContainer ? 0 : 8);
        this.infoContainer.setVisibility(toShow != this.infoContainer ? 8 : 0);
        this.screenTitle.setText(titleRes);
    }

    private List<FeatureItem> buildItems() {
        String str;
        List<FeatureItem> list = new ArrayList<>();
        if (StatusChecker.isModuleActive()) {
            str = "Módulo activo (LSPosed lo está cargando)";
        } else {
            str = "Módulo inactivo — activalo en LSPosed Manager";
        }
        this.statusItem = FeatureItem.status(str, getTikTokVersionLabel(), () -> openTikTok());
        list.add(this.statusItem);
        list.add(FeatureItem.header(getString(R.string.section_ads)));
        list.add(FeatureItem.toggle(getString(R.string.feature_ad_blocker_title), getString(R.string.feature_ad_blocker_desc), Prefs.KEY_AD_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_ads_id_title), getString(R.string.feature_ads_id_desc), Prefs.KEY_ADS_ID_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_ads_metadata_title), getString(R.string.feature_ads_metadata_desc), Prefs.KEY_ADS_METADATA_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_location_title), getString(R.string.feature_location_desc), Prefs.KEY_LOCATION_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_ad_signals_title), getString(R.string.feature_ad_signals_desc), Prefs.KEY_AD_SIGNALS_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_url_sanitizer_title), getString(R.string.feature_url_sanitizer_desc), Prefs.KEY_URL_SANITIZER_BLOCKER, true));
        list.add(FeatureItem.header(getString(R.string.section_feed)));
        list.add(FeatureItem.toggle(getString(R.string.feature_hide_live_title), getString(R.string.feature_hide_live_desc), Prefs.KEY_HIDE_LIVE, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_hide_story_title), getString(R.string.feature_hide_story_desc), Prefs.KEY_HIDE_STORY, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_hide_shop_title), getString(R.string.feature_hide_shop_desc), Prefs.KEY_HIDE_SHOP, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_hide_image_title), getString(R.string.feature_hide_image_desc), Prefs.KEY_HIDE_IMAGE, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_hide_music_title), getString(R.string.feature_hide_music_desc), Prefs.KEY_HIDE_PROMOTED_MUSIC, false));
        list.add(numberAction(R.string.feature_max_duration_title, Prefs.KEY_MAX_DURATION_SEC, 0L));
        list.add(numberAction(R.string.feature_min_views_title, Prefs.KEY_MIN_VIEWS, 0L));
        list.add(numberAction(R.string.feature_min_likes_title, Prefs.KEY_MIN_LIKES, 0L));
        list.add(textAction(R.string.feature_blocklist_title, Prefs.KEY_CAPTION_BLOCKLIST, ""));
        list.add(FeatureItem.toggle(getString(R.string.feature_stop_loop_title), getString(R.string.feature_stop_loop_desc), Prefs.KEY_STOP_VIDEO_LOOPING, false));
        list.add(FeatureItem.header(getString(R.string.section_media)));
        list.add(FeatureItem.toggle(getString(R.string.feature_watermark_title), getString(R.string.feature_watermark_desc), Prefs.KEY_WATERMARK_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_photo_watermark_title), getString(R.string.feature_photo_watermark_desc), Prefs.KEY_PHOTO_WATERMARK_BLOCKER, true));
        list.add(FeatureItem.toggle(getString(R.string.feature_download_unlock_title), getString(R.string.feature_download_unlock_desc), Prefs.KEY_DOWNLOAD_UNLOCK_BLOCKER, true));
        list.add(FeatureItem.header(getString(R.string.section_ui)));
        list.add(FeatureItem.toggle(getString(R.string.feature_hide_tako_title), getString(R.string.feature_hide_tako_desc), Prefs.KEY_HIDE_TAKO_AI, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_screen_capture_title), getString(R.string.feature_screen_capture_desc), Prefs.KEY_SCREEN_CAPTURE_BLOCKER, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_disable_login_title), getString(R.string.feature_disable_login_desc), Prefs.KEY_DISABLE_LOGIN, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_google_login_title), getString(R.string.feature_google_login_desc), Prefs.KEY_GOOGLE_LOGIN_FIX, false));
        this.fontItem = FeatureItem.action(getString(R.string.feature_font_style_title), getString(R.string.feature_font_style_desc), currentFontLabel(), () -> showFontPicker());
        list.add(this.fontItem);
        this.speedItem = FeatureItem.action(getString(R.string.feature_playback_speed_title), getString(R.string.feature_playback_speed_desc), currentSpeedLabel(), () -> showSpeedPicker());
        list.add(this.speedItem);
        list.add(FeatureItem.header(getString(R.string.section_region)));
        this.regionItem = FeatureItem.action(getString(R.string.feature_region_title), getString(R.string.feature_region_desc), currentRegionLabel(), () -> showRegionPicker());
        list.add(this.regionItem);
        list.add(FeatureItem.toggle(getString(R.string.feature_force_region_title), getString(R.string.feature_force_region_desc), Prefs.KEY_FORCE_REGION, false));
        list.add(FeatureItem.toggle(getString(R.string.feature_sim_region_title), getString(R.string.feature_sim_region_desc), Prefs.KEY_SIM_REGION, false));
        list.add(FeatureItem.header(getString(R.string.section_streak)));
        list.add(FeatureItem.toggle(getString(R.string.feature_streak_title), getString(R.string.feature_streak_desc), Prefs.KEY_STREAK_ENABLED, false, (z) -> buildItemsImpl2(z)));
        int hour = StreakReminderScheduler.getHour(this);
        int minute = StreakReminderScheduler.getMinute(this);
        this.timeItem = FeatureItem.action(getString(R.string.feature_streak_time_title), getString(R.string.feature_streak_time_desc), String.format("%02d:%02d", Integer.valueOf(hour), Integer.valueOf(minute)), () -> showTimePicker());
        list.add(this.timeItem);
        this.timeItemPosition = list.size() - 1;
        return list;
    }

        public void buildItemsImpl2(boolean checked) {
        StreakReminderScheduler.setEnabled(this, checked);
    }

        public void openTikTok() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(TIKTOK_PACKAGE);
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, R.string.tiktok_not_installed, 0).show();
        }
    }

    private String getTikTokVersionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(TIKTOK_PACKAGE, 0);
            long versionCode = info.getLongVersionCode();
            return "TikTok instalado: " + info.versionName + " (" + versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.tiktok_not_installed);
        }
    }

    private String numberLabel(long value) {
        return value > 0 ? String.valueOf(value) : getString(R.string.value_disabled);
    }

    private String textLabel(String value) {
        return (value == null || value.trim().isEmpty()) ? getString(R.string.value_disabled) : value;
    }

    private FeatureItem numberAction(final int titleRes, final String key, final long defaultValue) {
        final FeatureItem[] holder = new FeatureItem[1];
        holder[0] = FeatureItem.action(getString(titleRes), numberLabel(AppPrefs.getLong(this, key, defaultValue)), () -> numberActionImpl4(titleRes, key, defaultValue, holder));
        return holder[0];
    }

        public void numberActionImpl4(int titleRes, final String key, long defaultValue, final FeatureItem[] holder) {
        showInputDialog(titleRes, String.valueOf(AppPrefs.getLong(this, key, defaultValue)), 2, (str) -> numberActionImpl3(key, holder, str));
    }

        public void numberActionImpl3(String key, FeatureItem[] holder, String input) {
        long parsed;
        try {
            parsed = input.isEmpty() ? 0L : Long.parseLong(input);
        } catch (NumberFormatException e) {
            parsed = 0;
        }
        AppPrefs.putLong(this, key, Math.max(0L, parsed));
        holder[0].value = numberLabel(Math.max(0L, parsed));
        this.adapter.notifyItemChanged(this.items.indexOf(holder[0]));
    }

    private FeatureItem textAction(final int titleRes, final String key, final String defaultValue) {
        final FeatureItem[] holder = new FeatureItem[1];
        holder[0] = FeatureItem.action(getString(titleRes), textLabel(AppPrefs.getString(this, key, defaultValue)), () -> textActionImpl6(titleRes, key, defaultValue, holder));
        return holder[0];
    }

        public void textActionImpl6(int titleRes, final String key, String defaultValue, final FeatureItem[] holder) {
        showInputDialog(titleRes, AppPrefs.getString(this, key, defaultValue), 1, (str) -> textActionImpl5(key, holder, str));
    }

        public void textActionImpl5(String key, FeatureItem[] holder, String input) {
        AppPrefs.putString(this, key, input.trim());
        holder[0].value = textLabel(input);
        this.adapter.notifyItemChanged(this.items.indexOf(holder[0]));
    }

    private void showInputDialog(int titleRes, String current, int inputType, final OnInput onInput) {
        final EditText input = new EditText(this);
        input.setInputType(inputType);
        if (inputType == 1) {
            input.setHint(R.string.feature_blocklist_hint);
        }
        input.setText("0".equals(current) ? "" : current);
        int pad = (int) (getResources().getDisplayMetrics().density * 20.0f);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);
        new AlertDialog.Builder(this).setTitle(titleRes).setMessage(R.string.feed_filter_note).setView(container).setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() { // from class: dev.ryan.tikratu.MainActivity$$ExternalSyntheticLambda7
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                onInput.onInput(input.getText().toString());
            }
        }).setNegativeButton(R.string.dialog_cancel, (DialogInterface.OnClickListener) null).show();
    }

    private String currentFontLabel() {
        if (!AppPrefs.getBoolean(this, Prefs.KEY_FONT_STYLE_BLOCKER, false)) {
            return getString(R.string.font_row_default);
        }
        String choice = AppPrefs.getString(this, Prefs.KEY_FONT_CHOICE, "system");
        if (Prefs.FONT_SERIF.equals(choice)) return getString(R.string.font_row_serif);
        if (Prefs.FONT_MONOSPACE.equals(choice)) return getString(R.string.font_row_monospace);
        if (Prefs.FONT_IOS.equals(choice)) return getString(R.string.font_row_ios);
        if (Prefs.FONT_HYPEROS.equals(choice)) return getString(R.string.font_row_hyperos);
        return getString(R.string.font_row_system);
    }

        public void showFontPicker() {
        final String[] choiceKeys = {"default", "system", Prefs.FONT_SERIF, Prefs.FONT_MONOSPACE, Prefs.FONT_IOS, Prefs.FONT_HYPEROS};
        String[] labels = {getString(R.string.font_choice_default), getString(R.string.font_choice_system), getString(R.string.font_choice_serif), getString(R.string.font_choice_monospace), getString(R.string.font_choice_ios), getString(R.string.font_choice_hyperos)};
        boolean enabled = AppPrefs.getBoolean(this, Prefs.KEY_FONT_STYLE_BLOCKER, false);
        String current = enabled ? AppPrefs.getString(this, Prefs.KEY_FONT_CHOICE, "system") : "default";
        int checked = 0;
        for (int i = 0; i < choiceKeys.length; i++) {
            if (choiceKeys[i].equals(current)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this).setTitle(R.string.feature_font_style_title).setSingleChoiceItems(labels, checked, (dialogInterface, i2) -> showFontPickerImpl8(choiceKeys, dialogInterface, i2)).setNegativeButton(R.string.dialog_cancel, (DialogInterface.OnClickListener) null).show();
    }

        public void showFontPickerImpl8(String[] choiceKeys, DialogInterface dialog, int which) {
        if ("default".equals(choiceKeys[which])) {
            AppPrefs.putBoolean(this, Prefs.KEY_FONT_STYLE_BLOCKER, false);
        } else {
            AppPrefs.putBoolean(this, Prefs.KEY_FONT_STYLE_BLOCKER, true);
            AppPrefs.putString(this, Prefs.KEY_FONT_CHOICE, choiceKeys[which]);
        }
        this.fontItem.value = currentFontLabel();
        this.adapter.notifyItemChanged(this.items.indexOf(this.fontItem));
        dialog.dismiss();
    }

    private String currentRegionLabel() {
        String code = AppPrefs.getString(this, Prefs.KEY_REGION_CODE, "");
        if (code == null || code.trim().isEmpty()) {
            return getString(R.string.region_none);
        }
        String[] codes = getResources().getStringArray(R.array.region_codes);
        String[] names = getResources().getStringArray(R.array.region_names);
        for (int i = 0; i < codes.length && i < names.length; i++) {
            if (codes[i].equalsIgnoreCase(code)) {
                return names[i];
            }
        }
        return code.toUpperCase();
    }

        public void showRegionPicker() {
        final String[] codes = getResources().getStringArray(R.array.region_codes);
        String[] names = getResources().getStringArray(R.array.region_names);
        String[] labels = new String[names.length + 1];
        labels[0] = getString(R.string.region_none);
        System.arraycopy(names, 0, labels, 1, names.length);
        String current = AppPrefs.getString(this, Prefs.KEY_REGION_CODE, "");
        int checked = 0;
        if (current != null && !current.trim().isEmpty()) {
            for (int i = 0; i < codes.length; i++) {
                if (codes[i].equalsIgnoreCase(current)) {
                    checked = i + 1;
                    break;
                }
            }
        }
        new AlertDialog.Builder(this).setTitle(R.string.region_dialog_title).setSingleChoiceItems(labels, checked, (dialogInterface, i2) -> showRegionPickerImpl9(codes, dialogInterface, i2)).setNegativeButton(R.string.dialog_cancel, (DialogInterface.OnClickListener) null).show();
    }

        public void showRegionPickerImpl9(String[] codes, DialogInterface dialog, int which) {
        String code = which == 0 ? "" : codes[which - 1];
        AppPrefs.putString(this, Prefs.KEY_REGION_CODE, code);
        this.regionItem.value = currentRegionLabel();
        this.adapter.notifyItemChanged(this.items.indexOf(this.regionItem));
        dialog.dismiss();
    }

    private String speedLabel(String value) {
        if (value == null || value.trim().equals(Prefs.DEFAULT_PLAYBACK_SPEED)) {
            return getString(R.string.speed_normal);
        }
        return value.trim() + "x";
    }

    private String currentSpeedLabel() {
        return speedLabel(AppPrefs.getString(this, Prefs.KEY_PLAYBACK_SPEED, Prefs.DEFAULT_PLAYBACK_SPEED));
    }

        public void showSpeedPicker() {
        String[] labels = new String[SPEED_VALUES.length];
        for (int i = 0; i < SPEED_VALUES.length; i++) {
            labels[i] = speedLabel(SPEED_VALUES[i]);
        }
        String current = AppPrefs.getString(this, Prefs.KEY_PLAYBACK_SPEED, Prefs.DEFAULT_PLAYBACK_SPEED);
        int checked = 2;
        for (int i2 = 0; i2 < SPEED_VALUES.length; i2++) {
            if (SPEED_VALUES[i2].equals(current)) {
                checked = i2;
                break;
            }
        }
        new AlertDialog.Builder(this).setTitle(R.string.speed_dialog_title).setSingleChoiceItems(labels, checked, (dialogInterface, i3) -> showSpeedPickerImpl10(dialogInterface, i3)).setNegativeButton(R.string.dialog_cancel, (DialogInterface.OnClickListener) null).show();
    }

        public void showSpeedPickerImpl10(DialogInterface dialog, int which) {
        AppPrefs.putString(this, Prefs.KEY_PLAYBACK_SPEED, SPEED_VALUES[which]);
        this.speedItem.value = currentSpeedLabel();
        this.adapter.notifyItemChanged(this.items.indexOf(this.speedItem));
        dialog.dismiss();
    }

        public void showTimePicker() {
        int hour = StreakReminderScheduler.getHour(this);
        int minute = StreakReminderScheduler.getMinute(this);
        new TimePickerDialog(this, (timePicker, i, i2) -> showTimePickerImpl11(timePicker, i, i2), hour, minute, true).show();
    }

        public void showTimePickerImpl11(TimePicker view, int h, int m) {
        StreakReminderScheduler.setTime(this, h, m);
        this.timeItem.value = String.format("%02d:%02d", Integer.valueOf(h), Integer.valueOf(m));
        this.adapter.notifyItemChanged(this.timeItemPosition);
    }
}
