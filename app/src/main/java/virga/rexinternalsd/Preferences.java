package virga.rexinternalsd;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Preferences extends AppCompatActivity {
    public static Context context;
    public static SharedPreferences prefs;

    // Ganti URL di bawah ini sesuai link milikmu
    private static final String URL_SOURCE_CODE = "https://github.com/pylerSM/XInternalSD";
    private static final String URL_CONTRIBUTE  = "https://github.com/pylerSM/XInternalSD/issues";
    private static final String URL_SUBSCRIBE   = "https://youtube.com/@void-vfs";
    private static final String URL_DONATE      = "https://saweria.co/voidvvfs";

    private static final String CHANGELOG_TEXT =
            "• Automatic dark mode following system theme\n" +
            "• Added Changelog dialog in Information tab\n" +
            "• Material 3 UI & Android 16 / LSPosed compatibility rebuild";

    private NestedScrollView pageHome, pageInfo;
    private MaterialSwitch switchCustomPath, switchEnableAll, switchSystemApps, switchFullAccess, switchShowIcon;
    private TextView tvSdPathValue, btnEnableApps, btnDisableApps;
    private LinearLayout navHome, navInfo, navLanguage;

    private CharSequence[] appNamesList = new CharSequence[0];
    private CharSequence[] packageNamesList = new CharSequence[0];
    private boolean isAppsLoaded = false;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        context = getApplicationContext();
        // Harus MODE_WORLD_READABLE (di-hook oleh LSPosed/Vector) supaya
        // XSharedPreferences di proses aplikasi target bisa baca setting.
        // Nama file = nama default PreferenceManager -> <package>_preferences
        try {
            prefs = context.getSharedPreferences(
                    context.getPackageName() + "_preferences", Context.MODE_WORLD_READABLE);
        } catch (Throwable t) {
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }
        applySavedLanguage();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadSavedSettings();
        setupListeners();
        reloadAppsList();
    }

    @SuppressWarnings("deprecation")
    private void applySavedLanguage() {
        String langCode = prefs.getString("app_language", "");
        Locale locale;
        if (langCode.isEmpty()) {
            locale = Resources.getSystem().getConfiguration().getLocales().get(0);
        } else if (langCode.contains("-r")) {
            String[] parts = langCode.split("-r");
            locale = new Locale(parts[0], parts[1]);
        } else {
            locale = new Locale(langCode);
        }
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private void initViews() {
        pageHome = findViewById(R.id.page_home);
        pageInfo = findViewById(R.id.page_info);

        switchCustomPath = findViewById(R.id.switch_custom_path);
        switchEnableAll = findViewById(R.id.switch_enable_all);
        switchSystemApps = findViewById(R.id.switch_system_apps);
        switchFullAccess = findViewById(R.id.switch_full_access);
        switchShowIcon = findViewById(R.id.switch_show_icon);

        tvSdPathValue = findViewById(R.id.tv_sd_path_value);
        btnEnableApps = findViewById(R.id.btn_enable_apps);
        btnDisableApps = findViewById(R.id.btn_disable_apps);

        navHome = findViewById(R.id.nav_home);
        navInfo = findViewById(R.id.nav_info);
        navLanguage = findViewById(R.id.nav_language);

        TextView tvVersion = findViewById(R.id.tv_app_version);
        if (tvVersion != null) {
            tvVersion.setText("Rebuild by Virga • v" + BuildConfig.VERSION_NAME);
        }
    }

    private void loadSavedSettings() {
        switchCustomPath.setChecked(prefs.getBoolean("custom_internal_sd", true));
        switchEnableAll.setChecked(prefs.getBoolean("enable_for_all_apps", false));
        switchSystemApps.setChecked(prefs.getBoolean("include_system_apps", false));
        switchFullAccess.setChecked(prefs.getBoolean("external_sdcard_full_access", true));
        switchShowIcon.setChecked(prefs.getBoolean("show_app_icon", true));

        String internalSd = prefs.getString("internal_sdcard_path", "");
        if (!internalSd.isEmpty()) {
            internalSd = Common.appendFileSeparator(internalSd);
            tvSdPathValue.setText(internalSd);
        } else {
            String externalSd = detectExternalSd();
            if (externalSd != null && !externalSd.isEmpty()) {
                externalSd = Common.appendFileSeparator(externalSd);
                tvSdPathValue.setText(externalSd);
                prefs.edit().putString("internal_sdcard_path", externalSd).apply();
            } else {
                tvSdPathValue.setText(R.string.enter_internal_sdcard_path);
            }
        }
    }

    private String detectExternalSd() {
        File[] dirs = context.getExternalMediaDirs();
        for (File dir : dirs) {
            if (dir != null && dir.exists() && Environment.isExternalStorageRemovable(dir)) {
                String absolutePath = dir.getAbsolutePath();
                int end = absolutePath.indexOf("/Android/");
                if (end > 0) {
                    return absolutePath.substring(0, end);
                }
            }
        }
        return null;
    }

    private void setupListeners() {
        // Settings Listeners
        findViewById(R.id.row_custom_path).setOnClickListener(v -> switchCustomPath.toggle());
        switchCustomPath.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("custom_internal_sd", isChecked).apply());

        findViewById(R.id.row_sd_path).setOnClickListener(v -> showEditPathDialog());

        findViewById(R.id.row_enable_all).setOnClickListener(v -> switchEnableAll.toggle());
        switchEnableAll.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("enable_for_all_apps", isChecked).apply());

        btnEnableApps.setOnClickListener(v -> showMultiSelectAppsDialog("enable_for_apps", getString(R.string.enable_for_apps)));
        btnDisableApps.setOnClickListener(v -> showMultiSelectAppsDialog("disable_for_apps", getString(R.string.disable_for_apps)));

        findViewById(R.id.row_system_apps).setOnClickListener(v -> switchSystemApps.toggle());
        switchSystemApps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("include_system_apps", isChecked).apply();
            reloadAppsList();
        });

        findViewById(R.id.row_full_access).setOnClickListener(v -> switchFullAccess.toggle());
        switchFullAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("external_sdcard_full_access", isChecked).apply();
            Toast.makeText(context, R.string.reboot_required, Toast.LENGTH_LONG).show();
        });

        findViewById(R.id.row_show_icon).setOnClickListener(v -> switchShowIcon.toggle());
        switchShowIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("show_app_icon", isChecked).apply();
            PackageManager pm = context.getPackageManager();
            int state = isChecked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            ComponentName alias = new ComponentName(context, BuildConfig.APPLICATION_ID + ".Settings");
            pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
        });

        // Information Page Listeners
        findViewById(R.id.btn_info_changelog).setOnClickListener(v -> showChangelogDialog());
        findViewById(R.id.btn_info_source).setOnClickListener(v -> openUrl(URL_SOURCE_CODE));
        findViewById(R.id.btn_info_contribute).setOnClickListener(v -> openUrl(URL_CONTRIBUTE));
        findViewById(R.id.btn_info_subscribe).setOnClickListener(v -> openUrl(URL_SUBSCRIBE));
        findViewById(R.id.btn_info_donate).setOnClickListener(v -> openUrl(URL_DONATE));

        // Bottom Floating Pill Navigation Listeners
        navHome.setOnClickListener(v -> {
            pageHome.setVisibility(View.VISIBLE);
            pageInfo.setVisibility(View.GONE);
            setActiveNav(navHome);
        });

        navInfo.setOnClickListener(v -> {
            pageHome.setVisibility(View.GONE);
            pageInfo.setVisibility(View.VISIBLE);
            setActiveNav(navInfo);
        });

        navLanguage.setOnClickListener(v -> {
            setActiveNav(navLanguage);
            showLanguageDialog();
        });
    }

    private void showChangelogDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Changelog (v" + BuildConfig.VERSION_NAME + ")")
                .setMessage(CHANGELOG_TEXT)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Browser not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setActiveNav(LinearLayout activeTab) {
        navHome.setBackground(null);
        navInfo.setBackground(null);
        navLanguage.setBackground(null);
        activeTab.setBackgroundResource(R.drawable.bg_nav_pill_active);
    }

    private void showEditPathDialog() {
        final EditText input = new EditText(this);
        String currentPath = prefs.getString("internal_sdcard_path", "");
        input.setText(currentPath);
        input.setHint("/storage/9C33-6BBD/");
        input.setSingleLine(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin / 2, margin, 0);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.internal_sdcard_path)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newPath = input.getText().toString().trim();
                    if (newPath.isEmpty()) {
                        tvSdPathValue.setText(R.string.enter_internal_sdcard_path);
                        prefs.edit().putString("internal_sdcard_path", "").apply();
                    } else {
                        newPath = Common.appendFileSeparator(newPath);
                        tvSdPathValue.setText(newPath);
                        prefs.edit().putString("internal_sdcard_path", newPath).apply();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showMultiSelectAppsDialog(String prefKey, String title) {
        if (!isAppsLoaded) {
            Toast.makeText(this, "Loading apps...", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> savedSet = prefs.getStringSet(prefKey, new HashSet<>());
        boolean[] checkedItems = new boolean[packageNamesList.length];
        for (int i = 0; i < packageNamesList.length; i++) {
            checkedItems[i] = savedSet.contains(packageNamesList[i].toString());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMultiChoiceItems(appNamesList, checkedItems, (dialog, which, isChecked) ->
                        checkedItems[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Set<String> newSet = new HashSet<>();
                    for (int i = 0; i < packageNamesList.length; i++) {
                        if (checkedItems[i]) {
                            newSet.add(packageNamesList[i].toString());
                        }
                    }
                    prefs.edit().putStringSet(prefKey, newSet).apply();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLanguageDialog() {
        final String[] langLabels = {
                "System Default", "English", "Bahasa Indonesia", "Čeština", "Deutsch",
                "Español", "فارسی", "हिन्दी", "Magyar", "Italiano",
                "日本語", "한국어", "Nederlands", "Português", "Русский",
                "Slovenčina", "简体中文", "繁體中文"
        };
        final String[] langCodes = {
                "", "en", "in", "cs", "de",
                "es", "fa", "hi", "hu", "it",
                "ja", "ko", "nl", "pt", "ru",
                "sk", "zh-rCN", "zh-rTW"
        };

        String currentLang = prefs.getString("app_language", "");
        int checkedItem = 0;
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(currentLang)) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Language")
                .setSingleChoiceItems(langLabels, checkedItem, (dialog, which) -> {
                    prefs.edit().putString("app_language", langCodes[which]).apply();
                    dialog.dismiss();
                    recreate();
                })
                .setOnDismissListener(d -> {
                    // Kembalikan sorotan pill ke halaman yang sedang terbuka
                    if (pageInfo.getVisibility() == View.VISIBLE) {
                        setActiveNav(navInfo);
                    } else {
                        setActiveNav(navHome);
                    }
                })
                .show();
    }

    public boolean isAllowedApp(ApplicationInfo appInfo) {
        boolean includeSystemApps = prefs.getBoolean("include_system_apps", false);
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && !includeSystemApps) {
            return false;
        }
        return !Arrays.asList(Common.MTP_APPS).contains(appInfo.packageName);
    }

    public void reloadAppsList() {
        isAppsLoaded = false;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<String[]> sortedApps = new ArrayList<>();

            for (ApplicationInfo app : packages) {
                if (isAllowedApp(app)) {
                    sortedApps.add(new String[]{
                            app.packageName,
                            app.loadLabel(pm).toString()
                    });
                }
            }

            Collections.sort(sortedApps, (entry1, entry2) -> entry1[1].compareToIgnoreCase(entry2[1]));

            List<CharSequence> names = new ArrayList<>();
            List<CharSequence> pkgs = new ArrayList<>();
            for (String[] entry : sortedApps) {
                names.add(entry[1] + "\n(" + entry[0] + ")");
                pkgs.add(entry[0]);
            }

            runOnUiThread(() -> {
                appNamesList = names.toArray(new CharSequence[0]);
                packageNamesList = pkgs.toArray(new CharSequence[0]);
                isAppsLoaded = true;
            });
        });
    }
}
