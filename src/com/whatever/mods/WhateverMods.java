package com.whatever.mods;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhateverMods {

    private static final String TAG = "WhateverMods";

    // ═══════════════════════════════════════════════════
    // ⚙️ YAHAN APNI SETTINGS — EDIT KARO
    // ═══════════════════════════════════════════════════

    // 🔑 Tera Supabase URL — key verify karne ke liye
    private static final String API_URL =
        "https://wdrksxlolrdtihopwojn.supabase.co/functions/v1/license/api/verify/initiate_v2";

    // 🔄 Update check URL (same Supabase pe, ya alag)
    private static final String UPDATE_URL =
        "https://wdrksxlolrdtihopwojn.supabase.co/functions/v1/license/api/app/register";

    // 🏷️ App ka naam — jo dialog mein dikhega
    private static final String APP_NAME = "Whatever Mods";

    // 📱 Teri current app version (yeh APK ka version hoga)
    private static final int CURRENT_VERSION = 1;

    // 🔗 Links — buttons ke liye
    private static final String GET_KEY_URL    = "https://wa.me/923000000000";         // teri web
    private static final String JOIN_US_URL    = "https://whatsapp.com/channel/XXXXX"; // whatsapp channel
    private static final String UPDATE_URL_BTN = "https://t.me/yourchannel";           // update link

    // 💾 Preferences
    private static final String PREF_NAME = "whatever_mods_prefs";
    private static final String PREF_KEY  = "saved_key";
    private static final String PREF_VER  = "seen_version";

    // ═══════════════════════════════════════════════════

    private final Context ctx;
    private final ExecutorService executor;
    private final Handler ui;

    public interface Callback {
        void onSuccess();
        void onFail(String msg);
    }

    public WhateverMods(Context c) {
        this.ctx = c.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.ui = new Handler(Looper.getMainLooper());
    }

    // 🎯 MAIN ENTRY POINT
    public static void inject(final Activity act) {
        if (act == null) return;
        final WhateverMods sdk = new WhateverMods(act);

        String savedKey = act.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY, null);

        if (savedKey != null && !savedKey.isEmpty()) {
            // Saved key hai → verify karo
            sdk.verifyKey(act, savedKey, new Callback() {
                @Override public void onSuccess() {
                    Log.d(TAG, "Saved key OK");
                    sdk.checkUpdate(act);  // update bhi check karo
                }
                @Override public void onFail(String msg) {
                    Log.d(TAG, "Saved key invalid");
                    sdk.showLicenseDialog(act);
                }
            });
        } else {
            sdk.showLicenseDialog(act);
        }
    }

    // ═══════════════════════════════════════════════════
    // 🎨 LICENSE DIALOG — poori UI
    // ═══════════════════════════════════════════════════
    private void showLicenseDialog(final Activity act) {
        ui.post(() -> {
            try {
                ScrollView scroll = new ScrollView(act);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(22), dp(24), dp(22), dp(24));

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#0A0A14"));
                bg.setCornerRadius(dp(18));
                bg.setStroke(dp(2), Color.parseColor("#8B5CF6"));
                root.setBackground(bg);
                scroll.addView(root);

                // 🏷️ Title
                TextView title = new TextView(act);
                title.setText("🔐 " + APP_NAME);
                title.setTextColor(Color.parseColor("#C4B5FD"));
                title.setTextSize(22);
                title.setTypeface(null, Typeface.BOLD);
                title.setGravity(Gravity.CENTER);
                root.addView(title);

                // 📝 Subtitle
                TextView sub = new TextView(act);
                sub.setText("Premium Unlocked Version");
                sub.setTextColor(Color.parseColor("#9CA3AF"));
                sub.setTextSize(12);
                sub.setGravity(Gravity.CENTER);
                sub.setPadding(0, dp(6), 0, dp(22));
                root.addView(sub);

                // 🔑 Input label
                TextView lbl = new TextView(act);
                lbl.setText("Enter License Key");
                lbl.setTextColor(Color.parseColor("#E5E7EB"));
                lbl.setTextSize(13);
                lbl.setPadding(0, 0, 0, dp(8));
                root.addView(lbl);

                // 🔑 Input field
                final EditText input = new EditText(act);
                input.setHint("XXXX-XXXX-XXXX");
                input.setHintTextColor(Color.parseColor("#6B7280"));
                input.setTextColor(Color.WHITE);
                input.setTextSize(15);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                input.setPadding(dp(14), dp(14), dp(14), dp(14));
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setColor(Color.parseColor("#14141F"));
                inputBg.setCornerRadius(dp(10));
                inputBg.setStroke(dp(1), Color.parseColor("#4C1D95"));
                input.setBackground(inputBg);
                root.addView(input);

                // 🔓 LOGIN button
                Button login = new Button(act);
                login.setText("UNLOCK");
                login.setTextColor(Color.WHITE);
                login.setTextSize(15);
                login.setTypeface(null, Typeface.BOLD);
                GradientDrawable btnBg = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{Color.parseColor("#8B5CF6"), Color.parseColor("#6D28D9")});
                btnBg.setCornerRadius(dp(10));
                login.setBackground(btnBg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
                lp.topMargin = dp(16);
                login.setLayoutParams(lp);
                root.addView(login);

                // 🎁 GET KEY button
                Button getKey = new Button(act);
                getKey.setText("🎁  GET KEY");
                getKey.setTextColor(Color.parseColor("#A78BFA"));
                getKey.setTextSize(14);
                GradientDrawable gk = new GradientDrawable();
                gk.setColor(Color.parseColor("#1A1A2E"));
                gk.setCornerRadius(dp(10));
                gk.setStroke(dp(1), Color.parseColor("#7C3AED"));
                getKey.setBackground(gk);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                lp2.topMargin = dp(10);
                getKey.setLayoutParams(lp2);
                root.addView(getKey);

                // 💬 JOIN US button
                Button join = new Button(act);
                join.setText("💬  JOIN US");
                join.setTextColor(Color.parseColor("#A78BFA"));
                join.setTextSize(14);
                GradientDrawable jn = new GradientDrawable();
                jn.setColor(Color.parseColor("#1A1A2E"));
                jn.setCornerRadius(dp(10));
                jn.setStroke(dp(1), Color.parseColor("#7C3AED"));
                join.setBackground(jn);
                LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                lp3.topMargin = dp(8);
                join.setLayoutParams(lp3);
                root.addView(join);

                // Footer
                TextView footer = new TextView(act);
                footer.setText("Powered by " + APP_NAME);
                footer.setTextColor(Color.parseColor("#4B5563"));
                footer.setTextSize(10);
                footer.setGravity(Gravity.CENTER);
                footer.setPadding(0, dp(16), 0, 0);
                root.addView(footer);

                final AlertDialog dlg = new AlertDialog.Builder(act)
                        .setView(scroll)
                        .setCancelable(false)
                        .create();

                // 🔓 LOGIN click
                login.setOnClickListener(v -> {
                    String key = input.getText().toString().trim();
                    if (key.isEmpty()) { toast("Enter your key"); return; }
                    login.setEnabled(false);
                    login.setText("Verifying...");

                    verifyKey(act, key, new Callback() {
                        @Override public void onSuccess() {
                            ui.post(() -> {
                                act.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                        .edit().putString(PREF_KEY, key).apply();
                                dlg.dismiss();
                                toast("✓ Access Granted! Enjoy.");
                                checkUpdate(act);
                            });
                        }
                        @Override public void onFail(String msg) {
                            ui.post(() -> {
                                login.setEnabled(true);
                                login.setText("UNLOCK");
                                toast("✗ " + msg);
                            });
                        }
                    });
                });

                // 🎁 GET KEY click
                getKey.setOnClickListener(v -> openUrl(act, GET_KEY_URL));

                // 💬 JOIN US click
                join.setOnClickListener(v -> openUrl(act, JOIN_US_URL));

                dlg.show();
            } catch (Exception e) {
                Log.e(TAG, "dialog error", e);
            }
        });
    }

    // ═══════════════════════════════════════════════════
    // 🔍 KEY VERIFY — Supabase
    // ═══════════════════════════════════════════════════
    private void verifyKey(final Activity act, final String key, final Callback cb) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject body = new JSONObject();
                body.put("key", key);
                body.put("deviceId", deviceId(act));
                body.put("appToken", "whatever_client");
                body.put("versionCode", CURRENT_VERSION);
                body.put("versionName", "1.0");

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int code = conn.getResponseCode();
                if (code != 200) { cb.onFail("Server error: " + code); return; }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                String status = resp.optString("status", "");

                if ("ACTIVE".equals(status)) cb.onSuccess();
                else cb.onFail(resp.optString("message", "Invalid or Expired Key!"));

            } catch (Exception e) {
                Log.e(TAG, "verify error", e);
                cb.onFail("Connection failed");
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ═══════════════════════════════════════════════════
    // 🔄 UPDATE CHECK — JSON se
    // ═══════════════════════════════════════════════════
    private void checkUpdate(final Activity act) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(UPDATE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code != 200) return;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                int latestVer = resp.optInt("latest_version", CURRENT_VERSION);
                String latestName = resp.optString("latest_version_name", "");
                String updateUrl = resp.optString("update_link", UPDATE_URL_BTN);

                if (latestVer > CURRENT_VERSION) {
                    ui.post(() -> showUpdateDialog(act, latestName, updateUrl));
                }
            } catch (Exception e) {
                Log.d(TAG, "update check skip: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void showUpdateDialog(Activity act, String version, final String url) {
        try {
            LinearLayout root = new LinearLayout(act);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(22), dp(24), dp(22), dp(24));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#0A0A14"));
            bg.setCornerRadius(dp(18));
            bg.setStroke(dp(2), Color.parseColor("#10B981"));
            root.setBackground(bg);

            TextView t = new TextView(act);
            t.setText("🚀 Update Available");
            t.setTextColor(Color.parseColor("#34D399"));
            t.setTextSize(20);
            t.setTypeface(null, Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            root.addView(t);

            TextView m = new TextView(act);
            m.setText("New version " + version + " is available.\nPlease update to continue.");
            m.setTextColor(Color.parseColor("#9CA3AF"));
            m.setTextSize(13);
            m.setGravity(Gravity.CENTER);
            m.setPadding(0, dp(12), 0, dp(20));
            root.addView(m);

            Button update = new Button(act);
            update.setText("UPDATE NOW");
            update.setTextColor(Color.WHITE);
            update.setTypeface(null, Typeface.BOLD);
            GradientDrawable ub = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.parseColor("#10B981"), Color.parseColor("#059669")});
            ub.setCornerRadius(dp(10));
            update.setBackground(ub);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
            update.setLayoutParams(lp);
            root.addView(update);

            Button later = new Button(act);
            later.setText("Later");
            later.setTextColor(Color.parseColor("#9CA3AF"));
            later.setBackgroundColor(Color.TRANSPARENT);
            root.addView(later);

            final AlertDialog dlg = new AlertDialog.Builder(act)
                    .setView(root)
                    .setCancelable(false)
                    .create();

            update.setOnClickListener(v -> {
                openUrl(act, url);
                dlg.dismiss();
            });
            later.setOnClickListener(v -> dlg.dismiss());
            dlg.show();
        } catch (Exception e) {
            Log.e(TAG, "update dialog error", e);
        }
    }

    // ═══════════════════════════════════════════════════
    // 🛠️ HELPERS
    // ═══════════════════════════════════════════════════
    private void openUrl(Activity act, String url) {
        try {
            act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) { toast("Cannot open link"); }
    }

    private String deviceId(Context c) {
        try {
            return Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) { return "unknown"; }
    }

    private void toast(String m) {
        ui.post(() -> Toast.makeText(ctx, m, Toast.LENGTH_LONG).show());
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}
