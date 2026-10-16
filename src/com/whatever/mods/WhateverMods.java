package com.whatever.mods;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    // 🎨 EDITABLE STRINGS
    // ═══════════════════════════════════════════════════

    public static final String your_dialog_name = "🔐 Gojo Satoru Mods";
    public static final String join_us_channel_link = "https://whatsapp.com/channel/XXXXX";
    public static final String get_key_link = "https://animezone-premiums.gt.tc/api/generate.php";
    public static final String put_your_json_link = "https://animezone-premiums.gt.tc/update.json";
    public static final String your_update_link = "https://t.me/yourchannel";
    public static final String your_api_url = "https://animezone-premiums.gt.tc/api/verify.php";

    public static final int CURRENT_VERSION = 1;

    // Logo image — base64 PNG (optional)
    // Khali chhod do abhi — purple circle fallback use hoga
    public static final String LOGO_BASE64 = "";

    // ═══════════════════════════════════════════════════

    private static final String PREF_NAME = "whatever_mods_prefs";
    private static final String PREF_KEY  = "saved_key";

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

    // ═══════════════════════════════════════════════════
    // 🎯 MAIN ENTRY POINT
    // ═══════════════════════════════════════════════════
    public static void inject(final Activity act) {
        if (act == null) return;
        final WhateverMods sdk = new WhateverMods(act);

        String savedKey = act.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY, null);

        if (savedKey != null && !savedKey.isEmpty()) {
            sdk.verifyKey(act, savedKey, new Callback() {
                @Override public void onSuccess() {
                    Log.d(TAG, "Saved key OK");
                    sdk.checkUpdate(act);
                }
                @Override public void onFail(String msg) {
                    Log.d(TAG, "Saved key invalid — showing dialog");
                    sdk.showLicenseDialog(act);
                }
            });
        } else {
            sdk.showLicenseDialog(act);
        }
    }

    // ═══════════════════════════════════════════════════
    // 🎨 LICENSE DIALOG
    // ═══════════════════════════════════════════════════
    private void showLicenseDialog(final Activity act) {
        ui.post(new Runnable() {
            @Override public void run() {
                try {
                    ScrollView scroll = new ScrollView(act);
                    LinearLayout root = new LinearLayout(act);
                    root.setOrientation(LinearLayout.VERTICAL);
                    root.setPadding(dp(22), dp(28), dp(22), dp(24));
                    root.setGravity(Gravity.CENTER_HORIZONTAL);

                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor("#0A0A14"));
                    bg.setCornerRadius(dp(18));
                    bg.setStroke(dp(2), Color.parseColor("#8B5CF6"));
                    root.setBackground(bg);
                    scroll.addView(root);

                    // ─── CIRCLE LOGO ───
                    ImageView logo = new ImageView(act);
                    int size = dp(80);
                    LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(size, size);
                    logoParams.bottomMargin = dp(14);
                    logo.setLayoutParams(logoParams);

                    boolean hasImage = false;
                    if (LOGO_BASE64 != null && !LOGO_BASE64.isEmpty()) {
                        try {
                            byte[] bytes = Base64.decode(LOGO_BASE64, Base64.DEFAULT);
                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (bmp != null) {
                                Bitmap circleBmp = getCircularBitmap(bmp);
                                if (circleBmp != null) {
                                    logo.setImageBitmap(circleBmp);
                                    hasImage = true;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "logo decode: " + e);
                        }
                    }

                    if (!hasImage) {
                        GradientDrawable circle = new GradientDrawable();
                        circle.setShape(GradientDrawable.OVAL);
                        circle.setColor(Color.parseColor("#8B5CF6"));
                        logo.setBackground(circle);
                    }
                    root.addView(logo);

                    // ─── TITLE ───
                    TextView title = new TextView(act);
                    title.setText(your_dialog_name);
                    title.setTextColor(Color.parseColor("#C4B5FD"));
                    title.setTextSize(20);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setGravity(Gravity.CENTER);
                    root.addView(title);

                    // ─── SUBTITLE ───
                    TextView sub = new TextView(act);
                    sub.setText("Premium Unlocked Version");
                    sub.setTextColor(Color.parseColor("#9CA3AF"));
                    sub.setTextSize(12);
                    sub.setGravity(Gravity.CENTER);
                    sub.setPadding(0, dp(4), 0, dp(20));
                    root.addView(sub);

                    // ─── INPUT FIELD ───
                    final EditText input = new EditText(act);
                    input.setHint("GOJO-XXXXXXXX");
                    input.setHintTextColor(Color.parseColor("#6B7280"));
                    input.setTextColor(Color.WHITE);
                    input.setTextSize(15);
                    input.setGravity(Gravity.CENTER);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                    input.setPadding(dp(14), dp(14), dp(14), dp(14));
                    GradientDrawable inputBg = new GradientDrawable();
                    inputBg.setColor(Color.parseColor("#14141F"));
                    inputBg.setCornerRadius(dp(10));
                    inputBg.setStroke(dp(1), Color.parseColor("#4C1D95"));
                    input.setBackground(inputBg);
                    LinearLayout.LayoutParams inp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    inp.bottomMargin = dp(12);
                    input.setLayoutParams(inp);
                    root.addView(input);

                    // ─── GET KEY BUTTON (PRIMARY — green) ───
                    final Button getKey = new Button(act);
                    getKey.setText("🎁  GET KEY");
                    getKey.setTextColor(Color.WHITE);
                    getKey.setTextSize(15);
                    getKey.setTypeface(null, Typeface.BOLD);
                    GradientDrawable gkBg = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{Color.parseColor("#10B981"), Color.parseColor("#059669")});
                    gkBg.setCornerRadius(dp(10));
                    getKey.setBackground(gkBg);
                    LinearLayout.LayoutParams gkp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
                    gkp.bottomMargin = dp(10);
                    getKey.setLayoutParams(gkp);
                    root.addView(getKey);

                    // ─── UNLOCK BUTTON (PRIMARY — purple) ───
                    final Button login = new Button(act);
                    login.setText("🔓  UNLOCK");
                    login.setTextColor(Color.WHITE);
                    login.setTextSize(15);
                    login.setTypeface(null, Typeface.BOLD);
                    GradientDrawable btnBg = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{Color.parseColor("#8B5CF6"), Color.parseColor("#6D28D9")});
                    btnBg.setCornerRadius(dp(10));
                    login.setBackground(btnBg);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
                    lp.bottomMargin = dp(10);
                    login.setLayoutParams(lp);
                    root.addView(login);

                    // ─── JOIN US BUTTON ───
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
                    join.setLayoutParams(lp3);
                    root.addView(join);

                    // ─── FOOTER ───
                    TextView footer = new TextView(act);
                    footer.setText("Powered by " + your_dialog_name);
                    footer.setTextColor(Color.parseColor("#4B5563"));
                    footer.setTextSize(10);
                    footer.setGravity(Gravity.CENTER);
                    footer.setPadding(0, dp(16), 0, 0);
                    root.addView(footer);

                    final AlertDialog dlg = new AlertDialog.Builder(act)
                            .setView(scroll)
                            .setCancelable(false)
                            .create();

                    // ═══ UNLOCK CLICK ═══
                    login.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            final String key = input.getText().toString().trim();
                            if (key.isEmpty()) { toast("Enter your key"); return; }
                            login.setEnabled(false);
                            login.setText("Verifying...");

                            verifyKey(act, key, new Callback() {
                                @Override public void onSuccess() {
                                    ui.post(new Runnable() {
                                        @Override public void run() {
                                            act.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                                    .edit().putString(PREF_KEY, key).apply();
                                            dlg.dismiss();
                                            toast("✓ Access Granted! Enjoy.");
                                            checkUpdate(act);
                                        }
                                    });
                                }
                                @Override public void onFail(final String msg) {
                                    ui.post(new Runnable() {
                                        @Override public void run() {
                                            login.setEnabled(true);
                                            login.setText("🔓  UNLOCK");
                                            toast("✗ " + msg);
                                        }
                                    });
                                }
                            });
                        }
                    });

                    // ═══ GET KEY CLICK — device_id background mein ═══
                    getKey.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            String devId = deviceId(act);
                            String url = get_key_link;
                            if (url.contains("?")) {
                                url += "&device_id=" + Uri.encode(devId);
                            } else {
                                url += "?device_id=" + Uri.encode(devId);
                            }
                            openUrl(act, url);
                        }
                    });

                    // ═══ JOIN US CLICK ═══
                    join.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            openUrl(act, join_us_channel_link);
                        }
                    });

                    dlg.show();
                } catch (Exception e) {
                    Log.e(TAG, "dialog error", e);
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════
    // 🔍 VERIFY KEY
    // ═══════════════════════════════════════════════════
    private void verifyKey(final Activity act, final String key, final Callback cb) {
        executor.execute(new Runnable() {
            @Override public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(your_api_url);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);

                    JSONObject body = new JSONObject();
                    body.put("key", key);
                    body.put("device_id", deviceId(act));

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
            }
        });
    }

    // ═══════════════════════════════════════════════════
    // 🔄 UPDATE CHECK
    // ═══════════════════════════════════════════════════
    private void checkUpdate(final Activity act) {
        executor.execute(new Runnable() {
            @Override public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(put_your_json_link);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);

                    int code = conn.getResponseCode();
                    if (code != 200) return;

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    boolean updateAvailable = resp.optBoolean("update_available", false);
                    int latestVersion = resp.optInt("latest_version", CURRENT_VERSION);
                    final String updateLink = resp.optString("update_link", your_update_link);
                    final String updateMsg = resp.optString("update_message",
                            "A new version is available. Please update to continue.");

                    if (updateAvailable && latestVersion > CURRENT_VERSION) {
                        ui.post(new Runnable() {
                            @Override public void run() {
                                showUpdateDialog(act, updateMsg, updateLink);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.d(TAG, "update check skip: " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    private void showUpdateDialog(final Activity act, String msg, final String url) {
        try {
            LinearLayout root = new LinearLayout(act);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(22), dp(24), dp(22), dp(24));
            root.setGravity(Gravity.CENTER_HORIZONTAL);

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
            m.setText(msg);
            m.setTextColor(Color.parseColor("#9CA3AF"));
            m.setTextSize(13);
            m.setGravity(Gravity.CENTER);
            m.setPadding(0, dp(12), 0, dp(20));
            root.addView(m);

            Button update = new Button(act);
            update.setText("⬇️  UPDATE NOW");
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

            update.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    openUrl(act, url);
                    dlg.dismiss();
                }
            });
            later.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dlg.dismiss(); }
            });
            dlg.show();
        } catch (Exception e) {
            Log.e(TAG, "update dialog error", e);
        }
    }

    // ═══════════════════════════════════════════════════
    // 🎨 CIRCULAR BITMAP HELPER
    // ═══════════════════════════════════════════════════
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        try {
            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, size, size);
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(Color.WHITE);
            canvas.drawOval(rectF, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

            int left = (bitmap.getWidth() - size) / 2;
            int top = (bitmap.getHeight() - size) / 2;
            Rect srcRect = new Rect(left, top, left + size, top + size);
            canvas.drawBitmap(bitmap, srcRect, rect, paint);

            return output;
        } catch (Exception e) {
            Log.e(TAG, "circle bitmap error: " + e);
            return bitmap;
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

    private void toast(final String m) {
        ui.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(ctx, m, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}
